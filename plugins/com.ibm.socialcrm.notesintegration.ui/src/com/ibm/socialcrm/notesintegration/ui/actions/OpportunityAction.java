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

public class OpportunityAction extends AbstractSugarAction {

	@Override
	public void doRun() {
		UiUtils.displaySugarItem(SugarType.OPPORTUNITIES, getSelectedText());
	}

}
