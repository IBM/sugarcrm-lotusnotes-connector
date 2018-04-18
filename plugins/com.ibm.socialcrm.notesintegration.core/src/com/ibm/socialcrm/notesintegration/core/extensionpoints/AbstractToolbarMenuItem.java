package com.ibm.socialcrm.notesintegration.core.extensionpoints;

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
import com.ibm.socialcrm.notesintegration.core.IProgressDisplayer;

public abstract class AbstractToolbarMenuItem {
	private BaseSugarEntry sugarEntry;
	private String _id = null;
	private IProgressDisplayer progessDisplayer;

	public AbstractToolbarMenuItem(BaseSugarEntry sugarEntry, String id) {
		setSugarEntry(sugarEntry);
		setId(id);
	}

	public abstract boolean shouldEnable();

	public abstract String getItemText();

	public abstract void onSelection();

	public void setSugarEntry(BaseSugarEntry sugarEntry) {
		this.sugarEntry = sugarEntry;
	}

	public BaseSugarEntry getSugarEntry() {
		return sugarEntry;
	}

	public void setId(String id) {
		this._id = id;
	}

	public String getId() {
		return _id;
	}

	public IProgressDisplayer getProgessDisplayer() {
		return progessDisplayer;
	}

	public void setProgessDisplayer(IProgressDisplayer progessDisplayer) {
		this.progessDisplayer = progessDisplayer;
	}

}
