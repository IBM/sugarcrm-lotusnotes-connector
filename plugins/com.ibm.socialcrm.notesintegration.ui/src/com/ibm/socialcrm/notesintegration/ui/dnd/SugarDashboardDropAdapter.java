package com.ibm.socialcrm.notesintegration.ui.dnd;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.utils.UpdateSelectionsBroadcaster;
import com.ibm.socialcrm.notesintegration.ui.actions.MailDocumentSelectionAction;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class SugarDashboardDropAdapter {
	private static String DND_INTERNET_SHORTCUT_DOCUMENT_NAME_EXTENSION = "url"; //$NON-NLS-1$
	private static String DND_INBOX_DOCUMENT_NAME_EXTENSION = "eml"; //$NON-NLS-1$

	BaseSugarEntry _anEntry = null;
	private DropTarget _dt = null;
	private TextTransfer _textTransfer = null;
	private FileTransfer _fileTransfer = null;

	public SugarDashboardDropAdapter(Composite composite, BaseSugarEntry anEntry) {
		_anEntry = anEntry;
		_dt = new DropTarget(composite, DND.DROP_COPY | DND.DROP_MOVE);
		_textTransfer = TextTransfer.getInstance();
		_fileTransfer = FileTransfer.getInstance();
		// _dt.setTransfer(new Transfer[] { _textTransfer });
		_dt.setTransfer(new Transfer[]{_textTransfer, _fileTransfer});
		addDropListener();
	}

	private void addDropListener() {
		_dt.addDropListener(new DropTargetListener() {

			@Override
			public void dragEnter(DropTargetEvent arg0) {
			}

			@Override
			public void dragLeave(DropTargetEvent arg0) {
			}

			@Override
			public void dragOperationChanged(DropTargetEvent arg0) {
			}

			@Override
			public void dragOver(DropTargetEvent arg0) {
			}

			@Override
			public void drop(DropTargetEvent arg0) {

				if (arg0 != null && _fileTransfer.isSupportedType(arg0.currentDataType)) {
					if (arg0 != null && arg0.data != null && arg0.data instanceof String[]) {
						String[] files = ((String[]) arg0.data);

						// An internet shortcut file is created when D&D an opened Notes msg (via this opened document's tab), or D&D
						// a Notes calendar (either from the calendar view or from opened calendar's tab). In this internet shortcut file,
						// there should be a line with "URL=" prefix points to the Notes document's url.
						if (isNotesInternetShortcutDocument(files)) {
							toExecuteNotesInternetShortcutDocument(files);
						}
						// If D&D a Notes msg from the inbox view, a temp. file was created in the xxx/com.ibm.rcp.csiviews directory. We will
						// read in this temp file, extracting necessary inf., comparing with inf. in the mailDocumentSelectionAction's SubjectCache map
						// to retrieve this Notes msg's url.
						else if (isNotesInboxDocument(files)) {
							toExecuteNotesInboxDocument(buildDropObject(_anEntry == null ? null : _anEntry.getId(), files));
						} else {
							toExecuteOSDocuments(buildDropObject(_anEntry == null ? null : _anEntry.getId(), files));
						}

					}

				}

			}

			// Insert sugar entry id to be the first element in the string array
			private String[] buildDropObject(String firstElement, String[] origArray) {
				String[] newArray = origArray;
				if (firstElement != null && origArray != null && origArray.length > 0) {
					List<String> list = new ArrayList<String>();
					list.add(firstElement);
					list.addAll(Arrays.asList(origArray));
					newArray = list.toArray(new String[list.size()]);
				}

				return newArray;
			}

			@Override
			public void dropAccept(DropTargetEvent arg0) {
			}

		});
	}

	private static void setupAssociate(SugarDashboardDndEntry entry) {
		UpdateSelectionsBroadcaster.getInstance().updateConnector(entry);

	}

	private static void setupUpload(String[] documents) {
		UpdateSelectionsBroadcaster.getInstance().updateDashboard(documents);

	}

	private boolean isNotesInboxDocument(String[] files) {
		boolean isTrue = false;
		if (files != null && files.length > 0 && (files[0].indexOf("com.ibm.rcp.csiviews") > -1) //$NON-NLS-1$
				&& (files[0].trim().toLowerCase().endsWith(DND_INBOX_DOCUMENT_NAME_EXTENSION))) {
			isTrue = true;
		}
		return isTrue;
	}

	private boolean isNotesInternetShortcutDocument(String[] files) {
		boolean isTrue = false;
		if (files != null && files.length > 0 && (files[0].indexOf("com.ibm.rcp.ui") > -1 || files[0].indexOf("com.ibm.rcp.csiviews") > -1) //$NON-NLS-1$ //$NON-NLS-2$
				&& (files[0].trim().toLowerCase().endsWith(DND_INTERNET_SHORTCUT_DOCUMENT_NAME_EXTENSION))) {
			isTrue = true;
		}

		return isTrue;
	}

	private void toExecuteNotesInternetShortcutDocument(String[] documents) {
		if (documents != null && documents.length > 0) {
			// Extract "URL=" line
			String documentName = documents[0];
			List<String> searchList = new ArrayList<String>();
			String urlEqualString = "URL="; //$NON-NLS-1$
			searchList.add(urlEqualString);
			List<String> resultList = readDocumentExtractLines(documentName, searchList);
			if (resultList != null && resultList.size() > 0) {
				// //extract url string from the line
				// String notesDocumentUrl = ((String)resultList.get(0)).substring(((String)resultList.get(0))
				// .indexOf(urlEqualString)
				// + urlEqualString.length() - 1);
				//        

				// docid is the last part of the url
				String docid = resultList.get(0).trim().substring(resultList.get(0).trim().lastIndexOf(ConstantStrings.FORWARD_SLASH) + 1);

				toExecuteNotesDocument(docid);
			} else {
				System.out.println("BAD D&D, can not find document url ... check content of document " + documentName);
			}
		}

	}

	private void toExecuteNotesInboxDocument(String[] documents) {

		if (documents != null && documents.length > 0) {
			// 41374 - smoehow in SC 1.3, "$Orig" has correct value, not "$TUA".
			// X-Notes-Item: 023B838F:1AC2166A-5E2262E2:45E45779;
			// type=4; name=$TUA
//			String documentName = documents[0];
//			String search = "$Orig"; //"$TUA"; //$NON-NLS-1$
//			String result = readDocumentExtractPrevLine(documentName, search);
//			String xitem = "x-notes-item:";
//			if (result.trim().toLowerCase().startsWith(xitem)) {
//				result = result.trim().substring(xitem.length());
//				String docid = result.trim().replaceAll(ConstantStrings.COLON, ConstantStrings.EMPTY_STRING).replaceAll(ConstantStrings.DASH, ConstantStrings.EMPTY_STRING).replaceAll(
//						ConstantStrings.SEMICOLON, ConstantStrings.EMPTY_STRING);
//				toExecuteNotesDocument(docid);
//			} else {
//				System.out.println("BAD D&D, can not find document url ... check content of document " + documentName);
//			}
//		}
//
//	}
			
			String documentName = extractCorrectFileName(documents);
			String[] search = {"$Orig", "$TUA"};
			boolean isFound = false;
			for (int i = 0; i < search.length; i++) {
				String docid = searchDocumentId(documentName, search[i]);
				if (docid != null) {
					isFound = true;
					toExecuteNotesDocument(docid);
				}
			}
			if (!isFound) {
				toExecuteNotesDocument(null);
				System.out.println("BAD D&D, can not find document url ... check content of document " + documentName);
			}

		}

	}

	private String searchDocumentId(String documentName, String search) {
		String docid = null;
		if (documentName != null && search != null) {
			String result = readDocumentExtractPrevLine(documentName, search);
			String xitem = "x-notes-item:";
			if (result.trim().toLowerCase().startsWith(xitem)) {
				result = result.trim().substring(xitem.length());
				docid = result.trim().replaceAll(ConstantStrings.COLON, ConstantStrings.EMPTY_STRING).replaceAll(ConstantStrings.DASH, ConstantStrings.EMPTY_STRING).replaceAll(
						ConstantStrings.SEMICOLON, ConstantStrings.EMPTY_STRING);
				if (docid != null && MailDocumentSelectionAction.getSugarDataCache() != null && MailDocumentSelectionAction.getSugarDataCache().containsKey(docid)) {
				} else {
					docid = null;
				}
			}
		}
		return docid;
	}

	

	private List<String> readDocumentExtractLines(String documentName, List<String> searchStrings) {
		List<String> returnStrings = new ArrayList<String>();

		if (documentName != null && searchStrings != null && searchStrings.size() > 0) {

			String sCurrentLine = ConstantStrings.EMPTY_STRING;
			BufferedReader br = null;

			try {
				br = new BufferedReader(new FileReader(documentName));
				while ((sCurrentLine = br.readLine()) != null || (searchStrings.size() != 0)) {
					for (int j = 0; j < searchStrings.size(); j++) {
						if (sCurrentLine.startsWith(((String) searchStrings.get(j)).trim())) {
							returnStrings.add(sCurrentLine);
							searchStrings.remove(j);
							break;

						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (br != null)
						br.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
		return returnStrings;
	}

	private String readDocumentExtractPrevLine(String documentName, String searchString) {
		String returnString = ConstantStrings.EMPTY_STRING;
		String prevString = ConstantStrings.EMPTY_STRING;

		if (documentName != null && searchString != null) {

			String sCurrentLine = ConstantStrings.EMPTY_STRING;
			BufferedReader br = null;

			try {
				br = new BufferedReader(new FileReader(documentName));
				while ((sCurrentLine = br.readLine()) != null) {
					if (sCurrentLine.trim().indexOf(searchString) > -1) {
						returnString = prevString;
						break;
					}
					prevString = sCurrentLine;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (br != null)
						br.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
		return returnString;
	}

	private void toExecuteOSDocuments(String[] files) {
		setupUpload(files);
	}

	private void toExecuteNotesDocument(String docid) {

		if (docid == null) {

			MessageDialog msgdialog = new MessageDialog(Display.getCurrent().getActiveShell(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_ERROR_TITLE), SFAImageManager
					.getImage(SFAImageManager.SALES_CONNECT), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_ERROR_MSG_DDMULTI), MessageDialog.ERROR,
					new String[]{IDialogConstants.OK_LABEL}, 0);
			msgdialog.open();
			return;
		}

		MailDocumentSelectionAction.getCurrentSugarDataMap();
		if (MailDocumentSelectionAction.getSugarDataCache() != null && MailDocumentSelectionAction.getSugarDataCache().containsKey(docid)) {
			// Map<SugarType, Set<SugarEntrySurrogate>> sugarDataMap=MailDocumentSelectionAction.getSugarDataCache().get(docid);
			setupAssociate(new SugarDashboardDndEntry(_anEntry, docid));
		} else {
			System.out.println("no map entry");
		}
	}
	
	private String extractCorrectFileName(String[] files) {
		String outFile = files == null? null : files[0];
		if (files != null && files.length > 0 && Platform.getLocation() != null) {
			String workspacePathX = Platform.getLocation().toFile().getAbsolutePath().toLowerCase();
			System.out.println("path=" + workspacePathX);
			if (workspacePathX != null) {
				if (workspacePathX.indexOf(ConstantStrings.FORWARD_SLASH) > -1) {
					workspacePathX = workspacePathX.replaceAll(ConstantStrings.FORWARD_SLASH, ConstantStrings.EMPTY_STRING);
				}
				if (workspacePathX.indexOf("\\") > -1) {
					workspacePathX=workspacePathX.replaceAll("\\\\", ConstantStrings.EMPTY_STRING);
				}
			}

			boolean isFound = false;
			for (int i = 0; i < files.length; i++) {
				String in = files[i].toLowerCase();
				if (in.indexOf(ConstantStrings.FORWARD_SLASH) > -1) {
					in = in.replaceAll(ConstantStrings.FORWARD_SLASH, ConstantStrings.EMPTY_STRING);
				}
				if (in.indexOf("\\") > -1) {
					in=in.replaceAll("\\\\", ConstantStrings.EMPTY_STRING);
				}

				if (in.startsWith(workspacePathX)) {
					outFile = files[i];
					System.out.println("outfiles]=" + outFile);
					isFound = true;
					break;
				}

			}
		}
		return outFile;
	}

}
