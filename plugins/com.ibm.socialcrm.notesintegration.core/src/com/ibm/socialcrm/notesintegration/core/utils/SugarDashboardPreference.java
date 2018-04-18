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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.json.JSONObject;
import org.apache.commons.json.JSONString;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;

public class SugarDashboardPreference {
	// for example: l, s f
	private static final String NAMEFORMAT_PREFERENCE_JSONKEY = "user_default_locale_name_format"; //$NON-NLS-1$

	// for example: Y.m.d
	private static final String DATEFORMAT_PREFERENCE_JSONKEY = "user_default_dateformat"; //$NON-NLS-1$

	// for example:
	// H.i 23:00
	// h:iA 11:00PM
	// h:ia 11:00pm
	// h:i A 11:00 PM
	private static final String TIMEFORMAT_PREFERENCE_JSONKEY = "user_default_timeformat";//$NON-NLS-1$

	// for example: USD
	private static final String CURRENCY_SYMBOL_PREFERENCE_JSONKEY = "user_currency_symbol";//$NON-NLS-1$

	// for example: , - 1000th separator
	private static final String NUMBER_ONE_THOUSANDTH_SEPARATOR_PREFERENCE_JSONKEY = "user_number_seperator"; //$NON-NLS-1$

	// for example: . - decimal separator
	private static final String NUMBER_DECIMAL_SEPARATOR_PREFERENCE_JSONKEY = "user_decimal_seperator"; //$NON-NLS-1$ 

	// Default Date strng format from web services
	private static final String DEFAULT_DATE_STRING_PATTERN = "yyyy-MM-dd"; //$NON-NLS-1$
	//the max number of account myitems we should pull for regexes
	private static final String MAX_ACCOUNT_MYITEMS_PREFERENCE_JSONKEY = "notes_max_num_myitems_accounts"; //$NON-NLS-1$
	//the max number of contact myitems we should pull for regexes
	private static final String MAX_CONTACT_MYITEMS_PREFERENCE_JSONKEY = "notes_max_num_myitems_contacts"; //$NON-NLS-1$
	//how many myitems we should pull in each bite
	private static final String REGEX_BATCH_SIZE_PREFERENCE_JSONKEY = "notes_regex_batch_size"; //$NON-NLS-1$
	

	private String _nameFormat = null;
	private String _dateFormat = null;
	private String _timeFormat = null;
	private String _currencySymbol = null;
	private String _numberOneThousandthSeparator = null;
	private String _numberDecimalSeparator = null;

	private static SugarDashboardPreference _preference = null;
	private static JSONObject _jsonObject = null;

	public SugarDashboardPreference() {
	}

	// l, s f
	public String getFormattedNameWithoutSalutation(String firstName, String lastName) {
		String formattedName = null;
		if (firstName != null && lastName != null) {
			if (getSugarNameFormatPreference() != null) {
				// split the format
				String[] nameFormats = getSugarNameFormatPreference().split(ConstantStrings.SPACE);
				for (int i = 0; i < nameFormats.length; i++) {
					if (nameFormats[i] != null) {
						if (nameFormats[i].contains("l")) //$NON-NLS-1$
						{
							nameFormats[i] = nameFormats[i].replace("l", lastName); //$NON-NLS-1$
						} else if (nameFormats[i].contains("f")) //$NON-NLS-1$
						{
							nameFormats[i] = nameFormats[i].replace("f", firstName); //$NON-NLS-1$
						} else {
							// Defect 33124: This will match s and p in the current scheme. Removing the check so future additions don't get copied to the string.
							nameFormats[i] = null;
						}
					}
				}

				// string up each formatted name
				StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
				boolean isFirst = true;
				for (int i = 0; i < nameFormats.length; i++) {
					if (nameFormats[i] != null) {
						if (!isFirst) {
							sb.append(ConstantStrings.SPACE);
						} else {
							isFirst = false;
						}
						sb.append(nameFormats[i]);
					}
				}
				formattedName = sb.toString();
			}
		}
		return formattedName;
	}

