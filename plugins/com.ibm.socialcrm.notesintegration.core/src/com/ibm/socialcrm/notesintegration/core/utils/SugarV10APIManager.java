package com.ibm.socialcrm.notesintegration.core.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.SugarLead;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

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
 * This class manages access to Sugar's v10 REST api. This class has the ability to post and get to the REST api, and handle the token refreshes automatically.
 * 
 */
public class SugarV10APIManager {
	private static SugarV10APIManager instance;
	public static final int MAX_OPPORTUNITIES_TO_BE_DISPLAYED = 50;
	public static final String OPPTY_SORTED_BY = "date_closed";
	private String oauthAccessToken;
	private String oauthRefreshToken;
	private String oauthDownloadToken;
	private String clientID = "notes";
	private String platform = "apiclient";
	private long refreshExpires = 0;

	private Map<String, String> stageEnumMap;

	private SugarV10APIManager() {
	}
	/**
	 * Clears the various v10 tokens so that a login will be forced on the next request
	 */
	public synchronized void flushUserTokens() {
		oauthAccessToken = "";
		oauthRefreshToken = "";
		oauthDownloadToken = "";
		refreshExpires = 0;
	}
	/**
	 * Updates all of the oauth tokens in one whack. Synchronized in case multiple threads are around as the tokens are interdependent.
	 * 
	 * @param access
	 * @param refresh
	 * @param download
	 * @param refreshExpiresIn
	 */
	private synchronized void updateOauthTokens(String access, String refresh, String download, int refreshExpiresIn) {
		oauthAccessToken = access;
		oauthRefreshToken = refresh;
		oauthDownloadToken = download;
		refreshExpires = System.currentTimeMillis() + refreshExpiresIn * 1000;

	}
	private String getPlatform() {
		return platform;
	}
	private String getClientID() {
		return clientID;
	}
	private String getRefreshToken() {
		return oauthRefreshToken;
	}
	private String getAccessToken() {
		return oauthAccessToken;
	}
	private String getDownloadToken() {
		return oauthDownloadToken;
	}
	private long getRefreshExpires() {
		return refreshExpires;
	}
	/**
	 * Determines if the refresh token is still within the epxire time
	 * 
	 * @return
	 */
	private boolean refreshTokenStillYoung() {
		return (getRefreshExpires() - System.currentTimeMillis()) > 1000;
	}

