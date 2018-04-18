package com.ibm.socialcrm.notesintegration.utils.datahub.calllog;

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

import java.util.Iterator;

import org.apache.commons.json.JSONObject;

import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.datahub.LoadableSFADataShare;

/**
 * There's nothing special in here now. This just acts as a normal SFADataShare. The caller will know what it is based on the type though.
 */
public class FieldOptionsDataShare extends LoadableSFADataShare<Object, Object, JSONObject> {
	public static final String SHARE_NAME = "options"; //$NON-NLS-1$

	public FieldOptionsDataShare() {
		super(SHARE_NAME);
	}

	@Override
	protected boolean doLoad(JSONObject optionsMap) {
		boolean success = true;
		try {
			Iterator iter = optionsMap.keys();
			while (iter.hasNext()) {
				String optionKey = iter.next().toString();
				put(optionKey, optionsMap.get(optionKey));
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
			success = false;
		}

		return success;
	}

}
