package com.ibm.socialcrm.notesintegration.connector;

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
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.Item;
import lotus.domino.Name;
import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.NotesThread;
import lotus.domino.Session;
import lotus.domino.View;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONObject;
import org.apache.commons.json.JSONString;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.UIJob;

import com.ibm.notes.java.api.data.NotesDatabaseData;
import com.ibm.notes.java.api.data.NotesDocumentData;
import com.ibm.notes.java.ui.NotesUIWorkspace;
import com.ibm.notes.java.ui.documents.NotesUIDocument;
import com.ibm.socialcrm.notesintegration.connector.util.ConnectorUtil;
import com.ibm.socialcrm.notesintegration.connector.util.NotesDocumentUploadOperations;
import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.SugarEntrySurrogate;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.core.utils.UpdateSelectionsBroadcaster;
import com.ibm.socialcrm.notesintegration.files.utils.DocumentUploadObject;
import com.ibm.socialcrm.notesintegration.ui.actions.MailDocumentSelectionAction;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateData;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateDataMap;
import com.ibm.socialcrm.notesintegration.ui.custom.SFAMessageDialogWithHyperlink;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

/**
 * This class is the manager of updating Sugar and Notes with the connector/association information.
 */
public class AssociateUpdateManager {
	private static String EMAILS = "Emails";//$NON-NLS-1$
	private static String MEETINGS = "Meetings"; //$NON-NLS-1$

	private static String NEW_DOCUMENT = "n"; //$NON-NLS-1$
	private static String EXISTING_DOCUMENT = "e"; //$NON-NLS-1$

	private static int FILE_UPLOAD_ERROR = 1;
	private static int SUGAR_ERROR = 2;
	private static int NOTES_ERROR = 3;

	// ============= Sugar update request ===============================================

	// 28906 - remove session parameter; otherwise, ibm_set_entry returns invalid session id
	// ... Beginning of the request ...
	private static String[] REQUEST_STRINGS_1 = {"{"}; //$NON-NLS-1$ 

	private static String[] REQUEST_STRINGS_2 = {"\"run_as\":\"\",", //$NON-NLS-1$ 
			"\"return_data\":false,", "\"records\":["}; //$NON-NLS-1$ //$NON-NLS-2$

	// ... SELECT block for each association goes here ...

	private static String[] REQUEST_STRINGS_RECORD_Block_FOR_NEW_MESSAGE_0 = {","}; //$NON-NLS-1$
	private static String[] REQUEST_STRINGS_RECORD_Block_FOR_NEW_MESSAGE_1 = {"{\"id\":\"1\",", //$NON-NLS-1$
			"\"id_field\":\"generate_sugar_id\",", "\"module_name\":"}; //$NON-NLS-1$ //$NON-NLS-2$

	// ... EMAILS/MEETINGS goes here ...

	private static String[] REQUEST_STRINGS_RECORD_Block_FOR_NEW_MESSAGE_2 = {"\"action\":\"create\",", //$NON-NLS-1$ 
			"\"name_value_list\":["}; //$NON-NLS-1$

	private static String[] REQUEST_STRINGS_RECORD_Block_FOR_EXISTING_MESSAGE_1 = {",{\"id\":"}; //$NON-NLS-1$
	private static String[] REQUEST_STRINGS_RECORD_Block_FOR_EXISTING_MESSAGE_1a = {"{\"id\":"}; //$NON-NLS-1$
	// ... existing email id goes here ...

	private static String[] REQUEST_STRINGS_RECORD_Block_FOR_EXISTING_MESSAGE_2 = {"\"id_field\":\"id\",",//$NON-NLS-1$
			"\"module_name\":"};//$NON-NLS-1$

	// ... EMAILS/MEETINGS goes here ...

	private static String[] REQUEST_STRINGS_RECORD_Block_FOR_EXISTING_MESSAGE_3 = {"\"action\":\"update\",", //$NON-NLS-1$  
			"\"version\":"}; //$NON-NLS-1$

	// ... existing email version goes here ...

	private static String[] REQUEST_STRINGS_RECORD_Block_FOR_EXISTING_MESSAGE_4 = {"\"name_value_list\":["}; //$NON-NLS-1$

	// ... if new mail, each Sugar fields goes here ...

	private static String[] REQUEST_STRINGS_RECORD_Block_2 = {"],", /* close name_value_list *///$NON-NLS-1$
			"\"relationships\":["}; //$NON-NLS-1$

	// ... relationships for each association goes here ...

	private static String[] REQUEST_STRINGS_4 = {"]" /* close relationship */, "}]" /* close record */, //$NON-NLS-1$ //$NON-NLS-2$
			",\"logical_app_id\":\"\"", "}"}; //$NON-NLS-1$ //$NON-NLS-2$ 

	// ============= get version ======================================================
	// 28906 - remove session parameter; otherwise, ibm_set_entry returns invalid session id
	private static String[] GV_STRINGS_1 = {"method=ibm_set_entry&", "arguments={"}; //$NON-NLS-1$ //$NON-NLS-2$ 

	private static String[] GV_STRINGS_2 = {"\"run_as\":\"\",\"return_data\":true,\"records\":[", "{\"id\":"}; //$NON-NLS-1$ //$NON-NLS-2$
	// ... sugar id goes here ...

	private static String[] GV_STRINGS_3 = {"\"id_field\":\"id\",\"module_name\":"}; //$NON-NLS-1$
	// ... module name goes here ...

	private static String[] GV_STRINGS_4 = {"\"action\":\"select\"}", "],\"logical_app_id\":\"\"}"}; //$NON-NLS-1$ //$NON-NLS-2$

	private Session _session = null;
	private Database _mailDb = null;
	private Document _mailDoc = null;
	private NotesUIDocument _doc = null;
	private String _mapkey = null;
	private AssociateDataMap _associateDataMapForSugarUpdate = null;
	// In case we need to restore AssociateDataMap
	private AssociateDataMap _bkupAssociateDataMap = null;
	private Map<File, String> _uploadDocumentMap = null;

	// key: email, value: name in Lotus name format (for example: anne wang/westford/ibm or anne wang/westford/ibm@notes)
	private Map<String, String> _sugarInvitees = null;

	private String[] _attachmentList = null;

	private String _errorMsg = null;

	private NotesDocumentUploadOperations _duOperations = null;

	private boolean _isDirtyCalendarWithoutAssociationUpdate;
	private boolean _isChairCancelMeeting;
	private boolean _isFirst;

	// for calendar update, this is the new required invitee list, name could be in Notes format (for example: anne wang/westford/ibm
	// or anne wang/westford/ibm@notes), or internet format (for example: anne_wang@us.ibm.com)
	private List<String> _requiredList_new = null;

	// for calendar update, this is the new optional invitee list, name could be in Notes format (for example: anne wang/westford/ibm
	// or anne wang/westford/ibm@notes), or internet format (for example: anne_wang@us.ibm.com)
	private List<String> _optionalList_new = null;

	// for calendar update, this is the list of invitee to be removed, name could be in Notes format (for example: anne wang/westford/ibm
	// or anne wang/westford/ibm@notes), or internet format (for example: anne_wang@us.ibm.com)
	private List<String> _uninviteedInviteeList = null;

	// key is email, value is status
	private Map<String, String> _inviteeStatusMap = null;

	// for new calendar, this is the merge of required and optional invitees
	// for calendar update, this is the merge of newly added required and
	// optional invitees plus invitees to be removed
	private List<String> _notesInvitees = null;

	// for new calendar, this is the merge of contacts in the required and optional invitee list
	// for calendar update, this is the merge of contacts in the newly added required and optional invitees plus
	// the uninvited invitees
	private Map<String, AssociateData> _inviteeContactMap = null;

	// for calendar update, this is the contacts in the uninviteed invitee list
	private Map<String, AssociateData> _uninvitedContactMap = null;

	private String _myCanonicalName = null;
	private boolean _isAnyAssociateWithLead = false;

	public AssociateUpdateManager(NotesUIDocument doc, String mapkey) {
		// _doc is mainly used to give us access to the DB name, path and, by opening the db, it will lead us to the backend document.
		_doc = doc;

		_mapkey = mapkey;

		if (getAssociateDataMapXML() != null) {
			AssociateDataMap associateDataMap = ConnectorUtil.decode(getAssociateDataMapXML());

			// We need to send out Sugar update request only for those AssociateData that have not been
			// marked as associated. Put them in _associateDataMapForSugarUpdate.
			_associateDataMapForSugarUpdate = associateDataMap.getSubset(false);

			_bkupAssociateDataMap = setBackupAssociateDataMap(associateDataMap);

			// 2015-03-13 check if any association with leads
			if (_associateDataMapForSugarUpdate != null) {
				_isAnyAssociateWithLead = _associateDataMapForSugarUpdate.isAnyAssociateWithLead();
			}

		}

		// Save attachment to be uploaded in _attachmentList.
		// Note that if this is new mail/meeting, we will upload all the attachments, and we will build
		// _attachmentList list at openNotes() method for performance reason.
		_attachmentList = createAttachmentList();

		_duOperations = new NotesDocumentUploadOperations();
	}

	private AssociateDataMap setBackupAssociateDataMap(AssociateDataMap associateDataMap) {
		// Deep copy, so if the association process fails, we will restore AssociateDataMap with this copy.
		AssociateDataMap bkup = ConnectorUtil.decode(ConnectorUtil.encode(associateDataMap));
		bkup.removeUnAssociated();

		if ((associateDataMap == null && _associateDataMapForSugarUpdate == null)
				|| (associateDataMap != null && _associateDataMapForSugarUpdate != null && _associateDataMapForSugarUpdate.isTheSame(associateDataMap))) {
			bkup = null;
		}

		return bkup;
	}