	// Formatting a date string returned from web services. This date string from
	// web servcies should be in DEFAULT_DATE_STRING_PATTERN format.
	public String getFormattedDate(String dtX) {
		String formattedDtX = null;
		if (dtX != null && dtX != "null") //$NON-NLS-1$  //Somehow string null can make its way in.
		{
			// String off hh:mm:ss after the first space if they exist
			int spaceIndex = dtX.indexOf(" "); //$NON-NLS-1$
			if (spaceIndex != -1) {
				dtX = dtX.substring(0, spaceIndex);
			}
			SimpleDateFormat formatter = new SimpleDateFormat(DEFAULT_DATE_STRING_PATTERN);
			try {
				formattedDtX = getFormattedDate(formatter.parse(dtX));
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}

		return formattedDtX;
	}

	public String getFormattedDate(Date dt) {
		String formattedDate = null;
		if (dt != null) {
			String javaFormat = getSugarDateFormatPreferenceForJava();
			if (javaFormat != null) {
				SimpleDateFormat formatter = new SimpleDateFormat(javaFormat);
				formattedDate = formatter.format(dt);
			}
		}
		return formattedDate;
	}

	/*
	 * Return Sugar's Date format preference in the valid Java format, for example: yyyy-MM-dd.
	 */
	public String getSugarDateFormatPreferenceForJava() {
		String javaFormat = null;

		String dateFormat = getSugarDateFormatPreference();
		if (dateFormat != null) {
			if (dateFormat.toLowerCase().contains("d")) //$NON-NLS-1$
			{
				dateFormat = dateFormat.toLowerCase().replace("d", "dd"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (dateFormat.toLowerCase().contains("y")) //$NON-NLS-1$
			{
				dateFormat = dateFormat.toLowerCase().replace("y", "yyyy"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (dateFormat.toLowerCase().contains("m")) //$NON-NLS-1$
			{
				// watch out, replacing m to upper case MM here
				dateFormat = dateFormat.toLowerCase().replace("m", "MM"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			javaFormat = dateFormat;
		}
		return javaFormat;
	}

	public String getFormattedTime(Date dt) {
		String formattedTime = null;
		if (dt != null) {
			String timeFormat = getSugarTimeFormatPreferenceForJava();
			if (timeFormat != null) {
				SimpleDateFormat formatter = new SimpleDateFormat(timeFormat);
				formattedTime = formatter.format(dt);
				if (isLowerCaseAMPM()) {
					formattedTime = formattedTime.toLowerCase();
				}
			}
		}
		return formattedTime;
	}

	/*
	 * Return Sugar's Time format preference in the valid Java format, for example: hh:mm a.
	 */
	public String getSugarTimeFormatPreferenceForJava() {
		String javaFormat = null;

		String timeFormat = getSugarTimeFormatPreference();
		if (timeFormat != null) {
			// 24 hours
			if (timeFormat.contains("H")) //$NON-NLS-1$
			{
				timeFormat = timeFormat.replace("H", "kk"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			// 12 hours
			else if (timeFormat.contains("h")) //$NON-NLS-1$
			{
				timeFormat = timeFormat.replace("h", "hh"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			timeFormat = timeFormat.replace(":i", ":mm"); //$NON-NLS-1$ //$NON-NLS-2$
			timeFormat = timeFormat.replace(".i", ":mm"); //$NON-NLS-1$ //$NON-NLS-2$

			if (timeFormat.toLowerCase().contains("a"))//$NON-NLS-1$
			{
				timeFormat = timeFormat.toLowerCase().replace("a", "a");//$NON-NLS-1$ //$NON-NLS-2$
			}
			javaFormat = timeFormat;
		}
		return javaFormat;
	}

	public boolean isLowerCaseAMPM() {
		boolean isLowerCaseAMPM = false;
		String timeFormat = getSugarTimeFormatPreference();
		if (timeFormat != null) {
			if (timeFormat.contains("a")) //$NON-NLS-1$
			{
				isLowerCaseAMPM = true;
			}
		}
		return isLowerCaseAMPM;
	}

	// Name format should be represented by "f" (first name), "l" (last name) and "s" (title).
	public String getSugarNameFormatPreference() {
		if (_nameFormat == null) {
			_nameFormat = getSugarPreference(NAMEFORMAT_PREFERENCE_JSONKEY);
		}
		return _nameFormat;
	}

	/*
	 * Return Sugar Date format preference. For example: Y.m.d.
	 */
	public String getSugarDateFormatPreference() {
		if (_dateFormat == null) {
			_dateFormat = getSugarPreference(DATEFORMAT_PREFERENCE_JSONKEY);
		}
		return _dateFormat;
	}

	public String getSugarTimeFormatPreference() {
		if (_timeFormat == null) {
			_timeFormat = getSugarPreference(TIMEFORMAT_PREFERENCE_JSONKEY);
		}
		return _timeFormat;
	}

	public String getSugarCurrencySymbolPreference() {
		if (_currencySymbol == null) {
			_currencySymbol = getSugarPreference(CURRENCY_SYMBOL_PREFERENCE_JSONKEY);
		}
		return _currencySymbol;
	}

	public String getSugarNumberOneThousandthSeparatorPreference() {
		if (_numberOneThousandthSeparator == null) {
			_numberOneThousandthSeparator = getSugarPreference(NUMBER_ONE_THOUSANDTH_SEPARATOR_PREFERENCE_JSONKEY);
		}
		return _numberOneThousandthSeparator;
	}

	public String getSugarNumberDecimalSeparatorPreference() {
		if (_numberDecimalSeparator == null) {
			_numberDecimalSeparator = getSugarPreference(NUMBER_DECIMAL_SEPARATOR_PREFERENCE_JSONKEY);
		}
		return _numberDecimalSeparator;
	}

	private String getSugarPreference(String jsonKeyString) {
		String pref = null;

		if (_jsonObject != null) {
			try {
				// 56713 - fix the cast exception
				String jsonString = "" +  _jsonObject.get(jsonKeyString);
				if (jsonString != null) {
					pref = jsonString.toString();
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}

		return pref;
	}

	public static SugarDashboardPreference getInstance() {
		if (_preference == null) {
			_preference = new SugarDashboardPreference();
		}
		return _preference;
	}

	// Calling web service to get Sugar localization preference. The result _jsonObject is a
	// static Object. If user changes connection url/credential at the Notes Preference page,
	// _jsonObject will be updated.
	// 
	public void getSugarPreference() {

		Job getSugarPreferenceJob = new Job("getSugarPreferenceJob") //$NON-NLS-1$
		{

			protected IStatus run(IProgressMonitor monitor) {
				StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
				sb.append("userid=").append(NotesAccountManager.getInstance().getCRMUser()).append(ConstantStrings.AMPERSAND); //$NON-NLS-1$
				sb.append("password=").append(NotesAccountManager.getInstance().getCRMPassword()).append((ConstantStrings.AMPERSAND)); //$NON-NLS-1$

				sb.append("method=getUserPreferencesForClient").append(ConstantStrings.AMPERSAND).append("arguments={}"); //$NON-NLS-1$ //$NON-NLS-2$

				String output = SugarWebservicesOperations.getInstance().callNativeSugarRestWebService(sb.toString());

				if (output != null) {
					try {
						_jsonObject = new JSONObject(output);

						_nameFormat = null;
						_dateFormat = null;
						_timeFormat = null;
						_currencySymbol = null;
						_numberOneThousandthSeparator = null;
						_numberDecimalSeparator = null;

					} catch (Exception e) {
						UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
					}
				}
				return Status.OK_STATUS;
			}
		};

		getSugarPreferenceJob.schedule();
	}
	public String getSugarMaxAccountMyItemsPreference() {
		String pref = getSugarPreference(MAX_ACCOUNT_MYITEMS_PREFERENCE_JSONKEY);
		if (pref != null) {
			return pref;
		} else {
			return "5000";
		}
	}
	public String getSugarMaxContactMyItemsPreference() {
		String pref = getSugarPreference(MAX_CONTACT_MYITEMS_PREFERENCE_JSONKEY);
		if (pref != null) {
			return pref;
		} else {
			return "5000";
		} 
	}
	public String getSugarMaxRegexBatchSizePreference() {
		String pref = getSugarPreference(REGEX_BATCH_SIZE_PREFERENCE_JSONKEY);
		if (pref != null) {
			return pref;
		} else {
			return "500";
		}
	}

}
