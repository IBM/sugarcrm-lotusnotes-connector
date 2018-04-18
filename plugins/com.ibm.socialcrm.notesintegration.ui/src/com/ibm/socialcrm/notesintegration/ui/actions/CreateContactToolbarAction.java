package com.ibm.socialcrm.notesintegration.ui.actions;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;

public class CreateContactToolbarAction extends AbstractCreateToolbarAction {
	@Override
	public String getUrl() {
		return NotesAccountManager.getInstance().getCRMServer() + "index.php?module=Contacts&action=EditView&return_module=Contacts&return_action=index"; //$NON-NLS-1$
	}
}