	public void updateAssociate() {

		// if (_attachmentMap == null)
		// {
		// _attachmentMap = new HashMap<String, String>();
		// }
		// else
		// {
		// _attachmentMap.clear();
		// }

		if (_doc != null && _doc.isNewDoc()) {
			_doc.getBEDocument().setItemValue(MailDocumentSelectionAction.CRM_ASSOCIATE, getAssociateDataMapXML());
			_doc.activate();
			_doc.save();

		}

		Job updateAssociateJob = new Job("updateAssociateJob") //$NON-NLS-1$
		{

			protected IStatus run(IProgressMonitor monitor) {
				int errorReason = -1;

				// create session,
				boolean isContinue = openNotes();

				if (isContinue) {
					int appType = getAppType();

					// Upload the attachment if there is attachment, and if this is the first association.
					// 49884 - calendar does not support attachment upload
					// 2015-03-15 - if the association involves lead ONLY, we will skip the upload process.
					// But if the association involves lead and any other type, ie contact/oppty/account, we will go ahead call the upload process,
					// and upload process has logic to skip setting relationship with leads (see AbstractDocumentUploadOperations.doUploadDocument()).
					if (appType != ConnectorUtil.CALENDAR_APP && isAttachmentExist() && getAssignedSugarId() == null && !isAssociateWithLead()) {
						UiUtils.log("1. to uploadAttachment"); //$NON-NLS-1$

						boolean isFileUploadOK = uploadAttachment();

						if (!isFileUploadOK || _duOperations.isAllFileUploadFailed() || _duOperations.isSomeFileUploadFailed()) {
							_errorMsg = _duOperations.getUploadErrorMsg();

							if (_errorMsg == null || _errorMsg.equals(ConstantStrings.EMPTY_STRING) || _errorMsg.equals(ConstantStrings.SPACE)) {
								_errorMsg = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_FILE_UPLOAD_WARNING_MSG);
							}

							showErrorDialog(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_FILE_UPLOAD_WARNING_TITLE), _errorMsg);

							if (_duOperations.isAllFileUploadFailed()) {
								errorReason = FILE_UPLOAD_ERROR;
								isContinue = false;
							}
						}

					} else {
						UiUtils.log("1. Skip uploadAttachment logic"); //$NON-NLS-1$
					}

					if (isContinue) {
						_errorMsg = null;
						UiUtils.log("2. to updateSugar"); //$NON-NLS-1$
						isContinue = updateSugar(appType);

						if (isContinue) {
							if (isChairCancelMeeting()) {
								UiUtils.log("3. Skip updateNotes"); //$NON-NLS-1$
							} else {
								UiUtils.log("3. to uploadNotes"); //$NON-NLS-1$
								isContinue = updateNotes();
							}
							UiUtils.log("4. DONE"); //$NON-NLS-1$
							if (!isContinue) {
								errorReason = NOTES_ERROR;
							}

							// 55883
							else {
								// 55883 - update assocation information in cache ...
								// usually we will refresh the cache when document is closed...
								// but if after doing the copyto, user clicks a different tab then comes back to this document / clicks on the current document
								// again, we need to update the document cache so the associated inf. wont be ignored.

								UiUtils.log("5. Update UI Cache");
								updateDocumentCache();

								UiUtils.log("6. All Done");

							}

						} else {
							errorReason = SUGAR_ERROR;
						}
					}

				} else {
					errorReason = 0;
				}

				// 55883 - Fire property change event to turn off update flag... No particular reason why use _associateDataMapForSugarUpdate here,
				// just in case it is useful.
				UpdateSelectionsBroadcaster.getInstance().updateConnectorIsDone(_associateDataMapForSugarUpdate);

				if (errorReason == FILE_UPLOAD_ERROR || errorReason == SUGAR_ERROR || errorReason == NOTES_ERROR) {

					final int errorReason1 = errorReason;
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {

							unAssociate();

							String msg = null;

							if (errorReason1 == FILE_UPLOAD_ERROR) {
								msg = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_ERROR_MSG_FILEUPLOAD, new String[]{getAssociateItemNames()});
							} else if (errorReason1 == SUGAR_ERROR) {
								msg = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_ERROR_MSG_SUGAR,
										new String[]{getAssociateItemNames() + (_errorMsg == null ? ConstantStrings.EMPTY_STRING : _errorMsg)});
								// just try to be a good citizen
								_errorMsg = null;

							} else if (errorReason1 == NOTES_ERROR) {
								msg = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_ERROR_MSG_NOTES, new String[]{getAssociateItemNames()});
							}
							if (msg != null) {

								showErrorDialog(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_ERROR_TITLE), msg);
							}

						}
					});
				}

				closeNotes();
				return Status.OK_STATUS;
			}

		};

		updateAssociateJob.setRule(ConnectorUtil.UPDATE_ASSOCIATE_JOB_RULE);
		updateAssociateJob.schedule();

	}

	private boolean isAssociateWithLead() {
		boolean isAssociateWithLead = false;
		if (_isAnyAssociateWithLead) {
			if (_associateDataMapForSugarUpdate != null && _associateDataMapForSugarUpdate.getMyMap() != null && !_associateDataMapForSugarUpdate.getMyMap().isEmpty()) {
				if (_associateDataMapForSugarUpdate.getMyMap().size() == 1) {
					isAssociateWithLead = true;
				}
			}
		}
		return isAssociateWithLead;
	}

	private void updateDocumentCache() {

		try {
			// get document cache
			Map<SugarType, Set<SugarEntrySurrogate>> sugarDataCache = MailDocumentSelectionAction.getSugarDataCache().get(_mailDoc.getUniversalID());
			if (sugarDataCache != null && !sugarDataCache.isEmpty()) {
				Set<SugarEntrySurrogate> entries = sugarDataCache.get(MailDocumentSelectionAction.getDefaultAssociateSugarType());
				if (entries.size() > 0) {
					boolean isFound = false;
					Iterator<SugarEntrySurrogate> it = entries.iterator();
					while (it.hasNext()) {
						SugarEntrySurrogate entry = it.next();
						if (entry.getName() != null && entry.getName().equalsIgnoreCase(MailDocumentSelectionAction.CRM_ASSOCIATE)) {
							// update
							entry.setAssociateDataMapXML(getAssociateDataMapXML());
							MailDocumentSelectionAction.setCurrentSugarDataMap(sugarDataCache);
							isFound = true;
							break;
						}
					}

					// if there are entries but no CRM_ASSOCIATE entry (for example: for the document suggestion scenario),
					// add the CRM_ASSOCIATE entry
					if (!isFound) {
						entries.add(new SugarEntrySurrogate(MailDocumentSelectionAction.CRM_ASSOCIATE, MailDocumentSelectionAction.getDefaultAssociateSugarType(), getAssociateDataMapXML(),
								_attachmentList, MailDocumentSelectionAction.getInstance().getNotesInvitees(_mailDoc, MailDocumentSelectionAction.REQUIRED_INVITEES_TYPE), MailDocumentSelectionAction
										.getInstance().getNotesInvitees(_mailDoc, MailDocumentSelectionAction.OPTIONAL_INVITEES_TYPE)));

						MailDocumentSelectionAction.setCurrentSugarDataMap(sugarDataCache);
					}

				} else {
					// no association entry exists yet... add the entry
					entries.add(new SugarEntrySurrogate(MailDocumentSelectionAction.CRM_ASSOCIATE, MailDocumentSelectionAction.getDefaultAssociateSugarType(), getAssociateDataMapXML(),
							_attachmentList, MailDocumentSelectionAction.getInstance().getNotesInvitees(_mailDoc, MailDocumentSelectionAction.REQUIRED_INVITEES_TYPE), MailDocumentSelectionAction
									.getInstance().getNotesInvitees(_mailDoc, MailDocumentSelectionAction.OPTIONAL_INVITEES_TYPE)));

					MailDocumentSelectionAction.setCurrentSugarDataMap(sugarDataCache);
				}
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}
	}

	private void unAssociate() {
		NotesUIWorkspace workspace = new NotesUIWorkspace();

		if (workspace.getCurrentDocument() != null) {

			// Reversing the tool bar Associate button text from Associated back to Associate
			String currDocId = ConnectorUtil.getDocId(workspace.getCurrentDocument());
			SugarEntrySurrogate sugarEntry = null;
			// Reversing the tool bar Associate button text from Associated to Associate.
			if (currDocId != null && currDocId.equals(_mapkey) && (sugarEntry = (SugarEntrySurrogate) NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(_mapkey)) != null) {
				// Update DocumentActionMap
				SugarEntrySurrogate newSugarEntry = ConnectorUtil.updateAssociateDataInSugarEntry(sugarEntry, ConnectorUtil.encode(_bkupAssociateDataMap));
				NotesUIDocumentSaveListenerRepository.updateSugarEntry(_mapkey, newSugarEntry);

				HashSet<SugarEntrySurrogate> set = new HashSet<SugarEntrySurrogate>();
				set.add(newSugarEntry);

				Map<SugarType, Set<SugarEntrySurrogate>> sugarDataMap = new HashMap<SugarType, Set<SugarEntrySurrogate>>();
				sugarDataMap.put(sugarEntry.getType(), set);
				// Fire property change
				UpdateSelectionsBroadcaster.getInstance().updateSelectedItems(sugarDataMap);

			}
		}
	}

	private String getAssociateItemNames() {
		String string = ConstantStrings.EMPTY_STRING;
		boolean isFirst = true;
		StringBuffer sb = new StringBuffer(string);
		if (_associateDataMapForSugarUpdate != null && _associateDataMapForSugarUpdate.getMyMap() != null && !_associateDataMapForSugarUpdate.getMyMap().isEmpty()) {
			Iterator<List<AssociateData>> it = _associateDataMapForSugarUpdate.getMyMap().values().iterator();
			while (it.hasNext()) {
				List<AssociateData> list = it.next();
				Iterator<AssociateData> associateDataIt = list.iterator();
				while (associateDataIt.hasNext()) {
					AssociateData associateData = associateDataIt.next();
					if (isFirst) {
						isFirst = false;
					} else {
						sb.append(ConstantStrings.COMMA).append(ConstantStrings.SPACE);
					}
					sb.append(ConnectorUtil.getFormattedName(associateData.getName()));

				}
			}

		}
		string = sb.toString();
		return string;
	}

	private void showErrorDialog(final String title, final String msg) {

		UIJob showErrorUIJob = new UIJob("showErrorUIJob") //$NON-NLS-1$
		{
			@Override
			public IStatus runInUIThread(IProgressMonitor arg0) {
				SFAMessageDialogWithHyperlink.open(title, SFAImageManager.getImage(SFAImageManager.SALES_CONNECT), msg);
				return Status.OK_STATUS;
			}

		};

		showErrorUIJob.schedule();

	}

	private boolean isAttachmentExist() {
		boolean isExist = false;
		if (_attachmentList != null && _attachmentList.length > 0) {
			isExist = true;
		}
		return isExist;
	}

	private String[] createAttachmentList() {

		List list = null;
		if (NotesUIDocumentSaveListenerRepository.getDocumentActionMap() != null) {
			SugarEntrySurrogate sugarEntry = (SugarEntrySurrogate) NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(_mapkey);
			if (sugarEntry != null) {
				String[] attachmentNames = sugarEntry.getAttachmentNames();
				if (attachmentNames != null && attachmentNames.length > 0) {
					for (int i = 0; i < attachmentNames.length; i++) {
						if (attachmentNames[i].substring(0) != null && attachmentNames[i].substring(0, 1).equalsIgnoreCase(UiUtils.ATTACHMENT_IS_SELECTED)) {
							if (list == null) {
								list = new ArrayList<String>();
							}
							list.add(attachmentNames[i].substring(1));
						}
					}
				}
			}

		}

		String[] strings = null;
		if (list != null) {
			strings = (String[]) list.toArray(new String[list.size()]);
		}
		return strings;

	}

	private boolean uploadAttachment() {

		// Build files to be processed
		getUploadDocumentMap().clear();

		for (int i = 0; i < _attachmentList.length; i++) // for each attachment
		{
			getUploadDocumentMap().put(new File(_attachmentList[i]), _attachmentList[i]);
		}
		Set<File> filesToBeProcessed = getUploadDocumentMap().keySet();
		Map<File, DocumentUploadObject> documentsAfterValidation = _duOperations.buildUploadDocumentList(filesToBeProcessed);

		// if doucmentsAfteValidation is null, we should stop the process
		if (documentsAfterValidation == null) {
			return false;
		}

		// Really doing upload now
		_duOperations.setDatabaseData(_mailDoc);
		return _duOperations.doUploadDocument(_associateDataMapForSugarUpdate.getMyMap(), documentsAfterValidation);
	}

	private int getAppType() {
		int appType = -1;
		if (_mailDoc != null) {
			try {
				String form = _mailDoc.getItemValueString("Form"); //$NON-NLS-1$
				appType = ConnectorUtil.getAppType(form);

			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}
		} else if (isChairCancelMeeting()) {
			appType = ConnectorUtil.CALENDAR_APP;
		}

		return appType;
	}

	private boolean updateSugar(int appType) {
		boolean isOK = false;
		// Call sugar web service to update association info
		try {
			String txt = null;

			if (appType == ConnectorUtil.MAIL_APP) {
				if (getAssignedSugarId() == null) {
					txt = buildSugarRequestForNewEmail(appType);
				} else {
					txt = buildSugarRequestForExistingEmail(appType);
				}
			} else if (appType == ConnectorUtil.CALENDAR_APP) {
				if (getAssignedSugarId() == null) {
					txt = buildSugarRequestForNewEmail(appType);
				} else {
					txt = buildSugarRequestForExistingCalendar(appType);
				}
			}

			if (txt != null && !txt.equalsIgnoreCase(ConstantStrings.EMPTY_STRING)) {
				UiUtils.log("About to execute updateSugar web service..."); //$NON-NLS-1$ 
				String output = SugarWebservicesOperations.getInstance().callNativeSugarRestWithMultipart("ibm_set_entry", txt); //$NON-NLS-1$

				UiUtils.webServicesLog("updateSugar", txt, output);//$NON-NLS-1$

				// Be sure the transaction was committed
				if (!isOutputCommitted(output)) {
					UtilsPlugin.getDefault().logErrorMessage(output, Activator.PLUGIN_ID);
					_errorMsg = "\n\n" + getErrorMsg(output); //$NON-NLS-1$
					output = null;
				}

				if (output != null) {
					// This is association for existing e-mail / meeting.
					if (getAssignedSugarId() != null) {

						isOK = true;
					} else
					// This is association for new e-mail / meeting. Extracting the Sugar assigned e-mail/meeting id
					// and save it in the Notes document.
					{
						String assignedSugarId = getJSONSugarId(output, appType);
						if (assignedSugarId != null) {
							updateAssignedSugarId(assignedSugarId);
							isOK = true;
						}
					}
				}
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);

		}
		return isOK;
	}

	private String getErrorMsg(String output) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

		if (output != null) {
			JSONObject jsonObject = null;
			try {
				jsonObject = new JSONObject(output);
				JSONArray array = jsonObject.getJSONArray("record_status"); //$NON-NLS-1$

				if (array != null) {
					for (int i = 0; i < array.size(); i++) {
						JSONObject o = array.getJSONObject(i);

						if (o != null) {
							int code = o.getInt("error_code"); //$NON-NLS-1$
							String msg = o.getString("error_msg"); //$NON-NLS-1$
							sb.append("\n").append(i).append("- ").append(msg).append(ConstantStrings.LEFT_PARENTHESIS).append(code).append(ConstantStrings.RIGHT_PARENTHESIS); //$NON-NLS-1$ //$NON-NLS-2$
							if (o.has("relationships")) { //$NON-NLS-1$
								JSONArray relationshipArray = o.getJSONArray("relationships"); //$NON-NLS-1$
								if (relationshipArray != null && relationshipArray.size() > 0) {
									for (int j = 0; j < relationshipArray.size(); j++) {
										int relcode = relationshipArray.getJSONObject(j).getInt("error_code"); //$NON-NLS-1$
										String relmsg = relationshipArray.getJSONObject(j).getString("error_msg"); //$NON-NLS-1$
										sb
												.append("\n ... ").append(i).append(ConstantStrings.PERIOD).append(j).append("- ").append(relmsg).append(ConstantStrings.LEFT_PARENTHESIS).append(relcode).append(ConstantStrings.RIGHT_PARENTHESIS); //$NON-NLS-1$ //$NON-NLS-2$

									}
								}
							}

						}
					}
				}

			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}

		return sb.toString();
	}

	// Sometimes Sugar did not commit the transaction, but returned status code 200. This method
	// will try to catch this by looking for the "data_committed" key pair. If the value is false,
	// returns false.
	private boolean isOutputCommitted(String output) {
		boolean isCommitted = false;
		if (output != null) {
			JSONObject jsonObject = null;
			try {
				jsonObject = new JSONObject(output);
				if (jsonObject.containsKey("data_committed")) //$NON-NLS-1$
				{
					isCommitted = jsonObject.getBoolean("data_committed"); //$NON-NLS-1$

				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}

		return isCommitted;
	}

	private String getJSONSugarId(String output, int appType) {
		String sugarId = null;
		if (output != null) {
			JSONObject jsonObject = null;
			try {
				jsonObject = new JSONObject(output);
				JSONArray recordStatusArray = jsonObject.getJSONArray("record_status"); //$NON-NLS-1$
				if (recordStatusArray != null) {
					for (int i = 0; i < recordStatusArray.length(); i++) {
						JSONObject recordStatusObject = recordStatusArray.getJSONObject(i);

						// 41544 - For some reason, JSON response type is changed, fix the problem accordingly
						String moduleX = null;
						if (recordStatusObject.get("module_name") instanceof JSONString) { //$NON-NLS-1$
							moduleX = ((JSONString) recordStatusObject.get("module_name")).toJSONString(); //$NON-NLS-1$
						} else if (recordStatusObject.get("module_name") instanceof String) { //$NON-NLS-1$
							moduleX = (String) recordStatusObject.get("module_name"); //$NON-NLS-1$
						}
						if (moduleX != null
								&& ((appType == ConnectorUtil.MAIL_APP && moduleX.equalsIgnoreCase(EMAILS)) || (appType == ConnectorUtil.CALENDAR_APP && moduleX.equalsIgnoreCase(MEETINGS)))) {

							String sugarIdX = null;
							if (recordStatusObject.get("sugar_id") instanceof JSONString) { //$NON-NLS-1$
								sugarIdX = ((JSONString) recordStatusObject.get("sugar_id")).toJSONString(); //$NON-NLS-1$
							} else if (recordStatusObject.get("sugar_id") instanceof String) { //$NON-NLS-1$
								sugarIdX = (String) recordStatusObject.get("sugar_id"); //$NON-NLS-1$
							}
							if (sugarIdX != null) {
								sugarId = sugarIdX;
							}

						}

					}
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}
		}
		return sugarId;
	}

	private String dateTimeToString(Object obj) {
		String dtX = null;
		if (obj != null && obj instanceof DateTime) {
			try {

				SimpleDateFormat outFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //$NON-NLS-1$
				outFormatter.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
				dtX = outFormatter.format(((DateTime) obj).toJavaDate());
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}
		}
		return dtX;
	}

	private String buildSugarRequestForNewEmail(int appType) {

		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

		for (int i = 0; i < REQUEST_STRINGS_1.length; i++) {
			sb.append(REQUEST_STRINGS_1[i]);
		}

		for (int i = 0; i < REQUEST_STRINGS_2.length; i++) {
			sb.append(REQUEST_STRINGS_2[i]);
		}

		_isFirst = true;

		if (appType == ConnectorUtil.CALENDAR_APP) {
			buildNotesInvitees();
			buildInviteeContactList();
		}

		// ... SELECT block for each association ...
		sb.append(buildSugarSelectBlocks());

		// ... SELECT block for each valid invitee ...
		if (appType == ConnectorUtil.CALENDAR_APP) {
			sb.append(buildInviteeSelectBlocks());
		}

		if (!_isFirst) {
			sb.append(ConstantStrings.COMMA);
		}

		for (int i = 0; i < REQUEST_STRINGS_RECORD_Block_FOR_NEW_MESSAGE_1.length; i++) {
			sb.append(REQUEST_STRINGS_RECORD_Block_FOR_NEW_MESSAGE_1[i]);
		}

		// ... module ...
		sb.append("\"").append(getModule(appType)).append("\"") //$NON-NLS-1$ //$NON-NLS-2$
				.append(ConstantStrings.COMMA);

		for (int i = 0; i < REQUEST_STRINGS_RECORD_Block_FOR_NEW_MESSAGE_2.length; i++) {
			sb.append(REQUEST_STRINGS_RECORD_Block_FOR_NEW_MESSAGE_2[i]);
		}

		// ... creating Sugar fields ...

		sb.append(buildSugarNameValueList(appType));

		for (int i = 0; i < REQUEST_STRINGS_RECORD_Block_2.length; i++) {
			sb.append(REQUEST_STRINGS_RECORD_Block_2[i]);
		}

		_isFirst = true;

		// ... relationships for each association ...
		sb.append(buildSugarRelationships(appType));

		// ... relationships for each valid invitee ...
		if (appType == ConnectorUtil.CALENDAR_APP) {
			sb.append(buildInviteeRelationships());
		}

		// ... closing ...
		for (int i = 0; i < REQUEST_STRINGS_4.length; i++) {
			sb.append(REQUEST_STRINGS_4[i]);
		}

		return sb.toString();
	}

	private String getModule(int appType) {
		String module = null;
		if (appType == ConnectorUtil.MAIL_APP) {
			module = EMAILS;
		} else if (appType == ConnectorUtil.CALENDAR_APP) {
			module = MEETINGS;
		}
		return module;
	}

	private String buildSugarRequestForExistingEmail(int appType) {

		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

		for (int i = 0; i < REQUEST_STRINGS_1.length; i++) {
			sb.append(REQUEST_STRINGS_1[i]);
		}

		for (int i = 0; i < REQUEST_STRINGS_2.length; i++) {
			sb.append(REQUEST_STRINGS_2[i]);
		}

		_isFirst = true;

		// ... SELECT block for each association ...
		sb.append(buildSugarSelectBlocks());

		sb.append(buildSugarVersion(appType));

		for (int i = 0; i < REQUEST_STRINGS_RECORD_Block_2.length; i++) {
			sb.append(REQUEST_STRINGS_RECORD_Block_2[i]);
		}

		_isFirst = true;

		// ... relationships for each association ...
		sb.append(buildSugarRelationships(appType));

		// ... closing ...
		for (int i = 0; i < REQUEST_STRINGS_4.length; i++) {
			sb.append(REQUEST_STRINGS_4[i]);
		}

		return sb.toString();
	}

	private String buildSugarVersion(int appType) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (_associateDataMapForSugarUpdate != null) {
			for (int i = 0; i < REQUEST_STRINGS_RECORD_Block_FOR_EXISTING_MESSAGE_1.length; i++) {
				sb.append(REQUEST_STRINGS_RECORD_Block_FOR_EXISTING_MESSAGE_1[i]);
			}
		} else {
			if (_isFirst) {
				_isFirst = false;
			} else {
				sb.append(ConstantStrings.COMMA);
			}
			for (int i = 0; i < REQUEST_STRINGS_RECORD_Block_FOR_EXISTING_MESSAGE_1a.length; i++) {
				sb.append(REQUEST_STRINGS_RECORD_Block_FOR_EXISTING_MESSAGE_1a[i]);
			}
		}

		// assigne sugar id
		sb.append("\"").append(getAssignedSugarId()).append("\"").append(ConstantStrings.COMMA); //$NON-NLS-1$ //$NON-NLS-2$

		for (int i = 0; i < REQUEST_STRINGS_RECORD_Block_FOR_EXISTING_MESSAGE_2.length; i++) {
			sb.append(REQUEST_STRINGS_RECORD_Block_FOR_EXISTING_MESSAGE_2[i]);
		}

		// type
		sb.append("\"").append(getModule(appType)).append("\"") //$NON-NLS-1$ //$NON-NLS-2$
				.append(ConstantStrings.COMMA);

		for (int i = 0; i < REQUEST_STRINGS_RECORD_Block_FOR_EXISTING_MESSAGE_3.length; i++) {
			sb.append(REQUEST_STRINGS_RECORD_Block_FOR_EXISTING_MESSAGE_3[i]);
		}

		// version
		// int version = getSugarVersion(appType) + 1;

		int version = getSugarVersion(appType);

		sb.append(String.valueOf(version)).append(ConstantStrings.COMMA);

		for (int i = 0; i < REQUEST_STRINGS_RECORD_Block_FOR_EXISTING_MESSAGE_4.length; i++) {
			sb.append(REQUEST_STRINGS_RECORD_Block_FOR_EXISTING_MESSAGE_4[i]);
		}

		return sb.toString();
	}

	private int getSugarVersion(int appType) {

		int sugarVersion = 0;

		String txt = null;

		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		for (int i = 0; i < GV_STRINGS_1.length; i++) {
			sb.append(GV_STRINGS_1[i]);
		}

		for (int i = 0; i < GV_STRINGS_2.length; i++) {
			sb.append(GV_STRINGS_2[i]);
		}

		// assigned sugar id
		sb.append("\"").append(getAssignedSugarId()).append("\"").append(ConstantStrings.COMMA); //$NON-NLS-1$ //$NON-NLS-2$

		for (int i = 0; i < GV_STRINGS_3.length; i++) {
			sb.append(GV_STRINGS_3[i]);
		}

		// type
		sb.append("\"").append(getModule(appType)).append("\"") //$NON-NLS-1$ //$NON-NLS-2$
				.append(ConstantStrings.COMMA);

		for (int i = 0; i < GV_STRINGS_4.length; i++) {
			sb.append(GV_STRINGS_4[i]);
		}
		txt = sb.toString();

		sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		sb.append(ConstantStrings.USERID).append(ConstantStrings.EQUALS).append(NotesAccountManager.getInstance().getCRMUser()).append(ConstantStrings.AMPERSAND);
		sb.append(ConstantStrings.PASSWORD).append(ConstantStrings.EQUALS).append(NotesAccountManager.getInstance().getCRMPassword()).append((ConstantStrings.AMPERSAND));
		sb.append(txt);

		String output = SugarWebservicesOperations.getInstance().callNativeSugarRestWebService(sb.toString());

		UiUtils.webServicesLog("getSugarVersion", txt, output); //$NON-NLS-1$

		if (output != null) {
			JSONObject jsonObject = null;
			try {
				jsonObject = new JSONObject(output);
				JSONArray recordStatusArray = jsonObject.getJSONArray("record_status"); //$NON-NLS-1$

				boolean isError = jsonObject.getBoolean("errors"); //$NON-NLS-1$

				if (recordStatusArray != null && !isError) {
					for (int i = 0; i < recordStatusArray.length(); i++) {
						JSONObject recordStatusObject = (JSONObject) recordStatusArray.getJSONObject(0).getJSONObject("return_data"); //$NON-NLS-1$
						String versionX = recordStatusObject.getJSONObject("version").get("value").toString(); //$NON-NLS-1$ //$NON-NLS-2$

						sugarVersion = Integer.valueOf((versionX == null || versionX.equals(ConstantStrings.EMPTY_STRING)) ? "0" //$NON-NLS-1$ 
								: versionX).intValue();
					}
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}
		}
		return sugarVersion;
	}

	private String buildSugarRelationships(int appType) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (_associateDataMapForSugarUpdate != null) {

			Iterator<Map.Entry<String, List<AssociateData>>> it = _associateDataMapForSugarUpdate.getMyMap().entrySet().iterator();
			while (it.hasNext()) {

				Entry<String, List<AssociateData>> entry = it.next();

				String type = entry.getKey().substring(1);
				List<AssociateData> list = entry.getValue();
				for (int i = 0; i < list.size(); i++) {
					if (!list.get(i).isAssociated()) {
						String id = list.get(i).getId();
						sb.append(buildSugarRelationship(type, id, _isFirst, appType));
						_isFirst = false;
					}
				}
			}
		}
		return sb.toString();
	}

	private String buildSugarRelationship(String type, String id, boolean isFirst, int appType) {
		return buildSugarRelationship(type, id, isFirst, appType, "create"); //$NON-NLS-1$ 
	}
	private String buildSugarRelationship(String type, String id, boolean isFirst, int appType, String action) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (type != null && id != null) {
			if (!isFirst) {
				sb.append(ConstantStrings.COMMA);
			}
			sb.append("{\"name\":\"").append(getRelationshipType(type, appType)).append("\",\"action\":\"").append(action).append("\",\"related_id\":\"") //$NON-NLS-1$ //$NON-NLS-2$
					.append(id).append("\"}"); //$NON-NLS-1$
		}
		return sb.toString();

	}
	private String getRelationshipType(String type, int appType) {
		String relationshipType = null;
		if (type != null) {
			if (appType == ConnectorUtil.MAIL_APP) {
				relationshipType = type.toLowerCase();
			} else if (appType == ConnectorUtil.CALENDAR_APP) {
				relationshipType = type.toLowerCase();
				if (relationshipType.equalsIgnoreCase(SugarType.OPPORTUNITIES.getParentType().toLowerCase())) {
					// 25981
					relationshipType = "opportunities";//$NON-NLS-1$
				}
			}
		}
		return relationshipType;

	}

	private String buildSugarSelectBlocks() {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

		if (_associateDataMapForSugarUpdate != null) {
			boolean isFirst = true;

			Iterator<Map.Entry<String, List<AssociateData>>> it = _associateDataMapForSugarUpdate.getMyMap().entrySet().iterator();
			while (it.hasNext()) {

				Entry<String, List<AssociateData>> entry = it.next();

				String type = entry.getKey().substring(1);
				List<AssociateData> list = entry.getValue();
				for (int i = 0; i < list.size(); i++) {
					if (!list.get(i).isAssociated()) {
						String id = list.get(i).getId();
						sb.append(buildSugarSelectBlock(type, id, isFirst));
						_isFirst = isFirst = false;
					}
				}
			}
		}
		return sb.toString();

	}

	private String buildInviteeRelationships() {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (getSugarInviteeMap() != null) {

			Iterator<Entry<String, String>> it = getSugarInviteeMap().entrySet().iterator();
			while (it.hasNext()) {

				Entry<String, String> entry = it.next();
				String email = entry.getKey();
				String notesidName = entry.getValue();

				String status = null;
				// 48249 - if i am not the chair, and NoticeType exists, the key of the getInviteeStatusMap() is sugar user id format ( for example: anne_wang@us.ibm.com)
				if (getInviteeStatusMap() != null && !getInviteeStatusMap().isEmpty() && getInviteeStatusMap().containsKey(email)) {
					status = (String) getInviteeStatusMap().get(email);
				} else {
					status = getStatusFromInviteeStatusMap(notesidName);
				}

				if (email != null) {
					sb.append(buildInviteeRelationship(email, "create", status, _isFirst)); //$NON-NLS-1$
					_isFirst = false;
				}

			}
		}
		return sb.toString();
	}

	private String buildInviteeSelectBlocks() {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

		// Querying every invitees in _notesInviteeList via Users module to determine if the invitee is a
		// valid SalesConnect user. If it is, save email and lotus notes id in _sugarInviteeMap.
		buildSugarInviteeMap();

		if (getSugarInviteeMap() != null) {

			Iterator<String> it = getSugarInviteeMap().keySet().iterator();
			while (it.hasNext()) {
				String email = it.next();
				// invitee select block is after sugar item select block, use false isFirst flag.
				sb.append(buildSugarSelectBlock("Users", email, "user_name", _isFirst));
				if (_isFirst) {
					_isFirst = false;
				}

			}
		}
		return sb.toString();

	}

	private Map<String, String> getSugarInviteeMap() {
		return _sugarInvitees;
	}
	private void buildSugarInviteeMap() {
		if (getNotesInvitees() != null && !getNotesInvitees().isEmpty()) {
			_sugarInvitees = performEmailGetEntryListWebService(buildUserQueryList(getNotesInvitees())); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	// example:"module_name":"Users","query":"(users.lotus_notes_id like 'anne wang/westford/IBM@notes%' or users.user_name='anne_wang@us.ibm.com')","select_fields":""}
	private String buildUserQueryList(List<String> idList) {
		boolean isFirst = true;
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (idList != null && !idList.isEmpty()) {

			sb.append("{\"module_name\":\"Users\",\"query\":\"("); //$NON-NLS-1$  
			for (String id : idList) {
				if (isFirst) {
					isFirst = false;
				} else {
					sb.append(" or "); //$NON-NLS-1$
				}

				if (isNotesIDFormat(id)) {
					sb.append("users.lotus_notes_id like '").append(stripDomain(id)).append("%' "); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					sb.append("users.user_name='").append(id).append("'"); //$NON-NLS-1$  //$NON-NLS-2$  
				}
			}
			sb.append(")\",\"select_fields\":\"\"}");
		}
		return sb.toString();
	}

	private Map<String, String> performEmailGetEntryListWebService(String arguments) {
		Map<String, String> map = null;
		if (arguments != null && !arguments.equals(ConstantStrings.EMPTY_STRING)) {

			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("method", "get_entry_list"); //$NON-NLS-1$ //$NON-NLS-2$
			parameters.put("arguments", arguments); //$NON-NLS-1$

			String output = SugarWebservicesOperations.getInstance().getSugarInfoFromWebService(parameters);

			UiUtils.webServicesLog("sugarValidEmails", null, output); //$NON-NLS-1$

			try {
				map = extractEmailJSONOutput(output);
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);

			}

		}
		return map;
	}

	private Map<String, String> extractEmailJSONOutput(String output) {
		Map<String, String> map = null;
		if (output != null) {
			try {

				final JSONObject searchResultsJSON = new JSONObject(output);

				if (searchResultsJSON.containsKey("entry_list")) //$NON-NLS-1$
				{

					JSONArray resultsArray = searchResultsJSON.getJSONArray("entry_list"); //$NON-NLS-1$

					for (int i = 0; i < resultsArray.length(); i++) {
						JSONObject entrylistObject = (JSONObject) resultsArray.get(i);
						JSONObject namevaluelistObject = entrylistObject.getJSONObject("name_value_list"); //$NON-NLS-1$

						String email = namevaluelistObject.getJSONObject("user_name").getString("value"); //$NON-NLS-1$ //$NON-NLS-2$
						String lotusNotesId = namevaluelistObject.getJSONObject("lotus_notes_id").getString("value"); //$NON-NLS-1$ //$NON-NLS-2$
						if (email != null) {
							if (map == null) {
								map = new HashMap<String, String>();
							}
							if (!map.containsKey(email)) {
								map.put(email, lotusNotesId);
							}
						}
					}
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}
		}
		return map;
	}
	private List<String> getNotesInviteeList() {
		List<String> l = null;

		if (_mailDoc != null) {
			try {

				if (l == null) {
					l = new ArrayList<String>();
				}

				List requiredL = MailDocumentSelectionAction.getInstance().getNotesInvitees(_mailDoc, MailDocumentSelectionAction.REQUIRED_INVITEES_TYPE);
				if (requiredL != null) {
					l.addAll(requiredL);
				}
				printList("\nAssoicateUpdateManager... for new meeting, REQUIRED", requiredL); //$NON-NLS-1$

				List optionalL = MailDocumentSelectionAction.getInstance().getNotesInvitees(_mailDoc, MailDocumentSelectionAction.OPTIONAL_INVITEES_TYPE);
				if (optionalL != null) {
					l.addAll(optionalL);
				}

				printList("\nAssoicateUpdateManager... for new meeting, OPTIONAL", optionalL); //$NON-NLS-1$

				extractCalendarInviteeStatus();

				if (getInviteeStatusMap() == null || getInviteeStatusMap().isEmpty()) {
				} else {
					Iterator<String> it = getInviteeStatusMap().keySet().iterator();
					while (it.hasNext()) {
						String name = it.next();
						if (!l.contains(name)) {
							l.add(name);
						}
					}

				}

				// Check if i am the chair, if not, add chair to the invitee list and add "accepted" as the invitee status
				// Note that Notes item CHAIR value is in canonical format.
				Vector chairV = _mailDoc.getItemValue("CHAIR"); //$NON-NLS-1$
				if (!amIChair(chairV)) {
					if (_session != null) {
						try {
							Name chairNameObject = _session.createName((String) chairV.get(0));
							// get chair name in regular format (non-canonical format)
							String chairName = chairNameObject.getAbbreviated();

							// add chair name to invitee list
							if (l == null) {
								l = new ArrayList<String>();
							}
							if (!l.contains(chairName)) {
								l.add(chairName);
							}
							UiUtils.log("\nAssoicateUpdateManager... for new meeting, adding chair to invitee list: " + chairName); //$NON-NLS-1$

							// add "accepted" as chair status
							getInviteeStatusMap().put(chairName, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ASSOCIATE_MEETING_INVITEE_STATUS_ACCEPT));

							UiUtils.log("\nAssoicateUpdateManager... for new meeting, adding Accepted status to invitee status map for chair: " + chairName); //$NON-NLS-1$

							// 48249 - if I am not the chair, and if NoticeType field exists, add me to the invitee list and
							// add NoticeType field as the invitee status. (i..e I accepted/declined/delegated the inviation)
							if (_mailDoc.hasItem("NoticeType")) { //$NON-NLS-1$ 
								String statusX = getNoticeType(_mailDoc);
								String bookFreeTime = getNotesItemString(_mailDoc, "BookFreeTime");
								if (statusX == null) {
									// 48248
									// this is an "update" document (i.e. a reschedule notification document) which does not have
									// my status... can I find the parent document?
									statusX = getNoticeType(getParentDocument());
									bookFreeTime = getNotesItemString(getParentDocument(), "BookFreeTime");
								}
								// 48249
								// if noticetype is "A" but bookfreetime is "1", this is actually a tentative accepted meeting.
								if (statusX != null && statusX.equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ASSOCIATE_MEETING_INVITEE_STATUS_ACCEPT))
										&& bookFreeTime != null && bookFreeTime.equalsIgnoreCase("1")) {
									statusX = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ASSOCIATE_MEETING_INVITEE_STATUS_TENTATIVEACCEPT);
								}

								if (statusX != null) {
									if (l == null) {
										l = new ArrayList<String>();
									}
									if (!l.contains(NotesAccountManager.getInstance().getCRMUser())) {
										l.add(NotesAccountManager.getInstance().getCRMUser());
									}
									UiUtils.log("\nAssoicateUpdateManager... for new meeting, adding me to invitee list: " + NotesAccountManager.getInstance().getCRMUser()); //$NON-NLS-1$

									// add "accepted" as chair status
									getInviteeStatusMap().put(NotesAccountManager.getInstance().getCRMUser(), statusX);

									UiUtils.log("\nAssoicateUpdateManager... for new meeting, adding status for me : " + statusX); //$NON-NLS-1$

								}
							}

						} catch (Exception e) {
							UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
						}
					}
				}

			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}
		}

		return l;
	}
	private Document getParentDocument() {
		Document parentDoc = null;
		try {
			String parentunid = _mailDoc.getParentDocumentUNID();
			UiUtils.log("\nThis is a notification document which does not have my status information, check the parent document: unid-" + parentunid);
			if (parentunid != null) {
				parentDoc = _mailDb.getDocumentByUNID(parentunid);
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}

		return parentDoc;
	}
	private String getNotesItemString(Document doc, String itemX) {
		String valueX = null;
		if (doc != null && itemX != null) {
			try {
				valueX = doc.getItemValueString(itemX); //$NON-NLS-1$ 
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}

		}
		return valueX;
	}

	private boolean amIChair(Vector v) {
		boolean amChair = false;
		if (v != null && !v.isEmpty() && _myCanonicalName != null) {
			if (v.contains(_myCanonicalName)) {
				amChair = true;
			}
		}
		return amChair;
	}

	private boolean isAnyValidEntry(Vector v) {
		boolean isValid = false;
		if (v != null && !v.isEmpty()) {
			for (int i = 0; i < v.size(); i++) {
				if (!v.get(i).equals(".")) {
					isValid = true;
				}
			}
		}
		return isValid;
	}

	private String stripDomain(String name) {
		String outX = name;
		if (outX != null) {
			if (outX.indexOf(ConstantStrings.FORWARD_SLASH) > -1 && outX.indexOf("@") > -1) {
				outX = outX.substring(0, outX.indexOf("@"));
			}
		}
		return outX;
	}

	private boolean isNotesIDFormat(String name) {
		boolean isNotesIDFormat = false;
		// if name contains "/", let's count it as Notes format, it could be <name>/<location>/ibm or <name>/<location>/ibm@<domain>
		// for example: "Bill Smith/westford/ibm" or "BIll Smith /westford/ibm@notes"
		if (name != null && name.indexOf(ConstantStrings.FORWARD_SLASH) > -1) {
			isNotesIDFormat = true;
		}
		return isNotesIDFormat;
	}

	private String buildSugarSelectBlock(String type, String id, boolean isFirst) {
		return buildSugarSelectBlock(type, id, "id", isFirst);
	}

	private String buildSugarSelectBlock(String type, String id, String idField, boolean isFirst) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (type != null && id != null) {
			if (!isFirst) {
				sb.append(ConstantStrings.COMMA);
			}
			sb.append("{\"id\":\"").append(id).append("\",\"").append("id_field").append("\":\"").append(idField).append("\",\"module_name\":\"").append(type) //$NON-NLS-1$ //$NON-NLS-2$ 
					.append("\",\"action\":\"select\"}"); //$NON-NLS-1$ 
		}
		return sb.toString();
	}

	private String buildSugarNameValueList(int appType) {

		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		int associateCount = 0;

		if (_associateDataMapForSugarUpdate != null) {
			boolean isFirstRecord = true;

			Iterator<Map.Entry<String, List<AssociateData>>> it = _associateDataMapForSugarUpdate.getMyMap().entrySet().iterator();
			while (it.hasNext() && isFirstRecord) {

				Entry<String, List<AssociateData>> entry = it.next();

				String type = entry.getKey().substring(1);
				List<AssociateData> list = entry.getValue();
				for (int i = 0; i < list.size(); i++) {
					if (!list.get(i).isAssociated()) {
						associateCount++;

						String id = list.get(i).getId();
						if (appType == ConnectorUtil.MAIL_APP) {
							sb.append(buildSugarMailNameValueListFields(type, id, associateCount, isFirstRecord));
						} else if (appType == ConnectorUtil.CALENDAR_APP) {
							sb.append(buildSugarCalendarNameValueListFields(type, id, associateCount, isFirstRecord));
						}
						isFirstRecord = false;
						break;
					}
				}
			}
		}
		return sb.toString();
	}

	private String buildSugarMailNameValueListFields(String type, String id, int associateCount, boolean isFirstRecord) {
		String request = null;
		boolean isContinue = false;

		String fromAddr = null;
		String toAddrs = null;
		String ccAddrs = null;
		String bccAddrs = null;
		String name = null;
		String description = null;
		String docCreatedDateX = null;

		try {
			fromAddr = encodeString(_mailDoc.getItemValueString("INetFrom")); //$NON-NLS-1$

			Vector<Object> v = _mailDoc.getItemValue("InetSendTo"); //$NON-NLS-1$
			Vector<Object> sub1 = _mailDoc.getItemValue("SendTo"); //$NON-NLS-1$
			toAddrs = encodeString(vectorToString(v, sub1));

			v = _mailDoc.getItemValue("InetCopyTo"); //$NON-NLS-1$
			sub1 = _mailDoc.getItemValue("CopyTo"); //$NON-NLS-1$
			ccAddrs = encodeString(vectorToString(v, sub1));

			v = _mailDoc.getItemValue("InetBlindCopyTo"); //$NON-NLS-1$
			sub1 = _mailDoc.getItemValue("BlindCopyTo"); //$NON-NLS-1$
			bccAddrs = encodeString(vectorToString(v, sub1));

			name = encodeString(_mailDoc.getItemValueString("Subject")); //$NON-NLS-1$

			String body = _mailDoc.getItemValueString("Body"); //$NON-NLS-1$

			if (_duOperations.getSuccessfulResultsMap() != null && !_duOperations.getSuccessfulResultsMap().isEmpty()) {
				body = appendAttachmentURLToBody(body);
			}

			description = encodeString(body);

			DateTime dt = null;
			try {
				dt = _mailDoc.getCreated();
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}
			docCreatedDateX = dateTimeToString(dt);

			isContinue = true;
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}

		if (isContinue) {
			StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

			if (!isFirstRecord) {
				sb.append(ConstantStrings.COMMA);
			}

			boolean isFirstField = true;
			buildSugarNameValueListField(sb, "from_addr", fromAddr, isFirstField); //$NON-NLS-1$

			isFirstField = false;
			buildSugarNameValueListField(sb, "created_by", NotesAccountManager.getInstance().getCRMUser(), isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "to_addrs", toAddrs, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "cc_addrs", ccAddrs, isFirstField);//$NON-NLS-1$
			buildSugarNameValueListField(sb, "bcc_addrs", bccAddrs, isFirstField);//$NON-NLS-1$
			buildSugarNameValueListField(sb, "name", name, isFirstField);//$NON-NLS-1$
			buildSugarNameValueListField(sb, "assigned_user_id", SugarWebservicesOperations.getInstance().getUserCNUM(), isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "description", description, isFirstField);//$NON-NLS-1$
			buildSugarNameValueListField(sb, "date_sent", docCreatedDateX, isFirstField);//$NON-NLS-1$
			buildSugarNameValueListField(sb, "parent_type", type, isFirstField);//$NON-NLS-1$
			buildSugarNameValueListField(sb, "parent_id", id, isFirstField);//$NON-NLS-1$
			buildSugarNameValueListField(sb, "status", "archived", isFirstField);//$NON-NLS-1$ //$NON-NLS-2$ 
			buildSugarNameValueListField(sb, "type", "out", isFirstField);//$NON-NLS-1$ //$NON-NLS-2$
			buildSugarNameValueListField(sb, "team_id", "1", isFirstField);//$NON-NLS-1$ //$NON-NLS-2$

			request = sb.toString();
		}
		return request;
	}

	private String appendAttachmentURLToBody(String body) {
		int i = 1;
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (body != null) {
			sb.append(body);
		}
		if (_duOperations.getSuccessfulResultsMap() != null && !_duOperations.getSuccessfulResultsMap().isEmpty()) {
			Iterator<Entry<String, DocumentUploadObject>> it = _duOperations.getSuccessfulResultsMap().entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, DocumentUploadObject> entry = it.next();
				DocumentUploadObject value = entry.getValue();

				if (value != null) {
					if (i == 1) {
						sb.append(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_ATTACHMENT_LINE,
								new String[]{String.valueOf(_duOperations.getSuccessfulResultsMap().size())}));
					}

					sb.append("\n").append(String.valueOf(i)).append(".").append(ConstantStrings.SPACE).append(UtilsPlugin //$NON-NLS-1$ //$NON-NLS-2$
							.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_DOCUMENT_FILE_NAME)).append(value.getUploadedDocumentName()).append(ConstantStrings.SPACE).append(
							ConstantStrings.SPACE).append(ConstantStrings.LEFT_PARENTHESIS).append(ConstantStrings.SPACE).append(ConstantStrings.SPACE).append(
							UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_DOCUMENT_FILE_URL)).append(buildDocumentURL(value.getUploadedDocumentSugarId())).append(
							ConstantStrings.SPACE).append(ConstantStrings.RIGHT_PARENTHESIS);

					i++;
				}
			}

		}

		return sb.toString();
	}

	private String buildDocumentURL(String id) {
		String url = ConstantStrings.EMPTY_STRING;
		if (id != null) {
			StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
			sb.append(NotesAccountManager.getInstance().getCRMServer()).append("index.php?module=Documents&action=DetailView&record=").append(id); //$NON-NLS-1$ 
			url = sb.toString();
		}
		UiUtils.log("buildDocumentURL..." + url); //$NON-NLS-1$ 
		return url;
	}

	/*
	 * convert names in the vector to a string, names are separated by commas. It is Notes' convention, if any problem with the name, it was replaced by either a space or a dot. When we see this, will
	 * try to use the name in sub1.
	 */
	private String vectorToString(Vector<Object> v, Vector<Object> sub1) {
		String outX = ConstantStrings.EMPTY_STRING;
		if (v != null) {
			StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
			boolean isFirst = true;
			int i = -1;
			Iterator<Object> it = v.iterator();
			while (it.hasNext()) {
				Object obj = it.next();
				i++;
				if (obj != null && obj instanceof String) {
					if (isFirst) {
						isFirst = false;
					} else {
						sb.append(ConstantStrings.COMMA);
					}
					if (((String) obj).equals(".")) //$NON-NLS-1$
					{
						if (sub1 != null && sub1.size() > i && sub1.get(i) != null && sub1.get(i) instanceof String) {
							obj = sub1.get(i);
						}
					}

					sb.append((String) obj);

				}

			}
			outX = sb.toString();
		}
		return outX;
	}

	private String encodeString(String s) {
		String outX = s;
		if (outX != null) {
			try {
				// Escape special characters in the JSON string
				outX = outX.replaceAll("\\\\", "\\\\\\\\"); //$NON-NLS-1$ //$NON-NLS-2$
				outX = outX.replaceAll("\n", "\\\\n"); //$NON-NLS-1$ //$NON-NLS-2$
				outX = outX.replaceAll("\r", "\\\\r"); //$NON-NLS-1$ //$NON-NLS-2$
				outX = outX.replaceAll("\t", "\\\\t"); //$NON-NLS-1$ //$NON-NLS-2$
				outX = outX.replaceAll("\\{", "\\\\u007b"); //$NON-NLS-1$ //$NON-NLS-2$
				outX = outX.replaceAll("\\}", "\\\\u007d"); //$NON-NLS-1$ //$NON-NLS-2$
				outX = outX.replaceAll("\\[", "\\\\u005b"); //$NON-NLS-1$ //$NON-NLS-2$
				outX = outX.replaceAll("\\]", "\\\\u005d"); //$NON-NLS-1$ //$NON-NLS-2$									
				outX = outX.replaceAll("\\]", "\\\\u005d"); //$NON-NLS-1$ //$NON-NLS-2$
				outX = outX.replaceAll("\"", "\\\\u0022"); //$NON-NLS-1$ //$NON-NLS-2$
				outX = outX.replaceAll("'", "\\\\u0027"); //$NON-NLS-1$ //$NON-NLS-2$				
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}
		}
		return outX;
	}

	private String buildSugarCalendarNameValueListFields(String type, String id, int associateCount, boolean isFirstRecord) {
		String request = null;
		boolean isContinue = false;

		String location = null;
		String dateStartX = null;
		String timeStartX = null;
		String dateEndX = null;
		String durationHoursX = null;
		String durationMinutesX = null;
		String appointmentType = null; /* 3=Meeting, 0=Appointment */
		String name = null;
		String description = null;
		String status = null;

		// Following are taken from WSDL:
		// added for sugar 1.0: -1 means no alert; otherwise the number of seconds prior to the start
		// But, web service would not accept "0", "-1", "00", but it takes an empty value "", so
		// use it when reminder is not turned on.
		String reminder_timeX = ""; //$NON-NLS-1$

		try {
			location = encodeString(_mailDoc.getItemValueString("Location")); //$NON-NLS-1$

			Object objStart = _mailDoc.getItemValueDateTimeArray("StartDateTime").get(0); //$NON-NLS-1$
			if (objStart != null && objStart instanceof DateTime) {
				dateStartX = dateTimeToString(objStart);
				timeStartX = ((DateTime) objStart).getTimeOnly();
			}

			Object objEnd = _mailDoc.getItemValueDateTimeArray("EndDateTime").get(0); //$NON-NLS-1$
			if (objEnd != null && objEnd instanceof DateTime) {
				dateEndX = dateTimeToString(objEnd);
			}

			int durationSeconds = ((DateTime) objEnd).timeDifference(((DateTime) objStart));
			int durationHours = durationSeconds / 3600;
			durationHoursX = String.valueOf(durationHours);

			// web service AVL likes to round up to 15 minutes
			int interval = 15;
			int durationMinutes = (durationSeconds - durationHours * 3600) / 60;

			durationMinutes = (durationMinutes + interval - 1) / interval * interval;
			durationMinutesX = String.valueOf(durationMinutes);

			appointmentType = getAppointmentType();

			name = encodeString(_mailDoc.getItemValueString("Subject")); //$NON-NLS-1$

			String body = _mailDoc.getItemValueString("Body"); //$NON-NLS-1$

			if (_duOperations.getSuccessfulResultsMap() != null && !_duOperations.getSuccessfulResultsMap().isEmpty()) {
				body = appendAttachmentURLToBody(body);
			}

			description = encodeString(body);

			// 47019 - as far as i can see meeting API allows only alarms 5, 10 and 15 minutes prior to meeting; other values
			// might cause AVL error... and because this is only a text field, does not trigger any notification to the user
			// so, remove it.
			// int alarmMinute = 1;
			//			String alarm = _mailDoc.getItemValueString("Alarms"); //$NON-NLS-1$
			//			if (alarm != null && alarm.equals("1")) //$NON-NLS-1$
			// {
			//				Integer alarmoffset = _mailDoc.getItemValueInteger("$AlarmOffset"); //$NON-NLS-1$
			// if (alarmoffset != null) {
			// alarmMinute = alarmoffset.intValue() * (-1);
			// }
			//
			// // web service does not like > 1 day
			// int reminder_time = Math.min(86400, alarmMinute * 60);
			//
			// // if 0, use default reminder_timeX.
			// if (reminder_time != 0) {
			// reminder_timeX = String.valueOf(reminder_time);
			// }
			//
			// }

			status = isMeetingInThePast((DateTime) objStart) ? "Held" : "planned"; //$NON-NLS-1$ //$NON-NLS-2$

			isContinue = true;
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}

		if (isContinue) {
			StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

			if (!isFirstRecord) {
				sb.append(ConstantStrings.COMMA);
			}

			boolean isFirstField = true;
			buildSugarNameValueListField(sb, "assigned_user_id", SugarWebservicesOperations.getInstance().getUserCNUM(), isFirstField); //$NON-NLS-1$

			isFirstField = false;
			buildSugarNameValueListField(sb, "created_by", NotesAccountManager.getInstance().getCRMUser(), isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "location", location, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "name", name, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "date_start", dateStartX, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "time_start", timeStartX, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "date_end", dateEndX, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "description", description, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "parent_type", type, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "parent_id", id, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "duration_hours", durationHoursX, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "duration_minutes", durationMinutesX, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "status", status, isFirstField); //$NON-NLS-1$ //$NON-NLS-2$
			buildSugarNameValueListField(sb, "direction", "Outbound", isFirstField); //$NON-NLS-1$ //$NON-NLS-2$
			buildSugarNameValueListField(sb, "team_id", "1", isFirstField); //$NON-NLS-1$ //$NON-NLS-2$

			buildSugarNameValueListField(sb, "reminder_time", reminder_timeX, isFirstField); //$NON-NLS-1$ 
			buildSugarNameValueListField(sb, "email_reminder_time", reminder_timeX, isFirstField); //$NON-NLS-1$ 

			request = sb.toString();
		}
		return request;
	}

	private String getAppointmentType() {
		String appointmentType = "Calls"; //$NON-NLS-1$
		try {
			String appType = _mailDoc.getItemValueString("AppointmentType"); //$NON-NLS-1$
			if (appType != null && appType.equalsIgnoreCase("3")) //$NON-NLS-1$
			{
				appointmentType = "Meetings"; //$NON-NLS-1$
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}
		return appointmentType;
	}

	private void buildSugarNameValueListField(StringBuffer sb, String name, String value, boolean isFirstField) {
		if (!isFirstField) {
			sb.append(ConstantStrings.COMMA);
		}
		if (sb != null && name != null && value != null) {
			sb.append("{\"name\":\"").append(name).append("\"").append(ConstantStrings.COMMA); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("\"value\":\"").append(value).append("\"}"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private boolean updateNotes() {
		boolean isOK = false;
		try {

			if (_mailDoc != null) {
				// Be sure all the AssociateData has isAssoicated flag marked as true. We might not need to do it here
				// because the values might have been updated in the AssociateToolBarControl, but, in case there's
				// timing issue, do it again.
				_mailDoc.replaceItemValue(MailDocumentSelectionAction.CRM_ASSOCIATE, updateAllAssociateDataI());

				_mailDoc.save(true);
				isOK = true;
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}
		return isOK;
	}

	private void updateAssignedSugarId(String assignedSugarId) {
		String out = null;
		if (getAssociateDataMapXML() != null) {
			AssociateDataMap associateDataMap = ConnectorUtil.decode(getAssociateDataMapXML());
			associateDataMap.setAssignedSugarId(assignedSugarId);
			out = ConnectorUtil.encode(associateDataMap);
			updateAssociateDataMap(out);
		}
	}

	// Mark all the associate entries as associated.
	private String updateAllAssociateDataI() {

		String out = null;
		if (getAssociateDataMapXML() != null) {

			AssociateDataMap associateDataMap = ConnectorUtil.decode(getAssociateDataMapXML());
			associateDataMap.setAllIsAssociated(true);
			out = ConnectorUtil.encode(associateDataMap);

		}
		return out;
	}

	private boolean openNotes() {
		boolean isOK = false;

		try {
			NotesThread.sinitThread();
			_session = NotesFactory.createSession();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}

		try {
			if (_doc != null) {

				String unid = null;
				// NotesException: Invalid Notes protocol, Notes URLs must start with notes://.
				final NotesDocumentData notesDocumentData = _doc.getDocumentData();
				NotesDatabaseData notesDatabaseData = _doc.getDatabaseData();
				if (notesDocumentData != null) {
					unid = notesDocumentData.getUnid();
				}

				_mailDb = _session.getDatabase(notesDatabaseData.getServer(), notesDatabaseData.getFilePath());

				if (!_mailDb.isOpen()) {
					// defect 23563 - use openWithFailover in case specified server is failed over
					// to the failover server.
					boolean isopen = _mailDb.openWithFailover(notesDatabaseData.getServer(), notesDatabaseData.getFilePath());

					String[] msgs = new String[]{"Open database: ", (isopen ? "Successful" : "Failed"), //$NON-NLS-1$  //$NON-NLS-2$ //$NON-NLS-3$
							" ... Orig. specified server: ", notesDatabaseData.getServer(), ", dbfile: ", notesDatabaseData.getFilePath(), //$NON-NLS-1$  //$NON-NLS-2$
							" ... Result server: ", _mailDb.getServer(), ", dbfile: ", _mailDb.getFileName()}; //$NON-NLS-1$  //$NON-NLS-2$

					if (isopen) {
						UtilsPlugin.getDefault().logInfoMessage(msgs, Activator.PLUGIN_ID);
					} else {
						UtilsPlugin.getDefault().logWarningMessage(msgs, Activator.PLUGIN_ID);

					}

				}
				_mailDoc = null;
				if (_mailDb.isOpen()) {
					try {
						_mailDoc = _mailDb.getDocumentByUNID(unid);

						// check if chair cancelled the meeting
						String noticetype = _mailDoc.getItemValueString("NoticeType");
						if (noticetype != null && noticetype.equalsIgnoreCase("c")) {
							setChairCancelMeeting(true);
							isOK = true;
							return isOK;
						}

					} catch (NotesException e) {

						if (isDirtyCalendarWithoutAssociationUpdate()) {
							UiUtils.log("Can not get Notes meeting document because chair cancelled the meeting"); //$NON-NLS-1$
							setChairCancelMeeting(true);
							isOK = true;
							return isOK;
						}

						// Don't panic. This might be legitimate exception. For example: user cancels a new mail/meeting.
						// The Sugar/Notes update process triggered by the Part close listener might throw a Notes Exception here
						// because it can not find the document.

						UiUtils.log("Association did not go through because you just cancelled a new message or a meeting invitation"); //$NON-NLS-1$
						// do nothing, return

						return isOK;
					}

				}
			} else
			// if DND from list view (for example: inbox list view)
			{
				Vector vals = _session.evaluate("@MailDbName"); //$NON-NLS-1$
				_mailDb = _session.getDatabase((String) vals.get(0), (String) vals.get(1));

				if (!_mailDb.isOpen()) {
					_mailDb.open();
				}

				_mailDoc = null;
				try {
					_mailDoc = _mailDb.getDocumentByUNID(_mapkey);
				} catch (NotesException ne) {
					// Sometimes, we catch these when the mail view is selected. It seems harmless enough.
					_mailDoc = null;
				}
			}

			if (isMailUnsend(_mailDoc)) {
				// CTRL-S while composing a mail, then cancel the new mail
			} else {
				// Build attachment list for new mail / meeting
				if (ConnectorUtil.isNewDocument(_mapkey) && (_duOperations.getSuccessfulResultsMap() == null || _duOperations.getSuccessfulResultsMap().isEmpty())) {
					_attachmentList = UiUtils.getNotesAttachmentNames(_mailDoc);
					if (_attachmentList != null) {
						for (int i = 0; i < _attachmentList.length; i++) {
							_attachmentList[i] = _attachmentList[i].substring(1);
						}
					}

				}

				isOK = true;
			}

			_myCanonicalName = getUserCanonicalName();

		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}

		return isOK;
	}

	private String getUserCanonicalName() {
		String userCanonicalName = null;
		try {
			if (_session != null) {
				userCanonicalName = _session.getUserName();
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}
		return userCanonicalName;
	}

	private boolean isMailUnsend(Document doc) {
		boolean isMailUnsend = false;
		if (doc != null) {
			try {
				String form = doc.getItemValueString("form"); //$NON-NLS-1$
				String mailer = doc.getItemValueString("$Mailer"); //$NON-NLS-1$
				String messageID = doc.getItemValueString("$MessageID"); //$NON-NLS-1$

				if (form != null
						&& form.equalsIgnoreCase("memo") && (mailer == null || mailer.equals(ConstantStrings.EMPTY_STRING)) && (messageID == null || messageID.equals(ConstantStrings.EMPTY_STRING))) //$NON-NLS-1$
				{
					isMailUnsend = true;
				}

			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}

		}
		return isMailUnsend;
	}

	private void closeNotes() {
		try {
			if (_mailDoc != null) {
				_mailDoc.recycle();
			}
			if (_mailDb != null) {
				_mailDb.recycle();
			}
			if (_session != null) {
				_session.recycle();
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		} finally {
			NotesThread.stermThread();
		}
	}

	private String getAssociateDataMapXML() {
		String associateDataMapXML = null;
		if (NotesUIDocumentSaveListenerRepository.getDocumentActionMap() != null) {
			SugarEntrySurrogate sugarEntry = (SugarEntrySurrogate) NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(_mapkey);
			if (sugarEntry != null) {
				associateDataMapXML = sugarEntry.getAssociateDataMapXML();
			}

		}
		return associateDataMapXML;
	}

	private void updateAssociateDataMap(String xml) {
		if (NotesUIDocumentSaveListenerRepository.getDocumentActionMap() != null) {
			SugarEntrySurrogate sugarEntry = (SugarEntrySurrogate) NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(_mapkey);
			if (sugarEntry != null) {
				sugarEntry.setAssociateDataMapXML(xml);
				NotesUIDocumentSaveListenerRepository.updateSugarEntry(_mapkey, sugarEntry);
			}
		}
	}

	private String getAssignedSugarId() {
		String sugarId = null;
		if (getAssociateDataMapXML() != null) {
			AssociateDataMap associateDataMap = ConnectorUtil.decode(getAssociateDataMapXML());
			sugarId = associateDataMap.getAssignedSugarId();
		}

		return sugarId;
	}

	public Map<File, String> getUploadDocumentMap() {
		if (_uploadDocumentMap == null) {
			_uploadDocumentMap = new HashMap<File, String>();
		}
		return _uploadDocumentMap;
	}

	// ------------------- Internal class used for file upload ------------------------
	/**
	 * This class provides a output stream that can url encode the output
	 */
	class URLEncodeFilterOutputStream extends FilterOutputStream {
		public boolean applyFilter = true;

		public URLEncodeFilterOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void write(byte[] bytes, int off, int len) throws IOException {
			if (applyFilter) {
				byte[] newBytes = urlEncodeBytes(bytes);
				// Same reason as for the write(byte[]) method.
				boolean oldFilter = applyFilter;
				applyFilter = false;
				super.write(newBytes, off, newBytes.length);
				applyFilter = oldFilter;
			} else {
				super.write(bytes, off, len);
			}
		}

		@Override
		public void write(byte[] bytes) throws IOException {
			if (applyFilter) {
				byte[] newBytes = urlEncodeBytes(bytes);
				// This can result in a call to write(byte[], int, int) so we don't want to double encode
				// NOT Thread safe! Shouldn't matter though as only one write method should be called at a time
				// from a given thread.
				boolean oldFilter = applyFilter;
				applyFilter = false;
				super.write(newBytes);
				applyFilter = oldFilter;
			} else {
				super.write(bytes);
			}
		}

		/**
		 * URL encodes the list of bytes and returns an array of url encoded bytes
		 * 
		 * @param bytes
		 * @return
		 */
		private byte[] urlEncodeBytes(byte[] bytes) {
			try {
				bytes = URLEncoder.encode(new String(bytes), "UTF-8").getBytes(); //$NON-NLS-1$
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}

			return bytes;
		}

		public void setApplyFilter(boolean applyFilter) {
			this.applyFilter = applyFilter;
		}

	}

	public void setDirtyCalendarWithoutAssociationUpdate(boolean b) {
		_isDirtyCalendarWithoutAssociationUpdate = b;
	}
	private boolean isDirtyCalendarWithoutAssociationUpdate() {
		return _isDirtyCalendarWithoutAssociationUpdate;
	}
	public void setChairCancelMeeting(boolean b) {
		_isChairCancelMeeting = b;
	}
	private boolean isChairCancelMeeting() {
		return _isChairCancelMeeting;
	}
	private String buildSugarRequestForExistingCalendar(int appType) {

		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

		for (int i = 0; i < REQUEST_STRINGS_1.length; i++) {
			sb.append(REQUEST_STRINGS_1[i]);
		}

		for (int i = 0; i < REQUEST_STRINGS_2.length; i++) {
			sb.append(REQUEST_STRINGS_2[i]);
		}

		_isFirst = true;

		if (!isChairCancelMeeting()) {

			buildNotesInvitees();
			buildInviteeContactList();

			// ... SELECT block for each item to be added ...
			sb.append(buildSugarSelectBlocks());

			// ... SELECT block for each contact to be removed ...
			if (getUninvitedContactMap() != null && !getUninvitedContactMap().isEmpty()) {
				Iterator<AssociateData> associateDataIt = getUninvitedContactMap().values().iterator();
				while (associateDataIt.hasNext()) {
					AssociateData assodiateData = associateDataIt.next();
					sb.append(buildSugarSelectBlock(SugarType.CONTACTS.getParentType(), assodiateData.getId(), _isFirst));
					_isFirst = false;
				}
			}

			// ... SELECT block for each valid invitee ...
			sb.append(buildInviteeSelectBlocks());

		}

		sb.append(buildSugarVersion(appType));

		// ... creating Sugar fields ...
		if (isChairCancelMeeting()) {
			sb.append("{\"name\":\"deleted\",\"value\":\"1\"}"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
		} else {
			sb.append(buildSugarCalendarNameValueListForExistingCalendar());
		}

		for (int i = 0; i < REQUEST_STRINGS_RECORD_Block_2.length; i++) {
			sb.append(REQUEST_STRINGS_RECORD_Block_2[i]);
		}

		if (!isChairCancelMeeting()) {

			_isFirst = true;

			// ... relationships for each association ...
			sb.append(buildSugarRelationships(appType));

			// ... relationships for uninvited contacts ...
			if (getUninvitedContactMap() != null && !getUninvitedContactMap().isEmpty()) {
				Iterator<AssociateData> associateDataIt = getUninvitedContactMap().values().iterator();
				while (associateDataIt.hasNext()) {
					AssociateData assodiateData = associateDataIt.next();
					sb.append(buildSugarRelationship(SugarType.CONTACTS.getParentType(), assodiateData.getId(), _isFirst, appType, "delete")); //$NON-NLS-1$ 
					_isFirst = false;
				}
			}

			sb.append(buildInviteeRelationshipsForExistingCalendar());

		}

		// ... closing ...
		for (int i = 0; i < REQUEST_STRINGS_4.length; i++) {
			sb.append(REQUEST_STRINGS_4[i]);
		}

		return sb.toString();
	}

	private String buildSugarCalendarNameValueListForExistingCalendar() {
		String request = null;
		boolean isContinue = false;

		String location = null;
		String dateStartX = null;
		String timeStartX = null;
		String dateEndX = null;
		String durationHoursX = null;
		String durationMinutesX = null;
		String appointmentType = null; /* 3=Meeting, 0=Appointment */
		String name = null;
		String description = null;
		String status = null;

		// Following are taken from WSDL:
		// added for sugar 1.0: -1 means no alert; otherwise the number of seconds prior to the start
		// But, web service would not accept "0", "-1", "00", but it takes an empty value "", so
		// use it when reminder is not turned on.
		String reminder_timeX = ""; //$NON-NLS-1$

		try {
			location = encodeString(_mailDoc.getItemValueString("Location")); //$NON-NLS-1$

			Object objStart = _mailDoc.getItemValueDateTimeArray("StartDateTime").get(0); //$NON-NLS-1$
			if (objStart != null && objStart instanceof DateTime) {
				dateStartX = dateTimeToString(objStart);
				timeStartX = ((DateTime) objStart).getTimeOnly();
			}

			Object objEnd = _mailDoc.getItemValueDateTimeArray("EndDateTime").get(0); //$NON-NLS-1$
			if (objEnd != null && objEnd instanceof DateTime) {
				dateEndX = dateTimeToString(objEnd);
			}

			int durationSeconds = ((DateTime) objEnd).timeDifference(((DateTime) objStart));
			int durationHours = durationSeconds / 3600;
			durationHoursX = String.valueOf(durationHours);

			// web service AVL likes to round up to 15 minutes
			int interval = 15;
			int durationMinutes = (durationSeconds - durationHours * 3600) / 60;

			durationMinutes = (durationMinutes + interval - 1) / interval * interval;
			durationMinutesX = String.valueOf(durationMinutes);

			appointmentType = getAppointmentType();

			name = encodeString(_mailDoc.getItemValueString("Subject")); //$NON-NLS-1$

			String body = _mailDoc.getItemValueString("Body"); //$NON-NLS-1$

			// no attachment
			// if (_duOperations.getSuccessfulResultsMap() != null && !_duOperations.getSuccessfulResultsMap().isEmpty()) {
			// body = appendAttachmentURLToBody(body);
			// }

			description = encodeString(body);

			// 47019 - as far as i can see meeting API allows only alarms 5, 10 and 15 minutes prior to meeting; other values
			// might cause AVL error... and because this is only a text field, does not trigger any notification to the user
			// so, remove it.
			// int alarmMinute = 1;
			//			String alarm = _mailDoc.getItemValueString("Alarms"); //$NON-NLS-1$
			//			if (alarm != null && alarm.equals("1")) //$NON-NLS-1$
			// {
			//				Integer alarmoffset = _mailDoc.getItemValueInteger("$AlarmOffset"); //$NON-NLS-1$
			// if (alarmoffset != null) {
			// alarmMinute = alarmoffset.intValue() * (-1);
			// }
			//
			// // web service does not like > 1 day
			// int reminder_time = Math.min(86400, alarmMinute * 60);
			//
			// // if 0, use default reminder_timeX.
			// if (reminder_time != 0) {
			// reminder_timeX = String.valueOf(reminder_time);
			// }
			//
			// }

			status = isMeetingInThePast((DateTime) objStart) ? "Held" : "planned"; //$NON-NLS-1$ //$NON-NLS-2$

			isContinue = true;
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}

		if (isContinue) {
			StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

			// if (!isFirstRecord) {
			// sb.append(ConstantStrings.COMMA);
			// }

			// 2014-04-04
			boolean isFirstField = true;
			// boolean isFirstField = _isFirstValueList;

			//buildSugarNameValueListField(sb, "assigned_user_id", SugarWebservicesOperations.getInstance().getUserCNUM(), isFirstField); //$NON-NLS-1$

			// isFirstField = false;
			//buildSugarNameValueListField(sb, "created_by", NotesAccountManager.getInstance().getCRMUser(), isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "location", location, isFirstField); //$NON-NLS-1$
			isFirstField = false;
			buildSugarNameValueListField(sb, "name", name, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "date_start", dateStartX, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "time_start", timeStartX, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "date_end", dateEndX, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "description", description, isFirstField); //$NON-NLS-1$
			//buildSugarNameValueListField(sb, "parent_type", type, isFirstField); //$NON-NLS-1$
			//buildSugarNameValueListField(sb, "parent_id", id, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "duration_hours", durationHoursX, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "duration_minutes", durationMinutesX, isFirstField); //$NON-NLS-1$
			buildSugarNameValueListField(sb, "status", status, isFirstField); //$NON-NLS-1$  
			//buildSugarNameValueListField(sb, "direction", "Outbound", isFirstField); //$NON-NLS-1$ //$NON-NLS-2$
			//buildSugarNameValueListField(sb, "team_id", "1", isFirstField); //$NON-NLS-1$ //$NON-NLS-2$

			buildSugarNameValueListField(sb, "reminder_time", reminder_timeX, isFirstField); //$NON-NLS-1$ 
			buildSugarNameValueListField(sb, "email_reminder_time", reminder_timeX, isFirstField); //$NON-NLS-1$ 

			request = sb.toString();
		}
		return request;
	}

	private void extractCalendarInviteesForExistingCalendar() {
		// logItems("From AssociateUpdatemanager.extractCalendarInvitees .........", _mailDoc);  //$NON-NLS-1$ 

		// extract updated invitees list
		List<String> requiredList = MailDocumentSelectionAction.getInstance().getNotesInvitees(_mailDoc, MailDocumentSelectionAction.REQUIRED_INVITEES_TYPE);
		List<String> optionalList = MailDocumentSelectionAction.getInstance().getNotesInvitees(_mailDoc, MailDocumentSelectionAction.OPTIONAL_INVITEES_TYPE);
		List<String> requiredList_cached = null;
		List<String> optionalList_cached = null;

		// get orig. invitees list so we can compare to see who was added to the invitee list
		requiredList_cached = getRequiredInviteesList();
		optionalList_cached = getOptionalInviteesList();

		// printList("current Required list", requiredList);
		// printList("orig Required list", requiredList_cached);
		// printList("current Optional list", optionalList);
		// printList("orig Optional list", optionalList_cached);

		// create new required bucket
		if (requiredList != null && !requiredList.isEmpty()) {
			for (int i = 0; i < requiredList.size(); i++) {
				String required = requiredList.get(i);
				// if entry does not exist in the cache => newly added required
				if (required != null && !required.equals(ConstantStrings.PERIOD) && (requiredList_cached == null || !requiredList_cached.contains(required))
						&& (getNewRequiredInviteeList() == null || !getNewRequiredInviteeList().contains(required))) {
					getNewRequiredInviteeList().add(required);
				}
			}
		} else {
			getNewRequiredInviteeList().clear();
		}

		// create new optional bucket
		if (optionalList != null && !optionalList.isEmpty()) {
			for (int i = 0; i < optionalList.size(); i++) {
				String optional = optionalList.get(i);
				// if entry does not exist in the cache ==> newly added optional
				if (optional != null && !optional.equals(ConstantStrings.PERIOD) && (optionalList_cached == null || !optionalList_cached.contains(optional))
						&& (getNewOptionalInviteeList() == null || !getNewOptionalInviteeList().contains(optional))) {
					getNewOptionalInviteeList().add(optional);
				}
			}
		} else {
			getNewOptionalInviteeList().clear();
		}

		// create uninvited user list
		try {
			Vector tempV = _mailDoc.getItemValue(MailDocumentSelectionAction.UNINVITED);
			if (tempV == null || tempV.isEmpty()) {
				// clear up the list
				getUninvitedInviteeList().clear();
			} else {

				for (int i = 0; i < tempV.size(); i++) {
					if (tempV.get(i) != null && (getUninvitedInviteeList() == null || !getUninvitedInviteeList().contains((String) tempV.get(i)))) {
						getUninvitedInviteeList().add((String) tempV.get(i));
					}
				}
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}

		printList("\nAssoicateUpdateManager... for existing meeting, newREQUIRED", getNewRequiredInviteeList()); //$NON-NLS-1$
		printList("\nAssoicateUpdateManager... for existing meeting, newOPTIONAL", getNewOptionalInviteeList()); //$NON-NLS-1$
		printList("\nAssoicateUpdateManager... for existing meeting, uninvided", getUninvitedInviteeList()); //$NON-NLS-1$

	}

	// loop through response documents, looking for documents with "NoticeType" and "INVITEENAME" items which indicates this document
	// contains this invitee's status.
	private void extractCalendarInviteeStatus() {
		Map<String, String> statusMap = new HashMap<String, String>();
		try {
			DocumentCollection dc = _mailDoc.getResponses();
			if (dc != null && dc.getCount() > 0) {
				Document caldoc = dc.getFirstDocument();
				while (caldoc != null) {
					if (caldoc.hasItem("NoticeType")) { //$NON-NLS-1$ 
						String nameX = null;
						String statusX = getNoticeType(caldoc);

						if (statusX != null && caldoc.hasItem("INVITEENAME")) { //$NON-NLS-1$ 
							nameX = caldoc.getItemValueString("INVITEENAME"); //$NON-NLS-1$ 
						}
						if (statusX != null && nameX != null) {

							if (nameX != null && statusX != null) {
								statusMap.put(nameX, statusX);
							}
						}

					}
					// recycle() the Document object while looping a document collection
					Document tempDoc = dc.getNextDocument(caldoc);
					caldoc.recycle();
					caldoc = tempDoc;

				}

			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}

		setInviteeStatusMap(statusMap);

		printStringMap("\nAssoicateUpdateManager... Invitee Status Map", getInviteeStatusMap()); //$NON-NLS-1$
	}

	private String getNoticeType(Document caldoc) {
		String statusX = null;
		try {
			String status = caldoc.getItemValueString("NoticeType"); //$NON-NLS-1$ 
			UiUtils.log("\nAssoicateUpdateManager... getNoticeType(), status=" + status); //$NON-NLS-1$

			if (status != null
					&& (status.equalsIgnoreCase("a") || status.equalsIgnoreCase("r") || status.equalsIgnoreCase("t") || status.equalsIgnoreCase("d") || status.equalsIgnoreCase("p") || status.equalsIgnoreCase("i"))) { //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$  //$NON-NLS-4$ 

				if (status.equalsIgnoreCase("a")) { //$NON-NLS-1$ 
					statusX = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ASSOCIATE_MEETING_INVITEE_STATUS_ACCEPT);
				} else if (status.equalsIgnoreCase("r")) { //$NON-NLS-1$ 

					statusX = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ASSOCIATE_MEETING_INVITEE_STATUS_DECLINED);

				} else if (status.equalsIgnoreCase("t")) { //$NON-NLS-1$ 
					statusX = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ASSOCIATE_MEETING_INVITEE_STATUS_COUNTEROFFER);
				} else if (status.equalsIgnoreCase("d")) { //$NON-NLS-1$ 
					statusX = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ASSOCIATE_MEETING_INVITEE_STATUS_DELEGATE);
				} else if (status.equalsIgnoreCase("p")) { //$NON-NLS-1$ 
					statusX = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ASSOCIATE_MEETING_INVITEE_STATUS_TENTATIVEACCEPT);
				} else if (status.equalsIgnoreCase("i")) { //$NON-NLS-1$ 
					statusX = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ASSOCIATE_MEETING_INVITEE_STATUS_INVITED);
				}
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}
		return statusX;
	}
	private List<String> getNewRequiredInviteeList() {
		if (_requiredList_new == null) {
			_requiredList_new = new ArrayList<String>();
		}
		return _requiredList_new;
	}
	private List<String> getNewOptionalInviteeList() {
		if (_optionalList_new == null) {
			_optionalList_new = new ArrayList<String>();
		}
		return _optionalList_new;
	}
	private List<String> getUninvitedInviteeList() {
		if (_uninviteedInviteeList == null) {
			_uninviteedInviteeList = new ArrayList<String>();
		}
		return _uninviteedInviteeList;
	}

	private List<String> getNotesInviteeListForExistingCalendar() {

		if (!isChairCancelMeeting()) {
			// build new invitees and uninvited invitees buckets.
			extractCalendarInviteesForExistingCalendar();
			// In addition, build a invitee status bucket.
			extractCalendarInviteeStatus();
		}

		// merge new requried invitees
		List<String> list = new ArrayList<String>();
		if (getNewRequiredInviteeList() == null || getNewRequiredInviteeList().isEmpty()) {
		} else {
			list.addAll(getNewRequiredInviteeList());
		}

		// merge new optional invitees
		if (getNewOptionalInviteeList() == null || getNewOptionalInviteeList().isEmpty()) {
		} else {
			if (list.isEmpty()) {
				list.addAll(getNewOptionalInviteeList());
			} else {
				for (int i = 0; i < getNewOptionalInviteeList().size(); i++) {
					if (!list.contains(getNewOptionalInviteeList().get(i))) {
						list.add(getNewOptionalInviteeList().get(i));
					}
				}
			}
		}

		// merge uninvited invitees
		if (getUninvitedInviteeList() == null || getUninvitedInviteeList().isEmpty()) {
		} else {

			if (list.isEmpty()) {
				list.addAll(getUninvitedInviteeList());
			} else {
				for (int i = 0; i < getUninvitedInviteeList().size(); i++) {
					if (!list.contains(getUninvitedInviteeList().get(i))) {
						list.add(getUninvitedInviteeList().get(i));
					}
				}
			}
		}

		// merge invitee status person
		if (getInviteeStatusMap() == null || getInviteeStatusMap().isEmpty()) {
		} else {
			Iterator<String> it = getInviteeStatusMap().keySet().iterator();
			while (it.hasNext()) {
				String name = it.next();
				if (!list.contains(name)) {
					list.add(name);
				}
			}

		}

		// 48249 - if I am not the chair, and if NoticeType field exists, add me to the invitee list and
		// add NoticeType field as the invitee status. (i..e I accepted/declined/delegated the inviation)
		try {
			if (!amIChair(_mailDoc.getItemValue("CHAIR")) && _mailDoc.hasItem("NoticeType")) { //$NON-NLS-1$ 
				String statusX = getNoticeType(_mailDoc);

				String bookFreeTime = getNotesItemString(_mailDoc, "BookFreeTime");
				if (statusX == null) {
					// 48248
					// this is an "update" document (i.e. a reschedule notification document) which does not have
					// my status... can I find the parent document?
					statusX = getNoticeType(getParentDocument());
					bookFreeTime = getNotesItemString(getParentDocument(), "BookFreeTime");
				}
				// 48249
				// if noticetype is "A" but bookfreetime is "1", this is actually a tentative accepted meeting.
				if (statusX != null && statusX.equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ASSOCIATE_MEETING_INVITEE_STATUS_ACCEPT)) && bookFreeTime != null
						&& bookFreeTime.equalsIgnoreCase("1")) {
					statusX = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ASSOCIATE_MEETING_INVITEE_STATUS_TENTATIVEACCEPT);
				}

				if (statusX != null) {

					if (!list.contains(NotesAccountManager.getInstance().getCRMUser())) {
						list.add(NotesAccountManager.getInstance().getCRMUser());
					}
					UiUtils.log("\nAssoicateUpdateManager... for existing meeting, adding me to invitee list: " + NotesAccountManager.getInstance().getCRMUser()); //$NON-NLS-1$

					// add "accepted" as chair status
					getInviteeStatusMap().put(NotesAccountManager.getInstance().getCRMUser(), statusX);

					UiUtils.log("\nAssoicateUpdateManager... for existing meeting, adding status for me : " + statusX); //$NON-NLS-1$

				}
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}

		return list;
	}

	private String buildInviteeRelationshipsForExistingCalendar() {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

		if (getSugarInviteeMap() != null) { // if there are valid sugar users
			// loop through new required invitee bucket, if any of them is valid sugar email, create the relationship
			if (getNewRequiredInviteeList() != null && !getNewRequiredInviteeList().isEmpty()) {
				Iterator<String> it = getNewRequiredInviteeList().iterator();
				while (it.hasNext()) {
					String name = it.next();
					String email = getEmailFromSugarInviteeMap(name);
					if (email != null) {
						sb.append(buildInviteeRelationship(email, "create", null, _isFirst)); //$NON-NLS-1$
						_isFirst = false;
					}
				}
			}
			// loop through new optional invitee bucket, if any of them is valid sugar email, create the relationship
			if (getNewOptionalInviteeList() != null && !getNewOptionalInviteeList().isEmpty()) {
				Iterator<String> it = getNewOptionalInviteeList().iterator();
				while (it.hasNext()) {
					String name = it.next();
					String email = getEmailFromSugarInviteeMap(name);
					if (email != null) {
						sb.append(buildInviteeRelationship(email, "create", null, _isFirst)); //$NON-NLS-1$
						_isFirst = false;
					}
				}
			}
			// loop through uninvited bucket, if any of them is valid sugar email, create the relationship
			if (getUninvitedInviteeList() != null && !getUninvitedInviteeList().isEmpty()) {
				Iterator<String> it = getUninvitedInviteeList().iterator();
				while (it.hasNext()) {
					String name = it.next();
					String email = getEmailFromSugarInviteeMap(name);
					if (email != null) {
						sb.append(buildInviteeRelationship(email, "delete", null, _isFirst)); //$NON-NLS-1$
						_isFirst = false;
					}
				}
			}

			// loop through invitee status bucket, if any of them is valid sugar email, and it's not in the uninvited
			// bucket, create the relationship
			if (getInviteeStatusMap() != null && !getInviteeStatusMap().isEmpty()) {
				Iterator<Entry<String, String>> it = getInviteeStatusMap().entrySet().iterator();
				while (it.hasNext()) {
					Entry<String, String> entry = it.next();
					String name = entry.getKey();
					String status = entry.getValue();
					if (!isUninvitedInvitee(name)) {
						// 48249 - if i am not the chair, and NoticeType exists, we need to create relationship attr for my status
						String email = null;
						if (name != null && name.equalsIgnoreCase(NotesAccountManager.getInstance().getCRMUser())) {
							email = name;
						} else {
							email = getEmailFromSugarInviteeMap(name);
						}
						sb.append(buildInviteeRelationship(email, "create", status, _isFirst)); //$NON-NLS-1$
						_isFirst = false;
					}
				}
			}

		}

		return sb.toString();
	}

	private boolean isUninvitedInvitee(String name) {
		boolean isUninvited = false;
		if (name == null || getUninvitedInviteeList() == null || getUninvitedInviteeList().isEmpty()) {
		} else {

			Iterator<String> it = getUninvitedInviteeList().iterator();
			while (it.hasNext()) {
				String uninvitedInviteeName = it.next();

				if (isMatchWithoutDomain(name, uninvitedInviteeName)) {
					isUninvited = true;
					break;
				}
			}
		}
		return isUninvited;

	}
	private boolean isMatchWithoutDomain(String name1, String name2) {
		boolean isMatch = false;
		if (name1 != null && name2 != null) {
			String name1NoDomain = stripDomain(name1);
			String name2NoDomain = stripDomain(name2);
			if ((name1NoDomain.length() == name2NoDomain.length() && name1NoDomain.equalsIgnoreCase(name2NoDomain))
					|| (name1NoDomain.length() > name2NoDomain.length() && name1NoDomain.toLowerCase().startsWith(name2NoDomain.toLowerCase()))
					|| (name1NoDomain.length() < name2NoDomain.length() && name2NoDomain.toLowerCase().startsWith(name1NoDomain.toLowerCase()))) {
				isMatch = true;
			}
		}
		return isMatch;
	}

	private String getEmailFromSugarInviteeMap(String nameInNotesFormat) {
		String emailX = null;
		if (nameInNotesFormat != null && getSugarInviteeMap() != null && !getSugarInviteeMap().isEmpty()) {
			System.out.println("aaa");
			Iterator<Entry<String, String>> it = getSugarInviteeMap().entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, String> entry = it.next();
				String email = entry.getKey();
				String inviteeNameInNotesFormat = entry.getValue();
				// if user does not have notes id, compare email instead
				if ((inviteeNameInNotesFormat != null && inviteeNameInNotesFormat.equals(ConstantStrings.EMPTY_STRING) && email != null && email.equalsIgnoreCase(nameInNotesFormat))
						|| (inviteeNameInNotesFormat != null && !inviteeNameInNotesFormat.equals(ConstantStrings.EMPTY_STRING) && isMatchWithoutDomain(nameInNotesFormat, inviteeNameInNotesFormat))) {
					emailX = email;
					break;
				}
			}

		}
		return emailX;
	}

	/*
	 * The key of inviteeStatusMap is name in various Notes ID format, it could be name with @notes domain, @ibm domain or without domain... check substring to find the match.
	 */
	private String getStatusFromInviteeStatusMap(String nameInNotesFormat) {
		String statusX = null;
		if (nameInNotesFormat != null && getInviteeStatusMap() != null && !getInviteeStatusMap().isEmpty()) {

			Iterator<Entry<String, String>> it = getInviteeStatusMap().entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, String> entry = it.next();
				String name = entry.getKey();
				String status = entry.getValue();
				if (isMatchWithoutDomain(nameInNotesFormat, name)) {
					statusX = status;
					break;
				}
			}
		}
		return statusX;
	}

	private String buildInviteeRelationship(String email, String action, String status, boolean isFirst) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (email != null && action != null) {
			if (!isFirst) {
				sb.append(ConstantStrings.COMMA);
			}

			if (status == null) {
				sb.append("{\"name\":\"users").append("\",\"action\":\"").append(action).append("\",\"related_id\":\"") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						.append(email).append("\"}"); //$NON-NLS-1$
			} else {
				sb.append("{\"name\":\"users").append("\",\"action\":\"").append(action).append("\",\"related_id\":\"") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						.append(email).append("\", \"relationship_attributes\": [{\"name\":\"accept_status\",\"value\":\"" + status + "\"}]}"); //$NON-NLS-1$  //$NON-NLS-2$
			}
		}
		return sb.toString();
	}

	private List<String> getRequiredInviteesList() {
		List<String> requriedInviteesList = null;
		if (NotesUIDocumentSaveListenerRepository.getDocumentActionMap() != null) {
			SugarEntrySurrogate sugarEntry = (SugarEntrySurrogate) NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(_mapkey);
			if (sugarEntry != null) {
				requriedInviteesList = sugarEntry.getRequiredInvitees();
			}

		}
		return requriedInviteesList;
	}

	private List<String> getOptionalInviteesList() {
		List<String> optionalInviteesList = null;
		if (NotesUIDocumentSaveListenerRepository.getDocumentActionMap() != null) {
			SugarEntrySurrogate sugarEntry = (SugarEntrySurrogate) NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(_mapkey);
			if (sugarEntry != null) {
				optionalInviteesList = sugarEntry.getOptionalInvitees();
			}

		}
		return optionalInviteesList;
	}

	private void setInviteeStatusMap(Map<String, String> map) {
		_inviteeStatusMap = map;
	}

	private Map<String, String> getInviteeStatusMap() {
		return _inviteeStatusMap;
	}
	private boolean isMeetingInThePast(DateTime objStart) {
		boolean isPast = false;
		try {
			if (objStart != null) {
				Date meetingDate = objStart.toJavaDate();
				Date now = new Date();
				if (now.after(meetingDate)) {
					isPast = true;
				}
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}
		return isPast;
	}

	private void buildInviteeContactList() {
		boolean isUpdated = false;
		AssociateDataMap associateDataMap = null;
		if (getNotesInvitees() != null && !getNotesInvitees().isEmpty()) {
			_inviteeContactMap = performContactsGetEntryListWebService(SugarWebservicesOperations.getInstance().buildModuleEmailArguments(getNotesInvitees(), "Contacts", "contacts.id")); //$NON-NLS-1$ //$NON-NLS-2$
			if (getInviteeContactMap() != null && !getInviteeContactMap().isEmpty()) {
				associateDataMap = ConnectorUtil.decode(getAssociateDataMapXML());
				List<String> idList = associateDataMap.getIdList(SugarType.CONTACTS.getParentType());

				Iterator<Map.Entry<String, AssociateData>> inviteeContactEntryIt = getInviteeContactMap().entrySet().iterator();
				while (inviteeContactEntryIt.hasNext()) {
					Map.Entry<String, AssociateData> entry = inviteeContactEntryIt.next();
					String email = entry.getKey();
					AssociateData inviteeContact = entry.getValue();
					if (getAssignedSugarId() == null) {
						// for new calendar, add the contact to associateDataMap
						if (idList == null || idList.isEmpty() || !idList.contains(inviteeContact.getId())) {
							associateDataMap.addAssociateData(SugarType.CONTACTS.getParentType(), inviteeContact);
							isUpdated = true;
						}
					} else {
						// for calendar update, if this contact is in the new required/optional list, add it to associateDataMap
						// otherwise, if this contact is in the uninvited list, remove it from associateDataMap, and build _uninvited contact map
						if (idList == null || idList.isEmpty() || ((isContactInNewInviteeList(email) && !idList.contains(inviteeContact.getId())))) {
							associateDataMap.addAssociateData(SugarType.CONTACTS.getParentType(), inviteeContact);
							isUpdated = true;
						} else if (idList != null && !idList.isEmpty() && idList.contains(inviteeContact.getId()) && isContactInUninvitedInviteeList(email)) {
							associateDataMap.removeAssociateData(SugarType.CONTACTS.getParentType(), inviteeContact, true, true, true);
							getUninvitedContactMap().put(email, inviteeContact);
							isUpdated = true;
						}
					}
				}
			}
		}
		if (isUpdated) {
			_associateDataMapForSugarUpdate = associateDataMap.getSubset(false);
			_bkupAssociateDataMap = setBackupAssociateDataMap(associateDataMap);
			String out = ConnectorUtil.encode(associateDataMap);
			updateAssociateDataMap(out);

		}
	}
	private boolean isContactInNewInviteeList(String id) {
		boolean isInList = false;
		if (id != null && ((getNewRequiredInviteeList() != null && getNewRequiredInviteeList().contains(id)) || (getNewOptionalInviteeList() != null && getNewOptionalInviteeList().contains(id)))) {
			isInList = true;
		}
		return isInList;
	}
	private boolean isContactInUninvitedInviteeList(String email) {
		boolean isInList = false;

		// 49152
		if (email != null && getUninvitedInviteeList() != null && isContains(getUninvitedInviteeList(), email)) {

			isInList = true;
		}
		return isInList;
	}

	private boolean isContains(List<String> list, String s) {
		boolean isContains = false;
		if (list != null && !list.isEmpty() && s != null) {
			Iterator<String> it = list.iterator();
			while (it.hasNext()) {
				String listX = it.next();
				if (listX != null && listX.equalsIgnoreCase(s)) {
					isContains = true;
					break;
				}
			}
		}
		return isContains;
	}

	private Map<String, AssociateData> getInviteeContactMap() {
		return _inviteeContactMap;
	}

	private void buildNotesInvitees() {
		if (getAssignedSugarId() == null) {
			_notesInvitees = getNotesInviteeList();
		} else {
			// merge new required invitee list, new optional invitee list, uninvited list and invitee status list into
			// _notesInvitees List
			_notesInvitees = getNotesInviteeListForExistingCalendar();
		}
	}

	private List<String> getNotesInvitees() {
		return _notesInvitees;
	}

	private Map<String, AssociateData> getUninvitedContactMap() {
		if (_uninvitedContactMap == null) {
			_uninvitedContactMap = new HashMap<String, AssociateData>();
		}
		return _uninvitedContactMap;
	}

	private Map<String, AssociateData> performContactsGetEntryListWebService(String arguments) {
		Map<String, AssociateData> map = null;
		if (arguments != null && !arguments.equals(ConstantStrings.EMPTY_STRING)) {

			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("method", "get_entry_list"); //$NON-NLS-1$ //$NON-NLS-2$
			parameters.put("arguments", arguments); //$NON-NLS-1$

			String output = SugarWebservicesOperations.getInstance().getSugarInfoFromWebService(parameters);

			UiUtils.webServicesLog("sugarValidEmails", null, output); //$NON-NLS-1$

			try {
				map = extractContactsJSONOutput(output);
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);

			}

		}
		return map;
	}

	private Map<String, AssociateData> extractContactsJSONOutput(String output) {
		Map<String, AssociateData> map = null;
		if (output != null) {
			try {

				final JSONObject searchResultsJSON = new JSONObject(output);

				if (searchResultsJSON.containsKey("entry_list")) //$NON-NLS-1$
				{

					JSONArray resultsArray = searchResultsJSON.getJSONArray("entry_list"); //$NON-NLS-1$

					for (int i = 0; i < resultsArray.length(); i++) {
						JSONObject entrylistObject = (JSONObject) resultsArray.get(i);
						JSONObject namevaluelistObject = entrylistObject.getJSONObject("name_value_list"); //$NON-NLS-1$

						String email = namevaluelistObject.getJSONObject("email1").getString("value");//$NON-NLS-1$ //$NON-NLS-2$
						String id = namevaluelistObject.getJSONObject("id").getString("value"); //$NON-NLS-1$ //$NON-NLS-2$
						String name = namevaluelistObject.getJSONObject("name").getString("value"); //$NON-NLS-1$ //$NON-NLS-2$

						if (email != null) {
							if (map == null) {
								map = new HashMap<String, AssociateData>();
							}
							if (!map.containsKey(email)) {
								map.put(email, new AssociateData(name, null, id, false));
							}
						}
					}
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}
		}
		return map;
	}
	private void print(String s, Vector v) {
		if (v == null || v.isEmpty()) {
			System.out.println(s + " is NULL"); //$NON-NLS-1$
		} else {
			for (int i = 0; i < v.size(); i++) {
				System.out.println(i + ". " + s + ":" + v.get(i)); //$NON-NLS-1$  //$NON-NLS-2$
			}
		}

	}

	private void logItems(String s, Document doc) {
		List<String> list = new ArrayList<String>();
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		try {
			if (s == null) {
				list.add("\n--- No Title ...\n"); //$NON-NLS-1$ 
			} else {
				list.add(s);
			}
			if (doc != null && doc.getItems() != null) {
				Vector v = doc.getItems();
				for (int i = 0; i < v.size(); i++) {

					Item item = (Item) v.get(i);
					Vector vc = item.getValues();
					if (vc != null) {
						for (int ii = 0; ii < vc.size(); ii++) {
							sb.setLength(0);
							sb.append(ii).append(". item: ").append(item.getName()).append(",  value: ").append(vc.get(ii)); //$NON-NLS-1$ //$NON-NLS-2$
							list.add(sb.toString());
						}
					} else {
						sb.setLength(0);
						sb.append(i).append(". item: ").append(item.getName()).append(",  value: NULL"); //$NON-NLS-1$ //$NON-NLS-2$
						list.add(sb.toString());

					}
				}
			} else {
				list.add("Document or Document items are null"); //$NON-NLS-1$ 
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}

		// test it
		// printList(s, list);

		UtilsPlugin.getDefault().logInfoMessage(list.toArray(new String[list.size()]), Activator.PLUGIN_ID);

	}

	private void printList(String s, List<String> l) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		sb.append(ConstantStrings.NEW_LINE).append(s);
		if (l == null || l.isEmpty()) {
			sb.append(ConstantStrings.NEW_LINE).append("List is NULL"); //$NON-NLS-1$  
		} else {
			for (int i = 0; i < l.size(); i++) {
				sb.append(ConstantStrings.NEW_LINE).append(l.get(i));
			}
		}
		UiUtils.log(sb.toString());
		// System.out.println(sb.toString());
	}
	private void printStringMap(String s, Map<String, String> m) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		sb.append(ConstantStrings.NEW_LINE).append(s);
		if (m == null || m.isEmpty()) {
			sb.append(ConstantStrings.NEW_LINE).append("Map is NULL"); //$NON-NLS-1$ 
		} else {
			Iterator<Entry<String, String>> it = m.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, String> entry = it.next();
				sb.append(ConstantStrings.NEW_LINE).append("key:").append(entry.getKey()).append(",value:").append(entry.getValue()); //$NON-NLS-1$  //$NON-NLS-2$ 
			}
		}
		UiUtils.log(sb.toString());
		// System.out.println(sb.toString());
	}

}
