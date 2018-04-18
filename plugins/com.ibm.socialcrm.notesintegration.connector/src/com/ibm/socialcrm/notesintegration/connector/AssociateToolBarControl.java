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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import com.ibm.notes.java.ui.NotesUIWorkspace;
import com.ibm.notes.java.ui.documents.NotesUIDocument;
import com.ibm.rcp.jface.action.SControlContribution;
import com.ibm.socialcrm.notesintegration.connector.util.AssociatePrefillManager;
import com.ibm.socialcrm.notesintegration.connector.util.ConnectorUtil;
import com.ibm.socialcrm.notesintegration.connector.util.CopytoObject;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarAccount;
import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.core.SugarEntrySurrogate;
import com.ibm.socialcrm.notesintegration.core.SugarLead;
import com.ibm.socialcrm.notesintegration.core.SugarOpportunity;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.core.utils.UpdateSelectionsBroadcaster;
import com.ibm.socialcrm.notesintegration.ui.actions.MailDocumentSelectionAction;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateData;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateDataMap;
import com.ibm.socialcrm.notesintegration.ui.custom.SFAToggleButton;
import com.ibm.socialcrm.notesintegration.ui.dnd.SugarDashboardDndEntry;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.workplace.noteswc.selection.NotesOpenDocumentSummary;
import com.ibm.workplace.noteswc.swt.NotesSashControl;
import com.ibm.workplace.noteswc.views.NotesViewPart;

public class AssociateToolBarControl extends SControlContribution {

	private Display _display = null;

	public final static String[] CALENDAR_FORM = {"appointment", "notice"}; //$NON-NLS-1$

