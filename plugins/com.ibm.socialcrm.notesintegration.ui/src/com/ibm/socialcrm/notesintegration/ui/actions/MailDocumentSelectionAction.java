package com.ibm.socialcrm.notesintegration.ui.actions;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.EmbeddedObject;
import lotus.domino.Item;
import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.NotesThread;
import lotus.domino.RichTextItem;
import lotus.domino.Session;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.ibm.csi.types.DocumentSummary;
import com.ibm.notes.java.api.data.NotesDatabaseData;
import com.ibm.notes.java.api.data.NotesDocumentData;
import com.ibm.notes.java.ui.NotesUIWorkspace;
import com.ibm.notes.java.ui.documents.NotesUIDocument;
import com.ibm.notes.java.ui.documents.NotesUIField;
import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.MailDatabaseInfo;
import com.ibm.socialcrm.notesintegration.core.SugarEntrySurrogate;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.core.utils.UpdateSelectionsBroadcaster;
import com.ibm.socialcrm.notesintegration.sugarwidgetupdate.SugarWidgetUpdateActivator;
import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.workplace.noteswc.selection.NotesCaretSelection;
import com.ibm.workplace.noteswc.selection.NotesFileCaretSelection;
import com.ibm.workplace.noteswc.swt.NotesSashControl;

public class MailDocumentSelectionAction implements IWorkbenchWindowActionDelegate {
	private static final String BODY = "Body"; //$NON-NLS-1$
	private static final String SUBJECT = "Subject"; //$NON-NLS-1$
	private static final String FORM = "form"; //$NON-NLS-1$
	private static final String UIDOC_SEND_TO = "EnterSendTo"; //$NON-NLS-1$
	private static final String UIDOC_COPY_TO = "EnterCopyTo"; //$NON-NLS-1$
	private static final String DOC_SEND_TO = "SendTo"; //$NON-NLS-1$
	private static final String DOC_COPY_TO = "CopyTo"; //$NON-NLS-1$
	private static final String REQUIREDATTENDEES = "REQUIREDATTENDEES"; //$NON-NLS-1$
	private static final String OPTIONALATTENDEES = "OPTIONALATTENDEES"; //$NON-NLS-1$
	public static final String REQUIRED_INVITEES_TYPE = "r"; //$NON-NLS-1$
	public static final String OPTIONAL_INVITEES_TYPE = "o"; //$NON-NLS-1$
	public static final String ALTREQUIREDNAMES = "AltRequiredNames"; //$NON-NLS-1$
	public static final String ALTOPTIONALNAMES = "AltOptionalNames"; //$NON-NLS-1$
	public static final String UNINVITED = "Uninvited"; //$NON-NLS-1$

	public static final String CRM_ASSOCIATE = "CRMAssociate"; //$NON-NLS-1$

	public static final int MAX_NAME_LIST = 100;

	/**
	 * This cache holds a map of UNIDs to the associated sugar data map. Storing this information means we don't have to reload it from LotusLive everytime the user switches emails.
	 */
	private static Map<String, Map<SugarType, Set<SugarEntrySurrogate>>> sugarDataCache;
	private static Set<String> cachedDocuments;

	private static Map<String, tempNotesInfo> subjectCache;

	/**
	 * The sugar data map for the current selection
	 */
	private static Map<SugarType, Set<SugarEntrySurrogate>> currentSugarDataMap;

	// TODO: We don't want to create a session and connect to the DB on every selection change as some
	// selection change events represent selection changes within the same mail document. We can create
	// a session pool to solve the 1st part of the problem.

	/**
	 * The unid of the current selection
	 */
	private static String currentUnid = ConstantStrings.EMPTY_STRING;

	/**
	 * The last text value selected that was searched for.
	 */
	private static String lastTextSelection = ConstantStrings.EMPTY_STRING;

	private static MailDocumentSelectionAction instance = null;
	Listener modifiedListener;

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// use array so we can pass the value to a Job
		final String[] selectionTitle = new String[]{ConstantStrings.EMPTY_STRING};
		final String[] form = new String[]{ConstantStrings.EMPTY_STRING};

		String unid = null;
		String databaseReplicaId = null;

		boolean updateOnly = false;

