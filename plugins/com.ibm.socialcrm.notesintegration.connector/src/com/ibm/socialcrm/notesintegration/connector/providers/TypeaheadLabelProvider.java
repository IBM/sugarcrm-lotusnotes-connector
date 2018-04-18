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

import org.apache.commons.json.JSONObject;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import com.ibm.socialcrm.notesintegration.connector.util.ConnectorUtil;

public class TypeaheadLabelProvider implements ILabelProvider {

	TypeaheadCollectionModel _model = null;

	public Image getImage(Object arg0) {
		return null;
	}

	public String getText(Object arg0) {

		String txt = null;
		if (arg0 != null && arg0 instanceof JSONObject) {
			txt = ConnectorUtil.getInstance().getResultParser(getCollectionModel().getCacheSugarType()).getTypeaheadText((JSONObject) arg0);
		}
		return txt;
	}

	public void addListener(ILabelProviderListener arg0) {
	}

	public void dispose() {
	}

	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	public void removeListener(ILabelProviderListener arg0) {
	}

	public void setCollectionModel(TypeaheadCollectionModel model) {
		_model = model;
	}

	public TypeaheadCollectionModel getCollectionModel() {
		return _model;
	}
}
