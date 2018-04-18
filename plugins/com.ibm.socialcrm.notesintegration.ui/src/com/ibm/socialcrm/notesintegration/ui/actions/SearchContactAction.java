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

import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class SearchContactAction extends AbstractSugarAction {
	@Override
	public void doRun() {
		UiUtils.displaySugarItem(SugarType.CONTACTS, getSelectedText(), getSelectedText().indexOf("@") == -1 ? ActionSearchFilter.SEARCH_BY_NAME : ActionSearchFilter.SEARCH_BY_EMAIL); //$NON-NLS-1$
	}

}
