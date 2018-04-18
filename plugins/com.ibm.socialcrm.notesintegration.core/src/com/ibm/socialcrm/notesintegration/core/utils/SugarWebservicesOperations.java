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

import java.beans.PropertyChangeSupport;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

import com.ibm.rcp.managedsettings.ManagedSettingsScope;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.OpportunityOwner;
import com.ibm.socialcrm.notesintegration.core.RevenueLineItem;
import com.ibm.socialcrm.notesintegration.core.SugarAccount;
import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.core.SugarEntrySurrogate;
import com.ibm.socialcrm.notesintegration.core.SugarOpportunity;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataHub;

public class SugarWebservicesOperations {

	public static final String TLSV12 = "TLSv1.2"; //$NON-NLS-1$
	public static final String TLSV1 = "TLSv1"; //$NON-NLS-1$
	private String _sslprotocol = TLSV12;

	public static final int MAX_RETRY_COUNT = 1;

	public static PropertyChangeSupport propertyChangeSupport = null;
	/**
	 * Property used when web service is called but current credential is not valid. This notifies listener to bring up the credential prompt page.
	 */
	public static final String BRING_UP_CREDENTIAL_PROMPT = "bringUpCredentialPrompt"; //$NON-NLS-1$

	public static final String GETINFO13_RESULTTAG = "key"; //$NON-NLS-1$

	// SugarCRM preference page match content type
	public static final String SALESCONNECT_LIVETEXT_MATCH_ACCOUNT_PREF_KEY = "com.ibm.socialcrm.salesconnect.livetext.match.account"; //$NON-NLS-1$
	public static final String SALESCONNECT_LIVETEXT_MATCH_OPPTY_PREF_KEY = "com.ibm.socialcrm.salesconnect.livetext.match.oppty"; //$NON-NLS-1$
	public static final String SALESCONNECT_LIVETEXT_MATCH_CONTACT_PREF_KEY = "com.ibm.socialcrm.salesconnect.livetext.match.contact"; //$NON-NLS-1$
	public static final String LIVETEXT_PREFERENCE_ACCOUNT_CONTENT_TYPE = "___enabled.SugarAccountType"; //$NON-NLS-1$
	public static final String LIVETEXT_PREFERENCE_OPPTY_CONTENT_TYPE = "___enabled.SugarOpportunityType"; //$NON-NLS-1$
	public static final String LIVETEXT_PREFERENCE_CONTACT_CONTENT_TYPE = "___enabled.SugarContactType"; //$NON-NLS-1$

	// A cache of all entries in Sugar that we pulled information for.
	private List<BaseSugarEntry> sugarEntries;

	// A cache of live text matches to BaseSugarEntry IDs that can be found in sugarEntries.
	// This map exists to help us locate items by the live text name which is not the actual
	// name of the entity that we are looking for. For example "Sam Daryn" is a live text item,
	// but the actual name in the database is "Samantha Daryn".
	private Map<String, String> liveTextMatchesCache;

	private static SugarWebservicesOperations instance;

	private Set<SugarEntrySurrogate> favorites;

	/**
	 * The session id from the last valid login session
	 */
	private String lastSession = ConstantStrings.EMPTY_STRING;

	/**
	 * The CNUM of the user that we are logged into sugar as. The CNUM is a combination of the users serial number and country code. It will be used as the primary key in the Sugar users table.
	 */
	private String userCNUM = ConstantStrings.EMPTY_STRING;

	/**
	 * The full name of the user we are logged into sugar as
	 */
	private String userFullName = ConstantStrings.EMPTY_STRING;

	/**
	 * An hint that probably means the user id/pw combination used to login into the CRM server was wrong.
	 */
	private boolean unableToLogin = false;

	private static Long currS;

	/**
	 * If this is true, it probably (but not necessarily) means we have a bad server address or the server is down.
	 */
	private boolean serverIncorrect = false;

	/**
	 * An hint that there's a connection problem, or maybe firewall, proxy, certificate problem. Should try the connection again later or contact system adm.
	 */
	private boolean unableToConnect = false;

	/*
	 * Flag to indicate if we should bring up the credential prompt dialog if invalid credential is encountered. Usually it is set to true, but, if we are currently in the Preference page, set it to
	 * false because user does not need the credential prompt dialog, he can modify credential data directly here.
	 */
	private boolean toPromptCredential = true;

	/*
	 * Indicating if the credential prompt dialog is currently open or not.
	 */
	private boolean isCredentialPromptOpen = false;

	/*
	 * If this Sugar Entry is not mine - for example when user tried to open a entry in the recent view that is not a my item.
	 */
	private boolean isInvalidSugarEntry = false;

	private IPreferenceChangeListener preferenceChangeListener = null;

	private String activityStreamWidgetName = null;

	private String connectionsURL = null;

	private SugarWebservicesOperations() {
	}

	public static SugarWebservicesOperations getInstance() {
		if (instance == null) {
			instance = new SugarWebservicesOperations();
		}
		return instance;
	}

	public enum WebserviceCallType {
		GET_INFO13, GET_ENTRY_LIST, GET_SAMETIME_INFO, GET_SUGAR_INFO, GET_FAVORITES, SET_FAVORITE, UNSET_FAVORITE, GET_SESSION, GET_TYPEAHEAD_INFO, CREATE_CALL_LOG, GET_CALL_FORM, SFA_REST
	}

	public enum GetInfo13RestulType {
		BASECARD, FOLLOWED, OPPTIES, RLIS
	}

	/**
	 * Calls the typeahead service to search a specific module type (contact, oppty, account) for the given search string.
	 * 
	 * @param module
	 * @param searchString
	 * @return
	 */
	public String getTypeaheadInfoFromWebservice(String module, String searchString) {
		return getTypeaheadInfoFromWebservice(module, searchString, null, null, null);
	}

	public String getTypeaheadInfoFromWebservice(String module, String searchString, String resultLimit, String isMyItems, String filter) {
		String result = "";

		if (module != null && !module.equals(ConstantStrings.EMPTY_STRING) && searchString != null && !searchString.equals(ConstantStrings.EMPTY_STRING)) {
			// If invalid credential, short circuit the intended web service
			if (unableToLogin()) {
				if (toPromptCredential()) {
					getPropertyChangeSupport().firePropertyChange(BRING_UP_CREDENTIAL_PROMPT, true, false);
				}
				return result;
			}
			try {
				result = SugarV10APIManager.getInstance().getTypeaheadInfoFromWebservice(module, searchString, resultLimit, isMyItems, filter);
			} catch (LoginException e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				unableToLogin = true;
				if (toPromptCredential()) {
					getPropertyChangeSupport().firePropertyChange(BRING_UP_CREDENTIAL_PROMPT, true, false);
				}
			}

		}
		return result;
	}

	/**
	 * Searches the typeahead service for all entries that match the given search string.
	 * 
	 * @param searchString
	 * @return
	 */
	// TODO: update this method to work with the new v10 elastic typeaheads
	public String getGlobalTypeaheadInfoFromWebservice(String searchString) {
		Map<String, String> parameters = null;
		if (searchString != null && !searchString.equals(ConstantStrings.EMPTY_STRING)) {
			try {
				parameters = new HashMap<String, String>();
				parameters.put("method", "getTypeAheadResultsCollected11"); //$NON-NLS-1$ //$NON-NLS-2$

				// Java's JSON object doens't preserver the order of the fields which is important to the webservice. So we
				// have to manually construct the JSON.
				String arguments = "{\"searchString\":\"" + searchString + "\",\"resultLimit\":\"300\",\"myItems\":\"true\"}"; //$NON-NLS-1$ //$NON-NLS-2$
				parameters.put("arguments", arguments); //$NON-NLS-1$
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
		return callWebservice(WebserviceCallType.SFA_REST, parameters, MAX_RETRY_COUNT);

	}

	/**
	 * Return the contact regular expressions for this user
	 * 
	 * @return
	 */
	public String getContactRegexes() {
		/*
		 * Map<String, String> parameters = new HashMap<String, String>(); parameters.put("method", "getRegex"); //$NON-NLS-1$ //$NON-NLS-2$ parameters.put("arguments", "{\"module\":\"Contacts\"}");
		 * //$NON-NLS-1$ //$NON-NLS-2$
		 * 
		 * return callWebservice(WebserviceCallType.SFA_REST, parameters, MAX_RETRY_COUNT);
		 */
		return getAccountOrContactRegexes("Contacts");
	}
	/**
	 * Return the account regular expressions for this user
	 * 
	 * @return
	 */
	public String getAccountRegexes() {
		return getAccountOrContactRegexes("Accounts");
	}
	/**
	 * Return the account regular expressions for this user
	 * 
	 * @return
	 */
	public String getAccountOrContactRegexes(String module) {
		/*
		 * Map<String, String> parameters = new HashMap<String, String>(); parameters.put("method", "getRegex"); //$NON-NLS-1$ //$NON-NLS-2$ parameters.put("arguments", "{\"module\":\"Accounts\"}");
		 * //$NON-NLS-1$ //$NON-NLS-2$
		 * 
		 * return callWebservice(WebserviceCallType.SFA_REST, parameters, MAX_RETRY_COUNT);
		 */
		String result = "";
		if (unableToLogin()) {
			if (toPromptCredential()) {
				getPropertyChangeSupport().firePropertyChange(BRING_UP_CREDENTIAL_PROMPT, true, false);
			}
			return result;
		}
		try {
			SugarV10RegexHelper helper = new SugarV10RegexHelper();
			result = helper.getV4Regex(module);
		} catch (LoginException e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			unableToLogin = true;
			if (toPromptCredential()) {
				getPropertyChangeSupport().firePropertyChange(BRING_UP_CREDENTIAL_PROMPT, true, false);
			}
		}
		return result;
	}

	/**
	 * Return the oppty regular expressions for this user
	 * 
	 * @return
	 */
	public String getOpptyRegexes() {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("method", "getRegex"); //$NON-NLS-1$ //$NON-NLS-2$
		parameters.put("arguments", "{\"module\":\"Opportunities\"}"); //$NON-NLS-1$ //$NON-NLS-2$

		return callWebservice(WebserviceCallType.SFA_REST, parameters, MAX_RETRY_COUNT);
	}

	/**
	 * Returns the URL of the connections server.
	 * 
	 * @return
	 */
	public String getConnectionsURL() {
		if (connectionsURL == null) {
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("method", "getConnectionsURL"); //$NON-NLS-1$ //$NON-NLS-2$

			connectionsURL = callWebservice(WebserviceCallType.SFA_REST, parameters, MAX_RETRY_COUNT, false);
			if (connectionsURL != null && connectionsURL.trim().length() > 0) {
				connectionsURL = connectionsURL.replaceAll("\\\\", ConstantStrings.EMPTY_STRING); //$NON-NLS-1$
				connectionsURL = connectionsURL.replaceAll("\"", ConstantStrings.EMPTY_STRING); //$NON-NLS-1$
			}
		}
		return connectionsURL;
	}

	/**
	 * Calls rest.php to get the base information for an account along with the contact data for that account
	 * 
	 * @param accountId
	 * @return
	 */
	public String getAccountWithContactData(String accountId) {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("method", "getContactsForAccountCollected"); //$NON-NLS-1$ //$NON-NLS-2$		
		String arguments = "{\"accountid\":\"" + accountId + "\"}"; //$NON-NLS-1$ //$NON-NLS-2$
		parameters.put("arguments", arguments); //$NON-NLS-1$
		return callWebservice(WebserviceCallType.SFA_REST, parameters, MAX_RETRY_COUNT);
	}

	// example: "module_name":"Contacts","query":"contacts.id in
	// (SELECT eabr.bean_id FROM email_addr_bean_rel eabr JOIN email_addresses ea ON (ea.id = eabr.email_address_id)
	// WHERE eabr.deleted=0 and (ea.email_address='stick_cinnamon@st.com' or ea.email_address='stick_cinnamon@st.com' ))","select_fields":""}
	public String buildModuleEmailArguments(List<String> idList, String moduleName, String columnName) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		boolean isFirst = true;
		if (idList != null && idList.size() > 0) {
			sb.append("{\"module_name\":\"").append(moduleName).append("\",\"query\":\"").append(columnName); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" in (SELECT eabr.bean_id FROM email_addr_bean_rel eabr JOIN email_addresses ea ON (ea.id = eabr.email_address_id)"); //$NON-NLS-1$
			sb.append(" WHERE eabr.deleted=0 and ("); //$NON-NLS-1$
			for (String id : idList) {
				if (isFirst) {
					isFirst = false;
				} else {
					sb.append(" or "); //$NON-NLS-1$
				}
				sb.append("ea.email_address='").append(id).append("'"); //$NON-NLS-1$  //$NON-NLS-2$
			}
			sb.append("))\",\"select_fields\":\"\"}"); //$NON-NLS-1$
		}
		return sb.toString();
	}

	/**
	 * Returns all of the activity data associated with an account/a contact/an oopportunity. Activities are defined as "emails, meetings, and calls"
	 * 
	 * @param accountId
	 * @return
	 */
	public String getActivitiesData(SugarType type, String id) {
		String out = null;

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("method", "getRelatedActivities"); //$NON-NLS-1$ //$NON-NLS-2$    

		String module = "account"; //$NON-NLS-1$
		if (type != null && id != null) {
			if (type.equals(SugarType.CONTACTS)) {
				module = "contact"; //$NON-NLS-1$
			} else if (type.equals(SugarType.OPPORTUNITIES)) {
				module = "opportunity"; //$NON-NLS-1$
			}
			String arguments = "{\"input\":[[\"" + module + "\",\"" + id + "\"]]}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			parameters.put("arguments", arguments); //$NON-NLS-1$
			out = callWebservice(WebserviceCallType.SFA_REST, parameters, MAX_RETRY_COUNT);
		}
		return out;
	}

