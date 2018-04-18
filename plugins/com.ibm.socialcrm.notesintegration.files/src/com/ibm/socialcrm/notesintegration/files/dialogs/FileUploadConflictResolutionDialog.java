package com.ibm.socialcrm.notesintegration.files.dialogs;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.progress.UIJob;

import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.DocumentInfo;
import com.ibm.socialcrm.notesintegration.core.SugarAccount;
import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.core.SugarOpportunity;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateData;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

/**
 * When we detect name conflicts with file uploads, we can use this dialog to help the user resolve the conflicts by renaming the files for upload or removing them from the upload operation.
 */
public class FileUploadConflictResolutionDialog extends Dialog {

	private static final String PROGRESS_BAR_ID = "progressBarId"; //$NON-NLS-1$

	private Shell _shell;
	private Composite _parent;
	private Composite _errorComposite;
	private Label _errorMsgLabel;
	private Label _versionLabel;
	private Label _newVersionLabel;
	private Label _noAssociationLabel;
	private Text _renameText;
	private Button _newVersionButton;
	private Label _newVersionNameLabel;
	private Button _renameButton;
	private Composite _associationComposite;
	private Composite _progressComposite = null;

	private SelectionListener _newVersionSelectionListener;

	private Color _errorCompositeColor;

	private Font _normalFont;

	private int _maxLabelWidth;

	private File _file;

	/*
	 * contains orig. document information retrieved from Connections server... Later, the GetDocumentRelationships API will update the sugarDocumentID field.
	 */
	private DocumentInfo _documentInfo;

	// contains association information returned from the GetDocumentRelationships API.
	private Map<String, Map<String, List<AssociateData>>> _sugarEntries = new HashMap<String, Map<String, List<AssociateData>>>();

	/**
	 * contains the new document information. If user cancelled this dialog, _newDoucmentInfo will remain null.
	 */
	private DocumentInfo _newDocumentInfo = null;

	private List<String> _namesTaken = null;

