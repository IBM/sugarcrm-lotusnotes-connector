package com.ibm.socialcrm.notesintegration.connector.util;

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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import lotus.domino.Document;
import lotus.domino.EmbeddedObject;
import lotus.domino.Item;
import lotus.domino.RichTextItem;

import com.ibm.socialcrm.notesintegration.connector.Activator;
import com.ibm.socialcrm.notesintegration.files.utils.AbstractDocumentUploadOperations;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;

public class NotesDocumentUploadOperations extends AbstractDocumentUploadOperations {

	private Document _mailDoc = null;

	public InputStream getFileInputStream(String filename, File filefile) {
		InputStream is = null;
		EmbeddedObject eo = getAttachmentObject(filename);
		try {
			is = eo.getInputStream();
		} catch (Exception e) {

		}
		return is;
	}

	public void setDatabaseData(Object o) {
		_mailDoc = (Document) o;
	}

	private EmbeddedObject getAttachmentObject(String attachmentName) {
		EmbeddedObject eo = null;
		if (_mailDoc != null && attachmentName != null) {
			List<String> nameList = new ArrayList<String>();
			try {
				Item item = _mailDoc.getFirstItem("Body"); //$NON-NLS-1$

				if (item instanceof RichTextItem) {
					RichTextItem body = (RichTextItem) item;
					if (body != null) {
						eo = body.getEmbeddedObject(attachmentName);
					}
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}
		}
		return eo;
	}

}
