package com.ibm.socialcrm.notesintegration.connector.providers;

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

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class AttachmentViewerContentProvider implements IStructuredContentProvider {

	public AttachmentViewerContentProvider() {
	}

	@Override
	public Object[] getElements(Object arg0) {
		String[] strings = null;
		if (arg0 == null) {
			strings = new String[0];
		} else {
			Object[] objects = (Object[]) arg0;
			strings = (String[]) objects;
		}

		return strings;
	}

	@Override
	public void dispose() {

	}

	@Override
	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {

	}

}
