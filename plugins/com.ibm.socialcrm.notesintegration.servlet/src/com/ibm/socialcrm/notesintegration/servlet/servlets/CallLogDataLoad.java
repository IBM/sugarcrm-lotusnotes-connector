package com.ibm.socialcrm.notesintegration.servlet.servlets;

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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.json.JSONString;
import org.apache.commons.lang.StringEscapeUtils;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.utils.SugarDashboardPreference;
import com.ibm.socialcrm.notesintegration.core.utils.WebServiceInfoDataShare;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.WebSecurityCodeProvider;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataHub;
import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataShare;
import com.ibm.socialcrm.notesintegration.utils.datahub.calllog.CallFormDataShare;
import com.ibm.socialcrm.notesintegration.utils.datahub.calllog.CurrentSugarEntryDataShare;

public class CallLogDataLoad extends HttpServlet {
	/**
	 *maybe rename to FormDataService
	 */
	private static final long serialVersionUID = 7532627434461877252L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String responseStr = "";
		String jsonPrefix = "{\"identifier\": \"key\",\"items\": [";
		String jsonSuffix = "]}";
		boolean sendingParticipantList = false;
		// System.out.println("In Dataload servlet " + request.getQueryString());
		HttpSession session = request.getSession(true);

		// check security code, return unauthorized if invalid
		boolean useSecurityCode = true;
		String securityCode = request.getParameter("securityCode");
		if (useSecurityCode && (securityCode == null || securityCode.equals(""))) {
			// must be first call from the page, so return not loaded yet but
			// burn the code to force it back through (the initial set must have
			// been before the page was ready and lost)
			// System.out.println("Returning not loaded yet 200");
			UtilsPlugin.getDefault().logInfoMessage("CallLogDataLoad: Returning not loaded yet and resetting security code.", CorePluginActivator.PLUGIN_ID);
			WebSecurityCodeProvider.getInstance().generateSecurityCode();
			response.setStatus(200);
			responseStr = jsonPrefix + "{ \"key\":\"notLoadedYet\", \"value\":\"Not Loaded Yet\", \"dataItemType\":\"notLoadedYet\" }";
			responseStr += jsonSuffix;
			OutputStream out = response.getOutputStream();
			out.write(responseStr.getBytes());
			out.flush();
			out.close();
			return;
		} else if (useSecurityCode && (!securityCode.equals(WebSecurityCodeProvider.getInstance().getSecurityCodeString()))) {
			// System.out.println("CallLogDataLoad: invalid security code.  expected: " + WebSecurityCodeProvider.getInstance().getSecurityCodeString());
			UtilsPlugin.getDefault().logInfoMessage("CallLogDataLoad: invalid security code.", CorePluginActivator.PLUGIN_ID);
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

		try {
			SFADataHub hub = SFADataHub.getInstance();
			CallFormDataShare callFormShare = (CallFormDataShare) hub.blockingGetDataShare(CallFormDataShare.SHARE_NAME, 3000);

			boolean hasCustomFields = false;
			String callFormLabel = "";
			if (callFormShare != null && callFormShare.isLoaded() && (SugarDashboardPreference.getInstance().getSugarDateFormatPreference() != null)) {
				if (callFormShare.getKeySet().contains("callFormLabel")) {
					callFormLabel = (String) callFormShare.get("callFormLabel");
					if (!callFormLabel.equals("lbl_call_information")) {
						// ignore the value that comes back if there is no form
						hasCustomFields = true;
					}
				}
			} else {
				// share is not ready yet, must still be loading so return a
				// truncated signal to try the load again
				responseStr = jsonPrefix + "{ \"key\":\"notLoadedYet\", \"value\":\"Not Loaded Yet\", \"dataItemType\":\"notLoadedYet\" }";
				responseStr += jsonSuffix;
				System.out.println("CallLogDataLoad - output:" + responseStr);
				UtilsPlugin.getDefault().logInfoMessage("CallLogDataLoad: response:" + responseStr, CorePluginActivator.PLUGIN_ID);
				response.setStatus(200);
				OutputStream out = response.getOutputStream();
				out.write(responseStr.getBytes());
				out.flush();
				out.close();
				return;
			}
			WebServiceInfoDataShare serviceInfoShare = (WebServiceInfoDataShare) hub.blockingGetDataShare(WebServiceInfoDataShare.SHARE_NAME, 5000);

			// add the labels for the static elements of the page.
			responseStr = jsonPrefix
					+ "{ \"key\":\"dateSubLabel\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_DATE_SUB))
					+ "\", \"dataItemType\":\"label\" },"
					+ "{ \"key\":\"durationLabel\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_DURATION))
					+ "\", \"dataItemType\":\"label\" },"
					+ "{ \"key\":\"statusLabel\", \"value\":\""
					+ "* "
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_STATUS))
					+ "\", \"dataItemType\":\"label\" },"
					+ "{ \"key\":\"dateLabel\", \"value\":\""
					+ "* "
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_DATE))
					+ "\", \"dataItemType\":\"label\" },"
					+ "{ \"key\":\"typeLabel\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_TYPE))
					+ "\", \"dataItemType\":\"label\" },"
					+ "{ \"key\":\"assignedLabel\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_ASSIGNED))
					+ "\", \"dataItemType\":\"label\" },"
					+ "{ \"key\":\"relatedToTitle\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_RELATED))
					+ "\", \"dataItemType\":\"label\" },"
					+ "{ \"key\":\"myItemsLabel\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_RELATED_MYITEMS))
					+ "\", \"dataItemType\":\"label\" },"
					+ "{ \"key\":\"relatedLabel\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_RELATED_CTG))
					+ "\", \"dataItemType\":\"label\" },"
					+ "{ \"key\":\"relatedCityLabel\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_RELATED_CITY))
					+ "\", \"dataItemType\":\"label\" },"
					+ "{ \"key\":\"relatedItemLabel\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_RELATED_ITEM))
					+ "\", \"dataItemType\":\"label\" },"
					+ "{ \"key\":\"summaryLabel\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_SUMMARY))
					+ "\", \"dataItemType\":\"label\" },"
					+ "{ \"key\":\"subjectLabel\", \"value\":\""
					+ "* "
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_SUBJECT))
					+ "\", \"dataItemType\":\"label\" },"
					// deal with option values for drop-downs
					// status
					+ "{ \"key\":\"status\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_PLANNED_MSG) + "|"
							+ UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_HELD_MSG) + "|"
							+ UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_NOTHELD_MSG))
					+ "\", \"dataItemType\":\"options\" },"
					// call_type
					+ "{ \"key\":\"call_type\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_FACE2FACE) + "|"
							+ UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_INBOUND) + "|"
							+ UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_OUTBOUND)) + "\", \"dataItemType\":\"options\" },"
					// related type
					+ "{ \"key\":\"parent_type\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_CLIENT) + "|"
							+ UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_OPPORTUNITY) + "|"
							+ UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_CONTACT) + "|"
							+ UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_LEAD)) + "\", \"dataItemType\":\"options\" }";

			// return the static messages that aren't in labels
			responseStr += ",{ \"key\":\"relatedTypeContactMsg\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_RELATED_TYPE_CONTACT_MSG)) + "\", \"dataItemType\":\"message\" }";
			responseStr += ",{ \"key\":\"relatedTypeCityMsg\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_RELATED_TYPE_CITY_MSG)) + "\", \"dataItemType\":\"message\" }";
			responseStr += ",{ \"key\":\"relatedTypeClientMsg\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_RELATED_TYPE_CLIENT_MSG)) + "\", \"dataItemType\":\"message\" }";
			responseStr += ",{ \"key\":\"relatedTypeOpptyMsg\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_RELATED_TYPE_OPPTY_MSG)) + "\", \"dataItemType\":\"message\" }";
			responseStr += ",{ \"key\":\"relatedTypeLeadMsg\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_RELATED_TYPE_LEAD_MSG)) + "\", \"dataItemType\":\"message\" }";
			responseStr += ",{ \"key\":\"successMsg\", \"value\":\"" + StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_SUCCESS_MSG))
					+ "\", \"dataItemType\":\"message\" }";
			responseStr += ",{ \"key\":\"failMsg\", \"value\":\"" + StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_FAIL_MSG))
					+ "\", \"dataItemType\":\"message\" }";
			responseStr += ",{ \"key\":\"requiredMsg\", \"value\":\"" + StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_REQUIRED_MSG))
					+ "\", \"dataItemType\":\"message\" }";
			responseStr += ",{ \"key\":\"relatedTypePleasePickMsg\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_RELATED_TYPE_PLEASEPICK_MSG))
					+ "\", \"dataItemType\":\"message\" }";
			responseStr += ",{ \"key\":\"relatedTypeNoMatchMsg\", \"value\":\""
					+ StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_RELATED_TYPE_NOMATCH_MSG)) + "\", \"dataItemType\":\"message\" }";
			responseStr += ",{ \"key\":\"saveButtonLabel\", \"value\":\"" + StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_SAVE_BTN))
					+ "\", \"dataItemType\":\"message\" }";
			responseStr += ",{ \"key\":\"resetButtonLabel\", \"value\":\"" + StringEscapeUtils.escapeJavaScript(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_RESET_BTN))
					+ "\", \"dataItemType\":\"message\" }";

			// add the help hover text
			responseStr += ",{ \"key\":\"helphover\", \"value\":\"" + UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_HELP_HOVER_TEXT)
					+ "\", \"dataItemType\":\"hovertext\" }";

			// add the date preference, so date widgets can display the proper
			// format
			String datePreference = SugarDashboardPreference.getInstance().getSugarDateFormatPreference();
			// convert to dojo format
			datePreference = datePreference.replace("Y", "yyyy");
			datePreference = datePreference.replace("m", "MM");
			datePreference = datePreference.replace("d", "dd");
			responseStr += ",{ \"key\":\"sugarDatePreference\", \"value\":\"" + StringEscapeUtils.escapeJavaScript(datePreference) + "\", \"dataItemType\":\"sugarDatePreference\" }";

			// System.out.println("custom form label=" + callFormLabel);
			if (hasCustomFields) {
				Set<String> fieldSet = callFormShare.getChildKeySet();

				// 88796 - The name of the "duration" custom field returned from the getCallForm API is changed in 3.2, from
				// "duration_hours" to "duration_minutes".
				// Because "duration_minutes" conflicts with the existing "stock element" "Duration" that was hard coded 
				// at the top of the call log form, so, adding "duration_minutes" to the stockElements list so the program will ignore
				// this custom field.
				//String stockElements[] = {"name", "status", "date_start", "call_type", "duration_hours", "assigned_user_name", "parent_name", "description"};
				String stockElements[] = {"name", "status", "date_start", "call_type", "duration_hours", "duration_minutes", "assigned_user_name", "parent_name", "description"};
				
				Set stockFieldNames = new HashSet(Arrays.asList(stockElements));

				ArrayList<SFADataShare> customFields = new ArrayList();
				for (String aKey : fieldSet) { // iterate over the top level
					// shares
					// System.out.println("key: " + aKey);
					SFADataShare child = callFormShare.getChildShare(aKey);
					Set<String> childFieldSet = child.getChildKeySet();
					// iterate over children
					for (String childKey : childFieldSet) { // iterate over the
						// child
						if (!stockFieldNames.contains(childKey)) {
							SFADataShare child2 = child.getChildShare(childKey);
							// must be custom
							// System.out.println("adding custom field:" +
							// childKey);
							customFields.add(child2);
						} else {
							// System.out.println("skipping " + childKey);
						}
					}

				}
				responseStr += ",{ \"key\":\"dynamicFieldsTitle\", \"value\":\"" + StringEscapeUtils.escapeJavaScript(callFormLabel) + "\", \"dataItemType\":\"label\" }";
				for (SFADataShare customField : customFields) {
					// handle each field
					String json = customFieldJSONStringFor(customField);
					if (!json.equals("")) {
						responseStr += "," + json;
					}
				}
			}

			// look for data showing we were invoked from the card
			SFADataShare shareDataFromCard = hub.getDataShare(CurrentSugarEntryDataShare.SHARE_NAME);
			boolean usingDataShare = false;
			if (shareDataFromCard != null && !shareDataFromCard.getKeySet().isEmpty()) {
				// found the data. loaded from a card, so set the related items
				BaseSugarEntry entry = (BaseSugarEntry) (shareDataFromCard.get(CurrentSugarEntryDataShare.CURRENT_SUGAR_ENTRY));
				// copy data
				// System.out.println("found a card datashare..." +
				// entry.getSugarType() + " " + entry.getId());
				SugarType cardType = entry.getSugarType();
				responseStr += ",{ \"key\":\"" + entry.getId() + "\", \"value\":\"" + StringEscapeUtils.escapeJavaScript(entry.getName())
						+ "\", \"dataItemType\":\"cardRelatedTo\", \"relatedModule\":\"" + cardType.getParentType() + "\"}";
				usingDataShare = true; // set this so we don't try and set the
				// typeahead using previous values below
				// clear the data share to return to normal operation
				// note: the field will be loaded from the typeahead service
				// with the short-circuit method. Leave the share intact
				// now and the typeahead service will clear it when done.
				// shareDataFromCard.clear();
			} else {
				// not loaded from a card, so restore previously entered values
				Map<String, String> fieldValues = (Map<String, String>) session.getAttribute("fieldValues");
				// hack a single entry to set the value for something simple
				// responseStr += ",{ \"key\":\"" +
				// StringEscapeUtils.escapeJavaScript("setvalue-description") +
				// "\", \"value\":\"" +
				// StringEscapeUtils.escapeJavaScript("foo") +
				// "\", \"dataItemType\":\"fieldValue\"}";
				if (fieldValues != null) {
					for (String aKey : fieldValues.keySet()) {
						String keyVal = fieldValues.get(aKey);
						if (!keyVal.equals("") && !aKey.equals("parent_type") && !aKey.equals("parent_id") && !aKey.equals("parentIdDisplayValue")) { // weed
							// out
							// empty
							// values
							// and
							// values
							// related
							// to
							// the
							// parent_id
							responseStr += ",{ \"key\":\"" + StringEscapeUtils.escapeJavaScript("setvalue-" + aKey) + "\", \"value\":\"" + StringEscapeUtils.escapeJavaScript(keyVal)
									+ "\", \"dataItemType\":\"fieldValue\"}";
							if (aKey.equals("participantsFilterData")) {
								if (!keyVal.equals("")) {
									// if the participants list isn't empty,
									// then remember it is being sent so we
									// don't send the user info later. that will
									// result in a dupe if we did
									sendingParticipantList = true;
								}
							}
						}

					}
					if (!usingDataShare) {
						// process the parent_id typeahead like we did using the
						// datashare above so the typeahead gets loaded
						String relatedId = fieldValues.get("parent_id");
						String relatedDisplayName = fieldValues.get("parentIdDisplayValue");
						String relatedModule = fieldValues.get("parent_type");
						if (relatedId != null && relatedDisplayName != null && relatedModule != null) { // don't check these
							// for "" so we will
							// remember the
							// related type even
							// if no item is
							// picked
							responseStr += ",{ \"key\":\"" + relatedId + "\", \"value\":\"" + StringEscapeUtils.escapeJavaScript(relatedDisplayName)
									+ "\", \"dataItemType\":\"repopulateRelatedTo\", \"relatedModule\":\"" + relatedModule + "\"}";
						}
					}
				}
			}
			if (!sendingParticipantList) {
				// fill in the user info
				// add the logged in user's name and cnum
				responseStr += ",{ \"key\":\"" + serviceInfoShare.get(WebServiceInfoDataShare.USER_CNUM) + "\", \"value\":\""
						+ StringEscapeUtils.escapeJavaScript((String) serviceInfoShare.get(WebServiceInfoDataShare.USER_FULL_NAME)) + "\", \"dataItemType\":\"userInfo\" }";
			}
			responseStr += jsonSuffix;
			// System.out.println("CallLogDataLoad: response:" + responseStr);
			UtilsPlugin.getDefault().logInfoMessage("CallLogDataLoad: response:" + responseStr, CorePluginActivator.PLUGIN_ID);
			response.setStatus(200);
			OutputStream out = response.getOutputStream();
			out.write(responseStr.getBytes());
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(500);
			UtilsPlugin.getDefault().logInfoMessage("CallLogDataLoad: error response:" + responseStr, CorePluginActivator.PLUGIN_ID);
			OutputStream out = response.getOutputStream();
			responseStr = jsonPrefix + jsonSuffix;
			out.write(responseStr.getBytes());
			out.flush();
			out.close();
		}

	}

	private String customFieldJSONStringFor(SFADataShare customField) {
		String jsonStr = "";
		jsonStr = "{ \"key\":\"" + StringEscapeUtils.escapeJavaScript(customField.getName()) + "\"";

		Set<String> keys = customField.getKeySet();
		for (String aKey : keys) {
			if (aKey.equals("label")) { // convert label to value to keep
				// consistent with other types
				jsonStr += ",\"value\":\"" + StringEscapeUtils.escapeJavaScript((String) customField.get(aKey)) + "\"";
			} else {
				jsonStr += ",\"" + StringEscapeUtils.escapeJavaScript(aKey) + "\":\"" + StringEscapeUtils.escapeJavaScript((String) customField.get(aKey)) + "\"";
			}

		}
		// add options
		SFADataShare opts = customField.getChildShare("options");
		if (opts != null) {
			jsonStr += "," + customFieldOptionsJSONStringFor(opts);
		}
		// add the dataItemType
		jsonStr += ",\"dataItemType\":\"customField\" }";

		return jsonStr;
	}

	private String customFieldOptionsJSONStringFor(SFADataShare customField) {
		String jsonStr = "";

		Set<String> keys = customField.getKeySet();
		int count = 0;
		boolean firstTime = true;
		// check for empty value and bubble it to the top as the default
		if (keys.contains("_empty_")) {
			jsonStr += "\"optionid0\":\"" + StringEscapeUtils.escapeJavaScript("_empty_") + "\",";
			jsonStr += "\"optionlabel0\":\"" + StringEscapeUtils.escapeJavaScript((String) customField.get("_empty_")) + "\"";
			firstTime = false;
			count++;
		}
		for (String aKey : keys) {
			if (!aKey.equals("_empty_")) { // empty was handled outside the loop, so skip it inside
				if (!firstTime) {
					jsonStr += ",";
				}
				String optKey = "optionid" + count;
				String optValue = "optionlabel" + count;
				jsonStr += "\"" + optKey + "\":\"" + StringEscapeUtils.escapeJavaScript(aKey) + "\",";
				String js = (String) customField.get(aKey);
				jsonStr += "\"" + optValue + "\":\"" + StringEscapeUtils.escapeJavaScript(js) + "\"";
				count++;
				firstTime = false;
			}
		}
		return jsonStr;
	}
}
