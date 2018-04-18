package com.ibm.socialcrm.notesintegration.ui.actions;

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

import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchWindow;

import com.ibm.rcp.ui.shelf.IWorkbenchWindowWithShelfPages;
import com.ibm.rcp.ui.shelf.ShelfPage;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.AbstractToolbarMenuItem;
import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataHub;
import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataShare;
import com.ibm.socialcrm.notesintegration.utils.datahub.calllog.CurrentSugarEntryDataShare;

public class OpenCallLog extends AbstractToolbarMenuItem {

	public OpenCallLog(BaseSugarEntry sugarEntry, String id) {
		super(sugarEntry, id);
	}

	@Override
	public String getItemText() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_OPEN_CALL_LOG);
	}

	@Override
	public void onSelection() {
		try {
			IAdapterManager adapterMgr = Platform.getAdapterManager();
			IWorkbenchWindow window = GenericUtils.getMainNotesWindow();
			IWorkbenchWindowWithShelfPages windowWithShelfPages = (IWorkbenchWindowWithShelfPages) adapterMgr.getAdapter(window, IWorkbenchWindowWithShelfPages.class);

			if (getCurrentSugarEntryDataShare() != null) {
				getCurrentSugarEntryDataShare().put(CurrentSugarEntryDataShare.CURRENT_SUGAR_ENTRY, getSugarEntry());
			}

			final ShelfPage shelfPage = windowWithShelfPages.getShelfPage(IWorkbenchWindowWithShelfPages.RIGHT);
			shelfPage.showView( "com.ibm.socialcrm.notesintegration.sidebar.consolidatedView"); //$NON-NLS-1$
		} catch (Exception e) {
		UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}
	}

	/**
	 * Returns the data share that contains the current BaseSugarEntry (from a call logging perspective)
	 */
	private SFADataShare getCurrentSugarEntryDataShare() {
		return SFADataHub.getInstance().getDataShare(CurrentSugarEntryDataShare.SHARE_NAME);
	}

	@Override
	public boolean shouldEnable() {
		return true;
	}

}
