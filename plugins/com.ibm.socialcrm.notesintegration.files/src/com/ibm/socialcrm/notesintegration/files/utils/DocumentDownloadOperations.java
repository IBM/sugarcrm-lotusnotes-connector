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

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.json.JSONObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;

import com.ibm.socialcrm.notesintegration.core.ConnectionsDocument;
import com.ibm.socialcrm.notesintegration.core.utils.AbderaConnectionsFileOperations;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.files.FilesPluginActivator;
import com.ibm.socialcrm.notesintegration.ui.custom.SFAMessageDialogWithHyperlink;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class DocumentDownloadOperations {
	public final static int DOCUMENT_DOWNLOAD_OPEN = 1;
	public final static int DOCUMENT_DOWNLOAD_SAVE = 2;
	private final static int Forbidden_403 = 403;
	private int _option = 0;
	private ConnectionsDocument _cd = null;
	private File _targetFile = null;

	public DocumentDownloadOperations(int option, ConnectionsDocument cd) {
		_option = option;
		_cd = cd;
	}

	public void toExecute() {
		if (_option == DOCUMENT_DOWNLOAD_OPEN || _option == DOCUMENT_DOWNLOAD_SAVE) {
			boolean isOK = toDownloadFile();

			if (!isOK && isUnauthorized()) {
				// call download validation API
				String output = SugarWebservicesOperations.getInstance().validateDownload(_cd.getSugarId());

				isOK = isDownloadValidated(output);
				if (isOK) {
					isOK = toDownloadFile();
				} else {
					return;
				}
			}
			if (isOK) {
				if (_option == DOCUMENT_DOWNLOAD_OPEN) {

					try {
						Desktop.getDesktop().open(_targetFile);
					} catch (Exception e) {
						e.printStackTrace();
					}

					// _targetFile.deleteOnExit();
				}

			} else {
				StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
				sb.append(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLAOD_FAILED)).append(ConstantStrings.LEFT_PARENTHESIS).append(
						AbderaConnectionsFileOperations._downloadStatus).append(ConstantStrings.RIGHT_PARENTHESIS);
				final String msg = sb.toString();

				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						SFAMessageDialogWithHyperlink.open(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_ERROR_TITLE), SFAImageManager
								.getImage(SFAImageManager.SALES_CONNECT), msg);

					}
				});
			}
		}
	}

	private boolean isUnauthorized() {
		boolean isUnauthorized = false;
		if (AbderaConnectionsFileOperations._downloadStatus == Forbidden_403) {
			isUnauthorized = true;
		}
		return isUnauthorized;
	}

	public boolean buildTargetFile() {
		boolean isOK = true;
		String directory = null;
		if (_option == DOCUMENT_DOWNLOAD_SAVE) {
			DirectoryDialog dd = new DirectoryDialog(Display.getDefault().getActiveShell(), SWT.NONE);
			directory = dd.open();
		} else if (_option == DOCUMENT_DOWNLOAD_OPEN) {
			directory = GenericUtils.getUniqueTempDir();
		}
		if (directory != null) {
			File dir = new File(directory);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			_targetFile = new File(directory, _cd.getFilename());
		} else {
			isOK = false;
		}
		return isOK;
	}

	private boolean toDownloadFile() {

		boolean isOK = false;
		AbderaConnectionsFileOperations._downloadStatus = 0;

		InputStream in = null;
		FileOutputStream out = null;
		try {
			in = AbderaConnectionsFileOperations.downloadFile(_cd.getDocUrl());
			if (in != null) {
				out = new FileOutputStream(_targetFile);

				byte[] buffer = new byte[4096];
				int bytesRead = 0;
				while ((bytesRead = in.read(buffer)) != -1) {
					out.write(buffer, 0, bytesRead);
				}
				out.flush();
				out.close();

				isOK = true;

			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, FilesPluginActivator.PLUGIN_ID);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (Exception e) {
				// Eat it
			}
		}
		return isOK;
	}

	private boolean isDownloadValidated(String output) {
		boolean isOK = false;
		if (output != null && !output.equals(ConstantStrings.EMPTY_STRING)) {
			try {
				JSONObject jsonObject = new JSONObject(output);
				boolean isTrueResponse = jsonObject.getBoolean("response"); //$NON-NLS-1$
				int errorCode = jsonObject.getInt("code"); //$NON-NLS-1$
				String errorMsg = jsonObject.getString("message"); //$NON-NLS-1$

				if (isTrueResponse) {
					isOK = true;
				} else {
					StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

					// not authorized, but no fancy message from API
					if (errorCode == 0) {
						sb.append(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLAOD_NOT_AUTHORIZED));
					} else

					// error from our API
					if (errorCode < 0) {
						sb.append(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLAOD_FAILED)).append(ConstantStrings.LEFT_PARENTHESIS).append(errorCode).append(
								ConstantStrings.RIGHT_PARENTHESIS);
					} else

					{
						sb.append(errorMsg).append(ConstantStrings.LEFT_PARENTHESIS).append(errorCode).append(ConstantStrings.RIGHT_PARENTHESIS);
					}

					final String msg = sb.toString();

					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							SFAMessageDialogWithHyperlink.open(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_ERROR_TITLE), SFAImageManager
									.getImage(SFAImageManager.SALES_CONNECT), msg);

						}
					});

				}

			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, FilesPluginActivator.PLUGIN_ID);
			}
		}
		return isOK;
	}
}
