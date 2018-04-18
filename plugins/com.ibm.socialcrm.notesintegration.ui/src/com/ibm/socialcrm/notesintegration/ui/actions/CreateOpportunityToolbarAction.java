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

public class CreateOpportunityToolbarAction extends AbstractCreateToolbarAction {
	@Override
	public String getUrl() {
		return NotesAccountManager.getInstance().getCRMServer() + "index.php?module=Opportunities&action=EditView&return_module=Opportunities&return_action=DetailView"; //$NON-NLS-1$
	}

}
