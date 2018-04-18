package com.ibm.socialcrm.notesintegration.connector;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.commons.json.JSONObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Geometry;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.progress.UIJob;

import com.ibm.notes.java.ui.documents.NotesUIDocument;
import com.ibm.socialcrm.notesintegration.connector.providers.AttachmentViewerContentProvider;
import com.ibm.socialcrm.notesintegration.connector.providers.AttachmentViewerLabelProvider;
import com.ibm.socialcrm.notesintegration.connector.providers.TypeaheadCollectionModel;
import com.ibm.socialcrm.notesintegration.connector.providers.TypeaheadContentProvider;
import com.ibm.socialcrm.notesintegration.connector.providers.TypeaheadLabelProvider;
import com.ibm.socialcrm.notesintegration.connector.util.AccountRedirectObj;
import com.ibm.socialcrm.notesintegration.connector.util.ConnectorUtil;
import com.ibm.socialcrm.notesintegration.connector.util.CopytoObject;
import com.ibm.socialcrm.notesintegration.connector.util.DocumentSugarItems;
import com.ibm.socialcrm.notesintegration.core.SugarEntrySurrogate;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateData;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateDataMap;
import com.ibm.socialcrm.notesintegration.ui.custom.SFAButtonWithX;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFACombo;

public class AssociateComposite {

	public static final String ASTERISK_LABEL_STRING = "* "; //$NON-NLS-1$

	private static final String WIDTH_PREFERENCE = "widthPreference"; //$NON-NLS-1$
	private static final String HEIGHT_PREFERENCE = "heightPreference"; //$NON-NLS-1$

	public static final int DEFAULT_WIDTH = 280;
	public static final int DEFAULT_HEIGHT = 313;

	private static final int WIDTH_MARGIN = 30;
	private static final int WIDTH_MARGIN_1 = 0;
	private static final int WIDTH_MARGIN_3 = 5;
	private static final int LEFT_WIDTH_MARGIN_4 = 40;
	private static final int RIGHT_WIDTH_MARGIN_4 = 40;
	private static final int HEIGHT_MARGIN = 10;
	private static final int HEIGHT_MARGIN_1 = 5;
	private static final int controlGapHeight = 1;

