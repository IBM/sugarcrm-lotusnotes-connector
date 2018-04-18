package com.ibm.socialcrm.notesintegration.connector.providers;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
