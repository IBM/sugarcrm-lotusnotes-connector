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

public class CreateOpportunityToolbarAction extends AbstractCreateToolbarAction {
	@Override
	public String getUrl() {
		return NotesAccountManager.getInstance().getCRMServer() + "index.php?module=Opportunities&action=EditView&return_module=Opportunities&return_action=DetailView"; //$NON-NLS-1$
	}

}
