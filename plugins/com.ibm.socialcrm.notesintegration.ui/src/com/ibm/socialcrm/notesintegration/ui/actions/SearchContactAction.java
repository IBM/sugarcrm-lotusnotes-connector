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

import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class SearchContactAction extends AbstractSugarAction {
	@Override
	public void doRun() {
		UiUtils.displaySugarItem(SugarType.CONTACTS, getSelectedText(), getSelectedText().indexOf("@") == -1 ? ActionSearchFilter.SEARCH_BY_NAME : ActionSearchFilter.SEARCH_BY_EMAIL); //$NON-NLS-1$
	}

}