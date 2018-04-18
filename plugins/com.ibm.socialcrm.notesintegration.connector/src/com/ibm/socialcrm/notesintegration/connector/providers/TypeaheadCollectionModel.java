package com.ibm.socialcrm.notesintegration.connector.providers;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.json.JSONObject;

import com.ibm.socialcrm.notesintegration.connector.util.CopytoObject;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateData;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class TypeaheadCollectionModel {
	// dialog control information
	private boolean _isMyItemsOnly = true;
	private String _cacheType = null;
	private String _cacheCity = null;
	private String _cacheText = null;
	private SugarType _cacheSugarType = null;

	// associated information
	private Map<String, CopytoObject> _copytoObjectMap = null;

	private String[] _associatedAttachmentList = null;

	private boolean _isToCopy = false;

	private JSONObject[] _wsResults = null;

	public TypeaheadCollectionModel() {
		setIsToCopy(false);
	}

	public boolean isMyItemsOnly() {
		return _isMyItemsOnly;
	}

	public void setIsMyItemsOnly(boolean b) {
		_isMyItemsOnly = b;
	}

	public String getCacheType() {
		return _cacheType;
	}

	public void setCacheType(String s) {
		_cacheType = s;
	}

	public SugarType getCacheSugarType() {

		return _cacheSugarType;
	}

	public void setCacheSugarType(SugarType type) {
		_cacheSugarType = type;
	}

	public String getCacheText() {
		return _cacheText;
	}

	public void setCacheText(String s) {
		_cacheText = s;
	}

	public String getCacheCity() {
		return _cacheCity;
	}

	public void setCacheCity(String s) {
		_cacheCity = s;
	}

	public JSONObject[] getWSResults() {
		return _wsResults;
	}

	public void setWSResults(JSONObject[] jsonObjects) {
		_wsResults = jsonObjects;
	}

	public Map<String, CopytoObject> getCopytoObjectMap() {
		return _copytoObjectMap;
	}

	public void addCopytoObjectMap(AssociateData associateData, String type, SugarType sugarType) {
		if (_copytoObjectMap == null) {
			_copytoObjectMap = new HashMap<String, CopytoObject>();
		}
		CopytoObject co = new CopytoObject(associateData, type, sugarType);
		_copytoObjectMap.put(co.getObjectKey(), co);

	}

	public void removeCopytoObjectMap(String objectKey) {
		if (objectKey != null && _copytoObjectMap != null && !_copytoObjectMap.isEmpty() && _copytoObjectMap.containsKey(objectKey)) {
			_copytoObjectMap.remove(objectKey);
		}
	}

	public String[] getAssociatedAttachmentList() {
		return _associatedAttachmentList;
	}

	public void setAssociatedAttachmentList(String[] list) {
		_associatedAttachmentList = list;
	}

	public void setIsToCopy(boolean b) {
		_isToCopy = b;
	}

	public boolean isToCopy() {
		return _isToCopy;
	}
}
