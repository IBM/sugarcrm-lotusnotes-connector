package com.ibm.socialcrm.notesintegration.connector;

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