	/**
	 * Gets the association information for a given document
	 * 
	 */
	public String getDocumentRelationships(String idType, List<String> connectionsUUIDs) {
		String out = null;
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("method", "getDocumentRelationships"); //$NON-NLS-1$ //$NON-NLS-2$
		String arguments = "{ \"input\":["; //$NON-NLS-1$ 
		for (String uuid : connectionsUUIDs) {
			arguments += "[ \"" + idType + "\",\"" + uuid + "\"],"; //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$
		}
		arguments = arguments.substring(0, arguments.length() - 1); // Strip off the trailing comma
		arguments += "] }"; //$NON-NLS-1$ 

		parameters.put("arguments", arguments); //$NON-NLS-1$
		out = callWebservice(WebserviceCallType.SFA_REST, parameters, MAX_RETRY_COUNT);
		return out;
	}

	/**
	 * Gets the summary data for all of the recently viewed cards
	 * 
	 * @param idMap
	 *        - Map of sugar id to type
	 * @return
	 */
	public String getRecentlyViewedCards(Map<String, String> idMap) {
		String out = null;

		Map<String, String> typeArgumentMap = new HashMap<String, String>() {
			{
				put(SugarType.ACCOUNTS.toString(), "accountid"); //$NON-NLS-1$
				put(SugarType.CONTACTS.toString(), "contactid"); //$NON-NLS-1$
				put(SugarType.OPPORTUNITIES.toString(), "opptyid"); //$NON-NLS-1$
			}
		};

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("method", "getInfo13"); //$NON-NLS-1$ //$NON-NLS-2$
		String arguments = "{ \"input\":["; //$NON-NLS-1$ 
		for (String id : idMap.keySet()) {
			arguments += "[ \"" + typeArgumentMap.get(idMap.get(id)) + "\", \"" + id + "\", \"recentlist\", \"" + id //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$
					+ "\" ],"; //$NON-NLS-1$			
		}
		arguments = arguments.substring(0, arguments.length() - 1); // Strip off the trailing comma
		arguments += "] }"; //$NON-NLS-1$ 

		parameters.put("arguments", arguments); //$NON-NLS-1$
		out = callWebservice(WebserviceCallType.SFA_REST, parameters, MAX_RETRY_COUNT);
		return out;
	}

	/**
	 * Returns all of the documents associated with an account/an oopportunity.
	 * 
	 * @param accountId
	 *        / oppty id
	 * @return
	 */
	public String getDocumentData(SugarType type, String id) {
		String out = null;

		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("method", "ibm_get_entry_list2"); //$NON-NLS-1$ //$NON-NLS-2$    

		String tableX = "accounts"; //$NON-NLS-1$
		if (type != null && id != null) {
			if (type.equals(SugarType.CONTACTS)) {
				tableX = "contacts"; //$NON-NLS-1$
			} else if (type.equals(SugarType.OPPORTUNITIES)) {
				tableX = "opportunities"; //$NON-NLS-1$
			}
			// For relationship, fields emails and meetings are correct... not sure about notes and calls...
			String arguments = "{\"run_as\":\"\",\"module_name\":\"" + type.getParentType() + "\",\"query\":\"" + tableX + ".id='" + id + "' \"," + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					"\"order_by\":\"\",\"offset\": \"\",\"select_fields\":\"\", \"related_records\": [\"documents\"]}"; //$NON-NLS-1$

			parameters.put("arguments", arguments); //$NON-NLS-1$
			out = callWebservice(WebserviceCallType.SFA_REST, parameters, MAX_RETRY_COUNT);
		}
		return out;
	}

