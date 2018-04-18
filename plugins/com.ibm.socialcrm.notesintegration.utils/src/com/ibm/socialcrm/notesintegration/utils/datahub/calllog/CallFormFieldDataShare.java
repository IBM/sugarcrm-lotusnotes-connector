package com.ibm.socialcrm.notesintegration.utils.datahub.calllog;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.util.Iterator;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONObject;

import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.datahub.LoadableSFADataShare;
import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataShare;

public class CallFormFieldDataShare extends LoadableSFADataShare<String, String, JSONObject> {
	public static final String NAME = "name"; //$NON-NLS-1$
	public static final String LABEL = "label"; //$NON-NLS-1$
	public static final String TYPE = "type"; //$NON-NLS-1$
	public static final String LEN = "len"; //$NON-NLS-1$
	public static final String REQUIRED = "required"; //$NON-NLS-1$
	public static final String OPTIONS = "options"; //$NON-NLS-1$
	public static final String SUBFIELDS = "subfields"; //$NON-NLS-1$

	public CallFormFieldDataShare(String name) {
		super(name);
	}

	@Override
	protected boolean doLoad(JSONObject formData) {
		boolean success = true;

		try {
			if (!formData.isNull(CallFormFieldDataShare.LABEL)) {
				put(CallFormFieldDataShare.LABEL, formData.getString(CallFormFieldDataShare.LABEL));
			}
			if (!formData.isNull(CallFormFieldDataShare.TYPE)) {
				put(CallFormFieldDataShare.TYPE, formData.getString(CallFormFieldDataShare.TYPE));
			}
			if (!formData.isNull(CallFormFieldDataShare.LEN)) {
				put(CallFormFieldDataShare.LEN, formData.getString(CallFormFieldDataShare.LEN));
			}
			if (!formData.isNull(CallFormFieldDataShare.REQUIRED)) {
				put(CallFormFieldDataShare.REQUIRED, formData.getString(CallFormFieldDataShare.REQUIRED));
			}

			if (formData.has(CallFormFieldDataShare.OPTIONS) && !formData.isNull(CallFormFieldDataShare.OPTIONS)) {
				FieldOptionsDataShare optionsDataShare = new FieldOptionsDataShare();
				addChildShare(optionsDataShare);

				// 86889 - In general, API returned enum as an JSONObject, and key-value pairs within the JSONObject...
				// But, for duration_hours, API returned enum as JSONArray with only values.
				// JSONObject optionsMap = formData.getJSONObject(CallFormFieldDataShare.OPTIONS);

				JSONObject optionsMap = new JSONObject();
				if (formData.containsKey(CallFormFieldDataShare.OPTIONS)) {
					Object obj = formData.get(CallFormFieldDataShare.OPTIONS);
					if (obj instanceof JSONObject) {
						optionsMap = formData.getJSONObject(CallFormFieldDataShare.OPTIONS);
					} else if (obj instanceof JSONArray) {

						JSONArray jarray = (JSONArray) obj;
						Iterator iterator = jarray.iterator();
						while (iterator.hasNext()) {
							String i = (String) iterator.next();
							optionsMap.put(i, i);
						}
					}
				}

				success &= optionsDataShare.loadDataShare(optionsMap);
			}
			if (formData.has(CallFormFieldDataShare.SUBFIELDS) && !formData.isNull(CallFormFieldDataShare.SUBFIELDS)) {
				SFADataShare subFieldsShare = new SFADataShare(CallFormFieldDataShare.SUBFIELDS);
				addChildShare(subFieldsShare);
				JSONArray subFields = formData.getJSONArray(CallFormFieldDataShare.SUBFIELDS);
				for (int i = 0; i < subFields.length(); i++) {
					JSONObject subFieldMap = subFields.getJSONObject(i);
					CallFormFieldDataShare subFieldDataShare = new CallFormFieldDataShare(subFieldMap.getString(NAME));
					subFieldDataShare.loadDataShare(subFieldMap);
					subFieldsShare.addChildShare(subFieldDataShare);
				}
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
			success = false;
		}
		return success;
	}

}
