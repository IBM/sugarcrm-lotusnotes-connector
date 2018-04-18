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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;

import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.utils.SugarDashboardPreference;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.sugarwidgetupdate.SugarWidgetUpdater;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.WebSecurityCodeProvider;

public class CallLogService extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3378971436319729226L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// used to save the form fields in the session in case they need to be pulled later on refresh
		// if a get is sent with no parameters, that clears the stored values

		// 56713
		if (SugarDashboardPreference.getInstance().getSugarDateFormatPreference() == null) {
			new SugarWidgetUpdater().loadUserData(false);
		}

		
		HttpSession session = request.getSession(true);

		// get the parameters
		Map<String, String[]> input = request.getParameterMap();
		//System.out.println("In CallLogService servlet " + request.getRequestURL() + "params:" + request.getQueryString());
		Map<String, String> fieldValues = new HashMap<String, String>();
		for (String aKey : input.keySet()) {
			// System.out.println("Putting in session...key:" + aKey + " value:" + input.get(aKey)[0]);
			fieldValues.put(aKey, input.get(aKey)[0]);
		}
		session.setAttribute("fieldValues", fieldValues);
		response.setStatus(200);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// read the posted values, create the call log entry, and return the result.
		HttpSession session = request.getSession(true);
		boolean useSecurityCode = true;
		//System.out.println("In CallLogService servlet " + request.getRequestURL());
		// check security code, return unauthorized if invalid
		String securityCode = request.getParameter("securityCode");
		if (useSecurityCode && (securityCode == null || securityCode.equals("") || !securityCode.equals(WebSecurityCodeProvider.getInstance().getSecurityCodeString()))) {
			//System.out.println("CallLogService: invalid security code.  expected: " + WebSecurityCodeProvider.getInstance().getSecurityCodeString());
			UtilsPlugin.getDefault().logInfoMessage("CallLogService: invalid security code.", CorePluginActivator.PLUGIN_ID);
			response.setStatus(403); // forbidden
			String responseStr = "403";
			OutputStream out = response.getOutputStream();
			out.write(responseStr.getBytes());
			out.flush();
			out.close();
			return;
		} else {
			// this code is burned, generate a new one
			WebSecurityCodeProvider.getInstance().generateSecurityCode();
		}
		// get the parameters
		Map<String, String[]> input = request.getParameterMap();
		// convert hours/minutes to minutes
		String minute = input.get("minutes")[0];
		String hour = input.get("hours")[0];
		String durationString = input.get("duration")[0];
		int hours = Integer.parseInt(hour);
		int minutes = Integer.parseInt(minute);

		// pull out the field names that have dates to be converted with UTC
		String dateFields = (String) input.get("dateFields")[0];
		// build the parameter map for the webservice. skip hours and map minutes to correct key
		Map wsParams = new HashMap();
		for (String aKey : input.keySet()) {
			// System.out.println("key:" + aKey + " value:" + input.get(aKey)[0]);
			if (aKey.equals("hours") || aKey.equals("dateFields") || aKey.equals("assigned_user_name") || aKey.equals("parent_id")) {
				// skip it
			} else if (aKey.equals("duration")) {
				wsParams.put("duration_minutes", durationString); // there will only be one of each param, so map to string,string
			} else if (dateFields.indexOf(aKey + ",") >= 0) { // include the trailing comma so we don't have prefix matches
				// convert the date to UTC/GMT. Incoming is midnight at current locale, needs to be midnight UTC
				if (aKey.equals("date_start")) {
					wsParams.put("date_start", convertDateToUTC(input.get(aKey)[0], hours, minutes)); // there will only be one of each param, so map to string,string
				} else {
					wsParams.put(aKey, convertDateToUTC(input.get(aKey)[0], 0, 0));
				}

			} else if (aKey.equals("relatedIDFilterData")) {
				/*
				 * prepare the sfanotes_related_to data from filter format from UI module,sugarid=label^^module2,sugarid2=label2 format to webservice module,sugarid^^module2,sugarid2 or for an
				 * employee sugarid=label
				 */

				String filterData = input.get(aKey)[0];
				String paramToBuild = "";
				String[] filters = filterData.split("\\^\\^");
				boolean firstTime = true;
				for (int i = 0; i < filters.length; i++) {
					String aFilterNoLabel = filters[i].split("=")[0];
					if (!firstTime) {
						paramToBuild += "^^";
					}
					firstTime = false;
					paramToBuild += aFilterNoLabel;
				}
				wsParams.put("sfanotes_related_to", paramToBuild);

			} else if (aKey.equals("participantsFilterData")) {
				/*
				 * prepare the assigned_user_id and sfanotes_additional_users data format from UI sugarid=label^^sugarid2=label2 format for WS: first item sugarid becomes assigned_user_id the rest
				 * become sfanotes_additional_users...format: sugarid1^^sugarid2
				 */
				String filterData = input.get(aKey)[0];
				// pull off the first one for assigned_user_id
				String assignedUserId = filterData.split("=")[0];
				// the rest become sfanotes_additional_users
				String paramToBuild = "";
				String[] filters = filterData.split("\\^\\^");
				boolean firstTime = true;
				for (int i = 1; i < filters.length; i++) {// start at 1 to skip the first filter
					String aFilterNoLabel = filters[i].split("=")[0];
					if (!firstTime) {
						paramToBuild += "^^";
					}
					firstTime = false;
					paramToBuild += aFilterNoLabel;
				}
				wsParams.put("assigned_user_id", assignedUserId);
				wsParams.put("sfanotes_additional_users", paramToBuild);
			} else {
				if (input.get(aKey).length > 1) {
					// URL encode the value for now until the main callWebservice is changed to use UTF-8
					List inputParms = Arrays.asList(input.get(aKey));
					List encodedParms = new ArrayList();
					for (Object o : inputParms) {
						encodedParms.add((String) o);
					}
					try {

						JSONArray arr = new JSONArray(encodedParms);
						wsParams.put(aKey, arr);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					wsParams.put(aKey, input.get(aKey)[0]);
				}
			}

		}
		// call the webservice
		String output = SugarWebservicesOperations.getInstance().createCallLogFromWebservice(wsParams);
		// System.out.println("response:" + output);
		boolean gotJSONResponse = output.indexOf("header\":{") >= 0;
		boolean hasError = false;
		boolean hasErrorCode = false;
		String errorCode = "";
		if (gotJSONResponse) {
			// check for error inside json response
			try {
				JSONObject json = new JSONObject(output);
				try {
					JSONObject saveResponse = json.getJSONObject("saveResponse");
					try {
						errorCode = saveResponse.getString("number");
						hasErrorCode = true; // the number has the error code, so we have an error to pass back
					} catch (JSONException e) {
						hasError = true; // if we got the object, then we should have been able to read the number, so error
					}
				} catch (JSONException e) {
					hasError = false; // if the saveResponse is a string, which makes this exception happen when asking for an object, then it was successful
				}
			} catch (JSONException e) {
				hasError = true; // something unexpected
			}
		} else {
			hasError = true; // no JSON response means if failed unexpectedly
		}
		if (hasError) {
			// there was a fail of some kind
			response.setStatus(500);

		} else {
			response.setStatus(200);
			if (hasErrorCode) {
				OutputStream out = response.getOutputStream();
				out.write(errorCode.getBytes());
				out.flush();
				out.close();
			}
		}
		// System.out.println("from WS:" + output);
		UtilsPlugin.getDefault().logInfoMessage("CallLogService: WS response:" + output, CorePluginActivator.PLUGIN_ID);
	}

	private String convertDateToUTC(String dateString, int hour, int minute) {
		TimeZone tz = TimeZone.getDefault();
		SimpleDateFormat informat = new SimpleDateFormat("yyyy-MM-dd");
		Date inDate;

		if (dateString.equals("")) {
			return dateString;
		}
		try {
			inDate = informat.parse(dateString);
			// set the time of the date object based on hour and minute
			inDate.setHours(hour);
			inDate.setMinutes(minute);
		} catch (java.text.ParseException e) {
			e.printStackTrace();
			return dateString; // just return the unconverted string
		}

		SimpleDateFormat outFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		outFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		String convertedDateTime = outFormatter.format(inDate);
		// System.out.println("input Date:" + inDate);
		// System.out.println("output Date:" + convertedDateTime);
		return convertedDateTime;

	}
}