	public String validateDownload(String docid) {

		Map<String, String> parameters = null;
		if (docid != null && !docid.equals(ConstantStrings.EMPTY_STRING)) {
			try {
				parameters = new HashMap<String, String>();
				parameters.put("method", "cwValidateDownload"); //$NON-NLS-1$ //$NON-NLS-2$

				// Java's JSON object doens't preserver the order of the fields which is important to the webservice. So we
				// have to manually construct the JSON.
				String arguments = "{\"documentId\":\"" + docid + "\"}"; //$NON-NLS-1$ //$NON-NLS-2$
				parameters.put("arguments", arguments); //$NON-NLS-1$
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
		return callWebservice(WebserviceCallType.SFA_REST, parameters, MAX_RETRY_COUNT);

	}

	/**
	 * get_entry_list of all user ids.
	 * 
	 * @param ID
	 *        List
	 * @return
	 */
	public String getUserEmails(List idList) {
		String out = null;
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

		if (idList != null && !idList.isEmpty()) {
			sb.append("{\"module_name\":\"Users\",\"query\":\""); //$NON-NLS-1$
			boolean isFirst = true;
			Iterator it = idList.iterator();
			while (it.hasNext()) {
				String id = (String) it.next();
				if (!isFirst) {
					sb.append(" or "); //$NON-NLS-1$
				} else {
					isFirst = false;
				}
				sb.append("users.id='").append(id).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			sb.append("\",\"select_fields\":\"\"}"); //$NON-NLS-1$

			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("method", "get_entry_list"); //$NON-NLS-1$ //$NON-NLS-2$    

			parameters.put("arguments", sb.toString()); //$NON-NLS-1$
			out = callWebservice(WebserviceCallType.GET_ENTRY_LIST, parameters, MAX_RETRY_COUNT);

		}
		return out;
	}

	/**
	 * Calls a native sugar web service (get_entry, set_entry, whatever) - for Restful request. This method uses multipart/form-data content type in order to handle some special characters in the
	 * request.
	 * 
	 * @return
	 */
	public String callNativeSugarRestWithMultipart(String method, String arguments) {
		String output = null;
		try {
			HttpClient client = getProxiedHttpClient();
			PostMethod post = new PostMethod(NotesAccountManager.getInstance().getSFARestServiceURL());

			Part[] parts = {new StringPart("userid", NotesAccountManager.getInstance().getCRMUser(), "UTF-8"), //$NON-NLS-1$ //$NON-NLS-2$
					new StringPart("password", NotesAccountManager.getInstance().getCRMPassword(), "UTF-8"), //$NON-NLS-1$ //$NON-NLS-2$
					new StringPart("method", method), //$NON-NLS-1$ 
					new StringPart("arguments", arguments, "UTF-8")}; //$NON-NLS-1$ //$NON-NLS-2$

			post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));

			doBeginningLog("sfaRest.php", method); //$NON-NLS-1$ 

			client.executeMethod(post);
			output = post.getResponseBodyAsString();
			UtilsPlugin.getDefault().logInfoMessage(WebServiceLogUtil.getDebugMsg(post, parts, output, post.getURI().toString(), 0, 0), CorePluginActivator.PLUGIN_ID);
			// System.out.println(WebServiceLogUtil.getDebugMsg(post, parts, output, post.getURI().toString(), 0, 0));
			doEndingLog("sfaRest.php", method); //$NON-NLS-1$ 

			JSONObject jsonObject = null;
			try {
				jsonObject = new JSONObject(output);
				String sessionId = jsonObject.getJSONObject("header").getString("sessionid"); //$NON-NLS-1$ //$NON-NLS-2$
				if (sessionId != null && sessionId.length() > 0) {
					lastSession = sessionId;
				}
			} catch (JSONException e) {
				// Eat it
			}

			post.releaseConnection();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}

		return output;
	}

	/**
	 * Tell sugar to follow or unfollow a certain record
	 * 
	 * @param module
	 * @param id
	 * @param action
	 * @return
	 */
	public String callFollowService(String module, String id, boolean action) {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("method", action ? "cwFollow20" : "cwUnfollow20"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// Java's JSON object doens't preserver the order of the fields which is important to the webservice. So we
		// have to manually construct the JSON.
		String arguments = "{\"moduleType\":\"" + module + "\",\"id\":" + "\"" + id + "\"" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
				+ "}"; //$NON-NLS-1$

		parameters.put("arguments", arguments); //$NON-NLS-1$

		return callWebservice(WebserviceCallType.SFA_REST, parameters);
	}

	public String createCallLogFromWebservice(Map<String, String> entries) {
		Map<String, String> parameters = null;
		parameters = new HashMap<String, String>();
		parameters.put("method", "saveCallMultiAssociate"); //$NON-NLS-1$ //$NON-NLS-2$
		// this service wants a single json object with the keys/values at the http param "callParameters"
		JSONObject json = new JSONObject(entries);
		String paramString = "{\"arguments\":" + json.toString() + " }"; //$NON-NLS-1$ //$NON-NLS-2$
		parameters.put("arguments", paramString/* json.toString() */); //$NON-NLS-1$

		return callWebservice(WebserviceCallType.SFA_REST, parameters);
	}

	/**
	 * Get the activity stream filter for a given object
	 * 
	 * @param module
	 * @param id
	 * @return
	 */
	public String callGetEventsFilter(String module, String id) {
		Map<String, String> parameters = null;
		parameters = new HashMap<String, String>();
		parameters.put("method", "cwGetEventsFilter"); //$NON-NLS-1$ //$NON-NLS-2$

		String arguments = "{\"moduleType\":\"" + module + "\",\"id\":" + "\"" + id + "\"" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
				+ "}"; //$NON-NLS-1$
		parameters.put("arguments", arguments); //$NON-NLS-1$

		return callWebservice(WebserviceCallType.SFA_REST, parameters);
	}

	/**
	 * Get the microblog filter for a given object
	 * 
	 * @param module
	 * @param id
	 * @return
	 */
	public String callGetMicroblogFilter(String module, String id) {
		Map<String, String> parameters = null;
		parameters = new HashMap<String, String>();
		parameters.put("method", "cwGetMicroblogFilter"); //$NON-NLS-1$ //$NON-NLS-2$

		String arguments = "{\"moduleType\":\"" + module + "\",\"id\":" + "\"" + id + "\"" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
				+ "}"; //$NON-NLS-1$
		parameters.put("arguments", arguments); //$NON-NLS-1$

		return callWebservice(WebserviceCallType.SFA_REST, parameters);
	}

	/**
	 * Posts a status update to the microblog for the specified id
	 * 
	 * @param module
	 * @param id
	 * @param status
	 * @return
	 */
	public String callPostStatusUpdate(String module, String id, String status) {
		Map<String, String> parameters = null;
		parameters = new HashMap<String, String>();
		parameters.put("method", "cwPostStatusUpdate"); //$NON-NLS-1$ //$NON-NLS-2$

		String arguments = "{\"moduleType\":\"" + module + "\",\"id\":" + "\"" + id + "\"" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
				+ "\"statusUpdate\":" + "\"" + status + "\"" + "}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		parameters.put("arguments", arguments); //$NON-NLS-1$

		return callWebservice(WebserviceCallType.SFA_REST, parameters);
	}

	/**
	 * Returns the name of the activity stream widget configured on this server. This lazy initializes the name of the widget so we don't keep asking the server. If you change the widget on the server
	 * (which would probably only happen in dev), restart your notes client.
	 * 
	 * @return
	 */
	public String getActivityStreamWidgetName() {
		if (activityStreamWidgetName == null) {
			try {
				final Map<String, String> parameters = new HashMap<String, String>();
				parameters.put("method", "getActivityStreamWidgetName"); //$NON-NLS-1$ //$NON-NLS-2$
				parameters.put("arguments", "{}"); //$NON-NLS-1$  //$NON-NLS-2$

				Job job = new Job("Getting widget name") { //$NON-NLS-1$

					@Override
					protected IStatus run(IProgressMonitor arg0) {
						try {
							String responseJSON = callWebservice(WebserviceCallType.SFA_REST, parameters);
							JSONObject responseObject = new JSONObject(responseJSON);
							activityStreamWidgetName = responseObject.getString("result"); //$NON-NLS-1$
						} catch (Exception e) {
							UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
						}
						return Status.OK_STATUS;
					}
				};

				job.schedule();
				job.join();

			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
		return activityStreamWidgetName;
	}

	public String getSessionFromWebservice() {
		return callWebservice(WebserviceCallType.GET_SESSION, null, MAX_RETRY_COUNT);
	}

	public String getSametimeInfoFromWebservice(List<String> accounts, List<String> opportunities, List<String> contacts) {
		return callWebservice(WebserviceCallType.GET_SAMETIME_INFO, buildSugarTypeParameters(accounts, opportunities, contacts));
	}

	public String getSugarInfoFromWebService(Map<String, String> additionalParam) {
		return callWebservice(WebserviceCallType.GET_ENTRY_LIST, additionalParam, 0);
	}

	// These are obsoleted methods which call the older version of getInfo API.
	// Commenting them out to see if we have switched to the new getInfo13 everywhere.
	// Eventually these methods should be removed.
	// public String getSugarInfoFromWebservice(List<String> accounts, List<String> opportunities, List<String> contacts) {
	// return callWebservice(WebserviceCallType.GET_SUGAR_INFO, buildSugarTypeParameters(accounts, opportunities, contacts), MAX_RETRY_COUNT);
	// }
	//
	//	
	//	
	//
	// public boolean loadSugarInfoFromWebservice(List<String> accounts, List<String> opportunities, List<String> contacts) {
	// boolean success = true;
	// String output = getSugarInfoFromWebservice(accounts, opportunities, contacts);
	// if (output != null) {
	// try {
	// processSugarEntryInfo(output);
	// } catch (Exception e) {
	// success = false;
	// UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
	// }
	// } else {
	// success = false;
	// }
	// return success;
	// }

	public boolean callSugarGetInfo13(SugarType sugarType, String id, GetInfo13RestulType resultType) {
		String resultTag = SugarWebservicesOperations.GETINFO13_RESULTTAG;
		String output = SugarWebservicesOperations.getInstance()
				.loadSugarInfo13FromWebservice(id, SugarWebservicesOperations.getInstance().getGetInfo13InputDataType(sugarType), resultType, resultTag);

		boolean success = false;
		if (output != null) {
			if (isInvalidSugarEntry(output)) {
				resetInvalidSugarEntry(true);
			} else {
				try {
					SugarWebservicesOperations.getInstance().processSugarEntryInfo13(sugarType, output, resultType, resultTag);
					success = true;
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
			}
		}
		return success;
	}

	private boolean isInvalidSugarEntry(String output) {
		boolean isInvalid = false;
		try {
			JSONObject outputJson = new JSONObject(output);
			String name = outputJson.getString("number"); //$NON-NLS-1$
			if (name.equals("-1")) //$NON-NLS-1$
			{
				isInvalid = true;
			}
		} catch (Exception e) {
			// eat it
		}
		return isInvalid;
	}

	public String loadSugarInfo13FromWebservice(String id, String inputDataType, GetInfo13RestulType resultType, String resultTag) {
		Map v10OutputMap = new HashMap<Boolean, String>();
		// flag indicating if it's been converted to v10 rest api
		boolean isConverted = false;

		// 81013
		// getInfo13() calls v10 api and re-constructs result to v4 api format. The returned map (v10OutputMap) contains 1 entry:
		// key is a boolean indicating if the v10 process is good, if it is, we can use the map value which should have the exact
		// format as the result from v4 api.
		// If the map key is false, ignore the map value, continue with the original v4 api process.
		// 
		String v4Output = null;
//		try {
//			v10OutputMap = SugarV10APIManager.getInstance().getInfo13(id, inputDataType, resultType, resultTag);
//		} catch (LoginException e) {
//			unableToLogin = true;
//			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
//		}
//		if (v10OutputMap != null) {
//			Iterator<Entry<Boolean, String>> it = v10OutputMap.entrySet().iterator();
//			Entry<Boolean, String> entry = it.next();
//			if (entry.getKey() != null && entry.getKey().booleanValue()) {
//				isConverted = true;
//				v4Output = entry.getValue();
//			}
//		}

		if (!isConverted) {
			v4Output = callWebservice(WebserviceCallType.GET_INFO13, buildGetInfo13Param(id, inputDataType, resultType, resultTag), MAX_RETRY_COUNT);

			if (isInvalidCredential(v4Output)) {
				unableToLogin = true;
			}
		}
		return v4Output;
	}
	private Map<String, String> buildGetInfo13Param(String id, String inputDataType, GetInfo13RestulType resultType, String resultTag) {
		Map<String, String> parameters = new HashMap<String, String>();
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

		if (id != null && inputDataType != null && resultType != null && resultTag != null) {
			String resultTypeX = null;
			switch (resultType) {
				case BASECARD :
					resultTypeX = "basecard"; //$NON-NLS-1$
					break;
				case FOLLOWED :
					resultTypeX = "followed"; //$NON-NLS-1$
					break;
				case OPPTIES :
					resultTypeX = "oppties"; //$NON-NLS-1$
					break;
				case RLIS :
					resultTypeX = "rlis"; //$NON-NLS-1$
					break;
			};

			sb.append("{\"input\":[[\"").append(inputDataType).append("\",\"").append(id).append("\",\"").append(resultTypeX).append("\",\"").append(resultTag).append("\"]]}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			parameters.put("method", "getInfo13"); //$NON-NLS-1$ //$NON-NLS-2$    
			parameters.put("arguments", sb.toString()); //$NON-NLS-1$
		}
		return parameters;
	}

	public String getGetInfo13InputDataType(SugarType sugarType) {
		String inputDataType = null;
		if (sugarType.equals(SugarType.ACCOUNTS)) {
			inputDataType = "accountid"; //$NON-NLS-1$
		} else if (sugarType.equals(SugarType.OPPORTUNITIES)) {
			inputDataType = "opptyid"; //$NON-NLS-1$
		} else if (sugarType.equals(SugarType.CONTACTS)) {
			inputDataType = "contactid"; //$NON-NLS-1$
		}
		return inputDataType;
	}

	public void processSugarEntryInfo13(SugarType sugarType, String output, GetInfo13RestulType resultType, String resultTag) {
		List<Map<String, Object>> sugarEntries = extractGetInfo13(output, resultTag);
		if (sugarEntries != null && !sugarEntries.isEmpty()) {
			try {
				JSONObject jsonObject = null;
				jsonObject = new JSONObject(output);
				JSONObject resultObj = jsonObject.getJSONObject("result").getJSONObject(resultTag); //$NON-NLS-1$
				Iterator<String> ids = resultObj.keys();
				for (Map<String, Object> entry : sugarEntries) {
					String id = ids.next();
					if (sugarType.equals(SugarType.ACCOUNTS)) {
						processSugarAccount(entry, id, resultType);
					} else if (sugarType.equals(SugarType.OPPORTUNITIES)) {
						processSugarOpportunities(entry, id, resultType);
					} else if (sugarType.equals(SugarType.CONTACTS)) {
						processSugarContact(entry, id, resultType);
					}
				}

			} catch (JSONException e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
	}

	private List<Map<String, Object>> extractGetInfo13(String output, String resultTag) {
		JSONObject jsonObject = null;
		List<Map<String, Object>> sugarEntries = new ArrayList<Map<String, Object>>();
		try {
			jsonObject = new JSONObject(output);
		} catch (JSONException e) {
			// End gracefully.
		}
		if (jsonObject != null) {
			try {
				if (jsonObject.containsKey("result") && jsonObject.getJSONObject("result").containsKey(resultTag)) { //$NON-NLS-1$ //$NON-NLS-2$
					JSONObject resultObj = jsonObject.getJSONObject("result").getJSONObject(resultTag); //$NON-NLS-1$
					Iterator<String> ids = resultObj.keys();

					while (ids.hasNext()) {
						String key = ids.next();
						if (resultObj.get(key) instanceof JSONObject) {
							JSONObject obj = resultObj.getJSONObject(key);
							sugarEntries.add(obj);
						}
					}
				}
			} catch (JSONException e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}

		}
		return sugarEntries;
	}

	// These are obsoleted methods which call the older version of getInfo API.
	// Commenting them out to see if we have switched to the new getInfo13 everywhere.
	// Eventually these methods should be removed.
	// public String getOpportunityInfoByNameFromWebservice(List<String> opportunities) {
	// Map<String, String> parameters = null;
	//
	// try {
	// int length = opportunities != null ? opportunities.size() : 0;
	// if (length > 0) {
	// int counter = 1;
	// parameters = new HashMap<String, String>();
	// String opportunityNameParameters = ConstantStrings.EMPTY_STRING;
	// for (String opportunity : opportunities) {
	// opportunityNameParameters += URLEncoder.encode(opportunity, ConstantStrings.UTF8) + (counter++ >= length ? ConstantStrings.EMPTY_STRING : ConstantStrings.COMMA);
	// }
	//				parameters.put("opptyname", opportunityNameParameters); //$NON-NLS-1$
	// }
	// } catch (UnsupportedEncodingException e1) {
	// UtilsPlugin.getDefault().logException(e1, CorePluginActivator.PLUGIN_ID);
	// }
	// return callWebservice(WebserviceCallType.GET_SUGAR_INFO, parameters);
	// }
	//
	// public String getContactInfoByNameFromWebservice(String name) {
	// Map<String, String> parameters = null;
	//
	// try {
	// if (name != null && !name.equals(ConstantStrings.EMPTY_STRING)) {
	// parameters = new HashMap<String, String>();
	//				parameters.put("contactname", URLEncoder.encode(name, ConstantStrings.UTF8)); //$NON-NLS-1$
	// }
	// } catch (UnsupportedEncodingException e1) {
	// UtilsPlugin.getDefault().logException(e1, CorePluginActivator.PLUGIN_ID);
	// }
	// return callWebservice(WebserviceCallType.GET_SUGAR_INFO, parameters);
	// }
	//
	// public String getContactInfoByEmailFromWebservice(String email) {
	// Map<String, String> parameters = null;
	// try {
	// if (email != null && !email.equals(ConstantStrings.EMPTY_STRING)) {
	// parameters = new HashMap<String, String>();
	//				parameters.put("contactemail", URLEncoder.encode(email, ConstantStrings.UTF8)); //$NON-NLS-1$
	// }
	// } catch (UnsupportedEncodingException e1) {
	// UtilsPlugin.getDefault().logException(e1, CorePluginActivator.PLUGIN_ID);
	// }
	// return callWebservice(WebserviceCallType.GET_SUGAR_INFO, parameters);
	// }
	// public String getAccountInfoByIdFromWebservice(String id) {
	// Map<String, String> parameters = null;
	// try {
	// if (id != null && !id.equals(ConstantStrings.EMPTY_STRING)) {
	// parameters = new HashMap<String, String>();
	//				parameters.put("clientid", URLEncoder.encode(id, ConstantStrings.UTF8).toUpperCase()); //$NON-NLS-1$
	// }
	// } catch (UnsupportedEncodingException e1) {
	// UtilsPlugin.getDefault().logException(e1, CorePluginActivator.PLUGIN_ID);
	// }
	// return callWebservice(WebserviceCallType.GET_SUGAR_INFO, parameters);
	// }

	public String getFavoritesFromWebservice() {
		return callWebservice(WebserviceCallType.GET_FAVORITES, new HashMap<String, String>() {
			{
				put("favaction", "get"); //$NON-NLS-1$,  //$NON-NLS-2$
			}
		});
	}

	public String setFavoriteFromWebservice(final SugarType sugarType, final String name) {
		String output = null;
		try {
			output = callWebservice(WebserviceCallType.SET_FAVORITE, new HashMap<String, String>() {
				{
					put("favaction", "setbyname"); //$NON-NLS-1$,  //$NON-NLS-2$
					put("recordtype", GenericUtils.getDatabaseModuleName(sugarType)); //$NON-NLS-1$
					put("recordid", URLEncoder.encode(name, ConstantStrings.UTF8)); //$NON-NLS-1$
				}
			});
		} catch (UnsupportedEncodingException e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}
		return output;
	}

	public String unsetFavorite(final String favoriteId) {
		return callWebservice(WebserviceCallType.GET_FAVORITES, new HashMap<String, String>() {
			{
				put("favaction", "unset"); //$NON-NLS-1$,  //$NON-NLS-2$
				put("favid", favoriteId); //$NON-NLS-1$,  
			}
		});
	}

	/**
	 * Returns the custom call logging form for the current user
	 * 
	 * @return
	 */
	public String getCallLogForm() {
		return callWebservice(WebserviceCallType.GET_CALL_FORM, new HashMap<String, String>());
	}

	private Map<String, String> buildSugarTypeParameters(List<String> accounts, List<String> opportunities, List<String> contacts) {
		HashMap<String, String> parameters = new HashMap<String, String>();
		try {
			int length = accounts != null ? accounts.size() : 0;
			if (length > 0) {
				int counter = 1;
				String accountsParameter = ConstantStrings.EMPTY_STRING;
				for (String account : accounts) {
					accountsParameter += URLEncoder.encode(account, ConstantStrings.UTF8) + (counter++ >= length ? ConstantStrings.EMPTY_STRING : ConstantStrings.COMMA);
				}
				parameters.put("accountid", accountsParameter); //$NON-NLS-1$
			}

			length = opportunities != null ? opportunities.size() : 0;
			if (length > 0) {
				int counter = 1;
				String opportunitiesParameter = ConstantStrings.EMPTY_STRING;
				for (String opportunity : opportunities) {
					opportunitiesParameter += URLEncoder.encode(opportunity, ConstantStrings.UTF8) + (counter++ >= length ? ConstantStrings.EMPTY_STRING : ConstantStrings.COMMA);
				}
				parameters.put("opptyid", opportunitiesParameter); //$NON-NLS-1$
			}

			length = contacts != null ? contacts.size() : 0;
			if (length > 0) {
				int counter = 1;
				String contactsParameter = ConstantStrings.EMPTY_STRING;
				for (String contact : contacts) {
					contactsParameter += URLEncoder.encode(contact, ConstantStrings.UTF8) + (counter++ >= length ? ConstantStrings.EMPTY_STRING : ConstantStrings.COMMA);
				}
				parameters.put("contactid", contactsParameter); //$NON-NLS-1$
			}
		} catch (UnsupportedEncodingException e1) {
			UtilsPlugin.getDefault().logException(e1, CorePluginActivator.PLUGIN_ID);
		}
		return parameters;
	}

	/**
	 * Maintains a map of location names to http proxies for the location.
	 */
	private Map<String, String> proxyMap = null;
	private String lastLocation = ConstantStrings.EMPTY_STRING;

	/**
	 * Returns the proxy server for the current notes location. This information is only initialized at startup.
	 * 
	 * @return
	 */
	private String getProxy() {
		String proxy = ConstantStrings.EMPTY_STRING;
		// //String currentLocation = ConstantStrings.EMPTY_STRING;
		// boolean refreshNeeded = false;
		// List locations = NotesLocationsPlugin.getLocations();
		// for (Object obj : locations)
		// {
		// if (obj instanceof Location)
		// {
		// Location location = (Location)obj;
		//
		// if (location.isCurrent())
		// {
		// if (!lastLocation.equals(location.getName()))
		// {
		// //If the location changes, lookup the proxy info again.
		// refreshNeeded = true;
		// }
		// lastLocation = location.getName();
		// break;
		// }
		// }
		// }
		//
		// if (proxyMap == null || refreshNeeded)
		// {
		// proxyMap = new HashMap<String, String>();
		//
		// try
		// {
		// NotesThread.sinitThread();
		// Session notesSession = NotesFactory.createSession();
		//        Database db = notesSession.getDatabase(null, "names"); //$NON-NLS-1$
		//
		// if (!db.isOpen())
		// {
		// boolean opened = db.open();
		// if (!opened)
		// {
		// //Do something to indicated this operation failed.
		// }
		// }
		// if (db.isOpen())
		// {
		// DocumentCollection collection = db.getAllDocuments();
		//
		// for (int i = 0; i < collection.getCount(); i++)
		// {
		// Document doc = null;
		// if (i == 0)
		// {
		// doc = collection.getFirstDocument();
		// }
		// else
		// {
		// doc = collection.getNextDocument();
		// }
		//
		// if (doc != null)
		// {
		//              String docForm = doc.getFirstItem("form").getValueString(); //$NON-NLS-1$
		//              if (docForm != null && docForm.equals("Location")) //$NON-NLS-1$
		// {
		//                String name = doc.getItemValueString("Name"); //$NON-NLS-1$
		//                String proxyServer = doc.getItemValueString("Proxy_HTTP"); //$NON-NLS-1$
		// proxyMap.put(name, proxyServer);
		// }
		// }
		// }
		// }
		// NotesThread.stermThread();
		// }
		// catch (Exception e)
		// {
		// UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		// }
		// }
		//
		// if (!lastLocation.equals(ConstantStrings.EMPTY_STRING))
		// {
		// proxy = proxyMap.get(lastLocation);
		// if (proxy == null)
		// {
		// proxy = ConstantStrings.EMPTY_STRING;
		// }
		// }
		return proxy;
	}

	/**
	 * Returns a new http client that is aware of the Notes proxy settings
	 * 
	 * @return
	 */
	public HttpClient getProxiedHttpClient() {
		// 63714 - server accept only TLSv1.2 protocol for poodle attack fix
		SSLContext.setDefault(getSSLContext());

		HttpClient client = new HttpClient();
		client.getParams().setContentCharset("UTF-8"); //$NON-NLS-1$
		String proxy = getProxy();
		if (proxy.length() > 0) {
			String proxyHost = proxy;
			int proxyPort = 0;
			int colonIndex = proxy.indexOf(":"); //$NON-NLS-1$
			if (colonIndex != -1) {
				proxyHost = proxy.substring(0, colonIndex);
				try {
					proxyPort = Integer.parseInt(proxy.substring(colonIndex + 1));
				} catch (Exception e) {
					// Log any errors but don't blow up completely
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
			}
			client.getHostConfiguration().setProxy(proxyHost, proxyPort);
		}
		return client;
	}

	private String callWebservice(WebserviceCallType type, Map<String, String> additionalParameters) {
		// By Default, call the web service once (i.e. retry = 0). If any web
		// services needs retries, call the
		// overloading callWebservice() method directly.
		return callWebservice(type, additionalParameters, 0);
	}

	// defect 21458 - adding retry argument
	private String callWebservice(WebserviceCallType type, Map<String, String> additionalParameters, int retry) {
		return callWebservice(type, additionalParameters, retry, true);
	}

	private String callWebservice(WebserviceCallType type, Map<String, String> additionalParameters, int retry, boolean hasJSONResponse) {
		String output = null;

		try {
			String server = NotesAccountManager.getInstance().getCRMServer();
			String user = NotesAccountManager.getInstance().getCRMUser();
			String password = NotesAccountManager.getInstance().getCRMPassword();

			String script = ConstantStrings.EMPTY_STRING;
			switch (type) {
				case GET_SAMETIME_INFO :
					script = "getUserList.php"; //$NON-NLS-1$
					break;
				case GET_SUGAR_INFO :
					script = "getInfo.php"; //$NON-NLS-1$
					break;
				case GET_TYPEAHEAD_INFO :
					script = "typeAhead.php"; //$NON-NLS-1$
					break;
				case CREATE_CALL_LOG :
					script = "saveCall.php"; //$NON-NLS-1$
					break;
				case GET_FAVORITES :
				case SET_FAVORITE :
				case UNSET_FAVORITE :
					script = "doFavorites.php"; //$NON-NLS-1$
					break;
				case GET_SESSION :
					script = "getSession.php"; //$NON-NLS-1$
					break;
				case GET_CALL_FORM :
					script = "getCallForm.php"; //$NON-NLS-1$
					break;
				case SFA_REST :
					script = "sfaRest.php"; //$NON-NLS-1$
					break;
				case GET_ENTRY_LIST :
					script = "sfaRest.php"; //$NON-NLS-1$
					break;
				case GET_INFO13 :
					script = "sfaRest.php"; //$NON-NLS-1$
					break;
			}

			if (script != null) {
				String uri = server + "custom/scrmsugarws/v2_1/" + script; //$NON-NLS-1$

				try {
					HttpClient client = getProxiedHttpClient();

					PostMethod post = new PostMethod(uri);
					post.addParameter("userid", user); //$NON-NLS-1$
					post.addParameter("password", password); //$NON-NLS-1$
					String session = getSessionId(false);

					// If invalid credential, short circuit the intended web service
					if (unableToLogin()) {
						if (!script.equals("getSession.php")) {
							if (toPromptCredential()) {
								getPropertyChangeSupport().firePropertyChange(BRING_UP_CREDENTIAL_PROMPT, true, false);
							}
						}
						return output;
					}

					if (!session.equals(ConstantStrings.EMPTY_STRING)) {
						post.addParameter("sessionid", getSessionId(false)); //$NON-NLS-1$  
					}

					if (additionalParameters != null) {
						for (String key : additionalParameters.keySet()) {
							post.addParameter(key, additionalParameters.get(key));
						}
					}

					for (int i = 0; i < (retry + 1); i++) {
						doBeginningLog(script, getMethod(additionalParameters));

						serverIncorrect = false;
						try {
							client.executeMethod(post);
							output = post.getResponseBodyAsString();
							UtilsPlugin.getDefault().logInfoMessage(WebServiceLogUtil.getDebugMsg(post, output, post.getURI().toString(), 0, 0), CorePluginActivator.PLUGIN_ID);
							// System.out.println(WebServiceLogUtil.getDebugMsg(post, output, post.getURI().toString(), 0, 0));
							doEndingLog(script, getMethod(additionalParameters));
							if (hasJSONResponse) {
								JSONObject jsonObject = null;
								try {
									// The response of the follow API is unconventional, it's just a String "followed" or
									// "unfollowed" (Note that the double quotes (") are part of the returned string). We
									// will strip the double quotes from the String and return it.
									if (isFollow(additionalParameters) && output != null && output.length() > 2) {
										output = output.substring(1, output.length() - 1);
									} else {
										jsonObject = new JSONObject(output);
										String sessionId = jsonObject.getJSONObject("header").getString("sessionid"); //$NON-NLS-1$ //$NON-NLS-2$
										if (sessionId != null && sessionId.length() > 0) {
											lastSession = sessionId;
										}
									}

									// terminate the retry loop if no exception
									if (retry > 0) {
										i = retry + 1;
									}
								}
								// If blank response body, or no header
								catch (JSONException e) {
									UtilsPlugin.getDefault().logException(e, null, WebServiceLogUtil.getDebugMsg(post, output, uri, retry, i), CorePluginActivator.PLUGIN_ID);

									if (isInvalidCredential(output)) {
										// if invalid credential,
										// no need to retry ... terminate the retry
										// loop
										if (retry > 0) {
											i = retry + 1;
										}
									}
								}
							}

							post.releaseConnection();
						} catch (Exception e) {
							UtilsPlugin.getDefault().logException(e, null, WebServiceLogUtil.getDebugMsg(post, output, uri, retry, i), CorePluginActivator.PLUGIN_ID);

							if (e instanceof UnknownHostException) {
								serverIncorrect = true;
								// if server url is incorrect, no need to retry
								// ... terminate the retry loop
								if (retry > 0) {
									i = retry + 1;
								}
							}
							// 63714 - server accept only TLSv1.2 protocol for poodle attack fix. But, if
							// this server does not accept TLSv1.2, we will downgrade to TLSv1.
							else if (e instanceof SSLHandshakeException || (e instanceof SSLException && ((SSLException) e).getMessage().toLowerCase().contains("protocol_version"))) {

								if (_sslprotocol == TLSV12) {
									UtilsPlugin.getDefault().logInfoMessage(
											"SSLHandshakeException with protocol:" + _sslprotocol + ", re-try by setting default protocol to " + TLSV1, CorePluginActivator.PLUGIN_ID); //$NON-NLS-1$ //$NON-NLS-2$
									// System.out.println("SSLHandshakeException with protocol:" + _sslprotocol + ", re-try by setting default protocol to " + TLSV1);
									_sslprotocol = TLSV1;
								} else if (_sslprotocol == TLSV1) {
									UtilsPlugin.getDefault().logInfoMessage(
											"SSLHandshakeException with protocol:" + _sslprotocol + ", re-try by setting default protocol to " + TLSV12, CorePluginActivator.PLUGIN_ID); //$NON-NLS-1$ //$NON-NLS-2$
									// System.out.println("SSLHandshakeException with protocol:" + _sslprotocol + ", re-try by setting default protocol to " + TLSV12);
									_sslprotocol = TLSV12;
								}
								SSLContext.setDefault(getSSLContext());
							}
						}
					}
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			output = null;
		}
		return output;

	}

	private boolean isFollow(Map<String, String> additionalParameters) {
		boolean isFollow = false;
		if (additionalParameters != null) {
			if (additionalParameters.get("method") != null) { //$NON-NLS-1$
				if (additionalParameters.get("method").equals("cwFollow") || additionalParameters.get("method").equals("cwUnfollow")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
					isFollow = true;
				}
			}
		}
		return isFollow;
	}

	private boolean isFollow20(Map<String, String> additionalParameters) {
		boolean isFollow = false;
		if (additionalParameters != null) {
			if (additionalParameters.get("method") != null) { //$NON-NLS-1$
				if (additionalParameters.get("method").equals("cwFollow20") || additionalParameters.get("method").equals("cwUnfollow20")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
					isFollow = true;
				}
			}
		}
		return isFollow;
	}

	private boolean isInvalidCredential(String output) {
		boolean isInvalid = false;
		try {
			JSONObject outputJson = new JSONObject(output);
			String name = outputJson.getString("number"); //$NON-NLS-1$
			if (name.equals("SFA0002")) //$NON-NLS-1$
			{
				isInvalid = true;
			} else if (name.equals("SFA0001")) //$NON-NLS-1$
			{
				// 47997 - the following logic probably is not correct... set isInvalid (credential) with or without user id
				// // 34940 - if user id was not supplied
				// String user = NotesAccountManager.getInstance().getCRMUser();
				// if (user == null || user.equals(ConstantStrings.EMPTY_STRING)) {
				// isInvalid = true;
				// }
				isInvalid = true;

			}
		} catch (Exception e) {
			// eat it
		}
		return isInvalid;
	}

	public void processSugarEntryInfo(String output) {
		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(output);
		} catch (JSONException e) {
			// End gracefully.
		}
		if (jsonObject != null) {
			try {
				if (jsonObject.containsKey(ConstantStrings.ACCOUNTS)) {
					Iterator<String> ids = jsonObject.getJSONObject(ConstantStrings.ACCOUNTS).keys();
					List<Map<String, Object>> accounts = processSugarEntries(jsonObject.getJSONArray(ConstantStrings.ACCOUNTS), SugarType.ACCOUNTS);
					for (Map<String, Object> account : accounts) {
						String id = ids.next();
						processSugarAccount(account, id);
					}
				}
				if (jsonObject.containsKey(ConstantStrings.OPPORTUNITIES)) {
					Iterator<String> ids = jsonObject.getJSONObject(ConstantStrings.OPPORTUNITIES).keys();
					List<Map<String, Object>> opportunities = processSugarEntries(jsonObject.getJSONArray(ConstantStrings.OPPORTUNITIES), SugarType.OPPORTUNITIES);
					for (Map<String, Object> opportunity : opportunities) {
						String id = ids.next();
						processSugarOpportunities(opportunity, id);
					}
				}
				if (jsonObject.containsKey(ConstantStrings.CONTACTS)) {
					Iterator<String> ids = jsonObject.getJSONObject(ConstantStrings.CONTACTS).keys();
					List<Map<String, Object>> contacts = processSugarEntries(jsonObject.getJSONArray(ConstantStrings.CONTACTS), SugarType.CONTACTS);
					for (Map<String, Object> contact : contacts) {
						String id = ids.next();
						processSugarContact(contact, id);
					}
				}
			} catch (JSONException e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
	}

	private List<Map<String, Object>> processSugarEntries(JSONArray jsonArray, SugarType sugarType) {
		List<Map<String, Object>> sugarEntries = new ArrayList<Map<String, Object>>();
		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				try {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					Iterator<String> keys = jsonObject.keys();
					while (keys.hasNext()) {
						try {
							sugarEntries.add(jsonObject.getJSONObject((keys.next())));
						} catch (JSONException e1) {
							UtilsPlugin.getDefault().logException(e1, CorePluginActivator.PLUGIN_ID);
						}
					}
				} catch (JSONException e) {
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
			}
		}
		return sugarEntries;
	}

	private void processSugarAccount(Map<String, Object> map, String id) {
		processSugarAccount(map, id, GetInfo13RestulType.BASECARD);
	}
	private void processSugarAccount(Map<String, Object> map, String id, GetInfo13RestulType resultType) {
		if (resultType != null && resultType.equals(GetInfo13RestulType.FOLLOWED)) {
			processSugarAccountFollowed(map, id);
		} else if (resultType != null && resultType.equals(GetInfo13RestulType.OPPTIES)) {
			processSugarAccountOppty(map, id);
		} else {

			if (map != null) {
				String name = getStringValueFromMap(map, ConstantStrings.DATABASE_NAME);
				if (id != null && name != null) {
					SugarAccount account = (SugarAccount) getSugarEntryById(id);
					if (account == null) {
						account = new SugarAccount(id, name);
					} else {
						account.setName(name);
					}
					String website = getStringValueFromMap(map, ConstantStrings.DATABASE_WEBSITE);
					if (website != null && !website.equals("http://")) //$NON-NLS-1$
					{
						account.setWebsite(website);
					}
					if (map.containsKey(ConstantStrings.DATABASE_PHYSICAL_ADDRESS_STREET)) {
						account.setStreet(getStringValueFromMap(map, ConstantStrings.DATABASE_PHYSICAL_ADDRESS_STREET));
					}
					if (map.containsKey(ConstantStrings.DATABASE_PHYSICAL_ADDRESS_CITY)) {
						account.setCity(getStringValueFromMap(map, ConstantStrings.DATABASE_PHYSICAL_ADDRESS_CITY));
					}
					if (map.containsKey(ConstantStrings.DATABASE_PHYSICAL_ADDRESS_STATE)) {
						account.setState(getStringValueFromMap(map, ConstantStrings.DATABASE_PHYSICAL_ADDRESS_STATE));
					}
					if (map.containsKey(ConstantStrings.DATABASE_PHYSICAL_ADDRESS_POSTAL_CODE)) {
						account.setPostalCode(getStringValueFromMap(map, ConstantStrings.DATABASE_PHYSICAL_ADDRESS_POSTAL_CODE));
					}
					if (map.containsKey(ConstantStrings.DATABASE_PHYSICAL_ADDRESS_COUNTRY)) {
						account.setCountry(getStringValueFromMap(map, ConstantStrings.DATABASE_PHYSICAL_ADDRESS_COUNTRY));
					}
					if (map.containsKey(ConstantStrings.DATABASE_PHONE_OFFICE)) {
						account.setOfficePhone(getStringValueFromMap(map, ConstantStrings.DATABASE_PHONE_OFFICE));
					}
					if (map.containsKey(ConstantStrings.DATABASE_CLIENT_ID)) {
						account.setClientId(getStringValueFromMap(map, ConstantStrings.DATABASE_CLIENT_ID));
					}
					if (map.containsKey(ConstantStrings.DATABASE_PHONE_FAX)) {
						account.setFax(getStringValueFromMap(map, ConstantStrings.DATABASE_PHONE_FAX));
					}
					if (map.containsKey(ConstantStrings.DATABASE_FOLLOWED)) {
						account.setFollowed(getBooleanValueFromMap(map, ConstantStrings.DATABASE_FOLLOWED));
					}
					if (map.containsKey(ConstantStrings.DATABASE_INDUSTRY)) {
						// Apparently we'll get either a String or JSONObject depending on whether an industry is actually set.
						if (map.get(ConstantStrings.DATABASE_INDUSTRY) instanceof JSONObject) {
							JSONObject industryObj = (JSONObject) map.get(ConstantStrings.DATABASE_INDUSTRY);
							account.setIndustryMap(jsonObjectToMap(industryObj));
						}
					}
					
					// 80623
					if (map.containsKey(ConstantStrings.DATABASE_INDUS_INDUSTRY)) {
						account.setIndusIndustry(getStringValueFromMap(map, ConstantStrings.DATABASE_INDUS_INDUSTRY));
					}
					
					// account.setPrimaryContactId(getStringValueFromMap(map, ConstantStrings.DATABASE_PRIMARY_CONTACT_ID));
					if (map.containsKey(ConstantStrings.DATABASE_OPPORTUNITIES_TOTAL)) {
						if (account.getOpportunityIDs() != null && !account.getOpportunityIDs().isEmpty()) {
							account.getOpportunityIDs().clear();
						}
						account.getOpportunityIDs().addAll(getStringValuesFromJsonArray(map, ConstantStrings.DATABASE_ACCOUNT_OPPORTUNITIES));
						Integer totalOpptys = getIntValueFromMap(map, ConstantStrings.DATABASE_OPPORTUNITIES_TOTAL);
						if (totalOpptys != null) {
							account.setTotalOpportunities(totalOpptys);
						}
					}
					account.setTimestamp(Calendar.getInstance());

					if (account.getTags() != null && !account.getTags().isEmpty()) {
						account.getTags().clear();
					}
					account.getTags().addAll(getStringValuesFromJsonArray(map, ConstantStrings.DATABASE_TAGS));
					if (getSugarEntries().contains(account)) {
						getSugarEntries().remove(account);
					}
					getSugarEntries().add(account);
				}
			}
		}
	}

	private void processSugarAccountFollowed(Map<String, Object> map, String id) {

		// String name = getStringValueFromMap(map, ConstantStrings.DATABASE_NAME);
		if (id != null) {
			SugarAccount account = (SugarAccount) getSugarEntryById(id);

			try {
				if (map.containsKey(ConstantStrings.DATABASE_FOLLOWED)) {
					account.setFollowed(getBooleanValueFromMap(map, ConstantStrings.DATABASE_FOLLOWED));
				}

				if (map.containsKey(ConstantStrings.DATABASE_ISPARENT)) {
					account.setIsParent(getBooleanValueFromMap(map, ConstantStrings.DATABASE_ISPARENT));
				}
				if (map.containsKey(ConstantStrings.DATABASE_RELATEDCLIENTS)) {
					// Integer num = getIntValueFromMap(map, ConstantStrings.DATABASE_RELATEDCLIENTS);
					// account.setNumOfRelatedClients(num == null ? 0 : Integer.valueOf(num));
					String numX = getStringValueFromMap(map, ConstantStrings.DATABASE_RELATEDCLIENTS);
					int num = 0;
					try {

						if (numX != null) {
							num = Integer.valueOf(numX);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					account.setNumOfRelatedClients(num);

				}
				if (map.containsKey(ConstantStrings.DATABASE_PARENTFOLLOWINFO)) {
					Object obj = getObjectFromMap(map, ConstantStrings.DATABASE_PARENTFOLLOWINFO);
					if (obj != null && obj instanceof JSONArray && ((JSONArray) obj).isEmpty()) {
						account.setFollowingParentCCMSId(null);
						account.setFollowingParentId(null);
						account.setFollowingParentLink(null);
						account.setFollowingParentName(null);
					} else if (obj != null && obj instanceof JSONObject) {
						
						if (((JSONObject) obj).has(ConstantStrings.DATABASE_CCMS_ID)) {
							account.setFollowingParentCCMSId(((JSONObject) obj).getString(ConstantStrings.DATABASE_CCMS_ID));
						}

						if (((JSONObject) obj).has(ConstantStrings.DATABASE_NAME)) {
							account.setFollowingParentName(((JSONObject) obj).getString(ConstantStrings.DATABASE_NAME));
						}
						if (((JSONObject) obj).has(ConstantStrings.DATABASE_PARENTLINK)) {
							account.setFollowingParentLink(((JSONObject) obj).getString(ConstantStrings.DATABASE_PARENTLINK));
						}
						if (((JSONObject) obj).has(ConstantStrings.DATABASE_ID)) {
							account.setFollowingParentId(((JSONObject) obj).getString(ConstantStrings.DATABASE_ID));
						}
						// }
					}
				}

				account.setTimestamp(Calendar.getInstance());

				if (getSugarEntries().contains(account)) {
					getSugarEntries().remove(account);
				}
				getSugarEntries().add(account);
			} catch (NullPointerException ne) {
				if (account != null) {
					UtilsPlugin.getDefault().logException(ne, CorePluginActivator.PLUGIN_ID);
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}

		}

	}
	private void processSugarAccountOppty(Map<String, Object> map, String id) {

		if (map != null) {
			if (id != null) {
				SugarAccount account = (SugarAccount) getSugarEntryById(id);

				try {
					if (account != null && account.getOpportunityIDs() != null && !account.getOpportunityIDs().isEmpty()) {
						account.getOpportunityIDs().clear();
					}

					Integer totalOpptys = 0;
					if (map.containsKey(ConstantStrings.DATABASE_OPPORTUNITIES_TOTAL)) {
						if (map.get(ConstantStrings.DATABASE_OPPORTUNITIES_TOTAL) instanceof Integer) {
							totalOpptys = (Integer) map.get(ConstantStrings.DATABASE_OPPORTUNITIES_TOTAL);
							if (totalOpptys != null) {
								account.setTotalOpportunities(totalOpptys);
							}
						}
					}

					if (totalOpptys > 0) {
						if (map.containsKey(ConstantStrings.DATABASE_ACCOUNT_OPPORTUNITIES)) {
							if (map.get(ConstantStrings.DATABASE_ACCOUNT_OPPORTUNITIES) instanceof JSONObject) {
								JSONObject obj = (JSONObject) map.get(ConstantStrings.DATABASE_ACCOUNT_OPPORTUNITIES);
								if (obj != null && obj.length() > 0) {
									List<String> values = new ArrayList<String>();
									Iterator<String> keys = obj.keys();

									// 87898
									while (keys.hasNext()) {
										String opptyid = keys.next();
										Map<String, Object> opptymap = GenericUtils.JSONObjectToMap(obj.getJSONObject(opptyid));
										processSugarOpportunities(opptymap, opptyid, GetInfo13RestulType.BASECARD);
										values.add(opptyid);
									}
									
									if (values != null && !values.isEmpty()) {
										account.getOpportunityIDs().addAll(values);
									}
								}
							}
						}
					}

					account.setTimestamp(Calendar.getInstance());

					if (getSugarEntries().contains(account)) {
						getSugarEntries().remove(account);
					}
					getSugarEntries().add(account);

				} catch (NullPointerException ne) {
					if (account != null) {
						UtilsPlugin.getDefault().logException(ne, CorePluginActivator.PLUGIN_ID);
					}
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
			}
		}

	}

	private void processSugarContact(Map<String, Object> map, String id) {
		processSugarContact(map, id, GetInfo13RestulType.BASECARD);
	}
	private void processSugarContact(Map<String, Object> map, String id, GetInfo13RestulType resultType) {
		if (resultType != null && resultType.equals(GetInfo13RestulType.OPPTIES)) {
			processSugarContactOppty(map, id);
		} else {
			if (map != null) {
				String name = SugarDashboardPreference.getInstance().getFormattedNameWithoutSalutation(getStringValueFromMap(map, ConstantStrings.DATABASE_FIRST_NAME),
						getStringValueFromMap(map, ConstantStrings.DATABASE_LAST_NAME));
				if (id != null && name != null) {
					SugarContact contact = (SugarContact) getSugarEntryById(id);
					if (contact == null) {
						contact = new SugarContact(id, name);
					} else {
						contact.setName(name);
					}
					if (map.containsKey(ConstantStrings.DATABASE_FIRST_NAME)) {
						contact.setFirstName(getStringValueFromMap(map, ConstantStrings.DATABASE_FIRST_NAME));
					}
					if (map.containsKey(ConstantStrings.DATABASE_LAST_NAME)) {
						contact.setLastName(getStringValueFromMap(map, ConstantStrings.DATABASE_LAST_NAME));
					}
					if (map.containsKey(ConstantStrings.DATABASE_TITLE)) {
						contact.setJobTitle(getStringValueFromMap(map, ConstantStrings.DATABASE_TITLE));
					}
					if (map.containsKey(ConstantStrings.DATABASE_PRIMARY_ADDRESS_STREET)) {
						contact.setStreet(getStringValueFromMap(map, ConstantStrings.DATABASE_PRIMARY_ADDRESS_STREET));
					}
					if (map.containsKey(ConstantStrings.DATABASE_PRIMARY_ADDRESS_CITY)) {
						contact.setCity(getStringValueFromMap(map, ConstantStrings.DATABASE_PRIMARY_ADDRESS_CITY));
					}
					if (map.containsKey(ConstantStrings.DATABASE_PRIMARY_ADDRESS_STATE)) {
						contact.setState(getStringValueFromMap(map, ConstantStrings.DATABASE_PRIMARY_ADDRESS_STATE));
					}
					if (map.containsKey(ConstantStrings.DATABASE_PRIMARY_ADDRESS_POSTAL_CODE)) {
						contact.setPostalCode(getStringValueFromMap(map, ConstantStrings.DATABASE_PRIMARY_ADDRESS_POSTAL_CODE));
					}
					if (map.containsKey(ConstantStrings.DATABASE_PRIMARY_ADDRESS_COUNTRY)) {
						contact.setCountry(getStringValueFromMap(map, ConstantStrings.DATABASE_PRIMARY_ADDRESS_COUNTRY));
					}
					if (map.containsKey(ConstantStrings.DATABASE_PHONE_WORK)) {
						contact.setOfficePhone(getStringValueFromMap(map, ConstantStrings.DATABASE_PHONE_WORK));
					}
					if (map.containsKey(ConstantStrings.DATABASE_PHONE_MOBILE)) {
						contact.setMobilePhone(getStringValueFromMap(map, ConstantStrings.DATABASE_PHONE_MOBILE));
					}
					if (map.containsKey(ConstantStrings.DATABASE_EMAIL_ADDRESS)) {
						contact.setEmail(getStringValueFromMap(map, ConstantStrings.DATABASE_EMAIL_ADDRESS));
					}
					if (map.containsKey(ConstantStrings.DATABASE_WEBSITE)) {
						contact.setWebsite(getStringValueFromMap(map, ConstantStrings.DATABASE_WEBSITE));
					}
					if (map.containsKey(ConstantStrings.ACCOUNT_NAME)) {
						contact.setAccountName(getStringValueFromMap(map, ConstantStrings.ACCOUNT_NAME));
					}
					if (map.containsKey(ConstantStrings.ACCOUNT_ID)) {
						contact.setAccountID(getStringValueFromMap(map, ConstantStrings.ACCOUNT_ID));
					}
					if (map.containsKey(ConstantStrings.DATABASE_EMAIL_OPT_OUT)) {
						contact.setEmailSuppressed(getStringValueFromMap(map, ConstantStrings.DATABASE_EMAIL_OPT_OUT).equals("1")); //$NON-NLS-1$
					}
					if (map.containsKey(ConstantStrings.DATABASE_PHONE_MOBILE_OPT_OUT)) {
						contact.setMobilePhoneSuppressed(getStringValueFromMap(map, ConstantStrings.DATABASE_PHONE_MOBILE_OPT_OUT).equals("1")); //$NON-NLS-1$
					}
					if (map.containsKey(ConstantStrings.DATABASE_PHONE_WORK_OPT_OUT)) {
						contact.setOfficePhoneSuppressed(getStringValueFromMap(map, ConstantStrings.DATABASE_PHONE_WORK_OPT_OUT).equals("1")); //$NON-NLS-1$
					}

					if (contact.getOpportunityIDs() != null && !contact.getOpportunityIDs().isEmpty()) {
						contact.getOpportunityIDs().clear();
					}
					if (map.containsKey(ConstantStrings.DATABASE_ACCOUNT_OPPORTUNITIES)) {
						contact.getOpportunityIDs().addAll(getStringValuesFromJsonArray(map, ConstantStrings.DATABASE_ACCOUNT_OPPORTUNITIES));
					}

					if (map.containsKey(ConstantStrings.DATABASE_OPPORTUNITIES_TOTAL)) {
						Integer totalOpptys = getIntValueFromMap(map, ConstantStrings.DATABASE_OPPORTUNITIES_TOTAL);
						if (totalOpptys != null) {
							contact.setTotalOpportunities(totalOpptys);
						}
					}
					contact.setTimestamp(Calendar.getInstance());
					if (getSugarEntries().contains(contact)) {
						getSugarEntries().remove(contact);
					}
					getSugarEntries().add(contact);
				}
			}
		}
	}

	private void processSugarContactOppty(Map<String, Object> map, String id) {

		if (map != null) {
			if (id != null) {
				SugarContact contact = (SugarContact) getSugarEntryById(id);

				try {
					if (contact != null && contact.getOpportunityIDs() != null && !contact.getOpportunityIDs().isEmpty()) {
						contact.getOpportunityIDs().clear();
					}
					if (map.containsKey(ConstantStrings.DATABASE_ACCOUNT_OPPORTUNITIES)) {
						if (map.get(ConstantStrings.DATABASE_ACCOUNT_OPPORTUNITIES) instanceof JSONObject) {
							JSONObject obj = (JSONObject) map.get(ConstantStrings.DATABASE_ACCOUNT_OPPORTUNITIES);
							if (obj != null && obj.length() > 0) {
								List<String> values = new ArrayList<String>();
								Iterator<String> keys = obj.keys();

								// 81013 - JSONObjects are not in any sort order, so, sort the key (i.e. the date_closed) here
								Map<String, String> sortedMap = new TreeMap<String, String>(Collections.reverseOrder());
								while (keys.hasNext()) {
									String opptyid = keys.next();
									JSONObject opptyObject = obj.getJSONObject(opptyid);
									if (opptyObject.containsKey(SugarV10APIManager.OPPTY_SORTED_BY)) { //$NON-NLS-1$
										sortedMap.put((String) opptyObject.get(SugarV10APIManager.OPPTY_SORTED_BY), opptyid); //$NON-NLS-1$
									}
								}
								Iterator<String> itSortedOpptyIds = sortedMap.values().iterator();
								while (itSortedOpptyIds.hasNext()) {
									String opptyid = itSortedOpptyIds.next();
									Map<String, Object> opptymap = GenericUtils.JSONObjectToMap(obj.getJSONObject(opptyid));
									processSugarOpportunities(opptymap, opptyid, GetInfo13RestulType.BASECARD);
									values.add(opptyid);
								}

								if (values != null && !values.isEmpty()) {
									contact.getOpportunityIDs().addAll(values);
								}
							}
						}
					}

					if (map.containsKey(ConstantStrings.DATABASE_OPPORTUNITIES_TOTAL)) {
						if (map.get(ConstantStrings.DATABASE_OPPORTUNITIES_TOTAL) instanceof Integer) {
							Integer totalOpptys = (Integer) map.get(ConstantStrings.DATABASE_OPPORTUNITIES_TOTAL);
							if (totalOpptys != null) {
								contact.setTotalOpportunities(totalOpptys);
							}
						}
					}
					contact.setTimestamp(Calendar.getInstance());

					if (getSugarEntries().contains(contact)) {
						getSugarEntries().remove(contact);
					}
					getSugarEntries().add(contact);

				} catch (NullPointerException ne) {
					if (contact != null) {
						UtilsPlugin.getDefault().logException(ne, CorePluginActivator.PLUGIN_ID);
					}
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
			}
		}

	}
	private void processSugarOpportunities(Map<String, Object> map, String id) {
		processSugarOpportunities(map, id, GetInfo13RestulType.BASECARD);
	}
	private void processSugarOpportunities(Map<String, Object> map, String id, GetInfo13RestulType resultType) {
		if (resultType != null && resultType.equals(GetInfo13RestulType.FOLLOWED)) {
			processSugarOpportunitiesFollowed(map, id);
		} else if (resultType != null && resultType.equals(GetInfo13RestulType.RLIS)) {
			processSugarOpportunitiesLineItem(map, id);
		} else {
			if (map != null) {
				String name = getStringValueFromMap(map, ConstantStrings.DATABASE_NAME);
				if (id != null && name != null) {
					SugarOpportunity opp = (SugarOpportunity) getSugarEntryById(id);
					if (opp == null) {
						opp = new SugarOpportunity(id, name);
					} else {
						opp.setName(name);
					}
					if (map.containsKey(ConstantStrings.DATABASE_DESCRIPTION)) {
						opp.setDescription(getStringValueFromMap(map, ConstantStrings.DATABASE_DESCRIPTION));
					}
					if (map.containsKey(ConstantStrings.ACCOUNT_NAME)) {
						opp.setAccountName(getStringValueFromMap(map, ConstantStrings.ACCOUNT_NAME));
					}

					if (map.containsKey(ConstantStrings.ACCOUNT_ID)) {
						opp.setAccountID(getStringValueFromMap(map, ConstantStrings.ACCOUNT_ID));
					}
					if (map.containsKey(ConstantStrings.DATABASE_AMOUNT)) {
						opp.setTotalRevenue(getStringValueFromMap(map, ConstantStrings.DATABASE_AMOUNT));
					}
					if (map.containsKey(ConstantStrings.DATABASE_DATE_CLOSED)) {
						opp.setDecisionDate(getStringValueFromMap(map, ConstantStrings.DATABASE_DATE_CLOSED));
					}
					if (map.containsKey(ConstantStrings.DATABASE_SALES_STAGE)) {
						opp.setSalesStage(getStringValueFromMap(map, ConstantStrings.DATABASE_SALES_STAGE));
					}
					if (map.containsKey(ConstantStrings.DATABASE_ASSIGNED_USER_NAME)) {
						opp.setAssignedUserName(getStringValueFromMap(map, ConstantStrings.DATABASE_ASSIGNED_USER_NAME));
					}
					if (map.containsKey(ConstantStrings.DATABASE_ASSIGNED_USER_EMAIL)) {
						opp.setAssignedUserEmail(getStringValueFromMap(map, ConstantStrings.DATABASE_ASSIGNED_USER_EMAIL));
					}
					if (map.containsKey(ConstantStrings.DATABASE_ASSIGNED_USER_ID)) {
						opp.setAssignedUserID(getStringValueFromMap(map, ConstantStrings.DATABASE_ASSIGNED_USER_ID));
					}
					if (map.containsKey(ConstantStrings.DATABASE_PRIMARY_CONTACT_NAME)) {
						opp.setPrimaryContact(getStringValueFromMap(map, ConstantStrings.DATABASE_PRIMARY_CONTACT_NAME));
					}
					if (map.containsKey(ConstantStrings.DATABASE_PRIMARY_CONTACT_ID)) {
						opp.setPrimaryContactID(getStringValueFromMap(map, ConstantStrings.DATABASE_PRIMARY_CONTACT_ID));
					}
					if (map.containsKey(ConstantStrings.DATABASE_WEBSITE)) {
						opp.setWebsite(getStringValueFromMap(map, ConstantStrings.DATABASE_WEBSITE));
					}
					if (map.containsKey(ConstantStrings.DATABASE_FOLLOWED)) {
						opp.setFollowed(getBooleanValueFromMap(map, ConstantStrings.DATABASE_FOLLOWED));
					}
					// Apparently we'll get either a String or JSONObject depending on whether an industry is actually set.
					if (map.containsKey(ConstantStrings.DATABASE_INDUSTRY)) {
						if (map.get(ConstantStrings.DATABASE_INDUSTRY) instanceof JSONObject) {
							JSONObject industryObj = (JSONObject) map.get(ConstantStrings.DATABASE_INDUSTRY);
							opp.setIndustryMap(jsonObjectToMap(industryObj));
						}
					}
					
					// 80623
					if (map.containsKey(ConstantStrings.DATABASE_INDUS_INDUSTRY)) {
						opp.setIndusIndustry(getStringValueFromMap(map, ConstantStrings.DATABASE_INDUS_INDUSTRY));
					}
					
					if (map.containsKey(ConstantStrings.DATABASE_REVENUE_LINE_ITEMS)) {
						JSONObject lineItems = (JSONObject) map.get(ConstantStrings.DATABASE_REVENUE_LINE_ITEMS);

						if (opp.getRevenueLineItems() != null && !opp.getRevenueLineItems().isEmpty()) {
							opp.getRevenueLineItems().clear(); // Clobber the old items
						}
						try {
							if (lineItems != null) {
								Iterator iter = lineItems.keys();
								while (iter.hasNext()) {
									String key = iter.next().toString();
									JSONObject lineItem = (JSONObject) lineItems.get(key);
									RevenueLineItem rli = new RevenueLineItem();
									rli.setAmount(lineItem.get(ConstantStrings.DATABASE_AMOUNT).toString());
									rli.setBillDate(lineItem.get(ConstantStrings.DATABASE_BILL_DATE).toString());
									rli.setLastModifiedDate(lineItem.get(ConstantStrings.DATABASE_LAST_MODIFIED_DATE).toString());
									rli.setName(lineItem.get(ConstantStrings.DATABASE_LEVEL15).toString());
									OpportunityOwner owner = new OpportunityOwner();
									owner.setEmail(lineItem.get(ConstantStrings.DATABASE_ASSIGNED_USER_EMAIL).toString());
									owner.setName(lineItem.get(ConstantStrings.DATABASE_ASSIGNED_USER_NAME).toString());
									rli.setOwner(owner);

									opp.getRevenueLineItems().add(rli);
								}
								Collections.sort(opp.getRevenueLineItems(), new Comparator<RevenueLineItem>() {
									@Override
									public int compare(RevenueLineItem item1, RevenueLineItem item2) {
										return item1.getLastModifiedDate().compareTo(item2.getLastModifiedDate());
									}
								});
							}
						} catch (Exception e) {
							UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
						}
					}
					opp.setTimestamp(Calendar.getInstance());
					if (getSugarEntries().contains(opp)) {
						getSugarEntries().remove(opp);
					}
					getSugarEntries().add(opp);
				}
			}
		}
	}

	private void processSugarOpportunitiesLineItem(Map<String, Object> map, String id) {
		if (map != null) {
			if (id != null) {
				SugarOpportunity opp = (SugarOpportunity) getSugarEntryById(id);
				try {
					if (opp != null && opp.getRevenueLineItems() != null && !opp.getRevenueLineItems().isEmpty()) {
						opp.getRevenueLineItems().clear(); // Clobber the old items
					}
					Iterator<String> iter = map.keySet().iterator();

					while (iter.hasNext()) {
						String key = iter.next().toString();
						JSONObject lineItem = (JSONObject) map.get(key);
						RevenueLineItem rli = new RevenueLineItem();
						rli.setAmount(lineItem.get(ConstantStrings.DATABASE_AMOUNT).toString());
						rli.setBillDate(lineItem.get(ConstantStrings.DATABASE_BILL_DATE).toString());
						rli.setLastModifiedDate(lineItem.get(ConstantStrings.DATABASE_LAST_MODIFIED_DATE).toString());

						// fix a nullpointerexception
						if (lineItem.containsKey(ConstantStrings.DATABASE_LEVEL15) && lineItem.get(ConstantStrings.DATABASE_LEVEL15) != null) {
							rli.setName(lineItem.get(ConstantStrings.DATABASE_LEVEL15).toString());
						}

						OpportunityOwner owner = new OpportunityOwner();
						owner.setEmail(lineItem.get(ConstantStrings.DATABASE_ASSIGNED_USER_EMAIL).toString());
						owner.setName(lineItem.get(ConstantStrings.DATABASE_ASSIGNED_USER_NAME).toString());
						rli.setOwner(owner);

						opp.getRevenueLineItems().add(rli);
					}
					Collections.sort(opp.getRevenueLineItems(), new Comparator<RevenueLineItem>() {
						@Override
						public int compare(RevenueLineItem item1, RevenueLineItem item2) {
							return item1.getLastModifiedDate().compareTo(item2.getLastModifiedDate());
						}
					});

					opp.setTimestamp(Calendar.getInstance());

					if (getSugarEntries().contains(opp)) {
						getSugarEntries().remove(opp);
					}
					getSugarEntries().add(opp);
				} catch (NullPointerException ne) {
					if (opp == null) {
					} else {
						UtilsPlugin.getDefault().logException(ne, CorePluginActivator.PLUGIN_ID);
					}
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
			}
		}
	}

	private void processSugarOpportunitiesFollowed(Map<String, Object> map, String id) {

		if (id != null) {
			SugarOpportunity opp = (SugarOpportunity) getSugarEntryById(id);

			try {
				if (map.containsKey(ConstantStrings.DATABASE_FOLLOWED) && getBooleanValueFromMap(map, ConstantStrings.DATABASE_FOLLOWED) != null) {
					opp.setFollowed(getBooleanValueFromMap(map, ConstantStrings.DATABASE_FOLLOWED));
				}

				opp.setTimestamp(Calendar.getInstance());

				if (getSugarEntries().contains(opp)) {
					getSugarEntries().remove(opp);
				}
				getSugarEntries().add(opp);
			} catch (NullPointerException ne) {
				if (opp == null) {
				} else {
					UtilsPlugin.getDefault().logException(ne, CorePluginActivator.PLUGIN_ID);
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}

		}

	}

	public SugarContact getSugarContactByEmail(String email) {
		SugarContact sugarEntry = null;
		List<BaseSugarEntry> entries = getSugarEntries();
		for (BaseSugarEntry entry : (List<BaseSugarEntry>) entries) {
			if (entry instanceof SugarContact) {
				SugarContact contact = (SugarContact) entry;
				if (contact.getEmail().equalsIgnoreCase(email)) {
					sugarEntry = contact;
					break;
				}
			}
		}
		return sugarEntry;
	}

	public SugarContact getSugarContactByLiveTextName(String liveTextName) {
		SugarContact sugarEntry = null;
		List<BaseSugarEntry> entries = getSugarEntries();
		for (BaseSugarEntry entry : entries) {
			if (entry instanceof SugarContact) {
				SugarContact contact = (SugarContact) entry;
				if (contact.getName().equalsIgnoreCase(liveTextName)) {
					sugarEntry = contact;
					break;
				} else if (getLiveTextMatchesCache().containsKey(liveTextName) && contact.getId().equals(getLiveTextMatchesCache().get(liveTextName))) {
					sugarEntry = contact;
					break;
				}
			}
		}
		return sugarEntry;
	}

	/**
	 * Find a sugar account by id
	 * 
	 * @param id
	 * @return
	 */
	public SugarAccount getSugarAccountById(String id) {
		SugarAccount account = null;
		if (id != null && id.length() > 0) {
			List<BaseSugarEntry> entries = getSugarEntries();
			for (BaseSugarEntry entry : entries) {
				if (entry instanceof SugarAccount) {
					SugarAccount tempAccount = (SugarAccount) entry;
					if (tempAccount.getClientId().equalsIgnoreCase(id)) {
						account = tempAccount;
					}
				}
			}
		}
		return account;
	}

	public BaseSugarEntry getSugarEntryByName(String name, SugarType type) {
		BaseSugarEntry sugarEntry = null;
		List<BaseSugarEntry> entries = getSugarEntries();
		for (BaseSugarEntry entry : (List<BaseSugarEntry>) entries) {
			if (entry.getName().equalsIgnoreCase(name)) {
				sugarEntry = entry;
				break;
			}
		}
		return sugarEntry;
	}

	public BaseSugarEntry getSugarEntryById(String id) {
		BaseSugarEntry sugarEntry = null;
		List<BaseSugarEntry> entries = getSugarEntries();
		if (entries != null && !entries.isEmpty()) {
			for (BaseSugarEntry entry : (List<BaseSugarEntry>) entries) {
				if (entry != null && entry.getId().equals(id)) {
					sugarEntry = entry;
					break;
				}
			}
		}
		return sugarEntry;
	}

	private Map<String, String> jsonObjectToMap(JSONObject jsonObj) {
		Map<String, String> map = new HashMap<String, String>();
		try {
			Iterator<String> keyIterator = jsonObj.keys();
			while (keyIterator.hasNext()) {
				String key = keyIterator.next();
				map.put(key, jsonObj.getString(key));
			}
		} catch (JSONException e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}
		return map;
	}

	private Object getObjectFromMap(Map<String, Object> map, String key) {
		Object value = null;
		try {
			value = map.get(key);
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}
		return value;
	}

	private List<String> getStringValuesFromJsonArray(Map<String, Object> map, String key) {
		List<String> values = new ArrayList<String>();
		JSONArray jsonArray = (JSONArray) map.get(key);
		for (int i = 0; i < jsonArray.length(); i++) {
			try {
				values.add(jsonArray.getString(i));
			} catch (JSONException e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
		return values;
	}

	private String getStringValueFromMap(Map<String, Object> map, String key) {
		String value = null;
		try {
			Object val = map.get(key);
			if (val != null && val instanceof String) {
				value = (String) val;
			}
			// 59674
			else if (val != null && val instanceof Integer) {
				value = ((Integer) val).toString();
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}
		return value;
	}

	private Integer getIntValueFromMap(Map<String, Object> map, String key) {
		Integer value = null;
		try {
			Object val = map.get(key);
			if (val != null && val instanceof Integer) {
				value = (Integer) val;
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}
		return value;
	}

	private Boolean getBooleanValueFromMap(Map<String, Object> map, String key) {
		Boolean value = null;
		try {
			Object val = map.get(key);
			if (val != null && val instanceof Boolean) {
				value = (Boolean) val;
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}

		return value;
	}

	public List<BaseSugarEntry> getSugarEntries() {
		if (sugarEntries == null) {
			sugarEntries = new ArrayList<BaseSugarEntry>();
		}
		return sugarEntries;
	}

	public Set<SugarEntrySurrogate> getFavorites() {
		return favorites;
	}

	public void setFavorites(Set<SugarEntrySurrogate> favorites) {
		this.favorites = favorites;
	}

	/**
	 * Returns a session id that is valid for the sugar server
	 * 
	 * @param refresh
	 * @return
	 */
	private boolean alreadyCalled = false;

	public String getSessionId(boolean refresh) {
		if (alreadyCalled || (lastSession != null && !lastSession.equals(ConstantStrings.EMPTY_STRING) && isCredentialPromptOpen())) {
		} else {
			if (lastSession.equals(ConstantStrings.EMPTY_STRING) || refresh) {
				Job job = new Job("Get Session Id") //$NON-NLS-1$
				{
					@Override
					protected IStatus run(IProgressMonitor arg0) {
						resetState();

						alreadyCalled = true;
						String jsonString = getSessionFromWebservice();
						alreadyCalled = false;

						// if unknownHost, the error exception was caught and
						// printed in getSessionFromWebservice(),
						// so no need to continue in this block.
						// otherwise, check if this is a valid JSON Object and
						// extract valid session and user inf, if it is valid.
						if (!serverIncorrect) {
							try {
								JSONObject object = null;
								try {
									object = new JSONObject(jsonString);
								} catch (Exception e) {
									// If we get stuff back that isn't JSON, it probably means that there is a connection problem.
									unableToConnect = true;
									// No need to print exception here, because it's been done in getSessionFromWebservice().
								}
								if (object != null) {
									JSONObject header = object.getJSONObject("header"); //$NON-NLS-1$
									lastSession = header.getString("sessionid"); //$NON-NLS-1$            

									setUserCNUM(header.getString("user_id")); //$NON-NLS-1$
									setUserFullName(header.getString("user_full_name")); //$NON-NLS-1$            
								}
							} catch (Exception e) {
								// If we get JSON back without a header, it probably means we couldn't login (invalid credential)
								// or a connection problem.
								if (isInvalidCredential(jsonString)) {
									unableToLogin = true;
									if (toPromptCredential()) {
										getPropertyChangeSupport().firePropertyChange(BRING_UP_CREDENTIAL_PROMPT, true, false);
									}

								} else {
									unableToConnect = true;
								}
								// No need to print exception here, because it's been done in getSessionFromWebservice().
							}
						}

						// If we successfully connect, reset the setting that says "Don't show me the server popup dialog)
						if (!unableToLogin && !serverIncorrect && !unableToConnect) {
							Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
							prefs.setValue(CorePluginActivator.PREFERENCES_SHOW_CONNECTION_ERRORS, Boolean.toString(true));
							CorePluginActivator.getDefault().savePluginPreferences();
						}
						return Status.OK_STATUS;
					}
				};

				job.schedule();
				try {
					job.join();
				} catch (InterruptedException e) {
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
			}
		}
		return lastSession;
	}

	/**
	 * This method builds leads url for Sugar 7
	 * 
	 * @return
	 */
	public String buildV10LeadsSeamlessURL(String leadid) {
		String s1 = NotesAccountManager.getInstance().getCRMServer(); //$NON-NLS-1$
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		sb.append(s1).append("#Leads/").append(leadid); //$NON-NLS-1$  
		return sb.toString();
	}

	/**
	 * This method takes a URL and adds the oauth tokens to it to work with Sugar 7 + oauth
	 * 
	 * @return
	 */
	public String buildV10SeamlessURL(String aUrl) {
		final String[] rebuiltUrl = new String[1];
		final String urlToTokenize = aUrl;

		Job job = new Job("Build SalesConnect direct URL") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				try {
					rebuiltUrl[0] = SugarV10APIManager.getInstance().buildSeamlessURL(urlToTokenize);
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
				return Status.OK_STATUS;
			}
		};

		job.schedule();
		try {
			job.join();
		} catch (InterruptedException e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}

		return rebuiltUrl[0];
	}

	/**
	 * This method takes a URL and adds the oauth tokens to it to work with Sugar 7 sidecar module + oauth
	 * 
	 * @return
	 */
	public String buildV10SeamlessSidecarURL(String aUrl) {
		final String[] rebuiltUrl = new String[1];
		final String urlToTokenize = aUrl;

		Job job = new Job("Build SalesConnect direct sidecar URL") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				try {
					rebuiltUrl[0] = SugarV10APIManager.getInstance().buildSeamlessSidecarURL(urlToTokenize);
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
				return Status.OK_STATUS;
			}
		};

		job.schedule();
		try {
			job.join();
		} catch (InterruptedException e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}

		return rebuiltUrl[0];
	}

	/**
	 * This method returns a session from sugar with affinity to the client. Sessions obtained through getSession() have affinity to the server. This is fine if we're using the server to proxy
	 * subsequent calls. But for things like seamless sessions, we need the login to happen from the client since the corresponding request will also come from the client.
	 * 
	 * @return
	 */
	public String getSeamlessClientSession() {
		final String[] localSession = new String[1];
		final String[] output = new String[1];

		Job job = new Job("Get Session Id") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				try {
					HttpClient client = getProxiedHttpClient();
					PostMethod post = new PostMethod(NotesAccountManager.getInstance().getCustomRestURL());
					String request = "method=login&input_type=JSON&response_type=JSON&rest_data={\"user_auth\":" + //$NON-NLS-1$
							"{\"encryption\":\"PLAIN\"," + //$NON-NLS-1$
							"\"user_name\":\"" + NotesAccountManager.getInstance().getCRMUser() + "\"," + //$NON-NLS-1$ //$NON-NLS-2$
							"\"password\":\"" + NotesAccountManager.getInstance().getCRMPassword() + "\"}}"; //$NON-NLS-1$ //$NON-NLS-2$
					post.setRequestEntity(new StringRequestEntity(request, "application/x-www-form-urlencoded", "UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$

					client.executeMethod(post);
					output[0] = post.getResponseBodyAsString();
					UtilsPlugin.getDefault().logInfoMessage(WebServiceLogUtil.getDebugMsg(post, output[0], post.getURI().toString(), 0, 0), CorePluginActivator.PLUGIN_ID);

					try {
						JSONObject response = new JSONObject(output[0]);
						localSession[0] = response.getString("id"); //$NON-NLS-1$

						// Make the session seamless to support use in URLs
						post = new PostMethod(NotesAccountManager.getInstance().getSugarRestURL()); // Gotta use the base rest url for the seamless call
						request = "method=seamless_login&input_type=JSON&response_type=JSON&rest_data={" + //$NON-NLS-1$
								"\"session\" : \"" + localSession[0] + "\"}"; //$NON-NLS-1$ //$NON-NLS-2$
						post.setRequestEntity(new StringRequestEntity(request, "application/x-www-form-urlencoded", "UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$
						client.executeMethod(post);
						// System.out.println(post.getResponseBodyAsString());
					} catch (Exception e) {
						// If we get stuff back that isn't JSON, it probably means that there is a connection problem.
						unableToConnect = true;
					}
					if (post.getStatusCode() != 200) {
						UtilsPlugin.getDefault().logErrorMessage(output[0], CorePluginActivator.PLUGIN_ID);
					}
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
				return Status.OK_STATUS;
			}
		};

		job.schedule();
		try {
			job.join();
		} catch (InterruptedException e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}

		return localSession[0];
	}

	private String maybeInsertSessionId(String aRequest) {
		/**
		 * If the request does not have a sessionid included, it is updated to include the current sessionid
		 */
		String requestWithSession = "";
		if (aRequest.indexOf("sessionid") == -1) {
			int methodIdx = aRequest.indexOf("&method");
			if (methodIdx > -1) {
				requestWithSession = aRequest.substring(0, methodIdx) + "&sessionid=" + getSessionId(false) + aRequest.substring(methodIdx);
			} else {
				requestWithSession = aRequest;
			}
		} else {
			requestWithSession = aRequest;
		}
		return requestWithSession;
	}

	/**
	 * Calls a native sugar web service (get_entry, set_entry, whatever) - for Restful request. This method uses application/x-www-form-urlencoded content type. If you have special characters (for
	 * example: "&", "+") in the request, use callNativeSugarRestWithMultipart() instead. Currently this method is being used for getting associated email version, and for getting Sugar user
	 * preference.
	 * 
	 * @return
	 */
	public String callNativeSugarRestWebService(String request) {
		String output = null;
		String requestWithSession;

		try {
			requestWithSession = maybeInsertSessionId(request);
			HttpClient client = getProxiedHttpClient();
			PostMethod post = new PostMethod(NotesAccountManager.getInstance().getSFARestServiceURL());
			post.setRequestEntity(new StringRequestEntity(requestWithSession, "application/x-www-form-urlencoded", "UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$

			final String scriptX = "sfaRest.php";
			final String methodX = getMethod(requestWithSession);
			doBeginningLog(scriptX, methodX);

			client.executeMethod(post);
			output = post.getResponseBodyAsString();
			UtilsPlugin.getDefault().logInfoMessage(WebServiceLogUtil.getDebugMsg(post, output, post.getURI().toString(), 0, 0), CorePluginActivator.PLUGIN_ID);

			// System.out.println(WebServiceLogUtil.getDebugMsg(post, output, post.getURI().toString(), 0, 0));
			doEndingLog(scriptX, methodX);

			if (post.getStatusCode() != 200) {
				output = null;
				UtilsPlugin.getDefault().logErrorMessage(output, CorePluginActivator.PLUGIN_ID);
			}
		} catch (Exception e) {
			output = null;
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}
		return output;
	}

	public void clearSession() {
		lastSession = ConstantStrings.EMPTY_STRING;
		SugarV10APIManager.getInstance().flushUserTokens();
	}

	/**
	 * If this is true, is probably means that the user id/pw used to login to the CRM server is incorrect.
	 * 
	 * @return
	 */
	public boolean unableToLogin() {
		return unableToLogin;
	}

	/**
	 * If this is true, is probably means that there is a connection problem.
	 * 
	 * @return
	 */
	public boolean unableToConnect() {
		return unableToConnect;
	}

	/**
	 * If this is true, it most likely means that the server is down or otherwise unreachable.
	 * 
	 * @return
	 */
	public boolean isServerIncorrect() {
		return serverIncorrect;
	}

	/**
	 * Returns true if any sort of error was encountered logging into the server
	 * 
	 * @return
	 */
	public boolean hasConnectionProblem() {
		return unableToLogin() || isServerIncorrect() || unableToConnect();
	}

	public boolean isInvalidSugarEntry() {
		return isInvalidSugarEntry;
	}

	public void resetInvalidSugarEntry(boolean b) {
		isInvalidSugarEntry = b;
	}

	/**
	 * Returns the CNUM of the currently logged in user
	 * 
	 * @return
	 */
	public String getUserCNUM() {
		return userCNUM;
	}

	/**
	 * Sets the users cnum
	 * 
	 * @param cnum
	 */
	private void setUserCNUM(String cnum) {
		userCNUM = cnum;

		// 56713
		if (SFADataHub.getInstance().getDataShare(WebServiceInfoDataShare.SHARE_NAME) == null) {
			SFADataHub.getInstance().addDataShare(new WebServiceInfoDataShare());
		}

		SFADataHub.getInstance().getDataShare(WebServiceInfoDataShare.SHARE_NAME).put(WebServiceInfoDataShare.USER_CNUM, cnum);

	}

	/**
	 * Returns the users full name
	 * 
	 * @return
	 */
	public String getUserFullName() {
		return userFullName;
	}

	private void setUserFullName(String fullName) {
		userFullName = fullName;
		SFADataHub.getInstance().getDataShare(WebServiceInfoDataShare.SHARE_NAME).put(WebServiceInfoDataShare.USER_FULL_NAME, fullName);
	}

	public Map<String, String> getLiveTextMatchesCache() {
		if (liveTextMatchesCache == null) {
			liveTextMatchesCache = new HashMap<String, String>();
		}
		return liveTextMatchesCache;
	}

	public void resetState() {
		unableToConnect = false;
		unableToLogin = false;
		serverIncorrect = false;
	}

	public static PropertyChangeSupport getPropertyChangeSupport() {
		if (propertyChangeSupport == null) {
			propertyChangeSupport = new PropertyChangeSupport(ConstantStrings.EMPTY_STRING);
		}
		return propertyChangeSupport;
	}

	public void setToPromptCredential(boolean b) {
		toPromptCredential = b;
	}

	public boolean toPromptCredential() {
		return toPromptCredential;
	}

	public void setCredentialPromptOpen(boolean b) {
		isCredentialPromptOpen = b;
	}

	public boolean isCredentialPromptOpen() {
		return isCredentialPromptOpen;
	}

	public void addPreferenceChangeListener() {
		if (preferenceChangeListener == null) {
			IEclipsePreferences preferences = new ManagedSettingsScope().getNode("com.ibm.rcp.content");
			preferenceChangeListener = new IEclipsePreferences.IPreferenceChangeListener() {
				@Override
				public void preferenceChange(PreferenceChangeEvent event) {
					if (event != null && event.getKey() != null & event.getNewValue() != null & event.getOldValue() != null) {
						if (event.getKey().equals(SugarWebservicesOperations.LIVETEXT_PREFERENCE_ACCOUNT_CONTENT_TYPE)) {
							Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
							prefs.setValue(SugarWebservicesOperations.SALESCONNECT_LIVETEXT_MATCH_ACCOUNT_PREF_KEY, event.getNewValue().equals("true") ? "true" : "false");
							CorePluginActivator.getDefault().savePluginPreferences();
						} else if (event.getKey().equals(SugarWebservicesOperations.LIVETEXT_PREFERENCE_OPPTY_CONTENT_TYPE)) {
							Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
							prefs.setValue(SugarWebservicesOperations.SALESCONNECT_LIVETEXT_MATCH_OPPTY_PREF_KEY, event.getNewValue().equals("true") ? "true" : "false");
							CorePluginActivator.getDefault().savePluginPreferences();
						} else if (event.getKey().equals(SugarWebservicesOperations.LIVETEXT_PREFERENCE_CONTACT_CONTENT_TYPE)) {
							Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
							prefs.setValue(SugarWebservicesOperations.SALESCONNECT_LIVETEXT_MATCH_CONTACT_PREF_KEY, event.getNewValue().equals("true") ? "true" : "false");
							CorePluginActivator.getDefault().savePluginPreferences();
						}

					}
					Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
					String account = prefs.getString(SugarWebservicesOperations.SALESCONNECT_LIVETEXT_MATCH_ACCOUNT_PREF_KEY);
					String oppty = prefs.getString(SugarWebservicesOperations.SALESCONNECT_LIVETEXT_MATCH_OPPTY_PREF_KEY);
					String contact = prefs.getString(SugarWebservicesOperations.SALESCONNECT_LIVETEXT_MATCH_CONTACT_PREF_KEY);

				}

			};
			preferences.addPreferenceChangeListener(preferenceChangeListener);

		}

	}

	public void removePreferenceChangeListener() {
		IEclipsePreferences preferences = new ManagedSettingsScope().getNode("com.ibm.rcp.content"); //$NON-NLS-1$
		preferences.removePreferenceChangeListener(preferenceChangeListener);
		preferenceChangeListener = null;
	}

	private String getMethod(Map<String, String> additionalParameters) {
		String method = ConstantStrings.EMPTY_STRING;
		String methodX = "method"; //$NON-NLS-1$
		if (additionalParameters != null) {
			if (additionalParameters.get(methodX) != null) {
				method = additionalParameters.get(methodX);
			}
		}
		return method;
	}

	private String getMethod(String parameters) {
		String method = ConstantStrings.EMPTY_STRING;
		String methodX = "method="; //$NON-NLS-1$
		if (parameters != null) {
			for (String param : parameters.trim().split(ConstantStrings.AMPERSAND)) {
				if (param != null && param.indexOf(methodX) > -1) {
					method = param.substring(param.indexOf(methodX) + methodX.length());
				}
			}
		}
		return method;
	}

	public static void doBeginningLog(String scriptX, String methodX) {
		currS = System.currentTimeMillis();
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (scriptX != null || methodX != null) {
			sb.append("\n... BEGINNING calling web service: ").append(scriptX).append(", method:").append(methodX).append(" ...\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		UtilsPlugin.getDefault().logInfoMessage(sb.toString(), CorePluginActivator.PLUGIN_ID);
		// System.out.println(sb.toString());
	}

	public static void doEndingLog(String scriptX, String methodX) {
		long currE = System.currentTimeMillis();
		long elapse = currE - currS;
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (scriptX != null || methodX != null) {
			sb.append("... ENDING calling web service: ").append(scriptX).append(", method:").append(methodX).append(", operation took: ").append(elapse).append("(milliseconds) ... \n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		UtilsPlugin.getDefault().logInfoMessage(sb.toString(), CorePluginActivator.PLUGIN_ID);
		// System.out.println(sb.toString());

	}

	// 63714 - server accept only TLSv1.2 protocol for poodle attack fix
	private SSLContext getSSLContext() {
		SSLContext sslContext = null;;
		try {
			// _sslprotocol is set to TLSv1.2 initially.
			sslContext = SSLContext.getInstance(_sslprotocol); //$NON-NLS-1$
		} catch (NoSuchAlgorithmException e1) {
			test();
			UtilsPlugin.getDefault().logException(e1, CorePluginActivator.PLUGIN_ID);
			UtilsPlugin.getDefault().logInfoMessage("NoSuchAlgorithmException in SugarWebservicesOperations, _sslprotocol" + _sslprotocol, CorePluginActivator.PLUGIN_ID); //$NON-NLS-1$
			System.out.println("NoSuchAlgorithmException in SugarWebservicesOperations, _sslprotocol" + _sslprotocol); //$NON-NLS-1$

			// for example: if MAC JVM does not support tlsv1.2, try tlsv1.
			if (_sslprotocol != null && _sslprotocol.equalsIgnoreCase(TLSV12)) {
				_sslprotocol = TLSV1;
				try {
					sslContext = SSLContext.getInstance(_sslprotocol); //$NON-NLS-1$
				} catch (NoSuchAlgorithmException e2) {
					UtilsPlugin.getDefault().logException(e2, CorePluginActivator.PLUGIN_ID);
					UtilsPlugin.getDefault().logInfoMessage("NoSuchAlgorithmException again in SugarWebservicesOperations, _sslprotocol" + _sslprotocol, CorePluginActivator.PLUGIN_ID); //$NON-NLS-1$
					System.out.println("NoSuchAlgorithmException again in SugarWebservicesOperations, _sslprotocol" + _sslprotocol); //$NON-NLS-1$
				}
			}
		}

		// set up a TrustManager that trusts everything
		try {
			sslContext.init(null, new TrustManager[]{new X509TrustManager() {

				@Override
				public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
					// TODO Auto-generated method stub

				}

				@Override
				public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
					// TODO Auto-generated method stub

				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					// TODO Auto-generated method stub
					return null;
				}

			}}, new SecureRandom());
		} catch (KeyManagementException e1) {
			UtilsPlugin.getDefault().logException(e1, CorePluginActivator.PLUGIN_ID);

		}
		return sslContext;
	}
	private void test() {
		try {
			if (SSLContext.getDefault() != null) {
				UtilsPlugin.getDefault().logInfoMessage("SSLContext.getDefault() != null" + _sslprotocol, CorePluginActivator.PLUGIN_ID); //$NON-NLS-1$
				UtilsPlugin.getDefault().logInfoMessage("SSLContext.getDefault().getProtocol()=" + SSLContext.getDefault().getProtocol(), CorePluginActivator.PLUGIN_ID); //$NON-NLS-1$
				System.out.println("SSLContext.getDefault() != null");
				System.out.println("SSLContext.getDefault().getProtocol()=" + SSLContext.getDefault().getProtocol());
			} else {
				UtilsPlugin.getDefault().logInfoMessage("SSLContext.getDefault() == null", CorePluginActivator.PLUGIN_ID); //$NON-NLS-1$
				System.out.println("SSLContext.getDefault() == null");
			}
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			UtilsPlugin.getDefault().logInfoMessage("how come SSLContext.getDefault() giving NoSuchAlgorithmException", CorePluginActivator.PLUGIN_ID); //$NON-NLS-1$
			System.out.println("how come SSLContext.getDefault() giving NoSuchAlgorithmException");
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}

	}
}
