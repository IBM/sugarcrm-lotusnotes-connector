package com.ibm.socialcrm.notesintegration.files.utils;

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
