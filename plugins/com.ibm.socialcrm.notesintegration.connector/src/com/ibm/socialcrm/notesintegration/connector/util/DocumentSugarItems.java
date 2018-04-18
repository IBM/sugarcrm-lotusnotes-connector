package com.ibm.socialcrm.notesintegration.connector.util;

/****************************************************************
 * IBM Confidential
 *
 * SFA050-Collaboration Source Materials
 *
 * (C) Copyright IBM Corp. 2012
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office
 *
 ***************************************************************/

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONObject;
import org.apache.commons.json.JSONString;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import com.ibm.socialcrm.notesintegration.connector.Activator;
import com.ibm.socialcrm.notesintegration.connector.providers.TypeaheadContentProvider;
import com.ibm.socialcrm.notesintegration.core.SugarEntrySurrogate;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.sugarwidgetupdate.SugarWidgetUpdateActivator;
import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.ui.actions.MailDocumentSelectionAction;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateData;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateDataMap;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

/*
 * Contains information on all sugar items discovered in a Notes document that potentially are
 * "real" sugar items and can be used for association.
 */
public class DocumentSugarItems {

	public final static int MAX_ACCOUNT_RESULTLIMIT = 10;

	private AssociateDataMap _associateDataMap = null;
	private Map<SugarType, Set<SugarEntrySurrogate>> _sugarDataCacheMap = null;
	private String[] _assigneeList = null;

	private int _currSelectIndex = -1;
	private int _maxLineWidth = 0; // max line width for all non-contact items

	// Note that Contact ID, Client sugar/site ID are not recognized by live text
	// so we're not going to worry about them here.
	private final static String SELECTWHERE_OPPTY_ID = "1"; //$NON-NLS-1$
	private final static String SELECTWHERE_CONTACT_NAME = "2"; //$NON-NLS-1$
	private final static String SELECTWHERE_CONTACT_EMAIL = "3"; //$NON-NLS-1$
	private final static String SELECTWHERE_ACCOUNT_NAME = "4"; //$NON-NLS-1$
	
	private final static String SELECTWHERE_ACCOUNT_CCMS = "5"; //$NON-NLS-1$
	Map<String, List<String>> _selectwherehMap = new HashMap<String, List<String>>();

	// Map< item name, Map< id, tempResultObj>>
	private Map<String, Map<String, tempResultObj>> _resultMap = new HashMap<String, Map<String, tempResultObj>>();

	public DocumentSugarItems(Map<SugarType, Set<SugarEntrySurrogate>> sugarDataCacheMap, String[] assigneeList, AssociateDataMap admap) {
		_sugarDataCacheMap = sugarDataCacheMap;
		_assigneeList = assigneeList;
		_associateDataMap = admap;

		// printSugarDataCacheMap();
		// printAssigneeList();
	}

