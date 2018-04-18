package com.ibm.socialcrm.notesintegration.core.utils;
/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/
/**
 * This class handles building the optimized regex+tag list for contacts and accounts.  It replaces the PHP code that used to perform the same function 
 * on the server.  To limit the number of changes needed elsewhere (risk), this class returns the same JSON structure that the v4 services used.
 */
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;

import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.regexp.DictionaryRegexOptimizer;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;

public class SugarV10RegexHelper {
	private SugarV10APIManager v10Manager;
	// the list of prefixes we factor out
	private String[] prefixList = {"Town of", "City of", "County of", "State of", "Department of", "Division of", "The"};
	// the list of suffexes we factor out
	private String[] suffixList = {"Company", "Incorporated", "Corporation", "Limited", "Group", "GmbH", "cyf", "ccc", "plc", "LLC", "LLP", "Ltd.", "Ltd", "Corp.", "Corp", "Co.", "Co", "Lic.", "Lic",
			"Inc.", "Inc"};
	public SugarV10RegexHelper() {
		setV10Manager(SugarV10APIManager.getInstance());
	}
	private SugarV10APIManager getV10Manager() {
		return v10Manager;
	}
	private void setV10Manager(SugarV10APIManager manager) {
		v10Manager = manager;
	}
	private String[] getPrefixList() {
		return prefixList;
	}
	private void setPrefixList(String[] prefixList) {
		this.prefixList = prefixList;
	}
	private String[] getSuffixList() {
		return suffixList;
	}
	private void setSuffixList(String[] suffixList) {
		this.suffixList = suffixList;
	}
	/**
	 * Returns the optimized regular expression and tag list for Account or Contact myitems. The items are retrieved from the server using a v10 get, 500 at a time. Utilizes DictionaryRegexOptimizer
	 * to produce the optimized regex.
	 * 
	 * @param module
	 *        A String containing either "Contacts" or "Accounts"
	 * @return String The JSONObject string containing the regular expression and tag list, in v4 format
	 */
	public String getV4Regex(String module) throws LoginException {
		boolean isAccountReq = module != null && module.equals("Accounts");
		boolean isContactReq = module != null && module.equals("Contacts");
		int currentOffset = 0;
		int previousOffset;
		String response;
		ArrayList itemsWithKeys = new ArrayList();
		DictionaryRegexOptimizer dro = new DictionaryRegexOptimizer();
		String request;
		UtilsPlugin.getDefault().logInfoMessage("SugarV10APIManager:getV4Regex - Beginning to pull regexes for: " + module, CorePluginActivator.PLUGIN_ID);
		// grab the server preferences for max items and batch size
		int batchSize = 500;
		int maxAccounts = 5000;
		int maxContacts = 5000;
		try {
			maxAccounts = Integer.parseInt(SugarDashboardPreference.getInstance().getSugarMaxAccountMyItemsPreference());
			maxContacts = Integer.parseInt(SugarDashboardPreference.getInstance().getSugarMaxContactMyItemsPreference());
			// acwtemp - just a temp. patch, remove it when 86466 is fixed
			// maxAccounts = 50;
			// maxContacts = 50;

			batchSize = Integer.parseInt(SugarDashboardPreference.getInstance().getSugarMaxRegexBatchSizePreference());
		} catch (Exception e) {
			UtilsPlugin.getDefault().logErrorMessage("SugarV10APIManager:getV4Regex - Unable to retrieve prefs for max values", CorePluginActivator.PLUGIN_ID);
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}
		try {
			do {
				if (isAccountReq) {
					request = buildAccountRequestFromOffset(currentOffset, batchSize);
				} else if (isContactReq) {
					request = buildContactRequestFromOffset(currentOffset, batchSize);
				} else {
					UtilsPlugin.getDefault().logErrorMessage("SugarV10APIManager:getV4Regex - Bad module type\n" + module, CorePluginActivator.PLUGIN_ID);
					return ""; // should never happen
				}
				previousOffset = currentOffset;
				response = getV10Manager().getURI(request);

				if (isAccountReq) {
					currentOffset = updateItemsFromAccountResponse(dro, itemsWithKeys, response);
				} else if (isContactReq) {
					currentOffset = updateItemsFromContactResponse(dro, itemsWithKeys, response);
				} else {
					UtilsPlugin.getDefault().logErrorMessage("SugarV10APIManager:getV4Regex - Bad module type\n" + module, CorePluginActivator.PLUGIN_ID);
					return ""; // should never happen
				}
			} while (currentOffset != -1 && currentOffset > previousOffset && ((currentOffset < maxContacts && isContactReq) || (currentOffset < maxAccounts && isAccountReq))); // failsafe to prevent
																																													// infinite loop
			// generate an optimized regex
			/*
			 * System.out.println("Items to match:"); for (int i=0;i<itemsWithKeys.size();i++) { System.out.print(itemsWithKeys.get(i)); } System.out.println("");
			 */
			response = buildV4RegexResponse(dro, itemsWithKeys);
			UtilsPlugin.getDefault().logInfoMessage("SugarV10APIManager:getV4Regex - Finished building regex for: " + module + " regex:" + response, CorePluginActivator.PLUGIN_ID);
			// System.out.println("Regex generated:" + response);
		} catch (JSONException e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			response = "";
		}
		return response;
	}
	/**
	 * Processes the v10 Accounts response into individual items. Prefixes and suffexes are removed from the account name, and tags are added. Then the items are added to the list DRO and the key list
	 * 
	 * @param dro
	 *        The DictionaryRegexOptimizer that is being used for this list
	 * @param itemsWithKeys
	 *        The list of item ids and the keys that will be matched
	 * @param response
	 *        The v10 JSON response to parse
	 * @return int The offset that marks where the next request should start
	 */
	private int updateItemsFromAccountResponse(DictionaryRegexOptimizer dro, ArrayList itemsWithKeys, String response) throws JSONException {
		int nextOffset;
		JSONObject result = new JSONObject(response);
		JSONArray records = result.getJSONArray("records");
		Iterator i = records.iterator();
		nextOffset = result.getInt("next_offset");
		while (i.hasNext()) {
			JSONObject aRecord = (JSONObject) i.next();
			ArrayList<String> recordValues = new ArrayList<String>(3);
			String id = aRecord.getString("id");
			String name = aRecord.getString("name");
			String altName = aRecord.getString("alt_language_name");
			name = removeAltNameFromName(name, altName);
			name = factorOutCommonPrefixes(name);
			name = factorOutCommonSuffixes(name);
			altName = factorOutCommonPrefixes(altName);
			altName = factorOutCommonSuffixes(altName);
			recordValues.add(id);
			recordValues.add(name);
			recordValues.add(altName);
			dro.addEntry(name);
			dro.addEntry(altName);
			JSONObject tags = aRecord.optJSONObject("tags");
			if (tags != null) {
				Iterator<String> tagKeys = tags.keys();
				while (tagKeys.hasNext()) {
					String aTag = tagKeys.next();
					recordValues.add(aTag);
					dro.addEntry(aTag);
				}
			}
			itemsWithKeys.add(recordValues);
		}
		return nextOffset;
	}
	/**
	 * Builds the JSON object that the v4 web services returned for a regex.
	 * 
	 * @param dro
	 *        The DictionaryRegexOptimizer that is being used for this list
	 * @param itemsWithKeys
	 *        The list of item ids and the keys that will be matched
	 * @return String The JSON string for the v4 response
	 */
	private String buildV4RegexResponse(DictionaryRegexOptimizer dro, ArrayList<ArrayList> itemsWithKeys) throws JSONException {
		JSONObject v4Response = new JSONObject();
		JSONArray regexDataArray = new JSONArray();
		JSONObject regexEntries = new JSONObject();

		v4Response.put("header", ""); // unused, but in case anyone is looking for it
		v4Response.put("regexData", regexDataArray);
		regexDataArray.put(dro.getRegex());
		regexDataArray.put(regexEntries);
		for (ArrayList<String> aRecord : itemsWithKeys) {
			String id = aRecord.get(0);
			JSONArray tags = new JSONArray();
			for (int i = 1; i < aRecord.size(); i++) {
				String aTag = aRecord.get(i);
				if (aTag != null && !aTag.equals("")) {
					tags.add(aTag);
				}
			}
			regexEntries.put(id, tags);
		}
		return v4Response.toString();
	}
	/**
	 * Given a name that may include (<alt name>) at the end, remove the parens and alt name portion if they are the same. Many accounts come back with "Some Company (Some Company)", so remove the
	 * redundant information if it is there. It's expected this will be the name of an account. A contact name would require more complicated processing.
	 * 
	 * @param name
	 *        The String that may include the alt name as well
	 * @param altName
	 *        The String representing the alt name
	 * @return String The name string without the alt portion, unless the alt portion is provided and differs from the name
	 */
	public String removeAltNameFromName(String name, String altName) {
		// the name may have alt_lang_name in parens if it exists. That gets long and is not useful for livetext, so let's drop that if it is the same as the regular name
		String namePortion = "";
		String altPortion = "";
		int openParenIdx = name.indexOf('(');
		int closeParenIdx = name.indexOf(')', openParenIdx);
		if (openParenIdx > 0 && closeParenIdx > openParenIdx) {
			// there is an alt name in parens in the name. Remove it if it is identical
			namePortion = name.substring(0, openParenIdx).trim();
			altPortion = name.substring(openParenIdx + 1, closeParenIdx).trim();
			if (namePortion.equals(altPortion)) {
				return namePortion;
			}
		}
		return name;
	}
	/**
	 * Factor out common prefixes from the account name provided. The prefix list is hardcoded in prefixList. Only the first prefix to match is removed.
	 * 
	 * @param input
	 *        The string that may contain common prefixes to remove
	 * @return String The string without the first common prefix we match
	 */
	private String factorOutCommonPrefixes(String input) {
		String[] prefixes = getPrefixList();
		String replaced = input;
		for (String aPrefix : prefixes) {
			// case insensitive: beginline+string+space
			replaced = input.replaceAll("(?i)^" + preg_quote(aPrefix) + " ", "");
			if (!input.equals(replaced)) {
				// when we find a match, break
				break;
			}
		}
		return replaced;
	}
	/**
	 * Factor out common suffixes from the account name provided. The suffix list is hardcoded in suffixList. Only the first suffix to match is removed.
	 * 
	 * @param input
	 *        The string that may contain common suffixes to remove
	 * @return String The string without the first common suffix we match
	 */
	private String factorOutCommonSuffixes(String input) {
		String[] suffixes = getSuffixList();
		String replaced = input;
		for (String aSuffix : suffixes) {
			// case insensitive: space+string+endofline
			replaced = input.replaceAll("(?i) " + preg_quote(aSuffix) + "$", "");
			if (!input.equals(replaced)) {
				// when we find a match, break
				break;
			}
		}
		return replaced;
	}
	/**
	 * Escape regex special chars like PHP's preg_quote
	 * 
	 * @param aString
	 *        The regex to escape
	 * @return String The escaped regex
	 */
	private String preg_quote(String aString) {
		return aString.replaceAll("[.\\\\+*?\\[\\^\\]$(){}=!<>|:\\-]", "\\\\$0");
	}
	/**
	 * Escape regex special chars like PHP's preg_quote
	 * 
	 * @param aString
	 *        The regex to escape
	 * @return String The escaped regex
	 */
	private String processStringForRegex(String input) {
		String out = input.replaceAll("\\s+$", ""); // remove whitespace/newlines from the end
		out = out.replaceAll("\\", "\\\\"); // slash to double slash
		out = out.replaceAll("\\[", "\\\\["); // bracket with slash bracket
		out = out.replaceAll("\\^", "\\\\^"); // s/\^/\\\^/g caret with slash caret
		out = out.replaceAll("\\$", "\\\\$"); // s/\$/\\\$/g $ with slash $
		out = out.replaceAll("\\|", ""); // s/\|//g a pipe with nothing
		out = out.replaceAll("\\?", "\\\\?"); // s/\?/\\\?/g ? with slash ?
		out = out.replaceAll("\\+", "\\\\+"); // s/\+/\\\+/g ? with slash ?
		return out;
	}
	/**
	 * Build the v10 Account myitem request
	 * 
	 * @param offset
	 *        An int with the current offset
	 * @param maxValues
	 *        An int with how many responses to get at a time
	 * @return String The REST URI to retrieve
	 */
	private String buildAccountRequestFromOffset(int offset, int maxValues) {
		String request = "Accounts?" + "fields=id,name,date_modified,alt_language_name,tags" + "&order_by=date_modified:DESC" + "&offset=" + offset + "&favorites=0" + "&my_items=1" + "&lite=1"
				+ "&max_num=" + maxValues;
		return request;
	}
	/**
	 * Build the v10 Contact myitem request
	 * 
	 * @param offset
	 *        An int with the current offset
	 * @param maxValues
	 *        An int with how many responses to get at a time
	 * @return String The REST URI to retrieve
	 */
	private String buildContactRequestFromOffset(int offset, int maxValues) {
		String request = "Contacts?" + "fields=id,date_modified,first_name,last_name,alt_lang_first_c,alt_lang_last_c,email" + "&order_by=date_modified:DESC" + "&offset=" + offset + "&favorites=0"
				+ "&my_items=1" + "&lite=1" + "&max_num=" + maxValues;
		return request;
	}
	/**
	 * Processes the v10 Contacts response into individual items. Each Contact gets tags for first last, last first with regular spaces and ideographic spaces. The same is then done for the alt_names
	 * if they differ. Then the items are added to the DRO and the key list.
	 * 
	 * @param dro
	 *        The DictionaryRegexOptimizer that is being used for this list
	 * @param itemsWithKeys
	 *        The list of item ids and the keys that will be matched
	 * @param response
	 *        The v10 JSON response to parse
	 * @return int The offset that marks where the next request should start
	 */
	private int updateItemsFromContactResponse(DictionaryRegexOptimizer dro, ArrayList itemsWithKeys, String response) throws JSONException {
		int nextOffset;
		JSONObject result = new JSONObject(response);
		JSONArray records = result.getJSONArray("records");
		Iterator i = records.iterator();
		nextOffset = result.getInt("next_offset");
		while (i.hasNext()) {
			JSONObject aRecord = (JSONObject) i.next();
			ArrayList<String> recordValues = new ArrayList<String>(9); // assuming basic fields and 1 email for starters
			recordValues.add(aRecord.containsKey("id") ? aRecord.getString("id") : ConstantStrings.EMPTY_STRING);
			// process first and last name
			String firstName = aRecord.containsKey("first_name") ? aRecord.getString("first_name") : ConstantStrings.EMPTY_STRING;
			String lastName = aRecord.containsKey("last_name") ? aRecord.getString("last_name") : ConstantStrings.EMPTY_STRING;
			String tempName = firstName + " " + lastName;
			String tempNameLF = lastName + " " + firstName;
			String tempNameDB = firstName + "\u3000" + lastName; // ideographic space
			String tempNameLFDB = lastName + "\u3000" + firstName;
			addItemToBoth(tempName, dro, recordValues);
			addItemToBoth(tempNameLF, dro, recordValues);
			addItemToBoth(tempNameDB, dro, recordValues);
			addItemToBoth(tempNameLFDB, dro, recordValues);

			// process alt names
			// 81620 ( or 81464)
			// String altFirstName = aRecord.getString("alt_lang_first_c");
			// String altLastName = aRecord.getString("alt_lang_last_c");
			String altFirstName = aRecord.containsKey("alt_lang_first_c") ? aRecord.getString("alt_lang_first_c") : ConstantStrings.EMPTY_STRING;
			String altLastName = aRecord.containsKey("alt_lang_last_c") ? aRecord.getString("alt_lang_last_c") : ConstantStrings.EMPTY_STRING;
			String tempAltName = "";
			String tempAltNameLF = "";
			String tempAltNameDB = "";
			String tempAltNameLFDB = "";
			if (!altFirstName.equals("")) {
				tempAltName = altFirstName;
				tempAltNameLF = altFirstName;
				tempAltNameDB = altFirstName;
				tempAltNameLFDB = altFirstName;
			}
			if (!altLastName.equals("")) {
				// if the first name was included, append last one with a space, otherwise just use the last name
				tempAltName = !tempAltName.equals("") ? tempAltName + " " + altLastName : altLastName;
				tempAltNameLF = !tempAltNameLF.equals("") ? altLastName + " " + tempAltNameLF : altLastName;
				tempAltNameDB = !tempAltNameDB.equals("") ? tempAltNameDB + "\u3000" + altLastName : altLastName;
				tempAltNameLFDB = !tempAltNameLFDB.equals("") ? altLastName + "\u3000" + tempAltNameLFDB : altLastName;
			}
			// don't add to tag array if the alt name is the same as the primary name
			if (!tempAltName.equals(tempName)) {
				addItemToBoth(tempAltName, dro, recordValues);
			}
			if (!tempAltNameLF.equals(tempNameLF)) {
				addItemToBoth(tempAltNameLF, dro, recordValues);
			}
			if (!tempAltNameDB.equals(tempNameDB)) {
				addItemToBoth(tempAltNameDB, dro, recordValues);
			}
			if (!tempAltNameLFDB.equals(tempNameLFDB)) {
				addItemToBoth(tempAltNameLFDB, dro, recordValues);
			}

			// process email addresses

			JSONObject email = aRecord.optJSONObject("email");
			processTagsIntoBoth(email, dro, recordValues);
			// add this record to the total
			itemsWithKeys.add(recordValues);
		}
		return nextOffset;
	}
	/**
	 * Convenience method to add an item to both the DRO and the tag list
	 * 
	 * @param dro
	 *        The DictionaryRegexOptimizer being used
	 * @param items
	 *        The ArrayList containing the ids and tags
	 */
	private void addItemToBoth(String item, DictionaryRegexOptimizer dro, ArrayList items) {
		if (item != null && !item.equals("")) {
			items.add(item);
			dro.addEntry(item);
		}
	}
	/**
	 * Process the JSONObject representing the tags into both the DRO and the tag list
	 * 
	 * @param tags
	 *        The JSONObject for the tag portion of the v10 resposne
	 * @param dro
	 *        The DictionaryRegexOptimizer being used
	 * @param recordValues
	 *        The ArrayList containing the ids and tags
	 */
	private void processTagsIntoBoth(JSONObject tags, DictionaryRegexOptimizer dro, ArrayList recordValues) {
		if (tags != null) {
			Iterator<String> tagKeys = tags.keys();
			while (tagKeys.hasNext()) {
				String aTag = tagKeys.next();
				recordValues.add(aTag);
				dro.addEntry(aTag);
			}
		}
	}
}
