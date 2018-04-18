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

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarOpportunity;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;

public class TypeaheadOpportunitiesParser extends AbstractTypeaheadResultParser {
	String[] _webServiceResultKeys = { ConstantStrings.DATABASE_NAME, ConstantStrings.DATABASE_DESCRIPTION, ConstantStrings.DATABASE_ID, ConstantStrings.DASH };

	// pattern of the string to be displayed in the typeahead area
	int[] _typeaheadTextIndex = { 0, _webServiceResultKeys.length - 1, 1 };

	// pattern of the string to be displayed for the itemfromdocument option with typeahead WS
	int[] _itemFromDocumentWithTypeaheadTextIndex = { 0, _webServiceResultKeys.length - 1, 1 };

	// ======================================================================================

	String[] _itemFromDocumentWebServiceResultKeys = { ConstantStrings.DATABASE_NAME, ConstantStrings.DATABASE_DESCRIPTION, ConstantStrings.DATABASE_ID, ConstantStrings.DASH };

	// pattern of the string to be displayed in the "item from Document" area
	int[] _itemFromDocumentTextIndex = { 0, _webServiceResultKeys.length - 1, 1 };

	// ======================================================================================
	// pattern of the string to be included in the associated information
	int[] _associatedTextIndex = { 0 };

	public String getSugarValue(BaseSugarEntry sugarEntry, String key) {
		String string = null;
		if (sugarEntry != null && key != null && sugarEntry instanceof SugarOpportunity) {
			if (key.equalsIgnoreCase(ConstantStrings.DATABASE_NAME)) {
				string = ((SugarOpportunity) sugarEntry).getName();
			} else if (key.equalsIgnoreCase(ConstantStrings.DATABASE_DESCRIPTION)) {
				string = ((SugarOpportunity) sugarEntry).getDescription();
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
