package com.ibm.socialcrm.notesintegration.core.extensionpoints;

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
