package com.ibm.socialcrm.notesintegration.ui.dnd;

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
