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

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.dialogs.FileSystemElement;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;

public class AttachmentViewerLabelProvider extends LabelProvider implements ITableLabelProvider {
	private WorkbenchLabelProvider provider = new WorkbenchLabelProvider();

	public AttachmentViewerLabelProvider() {
	}

	public String getColumnText(Object obj, int index) {
		StringBuffer sb = new StringBuffer(ConstantStrings.SPACE);
		sb.append(ConstantStrings.SPACE).append(((String) obj).substring(1));
		return sb.toString();
	}

	public Image getColumnImage(Object obj, int index) {
		// return getImage(obj);
		return null;
	}

	public Image getImage(Object obj) {
		Image image = null;

		if (obj != null && obj instanceof String) {

			FileSystemElement fakeFile = new FileSystemElement((String) obj, null, false);
			image = provider.getImage(fakeFile);

		}
		return image;
	}

}
