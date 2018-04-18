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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.json.JSONObject;
import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.DocumentInfo;
import com.ibm.socialcrm.notesintegration.core.utils.AbderaConnectionsFileOperations;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.core.utils.WebServiceLogUtil;
import com.ibm.socialcrm.notesintegration.files.dialogs.FileUploadConflictResolutionDialog;
import com.ibm.socialcrm.notesintegration.files.language.LanguageDetectionServices;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateData;
import com.ibm.socialcrm.notesintegration.ui.utils.URLEncodeFilterOutputStream;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public abstract class AbstractDocumentUploadOperations {

	private static String CONTENT_TYPE = "application/x-www-form-urlencoded"; //$NON-NLS-1$

	private String posterString = null;

	// ============= file upload request ===============================================

	private static String[] FU_STRINGS_1 = {"method=ibm_upload_file_multiple&", //$NON-NLS-1$
			"arguments={", "\"file\":", "{\"filename\":"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	// ... file name goes here ...

	private static String[] FU_STRINGS_2 = {"\"language\":\""}; //$NON-NLS-1$"  
	// ... language goes here ...

	private static String[] FU_STRINGS_2a = {"\"document_id\":\""}; //$NON-NLS-1$"  
	// ... document sugar id goes here ...

	private static String[] FU_STRINGS_3 = {"\"file\":\""}; //$NON-NLS-1$
	// ... file content goes here

	private static String[] FU_STRINGS_4 = {"\"},", "\"relationships\":["}; //$NON-NLS-1$ //$NON-NLS-2$

	// repeat following (4a, 4b, 4c) for each relationship
	private static String[] FU_STRINGS_4a = {"{\"module\":\""}; //$NON-NLS-1$
	// ... module goes here (Accounts, Opportunities, Clients) ...
	private static String[] FU_STRINGS_4b = {"\",\"related_id\":\""}; //$NON-NLS-1$
	// ... id goes here
	private static String[] FU_STRINGS_4c = {"\"}"}; //$NON-NLS-1$

	private static String[] FU_STRINGS_5 = {"]}"}; //$NON-NLS-1$

	// ====== results =========================================
	Map<String, DocumentUploadObject> _okResultsMap = null;
	Map<String, DocumentUploadObject> _badResultsMap = null;
	private boolean success = true;
	private ByteArrayInputStream bis = null;

	public boolean doUploadDocument(TreeMap<String, List<AssociateData>> sugarItemMap, Map<File, DocumentUploadObject> documentsToBeUploaded) {
		getSuccessfulResultsMap().clear();
		getFailedResultsMap().clear();

		boolean isContinue = true;
		boolean isFirst = true;
		List<String> createdRelationshipIdList = new ArrayList<String>();

		StringBuffer sbRelationship = new StringBuffer(ConstantStrings.EMPTY_STRING);

		Iterator<Map.Entry<String, List<AssociateData>>> it = sugarItemMap.entrySet().iterator();
		while (it.hasNext()) // for each item type
		{

			Entry<String, List<AssociateData>> entry = it.next();

			String type = entry.getKey().substring(1);
			List<AssociateData> list = entry.getValue();

			if (!type.equalsIgnoreCase(SugarType.LEADS.getParentType())) {
				for (int i = 0; i < list.size(); i++) // for each association within an Item
				{
					String id = getRelationshipId(type, list.get(i)); // sugar id
					if (isToCreateRelationship(id, createdRelationshipIdList)) {
						sbRelationship.append(isFirst ? ConstantStrings.EMPTY_STRING : ConstantStrings.COMMA);

						sbRelationship.append(buildDocumentRelationship(getRelationshipType(type), id));
						createdRelationshipIdList.add(id);
						if (isFirst) {
							isFirst = false;
						}
					}
				}
			}
		}

		if (sbRelationship != null && !sbRelationship.toString().equals(ConstantStrings.EMPTY_STRING)) {
			InputStream is = null;

			Iterator<Entry<File, DocumentUploadObject>> itdoc = documentsToBeUploaded.entrySet().iterator();
			while (itdoc.hasNext()) {
				Entry<File, DocumentUploadObject> entry = itdoc.next();
				File file = entry.getKey();
				DocumentUploadObject udo = entry.getValue();

				try {

					is = getFileInputStream(udo.getFileName(), file);

					ByteArrayOutputStream bkupOutputStream = bkupInputStream(is);
					updateDocumentLanguage(udo, new ByteArrayInputStream(bkupOutputStream.toByteArray()));
					bis = new ByteArrayInputStream(bkupOutputStream.toByteArray());

					String requestPart1 = buildUploadRequestHeader(udo);
					UiUtils.log("Upload document web service 1 of 2:" + maskpswd(requestPart1)); //$NON-NLS-1$

					String sugar_id = (udo.getUploadedDocumentSugarId() == null) ? "" : ",\"document_id\":\"" + udo.getUploadedDocumentSugarId(); //$NON-NLS-1$  //$NON-NLS-2$
					posterString = "method=ibm_upload_file_multiple&input_type=JSON&response_type=JSON&rest_data={\"session\":\"<SESSION>\",\"file\":{\"filename\":\"" //$NON-NLS-1$
							+ udo.getConnectionsFileName() + "\",\"file\":\"Zm9v\",\"language\":\"" + udo.getLanguage() + sugar_id; //$NON-NLS-1$

					isContinue = callUploadDocumentServices(udo, requestPart1, sbRelationship.toString(), bis);

				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
				} finally {
					try {
						if (is != null) {
							is.close();
						}
						if (bis != null) {
							bis.close();
						}
					} catch (Exception e) {
						UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
					}
				}
			} // ///
		}

		else {
			isContinue = false;
		}
		return isContinue;
	}
	private String maskpswd(String s) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (s != null) {
			String password = "&password="; //$NON-NLS-1$
			String[] ss = s.split(password);
			if (ss.length > 1) {
				sb.append(ss[0]).append(password).append("######").append(ss[1].substring(ss[1].indexOf(ConstantStrings.AMPERSAND))); //$NON-NLS-1$
			}
		}
		return sb.toString();
	}

	private ByteArrayOutputStream bkupInputStream(InputStream is) {

		int len;
		int size = 1024;
		byte[] buf;

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		buf = new byte[2014];
		try {
			while ((len = is.read(buf, 0, size)) != -1) {
				bos.write(buf, 0, len);
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
		}
		return bos;
	}

	private boolean isToCreateRelationship(String id, List<String> list) {
		boolean isToCreate = true;
		if (id != null && list != null && !list.isEmpty()) {
			if (list.contains(id)) {
				isToCreate = false;
			}
		}
		return isToCreate;
	}

	private String getRelationshipType(String type) {
		String attachmentType = type;
		if (type != null) {
			if (type.equalsIgnoreCase(SugarType.CONTACTS.getParentType())) {
				attachmentType = SugarType.ACCOUNTS.getParentType();
			}
		}
		return attachmentType;
	}

	private String getRelationshipId(String type, AssociateData associateData) {
		String attachmentId = null;
		if (type != null && associateData != null) {
			attachmentId = associateData.getId();

			// If Contacts, need to use its client's id. This information is saved in AssociateData's extended field.
			if (type.equalsIgnoreCase(SugarType.CONTACTS.getParentType())) {
				String extended = associateData.getExtended();
				if (extended != null) {
					String[] keypairs = extended.split(ConstantStrings.COMMA);
					for (int i = 0; i < keypairs.length; i++) {
						String[] pairs = keypairs[i].split(ConstantStrings.EQUALS);
						if (pairs[0] != null && pairs[0].equalsIgnoreCase(ConstantStrings.ACCOUNT_ID)) {
							attachmentId = pairs[1];
							break;
						}
					}
				}
			}
		}
		return attachmentId;
	}

	private String buildUploadRequestHeader(DocumentUploadObject udo) {

		String header = null;
		if (udo != null && udo.getFileName() != null) {
			StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
			sb.append(ConstantStrings.USERID).append(ConstantStrings.EQUALS).append(NotesAccountManager.getInstance().getCRMUser()).append(ConstantStrings.AMPERSAND).append(ConstantStrings.PASSWORD)
					.append(ConstantStrings.EQUALS).append(NotesAccountManager.getInstance().getCRMPassword()).append(ConstantStrings.AMPERSAND).append("sessionid") //$NON-NLS-1$
					.append(ConstantStrings.EQUALS).append(SugarWebservicesOperations.getInstance().getSessionId(false)).append(ConstantStrings.AMPERSAND);

			for (int i = 0; i < FU_STRINGS_1.length; i++) {
				sb.append(FU_STRINGS_1[i]);
			}

			// ... file name with suffix to make it unique ...
			// 34545 - convert file name to unicode in case it has dbcs
			sb.append("\"").append(StringEscapeUtils.escapeJava(udo.getConnectionsFileName()/* createUniqueFileName(filename) */)).append("\"").append(ConstantStrings.COMMA); //$NON-NLS-1$ //$NON-NLS-2$

			for (int i = 0; i < FU_STRINGS_2.length; i++) {
				sb.append(FU_STRINGS_2[i]);
			}

			// ... language
			sb.append(udo.getLanguage()).append("\"").append(ConstantStrings.COMMA); //$NON-NLS-1$ //$NON-NLS-2$

			if (udo.getUploadedDocumentSugarId() != null) {
				// document_id
				for (int i = 0; i < FU_STRINGS_2a.length; i++) {
					sb.append(FU_STRINGS_2a[i]);
				}

				// ... document_id
				sb.append(udo.getUploadedDocumentSugarId()).append("\"").append(ConstantStrings.COMMA); //$NON-NLS-1$ //$NON-NLS-2$
			}

			for (int i = 0; i < FU_STRINGS_3.length; i++) {
				sb.append(FU_STRINGS_3[i]);
			}

			header = sb.toString();
		}

		return header;
	}

	private String buildDocumentRelationship(String type, String id) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (type != null && id != null) {

			for (int i = 0; i < FU_STRINGS_4a.length; i++) {
				sb.append(FU_STRINGS_4a[i]);
			}
			sb.append(type);
			for (int i = 0; i < FU_STRINGS_4b.length; i++) {
				sb.append(FU_STRINGS_4b[i]);
			}
			sb.append(id);
			for (int i = 0; i < FU_STRINGS_4c.length; i++) {
				sb.append(FU_STRINGS_4c[i]);
			}
		}
		return sb.toString();
	}

	private String createUniqueFileName(String filename) {
		String string = null;
		if (filename != null) {
			StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
			String fileSuffix = UiUtils.javaDateToString(new Date(), "yyyyMMdd-HHmm"); //$NON-NLS-1$
			int index = filename.lastIndexOf("."); //$NON-NLS-1$
			sb.append(filename.substring(0, index)).append(ConstantStrings.DASH).append(fileSuffix).append(filename.substring(index));
			string = sb.toString();
		}

		return string;
	}

	/*
	 * Construct the web service call with header, file inputstream, and relationship statements. Then execute the web service.
	 */
	public boolean callUploadDocumentServices(final DocumentUploadObject udo, final String header, final String relationship, final InputStream fileInputStream) {
		success = true;

		try {
			// if nothing in fileinputstream, exit now
			if (fileInputStream == null || fileInputStream.available() == 0) {
				updateUploadObjectWithResult(udo, null, null, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_FILE_UPLOAD_WARNING_MSG));
				getFailedResultsMap().put(udo.getFileName(), udo);
				success = false;
				return success;
			}

			String tempDir = GenericUtils.getUniqueTempDir();

			final File tempDirectory = new File(tempDir);
			tempDirectory.mkdirs();
			final String tempFileName = tempDir + "/tempUpload.file"; //$NON-NLS-1$

			final URLEncodeFilterOutputStream tempFileOutputStream = new URLEncodeFilterOutputStream(new FileOutputStream(tempFileName));

			final PipedOutputStream rawOut = new PipedOutputStream();
			final PipedInputStream base64In = new PipedInputStream();
			rawOut.connect(base64In);

			Thread rawOutWriter = new Thread() {
				@Override
				public void run() {
					try {
						byte[] bytes = new byte[4096];
						int bytesRead = 0;
						while ((bytesRead = fileInputStream.read(bytes)) != -1) {
							rawOut.write(bytes, 0, bytesRead);
							rawOut.flush();
						}
						rawOut.close();
					} catch (Exception e) {
						UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
					}
				}
			};
			rawOutWriter.start();

			Thread base64InReader = new Thread() {
				@Override
				public void run() {
					try {
						byte[] bytes = new byte[4096];
						int bytesRead = 0;

						Base64OutputStream base64outStream = new Base64OutputStream(tempFileOutputStream, true, -1, new byte[]{});

						// Don't URL encode the header
						tempFileOutputStream.setApplyFilter(false);

						String[] headerStrings = header.split("arguments[=]"); //$NON-NLS-1$
						String headerString1 = headerStrings[0] + "arguments="; //$NON-NLS-1$ 

						tempFileOutputStream.write(headerString1.getBytes(), 0, headerString1.getBytes().length);
						tempFileOutputStream.setApplyFilter(true);
						tempFileOutputStream.write(headerStrings[1].getBytes(), 0, headerStrings[1].getBytes().length);

						while ((bytesRead = base64In.read(bytes)) != -1) {
							base64outStream.write(bytes, 0, bytesRead);
							base64outStream.flush();
						}

						// Flush out remaining bytes - defect 23241, 23297.
						base64outStream.close();
						tempFileOutputStream.close();

						// Re-open file to append more inf.
						URLEncodeFilterOutputStream appendFileOutputStream = new URLEncodeFilterOutputStream(new FileOutputStream(tempFileName, true));
						base64outStream = new Base64OutputStream(appendFileOutputStream, true, -1, new byte[]{});
						appendFileOutputStream.setApplyFilter(true);

						// Finish out the JSON
						StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
						for (int i = 0; i < FU_STRINGS_4.length; i++) {
							sb.append(FU_STRINGS_4[i]);
						}

						sb.append(relationship);

						for (int i = 0; i < FU_STRINGS_5.length; i++) {
							sb.append(FU_STRINGS_5[i]);
						}
						UiUtils.log("Upload file web service 2 of 2:" + sb.toString()); //$NON-NLS-1$

						byte[] closingBracket = sb.toString().getBytes();

						posterString = posterString + sb.toString();

						appendFileOutputStream.write(closingBracket);
						base64outStream.flush();
						base64outStream.close();
						appendFileOutputStream.close();

						base64In.close();

					} catch (Exception e) {
						UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
					}
				}
			};
			base64InReader.start();
			base64InReader.join();

			// Stream the file to the server
			final HttpClient client = SugarWebservicesOperations.getInstance().getProxiedHttpClient();
			// final PostMethod post = new PostMethod(NotesAccountManager.getInstance().getCRMServiceURL());
			final PostMethod post = new PostMethod(NotesAccountManager.getInstance().getCRMServer() + "custom/scrmsugarws/v2_1/sfaRest.php"); //$NON-NLS-1$

			post.setRequestEntity(new FileRequestEntity(new File(tempFileName), "application/x-www-form-urlencoded")); //$NON-NLS-1$
			// UiUtils.log("About to execute uploadFile web service... url=" + post.getURI().toString() + ", content type:" //$NON-NLS-1$  //$NON-NLS-2$
			// + post.getRequestEntity().getContentType());

			final String scriptX = "sfaRest.php"; //$NON-NLS-1$
			final String methodX = "ibm_upload_file_multiple"; //$NON-NLS-1$
			SugarWebservicesOperations.doBeginningLog(scriptX, methodX);

			Job job = new Job("upload file") //$NON-NLS-1$
			{
				@Override
				protected IStatus run(IProgressMonitor arg0) {
					// boolean success = true;
					try {
						client.executeMethod(post);
						String output = post.getResponseBodyAsString();
						UiUtils.log(WebServiceLogUtil.getDebugMsg(post, output, post.getURI().toString(), 0, 0));
						UiUtils.webServicesLog("uploadFile - " + udo.getFileName(), null, output); //$NON-NLS-1$

						UiUtils.posterLog("uploadFile", post.getURI().toString(), CONTENT_TYPE, posterString); //$NON-NLS-1$ 

						SugarWebservicesOperations.doEndingLog(scriptX, methodX);

						JSONObject jsonObject = new JSONObject(output);
						if (isSuccess(jsonObject)) {
							String uploadFileDocumentId = jsonObject.getString("document_id"); //$NON-NLS-1$
							String uploadFileName = jsonObject.getString("filename"); //$NON-NLS-1$

							// save attachment sugar id in the map so we can refer to it in Sugar Email/Meeting.
							if (uploadFileDocumentId != null && uploadFileName != null) {
								updateUploadObjectWithResult(udo, uploadFileName, uploadFileDocumentId, null);
								getSuccessfulResultsMap().put(udo.getFileName(), udo);
							}
						} else {
							// update udo and save it in the FailedResultsMap
							updateUploadObjectWithResult(udo, null, null, getErrorMsg(jsonObject));
							getFailedResultsMap().put(udo.getConnectionsFileName(), udo);
							success = false;
						}
					} catch (Exception e) {
						// If exception, save UI_ASSOCIATE_FILE_UPLOAD_WARNING_MSG in udo.
						updateUploadObjectWithResult(udo, null, null, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_FILE_UPLOAD_WARNING_MSG));
						getFailedResultsMap().put(udo.getFileName(), udo);
						success = false;
						UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
					}

					// Remove the temp file after upload is done
					if (tempDirectory != null) {
						File[] files = tempDirectory.listFiles();
						for (File file : files) {
							file.delete();
						}
						tempDirectory.delete();
					}

					return Status.OK_STATUS;
				}
			};

			job.schedule();
			job.join();
		} catch (Exception e) {
			success = false;
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}

		return success;
	}

	private boolean isSuccess(JSONObject jsonObject) {
		boolean isOK = false;
		if (jsonObject != null) {
			try {
				String successX = jsonObject.getString("error_msg"); //$NON-NLS-1$
				if (successX != null && (successX.toLowerCase().equalsIgnoreCase("success") || successX.equals(ConstantStrings.EMPTY_STRING))) //$NON-NLS-1$
				{
					isOK = true;
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
		return isOK;
	}

	// method concatinates error_code and error_msg
	private String getErrorMsg(JSONObject jsonObject) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		String errorMsg = null;
		int errorCode = 0;
		if (jsonObject != null) {
			try {
				errorCode = jsonObject.getInt("error_code"); //$NON-NLS-1$
				errorMsg = jsonObject.getString("error_msg"); //$NON-NLS-1$
				if (errorMsg != null) {
					sb.append(errorMsg).append(ConstantStrings.LEFT_PARENTHESIS).append(errorCode).append(ConstantStrings.RIGHT_PARENTHESIS);
				}

			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
		return sb.toString();
	}

	public String getFailedFileNames() {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (getFailedResultsMap() != null && !getFailedResultsMap().isEmpty()) {
			Iterator<String> names = getFailedResultsMap().keySet().iterator();
			boolean isFirst = true;
			while (names.hasNext()) {
				String name = names.next();
				if (isFirst) {
					isFirst = false;
				} else {
					sb.append(ConstantStrings.COMMA).append(ConstantStrings.SPACE);
				}
				sb.append(name);
			}
		}
		return sb.toString();
	}

	private void updateUploadObjectWithResult(DocumentUploadObject duo, String fileNameFromResponse, String sugarIdFromResponse, String responseMsg) {
		if (duo != null) {
			duo.setUploadedDocumentName(fileNameFromResponse);
			duo.setUploadedDocumentSugarId(sugarIdFromResponse);
			duo.setUploadMsg(responseMsg);
		}
	}

	public Map<File, DocumentUploadObject> buildUploadDocumentList(final Collection<File> documentsBeforeValidation) {
		final Collection<File> fileList = documentsBeforeValidation;
		final List<File> toRemove = new ArrayList<File>();
		// Yes, this is kinda clumsy, but we need to be able to keep track of the original file and the new file name to present to the user
		// when doing conflict resolution.
		final Map<File, DocumentInfo> needsResolution = new HashMap<File, DocumentInfo>();
		final Map<File, DocumentInfo> newFileNameMap = new HashMap<File, DocumentInfo>(); // Contains a map of the original files to the new file names

		final HashMap<String, DocumentInfo> myFiles = AbderaConnectionsFileOperations.getMyFiles();

		// 41823 - if getMyFiles() returned error, it will save the error msg in the myFiles map, display it and return null.
		// It is the calling method's responsibility to stop the rest of the process.
		if (AbderaConnectionsFileOperations.isErrorInMyFiles(myFiles)) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
					sb.append(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOUCMENTS_UPLOAD_ERROR_MSG_1)).append(
							UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_UPLOAD_ERROR_STATUS,
									new String[]{myFiles.get(AbderaConnectionsFileOperations.CONNECTIONS_ERROR).getDocumentName()}));

					MessageDialog msgdialog = new MessageDialog(Display.getDefault().getActiveShell(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_UPLOAD_ERROR_TITLE),
							SFAImageManager.getImage(SFAImageManager.SALES_CONNECT), sb.toString(), MessageDialog.WARNING, new String[]{IDialogConstants.OK_LABEL}, 0);

					msgdialog.open();
				}
			});
			return null;
		}

		if (myFiles != null) {
			final List<String> namesTaken = new ArrayList<String>();
			Iterator<String> it = myFiles.keySet().iterator();
			while (it.hasNext()) {
				namesTaken.add(it.next());
			}
			for (final File file : fileList) {

				if (myFiles.containsKey(file.getName())) {
					// if (newFileNameMap.get(file) != null && myFiles.containsKey(newFileNameMap.get(file))) {
					// needsResolution.put(file, newFileNameMap.get(file));
					// } else if (newFileNameMap.containsKey(file) && newFileNameMap.get(file) != null) {
					// // do nothing
					// } else {
					// needsResolution.clear();
					// needsResolution.put(file, new DocumentInfo(file.getName(), myFiles.get(file.getName())));
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							boolean isConflictCancelled = false;
							FileUploadConflictResolutionDialog dialog = new FileUploadConflictResolutionDialog(Display.getDefault().getActiveShell(), file, myFiles.get(file.getName()), namesTaken);
							dialog.open();
							toRemove.add(file);

							// conflict dialog was cancelled
							if (dialog.getNewDocumentInfo() == null) {
								isConflictCancelled = true;
							} else {
								newFileNameMap.put(file, dialog.getNewDocumentInfo());
								if (namesTaken != null && !namesTaken.contains(dialog.getNewDocumentInfo().getDocumentName())) {
									namesTaken.add(dialog.getNewDocumentInfo().getDocumentName());
								}
							}
							// fileList.remove(file);

						}
					});
				}

			}
		}
		if (toRemove != null && !toRemove.isEmpty()) {
			for (int i = 0; i < toRemove.size(); i++) {
				fileList.remove(toRemove.get(i));
			}
		}

		Map<File, DocumentUploadObject> documentsAfterValidation = new HashMap<File, DocumentUploadObject>();
		if (fileList != null && newFileNameMap != null) {
			processBasicDocuments(fileList, documentsAfterValidation);
			processDocumentsWithNewName(newFileNameMap, documentsAfterValidation);
		}

		return documentsAfterValidation;

	}

	private void processBasicDocuments(Collection<File> fileList, Map<File, DocumentUploadObject> filesToBeUploaded) {
		if (fileList != null && !fileList.isEmpty()) {
			for (File file : fileList) {
				filesToBeUploaded.put(file, new DocumentUploadObject(file.getName(), file.getName(), null, null));
			}
		}
	}

	private void processDocumentsWithNewName(Map<File, DocumentInfo> newFileNameMap, Map<File, DocumentUploadObject> filesToBeUploaded) {
		if (newFileNameMap != null && !newFileNameMap.isEmpty()) {
			for (File file : newFileNameMap.keySet()) {
				if (newFileNameMap.get(file) != null) {
					File newFile = new File(file.getAbsolutePath(), ((DocumentInfo) newFileNameMap.get(file)).getDocumentName());
					filesToBeUploaded.put(file, new DocumentUploadObject(file.getName(), newFile.getName(), null, ((DocumentInfo) newFileNameMap.get(file)).getSugarDocumentID()));
				}
			}
		}
	}

	private void updateDocumentLanguage(DocumentUploadObject duo, InputStream is) {
		try {
			if (LanguageDetectionServices.getInstance() != null) {

				String language = LanguageDetectionServices.getInstance().getLanguage(duo.getFileName(), is);
				duo.setLanguage(language);

			}

		} catch (java.util.NoSuchElementException e) {

		}
	}

	public Map<String, DocumentUploadObject> getSuccessfulResultsMap() {
		if (_okResultsMap == null) {
			_okResultsMap = new TreeMap<String, DocumentUploadObject>();

		}
		return _okResultsMap;
	}

	public Map<String, DocumentUploadObject> getFailedResultsMap() {
		if (_badResultsMap == null) {
			_badResultsMap = new TreeMap<String, DocumentUploadObject>();

		}
		return _badResultsMap;
	}

	public void setDatabaseData(Object o) {
	}

	public String getUploadErrorMsg() {
		String errorMsg = ConstantStrings.EMPTY_STRING;
		// get all the upload error messages
		List<String> errorMsgList = getUploadErrorMsgs();

		if (errorMsgList == null || errorMsgList.isEmpty()) {
		} else {
			StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
			for (int i = 0; i < errorMsgList.size(); i++) {
				String s = errorMsgList.get(i);
				if (s == null || s.equals(ConstantStrings.EMPTY_STRING) || s.equals(ConstantStrings.SPACE)) {
				} else {
					if (i > 0) {
						sb.append("\n"); //$NON-NLS-1$
					}
					sb.append(errorMsgList.get(i));
				}
			}
			errorMsg = sb.toString();
		}

		return errorMsg;
	}

	private List getUploadErrorMsgs() {
		List<String> errorMsgList = new ArrayList<String>();
		if (isSomeFileUploadFailed()) {
			Iterator<DocumentUploadObject> it = getFailedResultsMap().values().iterator();
			while (it.hasNext()) {
				DocumentUploadObject du = it.next();
				if (du.getUploadMsg() != null && !errorMsgList.contains(du.getUploadMsg())) {
					errorMsgList.add(du.getUploadMsg());
				}

			}

		}
		return errorMsgList;
	}

	public boolean isSomeFileUploadFailed() {
		boolean isFailed = false;
		if (getFailedResultsMap() != null && !getFailedResultsMap().isEmpty()) {
			isFailed = true;
		}
		return isFailed;
	}

	public boolean isAllFileUploadFailed() {
		boolean isFailed = false;
		if ((getFailedResultsMap() != null && !getFailedResultsMap().isEmpty()) && (getSuccessfulResultsMap() == null || getSuccessfulResultsMap().isEmpty())) {
			isFailed = true;
		}
		return isFailed;
	}

	abstract public InputStream getFileInputStream(String filename, File filefile);

}
