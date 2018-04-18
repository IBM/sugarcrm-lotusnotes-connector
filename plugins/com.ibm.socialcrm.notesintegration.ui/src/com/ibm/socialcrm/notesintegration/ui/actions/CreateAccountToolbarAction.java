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

public class CreateAccountToolbarAction extends AbstractCreateToolbarAction {
	@Override
	public String getUrl() {
		return NotesAccountManager.getInstance().getCRMServer() + "index.php?module=Accounts&action=EditView&return_module=Accounts&return_action=index"; //$NON-NLS-1$
	}
}