		// Sometimes when switching to an open document tab, the selection is an instance of
		// NotesFileCaretSelection. Not really sure why, adding the scenario here so the selection
		// listener can receive correct selection event.
		if (selection instanceof StructuredSelection || selection instanceof NotesFileCaretSelection) {
			Object obj = null;
			if (selection instanceof StructuredSelection) {
				obj = ((StructuredSelection) selection).getFirstElement();
			}
			if ((selection instanceof StructuredSelection && obj != null) || (selection instanceof NotesFileCaretSelection)) {
				if (selection instanceof StructuredSelection) {
					unid = ConstantStrings.EMPTY_STRING;

					if (obj instanceof NotesUIDocument) {
						// If a document is opened we will get here.
						final NotesUIDocument notesUiDocument = (NotesUIDocument) obj;
						selectionTitle[0] = notesUiDocument.getTitle();
						form[0] = notesUiDocument.getForm();

						// Occasionally we get a rogue document come through here and get document data will choke if the url doesn't begin with Notes://.
						if (notesUiDocument.getUrl() != null && notesUiDocument.getUrl().startsWith("Notes://")) //$NON-NLS-1$
						{
							final NotesDocumentData notesDocumentData = notesUiDocument.getDocumentData();
							NotesDatabaseData notesDatabaseData = notesUiDocument.getDatabaseData();
							if (notesDocumentData != null) {
								unid = notesDocumentData.getUnid();
							}
							if (notesDatabaseData != null) {
								databaseReplicaId = notesDatabaseData.getReplicaId();
							}
							// it seems NotesUiDocument class does not have equals method defined, so
							// contains(NotesUiDocument) does not work correctly, the if (!getCachedDocuments().contains(notesUiDocument))
							// always returns true and it causesmultiple closelistener created for a given NotesUiDocument.
							// Changing entry of the CachedDocument from NotesUiDocument to NotesDocumentData.getUnid()
							// 
							// if (!getCachedDocuments().contains(notesUiDocument)) {
							// getCachedDocuments().add(notesUiDocument);
							if (!getCachedDocuments().contains(notesDocumentData.getUnid())) {
								getCachedDocuments().add(notesDocumentData.getUnid());
								notesUiDocument.addCloseListener(new Listener() {
									/**
									 * Clear the cached data when the email is closed.
									 */
									public void handleEvent(Event evt) {
										if (notesDocumentData != null) {
											clearUpCache(notesDocumentData.getUnid());
											notesUiDocument.removeModifiedListener(modifiedListener);
										}
									}
								});

								modifiedListener = new Listener() {
									public void handleEvent(Event event) {

										try {
											// if it's calendar, add the entry to isModified list
											if (notesUiDocument != null && notesUiDocument.getForm() != null && notesUiDocument.getForm().equalsIgnoreCase("appointment")) {
												String unid = null;
												if (notesUiDocument.getDocumentData() == null || notesUiDocument.getDocumentData().getUnid() == null) {
													if (event.widget instanceof NotesSashControl && ((NotesSashControl) event.widget).getDocument() != null
															&& ((NotesSashControl) event.widget).getDocument().getUnid() != null) {

														unid = ((NotesSashControl) event.widget).getDocument().getUnid();
													}
												} else {
													unid = notesUiDocument.getDocumentData().getUnid();
												}
												if (unid != null) {
													UiUtils.log("MailDocumentSelectionAction.HandleEvent for doc modified listener: setModified to true");
													setIsModified(unid, true);
												}

											}

										} catch (Exception e) {
											UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
										}

									}
								};

								notesUiDocument.addModifiedListener(modifiedListener);

							}
						}
					} else if (obj instanceof DocumentSummary) {
						// Documents we may have missed. For example if we are in preview mode.
						Pattern notesUrlPattern = Pattern.compile("notes://([^/]*?)/([^/]*?)/", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$
						DocumentSummary notesDocumentSummary = (DocumentSummary) obj;
						String url = notesDocumentSummary.getUrl();
						if (url != null) {
							Matcher patternMatcher = notesUrlPattern.matcher(url);
							if (patternMatcher.find() && patternMatcher.groupCount() == 2) {
								databaseReplicaId = patternMatcher.group(2);
							}
						}

						String id = ((DocumentSummary) obj).getDocumentKey().getUniqueId();

						if (id != null && !id.equals(ConstantStrings.EMPTY_STRING)) {
							String[] groups = id.split(ConstantStrings.COLON);
							if (groups == null || groups.length < 2) {
								unid = id;
							} else {
								unid = groups[1];
							}
						}
					}
				} else if (selection instanceof NotesFileCaretSelection) {

					NotesDatabaseData notesDatabaseData = ((NotesFileCaretSelection) selection).getDatabaseData();
					unid = ((NotesFileCaretSelection) selection).getUnid();
					if (notesDatabaseData != null) {
						databaseReplicaId = notesDatabaseData.getReplicaId();
					}

				}

				if (databaseReplicaId != null && !databaseReplicaId.equals(MailDatabaseInfo.getInstance().getMailDbReplicaId())) {
					String[] msgs = new String[]{
							"MailDocumentSelectionAction: The following two values should be the same if this is the mail database: \ndatabaseReplicaId=", databaseReplicaId, ", \nMailDatabaseInfo.getInstance().getMailDbReplicaId()=", MailDatabaseInfo.getInstance().getMailDbReplicaId()}; //$NON-NLS-1$  //$NON-NLS-2$ 
					UtilsPlugin.getDefault().logInfoMessage(msgs, UiPluginActivator.PLUGIN_ID);
				}
				Map<SugarType, Set<SugarEntrySurrogate>> sugarDataMap;
				if ((sugarDataMap = getSugarDataCache().get(unid)) != null) {
					updateOnly = true;

					setCurrentSugarDataMap(sugarDataMap);
					currentUnid = unid;
					broadcast(currentUnid, updateOnly);

				} else if (unid != null && databaseReplicaId != null && databaseReplicaId.equals(MailDatabaseInfo.getInstance().getMailDbReplicaId())) {

					final String tmpUnid = unid;

					// 36962 - sometimes pattern matching logic is slow, put it in a background Job to free up UI thread.
					Job buildSugarInfoJob = new Job("Building sugar information for doc: " + tmpUnid) //$NON-NLS-1$
					{

						@Override
						protected IStatus run(IProgressMonitor arg0) {
							Session session = null;
							Database mailDb = null;

							// Put a dummy entry in SugarDataCache Map for this document to avoid running the job multiple times.
							// At the end of this job, this entry in SugarDataCache map will be updated.
							Map<SugarType, Set<SugarEntrySurrogate>> tmpSugarDataMap = new HashMap<SugarType, Set<SugarEntrySurrogate>>();
							tmpSugarDataMap.put(SugarType.OPPORTUNITIES, new HashSet<SugarEntrySurrogate>());
							tmpSugarDataMap.put(SugarType.ACCOUNTS, new HashSet<SugarEntrySurrogate>());
							tmpSugarDataMap.put(SugarType.CONTACTS, new HashSet<SugarEntrySurrogate>());
							getSugarDataCache().put(tmpUnid, tmpSugarDataMap);

							try {
								NotesThread.sinitThread();
								session = NotesFactory.createSession();

								Vector vals = session.evaluate("@MailDbName"); //$NON-NLS-1$
								mailDb = session.getDatabase((String) vals.get(0), (String) vals.get(1));

								if (!mailDb.isOpen()) {
									// defect 23563 - use openWithFailover in case specified server is failed over
									// to the failover server.
									boolean isopen = mailDb.openWithFailover((String) vals.get(0), (String) vals.get(1));

									String[] msgs = new String[]{"Open database: ", (isopen ? "Successful" : "Failed"), //$NON-NLS-1$  //$NON-NLS-2$ //$NON-NLS-3$
											" ... Orig. specified server: ", (String) vals.get(0), ", dbfile: ", (String) vals.get(1), //$NON-NLS-1$ //$NON-NLS-2$
											" ... Result server: ", mailDb.getServer(), ", dbfile: ", mailDb.getFileName()}; //$NON-NLS-1$  //$NON-NLS-2$

									if (isopen) {
										UtilsPlugin.getDefault().logInfoMessage(msgs, UiPluginActivator.PLUGIN_ID);
									} else {
										UtilsPlugin.getDefault().logWarningMessage(msgs, UiPluginActivator.PLUGIN_ID);

									}

								}

								Document mailDoc = null;
								try {
									if (mailDb.isOpen()) {
										mailDoc = mailDb.getDocumentByUNID(tmpUnid);
									}
								} catch (NotesException ne) {
									// Sometimes, we catch these when the mail view is selected. It seems harmless enough.
									mailDoc = null;
								}
								if (mailDoc != null) {
									// Get the subject to set the selection title
									Vector subject = mailDoc.getItemValue(SUBJECT);
									if (subject != null && subject.size() > 0) {
										String text = (String) subject.get(0);
										if (text != null) {
											selectionTitle[0] = text;
										}
									} else {
										// Assume an empty subject
										selectionTitle[0] = ConstantStrings.EMPTY_STRING;
									}

									// Get the FORM value
									Vector formV = mailDoc.getItemValue(FORM);
									if (formV != null && formV.size() > 0) {
										String text = (String) formV.get(0);
										if (text != null) {
											form[0] = text;
										}
									} else {
										// Assume an empty form
										form[0] = ConstantStrings.EMPTY_STRING;
									}

									getSubjectCache().put(tmpUnid, new tempNotesInfo(selectionTitle[0], form[0], getNotesAssignees(mailDoc)));

									// Find sugar data in the mail (body, subject)
									tmpSugarDataMap = findSugarData(mailDoc);
									if (getSugarDataCache().containsKey(tmpUnid)) {
										getSugarDataCache().remove(tmpUnid);
									}
									getSugarDataCache().put(tmpUnid, tmpSugarDataMap);
								}

							} catch (Exception e) {
								UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
							} finally {
								try {
									if (mailDb != null) {
										mailDb.recycle();
									}
									if (session != null) {
										session.recycle();
									}
								} catch (Exception e) {
									UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
								} finally {
									NotesThread.stermThread();
								}
							}

							setCurrentSugarDataMap(tmpSugarDataMap);
							currentUnid = tmpUnid;

							Display.getDefault().asyncExec(new Runnable() {
								@Override
								public void run() {
									broadcast(currentUnid, false);
								}
							});

							return Status.OK_STATUS;
						} // run

					};
					buildSugarInfoJob.schedule();

				} else {

					setCurrentSugarDataMap(sugarDataMap);
					currentUnid = unid;
					broadcast(currentUnid, false);

				}

			}
		}
	}