	public void getDocumentItems() {

		Job job = new Job("getDocumentItemsJob") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				getSugarItems();
				return Status.OK_STATUS;
			}

		};
		// Setting job rule so jobs following this rule will be executed in the correct order.
		job.setRule(UiUtils.DISPLAY_SUGAR_ITEM_BY_ID_JOB_RULE);
		job.schedule();

	}

	private void getSugarItems() {
		buildOpportunitiesList();
		buildContactsList();
		buildAccountsList();

		if (_selectwherehMap != null && !_selectwherehMap.isEmpty()) {
			SugarType type = null;

			Iterator<Entry<String, List<String>>> it = _selectwherehMap.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, List<String>> entry = it.next();
				String arguments = ConstantStrings.EMPTY_STRING;

				if (entry.getKey().equals(SELECTWHERE_OPPTY_ID)) {
					type = SugarType.OPPORTUNITIES;
					arguments = buildModuleArguments(entry.getValue(), type.getParentType(), "opportunities.id"); //$NON-NLS-1$
					performGetEntryListWebService(arguments, type);
				} else if (entry.getKey().equals(SELECTWHERE_CONTACT_NAME)) {
					type = SugarType.CONTACTS;
					performTypeAheadWebservice(entry.getValue(), type);
				} else if (entry.getKey().equals(SELECTWHERE_CONTACT_EMAIL)) {
					type = SugarType.CONTACTS;
					arguments = SugarWebservicesOperations.getInstance().buildModuleEmailArguments(entry.getValue(), type.getParentType(), "contacts.id"); //$NON-NLS-1$
					performGetEntryListWebService(arguments, type);
				} else if (entry.getKey().equals(SELECTWHERE_ACCOUNT_NAME)) {
					type = SugarType.ACCOUNTS;
					performTypeAheadWebservice(entry.getValue(), type);
				} 
				
				else if (entry.getKey().equals(SELECTWHERE_ACCOUNT_CCMS)) {
					type = SugarType.ACCOUNTS;
					arguments = buildModuleArguments(entry.getValue(), type.getParentType(), "accounts.ccms_id"); //$NON-NLS-1$
					performGetEntryListWebService(arguments, type);
				}
				
			}
		}
	}

	private void performTypeAheadWebservice(List<String> nameList, SugarType type) {
		if (type != null && nameList != null && !nameList.isEmpty()) {
			Iterator<String> nameIt = nameList.iterator();
			while (nameIt.hasNext()) {
				String name = nameIt.next();
				JSONObject[] jsonObjects = null;

				try {

					String searchResults = SugarWebservicesOperations.getInstance().getTypeaheadInfoFromWebservice(type.getParentType(), name, TypeaheadContentProvider.TYPEAHEAD_RESULTLIMIT, "true", //$NON-NLS-1$
							ConstantStrings.EMPTY_STRING);

					UiUtils.webServicesLog("typeahead", null, searchResults); //$NON-NLS-1$
					final JSONObject searchResultsJSON = new JSONObject(searchResults);
					JSONArray resultsArray = searchResultsJSON.getJSONObject(ConstantStrings.RESULTS).getJSONArray(ConstantStrings.DATABASE_FIELDS);

					ArrayList<JSONObject> jsonObjectList = new ArrayList<JSONObject>();

					for (int i = 0; i < resultsArray.length(); i++) {
						JSONObject jsonObject = (JSONObject) resultsArray.get(i);
						jsonObjectList.add(jsonObject);
					}

					jsonObjects = jsonObjectList.toArray(new JSONObject[jsonObjectList.size()]);
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
					jsonObjects = new JSONObject[0];
				}

				if (jsonObjects != null && jsonObjects.length > 0) {
					for (JSONObject jsonObject : jsonObjects) {

						String id = null;
						try {
							id = jsonObject.getString(ConstantStrings.DATABASE_ID);
						} catch (Exception e) {
							id = null;
						}

						if (id != null && !isAssociated(type, id)) {
							String mapname = null;
							if (type.equals(SugarType.ACCOUNTS)) {
								mapname = extractAccountJSONNameValue(jsonObject);
							} else {
								mapname = ConnectorUtil.getInstance().getResultParser(type).getItemFromDocumentWithTypeaheadText(jsonObject);
							}

							Map<String, tempResultObj> results = null;
							if (_resultMap.containsKey(mapname)) {
								results = _resultMap.get(mapname);
							} else {
								results = new HashMap<String, tempResultObj>();
								_resultMap.put(mapname, results);
							}

							results.put(id, new tempResultObj(id, type, ConnectorUtil.getInstance().getResultParser(type).getItemFromDocumentWithTypeaheadText(jsonObject), ConnectorUtil.getInstance()
									.getResultParser(type).getAssociatedText(jsonObject), ConnectorUtil.getInstance().getResultParser(type).getAssociatedExtendedText(jsonObject)));

						}
					}
				}

			}

		}
	}

	private void performGetEntryListWebService(String arguments, SugarType type) {
		if (type != null && arguments != null && !arguments.equals(ConstantStrings.EMPTY_STRING)) {

			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("method", "get_entry_list"); //$NON-NLS-1$ //$NON-NLS-2$
			parameters.put("arguments", arguments); //$NON-NLS-1$

			String output = SugarWebservicesOperations.getInstance().getSugarInfoFromWebService(parameters);

			UiUtils.webServicesLog("sugarIteminDocument", null, output); //$NON-NLS-1$

			try {

				extractJSONOutput(type, output);
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);

			}

		}
	}

	// example: "module_name":"Opportunities","query":"(opportunities.id='DC28C2B4' or opportunities.id='xxxxxxxx')","select_fields":""
	private String buildModuleArguments(List<String> idList, String moduleName, String columnName) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		boolean isFirst = true;
		if (idList != null && idList.size() > 0) {
			sb.append("{\"module_name\":\"").append(moduleName).append("\",\"query\":\"("); //$NON-NLS-1$ //$NON-NLS-2$
			for (String id : idList) {
				if (isFirst) {
					isFirst = false;
				} else {
					sb.append(" or "); //$NON-NLS-1$
				}
				sb.append(columnName).append("='").append(id).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			sb.append(")\",\"select_fields\":\"\"}"); //$NON-NLS-1$ 
		}
		return sb.toString();
	}

	private void extractJSONOutput(SugarType type, String output) {
		try {

			final JSONObject searchResultsJSON = new JSONObject(output);

			if (searchResultsJSON.containsKey("entry_list")) //$NON-NLS-1$
			{

				JSONArray resultsArray = searchResultsJSON.getJSONArray("entry_list"); //$NON-NLS-1$

				for (int i = 0; i < resultsArray.length(); i++) {
					JSONObject entrylistObject = (JSONObject) resultsArray.get(i);
					JSONObject namevaluelistObject = entrylistObject.getJSONObject("name_value_list"); //$NON-NLS-1$

					String id = entrylistObject.getString("id"); //$NON-NLS-1$

					if (!isAssociated(type, id)) {
						String name = null;
						if (type.equals(SugarType.ACCOUNTS)) {
							name = extractAccountJSONNameValue(namevaluelistObject);
						} else {
							name = extractNonAccountJSONNameValue(type, namevaluelistObject);
						}

						Map<String, tempResultObj> results = null;
						if (_resultMap.containsKey(name)) {
							results = _resultMap.get(name);
						} else {
							results = new HashMap<String, tempResultObj>();
							_resultMap.put(name, results);
						}

						results.put(id, new tempResultObj(id, type, ConnectorUtil.getInstance().getResultParser(type).getItemFromDocumentText(namevaluelistObject), ConnectorUtil.getInstance()
								.getResultParser(type).getAssociatedText(namevaluelistObject), ConnectorUtil.getInstance().getResultParser(type).getAssociatedExtendedText(namevaluelistObject)));

					}

				}
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);

		}
	}

	private String extractNonAccountJSONNameValue(SugarType type, JSONObject jsonObject) {
		String name = ConstantStrings.EMPTY_STRING;
		if (jsonObject != null && type != null) {
			name = ConnectorUtil.getInstance().getResultParser(type).getItemFromDocumentText(jsonObject);
		}
		return name;

	}

	private String extractAccountJSONNameValue(JSONObject jsonObject) {
		String name = ConstantStrings.EMPTY_STRING;
		if (jsonObject != null) {
			try {
				if (jsonObject.containsKey(ConstantStrings.DATABASE_NAME) && (jsonObject.get(ConstantStrings.DATABASE_NAME) instanceof JSONObject)) {
					name = jsonObject.getJSONObject(ConstantStrings.DATABASE_NAME).getString("value"); //$NON-NLS-1$
				} else if (jsonObject.containsKey(ConstantStrings.DATABASE_NAME) && (jsonObject.get(ConstantStrings.DATABASE_NAME) instanceof JSONString)) {
					name = jsonObject.getString(ConstantStrings.DATABASE_NAME);
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}
		}
		return name;
	}

	/*
	 * extract all the opportunity items
	 */
	private void buildOpportunitiesList() {

		if (_sugarDataCacheMap != null && !_sugarDataCacheMap.isEmpty() && _sugarDataCacheMap.containsKey(SugarType.OPPORTUNITIES)) {
			Set<SugarEntrySurrogate> set = _sugarDataCacheMap.get(SugarType.OPPORTUNITIES);
			Iterator<SugarEntrySurrogate> it = set.iterator();
			while (it.hasNext()) {
				SugarEntrySurrogate sugarEntry = it.next();

				if (sugarEntry.getName() != null && !isToFilterOutFromBuildList(sugarEntry)) {

					List<String> selectwhereList = _selectwherehMap.get(SELECTWHERE_OPPTY_ID);
					if (selectwhereList == null) {
						selectwhereList = new ArrayList<String>();
						_selectwherehMap.put(SELECTWHERE_OPPTY_ID, selectwhereList);
					}
					selectwhereList.add(sugarEntry.getName());
				}
			}
		}

	}

	private void buildContactsList() {

		if (_sugarDataCacheMap != null && !_sugarDataCacheMap.isEmpty() && _sugarDataCacheMap.containsKey(SugarType.CONTACTS)) {
			Set<SugarEntrySurrogate> set = _sugarDataCacheMap.get(SugarType.CONTACTS);
			Iterator<SugarEntrySurrogate> it = set.iterator();
			while (it.hasNext()) {
				SugarEntrySurrogate sugarEntry = it.next();

				if (sugarEntry.getName() != null && !isToFilterOutFromBuildList(sugarEntry)) {
					String mapIndex = SELECTWHERE_CONTACT_NAME;
					if (sugarEntry.getName() != null && sugarEntry.getName().contains("@")) //$NON-NLS-1$
					{
						mapIndex = SELECTWHERE_CONTACT_EMAIL;
					}
					List<String> selectwhereList = _selectwherehMap.get(mapIndex);
					if (selectwhereList == null) {
						selectwhereList = new ArrayList<String>();
						_selectwherehMap.put(mapIndex, selectwhereList);
					}
					selectwhereList.add(sugarEntry.getName());
				}

			}

		}
		if (_assigneeList != null && _assigneeList.length > 0) {
			for (String assignee : _assigneeList) {
				List<String> selectwhereList = _selectwherehMap.get(SELECTWHERE_CONTACT_EMAIL);
				if (selectwhereList == null) {
					selectwhereList = new ArrayList<String>();
					_selectwherehMap.put(SELECTWHERE_CONTACT_EMAIL, selectwhereList);
				}
				selectwhereList.add(assignee);
			}
		}
	}

	private void buildAccountsList() {

		if (_sugarDataCacheMap != null && !_sugarDataCacheMap.isEmpty() && _sugarDataCacheMap.containsKey(SugarType.ACCOUNTS)) {
			Set<SugarEntrySurrogate> set = _sugarDataCacheMap.get(SugarType.ACCOUNTS);
			Iterator<SugarEntrySurrogate> it = set.iterator();
			while (it.hasNext()) {
				SugarEntrySurrogate sugarEntry = it.next();

				if (sugarEntry.getName() != null && !isToFilterOutFromBuildList(sugarEntry)) {

					if (isAccountName(sugarEntry.getName())) {

						List<String> selectwhereList = _selectwherehMap.get(SELECTWHERE_ACCOUNT_NAME);
						if (selectwhereList == null) {
							selectwhereList = new ArrayList<String>();
							_selectwherehMap.put(SELECTWHERE_ACCOUNT_NAME, selectwhereList);
						}
						selectwhereList.add(sugarEntry.getName());

					} else {
						
						
						List<String> selectwhereList = _selectwherehMap.get(SELECTWHERE_ACCOUNT_CCMS);
						if (selectwhereList == null) {
							selectwhereList = new ArrayList<String>();
							_selectwherehMap.put(SELECTWHERE_ACCOUNT_CCMS, selectwhereList);
						}
						
						selectwhereList.add(sugarEntry.getName());

					}

				}
			}
		}
	}

	private boolean isToFilterOutFromBuildList(SugarEntrySurrogate sugarEntry) {
		boolean isToFilterOut = false;
		if (sugarEntry != null) {
			if (sugarEntry.getType().equals(MailDocumentSelectionAction.getDefaultAssociateSugarType()) && sugarEntry.getName() != null
					&& sugarEntry.getName().equals(MailDocumentSelectionAction.CRM_ASSOCIATE)) {
				isToFilterOut = true;
			}
		}
		return isToFilterOut;
	}

	private boolean isAccountName(String text) {
		boolean isAccountName = false;
		Map<String, Set<String>> tagMap = SugarWidgetUpdateActivator.getDefault().getTagMapForType(SugarType.ACCOUNTS);
		// printTagMap(tagMap);

		Pattern pattern = SugarWidgetUpdateActivator.getDefault().getAccountsPattern();

		if (text != null && pattern != null && !pattern.equals(ConstantStrings.EMPTY_STRING) && tagMap != null && !tagMap.isEmpty()) {
			try {

				String matchid = ConstantStrings.EMPTY_STRING;

				Matcher matcher = pattern.matcher(text);
				while (matcher.find()) {
					String match = matcher.group();

					for (String key : tagMap.keySet()) {
						Set<String> tagsForThisKey = tagMap.get(key);
						for (String tag : tagsForThisKey) {
							Pattern tagPattern = Pattern.compile(tag, Pattern.CASE_INSENSITIVE);
							if (tagPattern.matcher(match).matches()) {
								isAccountName = true;
								break;
							}
						}

					}
				}
			}

			catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
			}
		}

		return isAccountName;

	}

	public List<String> getFormattedResultList(int limitPixel) {
		List<String> list = new ArrayList<String>();
		if (getResultList() != null && !getResultList().isEmpty()) {
			Iterator<tempResultObj> it = getResultList().iterator();
			while (it.hasNext()) {
				tempResultObj obj = it.next();
				String formattedResult = obj.getFormattedResult();

				int myPixel = ConnectorUtil.getPixel(formattedResult).x;
				if (myPixel > limitPixel) { // need to ellipsize this line
					formattedResult = toEllipsize(formattedResult, limitPixel);

				}

				list.add(formattedResult);
			}

		}
		return list;
	}

	private String toEllipsize(String s, int limitPixel) {
		String ellipsizedString = new String(s);
		int myPixel = ConnectorUtil.getPixel(ellipsizedString).x;
		int averageCharWidth = myPixel / ellipsizedString.length();
		// get appoximate number of characters the limitPixel could accommodate
		int numOfChar = limitPixel / averageCharWidth;
		ellipsizedString = ellipsizedString.substring(0, Math.min(ellipsizedString.length(), numOfChar) - 4) + "..."; //$NON-NLS-1$
		boolean toChop = true;
		while (toChop) {
			if (ConnectorUtil.getPixel(ellipsizedString).x >= limitPixel) {
				// filter out character
				ellipsizedString = ellipsizedString.substring(0, ellipsizedString.length() - 4) + "..."; //$NON-NLS-1$
			} else {
				if (ConnectorUtil.getPixel(s.substring(0, ellipsizedString.length() - 2) + "...").x >= limitPixel) //$NON-NLS-1$
				{
					toChop = false;
				} else {
					// add character
					ellipsizedString = s.substring(0, ellipsizedString.length() - 2) + "..."; //$NON-NLS-1$
				}

			}

		}

		return ellipsizedString;
	}

	public void setCurrSelectIndex(int i) {
		_currSelectIndex = i;
	}

	public int getCurrSelectIndex() {
		return _currSelectIndex;
	}

	public String getCurrId(int i) {
		String id = null;
		if (getResultList() != null && getResultList().size() > i) {
			id = getResultList().get(i).getId();
		}
		return id;
	}

	public SugarType getCurrSugarType(int i) {
		SugarType type = null;
		if (getResultList() != null && getResultList().size() > i) {
			type = getResultList().get(i).getSugarType();
		}
		return type;
	}

	public String getCurrType(int i) {
		String type = null;
		if (getCurrSugarType(i) != null) {
			type = getCurrSugarType(i).getParentType();
		}
		return type;
	}

	public String getCurrFormattedResult(int i) {
		String formattedResult = null;
		if (getResultList() != null && getResultList().size() > i) {
			formattedResult = getResultList().get(i).getFormattedResult();
		}
		return formattedResult;
	}

	public String getCurrAssociatedResult(int i) {
		String associatedResult = null;
		if (getResultList() != null && getResultList().size() > i) {
			associatedResult = getResultList().get(i).getAssociatedResult();
		}
		return associatedResult;
	}

	public String getCurrExtendedResult(int i) {
		String extendedResult = null;
		if (getResultList() != null && getResultList().size() > i) {
			extendedResult = getResultList().get(i).getExtendedResult();
		}
		return extendedResult;
	}

	public List<tempResultObj> getResultList() {

		// use TreeSet to keep entries in sorted order. When adding a tempResultObj, tempResultObj.compareTo() method will
		// be called to determine this entry order.
		Set<tempResultObj> set = new TreeSet<tempResultObj>();

		List<tempResultObj> list = new ArrayList<tempResultObj>();

		if (_resultMap != null && !_resultMap.isEmpty()) {
			// check if number of any client entries beyond max.
			Iterator<Entry<String, Map<String, tempResultObj>>> it1 = _resultMap.entrySet().iterator();
			while (it1.hasNext()) {
				Entry<String, Map<String, tempResultObj>> entry = it1.next();
				String keyName = entry.getKey();
				Map<String, tempResultObj> results = entry.getValue();
				int count = getItemCount(results);

				Object[] obj = results.values().toArray();
				if (((tempResultObj) obj[0]).getSugarType().equals(SugarType.ACCOUNTS) && count > MAX_ACCOUNT_RESULTLIMIT) {
					if (keyName != null) {
						AccountRedirectObj accountRedirectObj = new AccountRedirectObj(keyName, count);
						set.add(new tempResultObj(((tempResultObj) obj[0]).getId(), ((tempResultObj) obj[0]).getSugarType(), accountRedirectObj.getText(), accountRedirectObj.getText(),
								accountRedirectObj.getText()));

						computeMaxLineWidth(accountRedirectObj.getText());
					}
				} else {

					Iterator<tempResultObj> itObj = results.values().iterator();
					while (itObj.hasNext()) {
						tempResultObj tempResultObj = itObj.next();
						// skip toskip entries
						if (!tempResultObj.toSkip()) {
							set.add(tempResultObj);
							if (!((tempResultObj) obj[0]).getSugarType().equals(SugarType.CONTACTS)) {
								computeMaxLineWidth(tempResultObj.getFormattedResult());
							}
						}
					}

				}
			}

		}
		list.addAll(set);
		return list;

	}

	private void computeMaxLineWidth(String s) {
		if (Display.getDefault() != null && s != null) {
			GC gc = new GC(Display.getDefault());
			Point size = gc.textExtent(s); // or textExtent
			_maxLineWidth = Math.max(_maxLineWidth, size.x);
			gc.dispose();
		}
	}

	private int getItemCount(Map<String, tempResultObj> results) {
		int count = 0;
		if (results != null && !results.isEmpty()) {
			Iterator<tempResultObj> it = results.values().iterator();
			while (it.hasNext()) {
				tempResultObj obj = it.next();
				if (!obj.toSkip) {
					count++;
				}
			}
		}
		return count;
	}

	public void update(String id, boolean toSkip) {
		if (_resultMap != null) {
			Iterator<Map<String, tempResultObj>> itResults = _resultMap.values().iterator();
			while (itResults.hasNext()) {
				Map<String, tempResultObj> results = itResults.next();
				if (results.containsKey(id)) {
					results.get(id).toSkip(toSkip);
				}
			}
		}
	}

	private boolean isAssociated(SugarType type, String id) {
		boolean isAssociated = false;

		if (getCurrAssociateDataMap() != null && getCurrAssociateDataMap().getMyMap().containsKey(getCurrAssociateDataMap().getWeightedType(type.getParentType(), true))) {
			List<AssociateData> associateDataList = getCurrAssociateDataMap().getMyMap().get(getCurrAssociateDataMap().getWeightedType(type.getParentType(), true));
			for (int i = 0; i < associateDataList.size(); i++) {
				if (associateDataList.get(i).getId() != null && associateDataList.get(i).getId().equalsIgnoreCase(id)) {
					isAssociated = true;
					break;
				}
			}
		}

		return isAssociated;

	}

	private AssociateDataMap getCurrAssociateDataMap() {
		return _associateDataMap;
	}

	public boolean isAnySugarItems() {
		boolean isAnySugarItems = false;

		if (getResultList() != null && !getResultList().isEmpty()) {
			isAnySugarItems = true;
		}
		return isAnySugarItems;
	}

	private class tempResultObj implements Comparator<tempResultObj>, Comparable<tempResultObj> {
		private String id = null;
		private SugarType type = null;
		private String formattedResult = null;
		private String associatedResult = null;
		private String extendedResult = null;
		private boolean toSkip = false;

		tempResultObj(String i, SugarType st, String result, String associated, String extended) {
			id = i;
			type = st;
			formattedResult = result;
			associatedResult = associated;
			extendedResult = extended;
		}

		String getId() {
			return id;
		}

		SugarType getSugarType() {
			return type;
		}

		String getFormattedResult() {
			return formattedResult;
		}

		String getAssociatedResult() {
			return associatedResult;
		}

		String getExtendedResult() {
			return extendedResult;
		}

		boolean toSkip() {
			return toSkip;
		}

		void toSkip(boolean b) {
			toSkip = b;
		}

		@Override
		public int compare(tempResultObj arg0, tempResultObj arg1) {
			// sort by sugar type ( oppty, contact, client) + formatted name (ignore case)

			int compare = -1;
			StringBuffer obj1sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
			StringBuffer obj2sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

			if (arg0 != null && arg1 != null) {
				obj1sb.append(convertSugarTypeToString(arg0.getSugarType())).append(arg0.getFormattedResult());
				obj2sb.append(convertSugarTypeToString(arg1.getSugarType())).append(arg1.getFormattedResult());
				compare = obj1sb.toString().compareToIgnoreCase(obj2sb.toString());
			}
			return compare;
		}

		private String convertSugarTypeToString(SugarType type) {
			String string = "1"; //$NON-NLS-1$
			if (type != null) {
				if (type.equals(SugarType.OPPORTUNITIES)) {
					string = "1"; //$NON-NLS-1$
				} else if (type.equals(SugarType.CONTACTS)) {
					string = "2"; //$NON-NLS-1$
				} else if (type.equals(SugarType.ACCOUNTS)) {
					string = "3"; //$NON-NLS-1$
				}
			}
			return string;

		}

		@Override
		public int compareTo(tempResultObj arg0) {
			return compare(this, arg0);
		}
	}

	private void printSugarDataCacheMap() {
		int i = -1;
		if (_sugarDataCacheMap != null && !_sugarDataCacheMap.isEmpty()) {
			Iterator<Entry<SugarType, Set<SugarEntrySurrogate>>> it = _sugarDataCacheMap.entrySet().iterator();
			while (it.hasNext()) {
				Entry<SugarType, Set<SugarEntrySurrogate>> entry = it.next();
				SugarType type = entry.getKey();
				Set<SugarEntrySurrogate> set = entry.getValue();
				Iterator<SugarEntrySurrogate> it1 = set.iterator();
				while (it1.hasNext()) {
					SugarEntrySurrogate sugarEntry = it1.next();
					System.out.println(++i + ". key=" + type.getDisplayName() + ", value (name=" + sugarEntry.getName() //$NON-NLS-1$ //$NON-NLS-2$
							+ ",tagid=" + sugarEntry.getTagId() + ",id=" + sugarEntry.getId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}

			}
		} else {
			System.out.println("sugarDataCacheMap is EMPTY"); //$NON-NLS-1$
		}
	}

	private void printAssigneeList() {
		if (_assigneeList != null && _assigneeList.length > 0) {
			for (int i = 0; i < _assigneeList.length; i++) {
				System.out.println(i + ". assignee: " + _assigneeList[i]); //$NON-NLS-1$
			}
		} else {
			System.out.println("assigneeList is empty"); //$NON-NLS-1$
		}

	}

	private void printTagMap(Map<String, Set<String>> tagMap) {
		if (tagMap != null && !tagMap.isEmpty()) {
			Iterator<Entry<String, Set<String>>> it = tagMap.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, Set<String>> entry = it.next();
				String key = entry.getKey();
				Set<String> setX = entry.getValue();
				Iterator<String> itString = setX.iterator();
				while (itString.hasNext()) {
					String s = itString.next();
					System.out.println("tagMap key:" + key + ", tag:" + s); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}

		} else {
			System.out.println("tagMap is null or EMPTY"); //$NON-NLS-1$
		}
	}

}