	private static final String COPYTO_TOOL_TIP_TEXT = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_REMOVE);

	private static String[] lineLabels = {ASTERISK_LABEL_STRING + UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CITY_LABEL_STRING),
			ASTERISK_LABEL_STRING + UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ACCOUNTS_LABEL_STRING),
			ASTERISK_LABEL_STRING + UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OPPORTUNITIES_LABEL_STRING),
			ASTERISK_LABEL_STRING + UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CONTACTS_LABEL_STRING),
			ASTERISK_LABEL_STRING + UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LEADS_LABEL_STRING),
			ASTERISK_LABEL_STRING + UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FIND_BY_LABEL_STRING),
			ASTERISK_LABEL_STRING + UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOOK_UP_LABEL_STRING),
			ASTERISK_LABEL_STRING + UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ATTACHMENTS_LABEL_STRING)};

	private static final String PROGRESS_BAR_ID = "progressBarId"; //$NON-NLS-1$

	private static final int FIND_BY_DOCUMENT_OPTION_INDEX = 4;
	private static final int FIND_BY_CONTACT_OPTION_INDEX = 0;

	private UIJob _removeProgressIndicatorUIjob = null;

	private boolean _doingLookup = false;

	private String separatorColor = "separatorColor"; //$NON-NLS-1$
	private String headerCompositeColor = "headerCompositeColor"; //$NON-NLS-1$
	private String titleColor = "titleColor"; //$NON-NLS-1$
	private String grayColor = "grayColor"; //$NON-NLS-1$

	private Font _helpTextFont = null;
	private Font _cityItalicFont = null;
	private Font _cityNormalFont = null;
	private Font _lookUpComboItalicFont = null;
	private Font _lookUpComboNormalFont = null;

	private Shell _AssociateCompositeShell = null;
	private Composite _parentComposite = null;
	public boolean _isOKOrCancelPressed = false;

	private Composite _container = null;
	private Composite _progressComposite = null;
	private Button _searchMyItemsButton = null;
	private Label _helpText = null;
	private SFACombo _findByCombo = null;
	private Label _headerLabel = null;
	private SFACombo _lookUpCombo = null;
	private ImageHyperlink _lookupImageLink = null;
	private Text _cityText = null;
	private Label _cityLabel = null;
	private Button _okBtn = null;
	private Button _cancelBtn = null;
	private Composite _attachmentAssociateWithContactsComposite = null;
	private CheckboxTableViewer _attachmentTblViewer = null;
	private Composite _copytoAreaComposite = null;
	private Composite _copytoComposite = null;
	private String[] _attachmentList = null;
	private String _txt = null;
	private Label _contactsLabel = null;

	private SelectionListener _okSelectionListener = null;
	private SelectionListener _cancelSelectionListener = null;
	private SelectionListener _searchMyItemsSelectionListener = null;
	private ModifyListener _findByModifyListener = null;
	private ModifyListener _cityModifyListener = null;
	private FocusListener _cityFocusListener = null;
	private KeyListener _lookUpComboKeyListener = null;
	private SelectionListener _lookUpSelectionListener = null;
	private ModifyListener _lookUpComboModifyListener = null;
	private FocusListener _lookUpComboFocusListener = null;
	private VerifyListener _lookUpComboVerifyListener = null;
	private MouseListener _lookUpComboMouseListener = null;
	// private MouseTrackListener _lookUpComboMouseTrackListener = null;
	// private MouseMoveListener _lookUpComboMouseMoveListener = null;
	private DisposeListener _lookUpComboDisposeListener = null;
	private Listener _lookUpComboResizeListener = null;

	private Listener _attachmentTblPaintListener = null;

	private TypeaheadCollectionModel _model = null;
	private TypeaheadLabelProvider _typeaheadLabelProvider = null;
	private TypeaheadContentProvider _typeaheadContentProvider = null;
	private List<String> _typeaheadFormattedResultsList = null;

	private NotesUIDocument _doc = null;
	private String _form = null;
	private AssociateDataMap _associateDataMap = null;
	private Display _display = null;

	private DocumentSugarItems _documentSugarItems = null;

	Map<String, SFAButtonWithX> _copytoRemoveButtons = new HashMap<String, SFAButtonWithX>();

	private AssociateData _toAssociateData = null;
	private SugarType _toSugarType = null;
	private boolean _isPrefill = false;
	private boolean _isToSetDefaultLookUp = true;

	private int _maxLabelWidth = -1;
	private int _maxLabelHeight = -1;
	private int _asteriskWidth = -1;

	private boolean _prevSearchMyitemsOption;

	final Timer timer = new Timer();
	private boolean _isAnyAssociateWithLead = false;

	protected AssociateComposite(Display display, NotesUIDocument doc, SugarEntrySurrogate associateSugarSurrogate, Map<SugarType, Set<SugarEntrySurrogate>> sugarDataCacheMap, String[] assigneeList,
			String form, AssociateData toAssociateData, SugarType toSugarType) {
		_display = display;

		_doc = doc;

		_form = form;

		if (associateSugarSurrogate != null) {
			_associateDataMap = getAssociateDataMap(associateSugarSurrogate.getAssociateDataMapXML());

			if (_associateDataMap != null) {
				_isAnyAssociateWithLead = _associateDataMap.isAnyAssociateWithLead();
			}

			// Calendar does not support attachment upload
			if (getAppType() != ConnectorUtil.CALENDAR_APP) {
				_attachmentList = associateSugarSurrogate.getAttachmentNames();
			}
		}

		// Create an object which handles the "Find By Document" option
		_documentSugarItems = new DocumentSugarItems(sugarDataCacheMap, assigneeList, getCurrAssociateDataMap());

		// From live text More action
		if (toAssociateData != null && toSugarType != null) {
			_isPrefill = true;
			_toAssociateData = toAssociateData;
			_toSugarType = toSugarType;
		}

		// UI
		_AssociateCompositeShell = createShell(display);

		_parentComposite = createParentComposite(_AssociateCompositeShell);
		createDialogArea(_parentComposite);
		createButtonsForButtonBar(_parentComposite);

		configureShell();
		addShellListeners();

		if (_isPrefill && !isAssociated()) {
			toAddToCopyto();
		}

		// Build the dropdown list for the Find By Document option and set UI to the Find By Document
		// default option.
		if (!_documentSugarItems.isAnySugarItems()) {
			buildNotesDocumentSugarItems();
		}

		_AssociateCompositeShell.layout(true);
		_AssociateCompositeShell.open();
		_AssociateCompositeShell.forceActive();

	}

	private void buildNotesDocumentSugarItems() {
		final String progressId = createProgressIndicator(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ITEM_COMPILING_PROGRESS_MESSAGE));

		// build the list - background job
		_documentSugarItems.getDocumentItems();

		_removeProgressIndicatorUIjob = new UIJob("Remove Progress Indicator") //$NON-NLS-1$
		{

			@Override
			public IStatus runInUIThread(IProgressMonitor arg0) {

				if (_documentSugarItems.getResultList() != null && !_documentSugarItems.getResultList().isEmpty() & getCollectionModel().getCopytoObjectMap() != null
						&& !getCollectionModel().getCopytoObjectMap().isEmpty()) {
					Iterator<String> it = getCollectionModel().getCopytoObjectMap().keySet().iterator();
					while (it.hasNext()) {
						_documentSugarItems.update(it.next(), true);
					}
				}
				if (_documentSugarItems.isAnySugarItems()) {
					// checking for dispose in case user hit cancel or close dialog
					if (_findByCombo != null && !_findByCombo.isDisposed()) {
						initFindByCombo();
					}
				}
				removeProgressIndicator(progressId);
				return Status.OK_STATUS;
			}
		};

		// Set rule so the job will be executed in the correct
		// order.
		_removeProgressIndicatorUIjob.setRule(UiUtils.DISPLAY_SUGAR_ITEM_BY_ID_JOB_RULE);
		_removeProgressIndicatorUIjob.schedule();
	}

	private boolean isAssociated() {
		boolean isAssociated = false;

		if (_toSugarType != null && _toAssociateData != null) {
			String type = _toSugarType.getParentType();
			String currId = _toAssociateData.getId();
			if (_associateDataMap != null && _associateDataMap.getMyMap() != null && _associateDataMap.getMyMap().containsKey(_associateDataMap.getWeightedType(type, true))) {
				List<AssociateData> associateDataList = (List<AssociateData>) _associateDataMap.getMyMap().get(_associateDataMap.getWeightedType(type, true));
				for (int i = 0; i < associateDataList.size(); i++) {
					if (associateDataList.get(i).getId() != null && associateDataList.get(i).getId().equalsIgnoreCase(currId)) {
						isAssociated = true;
						break;
					}
				}
			}
		}
		return isAssociated;
	}

	private Shell createShell(Display display) {
		Shell shell = new Shell(display /* Display.getDefault() */, SWT.RESIZE | SWT.TITLE | SWT.MIN | SWT.MAX | SWT.CLOSE | SWT.MODELESS);
		shell.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).create());
		shell.setLayoutData(GridDataFactory.fillDefaults().create());

		return shell;
	}

	private Composite createParentComposite(Shell shell) {
		Composite parentComposite = new Composite(shell, SWT.NONE);
		parentComposite.setLayout(GridLayoutFactory.fillDefaults().create());
		parentComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		return parentComposite;
	}

	public Shell getShell() {
		return _AssociateCompositeShell;
	}

	private void addShellListeners() {

		// Close the shell when the user presses ESC
		getShell().addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					if (_lookUpCombo != null && _lookUpCombo.getListVisible()) {
					} else {
						close();
					}
				}
			}
		});

		// Listener to listen for shell resize
		getShell().addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				// Don't save the preferences when the user maximizes the size
				if (!getShell().getMaximized()) {
					// Set the explicit size of the parent composite so that if
					// we add a progress composite, the shell
					// expands to accommodate the new widget rather than shrink
					// the parentComposite within the shell.
					Point point = getShell().getSize();
					((GridData) getShell().getLayoutData()).widthHint = point.x;
					((GridData) getShell().getLayoutData()).heightHint = point.y;

				}
			}
		});
	}

	private AssociateDataMap getAssociateDataMap(String associateDataMapXML) {
		AssociateDataMap associateDataMap = null;
		if (associateDataMapXML != null) {
			associateDataMap = ConnectorUtil.decode(associateDataMapXML);
		}
		return associateDataMap;
	}

	protected Control createDialogArea(Composite parent) {

		_container = new Composite(parent, SWT.NONE);

		_container.setLayout(GridLayoutFactory.fillDefaults().create());
		_container.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		GridData data = (GridData) _container.getLayoutData();
		GC gc = new GC(_container.getDisplay());
		data.widthHint = gc.getFontMetrics().getAverageCharWidth() * 100;
		gc.dispose();
		data.horizontalIndent = 0;
		data.verticalIndent = 0;
		data.grabExcessHorizontalSpace = true;
		_container.setLayoutData(data);

		_container.setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).spacing(SWT.DEFAULT, SWT.DEFAULT).numColumns(1).create());
		_container.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));
		if (_associateDataMap != null && (_associateDataMap.getMyMap() != null && !_associateDataMap.getMyMap().isEmpty())) {
			createHeaderComposite(_container);
			createSeparator(_container);
		}

		createBodyComposite(_container);

		createCopyToArea(_container);

		createPaddingSpaceArea(_container);

		createSeparator(_container);

		// create the progress bar at the bottom of the shell
		createProgressComposite();

		return parent;
	}

	private void createPaddingSpaceArea(Composite container) {
		Composite paddingComposite = new Composite(container, SWT.NONE);
		paddingComposite.setLayout(GridLayoutFactory.fillDefaults().margins(0, 12).numColumns(1).create());
		paddingComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		paddingComposite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

	}

	private void createSeparator(Composite container) {

		if (JFaceResources.getColorRegistry().get(separatorColor) == null) {
			JFaceResources.getColorRegistry().put(separatorColor, new RGB(176, 176, 176));
		}

		Label separator = new Label(_container, SWT.HORIZONTAL);
		separator.setLayoutData(GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 2).span(1, 1).grab(true, false).create());
		separator.setBackground(JFaceResources.getColorRegistry().get(separatorColor));

	}

	private void createHeaderComposite(Composite container) {
		if (JFaceResources.getColorRegistry().get(headerCompositeColor) == null) {
			JFaceResources.getColorRegistry().put(headerCompositeColor, new RGB(238, 238, 238));
		}

		Composite headerComposite = new Composite(container, SWT.NONE);
		Rectangle margins = Geometry.createDiffRectangle(LEFT_WIDTH_MARGIN_4, RIGHT_WIDTH_MARGIN_4 + getAsteriskWidth(), 15, 10);

		headerComposite.setLayout(GridLayoutFactory.fillDefaults().extendedMargins(margins).margins(WIDTH_MARGIN_1, 0).spacing(SWT.DEFAULT, SWT.DEFAULT).numColumns(2).create());
		headerComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		headerComposite.setBackground(JFaceResources.getColorRegistry().get(headerCompositeColor));

		// ==============================================
		Label headerAsteriskLabel = new Label(headerComposite, SWT.None);
		headerAsteriskLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(1, 1).create());
		headerAsteriskLabel.setText(ASTERISK_LABEL_STRING);
		headerAsteriskLabel.setVisible(false);
		headerAsteriskLabel.setBackground(headerComposite.getBackground());

		_headerLabel = new Label(headerComposite, SWT.NONE);
		// use 20 for hint height so it looks like it has equal space as spaces between Clients/oppty/contacts.
		_headerLabel.setLayoutData(GridDataFactory.fillDefaults().indent(1, 0).hint(SWT.DEFAULT, 20).create());
		_headerLabel.setText(getHeaderLabel());
		_headerLabel.setFont(SugarItemsDashboard.getInstance().getTableHeaderFont());
		_headerLabel.setBackground(headerComposite.getBackground());

		// ==============================================
		Map<String, List<AssociateData>> map = _associateDataMap.getMyMap();
		if (map != null) {
			boolean isFirst = true;
			Set<Entry<String, List<AssociateData>>> entryset = map.entrySet();
			Iterator<Entry<String, List<AssociateData>>> it = entryset.iterator();
			while (it.hasNext()) {
				Entry<String, List<AssociateData>> entry = (Entry<String, List<AssociateData>>) it.next();
				String type = entry.getKey().substring(1);
				List<AssociateData> names = entry.getValue();
				createAssociateHeaderItem(headerComposite, type, names);
			}

		}
	}

	private void createBodyComposite(Composite container) {

		Composite bodyComposite = new Composite(container, SWT.NONE);
		Rectangle margins = Geometry.createDiffRectangle(LEFT_WIDTH_MARGIN_4, RIGHT_WIDTH_MARGIN_4 + getAsteriskWidth(), 15, 0);
		// Set controLGapHeight to 1 so we can play with height gaps among
		// children via children's indent parameter
		bodyComposite.setLayout(GridLayoutFactory.fillDefaults().extendedMargins(margins).spacing(SWT.DEFAULT, controlGapHeight).numColumns(3).create());

		bodyComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		bodyComposite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

		// ==============================================
		// help text
		createPaddingControl(1, bodyComposite);
		_helpText = new Label(bodyComposite, SWT.WRAP);
		_helpText.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).hint(getMaxLabelWidth(), 2 * getMaxLabelHeight()).create());
		_helpText.setText(getHelpText());
		_helpText.setFont(getHelpTextFont());
		_helpText.setBackground(bodyComposite.getBackground());

		// padding empty lines between help text and Find my items only line
		createPaddingControl(3, bodyComposite);

		// ==============================================
		// Find my items only (aka. Search my items only)

		createPaddingControl(2, bodyComposite);

		_searchMyItemsButton = new Button(bodyComposite, SWT.CHECK);
		_searchMyItemsButton.setLayoutData(GridDataFactory.fillDefaults().hint(getMaxLabelWidth(), 1 * getMaxLabelHeight()).indent(15, 6 * controlGapHeight).create());
		_searchMyItemsButton.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SEARCH_MY_ITEMS_ONLY_LABEL_STRING));

		_searchMyItemsButton.setSelection(true);
		_prevSearchMyitemsOption = true;

		addSearchMyItemsListeners();

		_searchMyItemsButton.setBackground(bodyComposite.getBackground());

		createPaddingControl(3, bodyComposite);

		// ==============================================
		// Category (aka. Find by)

		Label findByAsteriskLabel = new Label(bodyComposite, SWT.None);
		findByAsteriskLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).indent(0, 4 * controlGapHeight).hint(getAsteriskWidth(), SWT.DEFAULT).span(1, 1).create());
		findByAsteriskLabel.setText(ASTERISK_LABEL_STRING);
		findByAsteriskLabel.setBackground(bodyComposite.getBackground());

		Label findByLabel = new Label(bodyComposite, SWT.None);
		findByLabel.setLayoutData(GridDataFactory.fillDefaults().hint(getMaxLabelWidth(), 1 * getMaxLabelHeight()).indent(1, 4 * controlGapHeight).align(SWT.LEFT, SWT.CENTER).span(1, 1).create());
		findByLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FIND_BY_LABEL_STRING));
		findByLabel.setBackground(bodyComposite.getBackground());

		_findByCombo = new SFACombo(bodyComposite, SWT.BORDER | SWT.READ_ONLY);

		_findByCombo.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).indent(15, 4 * controlGapHeight).span(1, 1).create());

		_findByCombo.setEnabled(true);

		initFindByCombo();

		addFindByListeners();

		// ==============================================
		// City
		createPaddingControl(1, bodyComposite);

		_cityLabel = new Label(bodyComposite, SWT.None);
		_cityLabel.setLayoutData(GridDataFactory.fillDefaults().hint(getMaxLabelWidth(), getMaxLabelHeight()).indent(1, 4 * controlGapHeight).align(SWT.LEFT, SWT.CENTER).span(1, 1).create());
		_cityLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CITY_LABEL_STRING));
		_cityLabel.setBackground(bodyComposite.getBackground());

		_cityText = new Text(bodyComposite, SWT.BORDER | SWT.SINGLE);
		_cityText.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).indent(15, 4 * controlGapHeight).span(1, 1).create());
		setCityEnabled(false);

		addCityListeners();

		// ==============================================
		// Item (aka. Look up)
		Label lookUpAsteriskLabel = new Label(bodyComposite, SWT.NONE);
		lookUpAsteriskLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).indent(0, 4 * controlGapHeight).hint(getAsteriskWidth(), SWT.DEFAULT).span(1, 1).create());
		lookUpAsteriskLabel.setText(ASTERISK_LABEL_STRING);
		lookUpAsteriskLabel.setBackground(bodyComposite.getBackground());

		Label lookUpLabel = new Label(bodyComposite, SWT.None);
		lookUpLabel.setLayoutData(GridDataFactory.fillDefaults().hint(getMaxLabelWidth(), getMaxLabelHeight()).indent(1, 4 * controlGapHeight).align(SWT.LEFT, SWT.CENTER).span(1, 1).create());
		lookUpLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOOK_UP_LABEL_STRING));
		lookUpLabel.setBackground(bodyComposite.getBackground());

		_lookUpCombo = new SFACombo(bodyComposite, SWT.DROP_DOWN | SWT.BORDER);
		_lookUpCombo.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).indent(15, 4 * controlGapHeight).grab(true, false).span(1, 1).create());

		_lookUpCombo.setEnabled(true);

		setLookUpDefaultText(true);

		addLookUpListeners();

		createTypeaheadProviders(bodyComposite);

		// ========================================
		if (isAttachmentExist()) {
			createAttachmentArea(bodyComposite);
		}

	}

	private String getHelpText() {
		String stringX = null;
		if (getAppType() == ConnectorUtil.MAIL_APP) {
			stringX = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ASSSOCIATE_HELP_TEXT_MESSAGE_STRING);
		} else if (getAppType() == ConnectorUtil.CALENDAR_APP) {
			stringX = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ASSSOCIATE_HELP_TEXT_MEETING_STRING);
		}
		return stringX;
	}

	private String getHeaderLabel() {
		String stringX = null;
		if (getAppType() == ConnectorUtil.MAIL_APP) {
			stringX = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ASSOCIATE_TO_EMAIL_LABEL_STRING);
		} else if (getAppType() == ConnectorUtil.CALENDAR_APP) {
			stringX = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ASSOCIATE_TO_MEETING_LABEL_STRING);
		}
		return stringX;
	}

	private String getCityText() {
		String stringX = ConstantStrings.EMPTY_STRING;
		if (_cityText != null && _cityText.getText() != null) {
			if (!(_cityText.getText().equals(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CITY_HELP_TEXT)))) {
				stringX = _cityText.getText();
			}
		}
		return stringX;
	}

	private void setCityDefaultText() {
		// If enabled, set help hint with light grey color, and be sure the background is reset to parent's
		_cityText.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CITY_HELP_TEXT));
		Color color = JFaceResources.getColorRegistry().get("sfaToggleUnpressedDisabledText"); //$NON-NLS-1$
		_cityText.setForeground(color);
		_cityText.setBackground(_cityText.getParent().getBackground());
		// set italic
		_cityText.setFont(getCityTextFont(true));

	}

	private void unsetCityDefaultText() {
		// ok, user is ready to enter a city value, we will clear up the help hint, set the foreground and background to regular color.
		_cityText.setText(ConstantStrings.EMPTY_STRING);
		Color color = _helpText.getForeground();
		_cityText.setForeground(color);
		_cityText.setBackground(_cityText.getParent().getBackground());
		// unset italic
		_cityText.setFont(getCityTextFont(false));
	}

	private void setCityDisableText() {
		// If disabled, set blank text and gray background
		_cityText.setText(ConstantStrings.EMPTY_STRING);
		Color color = JFaceResources.getColorRegistry().get("sfaTogglePressedEnabledBackground"); //$NON-NLS-1$
		_cityText.setForeground(color);
		_cityText.setBackground(color);
	}

	private void setLookUpDefaultText(boolean b) {

		if (b) {

			String text = getLookUpDefaultText();
			_lookUpCombo.setText(text);

			Color color = JFaceResources.getColorRegistry().get("sfaToggleUnpressedDisabledText"); //$NON-NLS-1$
			_lookUpCombo.setForeground(color);
			// set italic
			_lookUpCombo.setFont(getLookUpComboFont(true));
		} else
		// If disabled, empty the help hint
		{
			_lookUpCombo.setText(ConstantStrings.EMPTY_STRING);

		}

	}

	private String getLookUpDefaultText() {
		String text = ConstantStrings.EMPTY_STRING;
		if (_findByCombo.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CONTACT))) {
			text = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOOK_UP_CONTACT_HELP_TEXT);
		} else if (_findByCombo.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_LEAD))) {
			text = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOOK_UP_LEAD_HELP_TEXT);
		} else if (_findByCombo.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_OPPORTUNITY))) {
			text = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOOK_UP_OPPTY_HELP_TEXT);
		} else if (_findByCombo.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CLIENT))) {
			text = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOOK_UP_CLIENT_HELP_TEXT);
		} else if (isFindByDocument()) {
			text = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOOK_UP_DOCUMENT_HELP_TEXT);
		}

		return text;
	}

	private boolean isFindByDocument() {
		boolean isFindByDocument = false;
		if (_findByCombo != null
				&& (_findByCombo.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_ITEMFROMEMAIL)) || _findByCombo.getText().equalsIgnoreCase(
						UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_ITEMFROMCALENDAR)))) {
			isFindByDocument = true;
		}
		return isFindByDocument;
	}

	private void unsetLookUpDefaultText() {
		// ok, user is ready to enter an item value, we will clear up the help hint, set the foreground to regular color.
		_lookUpCombo.setText(ConstantStrings.EMPTY_STRING);
		Color color = _helpText.getForeground();
		_lookUpCombo.setForeground(color);
		// unset italic
		_lookUpCombo.setFont(getLookUpComboFont(false));

	}

	private void createAttachmentArea(Composite bodyComposite) {

		// ==============================================
		// Include all attachments label
		createPaddingControl(1, bodyComposite);

		Label attachmentsLabel = new Label(bodyComposite, SWT.None);
		attachmentsLabel.setLayoutData(GridDataFactory.fillDefaults().hint(getMaxLabelWidth(), getMaxLabelHeight()).indent(1, 6 * controlGapHeight).align(SWT.LEFT, SWT.TOP).span(1, 1).create());
		attachmentsLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ATTACHMENTS_LABEL_STRING));
		attachmentsLabel.setBackground(bodyComposite.getBackground());

		attachmentsLabel.setEnabled(true);

		if (_associateDataMap != null) {
			Label uploadAlready = new Label(bodyComposite, SWT.NONE);
			uploadAlready.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).indent(15, 4 * controlGapHeight).span(1, 1).create());
			// if (_isAnyAssociateWithLead) {
			// uploadAlready.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ATTACHMENTS_UPLOADED_NOTSUPPORTED_FOR_LEAD));
			// } else {
			uploadAlready.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ATTACHMENTS_UPLOADED_ALREADY));
			// }
			uploadAlready.setBackground(bodyComposite.getBackground());
		}

		else {
			// attachment file list
			// grab(true, false) will remove vertical scroll bar... don't do it
			_attachmentTblViewer = CheckboxTableViewer.newCheckList(bodyComposite, SWT.BORDER | SWT.FULL_SELECTION);
			_attachmentTblViewer.getTable().setLayoutData(
					GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).indent(15, 4 * controlGapHeight).hint(SWT.DEFAULT, 60).span(1, 1).create());

			addAttachmentTblListeners();

			_attachmentTblViewer.setContentProvider(new AttachmentViewerContentProvider());
			_attachmentTblViewer.setLabelProvider(new AttachmentViewerLabelProvider());

			// Trigger intput change
			_attachmentTblViewer.setInput(_attachmentList);

			// Update attachment file check box
			updateAttachmentTblViewerCheckbox();

			// ==============================================
			// attachment with contacts label

			_attachmentAssociateWithContactsComposite = new Composite(bodyComposite, SWT.NONE);
			// Set controLGapHeight to 1 so we can play with height gaps among
			// children via children's indent parameter
			_attachmentAssociateWithContactsComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).create());
			_attachmentAssociateWithContactsComposite.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).grab(true, true).create());
			_attachmentAssociateWithContactsComposite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

			// dummy padding to keep columns aligned with columns of the other Composites
			Label paddingLabel = new Label(_attachmentAssociateWithContactsComposite, SWT.None);
			paddingLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(getAsteriskWidth(), getMaxLabelHeight()).indent(0, 4 * controlGapHeight).span(1, 1).create());
			paddingLabel.setBackground(_attachmentAssociateWithContactsComposite.getBackground());
			paddingLabel.setVisible(false);
			// label
			Label paddingLabel1 = new Label(_attachmentAssociateWithContactsComposite, SWT.None);
			paddingLabel1.setLayoutData(GridDataFactory.fillDefaults().hint(getMaxLabelWidth(), getMaxLabelHeight()).indent(1, 10 * controlGapHeight).align(SWT.LEFT, SWT.TOP).span(1, 1).create());
			paddingLabel1.setBackground(_attachmentAssociateWithContactsComposite.getBackground());
			paddingLabel1.setVisible(false);

			//      

			_contactsLabel = new Label(_attachmentAssociateWithContactsComposite, SWT.NONE | SWT.WRAP);
			_contactsLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ATTACHMENT_WITH_CONTACTS_LABEL_STRING));
			_contactsLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).indent(WIDTH_MARGIN_3, 4 * controlGapHeight).span(1, 1).create());
			_contactsLabel.setBackground(bodyComposite.getBackground());
			updateAttachmentContactsLabel();

			if (_associateDataMap != null) {
				_attachmentTblViewer.getTable().setEnabled(false);
			} else {
				_attachmentTblViewer.getTable().setEnabled(true);
			}

			_attachmentTblViewer.getTable().layout(true);
			_attachmentTblViewer.getTable().update();
		}
	}

	private void updateAttachmentTblViewerCheckbox() {
		if (_attachmentTblViewer != null && isAttachmentExist() && _associateDataMap == null) {
			for (int i = 0; i < _attachmentList.length; i++) {
				if (_attachmentList[i].substring(0, 1).equalsIgnoreCase(UiUtils.ATTACHMENT_IS_SELECTED)) {
					_attachmentTblViewer.setChecked(_attachmentList[i], false);
				}
			}
		}
	}

	private void updateAttachmentContactsLabel() {
		if (_findByCombo.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CONTACT))) {
			_contactsLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ATTACHMENT_WITH_CONTACTS_LABEL_STRING));
			_attachmentAssociateWithContactsComposite.setVisible(true);
			((GridData) (_attachmentAssociateWithContactsComposite.getLayoutData())).exclude = false;
			if (_associateDataMap != null) {
				_attachmentAssociateWithContactsComposite.setEnabled(false);
			} else {
				_attachmentAssociateWithContactsComposite.setEnabled(true);
			}
			// enable attachment in case it was disabled
			_attachmentTblViewer.getTable().setEnabled(true);
		} else if (_findByCombo.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_LEAD))) {
			// display not support lead text
			_contactsLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ATTACHMENTS_UPLOADED_NOTSUPPORTED_FOR_LEAD));
			_attachmentAssociateWithContactsComposite.setVisible(true);
			((GridData) (_attachmentAssociateWithContactsComposite.getLayoutData())).exclude = false;
			if (_associateDataMap != null) {
				_attachmentAssociateWithContactsComposite.setEnabled(false);
			} else {
				_attachmentAssociateWithContactsComposite.setEnabled(true);
			}
			// disable attachment
			_attachmentTblViewer.getTable().setEnabled(false);
		} else {
			((GridData) (_attachmentAssociateWithContactsComposite.getLayoutData())).exclude = true;
			// enable attachment in case it was disabled
			_attachmentTblViewer.getTable().setEnabled(true);
		}

		getShell().pack(true);
		getShell().layout(true);
	}

	private void createCopyToArea(Composite container) {

		_copytoAreaComposite = new Composite(container, SWT.NONE);
		Rectangle margins = Geometry.createDiffRectangle(LEFT_WIDTH_MARGIN_4, RIGHT_WIDTH_MARGIN_4 + getAsteriskWidth(), 0, 0);
		// Set controLGapHeight to 1 so we can play with height gaps among
		// children via children's indent parameter
		_copytoAreaComposite.setLayout(GridLayoutFactory.fillDefaults().extendedMargins(margins).margins(WIDTH_MARGIN_1, 0).spacing(SWT.DEFAULT, controlGapHeight).numColumns(3).create());

		_copytoAreaComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		_copytoAreaComposite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

		// ==============================================
		// Copy to area
		// 
		// dummy padding to keep columns aligned with columns of the other Composites
		Label paddingLabel = new Label(_copytoAreaComposite, SWT.None);
		paddingLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).indent(0, 4 * controlGapHeight).span(1, 1).create());
		paddingLabel.setText(ASTERISK_LABEL_STRING);
		paddingLabel.setBackground(_copytoAreaComposite.getBackground());
		paddingLabel.setVisible(false);

		// label
		Label copytoLabel = new Label(_copytoAreaComposite, SWT.None);
		copytoLabel.setLayoutData(GridDataFactory.fillDefaults().hint(getMaxLabelWidth(), getMaxLabelHeight()).indent(1, 10 * controlGapHeight).align(SWT.LEFT, SWT.TOP).span(1, 1).create());
		copytoLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.COPYTO_LABEL_STRING));
		copytoLabel.setBackground(_copytoAreaComposite.getBackground());

		// copyto wrap around list
		// grab(true, false) will remove vertical scroll bar... don't do it
		_copytoComposite = new Composite(_copytoAreaComposite, SWT.NONE);
		_copytoComposite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).indent(10, 4 * controlGapHeight).span(1, 1).create());

		_copytoComposite.setBackground(_copytoAreaComposite.getBackground());

		RowLayout layout = new RowLayout();
		layout.wrap = true;
		layout.pack = true;
		_copytoComposite.setLayout(layout);

		createCopytoWraparounds();

	}

	private void createCopytoWraparounds() {

		if (getCollectionModel() != null && getCollectionModel().getCopytoObjectMap() != null && !getCollectionModel().getCopytoObjectMap().isEmpty()) {
			createCopytoWidgets();
			setCopytoVisible(true);
		} else {
			setCopytoVisible(false);
		}

		// don't layout/pack/update _copytoAreaComposite here, it will resize copyto widgets strangely.

		getShell().pack(true);
		getShell().layout(true);
	}

	private void createCopytoWidgets() {

		if (getCollectionModel() != null && getCollectionModel().getCopytoObjectMap() != null && !getCollectionModel().getCopytoObjectMap().isEmpty()) {
			Iterator<CopytoObject> it = getCollectionModel().getCopytoObjectMap().values().iterator();
			while (it.hasNext()) {
				CopytoObject co = it.next();
				if (co.getObjectKey() != null) {
					if (_copytoRemoveButtons != null && !_copytoRemoveButtons.isEmpty() && _copytoRemoveButtons.containsKey(co.getObjectKey())) {
						// do nothing
					} else {
						SFAButtonWithX widget = new SFAButtonWithX(_copytoComposite, SWT.NONE, co.getDisplayName(), COPYTO_TOOL_TIP_TEXT, new CopytoRemoveAction(co.getObjectKey(), this));
						_copytoRemoveButtons.put(co.getObjectKey(), widget);

					}
				}

			}
		}

		_copytoComposite.layout(true, true);
		_copytoComposite.getParent().layout(true, true);
		_copytoAreaComposite.layout(true, true);
		_copytoAreaComposite.getParent().layout(true, true);
		getShell().layout(true, true);
	}

	public void removeCopyto(String id) {

		Map<String, CopytoObject> coMap = getCollectionModel().getCopytoObjectMap();
		if (coMap != null && !coMap.isEmpty() && coMap.containsKey(id)) {
			getCollectionModel().removeCopytoObjectMap(id);
			_copytoRemoveButtons.get(id).dispose();
			_copytoRemoveButtons.remove(id);
			createCopytoWraparounds();

			updateLookupBackendList(id, false);

			initFindByCombo();

			if (getCollectionModel().getCopytoObjectMap() == null || getCollectionModel().getCopytoObjectMap().isEmpty()) {
				if (_okBtn != null && !_okBtn.isDisposed()) {
					_okBtn.setEnabled(false);
				}
			}
		}
	}

	private void setCopytoVisible(boolean b) {
		if (_copytoAreaComposite != null && !_copytoAreaComposite.isDisposed()) {
			if (!b) {
				((GridData) (_copytoAreaComposite.getLayoutData())).exclude = true;
			} else {
				((GridData) (_copytoAreaComposite.getLayoutData())).exclude = false;
			}
			_copytoAreaComposite.setVisible(b);
			getShell().layout(true);
		}
	}

	private void addSearchMyItemsListeners() {
		_searchMyItemsSelectionListener = new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// 43235 - make checkbox stick
				_prevSearchMyitemsOption = _searchMyItemsButton.getSelection();
			}

		};
		_searchMyItemsButton.addSelectionListener(_searchMyItemsSelectionListener);
	}

	private void addFindByListeners() {
		_findByModifyListener = new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				if (_findByCombo.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CLIENT))) {
					setCityEnabled(true);
				} else {
					setCityEnabled(false);
				}

				if (_attachmentTblViewer != null && isAttachmentExist()) {
					updateAttachmentContactsLabel();
				}

				if (isFindByDocument()) {
					initLookUpCombo();
				} else {
					_lookUpCombo.removeAll();
				}
				initMyItems();

				if (_isToSetDefaultLookUp) {
					setLookUpDefaultText(true);
				} else {
					// sometimes we don't want to set default.
					// For Example: when in Find By Document option, user clicks the drop down line which
					// redirect to Account typeahead process (if there are too many Account items for this
					// given account name). No need to set default here, actually, due to timing issue,
					// setting default here might cause incorrect font being displayed.
					_isToSetDefaultLookUp = true;
				}

				// set flag to false so lookup will proceed
				setDoingLookup(false);

			}

		};
		_findByCombo.addModifyListener(_findByModifyListener);
	}

	private void addCityListeners() {
		_cityModifyListener = new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent arg0) {
				_isPrefill = false;
			}
		};
		_cityText.addModifyListener(_cityModifyListener);

		_cityFocusListener = new FocusListener() {
			public void focusGained(FocusEvent event) {
				if (_cityText.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CITY_HELP_TEXT))) {
					unsetCityDefaultText();
				}
			}

			public void focusLost(FocusEvent event) {
				if (_cityText.getText().equalsIgnoreCase(ConstantStrings.EMPTY_STRING)) {
					setCityDefaultText();
				}

			}

		};
		_cityText.addFocusListener(_cityFocusListener);

	}

	private void addLookUpListeners() {

		_lookUpSelectionListener = new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				// If lookup dropdown list is still visible => user uses up/down keyboard keys going
				// through the list... we are not going to copy the item to copyto area until
				// user makes decision and presses the Enter key (KeyAdapter listener).
				// If lookup dropdown list is not visible => user uses mouse selecting the
				// current item, we will copy this itme to copyto area.
				// 
				if (!_lookUpCombo.getListVisible()) {
					if (isToCopyto()) {
						toAddToCopyto();
					}
				}

				_isPrefill = false;
			}

		};
		_lookUpCombo.addSelectionListener(_lookUpSelectionListener);

		_lookUpComboModifyListener = new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent arg0) {
				_txt = _lookUpCombo.getText();
				String dropdownselected = null;
				if (_lookUpCombo.getSelectionIndex() > -1) {
					dropdownselected = _lookUpCombo.getItems()[_lookUpCombo.getSelectionIndex()];
				}

				_isPrefill = false;

				if (_txt != null && !_txt.equals(dropdownselected)) {
					typeaheadSuggestionAction();
				}
			}

		};
		_lookUpCombo.addModifyListener(_lookUpComboModifyListener);

		// Add focuslistener so we can add help hint in the lookup combo field
		_lookUpComboFocusListener = new FocusListener() {
			public void focusGained(FocusEvent event) {
				if (_lookUpCombo.getText().equalsIgnoreCase(getLookUpDefaultText())) {
					unsetLookUpDefaultText();
				}
			}

			public void focusLost(FocusEvent event) {
				if (_lookUpCombo.getText().equalsIgnoreCase(ConstantStrings.EMPTY_STRING)) {
					setLookUpDefaultText(true);
				}

			}

		};
		_lookUpCombo.addFocusListener(_lookUpComboFocusListener);

		_lookUpComboMouseListener = new MouseListener() {

			@Override
			public void mouseDoubleClick(MouseEvent mouseevent) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent mouseevent) {
				if (isFindByDocument() && _lookUpCombo != null && _lookUpCombo.getListVisible()) {
					setLookUpDefaultText(false);
				}

			}

			@Override
			public void mouseUp(MouseEvent mouseevent) {
				// if (_lookUpCombo.getText() != null && !_lookUpCombo.getText().equals(ConstantStrings.EMPTY_STRING) && !_lookUpCombo.getText().equals(getLookUpDefaultText())) {
				// if (isToCopyto()) {
				// toAddToCopyto();
				// }
				// }

			}

		};

		_lookUpCombo.addMouseListener(_lookUpComboMouseListener);

		_lookUpComboKeyListener = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent arg0) {

				_isPrefill = false;

				if (arg0.keyCode == SWT.ESC) {
					_lookUpCombo.removeAll();
					setLookUpDefaultText(true);
					initFindByCombo();
					_findByCombo.setFocus();
				}

				else if (arg0.keyCode == SWT.ARROW_LEFT || arg0.keyCode == SWT.ARROW_RIGHT || arg0.keyCode == SWT.HOME || arg0.keyCode == SWT.END || arg0.keyCode == SWT.ARROW_DOWN
						|| arg0.keyCode == SWT.ARROW_UP || arg0.keyCode == SWT.CR || arg0.keyCode == SWT.KEYPAD_CR || arg0.keyCode == SWT.CANCEL) {
					// user selected a typeahead value, add it to copyto list
					if (arg0.keyCode == SWT.CR) {
						if (isToCopyto()) {

							toAddToCopyto();
						}
					}
				}

				else {
					typeaheadSuggestionAction();

				}
			}
		};

		_lookUpCombo.addKeyListener(_lookUpComboKeyListener);

		_lookUpComboVerifyListener = new VerifyListener() {

			@Override
			public void verifyText(VerifyEvent arg0) {
				if (!isFindByDocument()) {

					String[] strings = _lookUpCombo.getItems();

					if (strings != null && strings.length > 0) {
						for (int i = 0; i < strings.length; i++) {
							if (strings[i].equals(arg0.text)) {

								if (_lookUpCombo.getSelectionIndex() == -1) {

									_lookUpCombo.select(i);

								}

								_isPrefill = false;
								arg0.doit = true;
							}
						}
					}
				}
			}
		};

		_lookUpCombo.addVerifyListener(_lookUpComboVerifyListener);

		_lookUpComboResizeListener = new Listener() {
			public void handleEvent(Event e) {

				if (isFindByDocument()) {
					List<String> resultList = _documentSugarItems.getFormattedResultList(_lookUpCombo.getBounds().width - 30);
					if (resultList != null && !resultList.isEmpty()) {
						String[] strings = (String[]) resultList.toArray(new String[resultList.size()]);
						updateComboDropdownList(strings);
					}
				}

			}
		};

		_lookUpCombo.addListener(SWT.Resize, _lookUpComboResizeListener);

		_lookUpComboDisposeListener = new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				// Kill the timer thread when we exit the dialog.
				timer.cancel();
			}
		};
		_lookUpCombo.addDisposeListener(_lookUpComboDisposeListener);

	}

	private void addAttachmentTblListeners() {
		final Table tbl = _attachmentTblViewer.getTable();

		if (!Display.getDefault().getHighContrast()) {
			_attachmentTblPaintListener = new Listener() {

				@Override
				public void handleEvent(Event event) {
					TableItem item = (TableItem) event.item;
					int itemIndex = tbl.indexOf(item);

					GC gc = event.gc;

					gc.setAlpha(50);
					Rectangle area = tbl.getClientArea();
					/*
					 * Need to adjust clipping area so the color will fill the table row.
					 */
					int width = area.x + area.width - event.x;
					if (width > 0) {
						Region region = new Region();
						gc.getClipping(region);
						region.add(0, event.y, width + event.x, event.height);
						gc.setClipping(region);
						region.dispose();
					}

					// Paint gray on the even line
					if (((itemIndex + 1) % 2) == 0) {
						Rectangle rec = event.getBounds();

						if (JFaceResources.getColorRegistry().get(grayColor) == null) {
							JFaceResources.getColorRegistry().put(grayColor, Display.getDefault().getSystemColor(SWT.COLOR_GRAY).getRGB());
						}

						gc.setBackground(JFaceResources.getColorRegistry().get(grayColor));
						gc.fillRectangle(0, rec.y, width + rec.x, rec.height);

					}

					// don't need to dispose gc because it was passed in, we did not create it
					// gc.dispose();

				}
			};
			tbl.addListener(SWT.PaintItem, _attachmentTblPaintListener);
		}

	}

	private void addOKButtonListeners() {

		_okSelectionListener = new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				okPressed();
			}

		};
		_okBtn.addSelectionListener(_okSelectionListener);
	}

	private void addCancelButtonListeners() {

		_cancelSelectionListener = new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (getCollectionModel() != null && getCollectionModel().getCopytoObjectMap() != null) {
					getCollectionModel().getCopytoObjectMap().clear();
				}
				close();
			}

		};
		_cancelBtn.addSelectionListener(_cancelSelectionListener);
	}

	private boolean isAttachmentExist() {
		boolean isAttachment = true;
		if (_attachmentList == null || _attachmentList.length == 0) {
			isAttachment = false;
		}
		return isAttachment;
	}

	private void setCityEnabled(boolean b) {
		if (_cityLabel != null && !_cityLabel.isDisposed()) {
			_cityLabel.setEnabled(b);
		}
		if (_cityText != null && !_cityText.isDisposed()) {
			_cityText.setEnabled(b);
			if (b) {
				setCityDefaultText();
			} else {
				setCityDisableText();
			}

		}
	}

	private void toAddToCopyto() {
		AssociateData associateData = null;
		String type = null;
		SugarType sugarType = null;

		if (_isPrefill) {
			associateData = _toAssociateData;
			type = _toSugarType.getType();
			sugarType = _toSugarType;
		} else {
			if (isFindByDocument()) {
				int i = _lookUpCombo.getSelectionIndex() == -1 ? 0 : _lookUpCombo.getSelectionIndex();
				_documentSugarItems.setCurrSelectIndex(i);
				type = _documentSugarItems.getCurrType(i);
				sugarType = _documentSugarItems.getCurrSugarType(i);
				associateData = new AssociateData(_documentSugarItems.getCurrAssociatedResult(i), _documentSugarItems.getCurrExtendedResult(i), _documentSugarItems.getCurrId(i), false);
			} else {
				associateData = getCurrTypeAheadResult(_lookUpCombo.getSelectionIndex() == -1 ? 0 : _lookUpCombo.getSelectionIndex());
				type = _findByCombo.getText();
				sugarType = getCollectionModel().getCacheSugarType();
			}
		}

		if (associateData != null) {

			_lookUpCombo.removeModifyListener(_lookUpComboModifyListener);
			_lookUpCombo.removeVerifyListener(_lookUpComboVerifyListener);

			// Don't save the preferences when the user maximizes the size
			Point currPoint = null;
			if (!getShell().getMaximized()) {
				// Set the explicit size of the parent composite so that if
				// we add a progress composite, the shell
				// expands to accommodate the new widget rather than shrink
				// the parentComposite within the shell.
				currPoint = getShell().getSize();

			}

			if (!isCopytoObjectExisted(new CopytoObject(associateData, type, sugarType))) {
				getCollectionModel().addCopytoObjectMap(associateData, type, sugarType);
				createCopytoWraparounds();

			}

			updateLookupBackendList(associateData.getId(), true);

			_lookUpCombo.clearSelection();
			_lookUpCombo.deselectAll();
			_lookUpCombo.removeAll();
			_lookUpCombo.addModifyListener(_lookUpComboModifyListener);
			_lookUpCombo.addVerifyListener(_lookUpComboVerifyListener);

			initFindByCombo();
			setCityDisableText();
			_findByCombo.setFocus();
			_okBtn.setEnabled(true);

		}
	}

	private void updateLookupBackendList(String id, boolean toRemoveFromDropdown) {
		_documentSugarItems.update(id, toRemoveFromDropdown);

	}

	private boolean isCopytoObjectExisted(CopytoObject co) {
		boolean isExisted = false;
		if (co != null && co.getObjectKey() != null && co.getObjectKey() != null && getCollectionModel() != null && getCollectionModel().getCopytoObjectMap() != null
				&& !getCollectionModel().getCopytoObjectMap().isEmpty() && getCollectionModel().getCopytoObjectMap().containsKey(co.getObjectKey())) {
			isExisted = true;
		}

		return isExisted;
	}

	private void initMyItems() {
		boolean isEnable = true;
		if (isFindByDocument()) {
			isEnable = false;
		}
		_searchMyItemsButton.setEnabled(isEnable);

		if (isFindByDocument()) {
			_searchMyItemsButton.setSelection(false);
		} else if (!isFindByDocument()) {
			_searchMyItemsButton.setSelection(_prevSearchMyitemsOption);
		}
	}

	private void initFindByCombo() {
		Vector<String> v = new Vector<String>();

		v.add(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CONTACT));
		v.add(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_OPPORTUNITY));
		v.add(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CLIENT));
		v.add(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_LEAD));
		// _findByCombo.select(0);

		// sugar item(s) in current document
		if (_documentSugarItems != null && _documentSugarItems.isAnySugarItems()) {
			if (getAppType() == ConnectorUtil.MAIL_APP) {
				v.add(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_ITEMFROMEMAIL));
			} else if (getAppType() == ConnectorUtil.CALENDAR_APP) {
				v.add(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_ITEMFROMCALENDAR));
			}
		}
		_findByCombo.setItems(v.toArray(new String[v.size()]));
		_findByCombo.select(0);
		_findByCombo.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

		// When modifying _findByCombo selection, it triggers _findByCombo
		// modify listener's modifyText()
		setDefaultFindByCombo();

	}

	private void setDefaultFindByCombo() {
		// sugar item(s) in current document
		if (_documentSugarItems != null && _documentSugarItems.isAnySugarItems()) {
			_findByCombo.select(FIND_BY_DOCUMENT_OPTION_INDEX);
		} else {
			_findByCombo.select(FIND_BY_CONTACT_OPTION_INDEX);
		}

	}

	private void initLookUpCombo() {
		if (isFindByDocument()) {
			List<String> resultList = _documentSugarItems.getFormattedResultList(_lookUpCombo.getBounds().width - 30);
			if (resultList != null && !resultList.isEmpty()) {
				String[] strings = (String[]) resultList.toArray(new String[resultList.size()]);
				updateComboDropdownList(strings);
			}

		}
	}

	private void initLookUpImageLink() {
		boolean isEnable = true;

		if (isFindByDocument()) {
			isEnable = false;
		}
		_lookupImageLink.setVisible(isEnable);
		_lookupImageLink.setEnabled(isEnable);
	}

	private boolean isToCopyto() {
		boolean toCopyto = true;
		if (isFindByDocument()) {

			if (_lookUpCombo != null && _lookUpCombo.getText() != null) {
				if (!isADropDownItem(_lookUpCombo.getText())) {
					toCopyto = false;
				} else {
					AccountRedirectObj accountRedirect = new AccountRedirectObj(_lookUpCombo.getText());
					if (accountRedirect.isRedirect()) {
						toCopyto = false;
						_isToSetDefaultLookUp = false;

						_findByCombo.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CLIENT));

						_lookUpCombo.setText(accountRedirect.getAccountName());
						_lookUpCombo.setFocus();
						setInput(false, SugarType.ACCOUNTS, ConstantStrings.EMPTY_STRING, accountRedirect.getAccountName());
					}
				}
			}
		}
		return toCopyto;
	}

	private boolean isADropDownItem(String s) {
		boolean isADropDownItem = false;
		if (s != null && _lookUpCombo.getItems() != null && _lookUpCombo.getItems().length > 0) {
			for (int i = 0; i < _lookUpCombo.getItems().length; i++) {
				if (s.equals(_lookUpCombo.getItems()[i])) {
					isADropDownItem = true;
					break;
				}
			}
		}
		return isADropDownItem;
	}

	private void setInput(final boolean isMyItemsOnly, final SugarType sugarType, final String city, final String txt) {
		Job setinputJob = new Job("setinputJob") //$NON-NLS-1$
		{

			@Override
			public IStatus run(IProgressMonitor arg0) {

				getCollectionModel().setIsMyItemsOnly(isMyItemsOnly);
				getCollectionModel().setCacheSugarType(sugarType);
				getCollectionModel().setCacheCity(city == null ? ConstantStrings.EMPTY_STRING : city.trim());

				// sleep a bit more so we can get the latest combo text user entered
				try {
					Thread.sleep(50);
				} catch (Exception e) {
					e.printStackTrace();
				}
				// the passed in txt might not be recent enough, so, use _txt again.
				getCollectionModel().setCacheText(_txt == null ? ConstantStrings.EMPTY_STRING : _txt.trim());

				// if current txt is too short, no need to call search API
				if (getCollectionModel().getCacheText() == null || getCollectionModel().getCacheText().isEmpty() || getCollectionModel().getCacheText().length() < 2) {
					setDoingLookup(false);
					return Status.OK_STATUS;
				}

				getCollectionModel().setWSResults(null);

				_typeaheadContentProvider.getElements(getCollectionModel());

				if (_typeaheadFormattedResultsList == null) {
					_typeaheadFormattedResultsList = new ArrayList<String>();
				} else {
					_typeaheadFormattedResultsList.clear();
				}

				if (getCollectionModel().getWSResults() != null) {
					for (int i = 0; i < getCollectionModel().getWSResults().length; i++) {
						String txt = _typeaheadLabelProvider.getText(getCollectionModel().getWSResults()[i]);
						if (txt != null) {
							_typeaheadFormattedResultsList.add(txt);
						}

					}
				}

				UIJob updateComboUIJob = new UIJob("updateComboUIJob") //$NON-NLS-1$
				{

					@Override
					public IStatus runInUIThread(IProgressMonitor arg0) {
						if (_lookUpCombo.isDisposed()) {
							return Status.OK_STATUS;
						}

						String[] strings = null;
						if (_typeaheadFormattedResultsList.size() == 0) {
							strings = new String[]{UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_RELATED_TYPE_NOMATCH_MSG)};
						} else {
							strings = (String[]) _typeaheadFormattedResultsList.toArray(new String[_typeaheadFormattedResultsList.size()]);
						}

						// Preserve current text and position
						String currTxt = _lookUpCombo.getText();
						Point point = _lookUpCombo.getSelection();

						_lookUpCombo.removeAll();
						updateComboDropdownList(strings);

						_lookUpCombo.setListVisible(true);

						_lookUpCombo.setFocus();

						// Restore text user entered
						_lookUpCombo.setText(currTxt);

						_lookUpCombo.setSelection(point);

						// acw
						_lookUpCombo.setEnabled(true);
						_lookUpCombo.redraw();

						setDoingLookup(false);

						return Status.OK_STATUS;
					}

				};
				updateComboUIJob.schedule();
				return Status.OK_STATUS;
			}

		};
		setinputJob.setSystem(true);
		setinputJob.schedule();
	}

	private void createAssociateHeaderItem(Composite headerComposite, final String type, List<AssociateData> items /*
																													 * , boolean isFirst
																													 */) {
		Composite associateHeaderComposite = new Composite(headerComposite, SWT.NONE);
		Rectangle margins = null;
		// space between each sugar types
		margins = Geometry.createDiffRectangle(0, 0, 3, 0);

		associateHeaderComposite.setLayout(GridLayoutFactory.fillDefaults().extendedMargins(margins).spacing(0, SWT.DEFAULT).numColumns(3).create());
		associateHeaderComposite.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).indent(0, 0).grab(true, false).create());
		associateHeaderComposite.setBackground(headerComposite.getBackground());

		Label accountAsteriskLabel = new Label(associateHeaderComposite, SWT.None);
		accountAsteriskLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(1, 1).create());
		accountAsteriskLabel.setText(ASTERISK_LABEL_STRING);
		accountAsteriskLabel.setVisible(false);

		Label accountsLabel = new Label(associateHeaderComposite, SWT.NONE);
		accountsLabel.setLayoutData(GridDataFactory.fillDefaults().hint(getMaxLabelWidth(), getMaxLabelHeight()).indent(1, HEIGHT_MARGIN_1).span(1, 1).create());
		accountsLabel.setBackground(headerComposite.getBackground());
		accountsLabel.setText(getTypeWithColon(type));

		Composite linkComposite = new Composite(associateHeaderComposite, SWT.NONE);
		linkComposite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).indent(10, 0).span(1, 1).create());
		linkComposite.setBackground(associateHeaderComposite.getBackground());

		RowLayout layout = new RowLayout();
		layout.wrap = true;
		layout.pack = true;
		linkComposite.setLayout(layout);

		for (int i = 0; i < items.size(); i++) {

			final Hyperlink accountsLink = new Hyperlink(linkComposite, SWT.NONE);
			accountsLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
			accountsLink.setBackground(headerComposite.getBackground());
			StringBuffer sb = new StringBuffer(ConnectorUtil.getFormattedName(items.get(i).getName()));
			sb.append((i == (items.size() - 1)) ? ConstantStrings.EMPTY_STRING : ConstantStrings.COMMA);
			accountsLink.setText(sb.toString());
			accountsLink.setLayoutData(new RowData());

			accountsLink.pack();

			final String id = items.get(i).getId();

			if (id != null && !id.equalsIgnoreCase(ConstantStrings.EMPTY_STRING)) {
				final HyperlinkAdapter accountsLinkListener = new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent evt) {
						if (type.equalsIgnoreCase(SugarType.LEADS.getParentType())) {
							/*
							 * this is sample url: https://sugarCRMURL/#Leads/7117887912b362a
							 */
							String module_name = SugarType.LEADS.getParentType();
							if (module_name != null) {
								String decoratedUrl = SugarWebservicesOperations.getInstance().buildV10LeadsSeamlessURL(id);
								System.out.println(decoratedUrl);
								GenericUtils.launchUrlInPreferredBrowser(decoratedUrl, true);
							}
						} else {
							final String progressId = createProgressIndicator(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ITEM_RETRIEVING_PROGRESS_MESSAGE));

							UiUtils.displaySugarItemById(getSugarType(type), id, null);

							UIJob removeProgressIndicatorUIjob = new UIJob("Remove Progress Indicator") //$NON-NLS-1$
							{

								@Override
								public IStatus runInUIThread(IProgressMonitor arg0) {
									removeProgressIndicator(progressId);
									return Status.OK_STATUS;
								}
							};

							// Set rule so the job will be executed in the correct
							// order.
							removeProgressIndicatorUIjob.setRule(UiUtils.DISPLAY_SUGAR_ITEM_BY_ID_JOB_RULE);
							removeProgressIndicatorUIjob.schedule();

						}

					}
				};

				accountsLink.addHyperlinkListener(accountsLinkListener);

				accountsLink.addDisposeListener(new DisposeListener() {

					@Override
					public void widgetDisposed(DisposeEvent arg0) {
						if (accountsLinkListener != null) {
							accountsLink.removeHyperlinkListener(accountsLinkListener);
						}

					}

				});

			}
		}

	}

	// Create progress bar composite in the Shell. Will set it to visible when
	// the Progress Bar
	// Indicator is needed.
	private void createProgressComposite() {
		_progressComposite = new Composite(getShell(), SWT.NONE);
		_progressComposite.setLayout(GridLayoutFactory.fillDefaults().create());
		_progressComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		((GridData) (_progressComposite.getLayoutData())).exclude = true;
		_progressComposite.setVisible(false);

		getShell().layout(true);

	}

	/*
	 * Tell the card to to create a progress section with the given message. This method will return an id of the newly created section. This id should be passed into removeProgressBar when the
	 * operation completes.
	 * 
	 * @param message
	 * 
	 * @return
	 */
	public String createProgressIndicator(final String message) {
		final String id = "progessBar_" + System.currentTimeMillis(); //$NON-NLS-1$

		_display.syncExec(new Runnable() {
			@Override
			public void run() {
				_progressComposite.setLayoutDeferred(true);

				if (!_progressComposite.isVisible()) {
					_progressComposite.setVisible(true);
					((GridData) (_progressComposite.getLayoutData())).exclude = false;
				}

				Composite composite = new Composite(_progressComposite, SWT.NONE);
				composite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).margins(5, 5).create());
				composite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
				composite.setData(PROGRESS_BAR_ID, id);

				Label label = new Label(composite, SWT.WRAP);
				label.setText(message);
				label.setLayoutData(GridDataFactory.fillDefaults().grab(false, false).create());

				ProgressBar progressBar = new ProgressBar(composite, SWT.INDETERMINATE);
				progressBar.setLayoutData(GridDataFactory.fillDefaults().indent(10, 0).grab(true, false).create());

				UiUtils.recursiveSetBackgroundColor(_progressComposite, getShell().getBackground());

				_progressComposite.setLayoutDeferred(false);

				_progressComposite.layout(true);
				getShell().layout(true);
				getShell().pack(true);
				getShell().layout(true);
			}
		});

		return id;
	}

	public void removeProgressIndicator(final String id) {
		_display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if (_progressComposite != null && !_progressComposite.isDisposed()) {
					for (Control control : _progressComposite.getChildren()) {
						Object storedId = control.getData(PROGRESS_BAR_ID);
						if (storedId != null && storedId.equals(id)) {
							control.dispose();
						}
					}

					if (_progressComposite.getChildren().length == 0) {
						_progressComposite.setVisible(false);
						((GridData) (_progressComposite.getLayoutData())).exclude = true;
					}

					_progressComposite.layout(true);
					getShell().pack(true);
					getShell().layout(true);
				}
			}
		});
	}

	private String getTypeWithColon(String type) {
		String typeWithColon = null;
		if (type != null) {
			if (type.equalsIgnoreCase(SugarType.CONTACTS.getParentType())) {
				typeWithColon = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CONTACTS_LABEL_STRING);
			} else if (type.equalsIgnoreCase(SugarType.LEADS.getParentType())) {
				typeWithColon = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LEADS_LABEL_STRING);
			} else if (type.equalsIgnoreCase(SugarType.OPPORTUNITIES.getParentType())) {
				typeWithColon = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OPPORTUNITIES_LABEL_STRING);
			} else if (type.equalsIgnoreCase(SugarType.ACCOUNTS.getParentType())) {
				typeWithColon = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ACCOUNTS_LABEL_STRING);
			}
		}
		return typeWithColon;
	}

	private SugarType getSugarType(String type) {
		SugarType sugarType = null;
		if (type != null) {
			if (type.equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CONTACT)) || type.equalsIgnoreCase(SugarType.CONTACTS.getParentType())) {
				sugarType = SugarType.CONTACTS;
			} else if (type.equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_LEAD)) || type.equalsIgnoreCase(SugarType.LEADS.getParentType())) {
				sugarType = SugarType.LEADS;
			} else if (type.equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_OPPORTUNITY)) || type.equalsIgnoreCase(SugarType.OPPORTUNITIES.getParentType())) {
				sugarType = SugarType.OPPORTUNITIES;
			} else if (type.equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CLIENT)) || type.equalsIgnoreCase(SugarType.ACCOUNTS.getParentType())) {
				sugarType = SugarType.ACCOUNTS;
			}
		}
		return sugarType;
	}

	private void createPaddingControl(int size, Composite composite) {

		for (int i = 0; i < size; i++) {
			new Label(composite, SWT.NONE);
		}
	}

	private Point computeMaxSize(Composite parent, String[] arrays) {
		int width = -1;
		int height = -1;
		if (parent == null || arrays == null || arrays.length == 0) {
			return null;
		}

		GC gc = new GC(parent);
		for (int i = 0; i < arrays.length; i++) {
			Point size = gc.textExtent(arrays[i]); // or textExtent
			width = Math.max(width, size.x);
			height = Math.max(height, size.y);
		}

		gc.dispose();
		return new Point(width, height);
	}

	private int getMaxLabelWidth() {
		if (_maxLabelWidth == -1) {
			Point point = computeMaxSize(_parentComposite, lineLabels);
			if (point != null) {
				_maxLabelWidth = point.x;
				_maxLabelHeight = point.y;
			}
		}
		return _maxLabelWidth;
	}

	private int getMaxLabelHeight() {

		if (_maxLabelHeight == -1) {
			Point point = computeMaxSize(_parentComposite, lineLabels);
			if (point != null) {
				_maxLabelWidth = point.x;
				_maxLabelHeight = point.y;
			}
		}
		return _maxLabelHeight;
	}

	private int getAsteriskWidth() {
		if (_asteriskWidth == -1) {
			Point point = computeMaxSize(_parentComposite, new String[]{ASTERISK_LABEL_STRING});
			if (point != null) {
				_asteriskWidth = point.x;
			}
		}
		return _asteriskWidth;
	}

	private void createTypeaheadProviders(Composite parent) {

		_typeaheadLabelProvider = new TypeaheadLabelProvider();
		_typeaheadLabelProvider.setCollectionModel(getCollectionModel());

		_typeaheadContentProvider = new TypeaheadContentProvider();
		_typeaheadContentProvider.setCollectionModel(getCollectionModel());
		_typeaheadContentProvider.setAssociateDataMap(getCurrAssociateDataMap());

	}

	public void setDoingLookup(boolean b) {
		_doingLookup = b;
	}

	protected TypeaheadCollectionModel getCollectionModel() {
		if (_model == null) {
			_model = new TypeaheadCollectionModel();
		}
		return _model;
	}

	public AssociateDataMap getCurrAssociateDataMap() {
		return _associateDataMap;
	}

	// Manage the dialog shell
	protected void configureShell() {
		if (getAppType() == ConnectorUtil.MAIL_APP) {
			getShell().setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_MAIL_TITLE_MESSAGE));
		} else if (getAppType() == ConnectorUtil.CALENDAR_APP) {
			getShell().setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_CALENDAR_TITLE_MESSAGE));
		}
		getShell().setFont(getTitleFont());

		if (JFaceResources.getColorRegistry().get(titleColor) == null) {
			JFaceResources.getColorRegistry().put(titleColor, new RGB(212, 212, 212));
		}
		getShell().setBackground(JFaceResources.getColorRegistry().get(titleColor));
		getShell().setImage(SFAImageManager.getImage(SFAImageManager.SALES_CONNECT));

		setDialogSize();
	}

	private void setDialogSize() {
		Preferences prefs = Activator.getDefault().getPluginPreferences();
		int xPref = prefs.getInt(WIDTH_PREFERENCE);
		int yPref = prefs.getInt(HEIGHT_PREFERENCE);
		if (xPref == 0) {
			xPref = DEFAULT_WIDTH;
		}
		if (yPref == 0) {
			yPref = DEFAULT_HEIGHT;
		}

		// yPref = 330;

		// UI is made up by 4 areas: existing association header area, to be associated area, to
		// be attachment list and copy to area.

		int headerY = 0;

		if (_associateDataMap != null) {
			headerY = getHeaderHeight();
		}

		int itemY = yPref;

		int attachmentY = 0;
		if (_attachmentTblViewer != null && isAttachmentExist()) {
			attachmentY = getAttachmentAreaHeight();
		}

		int copytoAreaY = 0;
		if (_copytoAreaComposite != null && _copytoAreaComposite.isVisible()) {
			copytoAreaY = getCopytoAreaHeight();
		}

		yPref = headerY + itemY + attachmentY + copytoAreaY;

		_AssociateCompositeShell.setSize(xPref, yPref);
	}

	private int getCopytoAreaHeight() {
		int height = 10;
		if (_copytoAreaComposite != null && _copytoAreaComposite.isVisible()) {
			height = 330 * 1 / 5;
		}
		return height;
	}

	private int getHeaderHeight() {
		int height = 15;
		if (_associateDataMap != null && _associateDataMap.getMyMap() != null && !_associateDataMap.getMyMap().isEmpty()) {
			int lineHeight = 25;
			height = height + _associateDataMap.getMyMap().size() * lineHeight;

		}
		return height;
	}

	private int getAttachmentAreaHeight() {
		int height = 120;
		if (_attachmentTblViewer != null && _attachmentTblViewer.getTable() != null) {
			height = height + _attachmentTblViewer.getTable().getItemCount() * (Math.min(5, _attachmentTblViewer.getTable().getItemHeight() + 5));
		}
		return height;
	}

	private int getAppType() {
		int appType = -1;
		if (_doc != null || _form != null) {
			String form = _form;
			if (_doc != null) {
				form = _doc.getForm();
			}
			appType = ConnectorUtil.getAppType(form);
		}
		return appType;
	}

	public Font getTitleFont() {
		return SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData();
	}

	private Font getHelpTextFont() {
		if (_helpTextFont == null) {
			String fontName = "helptext-plusone"; //$NON-NLS-1$
			if (JFaceResources.getFontRegistry().hasValueFor(fontName)) {
				_helpTextFont = JFaceResources.getFontRegistry().get(fontName);
			} else {
				FontData[] fd = _helpText.getFont().getFontData();
				fd[0].setHeight(fd[0].getHeight() + 1);
				JFaceResources.getFontRegistry().put(fontName, fd);
				_helpTextFont = JFaceResources.getFontRegistry().get(fontName);
			}
		}
		return _helpTextFont;

	}

	private Font getCityTextFont(boolean isItalic) {
		Font font = null;
		if (isItalic) {
			if (_cityItalicFont == null) {
				String fontName = "city-italic"; //$NON-NLS-1$
				if (JFaceResources.getFontRegistry().hasValueFor(fontName)) {
					_cityItalicFont = JFaceResources.getFontRegistry().get(fontName);
				} else {
					FontData[] fontData = _cityText.getFont().getFontData();
					fontData[0].setStyle(SWT.ITALIC);
					JFaceResources.getFontRegistry().put(fontName, fontData);
					_cityItalicFont = JFaceResources.getFontRegistry().get(fontName);
				}
			}
			font = _cityItalicFont;
		} else {
			if (_cityNormalFont == null) {
				String fontName = "city-normal"; //$NON-NLS-1$
				if (JFaceResources.getFontRegistry().hasValueFor(fontName)) {
					_cityNormalFont = JFaceResources.getFontRegistry().get(fontName);
				} else {
					FontData[] fontData = _cityText.getFont().getFontData();
					fontData[0].setStyle(SWT.NORMAL);
					JFaceResources.getFontRegistry().put(fontName, fontData);
					_cityNormalFont = JFaceResources.getFontRegistry().get(fontName);
				}
			}
			font = _cityNormalFont;
		}
		return font;
	}

	private Font getLookUpComboFont(boolean isItalic) {
		Font font = null;
		if (isItalic) {
			if (_lookUpComboItalicFont == null) {
				String fontName = "lookupcombo-italic"; //$NON-NLS-1$
				if (JFaceResources.getFontRegistry().hasValueFor(fontName)) {
					_lookUpComboItalicFont = JFaceResources.getFontRegistry().get(fontName);
				} else {
					FontData[] fontData = _lookUpCombo.getFont().getFontData();
					fontData[0].setStyle(SWT.ITALIC);
					JFaceResources.getFontRegistry().put(fontName, fontData);
					_lookUpComboItalicFont = JFaceResources.getFontRegistry().get(fontName);
				}
			}
			font = _lookUpComboItalicFont;
		} else {
			if (_lookUpComboNormalFont == null) {
				String fontName = "lookupcombo-normal"; //$NON-NLS-1$
				if (JFaceResources.getFontRegistry().hasValueFor(fontName)) {
					_lookUpComboNormalFont = JFaceResources.getFontRegistry().get(fontName);
				} else {
					FontData[] fontData = _lookUpCombo.getFont().getFontData();
					fontData[0].setStyle(SWT.NORMAL);
					JFaceResources.getFontRegistry().put(fontName, fontData);
					_lookUpComboNormalFont = JFaceResources.getFontRegistry().get(fontName);
				}
			}
			font = _lookUpComboNormalFont;
		}
		return font;

	}

	private AssociateData getCurrTypeAheadResult(int index) {
		AssociateData associateData = null;
		if (getCollectionModel().getWSResults() != null && getCollectionModel().getWSResults().length > index) {
			String id = ConstantStrings.EMPTY_STRING;
			Object obj = getCollectionModel().getWSResults()[index];
			if (obj instanceof JSONObject) {
				try {
					JSONObject jsonObject = (JSONObject) obj;
					id = jsonObject.getString(ConstantStrings.DATABASE_ID);

					associateData = new AssociateData(ConnectorUtil.getInstance().getResultParser(getCollectionModel().getCacheSugarType()).getAssociatedText(jsonObject), ConnectorUtil.getInstance()
							.getResultParser(getCollectionModel().getCacheSugarType()).getAssociatedExtendedText(jsonObject), id, false);
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
				}
			}
		}
		return associateData;
	}

	public void close() {
		if (_searchMyItemsSelectionListener != null && !_searchMyItemsButton.isDisposed()) {
			_searchMyItemsButton.removeSelectionListener(_searchMyItemsSelectionListener);
		}
		if (_findByModifyListener != null && !_findByCombo.isDisposed()) {
			_findByCombo.removeModifyListener(_findByModifyListener);
		}
		if (_cityModifyListener != null && !_cityText.isDisposed()) {
			_cityText.removeModifyListener(_cityModifyListener);
		}
		if (_cityFocusListener != null && !_cityText.isDisposed()) {
			_cityText.removeFocusListener(_cityFocusListener);
		}
		if (_lookUpComboKeyListener != null && !_lookUpCombo.isDisposed()) {
			_lookUpCombo.removeKeyListener(_lookUpComboKeyListener);
		}
		if (_lookUpComboModifyListener != null && !_lookUpCombo.isDisposed()) {
			_lookUpCombo.removeModifyListener(_lookUpComboModifyListener);
		}
		if (_lookUpComboFocusListener != null && !_lookUpCombo.isDisposed()) {
			_lookUpCombo.removeFocusListener(_lookUpComboFocusListener);
		}
		if (_lookUpSelectionListener != null && !_lookUpCombo.isDisposed()) {
			_lookUpCombo.removeSelectionListener(_lookUpSelectionListener);
		}
		if (_lookUpComboVerifyListener != null && !_lookUpCombo.isDisposed()) {
			_lookUpCombo.removeVerifyListener(_lookUpComboVerifyListener);
		}
		if (_lookUpComboMouseListener != null && !_lookUpCombo.isDisposed()) {
			_lookUpCombo.removeMouseListener(_lookUpComboMouseListener);
		}
		// if (_lookUpComboMouseTrackListener != null && !_lookUpCombo.isDisposed()) {
		// _lookUpCombo.removeMouseTrackListener(_lookUpComboMouseTrackListener);
		// }
		// if (_lookUpComboMouseMoveListener != null && !_lookUpCombo.isDisposed()) {
		// _lookUpCombo.removeMouseMoveListener(_lookUpComboMouseMoveListener);
		// }
		if (_lookUpComboResizeListener != null && !_lookUpCombo.isDisposed()) {
			_lookUpCombo.removeListener(SWT.Resize, _lookUpComboResizeListener);
		}
		if (_lookUpComboDisposeListener != null && !_lookUpCombo.isDisposed()) {
			_lookUpCombo.removeDisposeListener(_lookUpComboDisposeListener);
		}

		if (_attachmentTblViewer != null && !_attachmentTblViewer.getTable().isDisposed() && _attachmentTblPaintListener != null) {
			_attachmentTblViewer.getTable().removeListener(SWT.PaintItem, _attachmentTblPaintListener);
		}

		if (_okSelectionListener != null && !_okBtn.isDisposed()) {
			_okBtn.removeSelectionListener(_okSelectionListener);
		}

		if (_cancelSelectionListener != null && !_cancelBtn.isDisposed()) {
			_cancelBtn.removeSelectionListener(_cancelSelectionListener);
		}

		if (_copytoRemoveButtons != null && !_copytoRemoveButtons.isEmpty()) {
			Iterator<SFAButtonWithX> it = _copytoRemoveButtons.values().iterator();
			while (it.hasNext()) {
				it.next().dispose();
			}
		}

		_isOKOrCancelPressed = true;

		if (_removeProgressIndicatorUIjob != null) {
			_removeProgressIndicatorUIjob.cancel();
			_removeProgressIndicatorUIjob = null;
		}

		if (getShell() != null) {
			if (getShell().getListeners(SWT.Traverse) != null) {
				getShell().removeListener(SWT.Traverse, getShell().getListeners(SWT.Traverse)[0]);
			}
			if (getShell().getListeners(SWT.Resize) != null) {
				getShell().removeListener(SWT.Resize, getShell().getListeners(SWT.Resize)[0]);
			}
		}
		getShell().close();

	}

	protected void createButtonsForButtonBar(Composite parent) {
		Point cl = _container.getLocation();
		Rectangle rec = _container.getBounds();
		Point point = computeMaxSize(parent, new String[]{UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_BUTTON_LABEL), IDialogConstants.CANCEL_LABEL});
		Composite buttonComposite = new Composite(parent, SWT.NONE);
		Rectangle margins = Geometry.createDiffRectangle(WIDTH_MARGIN, WIDTH_MARGIN + 3, 1, 8);
		buttonComposite.setLayout(GridLayoutFactory.fillDefaults().extendedMargins(margins).numColumns(4).equalWidth(false).create());
		buttonComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		// padding button to grab horizontal space when resizing
		Button paddingBtn = new Button(buttonComposite, SWT.NONE);

		// hack to force the end of the cancel button to align with lookup field - might need a better way to do this.
		paddingBtn.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(rec.width - (2 * (point.x + WIDTH_MARGIN)) - 45, point.y + HEIGHT_MARGIN + 10).create());
		paddingBtn.setVisible(false);

		// Associate Button
		_okBtn = new Button(buttonComposite, SWT.PUSH);
		_okBtn.setLayoutData(GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).hint(point.x + WIDTH_MARGIN, point.y + HEIGHT_MARGIN).create());
		_okBtn.setFont(JFaceResources.getDialogFont());
		_okBtn.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_BUTTON_LABEL));

		if (_isPrefill && !isAssociated()) {
			setAssociateButtonEnabled(true);
		} else {
			setAssociateButtonEnabled(false);
		}
		// }
		addOKButtonListeners();

		// Cancel Button
		_cancelBtn = new Button(buttonComposite, SWT.PUSH);
		_cancelBtn.setLayoutData(GridDataFactory.fillDefaults().hint(point.x + WIDTH_MARGIN, point.y + HEIGHT_MARGIN).align(SWT.CENTER, SWT.CENTER).create());
		_cancelBtn.setText(IDialogConstants.CANCEL_LABEL);
		addCancelButtonListeners();

		// padding button to grab horizontal space when resizing
		Button paddingBtn1 = new Button(buttonComposite, SWT.NONE);
		paddingBtn1.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		paddingBtn1.setVisible(false);

		parent.layout();
	}

	protected void updateComboDropdownList(String[] strings) {
		if (!_lookUpCombo.isDisposed() && strings != null) {
			_lookUpCombo.setItems(strings);
		}
	}

	protected void setAssociateButtonEnabled(boolean b) {
		if (_okBtn != null) {
			_okBtn.setEnabled(b);
		}
	}

	protected void okPressed() {
		if (_attachmentTblViewer != null && isAttachmentExist()) {
			updateAttachmentList();
		}
		getCollectionModel().setAssociatedAttachmentList(_attachmentList);

		getCollectionModel().setIsToCopy(true);

		close();

	}

	// Mark selected attachment as selected
	private void updateAttachmentList() {

		// Mark only those selected as selected in the _attachmentList
		Object[] selectedList = _attachmentTblViewer.getCheckedElements();
		if (selectedList != null && selectedList.length > 0) {
			for (int i = 0; i < _attachmentList.length; i++) {
				boolean isSelected = false;
				String attachmentString = ((String) _attachmentList[i]).substring(1);
				for (int j = 0; j < selectedList.length; j++) {
					String selectedString = ((String) selectedList[j]).substring(1);
					if (attachmentString != null && selectedString != null && attachmentString.equalsIgnoreCase(selectedString)) {
						isSelected = true;
						break;
					}
				}
				StringBuffer sb = new StringBuffer(isSelected ? UiUtils.ATTACHMENT_IS_SELECTED : UiUtils.ATTACHMENT_IS_NOT_SELECTED);
				sb.append(_attachmentList[i].substring(1));
				_attachmentList[i] = sb.toString();

			}
		} else {
			updateAllAttachmentList(false);
		}

	}

	private void updateAllAttachmentList(boolean b) {
		if (_attachmentTblViewer != null && isAttachmentExist()) {
			for (int i = 0; i < _attachmentList.length; i++) {
				StringBuffer sb = new StringBuffer(b ? UiUtils.ATTACHMENT_IS_SELECTED : UiUtils.ATTACHMENT_IS_NOT_SELECTED);
				sb.append(_attachmentList[i].substring(1));
				_attachmentList[i] = sb.toString();
			}
		}
	}

	private void typeaheadSuggestionAction() {
		if (!isFindByDocument() && _lookUpCombo != null && !_lookUpCombo.isDisposed() && _lookUpCombo.getText() != null && _lookUpCombo.getText().trim().length() >= 2
				&& !_lookUpCombo.getText().equals(getLookUpDefaultText()) && !_doingLookup) {
			final boolean isMyItemsOnly = _searchMyItemsButton.getSelection();
			final String type = _findByCombo.getItems()[_findByCombo.getSelectionIndex() == -1 ? 0 : _findByCombo.getSelectionIndex()];
			final SugarType sugarType = getSugarType(type);
			final String city = getCityText();

			// wait for 500ms before executing setInput... when setInput is done, it sets DoingLookup flag
			// back to false to open the If condition.
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					// Use _txt to pick up the latest values user entered.
					setInput(isMyItemsOnly, sugarType, city, _txt);
				}
			}, 500);

			// set DoingLookup flag to true to block the If condition
			setDoingLookup(true);
		}
	}
}