	public List getNotesInvitees(Document mailDoc, String type) {
		List invitees = new ArrayList<String>();
		Vector<String> inetV = new Vector<String>();
		Vector<String> altV = new Vector<String>();
		Vector<String> tempV = new Vector<String>();
		try {
			if (mailDoc != null && type != null && type.equalsIgnoreCase(REQUIRED_INVITEES_TYPE)) {
				if (!mailDoc.hasItem(REQUIREDATTENDEES)) {
					UiUtils.log("\nMailDocumentSelectionAction.getNotesInvitees ... document does NOT have field: " + REQUIREDATTENDEES); //$NON-NLS-1$
				}
				inetV = mailDoc.getItemValue(REQUIREDATTENDEES);
				altV = mailDoc.getItemValue(ALTREQUIREDNAMES);
			} else if (mailDoc != null && type != null && type.equalsIgnoreCase(OPTIONAL_INVITEES_TYPE)) {
				if (!mailDoc.hasItem(OPTIONALATTENDEES)) {
					UiUtils.log("\nMailDocumentSelectionAction.getNotesInvitees ... document does NOT have field: " + OPTIONALATTENDEES); //$NON-NLS-1$
				}
				inetV = mailDoc.getItemValue(OPTIONALATTENDEES);
				altV = mailDoc.getItemValue(ALTOPTIONALNAMES);
			}
			if (inetV != null && inetV.size() > 0) {
				for (int i = 0; i < inetV.size(); i++) {
					if (inetV.get(i) == null || inetV.get(i).equals(ConstantStrings.PERIOD)) {
						invitees.add(altV.get(i));
					} else {
						invitees.add(inetV.get(i));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return invitees;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow arg0) {
	}

	@Override
	public void run(IAction arg0) {
	}

	private void broadcast(String unid, boolean updateOnly) {

		// Sometimes if user selects/opens/closes a document in fairly quick speed, and if the buildSugarInfoJob batch job
		// in a sluggish mode, we can get into a situation where document A is opened, but batch job for document B is
		// just finished. In this case, we want to skip broadcast for document B.
		// Checking NotesCaretSelection to avoid Notes throwing invalid protocol exception for new document
		if (getCurrDoc() != null && !(getCurrDoc() instanceof NotesCaretSelection) && getCurrDoc().getDocumentData() != null && getCurrDoc().getDocumentData().getUnid() != null
				&& !getCurrDoc().getDocumentData().getUnid().equals(unid)) {
			// System.out.println("..... skip broadcasting for ..." + unid);
			return;
		}

		if (unid != null && !unid.equals(ConstantStrings.EMPTY_STRING) && getSubjectCache().get(unid) != null) {
			UpdateSelectionsBroadcaster.getInstance().updateSelectedTitle(getSubjectCache().get(unid).getSubject());
		} else if (!updateOnly) {
			UpdateSelectionsBroadcaster.getInstance().updateSelectedTitle(null);
		}

		if (getCurrentSugarDataMap() != null) {
			UpdateSelectionsBroadcaster.getInstance().updateSelectedItems(getCurrentSugarDataMap());
			if (!updateOnly) {
				UpdateSelectionsBroadcaster.getInstance().updateOpportunities(getCurrentSugarDataMap().get(SugarType.OPPORTUNITIES));
				UpdateSelectionsBroadcaster.getInstance().updateContacts(getCurrentSugarDataMap().get(SugarType.CONTACTS));
				UpdateSelectionsBroadcaster.getInstance().updateAccounts(getCurrentSugarDataMap().get(SugarType.ACCOUNTS));
			}
		}
	}

	private NotesUIDocument getCurrDoc() {
		NotesUIDocument currDoc = null;
		NotesUIWorkspace workspace = new NotesUIWorkspace();

		if (workspace.getCurrentDocument() != null) {
			currDoc = workspace.getCurrentDocument();
		}
		return currDoc;
	}

	/**
	 * Finds all live text matches in the given document and returns the matches as a map.
	 * 
	 * @param uiDoc
	 * @return
	 */
	public Map<SugarType, Set<SugarEntrySurrogate>> findSugarData(NotesUIDocument uiDoc) {
		Map<SugarType, Set<SugarEntrySurrogate>> matches = new HashMap<SugarType, Set<SugarEntrySurrogate>>();
		matches.put(SugarType.OPPORTUNITIES, new HashSet<SugarEntrySurrogate>());
		matches.put(SugarType.ACCOUNTS, new HashSet<SugarEntrySurrogate>());
		matches.put(SugarType.CONTACTS, new HashSet<SugarEntrySurrogate>());

		Map<String, Set<String>> accountTagsMap = SugarWidgetUpdateActivator.getDefault().getAccountTagsMap();
		if (SugarWidgetUpdateActivator.getDefault().getAccountsPattern() != null) {
			matches.get(SugarType.ACCOUNTS).addAll(findMatches(getFieldValue(uiDoc, SUBJECT), SugarWidgetUpdateActivator.getDefault().getAccountsPattern(), SugarType.ACCOUNTS, accountTagsMap));
			matches.get(SugarType.ACCOUNTS).addAll(findMatches(getFieldValue(uiDoc, BODY), SugarWidgetUpdateActivator.getDefault().getAccountsPattern(), SugarType.ACCOUNTS, accountTagsMap));
		}
		Map<String, Set<String>> contactTagsMap = SugarWidgetUpdateActivator.getDefault().getContactTagsMap();
		if (SugarWidgetUpdateActivator.getDefault().getContactsPattern() != null) {
			matches.get(SugarType.CONTACTS).addAll(findMatches(getFieldValue(uiDoc, SUBJECT), SugarWidgetUpdateActivator.getDefault().getContactsPattern(), SugarType.CONTACTS, contactTagsMap));
			matches.get(SugarType.CONTACTS).addAll(findMatches(getFieldValue(uiDoc, BODY), SugarWidgetUpdateActivator.getDefault().getContactsPattern(), SugarType.CONTACTS, contactTagsMap));
		}
		Map<String, Set<String>> opportunityTagsMap = SugarWidgetUpdateActivator.getDefault().getOpportunityTagsMap();
		if (SugarWidgetUpdateActivator.getDefault().getOpportunitiesPatern() != null) {
			matches.get(SugarType.OPPORTUNITIES).addAll(
					findMatches(getFieldValue(uiDoc, SUBJECT), SugarWidgetUpdateActivator.getDefault().getOpportunitiesPatern(), SugarType.OPPORTUNITIES, opportunityTagsMap));
			matches.get(SugarType.OPPORTUNITIES).addAll(
					findMatches(getFieldValue(uiDoc, BODY), SugarWidgetUpdateActivator.getDefault().getOpportunitiesPatern(), SugarType.OPPORTUNITIES, opportunityTagsMap));
		}
		// TODO: This is the default opportunities pattern, we may want to comment this out for the real data
		// matches.get(SugarType.OPPORTUNITIES).addAll(findMatches(getFieldValue(uiDoc, SUBJECT), GenericUtils.OPPORTUNITY_PATTERN, SugarType.OPPORTUNITIES, null));
		// matches.get(SugarType.OPPORTUNITIES).addAll(findMatches(getFieldValue(uiDoc, BODY), GenericUtils.OPPORTUNITY_PATTERN, SugarType.OPPORTUNITIES, null));
		// checking for null (In case GenericUtils.OPPORTUNITY_PATTERN is still null) to skip the nullpointerexception
		Set<SugarEntrySurrogate> set = findMatches(getFieldValue(uiDoc, SUBJECT), GenericUtils.OPPORTUNITY_PATTERN, SugarType.OPPORTUNITIES, null);
		if (set != null) {
			matches.get(SugarType.OPPORTUNITIES).addAll(set);
		}
		set = findMatches(getFieldValue(uiDoc, BODY), GenericUtils.OPPORTUNITY_PATTERN, SugarType.OPPORTUNITIES, null);
		if (set != null) {
			matches.get(SugarType.OPPORTUNITIES).addAll(set);
		}

		return matches;
	}

	private static String getFieldValue(NotesUIDocument uiDoc, String fieldName) {
		String valueX = null;
		if (uiDoc != null && fieldName != null) {
			NotesUIField field = uiDoc.getField(fieldName);
			if (field != null) {
				valueX = field.getText();
			}
		}
		return valueX;
	}

	/**
	 * Finds all live text matches in the given document and returns the matches as a map.
	 * 
	 * @param notesDoc
	 * @return
	 */
	private Map<SugarType, Set<SugarEntrySurrogate>> findSugarData(Document notesDoc) {
		Map<SugarType, Set<SugarEntrySurrogate>> matches = new HashMap<SugarType, Set<SugarEntrySurrogate>>();
		matches.put(SugarType.OPPORTUNITIES, new HashSet<SugarEntrySurrogate>());
		matches.put(SugarType.ACCOUNTS, new HashSet<SugarEntrySurrogate>());
		matches.put(SugarType.CONTACTS, new HashSet<SugarEntrySurrogate>());

		Map<String, Set<String>> accountTagsMap = SugarWidgetUpdateActivator.getDefault().getAccountTagsMap();
		if (SugarWidgetUpdateActivator.getDefault().getAccountsPattern() != null && isMatchPreferenceSelected(SugarType.ACCOUNTS.getParentType())) {
			matches.get(SugarType.ACCOUNTS).addAll(findMatches(notesDoc, SUBJECT, SugarWidgetUpdateActivator.getDefault().getAccountsPattern(), SugarType.ACCOUNTS, accountTagsMap));
			matches.get(SugarType.ACCOUNTS).addAll(findMatches(notesDoc, BODY, SugarWidgetUpdateActivator.getDefault().getAccountsPattern(), SugarType.ACCOUNTS, accountTagsMap));
		}
		Map<String, Set<String>> contactTagsMap = SugarWidgetUpdateActivator.getDefault().getContactTagsMap();
		if (SugarWidgetUpdateActivator.getDefault().getContactsPattern() != null && isMatchPreferenceSelected(SugarType.CONTACTS.getParentType())) {
			matches.get(SugarType.CONTACTS).addAll(findMatches(notesDoc, SUBJECT, SugarWidgetUpdateActivator.getDefault().getContactsPattern(), SugarType.CONTACTS, contactTagsMap));
			matches.get(SugarType.CONTACTS).addAll(findMatches(notesDoc, BODY, SugarWidgetUpdateActivator.getDefault().getContactsPattern(), SugarType.CONTACTS, contactTagsMap));
		}
		Map<String, Set<String>> opportunityTagsMap = SugarWidgetUpdateActivator.getDefault().getOpportunityTagsMap();
		if (SugarWidgetUpdateActivator.getDefault().getOpportunitiesPatern() != null && isMatchPreferenceSelected(SugarType.OPPORTUNITIES.getParentType())) {
			matches.get(SugarType.OPPORTUNITIES).addAll(findMatches(notesDoc, SUBJECT, SugarWidgetUpdateActivator.getDefault().getOpportunitiesPatern(), SugarType.OPPORTUNITIES, opportunityTagsMap));
			matches.get(SugarType.OPPORTUNITIES).addAll(findMatches(notesDoc, BODY, SugarWidgetUpdateActivator.getDefault().getOpportunitiesPatern(), SugarType.OPPORTUNITIES, opportunityTagsMap));
		}
		// TODO: This is the default opportunities pattern, we may want to comment this out for the real data
		matches.get(SugarType.OPPORTUNITIES).addAll(findMatches(notesDoc, SUBJECT, GenericUtils.OPPORTUNITY_PATTERN, SugarType.OPPORTUNITIES, null));
		matches.get(SugarType.OPPORTUNITIES).addAll(findMatches(notesDoc, BODY, GenericUtils.OPPORTUNITY_PATTERN, SugarType.OPPORTUNITIES, null));

		setAssociateData(notesDoc, matches);

		return matches;
	}

	private boolean isMatchPreferenceSelected(String type) {
		boolean isSelected = true;
		if (type != null) {
			Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
			if (SugarType.ACCOUNTS.getParentType().equals(type)) {
				isSelected = prefs.getBoolean(SugarWebservicesOperations.SALESCONNECT_LIVETEXT_MATCH_ACCOUNT_PREF_KEY);
			} else if (SugarType.OPPORTUNITIES.getParentType().equals(type)) {
				isSelected = prefs.getBoolean(SugarWebservicesOperations.SALESCONNECT_LIVETEXT_MATCH_OPPTY_PREF_KEY);
			} else if (SugarType.CONTACTS.getParentType().equals(type)) {
				isSelected = prefs.getBoolean(SugarWebservicesOperations.SALESCONNECT_LIVETEXT_MATCH_CONTACT_PREF_KEY);
			}
		}
		return isSelected;

	}

	private void setAssociateData(Document notesDoc, Map<SugarType, Set<SugarEntrySurrogate>> matches) {
		String associateDataMapXML = getNotesItemValue(notesDoc, CRM_ASSOCIATE);
		String[] attachmentNames = getNotesAttachmentNames(notesDoc);
		// Get invitee information
		List requiredInvitees = getNotesInvitees(notesDoc, REQUIRED_INVITEES_TYPE);
		List optionalInvitees = getNotesInvitees(notesDoc, OPTIONAL_INVITEES_TYPE);

		// Create a SugarEntry if either the Document has Associate information or the Document has attachments.
		if ((associateDataMapXML != null && !associateDataMapXML.equals(ConstantStrings.EMPTY_STRING)) || (attachmentNames != null && attachmentNames.length > 0)) {
			SugarType type = getDefaultAssociateSugarType();
			if (type != null) {
				Set<SugarEntrySurrogate> entrySet = new HashSet<SugarEntrySurrogate>();
				entrySet.add(new SugarEntrySurrogate(CRM_ASSOCIATE, type, associateDataMapXML, attachmentNames, requiredInvitees, optionalInvitees));
				matches.get(type).addAll(entrySet);
			}
		}
	}

	private String[] getNotesAttachmentNames(Document doc) {
		String[] attachmentNames = null;

		if (doc != null) {
			List<String> nameList = new ArrayList<String>();
			try {
				Item item = doc.getFirstItem("Body"); //$NON-NLS-1$

				if (item instanceof RichTextItem) {
					RichTextItem body = (RichTextItem) item;
					if (body != null) {
						Vector v = body.getEmbeddedObjects();
						if (v != null) {
							Enumeration e = v.elements();
							while (e.hasMoreElements()) {
								EmbeddedObject eo = (EmbeddedObject) e.nextElement();
								if (eo.getName() != null) {
									// Use the 1st byte as select flag - used in AssociateDialog class... Lazy way to avoid creating an
									// Object.
									StringBuffer sb = new StringBuffer("1"); //$NON-NLS-1$
									sb.append(eo.getName());
									nameList.add(sb.toString());
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (!nameList.isEmpty()) {
				attachmentNames = nameList.toArray(new String[nameList.size()]);
			}
		}
		return attachmentNames;
	}

	private String[] getNotesAssignees(Document doc) {
		String[] assignees = null;

		if (doc != null) {
			List<String> nameList = new ArrayList<String>();
			try {
				Item item = null;
				if (doc.hasItem(DOC_SEND_TO)) {
					item = doc.getFirstItem(DOC_SEND_TO);
				} else if (doc.hasItem(REQUIREDATTENDEES)) {
					item = doc.getFirstItem(REQUIREDATTENDEES);
				}

				if (item != null && item.getValues() != null) {

					nameList.addAll(item.getValues());
				}

				item = null;
				if (doc.hasItem(DOC_COPY_TO)) {
					item = doc.getFirstItem(DOC_COPY_TO);
				} else if (doc.hasItem(OPTIONALATTENDEES)) {
					item = doc.getFirstItem(OPTIONALATTENDEES);
				}

				if (item != null && item.getValues() != null) {
					nameList.addAll(item.getValues());
				}
				if (nameList.size() > MAX_NAME_LIST) {
					nameList = nameList.subList(0, MAX_NAME_LIST);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			if (!nameList.isEmpty()) {
				assignees = nameList.toArray(new String[nameList.size()]);
			}
		}
		return assignees;
	}

	private List<String> toTrim(String s) {
		List<String> list = new ArrayList<String>();
		if (s != null) {

			String[] ss = s.split(ConstantStrings.COMMA);
			for (int i = 0; i < ss.length; i++) {
				if (ss[i].equals(ConstantStrings.EMPTY_STRING)) {
					// nothing
				} else {
					list.add(ss[i].trim());
				}
			}
		}
		return list;
	}

	public String[] getNotesAssignees(NotesUIDocument uiDoc) {
		String[] assignees = null;

		if (uiDoc != null) {
			List<String> nameList = new ArrayList<String>();
			try {

				String valueX = getFieldValue(uiDoc, UIDOC_SEND_TO);

				if (valueX != null) {

					nameList.addAll(toTrim(valueX));
				}
				valueX = getFieldValue(uiDoc, UIDOC_COPY_TO);

				if (valueX != null) {
					nameList.addAll(toTrim(valueX));
				}

				if (nameList.size() > MAX_NAME_LIST) {
					nameList = nameList.subList(0, MAX_NAME_LIST);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			if (!nameList.isEmpty()) {
				assignees = nameList.toArray(new String[nameList.size()]);
			}
		}
		return assignees;
	}

	public static SugarType getDefaultAssociateSugarType() {
		return SugarType.CONTACTS;
	}

	private String getNotesItemValue(Document notesDoc, String itemName) {
		String valueX = null;
		if (notesDoc != null && itemName != null) {
			try {
				Vector items = notesDoc.getItemValue(itemName);
				if (items != null && items.size() > 0) {
					String text = (String) items.get(0);
					valueX = text;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return valueX;
	}

	private Set<SugarEntrySurrogate> findMatches(String text, Pattern pattern, SugarType type, Map<String, Set<String>> tags) {
		Set<SugarEntrySurrogate> matches = null;

		if (pattern != null && !pattern.equals(ConstantStrings.EMPTY_STRING)) {
			try {

				if (text != null) {
					matches = findMatchesProcess(text, pattern, type, tags);
				}

			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
			}
		}
		return matches;
	}

	private Set<SugarEntrySurrogate> findMatches(Document notesDoc, String itemName, Pattern pattern, SugarType type, Map<String, Set<String>> tags) {
		Set<SugarEntrySurrogate> matches = new HashSet<SugarEntrySurrogate>();
		if (pattern != null && !pattern.equals(ConstantStrings.EMPTY_STRING)) {
			try {
				Vector items = notesDoc.getItemValue(itemName);
				if (items != null && items.size() > 0) {
					String matchid = ConstantStrings.EMPTY_STRING;
					String text = (String) items.get(0);
					if (text != null) {
						matches = findMatchesProcess(text, pattern, type, tags);
					}

				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
			}
		}
		return matches;
	}

	private Set<SugarEntrySurrogate> findMatchesProcess(String text, Pattern pattern, SugarType type, Map<String, Set<String>> tags) {

		Set<SugarEntrySurrogate> matches = new HashSet<SugarEntrySurrogate>();
		Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			String match = matcher.group();

			String matchName = match;

			if (tags != null) {
				for (String key : tags.keySet()) {
					Set<String> tagsForThisKey = tags.get(key);
					for (String tag : tagsForThisKey) {
						Pattern tagPattern = Pattern.compile(tag, Pattern.CASE_INSENSITIVE);
						if (tagPattern.matcher(match).matches()) {
							// The match that we found is a tag of a known account. So that we do not
							// have multiple drop zones for the same account, we will use the account key
							// as the match.
							match = key;
							break;
						}
					}

				}
			}
			// TODO: This is a temporary hack
			if (!match.equals(ConstantStrings.EMPTY_STRING)) {
				matches.add(new SugarEntrySurrogate(matchName.replaceAll(ConstantStrings.GREATER_THAN, ConstantStrings.EMPTY_STRING), match.replaceAll(ConstantStrings.GREATER_THAN,
						ConstantStrings.EMPTY_STRING), type));

			}
		}
		return matches;
	}

	public static Map<String, Map<SugarType, Set<SugarEntrySurrogate>>> getSugarDataCache() {
		if (sugarDataCache == null) {
			sugarDataCache = new HashMap<String, Map<SugarType, Set<SugarEntrySurrogate>>>();
		}
		return sugarDataCache;
	}

	public static Set<String> getCachedDocuments() {
		if (cachedDocuments == null) {
			cachedDocuments = new HashSet<String>();
		}
		return cachedDocuments;
	}

	public static Map<SugarType, Set<SugarEntrySurrogate>> getCurrentSugarDataMap() {
		if (currentSugarDataMap == null) {
			currentSugarDataMap = new HashMap<SugarType, Set<SugarEntrySurrogate>>();
		}
		return currentSugarDataMap;
	}

	public static void setCurrentSugarDataMap(Map<SugarType, Set<SugarEntrySurrogate>> currentSugarDataMap) {
		MailDocumentSelectionAction.currentSugarDataMap = currentSugarDataMap;
	}

	public static Map<String, tempNotesInfo> getSubjectCache() {
		if (subjectCache == null) {
			subjectCache = new HashMap<String, tempNotesInfo>();
		}
		return subjectCache;
	}

	private class tempNotesInfo {

		private String subject = null;
		private String form = null;
		private String[] assignees = null;

		tempNotesInfo(String s, String f, String[] ss) {
			subject = s;
			form = f;
			assignees = ss;
		}

		String getSubject() {
			return subject;
		}

		String getForm() {
			return form;
		}

		String[] getAssignees() {
			return assignees;
		}

		void setSubject(String s) {
			subject = s;
		}

		void setForm(String f) {
			form = f;
		}

		void setAssignees(String[] ss) {
			assignees = ss;
		}

	}

	public static String getForm(String unid) {
		String form = null;
		if (getSubjectCache() != null && getSubjectCache().get(unid) != null) {
			form = getSubjectCache().get(unid).getForm();
		}
		return form;
	}

	public static String[] getAssignees(String unid) {
		String[] assignees = null;
		if (getSubjectCache() != null && getSubjectCache().get(unid) != null) {
			assignees = getSubjectCache().get(unid).getAssignees();
		}
		return assignees;
	}

	public static String getCurrentUnid() {
		return currentUnid;
	}

	public static String getLastTextSelection() {
		return lastTextSelection;
	}

	public static void setLastTextSelection(String lastTextSelection) {
		MailDocumentSelectionAction.lastTextSelection = lastTextSelection;
	}

	public static MailDocumentSelectionAction getInstance() {
		if (instance == null) {
			instance = new MailDocumentSelectionAction();
		}
		return instance;
	}

	public static void clearUpCache(String id) {
		if (id != null) {
			getSugarDataCache().remove(id);
			getCachedDocuments().remove(id);
			// System.out.println("====== REMOVE cached data from sugarDataCache for " + id);
		}
	}

	public static List<String> isModifiedList = null;
	public static List<String> getIsModifiedList() {
		if (isModifiedList == null) {
			isModifiedList = new ArrayList<String>();
		}
		return isModifiedList;
	}

	public static void setIsModified(String id, boolean b) {
		if (getIsModifiedList().contains(id)) {
			if (!b) {
				getIsModifiedList().remove(id);
			}
		} else {
			if (b) {
				getIsModifiedList().add(id);
			}
		}
	}
	public static boolean isModified(String id) {
		boolean isModified = false;
		if (getIsModifiedList().contains(id)) {
			isModified = true;
		}
		return isModified;
	}
}
