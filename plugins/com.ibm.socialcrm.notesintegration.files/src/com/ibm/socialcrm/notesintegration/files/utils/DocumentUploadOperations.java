package com.ibm.socialcrm.notesintegration.files.utils;

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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DocumentUploadOperations extends AbstractDocumentUploadOperations {

	public InputStream getFileInputStream(String filename, File filefile) {
		// InputStream is = null;
		FileInputStream fis = null;
		if (filename != null) {
			File file = filefile;
			if (file == null) {
				file = new File(filename);
			}

			try {
				fis = new FileInputStream(file);

			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return fis;
	}

}
