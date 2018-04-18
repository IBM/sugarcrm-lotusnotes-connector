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

import java.util.Comparator;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONObject;

import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.datahub.LoadableSFADataShare;
import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataShare;

/**
 * Data share for the high level call form information. This will hold information about individual fields.
 * 
 * @author bcbull
 */
public class CallFormDataShare extends LoadableSFADataShare<Object, Object, JSONObject> {
	// There should only be one instance of this share. This is the preferred name.
	public static final String SHARE_NAME = "callLogFormData"; //$NON-NLS-1$

	public static final String BASE_KEY = "key"; //$NON-NLS-1$

	private Comparator childShareComparator;

	private static CallFormDataShare instance;

	public static CallFormDataShare getInstance() {
		if (instance == null) {
			instance = new CallFormDataShare();
		}
		return instance;
	}

	private CallFormDataShare() {
		super(SHARE_NAME);
	}

	@Override
	protected boolean doLoad(JSONObject callForm) {
		boolean success = true;
		try {
			// JSONObject formEntry = formArray.getJSONObject(0);
			int ctr = 0;

			while (callForm.containsKey(BASE_KEY + ctr)) {
				// Create a data share underneath the call form share to house the key. Then
				// put the actual field data underneath that
				SFADataShare keyShare = new SFADataShare<String, String>(BASE_KEY + ctr);
				addChildShare(keyShare);

				try {
					JSONObject formData = callForm.getJSONObject(BASE_KEY + ctr);
					CallFormFieldDataShare fieldDataShare = new CallFormFieldDataShare(formData.getString(CallFormFieldDataShare.NAME));
					success &= fieldDataShare.loadDataShare(formData);
					keyShare.addChildShare(fieldDataShare);
				} catch (Exception e) {
					// If someone modifies the call form on the server, we may get a null object
					// back, just eat it and move on.
					UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
				}
				ctr++;
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
			success = false;
		}
		return success;
	}

	@Override
	protected Comparator getChildShareComparator() {
		if (childShareComparator == null) {
			childShareComparator = new Comparator<String>() {
				@Override
				public int compare(String shareKey1, String shareKey2) {
					int compare = 1;
					// If we're comparing keyX vs keyY, do a numeric, not asciibetical comparison
					if (shareKey1.startsWith(BASE_KEY) && shareKey2.startsWith(BASE_KEY)) {
						Integer key1 = Integer.parseInt(shareKey1.substring(BASE_KEY.length()));
						Integer key2 = Integer.parseInt(shareKey2.substring(BASE_KEY.length()));
						compare = key1.compareTo(key2);
					} else {
						compare = shareKey1.compareTo(shareKey2);
					}
					return compare;
				}
			};
		}
		return childShareComparator;
	}

}
