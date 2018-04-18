package com.ibm.socialcrm.notesintegration.ui.dnd;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;

/*
 * mainly used to facilitate DND a mail/meeting to a live text card
 */
public class SugarDashboardDndEntry {
	BaseSugarEntry _sugarEntry = null;
	String _docid = null;

	public SugarDashboardDndEntry(BaseSugarEntry sugarEntry, String docid) {
		_sugarEntry = sugarEntry;
		_docid = docid;
	}

	public BaseSugarEntry getSugarEntry() {
		return _sugarEntry;
	}

	public String getDocid() {
		return _docid;
	}

}