	/**
	 * Get singleton instance of SugarV10APIManager
	 * 
	 * @return
	 */
	public static SugarV10APIManager getInstance() {
		if (instance == null) {
			instance = new SugarV10APIManager();
		}
		return instance;
	}
	/**
	 * Logs into Sugar via V10 api and stores the oauth tokens
	 * 
	 * @return
	 */
	private boolean login() throws LoginException {
		HttpClient client = getHttpClient();
		PostMethod post = new PostMethod(NotesAccountManager.getInstance().getV10TokenURL());
		String request = "{\"grant_type\":\"password\",\"username\":\"" + NotesAccountManager.getInstance().getCRMUser() + "\",\"password\":\"" + NotesAccountManager.getInstance().getCRMPassword()
				+ "\",\"client_id\":\"" + getClientID() + "\",\"platform\":\"" + getPlatform() + "\",\"client_secret\":\"\"}";
		String responseBody;
		int responseCode;

		try {
			//post.setRequestEntity(new StringRequestEntity(request, "application/x-www-form-urlencoded", "UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$
			post.setRequestEntity(new StringRequestEntity(request, "application/json", "UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$ //defect 112616
			client.executeMethod(post);
			responseCode = post.getStatusCode();
			responseBody = post.getResponseBodyAsString();
			UtilsPlugin.getDefault().logInfoMessage("SugarV10APIManager:login - Oauth login.  Response code: " + responseCode + " response:" + responseBody, CorePluginActivator.PLUGIN_ID);
			if (responseCode == 200) {
				JSONObject jsonObject = null;
				try {
					jsonObject = new JSONObject(responseBody);
					String accessToken = jsonObject.getString("access_token");
					String refreshToken = jsonObject.getString("refresh_token");
					String download = jsonObject.getString("refresh_token");
					int refresh = jsonObject.getInt("refresh_expires_in");
					updateOauthTokens(accessToken, refreshToken, download, refresh);
				} catch (JSONException e) {
					UtilsPlugin.getDefault().logErrorMessage("SugarV10APIManager:login - Failed to parse oauth tokens\n" + responseBody, CorePluginActivator.PLUGIN_ID);
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
				return true;
			} else {
				// credentials or server are bad. Should show the login dialog. Throw a loginException
				throw new LoginException("Failed to login to Sugar.  Response code:" + responseCode);
			}

		} catch (Exception e) {
			if (e instanceof LoginException) {
				throw (LoginException) e;
			} else {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
		return false;
	}
	/**
	 * Parses the oauth tokens out of the response from a login call.
	 * 
	 * @param responseBody
	 * @return
	 */
	private boolean processTokensFromResponse(String responseBody) {
		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(responseBody);
			String accessToken = jsonObject.getString("access_token");
			String refreshToken = jsonObject.getString("refresh_token");
			String download = jsonObject.getString("refresh_token");
			int refresh = jsonObject.getInt("refresh_expires_in");
			updateOauthTokens(accessToken, refreshToken, download, refresh);
			return true;
		} catch (JSONException e) {
			UtilsPlugin.getDefault().logErrorMessage("SugarV10APIManager:processTokensFromResponse - Failed to parse oauth tokens\n" + responseBody, CorePluginActivator.PLUGIN_ID);
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}
		return false;
	}

	/**
	 * Refreshes the access token if the refresh token is still valid. Otherwise performs a login to get new tokens
	 * 
	 * @return
	 */
	private boolean refreshAccessToken() throws LoginException {
		if (refreshTokenStillYoung()) {
			PostMethod aPost;
			String data = "{\"grant_type\":\"refresh_token\",\"client_id\":\"sugar\",\"client_secret\":\"\",\"refresh_token\":\"" + getRefreshToken() + "\",\"platform\":\"base\",\"refresh\":true}";
			aPost = processPostRequest(NotesAccountManager.getInstance().getV10TokenURL(), data, false);
			if (aPost.getStatusCode() != 200) {
				return login();
			} else if (aPost.getStatusCode() == 200) {
				try {
					processTokensFromResponse(aPost.getResponseBodyAsString());
					return true;
				} catch (Exception e) {
					UtilsPlugin.getDefault().logErrorMessage("SugarV10APIManager:refreshAccessToken - Failed to parse oauth tokens", CorePluginActivator.PLUGIN_ID);
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}

			}
		} else {
			return login();
		}
		return false;
	}

	/**
	 * Checks the access token with a simple get request. If expired, it handles the refresh.
	 * 
	 * @return
	 */
	private boolean checkAndRefreshAccessToken() throws LoginException {
		// try a simple request with our current tokens
		GetMethod get = processGetRequest(NotesAccountManager.getInstance().getV10ApiURL() + "/me", false);
		if (get.getStatusCode() != 200) {
			return refreshAccessToken();
		} else {
			return true;
		}
	}

	/**
	 * Builds a "seamless" login URL from a given URL and the current oauth tokens. This capability is now moot with the saml issues that were raised, so build a url in the bwc format with our client
	 * id
	 * 
	 * @param aUrl
	 *        the url Notes would like to open
	 * @return
	 */
	public String buildSeamlessURL(String aUrl) {
		// checkAndRefreshAccessToken();
		return addTokensToURL(aUrl);
		/*
		 * if (checkAndRefreshAccessToken()) { return addTokensToURL(aUrl); } else { UtilsPlugin.getDefault().logErrorMessage("SugarV10APIManager:buildSeamlessURL - Unable to complete oauth login",
		 * CorePluginActivator.PLUGIN_ID); } return ""; //TODO: This should probably throw an exception
		 */
	}

	/**
	 * Takes a URL and processes it into the bwc format using the oauth tokens for "seamless" login. Current SAML solution means we use the client_id and platform, but not any login tokens, so the
	 * user may be challenged, but have full SSO afterward Resulting url format: <host>/index.php?platform=apiclient&client_id=<notes_client_id>#bwc/<old URI>
	 * 
	 * @param aUrl
	 *        the url Notes would like to open
	 * @return
	 */
	private String addTokensToURL(String aUrl) {
		String finishedURL = "";
		try {
			URI aUri = new URI(aUrl, false);
			finishedURL = aUri.getScheme() + "://" + aUri.getAuthority();
			if (aUri.getPort() > 0) {
				finishedURL += ":" + aUri.getPort();
			}
			finishedURL += aUri.getPath() + "?" + "platform=" + getPlatform() + "&client_id=" + getClientID() + "#bwc/index.php?" + aUri.getQuery();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logErrorMessage("SugarV10APIManager:addTokensToURL - Failed to add tokens to url", CorePluginActivator.PLUGIN_ID);
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}
		return finishedURL;
	}

	/**
	 * Builds a "seamless" login sidecar URL from a given URL and the current oauth tokens. This capability is now moot with the saml issues that were raised, so build a url in the sidecar format with
	 * our client id
	 * 
	 * @param aUrl
	 *        the sidecar url Notes would like to open
	 * @return
	 */
	public String buildSeamlessSidecarURL(String aUrl) {
		// checkAndRefreshAccessToken();
		return addTokensToSidecarURL(aUrl);
		/*
		 * if (checkAndRefreshAccessToken()) { return addTokensToURL(aUrl); } else { UtilsPlugin.getDefault().logErrorMessage("SugarV10APIManager:buildSeamlessURL - Unable to complete oauth login",
		 * CorePluginActivator.PLUGIN_ID); } return ""; //TODO: This should probably throw an exception
		 */
	}

	/**
	 * Takes a URL and processes it into the sidecar format using the oauth tokens for "seamless" login. Current SAML solution means we use the client_id and platform, but not any login tokens, so the
	 * user may be challenged, but have full SSO afterward Resulting url format: <host>/index.php?platform=apiclient&client_id=<notes_client_id>#<sugar type>/<id>
	 * 
	 * @param aUrl
	 *        the url Notes would like to open
	 * @return
	 */
	private String addTokensToSidecarURL(String aUrl) {
		String finishedURL = "";
		try {
			URI aUri = new URI(aUrl, false);
			finishedURL = aUri.getScheme() + "://" + aUri.getAuthority();
			if (aUri.getPort() > 0) {
				finishedURL += ":" + aUri.getPort();
			}
			finishedURL += aUri.getPath() + "?" + "platform=" + getPlatform() + "&client_id=" + getClientID() + "#" + aUri.getQuery();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logErrorMessage("SugarV10APIManager:addTokensToURL - Failed to add tokens to sidecar url", CorePluginActivator.PLUGIN_ID);
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}

		return finishedURL;
	}

	/**
	 * Utility method to create a new http client ready for UTF-8
	 * 
	 * @return HttpClient an apache httpclient
	 */
	private HttpClient getHttpClient() {
		HttpClient client = new HttpClient();
		client.getParams().setContentCharset("UTF-8"); //$NON-NLS-1$
		return client;
	}
	/**
	 * Post the data to the v10 api using the provided url. Handle token refreshes.
	 * 
	 * @param url
	 *        the url to post to
	 * @param data
	 *        the data to post
	 * @return a PostMethod httpclient object
	 */
	private PostMethod processPostRequest(String url, String data) throws LoginException {
		// The normal path is to have token refresh handled
		return processPostRequest(url, data, true);
	}

	/**
	 * Post the data to the v10 api using the provided url. Optionally handle token refreshes.
	 * 
	 * @param url
	 *        the url to post to
	 * @param data
	 *        the data to post
	 * @param processRefresh
	 *        if true, an expired token response will trigger a login and a re-attempt
	 * @return
	 */
	private PostMethod processPostRequest(String url, String data, boolean processRefresh) throws LoginException {
		HttpClient client = getHttpClient();
		PostMethod post = new PostMethod(url);
		String request = data;
		String responseBody;
		int responseCode;
		try {
			//post.setRequestEntity(new StringRequestEntity(request, "application/x-www-form-urlencoded", "UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$
			post.setRequestEntity(new StringRequestEntity(request, "application/json", "UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$ //defect 112616
			post.addRequestHeader("OAuth-Token", getAccessToken());
			client.executeMethod(post);
			responseCode = post.getStatusCode();
			responseBody = post.getResponseBodyAsString();
			UtilsPlugin.getDefault().logInfoMessage("SugarV10APIManager:simplePostRestRequest - Response code: " + responseCode + " response:" + responseBody, CorePluginActivator.PLUGIN_ID);
			if (responseCode == 401 && processRefresh) {
				UtilsPlugin.getDefault().logInfoMessage("SugarV10APIManager:simplePostRestRequest - Access expired.  Refreshing...", CorePluginActivator.PLUGIN_ID);
				if (refreshAccessToken()) {
					// refresh successful, post again and return
					client.executeMethod(post);
				}
			}

		} catch (Exception e) {
			if (e instanceof LoginException) {
				throw (LoginException) e;
			} else {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
		return post;
	}
	/**
	 * Process a v10 get request for this uri. Handle token refreshes.
	 * 
	 * @param uri
	 *        the uri to get from the rest api endpoint
	 * @return a String with the response
	 * @throws LoginException
	 */
	public String getURI(String uri) throws LoginException {
		String response = "";
		String url = getV10ApiURL();
		if (uri != null && uri.length() > 0) {
			if (uri.charAt(0) != '/') {
				url += "/";
			}
			url += uri;
		}
		try {
			response = processGetRequest(url, true).getResponseBodyAsString();
		} catch (IOException e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}
		return response;
	}
	/**
	 * Process a v10 get request. Handle token refreshes.
	 * 
	 * @param url
	 *        the url to get
	 * @return a GetMethod object from httpclient
	 */
	private GetMethod processGetRequest(String url) throws LoginException {
		return processGetRequest(url, true);
	}
	/**
	 * Process a v10 get request. Optionally handle token refreshes.
	 * 
	 * @param url
	 *        the url to get
	 * @param processRefresh
	 *        if true, an expired token response will trigger a login and a re-attempt
	 * @return
	 */
	private GetMethod processGetRequest(String url, boolean processRefresh) {
		UtilsPlugin.getDefault().logInfoMessage("SugarV10APIManager:processGetRequest - Request: " + url, CorePluginActivator.PLUGIN_ID);
		// System.out.println("Processing get request:" + url);
		HttpClient client = getHttpClient();
		GetMethod get = new GetMethod(url);
		String responseBody;
		int responseCode;
		try {
			get.addRequestHeader("OAuth-Token", getAccessToken());
			client.executeMethod(get);
			responseCode = get.getStatusCode();
			responseBody = get.getResponseBodyAsString();
			UtilsPlugin.getDefault().logInfoMessage("SugarV10APIManager:processGetRequest - Response code: " + responseCode + " response:" + responseBody, CorePluginActivator.PLUGIN_ID);
			if (responseCode == 401 && processRefresh) {
				UtilsPlugin.getDefault().logInfoMessage("SugarV10APIManager:processGetRequest - Access expired.  Refreshing...", CorePluginActivator.PLUGIN_ID);
				if (refreshAccessToken()) {
					// refresh successful, post again and return
					get.removeRequestHeader("OAuth-Token");
					get.addRequestHeader("OAuth-Token", getAccessToken());
					client.executeMethod(get);
					// 81464 - re-display resposne
					responseCode = get.getStatusCode();
					responseBody = get.getResponseBodyAsString();
					UtilsPlugin.getDefault().logInfoMessage("SugarV10APIManager:processGetRequest(2) - Response code: " + responseCode + " response:" + responseBody, CorePluginActivator.PLUGIN_ID);
				}
			}

		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}
		return get;
	}
	public String getV10ApiURL() {
		return NotesAccountManager.getInstance().getV10ApiURL();
	}
	/**
	 * Provide a similar type-ahead service to the original in SugarWebServicesOperations, only using the v10 api.
	 * 
	 * @param module
	 *        the module to search
	 * @param searchString
	 *        the string of search terms
	 * @return
	 */
	public String getTypeaheadInfoFromWebservice(String module, String searchString) throws LoginException {
		return getTypeaheadInfoFromWebservice(module, searchString, null, null, null);
	}
	public String getTypeaheadInfoFromWebservice(String module, String searchString, String resultLimit, String isMyItems, String filter) throws LoginException {
		String limit = "30";
		String fields = "";
		String myItems = "0";
		String resultBody = "";
		String v4Result = "";
		String url = "";

		if (resultLimit != null && !resultLimit.equals("")) {
			limit = resultLimit;
		}

		if (module != null && module.equals("Accounts")) {
			// 85309
			// fields = "name,id,alt_language_name,ccms_id,indus_class_rollup,billing_address_city,billing_address_state,billing_address_country,ccms_level";
			fields = "name,id,alt_language_name,ccms_id,indus_industry,indus_class_rollup,billing_address_city,billing_address_state,billing_address_country,ccms_level";
		} else if (module != null && module.equals("Contacts")) {
			fields = "name,id,primary_address_city,primary_address_state,primary_address_country,first_name,last_name,salutation,title,alt_lang_first_c,alt_lang_last_c,alt_lang_preferred_name_c,preferred_name_c,account_name,account_id";
		} else if (module != null && module.equals("Leads")) {
			fields = "name,id,primary_address_city,primary_address_state,primary_address_country,first_name,last_name,salutation,title,full_name,account_name,account_id,contact_name,contact_id";
		} else if (module != null && module.equals("Opportunities")) {
			fields = "id,name,description,date_closed";
		}
		// 75177
		else if (module != null && module.equals("Users")) {
			fields = "id,preferred_name,first_name,last_name";
		}

		// 2015-03-16 defect 55417 v10 filter not working for myitem is fixed, so, use filter API for both
		// myitem or not myitem type ahead search.
		// 2015-03-31 Woops, 55417 seems still not fixed for EU contacts/opportunities/clients. Back to the
		// following logic. ( and keep the new code commented out below)
		if (isMyItems != null && isMyItems.toLowerCase().equals("true")) { //$NON-NLS-1$
			myItems = "1";
			// it seems, for lead, ?q is not working when my_items is true... so, use filter logic instead.
			if (module != null && module.equals("Leads")) {
				url = prepareTypeaheadRequestWithFilter(module, searchString, fields, limit, myItems, filter);
			} else {
				// TODO: This should change to use the /filter api once that is working with myitems (open defect 55417). It would allow the city filter to work in the search
				url = prepareTypeaheadRequestWithGlobalSearch(module, searchString, fields, limit, myItems);
			}
		} else {
			url = prepareTypeaheadRequestWithFilter(module, searchString, fields, limit, myItems, filter);
		}

		// if (isMyItems != null) {
		//			if (isMyItems.toLowerCase().equals("true")) { //$NON-NLS-1$
		// myItems = "1";
		// } else {
		// myItems = "0";
		// }
		// url = prepareTypeaheadRequestWithFilter(module, searchString, fields, limit, myItems, filter);
		// }

		try {
			System.out.println("Search String: " + searchString + " v10 request:" + url);
			GetMethod result = processGetRequest(url);
			resultBody = result.getResponseBodyAsString();
			v4Result = typeaheadV4ResponseFromV10Results(module, resultBody, fields);
			// System.out.println("v10 response:" + resultBody);
		} catch (Exception e) {
			if (e instanceof LoginException) {
				throw (LoginException) e;
			} else {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
		return v4Result;
	}

	public String getEnumFromAPI(String module, String enumField) throws LoginException {
		String url = getV10ApiURL() + "/" + module + "/enum/" + enumField; //$NON-NLS-1$  //$NON-NLS-2$
		String results = ConstantStrings.EMPTY_STRING;
		try {
			// System.out.println("Search String: " + searchString + " v10 request:" + url);
			GetMethod result = processGetRequest(url);
			results = result.getResponseBodyAsString();
			// System.out.println("v10 response:" + results);
		} catch (Exception e) {
			if (e instanceof LoginException) {
				throw (LoginException) e;
			} else {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}

		return results;
	}

	/**
	 * Prepare a type ahead request using the v10 global search api, which supports myitems. This api does not allow for additional conditions, so the city filter is not possible. This api finds hits
	 * in any elastic field and splits the string into words and ORs them. It casts a wide net.
	 * 
	 * @param module
	 *        the module to search. Expected values are 'Accounts', 'Opportunities' and 'Contacts'.
	 * @param searchString
	 *        the string we are searching for
	 * @param fields
	 *        the fields that should be included in the response
	 * @param limit
	 *        the number of responses to return
	 * @param myItems
	 *        limit the search to myItems. Expect value is 'true' or 'false'.
	 * @return
	 */
	private String prepareTypeaheadRequestWithGlobalSearch(String module, String searchString, String fields, String limit, String myItems) {
		String encodedSearchString;
		encodedSearchString = tryToUrlEncode(searchString);
		String url = getV10ApiURL() + "/search?q=" + encodedSearchString + "&max_num=" + limit + "&offset=0&fields=" + fields + "&my_items=" + myItems + "&module_list=" + module;
		return url;
	}
	/**
	 * Prepare a type ahead request using the v10 filter api, which does not support myitems at the moment. This api acts on a single module and filters the results with conditions. It allows control
	 * of multiple conditions, enabling searching on ccms_id, name, alt_language_name and filtering by billing_address_city. Words are ANDed together, so a match must include all words provided, which
	 * should yield very precise results.
	 * 
	 * @param module
	 *        the module to search. Expected values are 'Accounts', 'Opportunities' and 'Contacts'.
	 * @param searchString
	 *        the string we are searching for
	 * @param fields
	 *        the fields that should be included in the response
	 * @param limit
	 *        the number of responses to return
	 * @param myItems
	 *        limit the search to myItems. Expect value is 'true' or 'false'.
	 * @return
	 */
	private String prepareTypeaheadRequestWithFilter(String module, String searchString, String fields, String limit, String myItems, String filter) {
		String filterTerms = "";
		if (module.equals("Accounts")) {
			filterTerms = buildNameFilterForField("filter[0][$or][0]", "name", searchString);
			filterTerms += "&" + buildNameFilterForField("filter[0][$or][1]", "alt_language_name", searchString);
			// check match of ccms_id as well
			filterTerms += "&filter[0][$or][2][ccms_id][$contains]=" + tryToUrlEncode(searchString.trim());

			// limit by city if needed
			if (filter != null && !filter.equals("")) {
				filterTerms += "&" + buildNameFilterForField("filter[1]", "billing_address_city", filter);
			}
		} else if (module.equals("Opportunities")) {
			// filterTerms = buildNameFilterForField("filter[0][$or][0]","name",searchString);
			filterTerms += "filter[0][$or][0][name][$contains]=" + tryToUrlEncode(searchString.trim());
			filterTerms += "&" + buildNameFilterForField("filter[0][$or][1]", "description", searchString);
		} else if (module.equals("Contacts")) {
			// TODO: Can switch to name search when this is fixed. Need to do it the hard way for now
			// defect 55605: Contacts filter api returns unrelated results when searching on name
			filterTerms = buildContactNameFilter("filter[0]", searchString);

		} else if (module.equals("Leads")) {
			// TODO: Can switch to name search when this is fixed. Need to do it the hard way for now
			// defect 55605: Contacts filter api returns unrelated results when searching on name
			filterTerms = buildLeadNameFilter("filter[0]", searchString);

		}
		// 75177
		else if (module.equals("Users")) {
			// TODO: Can switch to name search when this is fixed. Need to do it the hard way for now
			// defect 55605: Contacts filter api returns unrelated results when searching on name
			filterTerms = buildUserNameFilter("filter[0]", searchString);

		}

		// Apache HTTPClient doesn't like the [] in a url, but Sugar won't work if you urlencode the whole query string, so
		// selectively replace [ with %5B and ] with %5D which seems to make everyone happy
		filterTerms = filterTerms.replaceAll("\\[", "%5B");
		filterTerms = filterTerms.replaceAll("\\]", "%5D");
		String url = getV10ApiURL() + "/" + module + "/filter?" + filterTerms + "&max_num=" + limit + "&offset=0&fields=" + fields + "&my_items=" + myItems;/* + myItems */// TODO: enable myitems once
		// it works
		// with the filter api
		return url;
	}
	/**
	 * Utility method for building the filter api parameters from a string of words Words will be split and added as $and conditions to the field provided
	 * 
	 * @param prefix
	 *        the current filter prefix that we are adding these conditions to
	 * @param field
	 *        the field we want to search with this list of values
	 * @param searchString
	 *        the search string the user entered that will be split
	 */
	private String buildNameFilterForField(String prefix, String field, String searchString) {
		String filterTerms = "";
		// break up search string into terms
		StringTokenizer st = new StringTokenizer(searchString);
		int tokenNum = 0;
		boolean andIsNeeded = st.countTokens() > 1; // we need an and operator if there are multiple words

		boolean first = true;
		while (st.hasMoreTokens()) {
			if (!first) {
				filterTerms += "&";
			}
			String token = st.nextToken().trim();
			filterTerms += prefix;
			if (andIsNeeded) {
				filterTerms += "[$and][" + tokenNum + "]";
			}
			filterTerms += "[" + field + "][$contains]=" + tryToUrlEncode(token);
			tokenNum++;
			first = false;
		}
		return filterTerms;
	}
	/**
	 * Utility method for building the filter api parameters for a contact from a string of words Words will be split and added as $or conditions across the contact name search fields
	 * 
	 * @param prefix
	 *        the current filter prefix that we are adding these conditions to
	 * @param searchString
	 *        the search string the user entered that will be split
	 */
	private String buildContactNameFilter(String prefix, String searchString) {
		String filterTerms = "";
		// break up search string into terms
		StringTokenizer st = new StringTokenizer(searchString);
		int tokenNum = 0;
		boolean andIsNeeded = st.countTokens() > 1; // we need an and operator if there are multiple words

		boolean first = true;
		String andString = "";
		while (st.hasMoreTokens()) {
			if (!first) {
				filterTerms += "&";
			}
			if (andIsNeeded) {
				andString = "[$and][" + tokenNum + "]";
			}
			String token = tryToUrlEncode(st.nextToken().trim());
			filterTerms += prefix + andString + "[$or][0][first_name][$contains]=" + token;
			filterTerms += "&" + prefix + andString + "[$or][1][last_name][$contains]=" + token;
			filterTerms += "&" + prefix + andString + "[$or][2][preferred_name_c][$contains]=" + token;
			filterTerms += "&" + prefix + andString + "[$or][3][alt_lang_first_c][$contains]=" + token;
			filterTerms += "&" + prefix + andString + "[$or][4][alt_lang_last_c][$contains]=" + token;
			filterTerms += "&" + prefix + andString + "[$or][5][alt_lang_preferred_name_c][$contains]=" + token;
			tokenNum++;
			first = false;
		}
		return filterTerms;
	}

	// 75177
	private String buildUserNameFilter(String prefix, String searchString) {
		String filterTerms = "";
		if (searchString == null || searchString.equals(ConstantStrings.EMPTY_STRING)) {
			return filterTerms;
		}
		// break up search string into terms
		StringTokenizer st = new StringTokenizer(searchString);
		int tokenNum = 0;
		boolean andIsNeeded = st.countTokens() > 1; // we need an and operator if there are multiple words

		boolean first = true;
		String andString = "";
		while (st.hasMoreTokens()) {
			if (!first) {
				filterTerms += "&";
			}
			if (andIsNeeded) {
				andString = "[$and][" + tokenNum + "]";
			}
			String token = tryToUrlEncode(st.nextToken().trim());
			filterTerms += prefix + andString + "[$or][0][first_name][$contains]=" + token;
			filterTerms += "&" + prefix + andString + "[$or][1][last_name][$contains]=" + token;
			filterTerms += "&" + prefix + andString + "[$or][2][preferred_name][$contains]=" + token;
			tokenNum++;
			first = false;
		}
		return filterTerms;
	}
	/**
	 * Utility method for building the filter api parameters for a lead from a string of words Words will be split and added as $or conditions across the lead name search fields
	 * 
	 * @param prefix
	 *        the current filter prefix that we are adding these conditions to
	 * @param searchString
	 *        the search string the user entered that will be split
	 */
	private String buildLeadNameFilter(String prefix, String searchString) {
		String filterTerms = "";
		// break up search string into terms
		StringTokenizer st = new StringTokenizer(searchString);
		int tokenNum = 0;
		boolean andIsNeeded = st.countTokens() > 1; // we need an and operator if there are multiple words

		boolean first = true;
		String andString = "";
		while (st.hasMoreTokens()) {
			if (!first) {
				filterTerms += "&";
			}
			if (andIsNeeded) {
				andString = "[$and][" + tokenNum + "]";
			}
			String token = tryToUrlEncode(st.nextToken().trim());
			// 69360 - use name_forward (instead of first_name or last_name) to facilitate federated search
			filterTerms += prefix + andString + "[name_forward][$contains]=" + token;

			tokenNum++;
			first = false;
		}
		return filterTerms;
	}

	/**
	 * Utility method to attempt a URLEncode (UTF-8) without having to catch it everywhere
	 * 
	 * @param somethingToEncode
	 *        a String to URLEncode
	 */
	private String tryToUrlEncode(String somethingToEncode) {
		try {
			return URLEncoder.encode(somethingToEncode, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// just use it as is
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}
		return somethingToEncode;
	}
	/**
	 * Utility method to convert a v10 filter or global search response into the json format the older v4 users expect.
	 * 
	 * @param module
	 *        the module of the data received
	 * @param v10Result
	 *        the json string with the raw v10 api response
	 * @param fields
	 *        a comma delimeted list of fields that need to be copied to the v4 response
	 */
	private String typeaheadV4ResponseFromV10Results(String module, String v10Result, String fields) throws JSONException {
		// convert fields string to ArrayList
		StringTokenizer st = new StringTokenizer(fields, ",");
		ArrayList fieldsToCopy = new ArrayList(st.countTokens());
		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();
			// filter out name from the list since we handle that specially
			if (!token.equals("name")) {
				fieldsToCopy.add(token);
			}
		}
		JSONObject v4Response = new JSONObject();
		JSONObject result = new JSONObject(v10Result);
		JSONArray records = result.getJSONArray("records");

		JSONObject v4Results = new JSONObject();
		v4Results.put("totalCount", records.length());
		JSONArray v4Fields = new JSONArray();

		Iterator i = records.iterator();
		while (i.hasNext()) {
			JSONObject v10 = (JSONObject) i.next();
			JSONObject v4 = new JSONObject();
			v4.put("module", v10.getString("_module"));
			if (module.equals("Accounts")) {
				v4.put("name", buildAccountName(v10));
			} else if (module.equals("Opportunities")) {
				v4.put("name", v10.getString("name"));
			} else if (module.equals("Leads")) { // for Leads, use the same logic as Contact's
				v4.put("name", buildContactName(v10));
			} // 75177
			else if (module.equals("Users")) {
				v4.put("name", buildUserName(v10));
			} else {
				v4.put("name", buildContactName(v10));
			}
			copyJSONValues(v10, v4, fieldsToCopy);
			// TODO: deal with industry somehow if any clients care
			v4Fields.add(v4);
		}

		v4Response.put("header", "");
		v4Results.put("fields", v4Fields);
		v4Response.put("results", v4Results);

		return v4Response.toString();
	}
	/**
	 * Utility method to copy a list of values from one jsonobject to another
	 * 
	 * @param from
	 *        the source JSONObject
	 * @param to
	 *        the destination JSONObject
	 * @param keys
	 *        a list of Strings with the key names to copy
	 */
	private void copyJSONValues(JSONObject from, JSONObject to, ArrayList keys) throws JSONException {
		for (int i = 0; i < keys.size(); i++) {
			to.put(keys.get(i), from.get(keys.get(i)));
		}

	}
	/**
	 * Utility method to build a contact name from a v10 JSONObject with the raw data
	 * 
	 * @param v10
	 *        the JSONObject with contact info
	 */
	private String buildContactName(JSONObject v10) throws JSONException {
		String firstName = v10.getString("first_name");
		String lastName = v10.getString("last_name");
		String clientName = v10.getString("account_name");
		String output = SugarDashboardPreference.getInstance().getFormattedNameWithoutSalutation(firstName, lastName);
		if (clientName != null && !clientName.equals("")) {
			output += " (" + clientName + ")";
		}
		return output;
	}

	// 75177
	/**
	 * Utility method to build a contact name from a v10 JSONObject with the raw data
	 * 
	 * @param v10
	 *        the JSONObject with contact info
	 */
	private String buildUserName(JSONObject v10) throws JSONException {
		String firstName = v10.getString("first_name");
		String lastName = v10.getString("last_name");
		String output = SugarDashboardPreference.getInstance().getFormattedNameWithoutSalutation(firstName, lastName);

		return output;
	}
	/**
	 * Utility method to build an account name from a v10 JSONObject with the raw data
	 * 
	 * @param v10
	 *        the JSONObject with account info
	 */
	private String buildAccountName(JSONObject v10) throws JSONException {
		String name = v10.getString("name");
		String city = v10.getString("billing_address_city");
		String ccmsid = v10.getString("ccms_id");

		String altName = v10.getString("alt_language_name");

		// the name will have alt_lang_name in parens if it exists. That gets long, so let's drop that if it is the same as the regular name
		String namePortion = "";
		String altPortion = "";
		int openParenIdx = name.indexOf('(');
		int closeParenIdx = name.indexOf(')', openParenIdx);
		if (openParenIdx > 0 && closeParenIdx > openParenIdx) {
			// there is an alt name in parens in the name. Remove it if it is identical
			namePortion = name.substring(0, openParenIdx).trim();
			altPortion = name.substring(openParenIdx + 1, closeParenIdx).trim();
			if (namePortion.equals(altPortion)) {
				name = namePortion;
			}
		} else if (openParenIdx < 0 && altName != null && !altName.equals("") && !altName.equals(name)) {
			// there was no alt name in the name param, and there is an alt name, and it differs from the name
			// add in the alt name
			name += " (" + altName + ")";
		}
		String output = name;
		// append the city and mpp_num
		if (!city.equals("")) {
			output += " - " + city;
		}
		if (!ccmsid.equals("")) {
			output += " - " + ccmsid;
		}

		return output;
	}

	public List<BaseSugarEntry> getInfoFromWebservice(String module, String[] searchIds) throws LoginException {

		String fields = "";
		String resultBody = "";
		String v4Result = "";
		String url = "";
		List<BaseSugarEntry> list = null;

		if (module != null && module.equals("Leads")) {
			fields = "name,id,email1,email_opt_out";
		}

		url = prepareGetInfoRequestWithFilter(module, searchIds, fields);

		try {
			// System.out.println("getInfo v10 request url:" + url);
			GetMethod result = processGetRequest(url);
			resultBody = result.getResponseBodyAsString();
			list = getInfoResponseFromV10Results(module, resultBody);
			// System.out.println("v10 response:" + resultBody);
		} catch (Exception e) {
			if (e instanceof LoginException) {
				throw (LoginException) e;
			} else {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
		return list;
	}

	private List<BaseSugarEntry> getInfoResponseFromV10Results(String module, String resultBody) {

		List<BaseSugarEntry> list = new ArrayList<BaseSugarEntry>();

		try {
			JSONObject v4Response = new JSONObject();
			JSONObject result = new JSONObject(resultBody);
			JSONArray records = result.getJSONArray("records");

			JSONObject v4Results = new JSONObject();
			v4Results.put("totalCount", records.length());

			Iterator i = records.iterator();
			while (i.hasNext()) {
				JSONObject v10 = (JSONObject) i.next();
				// v4.put("module", v10.getString("_module"));
				if (module.equals("Leads")) {
					String id = v10.getString("id");
					String name = v10.getString("name");
					String email = v10.getString("email1");

					// if email is blank or empty
					// boolean emailSuppressed = v10.getBoolean("email_opt_out");
					boolean emailSuppressed = false;
					if (email == null || email.equals(ConstantStrings.EMPTY_STRING)) {
					} else {
						emailSuppressed = v10.getBoolean("email_opt_out");
					}

					BaseSugarEntry sugarEntry = new SugarLead(id, name);
					sugarEntry.setEmail(email);
					sugarEntry.setEmailSuppressed(emailSuppressed);
					list.add(sugarEntry);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	/**
	 * Prepare a getinfo request using the v10 filter api. example:
	 * https://sugarCRMURL/rest/v10/Leads/filter?filter[0][$or][0][id]=36c222fe3c5df84&filter[0][$or][1
	 * ][id]=7117887912b362a&fields=name,id,first_name,last_name,email1
	 * 
	 * @param module
	 *        the module to search. Expected values is "Leads".
	 * @param searchIds
	 *        the IDs we are searching for
	 * @param fields
	 *        the fields that should be included in the response
	 * @return
	 */
	private String prepareGetInfoRequestWithFilter(String module, String[] searchIds, String fields) {
		String filterTerms = "";
		if (module.equals("Leads")) {
			filterTerms = buildLeadGetInfoFilter(searchIds);
		}

		// Apache HTTPClient doesn't like the [] in a url, but Sugar won't work if you urlencode the whole query string, so
		// selectively replace [ with %5B and ] with %5D which seems to make everyone happy
		filterTerms = filterTerms.replaceAll("\\[", "%5B");
		filterTerms = filterTerms.replaceAll("\\]", "%5D");
		String url = getV10ApiURL() + "/" + module + "/filter?" + filterTerms + "&fields=" + fields;
		return url;
	}

	/**
	 * Utility method for building the getInfo filter for leads. example: filter[0][$or][0][id]=36c222fe3c5df84&filter[0][$or][1][id]=7117887912b362a
	 * 
	 * @param searchIds
	 *        the lead ids for getInfo
	 */
	private String buildLeadGetInfoFilter(String[] searchIds) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		String filter1X = "filter[0][$or][";
		String filter2X = "][id]=";

		boolean first = true;
		String andString = "";
		for (int i = 0; i < searchIds.length; i++) {
			if (first) {
				first = false;
			} else {
				sb.append(ConstantStrings.AMPERSAND);
			}
			sb.append(filter1X).append(i).append(filter2X).append(searchIds[i]);

		}
		return sb.toString();
	}

	/**
	 * Provide a similar type-ahead service to the original in SugarWebServicesOperations, only using the v10 api.
	 * 
	 * @param module
	 *        the module to search
	 * @param searchString
	 *        the string of search terms
	 * @return
	 */
	public String getGlobalTypeaheadInfoFromWebservice(String searchString) throws LoginException {
		return getGlobalTypeaheadInfoFromWebservice(searchString, null, "1", null);
	}
	public String getGlobalTypeaheadInfoFromWebservice(String searchString, String resultLimit, String myItems, String filter) throws LoginException {
		String limit = "30";
		String fields = "";
		String resultBody = "";
		String url = "";
		String module = "Accounts%2cContacts%2cOpportunities";

		if (resultLimit != null && !resultLimit.equals("")) {
			limit = resultLimit;
		}

		// 86811 - be sure there's no space in the fields, and adding indus_class_name for ci simplification
		// 85309
		// fields =
		// "name,id,ccms_id,indus_class_rollup,billing_address_city,billing_address_state,billing_address_country,ccms_level,industry,primary_address_city,primary_address_state,primary_address_country,first_name,last_name,title,account_name,description,date_closed";
		fields = "name,id,ccms_id,indus_industry,indus_class_name,indus_class_rollup,billing_address_city,billing_address_state,billing_address_country,ccms_level,industry,primary_address_city,primary_address_state,primary_address_country,first_name,last_name,title,account_name,description,date_closed";

		// TODO: This should change to use the /filter api once that is working with myitems (open defect 55417). It would allow the city filter to work in the search
		url = prepareTypeaheadRequestWithGlobalSearch(module, searchString, fields, limit, myItems);

		try {
			// System.out.println("Search String: " + searchString + " v10 request:" + url);
			GetMethod result = processGetRequest(url);
			resultBody = result.getResponseBodyAsString();
			// System.out.println("v10 response:" + resultBody);
		} catch (Exception e) {
			if (e instanceof LoginException) {
				throw (LoginException) e;
			} else {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
		return resultBody;
	}

	// 81013 - calling v10 API -> convert to v4 API format -> put the result in the return map, set the map key to true indicating
	// the calling method can use this result. If set the map key to false, the calling methind should ignore this result, and call
	// v4 API instead.
	public Map<Boolean, String> getInfo13(String id, String inputDataType, SugarWebservicesOperations.GetInfo13RestulType resultType, String resultTag) throws LoginException {
		Map v10ResultMap = new HashMap<Boolean, String>();
		v10ResultMap.put(Boolean.valueOf(false), null);

		JSONObject v10JSONObject = null;
		if (id != null && inputDataType != null && resultTag != null) {
			String resultTypeX = null;
			if (inputDataType != null) {
				if (inputDataType.equalsIgnoreCase("accountid")) { //$NON-NLS-1$
					switch (resultType) {
						case BASECARD :
							v10JSONObject = getAccountBasecard(id, inputDataType, resultType, resultTag);
							v10ResultMap.clear();
							// If you comment out the (true) line, and un-comment the (false) line, you will be able to:
							// - see the re-constructed v4 result (from v10 api), and
							// - display the card with the orig. v4 API result
							// This can be used for testing to compare the 2 results... but before you check in the code, be sure
							// to un-comment the (true) line, and comment out the (false) line.
							v10ResultMap.put(Boolean.valueOf(true), convertGetinfo13AccountBasecardToV4Response(id, v10JSONObject));
							// v10ResultMap.put(Boolean.valueOf(false), convertGetinfo13AccountBasecardToV4Response(id, v10JSONObject));
							break;
						case FOLLOWED :
							resultTypeX = "followed"; //$NON-NLS-1$
							break;
						case OPPTIES :
							v10JSONObject = getAccountOppty(id, inputDataType, resultType, resultTag);
							v10ResultMap.clear();
							// If you comment out the (true) line, and un-comment the (false) line, you will be able to:
							// - see the re-constructed v4 result (from v10 api), and
							// - display the card with the orig. v4 API result
							// This can be used for testing to compare the 2 results... but before you check in the code, be sure
							// to un-comment the (true) line, and comment out the (false) line.
							v10ResultMap.put(Boolean.valueOf(true), convertGetinfo13AccountOpptyToV4Response(id, v10JSONObject));
							// v10ResultMap.put(Boolean.valueOf(false), convertGetinfo13AccountOpptyToV4Response(id, v10JSONObject));
							break;
					};
				} else if (inputDataType.equalsIgnoreCase("opptyid")) { //$NON-NLS-1$
					switch (resultType) {
						case BASECARD :
							v10JSONObject = getOpptyBasecard(id, inputDataType, resultType, resultTag);
							v10ResultMap.clear();
							// If you comment out the (true) line, and un-comment the (false) line, you will be able to:
							// - see the re-constructed v4 result (from v10 api), and
							// - display the card with the orig. v4 API result
							// This can be used for testing to compare the 2 results... but before you check in the code, be sure
							// to un-comment the (true) line, and comment out the (false) line.
							v10ResultMap.put(Boolean.valueOf(true), convertGetinfo13OpptyBasecardToV4Response(id, v10JSONObject));
							// v10ResultMap.put(Boolean.valueOf(false), convertGetinfo13OpptyBasecardToV4Response(id, v10JSONObject));
							break;
						case FOLLOWED :
							resultTypeX = "followed"; //$NON-NLS-1$
							break;
						case RLIS :
							v10JSONObject = getOpptyRLIS(id, inputDataType, resultType, resultTag);
							v10ResultMap.clear();
							// If you comment out the (true) line, and un-comment the (false) line, you will be able to:
							// - see the re-constructed v4 result (from v10 api), and
							// - display the card with the orig. v4 API result
							// This can be used for testing to compare the 2 results... but before you check in the code, be sure
							// to un-comment the (true) line, and comment out the (false) line.
							v10ResultMap.put(Boolean.valueOf(true), convertGetinfo13OpptyRLISToV4Response(id, v10JSONObject));
							// v10ResultMap.put(Boolean.valueOf(false), convertGetinfo13OpptyRLISToV4Response(id, v10JSONObject));
							break;
					};
				} else if (inputDataType.equalsIgnoreCase("contactid")) { //$NON-NLS-1$
					switch (resultType) {
						case BASECARD :
							v10JSONObject = getContactBasecard(id, inputDataType, resultType, resultTag);
							v10ResultMap.clear();
							// If you comment out the (true) line, and un-comment the (false) line, you will be able to:
							// - see the re-constructed v4 result (from v10 api), and
							// - display the card with the orig. v4 API result
							// This can be used for testing to compare the 2 results... but before you check in the code, be sure
							// to un-comment the (true) line, and comment out the (false) line.
							v10ResultMap.put(Boolean.valueOf(true), convertGetinfo13ContactBasecardToV4Response(id, v10JSONObject));
							// v10ResultMap.put(Boolean.valueOf(false), convertGetinfo13ContactBasecardToV4Response(id, v10JSONObject));
							break;
						case OPPTIES :
							v10JSONObject = getContactOppty(id, inputDataType, resultType, resultTag);
							v10ResultMap.clear();
							// If you comment out the (true) line, and un-comment the (false) line, you will be able to:
							// - see the re-constructed v4 result (from v10 api), and
							// - display the card with the orig. v4 API result
							// This can be used for testing to compare the 2 results... but before you check in the code, be sure
							// to un-comment the (true) line, and comment out the (false) line.
							v10ResultMap.put(Boolean.valueOf(true), convertGetinfo13ContactOpptyToV4Response(id, v10JSONObject));
							// v10ResultMap.put(Boolean.valueOf(false), convertGetinfo13ContactOpptyToV4Response(id, v10JSONObject));
							break;
					};
				}
			}
		}
		return v10ResultMap;
	}

	public JSONObject getAccountBasecard(String id, String inputDataType, SugarWebservicesOperations.GetInfo13RestulType resultType, String resultTag) throws LoginException {

		// the following logic uses modules/<modules-id> API
		String moduleX = SugarType.ACCOUNTS.getParentType();
		String queryX = httpclientEncode("/" + id);
		// missing indus_class_name (2nd line in Account card's header) in response (defect 81480),
		// indus_industry in account_cstm tbl does not have data, yet.
		String fieldsX = "?fields=id,name,tags,ccms_id,leadclient_rep,leadclient_rep_name,billing_address_street,billing_address_city,billing_address_state,billing_address_postalcode,billing_address_country,phone_office,phone_fax,industry,indus_class_name,indus_industry,default_site_id,defualt_site_id_sugar";

		// // the following logic uses filter API, the response contains "records:"... might want to try it to compare the
		// // performance against the above API.
		// String moduleX = SugarType.ACCOUNTS.getParentType();
		// String queryX = httpclientEncode("?filter[0][id]=" + id + "&filter[0][deleted]=0");
		// String fieldsX = "&fields=" + accountBasecardFields;

		return getV10ModuleGET(moduleX, queryX, fieldsX);
	}

	public JSONObject getAccountOppty(String id, String inputDataType, SugarWebservicesOperations.GetInfo13RestulType resultType, String resultTag) throws LoginException {

		String moduleX = SugarType.ACCOUNTS.getParentType();
		String queryX = httpclientEncode("/" + id + "/link/opportunities?my_items=1&filter[0][sales_stage][$lt]=07&order_by=" + OPPTY_SORTED_BY + ":DESC&max_num=" + MAX_OPPORTUNITIES_TO_BE_DISPLAYED);
		// missing amount_user (defect 81480)
		// extra documents returned (defect 81124)
		String fieldsX = "&fields=name,description,date_closed,sales_stage,amount,amount_usdollar,amount_user,currency_id";

		return getV10ModuleGET(moduleX, queryX, fieldsX);
	}

	public String getAccountIDFromCCMSId(String ccms_id) throws LoginException {
		String id = null;
		String moduleX = SugarType.ACCOUNTS.getParentType();
		String queryX = httpclientEncode("?filter[0][ccms_id]=" + ccms_id);
		String fieldsX = "&fields=id";
		try {
			JSONObject jsonObject = getV10ModuleGET(moduleX, queryX, fieldsX);

			if (jsonObject != null) {
				JSONArray records = jsonObject.getJSONArray("records");
				if (records != null && records.length() > 0) {
					Iterator i = records.iterator();
					while (i.hasNext()) {
						JSONObject v10 = (JSONObject) i.next();
						id = v10.getString("id");
						break;
					}
				}
			}

		} catch (JSONException e) {
			UtilsPlugin.getDefault().logErrorMessage("SugarV10APIManager:getAccountIDFromMppNum - Failed to find account id from ccms id " + ccms_id + "\n", CorePluginActivator.PLUGIN_ID);
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}
		return id;
	}

	public JSONObject getOpptyBasecard(String id, String inputDataType, SugarWebservicesOperations.GetInfo13RestulType resultType, String resultTag) throws LoginException {

		// the following logic uses modules/<modules-id> API
		String moduleX = SugarType.OPPORTUNITIES.getParentType();
		String queryX = httpclientEncode("/" + id);
		// defect 81480
		// missing amount_user (defect 81480)
		// missing assigned_user_email1 ( defect 81480)
		// missing account_indus_industry (defect 81480)
		String fieldsX = "?fields=id,name,description,date_closed,sales_stage,amount,amount_usdollar,currency_id,amount_user,assigned_user_id,assigned_user_name,assigned_bp_id,assigned_bp_name,assigned_user_email1,account_id,account_name,account_indus_industry,contact_id_c,pcontact_id_c";

		// // the following logic uses filter API, the response contains "records:"... might want to try it to compare the
		// // performance against the above API.
		// String moduleX = SugarType.ACCOUNTS.getParentType();
		// String queryX = httpclientEncode("?filter[0][id]=" + id + "&filter[0][deleted]=0");
		// String fieldsX =
		// "&fields=id,name,description,date_closed,sales_stage,amount,amount_usdollar,currency_id,assigned_user_id,assigned_user_name,assigned_bp_id,assigned_bp_name,account_id,account_name,contact_id_c,pcontact_id_c";

		return getV10ModuleGET(moduleX, queryX, fieldsX);
	}

	public JSONObject getOpptyRLIS(String id, String inputDataType, SugarWebservicesOperations.GetInfo13RestulType resultType, String resultTag) throws LoginException {

		// defect 81124, 81480
		String moduleX = SugarType.OPPORTUNITIES.getParentType();
		String queryX = httpclientEncode("/" + id + "/link/opportun_revenuelineitems");
		// missing revenue_amount_user (81480)
		// missing assigned_user_email1 ( 81480)
		String fieldsX = "&fields=id,level15_name,fcast_date_tran,date_modified,revenue_amount,revenue_amount_usd,currency_id,revenue_amount_user,assigned_user_id,assigned_user_name,assigned_user_email1";

		return getV10ModuleGET(moduleX, queryX, fieldsX);
	}

	public JSONObject getContactBasecard(String id, String inputDataType, SugarWebservicesOperations.GetInfo13RestulType resultType, String resultTag) throws LoginException {

		// the following logic uses modules/<modules-id> API
		String moduleX = SugarType.CONTACTS.getParentType();
		String queryX = httpclientEncode("/" + id);
		// missing account_indus_industry (81480)
		String fieldsX = "?fields=id,first_name,last_name,alt_lang_first_c,alt_lang_last_c,title,account_name,account_id,account_indus_industry,phone_work,phone_work_suppressed,phone_mobile,phone_mobile_suppressed,email1,email_opt_out,primary_address_street,primary_address_city,primary_address_state,primary_address_postalcode,primary_address_country";

		// / // the following logic uses filter API, the response contains "records:"... might want to try it to compare the
		// // performance against the above API.
		// String moduleX = SugarType.ACCOUNTS.getParentType();
		// String queryX = httpclientEncode("?filter[0][id]=" + id + "&filter[0][deleted]=0");
		// String fieldsX =
		// "&fields=id,name,description,date_closed,sales_stage,amount,amount_usdollar,currency_id,assigned_user_id,assigned_user_name,assigned_bp_id,assigned_bp_name,account_id,account_name,contact_id_c,pcontact_id_c";

		return getV10ModuleGET(moduleX, queryX, fieldsX);
	}

	public JSONObject getContactOppty(String id, String inputDataType, SugarWebservicesOperations.GetInfo13RestulType resultType, String resultTag) throws LoginException {

		String moduleX = SugarType.CONTACTS.getParentType();
		String queryX = httpclientEncode("/" + id + "/link/opportunities?my_items=1&filter[0][sales_stage][$lt]=07&order_by=" + OPPTY_SORTED_BY + ":DESC&max_num=" + MAX_OPPORTUNITIES_TO_BE_DISPLAYED);
		// missing amount_user (81480)
		String fieldsX = "&fields=name,description,date_closed,sales_stage,amount,amount_usdollar,currency_id,amount_user";

		return getV10ModuleGET(moduleX, queryX, fieldsX);
	}

	public JSONObject getV10ModuleGET(String moduleX, String queryX, String fieldsX) throws LoginException {
		JSONObject v10ResponseJSONObject = null;
		if (moduleX != null && queryX != null) {

			try {
				v10ResponseJSONObject = getV10ModuleGETResponse(moduleX, queryX, fieldsX);

			} catch (Exception e) {
				if (e instanceof LoginException) {
					throw (LoginException) e;
				} else {
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
			}

		}

		return v10ResponseJSONObject;
	}

	private JSONArray convertStringToJSONArray(String s) {
		JSONArray jsonArray = new JSONArray();
		if (s != null) {
			StringTokenizer st = new StringTokenizer(s);
			while (st.hasMoreTokens()) {
				String token = st.nextToken().trim();
				jsonArray.add(token);
			}
		}
		return jsonArray;
	}

	private String httpclientEncode(String s) {
		String outX = s;
		// Apache HTTPClient doesn't like the [] in a url, but Sugar won't work if you urlencode the whole query string, so
		// selectively replace [ with %5B and ] with %5D which seems to make everyone happy
		if (s != null) {
			outX = s.replaceAll("\\[", "%5B");
			outX = outX.replaceAll("\\]", "%5D");
		}
		return outX;
	}

	/**
	 * Helper method to decode the data we get back from the web service. Turns &#XXX; into the appropriate character
	 * 
	 * @param jsonValue
	 * @return
	 */
	private String decodeJSONValue(String jsonValue) {

		String newValue = jsonValue;

		if (jsonValue == null) {
			newValue = ConstantStrings.EMPTY_STRING;
		}

		Pattern pattern = Pattern.compile("&#([0-9]*);"); //$NON-NLS-1$
		Matcher m = pattern.matcher(jsonValue);
		while (m.find()) {
			try {
				String characterEntity = m.group();
				char newChar = new Character((char) Integer.parseInt(m.group(1)));
				newValue = newValue.replaceAll(characterEntity, newChar + ""); //$NON-NLS-1$
			} catch (Exception e) {
				// Just eat and log any exceptions out of here. Shouldn't be any, but just in case
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
		newValue = newValue.replaceAll("&lt;", "<"); //$NON-NLS-1$ //$NON-NLS-2$
		newValue = newValue.replaceAll("&gt;", ">"); //$NON-NLS-1$ //$NON-NLS-2$
		newValue = newValue.replaceAll("&quot;", "\""); //$NON-NLS-1$ //$NON-NLS-2$
		return newValue;
	}

	private String getOneEnumValue(String moduleX, String enumField, String enumKey) throws LoginException {
		String enumValueX = ConstantStrings.EMPTY_STRING;
		if (moduleX != null && enumField != null && enumKey != null) {
			try {
				String enumResults = getEnumFromAPI(moduleX, enumField);
				JSONObject enumJSONObject = new JSONObject(enumResults);
				if (enumJSONObject != null && !enumJSONObject.isEmpty() && enumJSONObject.containsKey(enumKey)) {
					try {
						enumValueX = enumJSONObject.getString(enumKey);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				if (e instanceof LoginException) {
					throw (LoginException) e;
				} else {
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
			}
		}
		return enumValueX;
	}

	private JSONObject getV10ModuleGETResponse(String moduleX, String queryX, String fieldsX) throws LoginException {
		JSONObject v10ResponseObject = null;
		if (moduleX != null) {
			try {
				String url = getV10ApiURL() + "/" + moduleX;
				if (queryX != null) {
					url = url + queryX;
				}
				if (fieldsX != null) {
					url = url + fieldsX;
				}
				// System.out.println("getV10ModuleGETResponse()... request url:" + url);
				UtilsPlugin.getDefault().logInfoMessage("SugarV10APIManager:getV10ModuleGETResponse()... request url:" + url, CorePluginActivator.PLUGIN_ID);
				GetMethod result = processGetRequest(url);
				String v10ResponseX = result.getResponseBodyAsString();
				UtilsPlugin.getDefault().logInfoMessage("SugarV10APIManager:getV10ModuleGETResponse()... status:" + result.getStatusCode() + " ... v10 response:" + v10ResponseX,
						CorePluginActivator.PLUGIN_ID);
				// System.out.println("getV10ModuleGETResponse()... status:" + result.getStatusCode() + " ... v10 response:" + v10ResponseX);
				if (v10ResponseX != null) {
					v10ResponseObject = new JSONObject(v10ResponseX);

				}
			} catch (Exception e) {
				if (e instanceof LoginException) {
					throw (LoginException) e;
				} else {
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
			}
		}
		return v10ResponseObject;
	}

	private String convertGetinfo13AccountBasecardToV4Response(String id, JSONObject v10Response) {
		String v4ResponseX = null;
		if (v10Response == null) {
			v10Response = new JSONObject();
		}

		// if this is response from the filter API
		if (v10Response.has("records")) {
			try {
				JSONArray records = v10Response.getJSONArray("records");

				Iterator i = records.iterator();
				while (i.hasNext()) {
					v10Response = (JSONObject) i.next();
					break;
				}
			} catch (Exception e) {
			}

		}

		try {
			// JSONObject v10Response = new JSONObject(v10Result);

			JSONObject v4Response = new JSONObject();
			JSONObject v4Result = new JSONObject();
			JSONObject v4Key = new JSONObject();
			JSONObject v4Fields = new JSONObject();

			// JSONObject v10DefaultSiteResponse = null;

			v4Fields.put("name", decodeJSONValue(v10Response.containsKey("name") ? v10Response.getString("name") : ConstantStrings.EMPTY_STRING));

			// industry
			JSONObject indusObject = new JSONObject();
			if (v10Response.containsKey("indus_class_name")) {
				String industryValue = getOneEnumValue(SugarType.ACCOUNTS.getParentType(), "indus_class_name", v10Response.getString("indus_class_name"));
				indusObject.put(v10Response.getString("indus_class_name"), industryValue);

			}
			v4Fields.put("industry", indusObject);

			// indus_industry - for card image
			v4Fields.put("indus_industry", v10Response.containsKey("indus_industry") ? v10Response.getString("indus_industry") : ConstantStrings.EMPTY_STRING);

			v4Fields.put("clientid", v10Response.containsKey("ccms_id") ? v10Response.get("ccms_id") : ConstantStrings.EMPTY_STRING);

			JSONObject v4ClientRep = new JSONObject();
			v4ClientRep.put("id", v10Response.containsKey("leadclient_rep") ? v10Response.get("leadclient_rep") : ConstantStrings.EMPTY_STRING);
			v4ClientRep.put("name", decodeJSONValue(v10Response.containsKey("leadclient_rep_name") ? v10Response.getString("leadclient_rep_name") : ConstantStrings.EMPTY_STRING));
			v4Fields.put("clientrep", v4ClientRep);

			// address
			v4Fields.put("pri_physical_street", v10Response.containsKey("billing_address_street") ? v10Response.getString("billing_address_street") : ConstantStrings.EMPTY_STRING);
			v4Fields.put("pri_physical_city", v10Response.containsKey("billing_address_city") ? v10Response.getString("billing_address_city") : ConstantStrings.EMPTY_STRING);
			v4Fields.put("pri_physical_state", getOneEnumValue(SugarType.ACCOUNTS.getParentType(), "billing_address_state", v10Response.containsKey("billing_address_state") ? v10Response
					.getString("billing_address_state") : ConstantStrings.EMPTY_STRING));
			v4Fields.put("pri_physical_postalcode", v10Response.containsKey("billing_address_postalcode") ? v10Response.getString("billing_address_postalcode") : ConstantStrings.EMPTY_STRING);
			v4Fields.put("pri_physical_country", v10Response.containsKey("billing_address_country") ? v10Response.getString("billing_address_country") : ConstantStrings.EMPTY_STRING);

			v4Fields.put("phone_office", v10Response.containsKey("phone_office") ? v10Response.get("phone_office") : ConstantStrings.EMPTY_STRING);
			v4Fields.put("phone_fax", v10Response.containsKey("phone_fax") ? v10Response.get("phone_fax") : ConstantStrings.EMPTY_STRING);

			JSONArray tagsArray = convertStringToJSONArray(decodeJSONValue(v10Response.containsKey("tags") ? v10Response.getString("tags") : ConstantStrings.EMPTY_STRING));
			v4Fields.put("tags", tagsArray);

			v4Fields.put("defaultSiteCCMSID", ConstantStrings.EMPTY_STRING);
			v4Fields.put("defaultSiteID", ConstantStrings.EMPTY_STRING);

			v4Key.put(id, v4Fields);
			v4Result.put("key", v4Key);
			v4Response.put("result", v4Result);

			v4ResponseX = v4Response.toString();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}

		UtilsPlugin.getDefault().logInfoMessage("\nSugarV10APIManager:convertGetinfo13AccountBasecardToV4Response() ... v4ResponseX:" + (v4ResponseX == null ? "null" : v4ResponseX) + "\n",
				CorePluginActivator.PLUGIN_ID);
		// System.out.println("\nconvertGetinfo13AccountBasecardToV4Response() ... v4ResponseX:" + (v4ResponseX == null ? "null" : v4ResponseX) + "\n");
		return v4ResponseX;
	}

	// Note that JSONObject does not retain sort order so we're not going to worry about the sorting order here. Instead,
	// we will sort the output right before saving it into the Java object here: SugarWebservicesOperations.processSugarAccountOppty()
	private String convertGetinfo13AccountOpptyToV4Response(String id, JSONObject v10Response) {

		String v4ResponseX = null;
		JSONObject v4Response = new JSONObject();
		JSONObject v4Result = new JSONObject();
		JSONObject v4Key = new JSONObject();
		JSONObject v4Fields = new JSONObject();
		JSONObject v4Oppties = new JSONObject();
		if (stageEnumMap != null) {
			stageEnumMap = null;
		}

		if (v10Response == null) {
			v10Response = new JSONObject();
		}

		try {
			if (v10Response.has("records")) {
				JSONArray records = v10Response.getJSONArray("records");
				v4Fields.put("opportunitiesTotal", records.size());
				if (records.size() > 0) {
					Iterator i = records.iterator();
					while (i.hasNext()) {
						JSONObject v10ResponseOfI = (JSONObject) i.next();
						JSONObject v4Oppty = new JSONObject();
						v4Oppty.put("name", v10ResponseOfI.containsKey("name") ? v10ResponseOfI.getString("name") : ConstantStrings.EMPTY_STRING);
						v4Oppty.put("description", v10ResponseOfI.containsKey("description") ? v10ResponseOfI.getString("description") : ConstantStrings.EMPTY_STRING);
						v4Oppty.put("sales_stage", getSalesStage(v10ResponseOfI));
						String dateClosedX = v10ResponseOfI.containsKey("date_closed") ? v10ResponseOfI.getString("date_closed") : ConstantStrings.EMPTY_STRING;
						v4Oppty.put("date_closed", dateClosedX);
						v4Oppty.put("amount", getAmountForUser(v10ResponseOfI, "amount", "amount_usdollar"));
						v4Oppties.put(v10ResponseOfI.containsKey("id") ? v10ResponseOfI.getString("id") : ConstantStrings.EMPTY_STRING, v4Oppty);
					}
					v4Fields.put("opportunities", v4Oppties);
				}
			}

			v4Key.put(id, v4Fields);
			v4Result.put("key", v4Key);
			v4Response.put("result", v4Result);

			v4ResponseX = v4Response.toString();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}

		UtilsPlugin.getDefault().logInfoMessage("\nSugarV10APIManager:convertGetinfo13AccountOpptyToV4Response() ... v4ResponseX:" + (v4ResponseX == null ? "null" : v4ResponseX) + "\n",
				CorePluginActivator.PLUGIN_ID);
		// System.out.println("\nconvertGetinfo13AccountOpptyToV4Response() ... v4ResponseX:" + (v4ResponseX == null ? "null" : v4ResponseX) + "\n");
		return v4ResponseX;
	}

	private String convertGetinfo13OpptyBasecardToV4Response(String id, JSONObject v10Response) {
		String v4ResponseX = null;
		if (v10Response == null) {
			v10Response = new JSONObject();
		}

		// if this is response from the filter API
		if (v10Response.has("records")) {
			try {
				JSONArray records = v10Response.getJSONArray("records");

				Iterator i = records.iterator();
				while (i.hasNext()) {
					v10Response = (JSONObject) i.next();
					break;
				}
			} catch (Exception e) {
			}

		}

		try {

			JSONObject v4Response = new JSONObject();
			JSONObject v4Result = new JSONObject();
			JSONObject v4Key = new JSONObject();
			JSONObject v4Fields = new JSONObject();

			v4Fields.put("name", decodeJSONValue(v10Response.containsKey("name") ? v10Response.getString("name") : ConstantStrings.EMPTY_STRING));
			v4Fields.put("description", decodeJSONValue(v10Response.containsKey("description") ? v10Response.getString("description") : ConstantStrings.EMPTY_STRING));
			v4Fields.put("account_name", decodeJSONValue(v10Response.containsKey("account_name") ? v10Response.getString("account_name") : ConstantStrings.EMPTY_STRING));
			v4Fields.put("account_id", v10Response.containsKey("account_id") ? v10Response.get("account_id") : ConstantStrings.EMPTY_STRING);
			v4Fields.put("date_closed", v10Response.containsKey("date_closed") ? v10Response.get("date_closed") : ConstantStrings.EMPTY_STRING);

			// missing... indus_industry - for card image
			v4Fields.put("indus_industry", v10Response.containsKey("indus_industry") ? v10Response.getString("indus_industry") : ConstantStrings.EMPTY_STRING);

			// industry
			JSONObject indusObject = new JSONObject();
			if (v10Response.containsKey("indus_class_name")) {
				String industryValue = getOneEnumValue(SugarType.ACCOUNTS.getParentType(), "indus_class_name", v10Response.getString("indus_class_name"));
				indusObject.put(v10Response.getString("indus_class_name"), industryValue);

			}
			v4Fields.put("industry", indusObject);
			v4Fields.put("sales_stage", getOneEnumValue(SugarType.OPPORTUNITIES.getParentType(), "sales_stage", v10Response.getString("sales_stage")));
			v4Fields.put("amount", getAmountForUser(v10Response, "amount", "amount_usdollar"));

			String userIDX = ConstantStrings.EMPTY_STRING;
			String userNameX = ConstantStrings.EMPTY_STRING;
			String userEmailX = ConstantStrings.EMPTY_STRING;
			if (v10Response.containsKey("assigned_user_id") && v10Response.get("assigned_user_id") != null && !((String) v10Response.get("assigned_user_id")).equals(ConstantStrings.EMPTY_STRING)) {
				userIDX = v10Response.containsKey("assigned_user_id") ? (String) v10Response.get("assigned_user_id") : ConstantStrings.EMPTY_STRING;
				userNameX = v10Response.containsKey("assigned_user_name") ? (String) v10Response.get("assigned_user_name") : ConstantStrings.EMPTY_STRING;

				// missing...
				userEmailX = v10Response.containsKey("assigned_user_email") ? (String) v10Response.get("assigned_user_email") : ConstantStrings.EMPTY_STRING;

			} else if (v10Response.containsKey("assigned_bp_id") && v10Response.get("assigned_bp_id") != null && !((String) v10Response.get("assigned_bp_id")).equals(ConstantStrings.EMPTY_STRING)) {
				userIDX = v10Response.containsKey("assigned_bp_id") ? (String) v10Response.get("assigned_bp_id") : ConstantStrings.EMPTY_STRING;
				userNameX = v10Response.containsKey("assigned_bp_name") ? (String) v10Response.get("assigned_bp_name") : ConstantStrings.EMPTY_STRING;
			}
			v4Fields.put("assigned_user_id", userIDX);
			v4Fields.put("assigned_user_name", userNameX);
			v4Fields.put("assigned_user_email", userEmailX);

			v4Fields.put("primary_contact_id", v10Response.containsKey("contact_id_c") ? v10Response.get("contact_id_c") : ConstantStrings.EMPTY_STRING);
			v4Fields.put("primary_contact_name", v10Response.containsKey("pcontact_id_c") ? v10Response.get("pcontact_id_c") : ConstantStrings.EMPTY_STRING);

			v4Key.put(id, v4Fields);
			v4Result.put("key", v4Key);
			v4Response.put("result", v4Result);

			v4ResponseX = v4Response.toString();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}

		UtilsPlugin.getDefault().logInfoMessage("\nSugarV10APIManager:convertGetinfo13OpptyBasecardToV4Response() ... v4ResponseX:" + (v4ResponseX == null ? "null" : v4ResponseX) + "\n",
				CorePluginActivator.PLUGIN_ID);
		// System.out.println("\nconvertGetinfo13OpptyBasecardToV4Response() ... v4ResponseX:" + (v4ResponseX == null ? "null" : v4ResponseX) + "\n");
		return v4ResponseX;
	}

	private String convertGetinfo13OpptyRLISToV4Response(String id, JSONObject v10Response) {
		String v4ResponseX = null;
		JSONObject v4Response = new JSONObject();
		JSONObject v4Result = new JSONObject();
		JSONObject v4Key = new JSONObject();
		JSONObject v4Fields = new JSONObject();
		JSONObject v4Oppties = new JSONObject();

		if (v10Response == null) {
			v10Response = new JSONObject();
		}

		try {
			if (v10Response.has("records")) {
				JSONArray records = v10Response.getJSONArray("records");

				Iterator i = records.iterator();
				while (i.hasNext()) {
					JSONObject v10ResponseOfI = (JSONObject) i.next();
					JSONObject v4Oppty = new JSONObject();
					v4Oppty.put("amount", getAmountForUser(v10ResponseOfI, "revenue_amount", "revenue_amount_usd"));
					v4Oppty.put("level15", v10ResponseOfI.containsKey("level15_name") ? v10ResponseOfI.getString("level15_name") : ConstantStrings.EMPTY_STRING);
					v4Oppty.put("assigned_user_id", v10ResponseOfI.containsKey("assigned_user_id") ? v10ResponseOfI.getString("assigned_user_id") : ConstantStrings.EMPTY_STRING);
					v4Oppty.put("assigned_user_name", v10ResponseOfI.containsKey("assigned_user_name") ? v10ResponseOfI.getString("assigned_user_name") : ConstantStrings.EMPTY_STRING);

					// missing...
					v4Oppty.put("assigned_user_email", v10ResponseOfI.containsKey("assigned_user_email") ? v10ResponseOfI.getString("assigned_user_email") : ConstantStrings.EMPTY_STRING);

					v4Oppty.put("bill_date", v10ResponseOfI.containsKey("fcast_date_tran") ? v10ResponseOfI.getString("fcast_date_tran") : ConstantStrings.EMPTY_STRING);
					v4Oppty.put("last_modified_date", v10ResponseOfI.containsKey("date_modified") ? v10ResponseOfI.getString("date_modified") : ConstantStrings.EMPTY_STRING);

					v4Oppties.put(v10ResponseOfI.containsKey("id") ? v10ResponseOfI.getString("id") : ConstantStrings.EMPTY_STRING, v4Oppty);

				}
			}

			v4Key.put(id, v4Oppties);
			v4Result.put("key", v4Key);
			v4Response.put("result", v4Result);

			v4ResponseX = v4Response.toString();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}

		UtilsPlugin.getDefault().logInfoMessage("\nSugarV10APIManager:convertGetinfo13OpptyRLISToV4Response() ... v4ResponseX:" + (v4ResponseX == null ? "null" : v4ResponseX) + "\n",
				CorePluginActivator.PLUGIN_ID);
		// System.out.println("\nconvertGetinfo13OpptyRLISToV4Response() ... v4ResponseX:" + (v4ResponseX == null ? "null" : v4ResponseX) + "\n");
		return v4ResponseX;
	}

	private String convertGetinfo13ContactBasecardToV4Response(String id, JSONObject v10Response) {
		String v4ResponseX = null;
		if (v10Response == null) {
			v10Response = new JSONObject();
		}

		// if this is response from the filter API
		if (v10Response.has("records")) {
			try {
				JSONArray records = v10Response.getJSONArray("records");

				Iterator i = records.iterator();
				while (i.hasNext()) {
					v10Response = (JSONObject) i.next();
					break;
				}
			} catch (Exception e) {
			}

		}

		try {

			JSONObject v4Response = new JSONObject();
			JSONObject v4Result = new JSONObject();
			JSONObject v4Key = new JSONObject();
			JSONObject v4Fields = new JSONObject();

			v4Fields.put("first_name", v10Response.containsKey("first_name") ? v10Response.getString("first_name") : ConstantStrings.EMPTY_STRING);
			v4Fields.put("last_name", v10Response.containsKey("last_name") ? v10Response.getString("last_name") : ConstantStrings.EMPTY_STRING);
			v4Fields.put("alt_first_name", v10Response.containsKey("alt_lang_first_c") ? v10Response.getString("alt_lang_first_c") : ConstantStrings.EMPTY_STRING);
			v4Fields.put("alt_last_name", v10Response.containsKey("alt_lang_last_c") ? v10Response.getString("alt_lang_last_c") : ConstantStrings.EMPTY_STRING);
			v4Fields.put("title", v10Response.containsKey("title") ? v10Response.getString("title") : ConstantStrings.EMPTY_STRING);

			v4Fields.put("account_name", decodeJSONValue(v10Response.containsKey("account_name") ? ((v10Response.isNull("account_name")) ? ConstantStrings.EMPTY_STRING : v10Response
					.getString("account_name")) : ConstantStrings.EMPTY_STRING));
			v4Fields.put("account_id", v10Response.containsKey("account_id") ? v10Response.get("account_id") : ConstantStrings.EMPTY_STRING);
			v4Fields.put("phone_work", v10Response.containsKey("phone_work") ? v10Response.get("phone_work") : ConstantStrings.EMPTY_STRING);
			v4Fields.put("phone_work_optout", convertBooleanToInt(v10Response, "phone_work_suppressed"));
			v4Fields.put("phone_mobile", v10Response.containsKey("phone_mobile") ? v10Response.get("phone_mobile") : ConstantStrings.EMPTY_STRING);
			v4Fields.put("phone_mobile_optout", convertBooleanToInt(v10Response, "phone_mobile_suppressed"));
			v4Fields.put("email1", v10Response.containsKey("email1") ? v10Response.get("email1") : ConstantStrings.EMPTY_STRING);
			v4Fields.put("email1_optout", convertBooleanToInt(v10Response, "email_opt_out"));
			v4Fields.put("primary_address_street", v10Response.containsKey("primary_address_street") ? v10Response.get("primary_address_street") : ConstantStrings.EMPTY_STRING);
			v4Fields.put("primary_address_city", v10Response.containsKey("primary_address_city") ? v10Response.get("primary_address_city") : ConstantStrings.EMPTY_STRING);

			v4Fields.put("primary_address_state", getOneEnumValue(SugarType.ACCOUNTS.getParentType(), "primary_address_state", v10Response.containsKey("primary_address_state") ? v10Response
					.getString("primary_address_state") : ConstantStrings.EMPTY_STRING));

			v4Fields.put("primary_address_postalcode", v10Response.containsKey("primary_address_postalcode") ? v10Response.get("primary_address_postalcode") : ConstantStrings.EMPTY_STRING);
			v4Fields.put("primary_address_country", v10Response.containsKey("primary_address_country") ? v10Response.get("primary_address_country") : ConstantStrings.EMPTY_STRING);

			v4Key.put(id, v4Fields);
			v4Result.put("key", v4Key);
			v4Response.put("result", v4Result);

			v4ResponseX = v4Response.toString();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}

		UtilsPlugin.getDefault().logInfoMessage("\nSugarV10APIManager:convertGetinfo13ContactBasecardToV4Response() ... v4ResponseX:" + (v4ResponseX == null ? "null" : v4ResponseX) + "\n",
				CorePluginActivator.PLUGIN_ID);
		// System.out.println("\nconvertGetinfo13ContactBasecardToV4Response() ... v4ResponseX:" + (v4ResponseX == null ? "null" : v4ResponseX) + "\n");
		return v4ResponseX;
	}

	// Note that JSONObject does not retain sort order so we're not going to worry about the sorting order here. Instead,
	// we will sort the output right before saving it into the Java object here: SugarWebservicesOperations.processSugarContactOppty()
	private String convertGetinfo13ContactOpptyToV4Response(String id, JSONObject v10Response) {

		String v4ResponseX = null;
		JSONObject v4Response = new JSONObject();
		JSONObject v4Result = new JSONObject();
		JSONObject v4Key = new JSONObject();
		JSONObject v4Fields = new JSONObject();
		JSONObject v4Oppties = new JSONObject();
		if (stageEnumMap != null) {
			stageEnumMap = null;
		}

		if (v10Response == null) {
			v10Response = new JSONObject();
		}

		try {
			if (v10Response.has("records")) {
				JSONArray records = v10Response.getJSONArray("records");
				v4Fields.put("opportunitiesTotal", records.size());
				if (records.size() > 0) {
					Iterator i = records.iterator();
					while (i.hasNext()) {
						JSONObject v10ResponseOfI = (JSONObject) i.next();
						JSONObject v4Oppty = new JSONObject();
						v4Oppty.put("name", v10ResponseOfI.containsKey("name") ? v10ResponseOfI.getString("name") : ConstantStrings.EMPTY_STRING);
						v4Oppty.put("description", v10ResponseOfI.containsKey("description") ? v10ResponseOfI.getString("description") : ConstantStrings.EMPTY_STRING);
						v4Oppty.put("sales_stage", getSalesStage(v10ResponseOfI));
						String dateClosedX = v10ResponseOfI.containsKey("date_closed") ? v10ResponseOfI.getString("date_closed") : ConstantStrings.EMPTY_STRING;
						v4Oppty.put("date_closed", dateClosedX);
						v4Oppty.put("amount", getAmountForUser(v10ResponseOfI, "amount", "amount_usdollar"));
						v4Oppties.put(v10ResponseOfI.containsKey("id") ? v10ResponseOfI.getString("id") : ConstantStrings.EMPTY_STRING, v4Oppty);
					}
					v4Fields.put("opportunities", v4Oppties);
				}
			}

			v4Key.put(id, v4Fields);
			v4Result.put("key", v4Key);
			v4Response.put("result", v4Result);

			v4ResponseX = v4Response.toString();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}

		UtilsPlugin.getDefault().logInfoMessage("\nSugarV10APIManager:convertGetinfo13ContactOpptyToV4Response() ... v4ResponseX:" + (v4ResponseX == null ? "null" : v4ResponseX) + "\n",
				CorePluginActivator.PLUGIN_ID);
		// System.out.println("\nconvertGetinfo13ContactOpptyToV4Response() ... v4ResponseX:" + (v4ResponseX == null ? "null" : v4ResponseX) + "\n");
		return v4ResponseX;
	}

	private String getSalesStage(JSONObject v10) throws LoginException {
		String salesStageX = ConstantStrings.EMPTY_STRING;
		if (v10 != null && v10.containsKey("sales_stage")) {
			// save all the enum values in the map, so we don't need to call it multiple times.
			try {
				String salesStageEnumKey = v10.getString("sales_stage");
				if (stageEnumMap == null) {
					stageEnumMap = getEnumValues(SugarType.OPPORTUNITIES.getParentType(), "sales_stage");
				}

				salesStageX = stageEnumMap.get(salesStageEnumKey);

			} catch (Exception e) {
				if (e instanceof LoginException) {
					throw (LoginException) e;
				} else {
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
			}
		}
		return salesStageX;
	}

	private String getAmountForUser(JSONObject v10, String amountFieldName, String amountUSDFieldName) {
		NumberFormat nf = new DecimalFormat("###,###,###,##0");
		String v10AmountForUser = "0";
		if (v10 != null) {
			String usDollarAmountInKFormatted = "0";
			try {
				if (v10.containsKey(amountUSDFieldName)) {
					String usDollarAmountX = v10.getString(amountUSDFieldName);
					usDollarAmountInKFormatted = nf.format(Double.valueOf(usDollarAmountX).intValue() / 1000);
				}
				v10AmountForUser = (v10.containsKey(amountFieldName) ? v10.getString(amountFieldName) : "0") + " (" + usDollarAmountInKFormatted + "k USD)";
			} catch (Exception e) {
			}
		}
		return v10AmountForUser;
	}

	private String convertBooleanToInt(JSONObject v10Response, String fieldName) {
		String booleanX = "0";
		try {
			if (v10Response != null && fieldName != null && v10Response.containsKey(fieldName) && v10Response.get(fieldName) instanceof Boolean) {

				int i = Boolean.valueOf(v10Response.getBoolean(fieldName)).compareTo(Boolean.FALSE);
				booleanX = String.valueOf(i);
			}
		} catch (Exception e) {
		}
		return booleanX;
	}

	private Map<String, String> getEnumValues(String module, String enumField) throws LoginException {
		Map<String, String> aMap = new HashMap<String, String>();
		String url = getV10ApiURL() + "/" + module + "/enum/" + enumField; //$NON-NLS-1$  //$NON-NLS-2$
		String results = ConstantStrings.EMPTY_STRING;
		try {
			// System.out.println("Search String: " + searchString + " v10 request:" + url);
			GetMethod result = processGetRequest(url);
			results = result.getResponseBodyAsString();
			// System.out.println("v10 response:" + results);
			JSONObject jsonObject = new JSONObject(results);
			Iterator it = jsonObject.keys();
			while (it.hasNext()) {
				String keyX = (String) it.next();
				String valueX = jsonObject.getString(keyX);
				aMap.put(keyX, valueX);
			}
		} catch (Exception e) {
			if (e instanceof LoginException) {
				throw (LoginException) e;
			} else {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
		return aMap;
	}

}
