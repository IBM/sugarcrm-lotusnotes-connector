package com.ibm.socialcrm.notesintegration.connector.util;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarAccount;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;

public class TypeaheadAccountsParser extends AbstractTypeaheadResultParser {

	String[] _webServiceResultKeys = {ConstantStrings.DATABASE_BILLING_ADDRESS_CITY, ConstantStrings.DATABASE_NAME, ConstantStrings.DATABASE_CCMS_ID, ConstantStrings.DATABASE_ID,
			ConstantStrings.LEFT_PARENTHESIS, ConstantStrings.RIGHT_PARENTHESIS, ConstantStrings.SPACE};

	int[] _typeaheadTextIndex = {1};

	// pattern of the string to be displayed for the itemfromdocument option with typeahead WS
	int[] _itemFromDocumentWithTypeaheadTextIndex = {1};

	// ======================================================================================

	String[] _itemFromDocumentWebServiceResultKeys = {ConstantStrings.DATABASE_BILLING_ADDRESS_CITY, ConstantStrings.DATABASE_NAME, ConstantStrings.DATABASE_CCMS_ID, ConstantStrings.DATABASE_ID,
			ConstantStrings.LEFT_PARENTHESIS, ConstantStrings.RIGHT_PARENTHESIS, ConstantStrings.SPACE};

	// pattern of the string to be displayed in the "item from document" area
	int[] _itemFromDocumentTextIndex = {1};

	// ======================================================================================
	// pattern of the string to be included in the associated information
	int[] _associatedTextIndex = {1};

	public String getSugarValue(BaseSugarEntry sugarEntry, String key) {
		String string = null;
		if (sugarEntry != null && key != null && sugarEntry instanceof SugarAccount) {
			if (key.equalsIgnoreCase(ConstantStrings.DATABASE_NAME)) {
				string = ((SugarAccount) sugarEntry).getName();
			} else if (key.equalsIgnoreCase(ConstantStrings.DATABASE_BILLING_ADDRESS_CITY)) {
				string = ((SugarAccount) sugarEntry).getCity();
			}

			else if (key.equalsIgnoreCase(ConstantStrings.DATABASE_CCMS_ID)) {
				string = ((SugarAccount) sugarEntry).getClientId();
			}
		}
		return string;
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
	public int[] getItemFromDocumentTextIndex() {
		return _itemFromDocumentTextIndex;
	}

	@Override
	public int[] getAssociatedExtendedTextIndex() {
		return null;
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