	/**
	 * I realize this constructor is a bit strange, but there are times when we may want to have a different display name to present to the user than the actual file. Hence, we pass in a map of File
	 * object to displayNames
	 * 
	 * @param shell
	 * @param fileMap
	 */
	public FileUploadConflictResolutionDialog(Shell shell, File file, DocumentInfo documentInfo, List<String> namesTaken) {
		super(shell);
		setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MODELESS);
		this._shell = shell;
		this._file = file;
		this._documentInfo = documentInfo;
		this._namesTaken = namesTaken;

	}

	private void setShellListeners() {
		// Close the shell when the user presses ESC
		getShell().addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					close();
				}
			}
		});

		// Listener to listen for shell resize
		getShell().addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				// Don't save the preferences when the user maximizes the size
				if (!getShell().getMaximized()) {
					// Set the explicit size of the parent composite so that if we add a progress composite, the shell
					// expands to accommodate the new widget rather than shrink the parentComposite within the shell.
					Point point = getShell().getSize();
					if (getShell() != null && !getShell().isDisposed()) {
						((GridData) getShell().getLayoutData()).widthHint = point.x;
						((GridData) getShell().getLayoutData()).heightHint = point.y;
					}
				}
			}
		});
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setLayoutData(GridDataFactory.fillDefaults().create());
		shell.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UPLOAD_CONFLICT_TITLE));
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		_parent = parent;
		parent = (Composite) super.createDialogArea(parent);
		createErrorComposite(parent);
		parent.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).margins(10, 10).create());
		buildFileComposite(parent);
		parent.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));
		createErrorMsgComposite(parent);
		createProgressComposite();
		retrieveAssociationData();

		setShellListeners();
		return parent;
	}

	/**
	 * Helper method to create the error composite
	 * 
	 * @param parent
	 * @param showRemoveButton
	 */
	private void createErrorComposite(Composite parent) {
		_errorComposite = new Composite(parent, SWT.BORDER);
		_errorComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).margins(10, 10).create());
		_errorComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		_errorComposite.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				getErrorCompositeColor().dispose();
			}
		});

		Label errorIcon = new Label(_errorComposite, SWT.NONE);
		errorIcon.setImage(SFAImageManager.getImage(SFAImageManager.ERROR_ICON));
		errorIcon.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).create());

		Label errorLabel = new Label(_errorComposite, SWT.WRAP);
		errorLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UPLOAD_CONFLICT_SINGLE_CONFLICT));
		errorLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(620, SWT.DEFAULT).create());
		UiUtils.recursiveSetBackgroundColor(_errorComposite, getErrorCompositeColor());
	}

	/*
	 * error message
	 */
	private void createErrorMsgComposite(Composite parent) {

		_errorMsgLabel = new Label(parent, SWT.WRAP);
		_errorMsgLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(620, SWT.DEFAULT).create());
		_errorMsgLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		_errorMsgLabel.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));
		setControlVisible(_errorMsgLabel, true);

	}

	/**
	 * Helper method to build each of the file parts
	 * 
	 * @param parent
	 * @param file
	 * @param showRemoveButton
	 */
	private void buildFileComposite(final Composite parent) {
		final Composite fileComposite = new Composite(parent, SWT.NONE);
		fileComposite.setLayout(GridLayoutFactory.fillDefaults().equalWidth(false).numColumns(3).spacing(15, 5).create());
		fileComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		// Document:
		Label documentLabel = new Label(fileComposite, SWT.NONE);
		documentLabel.setLayoutData(GridDataFactory.fillDefaults().create());
		documentLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UPLOAD_CONFLICT_DOCUMENT));

		Label documentNameLabel = new Label(fileComposite, SWT.NONE);
		documentNameLabel.setLayoutData(GridDataFactory.fillDefaults().grab(false, false).create());
		documentNameLabel.setText(_documentInfo == null ? ConstantStrings.EMPTY_STRING : _documentInfo.getDocumentName());

		_versionLabel = new Label(fileComposite, SWT.NONE);
		_versionLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		_versionLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UPLOAD_CONFLICT_VERSION,
				new String[]{_documentInfo == null ? ConstantStrings.EMPTY_STRING : _documentInfo.getVersion()}));

		// Associations:
		Label associationLabel = new Label(fileComposite, SWT.NONE);
		associationLabel.setLayoutData(GridDataFactory.fillDefaults().grab(false, false).create());
		associationLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UPLOAD_CONFLICT_ASSOCIATIONS));

		_associationComposite = new Composite(fileComposite, SWT.NONE);
		_associationComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0).spacing(0, 0).create());
		_associationComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());

		_noAssociationLabel = new Label(_associationComposite, SWT.WRAP);
		_noAssociationLabel.setLayoutData(GridDataFactory.fillDefaults().hint(570, SWT.DEFAULT).grab(true, false).create());
		_noAssociationLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UPLOAD_CONFLICT_NO_ASSOCIATIONS));
		setControlVisible(_noAssociationLabel, false);

		// will create association items after getDocumentRelationships WS is done.
		// createAssociationItems(_associationComposite);

		Composite optionComposite = new Composite(fileComposite, SWT.NONE);
		Rectangle margins = Geometry.createDiffRectangle(0, 0, 15, 15);
		optionComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).extendedMargins(margins).create());
		optionComposite.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).grab(true, false).create());

		// Create a new version
		_newVersionButton = new Button(optionComposite, SWT.RADIO);
		_newVersionButton.setLayoutData(GridDataFactory.fillDefaults().create());
		_newVersionButton.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UPLOAD_CONFLICT_CREATE_NEW_VERSION));
		addNewVersionButtonListener();

		_newVersionNameLabel = new Label(optionComposite, SWT.NONE);
		_newVersionNameLabel.setLayoutData(GridDataFactory.fillDefaults().create());
		_newVersionNameLabel.setText(_documentInfo == null ? ConstantStrings.EMPTY_STRING : _documentInfo.getDocumentName());

		_newVersionLabel = new Label(optionComposite, SWT.NONE);
		_newVersionLabel.setLayoutData(GridDataFactory.fillDefaults().create());
		_newVersionLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UPLOAD_CONFLICT_VERSION, new String[]{getOneUpVersion()}));

		// Rename the document
		_renameButton = new Button(optionComposite, SWT.RADIO);
		_renameButton.setLayoutData(GridDataFactory.fillDefaults().create());
		_renameButton.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UPLOAD_CONFLICT_RENAME_DOCUMENT));
		addRenameButtonListener();

		_renameText = new Text(optionComposite, SWT.BORDER);
		_renameText.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).grab(true, false).create());
		_renameText.setBackground(null);
		_renameText.setText(_documentInfo == null ? ConstantStrings.EMPTY_STRING : _documentInfo.getDocumentName());
		_renameText.setEnabled(false);
		addRenameTextListener();

		UiUtils.recursiveSetBackgroundColor(fileComposite, JFaceColors.getBannerBackground(Display.getDefault()));
	}

	private void addNewVersionButtonListener() {
		_newVersionSelectionListener = new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				_newVersionNameLabel.setEnabled(_newVersionButton.getSelection());
				_newVersionLabel.setEnabled(_newVersionButton.getSelection());
				_renameText.setEnabled(!_newVersionButton.getSelection());

				setControlVisible(_errorMsgLabel, false);
				getShell().pack(true);
				getShell().layout(true);

			}

		};
		_newVersionButton.addSelectionListener(_newVersionSelectionListener);
		_newVersionButton.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				if (_newVersionSelectionListener != null) {
					_newVersionButton.removeSelectionListener(_newVersionSelectionListener);
				}

			}

		});
	}
	private void addRenameButtonListener() {
		final SelectionListener renameSelectionListener = new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				_renameText.setEnabled(_renameButton.getSelection());
				_newVersionNameLabel.setEnabled(!_renameButton.getSelection());
				_newVersionLabel.setEnabled(!_renameButton.getSelection());
				setControlVisible(_errorMsgLabel, false);
				getShell().pack(true);
				getShell().layout(true);

			}

		};
		_renameButton.addSelectionListener(renameSelectionListener);

		_renameButton.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				if (renameSelectionListener != null) {
					_renameButton.removeSelectionListener(renameSelectionListener);
				}

			}

		});
	}

	private void addRenameTextListener() {
		final ModifyListener renameTextModifyListener = new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent arg0) {
				if (_errorMsgLabel.isVisible()) {
					setControlVisible(_errorMsgLabel, false);
					getShell().pack(true);
					getShell().layout(true);
				}
			}

		};

		_renameText.addModifyListener(renameTextModifyListener);

		_renameText.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				if (renameTextModifyListener != null) {
					_renameText.removeModifyListener(renameTextModifyListener);
				}

			}

		});
	}

	private void setControlVisible(Control c, boolean b) {

		((GridData) c.getLayoutData()).exclude = !b;
		c.setVisible(b);

	}

	private String getOneUpVersion() {
		String oneUpX = ConstantStrings.EMPTY_STRING;
		int oneUp = 1;
		try {
			if (_documentInfo != null) {
				String s = _documentInfo.getVersion();
				oneUp = Integer.valueOf(s).intValue() + 1;
				oneUpX = String.valueOf(oneUp);
			}
		} catch (Exception e) {
		}
		return oneUpX;
	}

	private void createAssociationItems(Composite parent, final String type, List<AssociateData> items) {

		_maxLabelWidth = -1;
		_maxLabelWidth = getMaxLabelWidth();

		Composite associateComposite = new Composite(parent, SWT.NONE);
		associateComposite.setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(2).create());
		associateComposite.setLayoutData(GridDataFactory.fillDefaults().indent(0, 0).grab(true, true).create());

		Label accountsLabel = new Label(associateComposite, SWT.NONE);
		accountsLabel.setLayoutData(GridDataFactory.fillDefaults().hint(_maxLabelWidth, SWT.DEFAULT).indent(0, SWT.DEFAULT).span(1, 1).create());
		accountsLabel.setBackground(associateComposite.getBackground());
		accountsLabel.setText(getTypeWithColon(type, items.size()));

		Composite linkComposite = new Composite(associateComposite, SWT.NONE);
		linkComposite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).hint(570, SWT.DEFAULT).grab(true, false).indent(10, 0).create());
		linkComposite.setBackground(associateComposite.getBackground());

		RowLayout layout = new RowLayout();
		layout.wrap = true;
		layout.pack = true;
		linkComposite.setLayout(layout);

		for (int i = 0; i < items.size(); i++) {

			// put image and item name in a Composite, so, wrapping logic will treat both as 1 unit.
			Composite itemComposite = new Composite(linkComposite, SWT.NONE);
			itemComposite.setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).numColumns(2).create());
			itemComposite.setBackground(associateComposite.getBackground());

			Label popoutLinkLabel = new Label(itemComposite, SWT.NONE);
			popoutLinkLabel.setImage(SFAImageManager.getImage(SFAImageManager.EXTERNAL_LINK));
			popoutLinkLabel.setLayoutData(GridDataFactory.fillDefaults().indent(10, 0).hint(15, SWT.DEFAULT).create());
			popoutLinkLabel.setToolTipText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OPEN_IN_SUGAR));
			// Don't forget to pack me
			popoutLinkLabel.pack();

			final Hyperlink accountsLink = new Hyperlink(itemComposite, SWT.NONE);
			accountsLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
			accountsLink.setBackground(associateComposite.getBackground());
			accountsLink.setFont(getSmallerFont() /* SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData() */); // acw-???

			StringBuffer sb = new StringBuffer(items.get(i).getName());
			sb.append((i == (items.size() - 1)) ? ConstantStrings.EMPTY_STRING : ConstantStrings.COMMA);
			sb.append(ConstantStrings.SPACE);
			accountsLink.setText(sb.toString());

			// don't forget to pack me
			accountsLink.pack();

			final String id = items.get(i).getId();

			if (id != null && !id.equalsIgnoreCase(ConstantStrings.EMPTY_STRING)) {
				final HyperlinkAdapter accountsLinkListener = new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent evt) {

						final String progressId = createProgressIndicator(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ITEM_RETRIEVING_PROGRESS_MESSAGE));

						if (type.equalsIgnoreCase(SugarType.CONTACTS.getType())) {
							SugarContact contact = new SugarContact(id, null);
							GenericUtils.launchUrlInPreferredBrowser(contact.getSugarUrl(), true);
						} else if (type.equalsIgnoreCase(SugarType.OPPORTUNITIES.getType())) {
							SugarOpportunity oppty = new SugarOpportunity(id, null);
							GenericUtils.launchUrlInPreferredBrowser(oppty.getSugarUrl(), true);
						} else if (type.equalsIgnoreCase(SugarType.ACCOUNTS.getType())) {
							SugarAccount account = new SugarAccount(id, null);
							GenericUtils.launchUrlInPreferredBrowser(account.getSugarUrl(), true);
						}

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

			itemComposite.pack();
		}

		UiUtils.recursiveSetBackgroundColor(parent, JFaceColors.getBannerBackground(Display.getDefault()));
		parent.layout(true);
		_parent.layout(true);
		_shell.layout();
	}

	// Font look tiny in Mac for some reason. So if we're on a Mac, bump the font size up
	private int macFontSizeAdjustment = GenericUtils.isMac() ? 4 : 0;

	public Font getSmallerFont() {
		if (_normalFont == null) {
			String fontName = "Arial-10-normal"; //$NON-NLS-1$
			if (JFaceResources.getFontRegistry().hasValueFor(fontName)) {
				_normalFont = JFaceResources.getFontRegistry().get(fontName);
			} else {
				JFaceResources.getFontRegistry().put(fontName, new FontData[]{new FontData("Arial", 10 + macFontSizeAdjustment, SWT.NORMAL)}); //$NON-NLS-1$
				_normalFont = JFaceResources.getFontRegistry().get(fontName);
			}
		}
		return _normalFont;
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
	 * Create a progress section with the given message. This method will return an id of the newly created section. This id should be passed into removeProgressBar when the operation completes.
	 * 
	 * @param message
	 * 
	 * @return
	 */
	public String createProgressIndicator(final String message) {
		final String id = "progessBar_" + System.currentTimeMillis(); //$NON-NLS-1$
		Display.getDefault().syncExec(new Runnable() {
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

				_progressComposite.setLayoutDeferred(false);

				_progressComposite.layout(true);
				getShell().layout(true);
				getShell().pack(true);
			}
		});

		return id;
	}

	public void removeProgressIndicator(final String id) {
		Display.getDefault().asyncExec(new Runnable() {
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

	private String getTypeWithColon(String type, int itemSize) {
		String typeWithColon = null;
		if (type != null) {
			if (type.equalsIgnoreCase(SugarType.CONTACTS.getType())) {
				if (itemSize > 1) {
					typeWithColon = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CONTACTS_LABEL_STRING);
				} else {
					typeWithColon = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CONTACT_LABEL_STRING);
				}
			} else if (type.equalsIgnoreCase(SugarType.OPPORTUNITIES.getType())) {
				if (itemSize > 1) {
					typeWithColon = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OPPORTUNITIES_LABEL_STRING);
				} else {
					typeWithColon = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OPPORTUNITY_LABEL_STRING);
				}
			} else if (type.equalsIgnoreCase(SugarType.ACCOUNTS.getType())) {
				if (itemSize > 1) {
					typeWithColon = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ACCOUNTS_LABEL_STRING);
				} else {
					typeWithColon = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ACCOUNT_LABEL_STRING);
				}
			}
		}
		return typeWithColon;
	}

	@Override
	protected void createButtonsForButtonBar(Composite buttonBar) {
		super.createButtonsForButtonBar(buttonBar);
		updateButtons();
	}

	private void updateButtons() {

		Button okButton = getButton(IDialogConstants.OK_ID);
		okButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				int isError = 0;
				if (_newVersionButton != null && !_newVersionButton.isDisposed() && _newVersionButton.getSelection() && _newVersionLabel != null && !_newVersionLabel.isDisposed()) {

					_newDocumentInfo = new DocumentInfo(_documentInfo.getDocumentName(), _newVersionLabel.getText(), _documentInfo.getConnectionsUUID());
					_newDocumentInfo.setSugarDocumentID(_documentInfo.getSugarDocumentID());
					isError = 0;

				} else if (_renameButton != null && !_renameButton.isDisposed() && _renameButton.getSelection()) {

					if (_renameText != null && !_renameText.isDisposed()) {
						if (isNameTaken(_renameText.getText())) {
							isError = 1;
						} else {
							_newDocumentInfo = new DocumentInfo(_renameText.getText(), "1", _documentInfo.getConnectionsUUID()); //$NON-NLS-1$
							_newDocumentInfo.setSugarDocumentID(null);
							isError = 0;
						}
					}

				}

				if (isError > 0) {
					setControlVisible(_errorMsgLabel, true);
					String errormsg = null;
					if (isError == 1) {
						errormsg = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UPLOAD_COINFLICT_ERROR_MSG1);
					}

					if (errormsg != null) {
						_errorMsgLabel.setText(errormsg);
					}
					getShell().pack(true);
					getShell().layout(true);

				} else {
					close();
				}

			}

		});

	}

	public void okPressed() {
		/* DO NOTHING HERE!!! */
		// this.close();
	}

	private boolean isNameTaken(String s) {
		boolean isTaken = false;
		if (_namesTaken == null || _namesTaken.isEmpty()) {
			isTaken = false;
		} else if (s != null && _namesTaken.contains(s)) {
			isTaken = true;
		}
		return isTaken;
	}

	private Color getErrorCompositeColor() {
		if (_errorCompositeColor == null) {
			_errorCompositeColor = new Color(Display.getDefault(), 250, 228, 222);
		}
		return _errorCompositeColor;
	}

	private void retrieveAssociationData() {

		final String progressId = createProgressIndicator(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ITEM_RETRIEVING_PROGRESS_MESSAGE));

		retrieveAssociationDataWebServices();

		UIJob updateUIAndRemoveProgressIndicatorUIjob = new UIJob("Update UI And Remove Progress Indicator") //$NON-NLS-1$
		{

			@Override
			public IStatus runInUIThread(IProgressMonitor arg0) {

				if (getAssociatedEntries() != null && !getAssociatedEntries().isEmpty()) {
					boolean isNoAssociation = true;
					Set<Entry<String, List<AssociateData>>> entryset = getAssociatedEntries().entrySet();
					Iterator<Entry<String, List<AssociateData>>> it = entryset.iterator();
					while (it.hasNext()) {
						Entry<String, List<AssociateData>> entry = (Entry<String, List<AssociateData>>) it.next();
						String type = entry.getKey();
						if (entry.getValue() != null && !entry.getValue().isEmpty()) {
							List<AssociateData> names = entry.getValue();
							createAssociationItems(_associationComposite, type, names);
							isNoAssociation = false;
						}
					}
					if (isNoAssociation) {
						setControlVisible(_noAssociationLabel, true);
					}

				} else {
					setControlVisible(_noAssociationLabel, true);
				}

				// if no sugar id, disable the new version option
				if (_documentInfo.getSugarDocumentID() == null) {
					_newVersionButton.setEnabled(false);
					_newVersionButton.setSelection(false);
					_newVersionNameLabel.setEnabled(false);
					_newVersionLabel.setEnabled(false);

					_renameButton.setSelection(true);
					_renameText.setEnabled(true);
				}

				_associationComposite.layout();

				removeProgressIndicator(progressId);
				return Status.OK_STATUS;
			}
		};

		// Set rule so the job will be executed in the correct
		// order.
		updateUIAndRemoveProgressIndicatorUIjob.setRule(UiUtils.DISPLAY_SUGAR_ITEM_BY_ID_JOB_RULE);
		updateUIAndRemoveProgressIndicatorUIjob.schedule();

	}
	private void retrieveAssociationDataWebServices() {
		Job job = new Job("retrieveAssociationDataWebServices") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				List<String> uuids = new ArrayList<String>();
				uuids.add(_documentInfo.getConnectionsUUID());
				String out = SugarWebservicesOperations.getInstance().getDocumentRelationships("connectionsid", uuids); //$NON-NLS-1$
				processGetDocumentRelationships(out);

				return Status.OK_STATUS;
			}
		};
		// Setting job rule so jobs following this rule will be executed in the correct order.
		job.setRule(UiUtils.DISPLAY_SUGAR_ITEM_BY_ID_JOB_RULE);
		job.schedule();
	}

	public void processGetDocumentRelationships(String output) {
		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(output);
		} catch (JSONException e) {
			// End gracefully.
		}
		if (jsonObject != null) {
			try {
				if (jsonObject.containsKey("result")) { //$NON-NLS-1$ 
					JSONObject resultObj = jsonObject.getJSONObject("result"); //$NON-NLS-1$
					Set<String> keySet = resultObj.keySet();
					if (!keySet.isEmpty()) {
						Iterator<String> it = keySet.iterator();
						while (it.hasNext()) {
							String key = it.next();
							Object obj = resultObj.get(key);
							if (obj instanceof JSONObject) {
								JSONObject associateObject = (JSONObject) obj;
								Map<String, List<AssociateData>> sugarEntry = new HashMap<String, List<AssociateData>>();

								// get document sugar id
								StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
								if (associateObject.has("SugarIDs") && associateObject.get("SugarIDs") instanceof JSONArray) { //$NON-NLS-1$  //$NON-NLS-2$
									JSONArray array = (JSONArray) associateObject.get("SugarIDs"); //$NON-NLS-1$
									for (int i = 0; i < array.length(); i++) {
										sb.append(i == 0 ? ConstantStrings.EMPTY_STRING : ConstantStrings.COMMA).append(array.get(i).toString());
										// For now, we will only take the first sugar id
										if (i == 0) {
											break;
										}
									}
									if (sb.length() > 0) {
										_documentInfo.setSugarDocumentID(sb.toString());
									}

								}

								// get associted oppties
								JSONObject opptyObject = null;
								if (associateObject.get("Opportunities") instanceof JSONArray) { //$NON-NLS-1$

								} else {
									opptyObject = associateObject.getJSONObject("Opportunities"); //$NON-NLS-1$
								}
								sugarEntry.put(SugarType.OPPORTUNITIES.getType(), extractSugarItems(opptyObject));

								// get associated accounts
								// HashMap accountsHashMap = new HashMap();
								JSONObject accountObject = null;
								if (associateObject.get("Accounts") instanceof JSONArray) { //$NON-NLS-1$
								} else {
									accountObject = associateObject.getJSONObject("Accounts"); //$NON-NLS-1$
								}
								sugarEntry.put(SugarType.ACCOUNTS.getType(), extractSugarItems(accountObject));

								_sugarEntries.put(key, sugarEntry);
							}
						}
					}
				}
			} catch (JSONException e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}

	}

	private List<AssociateData> extractSugarItems(JSONObject object) {
		List<AssociateData> associateDatas = new ArrayList<AssociateData>();

		try {
			if (object != null && !object.isEmpty()) {
				Iterator<String> it = object.keySet().iterator();
				while (it.hasNext()) {
					String id = it.next();
					String name = object.getString(id);
					AssociateData associateData = new AssociateData(name, ConstantStrings.EMPTY_STRING, id, true);
					associateDatas.add(associateData);
				}
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}
		return associateDatas;
	}

	public Map<String, Map<String, List<AssociateData>>> getSugarEntries() {
		if (_sugarEntries == null) {
			_sugarEntries = new HashMap<String, Map<String, List<AssociateData>>>();
		}
		return _sugarEntries;
	}

	public Map<String, List<AssociateData>> getAssociatedEntries() {
		return getSugarEntries().get(_documentInfo.getConnectionsUUID());
	}

	public DocumentInfo getNewDocumentInfo() {
		return _newDocumentInfo;
	}

	protected int getMaxLabelWidth() {

		if (_maxLabelWidth == -1) {
			StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
			boolean isFirst = true;
			Iterator<String> it = getAssociatedEntries().keySet().iterator();
			while (it.hasNext()) {
				String type = it.next();
				int itemsize = getAssociatedEntries().get(type).size();
				if (itemsize > 0) {
					if (type != null && type.equals(SugarType.ACCOUNTS.getType())) {
						sb.append(isFirst ? ConstantStrings.EMPTY_STRING : ConstantStrings.COMMA).append(
								(itemsize >= 1 ? UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ACCOUNTS_LABEL_STRING) : UtilsPlugin.getDefault().getResourceString(
										UtilsPluginNLSKeys.ACCOUNT_LABEL_STRING)));
					} else if (type != null && type.equals(SugarType.OPPORTUNITIES.getType())) {
						sb.append(isFirst ? ConstantStrings.EMPTY_STRING : ConstantStrings.COMMA).append(
								(itemsize >= 1 ? UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OPPORTUNITIES_LABEL_STRING) : UtilsPlugin.getDefault().getResourceString(
										UtilsPluginNLSKeys.OPPORTUNITY_LABEL_STRING)));
					} else if (type != null && type.equals(SugarType.CONTACTS.getType())) {
						sb.append(isFirst ? ConstantStrings.EMPTY_STRING : ConstantStrings.COMMA).append(
								(itemsize >= 1 ? UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CONTACTS_LABEL_STRING) : UtilsPlugin.getDefault().getResourceString(
										UtilsPluginNLSKeys.CONTACT_LABEL_STRING)));
					}
					if (isFirst) {
						isFirst = false;
					}
				}
			}
			String[] associationLabels = sb.toString().split(ConstantStrings.COMMA);

			Point point = computeMaxSize(_parent, associationLabels);
			if (point != null) {
				_maxLabelWidth = point.x;
				_maxLabelWidth += 18; // Add a buffer to improve spacing
			}
		}
		return _maxLabelWidth;
	}

	public Point computeMaxSize(Composite parent, String[] arrays) {
		int width = -1;
		int height = -1;
		if (parent == null || arrays == null || arrays.length == 0) {
			return null;
		}

		GC gc = new GC(parent);
		gc.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
		for (int i = 0; i < arrays.length; i++) {

			Point size = gc.textExtent(arrays[i]); // or textExtent
			width = Math.max(width, size.x);
			height = Math.max(height, size.y);
		}

		gc.dispose();
		return new Point(width, height);
	}

}
