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

import java.util.Map;

import org.apache.commons.json.JSONObject;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.core.utils.SugarDashboardPreference;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;

public class TypeaheadContactsParser extends AbstractTypeaheadResultParser {

	private static String ACCOUNT_NAME = "account_name";

	String[] _webServiceResultKeys = { ConstantStrings.DATABASE_TITLE, ConstantStrings.DATABASE_FIRST_NAME, ConstantStrings.DATABASE_LAST_NAME, ACCOUNT_NAME, ConstantStrings.DATABASE_ID, //$NON-NLS-1$
			ConstantStrings.ACCOUNT_ID, ConstantStrings.DATABASE_NAME, FORMAT_NAME_TAG + "1", //$NON-NLS-1$
			ConstantStrings.SPACE, ConstantStrings.DASH };

	// pattern of the string to be displayed in the typeahead area
	int[] _typeaheadTextIndex = { 6 };

	// pattern of the string to be displayed for the itemfromdocument option with typeahead WS
	int[] _itemFromDocumentWithTypeaheadTextIndex = { 6 };

	// ======================================================================================
	String[] _itemFromDocumentWebServiceResultKeys = { ConstantStrings.DATABASE_TITLE, ConstantStrings.DATABASE_FIRST_NAME, ConstantStrings.DATABASE_LAST_NAME, ACCOUNT_NAME,
			ConstantStrings.DATABASE_ID, //$NON-NLS-1$
			ConstantStrings.ACCOUNT_ID, ConstantStrings.DATABASE_NAME, FORMAT_NAME_TAG + "1", //$NON-NLS-1$
			ConstantStrings.SPACE, ConstantStrings.DASH };

	// pattern of the string to be displayed in the "item from document" area
	int[] _itemFromDocumentTextIndex = { 6 };

	// ======================================================================================
	// pattern of the string to be included in the associated information
	// int[] _associatedTextIndex = { 1, 2 };
	int[] _associatedTextIndex = { 6 };

	// ======================================================================================
	// pattern of the string to be included in the associated information
	int[] _associatedExtendedTextindex = { 5 };

	// Override me if you need formatted name.
	public String getFormattedName(Object object, String key) {
		String text = null;
		if (key != null && key.equalsIgnoreCase(FORMAT_NAME_TAG + "1")) {
			String firstName = null;
			String lastName = null;

			if (object != null && object instanceof JSONObject) {
				firstName = getJSONValue((JSONObject) object, getWebServiceResultKeys()[1]);
				lastName = getJSONValue((JSONObject) object, getWebServiceResultKeys()[2]);

			} else if (object != null && object instanceof SugarContact) {
				firstName = getFirstNameFromSugar((SugarContact) object);
				lastName = getLastNameFromSugar((SugarContact) object);
			} else if (object != null && object instanceof Map) {
				firstName = (String) ((Map) object).get((Object) getWebServiceResultKeys()[1]);
				lastName = (String) ((Map) object).get((Object) getWebServiceResultKeys()[2]);
			}

			if (firstName != null && lastName != null) {
				text = SugarDashboardPreference.getInstance().getFormattedNameWithoutSalutation(firstName, lastName);
				// Sometimes user_default_locale_name_format in preference is not found, it will return
				// a null text. In this case, construct text as first name + " " + last name.
				if (text == null) {
					text = firstName + ConstantStrings.SPACE + lastName;
				}

			}

		}
		return text;
	}

	private String getFirstNameFromSugar(SugarContact sugarContact) {
		String name = null;
		if (sugarContact != null) {
			name = sugarContact.getFirstName();
		}
		return name;
	}

	private String getLastNameFromSugar(SugarContact sugarContact) {
		String name = null;
		if (sugarContact != null) {
			name = sugarContact.getLastName();
		}
		return name;
	}

	@Override
	public int[] getAssociatedTextIndex() {
		return _associatedTextIndex;
	}

	@Override
	public String[] getWebServiceResultKeys() {
		return _webServiceResultKeys;
	}

	@Override
	public int[] getTypeaheadTextIndex() {
		return _typeaheadTextIndex;
	}

	@Override
	public int[] getAssociatedExtendedTextIndex() {
		return _associatedExtendedTextindex;
	}

	@Override
	public int[] getItemFromDocumentTextIndex() {
		return _itemFromDocumentTextIndex;
	}

	public String getSugarValue(BaseSugarEntry sugarEntry, String key) {
		String string = null;
		if (sugarEntry != null && key != null && sugarEntry instanceof SugarContact) {
			if (key.equalsIgnoreCase(ACCOUNT_NAME)) {
				string = ((SugarContact) sugarEntry).getAccountName();
			} else if (key.equalsIgnoreCase(ConstantStrings.DATABASE_FIRST_NAME)) {
				string = ((SugarContact) sugarEntry).getFirstName();
			} else if (key.equalsIgnoreCase(ConstantStrings.DATABASE_LAST_NAME)) {
				string = ((SugarContact) sugarEntry).getLastName();
			} else if (key.equalsIgnoreCase(ConstantStrings.ACCOUNT_ID)) {
				string = ((SugarContact) sugarEntry).getAccountID();
			} else if (key.equalsIgnoreCase(ConstantStrings.DATABASE_NAME)) {
				string = ((SugarContact) sugarEntry).getName();
			}
		}
		return string;
	}

	@Override
	public String[] getItemFromDocumentWebServiceResultKeys() {
		return _itemFromDocumentWebServiceResultKeys;
	}

	@Override
	public int[] getItemFromDocumentWithTypeaheadTextIndex() {
		return _itemFromDocumentWithTypeaheadTextIndex;
	}

}
