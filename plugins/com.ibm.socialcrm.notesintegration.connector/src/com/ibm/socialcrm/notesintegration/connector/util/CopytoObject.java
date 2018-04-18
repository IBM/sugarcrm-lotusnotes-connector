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

import com.ibm.socialcrm.notesintegration.ui.connector.AssociateData;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

/*
 * This object contains inf. for each item user wants to copy to SugarCRM
 */
public class CopytoObject {
	private AssociateData _associatedData = null;
	private String _associatedType = null;
	private SugarType _associatedSugarType = null;
	private String _objectKey;
	private String _displayName = null;

	public CopytoObject(AssociateData associateData, String type, SugarType sugarType) {
		_associatedData = associateData;
		_associatedType = type;
		_associatedSugarType = sugarType;

		_objectKey = createObjectKey();
		_displayName = createDisplayName();
	}

	private String createObjectKey() {
		String key = null;
		if (_associatedData != null) {
			key = _associatedData.getId();
		}
		return key;
	}

	public String createDisplayName() {
		String displayName = ConstantStrings.EMPTY_STRING;
		if (_associatedData != null) {
			displayName = ConnectorUtil.getFormattedName(_associatedData.getName());
		}
		return displayName;
	}

	public AssociateData getAssociatedData() {
		return _associatedData;
	}

	public String getAssociatedType() {
		return _associatedType;
	}

	public SugarType getAssociateSugarType() {
		return _associatedSugarType;
	}

	public String getObjectKey() {
		return _objectKey;
	}

	public String getDisplayName() {
		return _displayName;
	}

}
