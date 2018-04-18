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

import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;

public class CreateContactToolbarAction extends AbstractCreateToolbarAction {
	@Override
	public String getUrl() {
		return NotesAccountManager.getInstance().getCRMServer() + "index.php?module=Contacts&action=EditView&return_module=Contacts&return_action=index"; //$NON-NLS-1$
	}
}