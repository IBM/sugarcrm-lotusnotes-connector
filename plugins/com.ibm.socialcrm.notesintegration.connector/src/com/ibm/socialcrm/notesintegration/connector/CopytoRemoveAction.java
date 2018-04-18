package com.ibm.socialcrm.notesintegration.connector;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import org.eclipse.jface.action.Action;

public class CopytoRemoveAction extends Action {
	AssociateComposite _associateComposite;
	String _id = null;

	public CopytoRemoveAction(String id, AssociateComposite associateComposite) {
		super();
		_id = id;
		_associateComposite = associateComposite;
	}

	public void run() {
		if (_associateComposite != null) {
			_associateComposite.removeCopyto(_id);
		}
	}

}
