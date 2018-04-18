package com.ibm.socialcrm.notesintegration.sametime.actions;

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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.AbstractToolbarMenuItem;
import com.ibm.socialcrm.notesintegration.sametime.SametimePluginActivator;
import com.ibm.socialcrm.notesintegration.sametime.utils.SametimeUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class SametimeLogin extends AbstractToolbarMenuItem {

	public SametimeLogin(BaseSugarEntry sugarEntry, String id) {
		super(sugarEntry, id);
	}

	@Override
	public String getItemText() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_SAMETIME_LOGIN);
	}

	@Override
	public void onSelection() {
		try {
			if (SametimeUtils.getCommunityService() != null && SametimeUtils.getCommunityService().getDefaultCommunity() != null
					&& SametimeUtils.getCommunityService().getDefaultCommunity().getHost() != null
					&& !SametimeUtils.getCommunityService().getDefaultCommunity().getHost().equals(ConstantStrings.EMPTY_STRING)) {
				SametimeUtils.getCommunityService().getDefaultCommunity().login();
			} else {
				MessageDialog.openError(Display.getDefault().getActiveShell(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_SAMETIME_LOGIN_ERROR_TITLE), UtilsPlugin.getDefault()
						.getResourceString(UtilsPluginNLSKeys.UI_SAMETIME_LOGIN_ERROR));
			}

		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, SametimePluginActivator.PLUGIN_ID);
		}
	}

	@Override
	public boolean shouldEnable() {
		return !SametimeUtils.isLoggedIn();
	}

}
