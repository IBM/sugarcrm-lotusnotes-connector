package com.ibm.socialcrm.notesintegration.servlet.servlets;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.apache.commons.lang.StringEscapeUtils;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.utils.SugarV10APIManager;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.core.utils.WebServiceInfoDataShare;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.WebSecurityCodeProvider;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataHub;
import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataShare;
import com.ibm.socialcrm.notesintegration.utils.datahub.calllog.CallFormDataShare;
import com.ibm.socialcrm.notesintegration.utils.datahub.calllog.CurrentSugarEntryDataShare;

public class TypeaheadService extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4330522578036606215L;

	private String trimString(int trimTo, String aString) {
		if (aString.length() <= trimTo) {
			// just return the string
			return aString;
		} else {
			return aString.substring(0, trimTo - 4) + "...";
		}
	}

	private void sortByClientID(List<JSONObject> list) {
		Collections.sort(list, new Comparator<JSONObject>() {
			// Sort results based on the client id where DC is first, SC second, then S third.
			// Within each group, sort alphabetically on name.
			@Override
			public int compare(JSONObject result1, JSONObject result2) {
				int compare = 0;
				String client1Id = "";
				String client2Id = "";
				try {
					client1Id = result1.getString("ccms_id");
				} catch (JSONException e) {
					// no client id, should only happen during development, but catch so as to not stop processing the list
				}
				try {
					client2Id = result2.getString("ccms_id");
				} catch (JSONException e) {
					// no client id, should only happen during development, but catch so as to not stop processing the list
				}

				int client1Type = getValueForClientId(client1Id);
				int client2Type = getValueForClientId(client2Id);

				if (client1Type > client2Type) {
					compare = 1;
				} else if (client1Type < client2Type) {
					compare = -1;
				} else if (client1Type == client2Type) {
					try {
						compare = result1.getString("name").compareTo(result2.getString("name"));
					} catch (JSONException e) {
						compare = 0;
					}
				}

				return compare;
			}

			// Returns 0 for a client id that starts with DC
			// 1 if it starts with SC
			// 2 if it starts with S
			// 3 if there's no prefix (should never happen
			private int getValueForClientId(String clientId) {
				int value = 3;
				if (clientId.toUpperCase().startsWith("DC")) //$NON-NLS-1$
				{
					value = 0;
				} else if (clientId.toUpperCase().startsWith("SC")) //$NON-NLS-1$
				{
					value = 1;
				} else if (clientId.toUpperCase().startsWith("S")) //$NON-NLS-1$
				{
					value = 2;
				}
				return value;
			}
		});

	}

	protected String parseIDsToSkip(String filterData) {
		/*
		 * The filters have the format module,sugarid=label^^module2,sugarid2=label2 for a related item or for an employee sugarid=label^^sugarid2=label2
		 * 
		 * Return a list of ids separated by commas (including the trailing comma) so they will be easy to test against with indexof
		 */
		// split on the ^^, then the =, then split on the comma
		String listOfIds = "";
		String[] filters = filterData.split("\\^\\^");
		for (int i = 0; i < filters.length; i++) {
			// now split on the =
			String[] almostSugarIds = filters[i].split("=");
			if (almostSugarIds[0].indexOf(",") >= 0) {
				listOfIds += almostSugarIds[0].split(",")[1] + ",";
			} else {
				listOfIds += almostSugarIds[0] + ",";
			}
		}
		return listOfIds;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String searchString = request.getParameter("searchString");
		String module = request.getParameter("module");
		String city = request.getParameter("city");
		String specificId = request.getParameter("id");
		String filterData = request.getParameter("filterData");
		String myItemsOnly = request.getParameter("myItemsOnly");
		String responseStr = "";
		String jsonPrefix = "{\"label\":\"name\",\"identifier\":\"id\",\"items\": [";
		String jsonSuffix = "]}";
		HttpSession session = request.getSession(true);
		//System.out.println("In Typeahead servlet " + request.getQueryString());
		boolean errored = false;

		UtilsPlugin.getDefault().logInfoMessage("TypeaheadService: WS request uri:" + request.getRequestURI() + " query string:" + request.getQueryString(), CorePluginActivator.PLUGIN_ID);
		
		if (myItemsOnly == null || myItemsOnly.equals("")) {
			myItemsOnly = "false";
		}
		
		//short circuit empty requests without burning the security code.  The typeahead fields seem to generate them when the page loads and they cause problems invalidating the security codes.
		if ((specificId == null || specificId == "") && (searchString == null || searchString == "" || module == null || module == "" || searchString.length() < 2)) {
			//System.out.println("TypeaheadService: Nothing to do.  Request query string:" + request.getQueryString());
			// return the empty json array
			UtilsPlugin.getDefault().logInfoMessage("TypeaheadService: Nothing to do.  Request string:" + request.getQueryString(), CorePluginActivator.PLUGIN_ID);
			response.setStatus(200);
			responseStr = jsonPrefix + jsonSuffix;
			OutputStream out = response.getOutputStream();
			out.write(responseStr.getBytes());
			out.flush();
			out.close();
			return;
		}
		// check security code, return unauthorized if invalid
		String securityCode = request.getParameter("securityCode");
		boolean useSecurityCode = true;
		if (useSecurityCode && (securityCode == null || securityCode.equals("") || !securityCode.equals(WebSecurityCodeProvider.getInstance().getSecurityCodeString()))) {
			//System.out.println("TypeaheadService: invalid security code.  expected: " + WebSecurityCodeProvider.getInstance().getSecurityCodeString());
			UtilsPlugin.getDefault().logInfoMessage("TypeaheadService: invalid security code.", CorePluginActivator.PLUGIN_ID);
			response.setStatus(403); // forbidden
			responseStr = jsonPrefix + jsonSuffix;
			OutputStream out = response.getOutputStream();
			out.write(responseStr.getBytes());
			out.flush();
			out.close();
			return;
		} else {
			// this code is burned, generate a new one
			WebSecurityCodeProvider.getInstance().generateSecurityCode();
		}
		// check query parms
		if (specificId != null) {
			// grab the data shares
			SFADataHub hub = SFADataHub.getInstance();
			CallFormDataShare callFormShare = (CallFormDataShare) hub.blockingGetDataShare(CallFormDataShare.SHARE_NAME, 20000);
			WebServiceInfoDataShare serviceInfoShare = (WebServiceInfoDataShare) hub.blockingGetDataShare(WebServiceInfoDataShare.SHARE_NAME, 20000);
			// return either the user or the item from the card
			String cnum = (String) serviceInfoShare.get(WebServiceInfoDataShare.USER_CNUM);
			if (cnum.equals(specificId)) {
				// return the single entry for the user
				String userName = (String) serviceInfoShare.get(WebServiceInfoDataShare.USER_FULL_NAME);
				String escapedUserName = StringEscapeUtils.escapeJavaScript(userName);
				responseStr = jsonPrefix + "{ \"name\":\"" + escapedUserName + "\", \"id\":\"" + cnum + "\"}" + jsonSuffix;
			} else {
				// must be looking for the card item
				SFADataShare shareDataFromCard = hub.getDataShare(CurrentSugarEntryDataShare.SHARE_NAME);
				BaseSugarEntry entry = (BaseSugarEntry) (shareDataFromCard.get(CurrentSugarEntryDataShare.CURRENT_SUGAR_ENTRY));
				if (entry != null) {
					SugarType cardType = entry.getSugarType();
					responseStr = jsonPrefix + "{ \"name\":\"" + StringEscapeUtils.escapeJavaScript(entry.getName()) + "\", \"id\":\"" + entry.getId() + "\"}" + jsonSuffix;
					// clear the share so next refresh won't think it was forced from a card
					shareDataFromCard.clear();
				} else {
					// try and pull the previous value from the session
					Map<String, String> fieldValues = (Map<String, String>) session.getAttribute("fieldValues");
					String relatedId = fieldValues.get("parent_id");
					String relatedDisplayName = fieldValues.get("parentIdDisplayValue");
					if (relatedId != null && relatedDisplayName != null) {
						responseStr = jsonPrefix + "{ \"name\":\"" + StringEscapeUtils.escapeJavaScript(relatedDisplayName) + "\", \"id\":\"" + relatedId + "\"}" + jsonSuffix;
					}

				}

			}

		} else if (searchString == null || searchString == "" || module == null || module == "" || searchString.length() < 2) {
			// return the empty json array
			responseStr = jsonPrefix + jsonSuffix;
		} else {

			// if query parms, hit the webservice on sugar and format the results
			//System.out.println("module=" + module + " searchString:" + searchString);
			String output = SugarWebservicesOperations.getInstance().getTypeaheadInfoFromWebservice(module, (searchString == null ? searchString : searchString.trim()), "30", myItemsOnly,
					(city == null ? city : city.trim()));
			//System.out.println("from WS:" + output);
			String idsToSkip = parseIDsToSkip(filterData);
			String itemsString = "";
			boolean skipped = false; // used to eliminate extra comma if we filter out an already selected item
			try {
				JSONObject json = new JSONObject(output);
				if (output.contains("results")) {
					JSONObject results = json.getJSONObject("results");
					JSONArray fields = results.getJSONArray("fields");
					List<JSONObject> entries = new ArrayList<JSONObject>();
					// grab all the objects, then maybe sort by client id
					for (int i = 0; i < fields.length(); i++) {
						JSONObject jsonObject = fields.getJSONObject(i);
						entries.add(jsonObject);
					}
					if (module.equals("Accounts")) {
						// sort by client id, then process
						sortByClientID(entries);
					}
					for (int i = 0; i < entries.size(); i++) {
						if (i != 0) {
							if (skipped) {
								skipped = false;
							} else {
								itemsString += ",";
							}
						}
						JSONObject jsonObject = entries.get(i);
						String name = "";
						String detail = "";
						String clientID = null;
						String formattedName = "";
						try {
							clientID = jsonObject.getString("client_id");
						} catch (JSONException e) {
							// do nothing
						}
						if (module.equals("Accounts")) {
							name += StringEscapeUtils.escapeJavaScript(jsonObject.getString("name"));
						} else if (module.equals("Opportunities")) {
							if (jsonObject.getString("description") != null) {
								detail = " (" + StringEscapeUtils.escapeJavaScript(trimString(33, jsonObject.getString("description"))) + ")";
							}
							name += StringEscapeUtils.escapeJavaScript(jsonObject.getString("name")) + detail;
						} else if (module.equals("Contacts")) {
							if (jsonObject.getString("name") != null) {
								name = StringEscapeUtils.escapeJavaScript(jsonObject.getString("name"));
							}
						}  
						else if (module.equals("Leads")) {
							if (jsonObject.getString("name") != null) {
								name = StringEscapeUtils.escapeJavaScript(jsonObject.getString("name"));
							}
						} else if (module.equals("Users")) {
							name += StringEscapeUtils.escapeJavaScript(jsonObject.getString("name"));
						}

						String sugarID = jsonObject.getString("id");
						// only add the item if it hasn't been selected already
						if (idsToSkip.indexOf(sugarID + ",") < 0) {
							itemsString += "{ \"name\":\"" + name + "\", \"id\":\"" + sugarID + "\"}";
						} else {
							skipped = true;
						}

					}
					//if last character is a comma, remove it
					if (itemsString.length() > 0 && itemsString.charAt(itemsString.length()-1) == ',') {
						itemsString = itemsString.substring(0, itemsString.length()-1);
					}
				}
			} catch (JSONException e) {
				errored = true;
			}
			responseStr = jsonPrefix + itemsString + jsonSuffix;
		}
		if (errored) {
			response.setStatus(500);
			responseStr = jsonPrefix + jsonSuffix;
		} else {
			response.setStatus(200);
		}
		//System.out.println("generated output:" + responseStr);
		UtilsPlugin.getDefault().logInfoMessage("TypeaheadService: WS response:" + responseStr, CorePluginActivator.PLUGIN_ID);
		OutputStream out = response.getOutputStream();
		out.write(responseStr.getBytes());
		out.flush();
		out.close();
	}

}
