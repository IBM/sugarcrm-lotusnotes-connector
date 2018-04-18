package com.ibm.socialcrm.notesintegration.connector.util;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