	private final static String[] MAIL_OR_CALENDAR_FORMS = {"memo", "appointment", "notice", "reply"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 

	private final static String ASSOCIATE_STRING = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.TOOL_BAR_ACTION_ASSOCIATE);
	private final static String ASSOCIATED_STRING = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.TOOL_BAR_ACTION_ASSOCIATED);

	private String[] _lineLabels = {GenericUtils.padStringWithSpaces(ASSOCIATE_STRING), GenericUtils.padStringWithSpaces(ASSOCIATED_STRING)};
	private int PADDING_WIDTH = 6;

	private int _maxLabelWidth = -1;

	IRunnableWithProgress runnable = null;

	private SugarEntrySurrogate _selectedSugarEntry = null;
	private String _documentActionMapKey = null;

	private Composite _parent;
	private SFAToggleButton _associateButton = null;

	private PropertyChangeListener _propertyChangedListener = null;
	private IPartListener _partListener = null;
	private String _form = null;
	private AssociateComposite _dialog = null;

	// flag indicating if a CopyTo process is in progress... If true, the association inf. in ActionMap is more up-to-date
	// than the inf. extracted from the Notes document.
	private boolean _isInUpdateProcess = false;

	private boolean _isCalendar;
	private String _newdocid;

	public AssociateToolBarControl() {
	}

	@Override
	protected Control createControl(final Composite parent) {
		_parent = parent;

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(GridLayoutFactory.fillDefaults().spacing(SWT.DEFAULT, SWT.DEFAULT).margins(0, 0).numColumns(2).create());

		Label imageControl = new Label(container, SWT.NONE);
		imageControl.setLayoutData(GridDataFactory.fillDefaults().grab(false, false).align(SWT.LEFT, SWT.CENTER).create());
		imageControl.setImage(SFAImageManager.getImage(SFAImageManager.SALES_CONNECT));

		computeMaxLabelSize();

		_associateButton = new SFAToggleButton(container, SWT.PUSH);

		_associateButton.setText(GenericUtils.padStringWithSpaces(ASSOCIATE_STRING));

		_associateButton.setFont(getButtonLabelFont());

		_associateButton.setEnabled(false);
		_associateButton.getButton().setToolTipText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_BUTTON_TOOL_TIP_TEXT));

		// Use MouseHover listener to display tooltip when the button is disabled
		container.addListener(SWT.MouseHover, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				Composite composite = (Composite) arg0.widget;
				composite.setToolTipText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_BUTTON_TOOL_TIP_TEXT));
			}

		});

		// The height of the button is set to the imageHeight + 2. It's a bit magical, but looks ok
		_associateButton.getButton().setLayoutData(
				GridDataFactory.fillDefaults().grab(false, false).indent(PADDING_WIDTH, 0).hint(_maxLabelWidth + 5 * PADDING_WIDTH, imageControl.getImage().getImageData().height + 2).align(SWT.LEFT,
						SWT.CENTER).create());

		addPropertyChangeListener();

		addSelectionListener();

		addPartListener();

		getToolbarAssociateButton().getButton().redraw();

		return container;
	}

	private SFAToggleButton getToolbarAssociateButton() {
		return _associateButton;
	}

	public void dispose() {
		super.dispose();
		if (_partListener != null) {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window != null) {
				window.getPartService().removePartListener(_partListener);
			}
		}

		if (_propertyChangedListener != null) {
			UpdateSelectionsBroadcaster.getInstance().unregisterListener(_propertyChangedListener);
		}
	}

	private void addPropertyChangeListener() {

		_propertyChangedListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				TempObject tempObject = null;

				if (evt != null) {

					// 55883
					if (evt.getPropertyName() != null && evt.getPropertyName().equals(UpdateSelectionsBroadcaster.CONNECTOR_UPDATE_IS_DONE)) {
						_isInUpdateProcess = false;

					} else

					// "CONNECTOR_UPDATE" could be triggered by one of the following scenarios:
					// From card's "More" action - associate this document... or create meeting with prefill information.
					// - create email/meeting triggered from SalesConnect
					// - d&d mail/calendar to a card
					if (evt.getPropertyName() != null && evt.getPropertyName().equals(UpdateSelectionsBroadcaster.CONNECTOR_UPDATE)) {
						updateAssociateForPrefill(evt.getNewValue());
					} else {

						// new msg or non mail/calendar
						if ((evt.getNewValue() == null && evt.getOldValue() == null) || (ConnectorUtil.isNewDocument())) {

							if (isMailOrCalendarForm()) {

								// Only if this is a Notes Document (Skipping others, for example: CSINavViewPart)
								if (ConnectorUtil.getPartId() != null && ConnectorUtil.getPartId().startsWith("com.ibm.workplace.noteswc.views.NotesViewPart@")) //$NON-NLS-1$
								{

									// In case user wants to make another association before closing the document
									if (!NotesUIDocumentSaveListenerRepository.getDocumentActionMap().containsKey(ConnectorUtil.getPartId())) {
										NotesUIDocumentSaveListenerRepository.getInstance().addDocumentListener(getCurrDoc(), ConnectorUtil.getPartId(), null, null);
									}
								}
								tempObject = getToolbarInformationWhenNew();
							} else { // disable any form other than mail or calendar
								tempObject = new TempObject(null, false, true);
							}
						} else if (evt.getNewValue() != null && evt.getNewValue() instanceof HashMap && evt.getPropertyName().equals(UpdateSelectionsBroadcaster.SELECTION_CHANGED)) {
							tempObject = getToolbarInformationWhenNotNew(evt);
						}

						if (tempObject != null) {
							updateUIToolbarAssociate(tempObject);

						}
					}
				}
			}
		};

		UpdateSelectionsBroadcaster.getInstance().registerListener(_propertyChangedListener);
	}

	private boolean isMailOrCalendarForm() {
		boolean isMailOrCalendarForm = false;
		NotesUIDocument doc = getCurrDoc();
		if (doc != null) {
			String form = doc.getForm();
			if (form != null) {
				for (int i = 0; i < MAIL_OR_CALENDAR_FORMS.length; i++) {
					if (form.equalsIgnoreCase(MAIL_OR_CALENDAR_FORMS[i])) {
						isMailOrCalendarForm = true;
						break;
					}
				}

			}
		}
		return isMailOrCalendarForm;
	}

	private void updateUIToolbarAssociate(TempObject tempObject) {
		_selectedSugarEntry = tempObject.getSugarEntry();
		setAssociateEnabled(tempObject.isEnable, false);
		if (tempObject.isAssociate) {
			drawAssociateToolbarImage(true);
		} else {
			drawAssociatedToolbarImage(true);
		}
	}

	private TempObject getToolbarInformationWhenNew() {
		TempObject tempObject = null;

		if (ConnectorUtil.getPartId() != null && NotesUIDocumentSaveListenerRepository.getDocumentActionMap().containsKey(ConnectorUtil.getPartId())) {
			tempObject = new TempObject(NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(ConnectorUtil.getPartId()), true, (NotesUIDocumentSaveListenerRepository
					.getDocumentActionMap().get(ConnectorUtil.getPartId()) == null || NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(ConnectorUtil.getPartId())
					.getAssociateDataMapXML() == null) ? true : false);
		} else {
			tempObject = new TempObject(null, isEnabledAssociatePart() ? true : false, true);

		}
		return tempObject;
	}

	private TempObject getToolbarInformationWhenNotNew(PropertyChangeEvent evt) {

		TempObject tempObject = null;

		final NotesUIDocument doc = getCurrDoc();

		// Document was opened and inf. was saved in the Document Action Map already
		if (getCurrId(doc) != null && NotesUIDocumentSaveListenerRepository.getDocumentActionMap().containsKey(getCurrId(doc))) {
			// 41544 -
			// If _isInUpdateProcess (i.e. the Copyto operation is in progress):
			// Document Action Map should have a more recent copy of sugardatamap than the one from the Notes Document,
			// so, don't override the copy in the Document Action Map.
			// For example: user pressed the Copyto button -> specified sugar item to be associated with -> pressed the Copy button -> we saved
			// the association information to the Document Action Map... Then, for some reason, before this information
			// was saved in the Notes Document, a selectionchange from the MailDocumentSelectionAction was broadcasted which
			// lead us to here. In this case, the associate information from the MailDocumentSelecitonAction extracted from the doucment was
			// out-of-date, so, use the information from the Document Action Map instead.
			// if not _isInUpdateProcess:
			// For performance reason, for each selected document, MailDocumentSelectionAction first sent out a dummy broadcast, then sent out
			// another broadcast after the sugarsurrogate was populated with inf. from Notes Document (including the association inf.). In this
			// case, use sugar inf. from MailDocumentSelectionAction broadcast instead.
			// 
			UiUtils.log("getToolbarInformationWhenNotNew ==>  getCurrId(doc)=" + getCurrId(doc) + ", _isInUpdateProcess=" + _isInUpdateProcess); //$NON-NLS-1$ //$NON-NLS-2$
			if (!_isInUpdateProcess) {
				Map<SugarType, Set<SugarEntrySurrogate>> sugarDataMap = (HashMap<SugarType, Set<SugarEntrySurrogate>>) evt.getNewValue();
				SugarEntrySurrogate associateSugarEntry = getAssociateSugarEntry(sugarDataMap);
				NotesUIDocumentSaveListenerRepository.updateSugarEntry(getCurrId(doc), associateSugarEntry);
			}

			tempObject = new TempObject(NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(getCurrId(doc)), true, (NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(
					getCurrId(doc)) == null || NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(getCurrId(doc)).getAssociateDataMapXML() == null) ? true : false);
		} else {
			// Get selected sugar data
			Map<SugarType, Set<SugarEntrySurrogate>> sugarDataMap = (HashMap<SugarType, Set<SugarEntrySurrogate>>) evt.getNewValue();

			SugarEntrySurrogate associateSugarEntry = getAssociateSugarEntry(sugarDataMap);

			if (doc != null && doc.isInPreviewPane()) {
				tempObject = new TempObject(associateSugarEntry, false, ((associateSugarEntry != null && associateSugarEntry.getAssociateDataMapXML() != null) ? false : true));
			}
			// Disable the Associate tool bar action if this is document editing (i.e. double clicking)
			else if (doc != null && doc.isEditMode() && !isMeeting(doc)) {
				tempObject = new TempObject(associateSugarEntry, false, ((associateSugarEntry != null && associateSugarEntry.getAssociateDataMapXML() != null) ? false : true));
			} else {
				// Enable the Associate tool bar action only if in the appropriate form
				if (isAssociateForm(doc)) {

					// Let's save this sugar entry in the document action map. This is especially useful when doing the "associate this
					// document" action from the live text.
					if (!NotesUIDocumentSaveListenerRepository.getDocumentActionMap().containsKey(getCurrId(doc))) {
						NotesUIDocumentSaveListenerRepository.getInstance().addDocumentListener(getCurrDoc(), getCurrId(doc), associateSugarEntry, null);
					}

					tempObject = new TempObject(associateSugarEntry, true, ((associateSugarEntry != null && associateSugarEntry.getAssociateDataMapXML() != null) ? false : true));
				} else {
					tempObject = new TempObject(associateSugarEntry, false, ((associateSugarEntry != null && associateSugarEntry.getAssociateDataMapXML() != null) ? false : true));
				}

			}
		}
		return tempObject;
	}

	// Associate is called while either the association dialog is to bypassed or an association is pre-determined.
	private void updateAssociateForPrefill(Object obj) {
		BaseSugarEntry[] prefillSugarEntry = null;

		boolean isDnd = false;

		// if D&D
		if (obj != null && obj instanceof SugarDashboardDndEntry) {
			// prefillSugarEntry = ((SugarDashboardDndEntry) obj).getSugarEntry();
			prefillSugarEntry = new BaseSugarEntry[1];
			prefillSugarEntry[0] = ((SugarDashboardDndEntry) obj).getSugarEntry();

			String prefillDocid = ((SugarDashboardDndEntry) obj).getDocid();

			Map<SugarType, Set<SugarEntrySurrogate>> sugarDataMap = MailDocumentSelectionAction.getSugarDataCache().get(((SugarDashboardDndEntry) obj).getDocid());
			_form = MailDocumentSelectionAction.getForm(((SugarDashboardDndEntry) obj).getDocid());
			_selectedSugarEntry = getAssociateSugarEntry(sugarDataMap);
			_documentActionMapKey = prefillDocid;
			isDnd = true;

		} else
		// if card's MORE action
		if (obj != null && obj instanceof BaseSugarEntry) {
			prefillSugarEntry = new BaseSugarEntry[1];
			prefillSugarEntry[0] = (BaseSugarEntry) obj;
		} else if (obj != null && obj instanceof ArrayList && !((ArrayList) obj).isEmpty() && ((ArrayList) obj).get(0) instanceof BaseSugarEntry) {
			// if schedule a meeting from SalesConnect's oppty/contact/client/Lead
			prefillSugarEntry = new BaseSugarEntry[((ArrayList) obj).size()];
			prefillSugarEntry = (BaseSugarEntry[]) ((ArrayList) obj).toArray(new BaseSugarEntry[((ArrayList) obj).size()]);
		}

		if (prefillSugarEntry != null && prefillSugarEntry.length > 0) {
			// If Associate this document ... Action - for association with existing document
			if (!ConnectorUtil.isNewDocument()) {

				AssociateData toAssociateData = AssociatePrefillManager.getInstance().createAssociateData(prefillSugarEntry[0]);
				SugarType toSugarType = prefillSugarEntry[0].getSugarType();
				String toTypeaheadText = AssociatePrefillManager.getInstance().createTypeaheadText(prefillSugarEntry[0]);
				associateToolbarButtonPressed(toAssociateData, toSugarType, toTypeaheadText, isDnd);
			} else
			// If Creating new email/meeting with prefill information Action - for association with new meeting
			{
				// Update toolbar associate button so the Associate tool bar button for this document is updated.
				_selectedSugarEntry = null;
				TempObject tempObject = new TempObject(null, true, true);
				updateUIToolbarAssociate(tempObject);

				// Create AssociateData - this is the same association information if user uses the AssociateComposite to
				// specify the association information.
				// AssociateData associateData = AssociatePrefillManager.getInstance().createAssociateData(prefillSugarEntry);
				AssociateData[] associateData = new AssociateData[prefillSugarEntry.length];
				for (int i = 0; i < prefillSugarEntry.length; i++) {
					associateData[i] = AssociatePrefillManager.getInstance().createAssociateData(prefillSugarEntry[i]);
				}

				// If the key existed in the documenActionMap already, use the sugarEntry from there; otherwise, build a new one. If build
				// a new sugarEntry, we will assume there's no existing association data in the sugarEntry.
				SugarEntrySurrogate sugarEntry = null;
				_documentActionMapKey = getCurrId(getCurrDoc());
				if (_documentActionMapKey != null && NotesUIDocumentSaveListenerRepository.getDocumentActionMap().containsKey(_documentActionMapKey)) {
					sugarEntry = NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(_documentActionMapKey);
				} else {
					// sugarEntry = new SugarEntrySurrogate(((BaseSugarEntry) obj).getName(), prefillSugarEntry[0].getSugarType(), null, null);
					sugarEntry = new SugarEntrySurrogate(prefillSugarEntry[0].getName(), prefillSugarEntry[0].getSugarType(), null, null);
				}

				// Call method to update sugar/Notes.
				SugarType sugarType = getSugarType(prefillSugarEntry[0]);
				NotesUIDocument doc = getCurrDoc();
				_documentActionMapKey = getCurrId(doc);

				Map<String, CopytoObject> copytoObjectMap = new HashMap<String, CopytoObject>();
				for (int i = 0; i < prefillSugarEntry.length; i++) {
					copytoObjectMap.put("prefill" + i, new CopytoObject(associateData[i], ((sugarType == null) ? null //$NON-NLS-1$
							: getSugarType(prefillSugarEntry[i]).getParentType()), sugarType));
				}

				associateDialogOKPressed(copytoObjectMap, null, sugarEntry, doc, _documentActionMapKey);
			}
		}
	}

	private SugarType getSugarType(BaseSugarEntry baseSugarEntry) {
		SugarType sugarType = null;
		if (baseSugarEntry instanceof SugarContact) {
			sugarType = SugarType.CONTACTS;
		} else {
			if (baseSugarEntry instanceof SugarOpportunity) {
				sugarType = SugarType.OPPORTUNITIES;
			} else if (baseSugarEntry instanceof SugarAccount) {
				sugarType = SugarType.ACCOUNTS;
			} else if (baseSugarEntry instanceof SugarLead) {
				sugarType = SugarType.LEADS;
			}
		}
		return sugarType;
	}

	private void addSelectionListener() {
		getToolbarAssociateButton().getButton().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				_display = Display.getDefault();
				associateToolbarButtonPressed();
			}

		});
	}

	private void computeMaxLabelSize() {

		GC gc = new GC(_parent);
		for (int i = 0; i < _lineLabels.length; i++) {
			Point size = gc.textExtent(_lineLabels[i]); // or textExtent
			_maxLabelWidth = Math.max(_maxLabelWidth, size.x);
		}
		gc.dispose();

	}

	private boolean isAssociateForm(NotesUIDocument doc) {
		boolean isAssociateForm = false;

		if (doc != null && doc.getForm() != null) {
			isAssociateForm = true;
		}
		return isAssociateForm;
	}

	private void associateToolbarButtonPressed() {
		associateToolbarButtonPressed(null, null, null);
	}

	private void associateToolbarButtonPressed(AssociateData toAssociateData, SugarType toSugarType, String toTypeaheadText) {
		associateToolbarButtonPressed(toAssociateData, toSugarType, toTypeaheadText, false);

	}

	private boolean isDialogOpened() {

		boolean isOpened = false;
		if (_dialog != null) {
			isOpened = true;
		}

		return isOpened;
	}

	private void associateToolbarButtonPressed(AssociateData toAssociateData, SugarType toSugarType, String toTypeaheadText, boolean isDnd) {

		if (isDialogOpened()) {
			return;
		}

		NotesUIDocument currDoc = null;
		// if Dnd, _documentActionMapKey was set in updateAssociateForPrefill() already.
		if (!isDnd) {
			currDoc = getCurrDoc();

			if (currDoc != null) {
				_documentActionMapKey = getCurrId(currDoc);
			}
		}
		// 46501
		else {
			currDoc = getCurrDoc();
		}

		// reset _isInUpdateProcess flag, just in case the reset logic at the end of associateDialogOKPressed() got skipped.
		// Need to check if there is existing runnable (i.e. there is existing copyto information that has not been applied to
		// SalesConnect, yet). If yes, do not set _isInUpdateProcess to false. For example, user scheduled a meeting from
		// an item in SalesConnect, we will create an association with this item, but this association will not be applied until
		// the meeting is submitted. If user presses the copyto button before the meeting it submitted, either to check the
		// association information or to add another item to the association, we do not want to reset _isInUpdateProcess to false.
		if (!hasNewAssociation(_documentActionMapKey)) {
			_isInUpdateProcess = false;
		}

		if (_display == null) {
			_display = Display.getDefault();
		}

		SugarEntrySurrogate sugarEntry = null;

		if (currDoc != null || isDnd) {

			if (NotesUIDocumentSaveListenerRepository.getDocumentActionMap() != null && !NotesUIDocumentSaveListenerRepository.getDocumentActionMap().isEmpty() && _documentActionMapKey != null
					&& NotesUIDocumentSaveListenerRepository.getDocumentActionMap().containsKey(_documentActionMapKey)) {
				// Get sugar entry from the map. This is the case where user is doing multiple association.
				sugarEntry = NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(_documentActionMapKey);
			} else {
				sugarEntry = _selectedSugarEntry;
			}

			if (SugarWebservicesOperations.getInstance().unableToLogin()) {
				SugarWebservicesOperations.getPropertyChangeSupport().firePropertyChange(SugarWebservicesOperations.BRING_UP_CREDENTIAL_PROMPT, true, false);
			} else {

				try {
					// Bring up the associate dialog
					_dialog = new AssociateComposite(_display, currDoc, sugarEntry, getSugarDataCache(currDoc, _documentActionMapKey), getAssignees(currDoc, _documentActionMapKey), _form,
							toAssociateData, toSugarType);

					while (_dialog != null && _dialog.getShell() != null && !_dialog.getShell().isDisposed()) {
						if (_display != null && !_display.readAndDispatch()) {
							_display.sleep();
						}
					}

					// To Associate
					if (_dialog != null && _dialog.getCollectionModel() != null && _dialog.getCollectionModel().isToCopy() && _dialog.getCollectionModel().getCopytoObjectMap() != null
							&& !_dialog.getCollectionModel().getCopytoObjectMap().isEmpty()) {
						String[] attachmentList = _dialog.getCollectionModel().getAssociatedAttachmentList();
						associateDialogOKPressed(_dialog.getCollectionModel().getCopytoObjectMap(), attachmentList, sugarEntry, currDoc, _documentActionMapKey, isDnd);
					}
				} catch (Exception e) {
					e.printStackTrace();
					// do nothing, just want to catch any exception, specifically if any from dialog, so
					// we can reset _dialog.
				}
			}
			_dialog = null;

		}

	}

	private Map<SugarType, Set<SugarEntrySurrogate>> getSugarDataCache(NotesUIDocument uiDoc, String docKey) {
		Map<SugarType, Set<SugarEntrySurrogate>> dataCache = null;

		// 30086
		// don't check if uiDoc == null because that will preclude d&d scenario
		if (docKey != null) {
			dataCache = MailDocumentSelectionAction.getSugarDataCache().get(docKey);
			if (isNewDoc(uiDoc) && dataCache == null) {
				dataCache = MailDocumentSelectionAction.getInstance().findSugarData(uiDoc);
			}

		}
		return dataCache;
	}

	private String[] getAssignees(NotesUIDocument uiDoc, String docKey) {
		String[] assignees = null;

		// 30086
		// don't check if uiDoc == null because that will preclude d&d scenario
		if (docKey != null) {
			assignees = MailDocumentSelectionAction.getAssignees(docKey);

			if (isNewDoc(uiDoc) && assignees == null) {
				assignees = MailDocumentSelectionAction.getInstance().getNotesAssignees(uiDoc);
			}

		}
		return assignees;
	}

	private boolean isNewDoc(NotesUIDocument doc) {
		boolean isNewDoc = false;
		if (doc != null && doc.isNewDoc()) {
			isNewDoc = true;
		}
		return isNewDoc;
	}

	private String getCurrId(NotesUIDocument doc) {
		String currId = null;
		if (doc != null) {
			if (doc.isNewDoc()) {
				currId = ConnectorUtil.getPartId();
			} else {
				if (NotesUIDocumentSaveListenerRepository.getDocumentActionMap().containsKey(ConnectorUtil.getPartId())) {
					currId = ConnectorUtil.getPartId();
				} else {
					currId = doc.getDocumentData().getUnid();
				}
			}
		}
		return currId;
	}

	private NotesUIDocument getCurrDoc() {
		NotesUIDocument currDoc = null;
		NotesUIWorkspace workspace = new NotesUIWorkspace();

		if (workspace.getCurrentDocument() != null) {
			currDoc = workspace.getCurrentDocument();
		}
		return currDoc;
	}

	private void associateDialogOKPressed(Map copytoObjectMap, String[] attachmentList, SugarEntrySurrogate sugarEntry, NotesUIDocument doc, String documentActionMapKey) {
		associateDialogOKPressed(copytoObjectMap, attachmentList, sugarEntry, doc, documentActionMapKey, false);
	}

	private void associateDialogOKPressed(Map copytoObjectMap, String[] attachmentList, SugarEntrySurrogate sugarEntry, NotesUIDocument doc, String documentActionMapKey, boolean isDnd) {

		if (copytoObjectMap != null && !copytoObjectMap.isEmpty() && documentActionMapKey != null) {

			_isInUpdateProcess = true;

			AssociateDataMap associateDataMap = null;

			if (sugarEntry == null || sugarEntry.getAssociateDataMapXML() == null) {
				associateDataMap = new AssociateDataMap();
			} else {
				associateDataMap = ConnectorUtil.decode(sugarEntry.getAssociateDataMapXML());
			}

			// merge the associate information received from the AssociateDialog into the existing AssociateDataMap in the Sugar Entry.
			Iterator<CopytoObject> it = copytoObjectMap.values().iterator();
			while (it.hasNext()) {
				CopytoObject co = it.next();
				associateDataMap.addAssociateData(co.getAssociateSugarType().getParentType(), co.getAssociatedData());
			}
			String associateDataXML = ConnectorUtil.encode(associateDataMap);
			sugarEntry = ConnectorUtil.updateAssociateDataInSugarEntry(sugarEntry, associateDataXML);

			// update the attachment list received from the AssociateDialog into the Sugar Entry.
			sugarEntry.setAttachmentNames(attachmentList);

			setIsCalendar(isCalendar(doc == null ? null : doc.getForm()));

			// If new document ... update Sugar and Notes at call back time
			if (ConnectorUtil.isNewDocument(doc) || isCalendar(doc == null ? null : doc.getForm())) {
				runnable = createRunnableForSugarAndNotes(doc);
				NotesUIDocumentSaveListenerRepository.getInstance().addDocumentListener(doc, documentActionMapKey, sugarEntry, runnable);
			} else if (isDnd || (doc != null && doc.isEditable()))
			// Update Sugar and Notes with association info right now.
			{

				NotesUIDocumentSaveListenerRepository.getInstance().addDocumentListener(doc, documentActionMapKey, sugarEntry, null);

				runnable = createRunnableForSugarAndNotes(doc);

				try {
					runnable.run(new NullProgressMonitor());
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
				}
				// Mark all the AssociateData in SugarEntry as associated (So, if user does another association
				// before closing the document, we won't send out dup. Notes/Sugar update request).
				// This copy of SugarEntry will be saved in DocumentActionMap. If user does another association
				// before closing the document, we will be able to continue the process basing on this
				// updated sugarEntry.
				NotesUIDocumentSaveListenerRepository.updateSugarEntry(_documentActionMapKey, updateAllAssociateDataInSugarEntry(sugarEntry));

				// 55883 - don't turn off the _isInUpdateProcess flag here, the update processing in the other thread might still be going...
				// wait for property change event instead
				// // Done applying association to SalesConnect, turn off the flag
				// _isInUpdateProcess = false;
			}

			drawAssociatedToolbarImage(true);

			// _isInUpdateProcess = false;
		}
	}

	private SugarEntrySurrogate updateAllAssociateDataInSugarEntry(SugarEntrySurrogate sugarEntry) {
		SugarEntrySurrogate newSugarEntry = null;
		if (sugarEntry != null) {
			AssociateDataMap associateDataMap = ConnectorUtil.decode(sugarEntry.getAssociateDataMapXML());
			associateDataMap.setAllIsAssociated(true);

			newSugarEntry = ConnectorUtil.updateAssociateDataInSugarEntry(sugarEntry, ConnectorUtil.encode(associateDataMap));
		}
		return newSugarEntry;
	}

	private boolean isMeeting(NotesUIDocument doc) {
		boolean isMeeting = false;
		if (doc != null && !doc.isNewDoc()) {
			if (doc.getForm() != null && doc.getForm().equalsIgnoreCase("Appointment")) //$NON-NLS-1$   
			{
				isMeeting = true;
			}
		}
		return isMeeting;
	}

	private void addPartListener() {

		_partListener = new IPartListener() {
			public void partActivated(IWorkbenchPart part) {
				// When switching tab, reset the Associate tool bar button to the default
				// state (i.e. "Associate" and disabled). If the selection change listener is
				// triggered, the state will be updated.
				drawAssociateToolbarImage(false);
				setAssociateEnabled(false, true);
			}

			public void partBroughtToTop(IWorkbenchPart part) {
			}

			public void partClosed(IWorkbenchPart part) {

				// if User did copyto, right after the dialog was closed, _isInUpdateProcess was set to true... so, if this flag is false, it
				// indicates that user did not do any copyto.
				if (!_isInUpdateProcess) {

					// If this is calendar update but user did not update any copyto/association information, we need to add a runnable here so
					// we can inject our SalesConnect update logic.
					if (isDirtyCalendar(part)) {

						NotesUIDocument uidoc = getWorkbenchDoc(part);
						String unid = getWorkbenchUnid(part);
						// Calendar was modified
						UiUtils.log("Part closing... isDirtyCalendar:" + unid + ", isModified?" + MailDocumentSelectionAction.getInstance().isModified(unid)); //$NON-NLS-1$
						if (MailDocumentSelectionAction.getInstance().isModified(unid)) {

							// existing calendar + calendar update (or cancel) + no association at all ==> do nothing
							if (NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(unid) == null
									|| NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(unid).getAssociateDataMapXML() == null) {
								UiUtils.log("DO NOTHING ...");
							}

							// existing calendar + calendar update + no new association ==> create runnable
							else if (isDirtyCalendarWithAssociationUpdate(part, false)) {
								UiUtils.log("Part closing... create runnable for :" + unid); //$NON-NLS-1$
								_documentActionMapKey = unid;
								IRunnableWithProgress runnable = createRunnableForSugarAndNotes(uidoc, true);
								NotesUIDocumentSaveListenerRepository.getInstance().addDocumentListener(uidoc, _documentActionMapKey, getActionMapSugarEntry(unid), runnable);
								setIsCalendar(true);
							}
							MailDocumentSelectionAction.getInstance().setIsModified(unid, false);
						} else
						// calendar was not modified, user canceled the operation
						{
							// was new association specified, remove the runnable
							if (isDirtyCalendarWithAssociationUpdate(part, true)) {
								_documentActionMapKey = unid;
								NotesUIDocumentSaveListenerRepository.getInstance().addDocumentListener(uidoc, _documentActionMapKey, getActionMapSugarEntry(unid), null);
								setIsCalendar(true);
							}
						}
					}
				}

				if (NotesUIDocumentSaveListenerRepository.getDocumentActionMap() != null && NotesUIDocumentSaveListenerRepository.getDocumentActionMap().containsKey(_documentActionMapKey)) {
					try {
						UiUtils.log("Part closing... checking if runnable available for :" + _documentActionMapKey); //$NON-NLS-1$
						if (NotesUIDocumentSaveListenerRepository.getDocumentRunnableMap().get(_documentActionMapKey) != null) {
							NotesUIDocumentSaveListenerRepository.getDocumentRunnableMap().get(_documentActionMapKey).run(null);
						}
					} catch (Exception e) {
						UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
					}

					_newdocid = null;
					if (ConnectorUtil.isNewDocument(_documentActionMapKey)) {
						_newdocid = getWorkbenchUnid(part);
					}
					UIJob removeActionMapEntry = new UIJob("removeActionMapEntry") //$NON-NLS-1$
					{

						@Override
						public IStatus runInUIThread(IProgressMonitor arg0) {
							UiUtils.log("Part closing... Remove entry from documentRunnableMap: " + _documentActionMapKey); //$NON-NLS-1$

							MailDocumentSelectionAction.clearUpCache(_documentActionMapKey);

							// In the scenario where a new document having the copyto information, we want to clear up the cache in MailDocumentSelectionAction,
							// so, when user clicks on the newly created document, it will reflect the "CopiedTo" status. For a new document,
							// _documentActionmapKey is not the real document id, _newdocid is the real new document's id.
							if (_newdocid != null) {
								MailDocumentSelectionAction.clearUpCache(_newdocid);
							}

							NotesUIDocumentSaveListenerRepository.getDocumentRunnableMap().remove(_documentActionMapKey);
							NotesUIDocumentSaveListenerRepository.getDocumentActionMap().remove(_documentActionMapKey);

							UiUtils.log("partClosed ==>  setting _isInUpdateProcess to FALSE"); //$NON-NLS-1$
							_isInUpdateProcess = false;

							return Status.OK_STATUS;
						}

					};

					// setting rule so this UIJob will be exeuted after the Sugar/Notes update job in AssoiateUpdateManager is finished.
					removeActionMapEntry.setRule(ConnectorUtil.UPDATE_ASSOCIATE_JOB_RULE);
					removeActionMapEntry.schedule();

				}
			}

			public void partDeactivated(IWorkbenchPart part) {
			}

			public void partOpened(IWorkbenchPart part) {
			}
		};

		if (PlatformUI.getWorkbench() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService() != null) {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(_partListener);
		}

	}
	private boolean isEnabledAssociatePart() {
		boolean isEnabledAssociatePart = false;

		NotesUIWorkspace workspace = new NotesUIWorkspace();
		if (PlatformUI.getWorkbench() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null
				&& PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart() != null
				&& PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart() instanceof com.ibm.workplace.noteswc.views.NotesViewPart
				&& workspace.getCurrentDocument() != null && isAssociateForm(workspace.getCurrentDocument())) {
			isEnabledAssociatePart = true;

		}
		return isEnabledAssociatePart;
	}

	private void setAssociateEnabled(boolean enabled, boolean toRedraw) {
		if (getToolbarAssociateButton() != null && !getToolbarAssociateButton().getButton().isDisposed()) {
			getToolbarAssociateButton().setEnabled(enabled);
		}

		if (toRedraw) {
			getToolbarAssociateButton().getButton().redraw();
		}

	}

	private void drawAssociateToolbarImage(boolean toRedraw) {
		getToolbarAssociateButton().setText(ASSOCIATE_STRING);

		getToolbarAssociateButton().setPressed(false);

		if (toRedraw) {
			getToolbarAssociateButton().getButton().redraw();
		}
	}

	private void drawAssociatedToolbarImage(boolean toRedraw) {
		getToolbarAssociateButton().setText(ASSOCIATED_STRING);

		// To get the dark background
		getToolbarAssociateButton().setPressed(true);

		if (toRedraw) {
			getToolbarAssociateButton().getButton().redraw();
		}

	}

	private SugarEntrySurrogate getAssociateSugarEntry(Map<SugarType, Set<SugarEntrySurrogate>> matches) {
		SugarEntrySurrogate sugarEntry = null;
		SugarType type = MailDocumentSelectionAction.getDefaultAssociateSugarType();

		if (matches != null && type != null) {
			Set<SugarEntrySurrogate> entrySet = matches.get(type);
			if (entrySet != null && !entrySet.isEmpty()) {
				for (SugarEntrySurrogate entry : entrySet) {
					if (entry.getName() != null && entry.getName().equals(MailDocumentSelectionAction.CRM_ASSOCIATE)) {
						sugarEntry = entry;
						break;
					}
				}
			}
		}
		return sugarEntry;
	}

	// Runnable to update sugar client and Notes document with association info.
	private IRunnableWithProgress createRunnableForSugarAndNotes(final NotesUIDocument doc) {
		return createRunnableForSugarAndNotes(doc, false);
	}

	private IRunnableWithProgress createRunnableForSugarAndNotes(final NotesUIDocument doc, final boolean isDirtyCalendarWithoutAssociationUpdate) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				// passing current map key to AssociateUpdateManager so it can get the latest SugarEntry from the map.
				// Do not pass getCurrId() value, because for new email/meeting, the runnable is executed at the part closing
				// time, getCurrId() might point to the incorrect value.
				AssociateUpdateManager manager = new AssociateUpdateManager(doc, _documentActionMapKey);
				manager.setDirtyCalendarWithoutAssociationUpdate(isDirtyCalendarWithoutAssociationUpdate);
				manager.updateAssociate();
			}
		};
	}

	private class TempObject {
		private SugarEntrySurrogate sugarEntry = null;
		private boolean isEnable = false;
		private boolean isAssociate = true; /* true:to be Associated, false:Associated already */

		TempObject(SugarEntrySurrogate s, boolean b1, boolean b2) {
			sugarEntry = s;
			isEnable = b1;
			isAssociate = b2;
		}

		SugarEntrySurrogate getSugarEntry() {
			return sugarEntry;
		}

		boolean isEnable() {
			return isEnable;
		}

		boolean isAssociate() {
			return isAssociate;
		}
	}

	Font _buttonLabelFont = null;

	public Font getButtonLabelFont() {
		if (_buttonLabelFont == null) {
			String fontName = "associateButtonLabelFont"; //$NON-NLS-1$
			if (JFaceResources.getFontRegistry().hasValueFor(fontName)) {
				_buttonLabelFont = JFaceResources.getFontRegistry().get(fontName);
			} else {
				JFaceResources.getFontRegistry().put(fontName, new FontData[]{new FontData("Arial", 9, SWT.NORMAL)}); //$NON-NLS-1$
				_buttonLabelFont = JFaceResources.getFontRegistry().get(fontName);
			}
		}
		return _buttonLabelFont;
	}
	private boolean isCalendar() {
		return _isCalendar;
	}

	public static boolean isCalendar(String form) {
		boolean isCalendar = false;
		for (int i = 0; i < CALENDAR_FORM.length; i++) {
			if (form != null && form.equalsIgnoreCase(CALENDAR_FORM[i])) {
				isCalendar = true;
				break;
			}
		}
		return isCalendar;
	}

	private boolean isDirtyCalendarWithAssociationUpdate(IWorkbenchPart part, boolean withAssociationUpdate) {
		boolean isDirtyCalendar = false;
		String unid = null;
		boolean isDirty = false;

		if (part != null && part instanceof NotesViewPart) {
			NotesViewPart viewPart = (NotesViewPart) part;
			if (viewPart != null && viewPart.getNotesControl() != null && viewPart.getNotesControl() instanceof NotesSashControl) {
				NotesSashControl sashControl = (NotesSashControl) viewPart.getNotesControl();
				NotesOpenDocumentSummary openDoc = sashControl.getDocument();
				if (openDoc != null) {

					if (isCalendar(openDoc.getForm())) {

						// this document was updated or deleted?
						if (openDoc.getViewContext() != null) {
							unid = openDoc.getUnid();
							if (unid != null) { // this will eliminate a cancelled New Calendar
								isDirty = openDoc.getViewContext().isDirty(); // indicate it's been updated or deleted
								if (isDirty) {
									UiUtils.log("partClosed ==>  setting _isInUpdateProcess to true because it is dirty"); //$NON-NLS-1$
									_isInUpdateProcess = true;

									if ((withAssociationUpdate && hasNewAssociation(unid)) || (!withAssociationUpdate && !hasNewAssociation(unid))) {
										isDirtyCalendar = true;
									}

								} // isDirty
							} // unid != null
						} // openDoc.getViewContext() != null
					} // is calendar
				} // openDoc != null
			}
		}

		if (!isDirtyCalendar) {
			UiUtils.log("partClosed ==>  setting _isInUpdateProcess to false because it is dirty but NO association"); //$NON-NLS-1$
			_isInUpdateProcess = false;
		}

		return isDirtyCalendar;
	}

	private boolean isCalendar(IWorkbenchPart part) {
		boolean isCalendar = false;

		if (part != null && part instanceof NotesViewPart) {
			NotesViewPart viewPart = (NotesViewPart) part;
			if (viewPart != null && viewPart.getNotesControl() != null && viewPart.getNotesControl() instanceof NotesSashControl) {
				NotesSashControl sashControl = (NotesSashControl) viewPart.getNotesControl();
				NotesOpenDocumentSummary openDoc = sashControl.getDocument();
				if (openDoc != null) {

					if (isCalendar(openDoc.getForm())) {

						isCalendar = true;

					} // is calendar
				} // openDoc != null
			}
		}

		return isCalendar;
	}

	private boolean isDirtyCalendar(IWorkbenchPart part) {
		boolean isDirtyCalendar = false;
		String form = null;
		String unid = null;
		boolean isDirty = false;
		boolean wasRelated = false;
		NotesUIDocument uidoc = null;

		if (part != null && part instanceof NotesViewPart) {
			NotesViewPart viewPart = (NotesViewPart) part;
			if (viewPart != null && viewPart.getNotesControl() != null && viewPart.getNotesControl() instanceof NotesSashControl) {
				NotesSashControl sashControl = (NotesSashControl) viewPart.getNotesControl();
				NotesOpenDocumentSummary openDoc = sashControl.getDocument();
				if (openDoc != null) {

					if (isCalendar(openDoc.getForm())) {

						// this document was updated or deleted?
						if (openDoc.getViewContext() != null) {
							unid = openDoc.getUnid();
							if (unid != null) { // this will eliminate a cancelled New Calendar
								isDirty = openDoc.getViewContext().isDirty(); // indicate it's been updated or deleted
								if (isDirty) {
									isDirtyCalendar = true;

								} // isDirty
							} // unid != null
						} // openDoc.getViewContext() != null
					} // is calendar
				} // openDoc != null
			}
		}

		return isDirtyCalendar;
	}

	private boolean hadAssociationInformation(String unid) {
		boolean hadAssociationInformation = false;

		// Had Copyto information?
		// Do not check the associateDataMapXML in MailDocumentSelectionAction.getSugarDataCache()... because
		// .. when document is closed, MailDoucmentSelectionAction removes the entry from the SugarDataCache,
		// .. then the document is selected in the inbox view, and it triggered MailDoucmentSelectionAction selection changed logic which creates
		// a dummy entry in the SugarDataCache ( for performance reason), and later updates the entry with real Notes information.
		// Because the timing issue, we might be looking at the dummy entry in the SugarDataCache and might get incorrect associateDataMapXML information.
		if (NotesUIDocumentSaveListenerRepository.getDocumentActionMap() != null && NotesUIDocumentSaveListenerRepository.getDocumentActionMap().containsKey(unid)
				&& NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(unid) != null) {
			String assoicateDataMapXML = NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(unid).getAssociateDataMapXML();
			if (assoicateDataMapXML == null || assoicateDataMapXML.equals(ConstantStrings.EMPTY_STRING) || assoicateDataMapXML.equals(ConstantStrings.SPACE)) {
				// no assoication information ... no more process is needed
			} else {
				hadAssociationInformation = true;
			}
		}
		return hadAssociationInformation;
	}

	private boolean hasNewAssociation(String unid) {
		boolean hasNewAssociation = true;

		if (unid == null) {
			hasNewAssociation = false;
		} else
		// If user added new copyto information, a real runnable would have been created right after the copyto dialog is closed... So, if
		// the runnable is null, it indicates that user did not add copyto information.
		if (NotesUIDocumentSaveListenerRepository.getDocumentRunnableMap() != null && NotesUIDocumentSaveListenerRepository.getDocumentRunnableMap().containsKey(unid)
				&& NotesUIDocumentSaveListenerRepository.getDocumentRunnableMap().get(unid) == null) {
			hasNewAssociation = false;

		}
		return hasNewAssociation;

	}
	private NotesUIDocument getWorkbenchDoc(IWorkbenchPart part) {
		NotesUIDocument uidoc = null;
		if (part != null && part instanceof NotesViewPart) {
			NotesViewPart viewPart = (NotesViewPart) part;
			if (viewPart != null && viewPart.getNotesControl() != null && viewPart.getNotesControl() instanceof NotesSashControl) {
				NotesSashControl sashControl = (NotesSashControl) viewPart.getNotesControl();
				NotesOpenDocumentSummary openDoc = sashControl.getDocument();
				if (openDoc != null) {
					uidoc = (NotesUIDocument) openDoc;
				}
			}
		}
		return uidoc;

	}
	private String getWorkbenchUnid(IWorkbenchPart part) {
		String unid = null;
		if (part != null && part instanceof NotesViewPart) {
			NotesViewPart viewPart = (NotesViewPart) part;
			if (viewPart != null && viewPart.getNotesControl() != null && viewPart.getNotesControl() instanceof NotesSashControl) {
				NotesSashControl sashControl = (NotesSashControl) viewPart.getNotesControl();
				NotesOpenDocumentSummary openDoc = sashControl.getDocument();
				if (openDoc != null) {
					unid = openDoc.getUnid();
				}
			}
		}
		return unid;

	}
	private SugarEntrySurrogate getActionMapSugarEntry(String unid) {
		// don't look up MailDocumentSelectionAction.getSugarDataCache() for sugarEntry, because it might be null due to timing ( i.e. if partActivate got
		// activated a bit before partclose, and MaiLDocumentSelectionAction might create a dummy sugar entry in SugarDataCache)
		return NotesUIDocumentSaveListenerRepository.getDocumentActionMap().get(unid);
	}

	private void setIsCalendar(boolean b) {
		_isCalendar = b;
	}

}
