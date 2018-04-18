package com.ibm.socialcrm.notesintegration.files.sidebar;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import com.ibm.rcp.swt.swidgets.SToolBar;
import com.ibm.rcp.swt.swidgets.SToolItem;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.files.FilesPluginActivator;
import com.ibm.socialcrm.notesintegration.files.utils.DocumentUploadObject;
import com.ibm.socialcrm.notesintegration.files.utils.DocumentUploadOperations;
import com.ibm.socialcrm.notesintegration.files.widgets.UploadTableViewer;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateData;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateDataMap;
import com.ibm.socialcrm.notesintegration.ui.custom.SFAButtonWithX;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFACombo;

public class FileUploadComposite extends Composite {
	private Composite instructionsComposite;
	private Composite uploadCompleteComposite;
	private Composite associationComposite;
	private Composite dropComposite;
	private Composite sugarItemAssociationComposite;

	private SToolItem findDocumentsItem;
	private SToolItem copyToSalesConnectItem;

	private Button myItemsButton;
	private SFACombo categoryCombo;
	private Label cityLabel;
	private Text cityText;
	private SFACombo itemCombo;
	private Button copyButton;
	private Button cancelButton;

	private Label copyToLabel;

	private Font italicsFont;
	private Font normalFont;

	private UploadTableViewer uploadViewer;

	private boolean showInstructions = true;
	private boolean showFinishedPanel = false;
	private boolean doingTypeaheadLookup = false; // Indicates if a web service call to the typeahead service is currently in progress
	private long lastKeystrokeTime = 0; // Used to maintain a 1/2 delay before firing the typeahead calls

	// Used to store the list of sugar ids currently selected to associate the files to during upload.
	private Set<String> currentlySelectedSugarIds = new HashSet<String>();

	private final String ASSOCIATE_DATA_KEY = "associateDataKey"; //$NON-NLS-1$
	private final String SUGAR_TYPE_KEY = "sugarTypeKey"; //$NON-NLS-1$

	private Map<String, String> sugarTypeMap = new HashMap<String, String>() {
		{
			put(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CONTACT), SugarType.CONTACTS.getParentType());
			put(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_OPPORTUNITY), SugarType.OPPORTUNITIES.getParentType());
			put(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CLIENT), SugarType.ACCOUNTS.getParentType());
		}
	};

	public FileUploadComposite(Composite parent) {
		super(parent, SWT.NONE);
		JFaceResources.getColorRegistry().put("textFieldPromptColor", new RGB(155, 155, 155)); //$NON-NLS-1$
		createComposite();
	}

	public void createComposite() {
		setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).create());
		setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		createToolbar();
		createAssociationComposite();
		createInstructionsComposite();
		createUploadCompleteComposite();
		createDropComposite();

		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent evt) {
				getItalicsFont().dispose();
				getNormalFont().dispose();
			}
		});
	}

	/**
	 * Creates the toolbar
	 * 
	 * @param parent
	 */
	private void createToolbar() {
		SToolBar toolbar = new SToolBar(this, SWT.FLAT);
		toolbar.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		findDocumentsItem = new SToolItem(toolbar, SWT.NONE);
		findDocumentsItem.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FILE_UPLOAD_FIND_DOCUMENTS));
		findDocumentsItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				FileDialog fileDialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.MULTI);
				fileDialog.open();
				String[] files = fileDialog.getFileNames();
				if (files != null) {
					Collection<File> existingFiles = (Collection<File>) uploadViewer.getInput();
					for (String newFile : files) {
						existingFiles.add(new File(fileDialog.getFilterPath() + File.separator + newFile));
					}
					uploadViewer.refresh();
					updateUIState();
				}
			}
		});

		new SToolItem(toolbar, SWT.SEPARATOR);

		copyToSalesConnectItem = new SToolItem(toolbar, SWT.NONE);
		copyToSalesConnectItem.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FILE_UPLOAD_COPY_TO_SALESCONNECT));
		copyToSalesConnectItem.setEnabled(false);
		copyToSalesConnectItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				showInstructions = false;
				showFinishedPanel = false;
				updateUIState();
				setItemDefaultText(true);
			}
		});
	}

	/**
	 * Creates the composite that lets the user select what items these files should be associated to.
	 * 
	 * @param parent
	 */
	private void createAssociationComposite() {
		associationComposite = new Composite(this, SWT.NONE);
		associationComposite.setLayout(GridLayoutFactory.fillDefaults().margins(5, 5).create());
		associationComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		Label descriptionLabel = new Label(associationComposite, SWT.WRAP);
		descriptionLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		descriptionLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FILE_UPLOAD_DESCRIPTION));

		final Composite selectionWidgetComposite = new Composite(associationComposite, SWT.NONE);
		selectionWidgetComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
		selectionWidgetComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		new Label(selectionWidgetComposite, SWT.NONE); // spacer label

		myItemsButton = new Button(selectionWidgetComposite, SWT.CHECK);
		myItemsButton.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FILE_UPLOAD_MY_ITEMS));
		myItemsButton.setLayoutData(GridDataFactory.fillDefaults().create());
		myItemsButton.setSelection(true);

		createCategorySection(selectionWidgetComposite);
		createCitySection(selectionWidgetComposite);
		createItemSection(selectionWidgetComposite);

		copyToLabel = new Label(selectionWidgetComposite, SWT.NONE);
		copyToLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FILE_UPLOAD_COPY_TO));
		copyToLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).create());

		sugarItemAssociationComposite = new Composite(selectionWidgetComposite, SWT.NONE);
		RowLayout rowLayout = new RowLayout();
		rowLayout.wrap = true;
		rowLayout.pack = true;
		sugarItemAssociationComposite.setLayout(rowLayout);
		sugarItemAssociationComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).indent(-5, -5).span(1, // the -5 helps the alignment since we're in an internal composite
				1).create());
		sugarItemAssociationComposite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent arg0) {
				Control[] children = sugarItemAssociationComposite.getChildren();
				for (Control child : children) {
					child.redraw();
				}
			}
		});

		Composite buttonComposite = new Composite(selectionWidgetComposite, SWT.NONE);
		buttonComposite.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).grab(true, false).align(SWT.END, SWT.BEGINNING).create());
		buttonComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());

		copyButton = new Button(buttonComposite, SWT.PUSH);
		copyButton.setLayoutData(GridDataFactory.fillDefaults().create());
		copyButton.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FILE_UPLOAD_COPY));
		copyButton.setEnabled(false);
		copyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				Job job = new Job("File Upload Conflict Resolution") //$NON-NLS-1$
				{
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						final Collection<File> origFileList = (Collection<File>) uploadViewer.getInput();
						// Do a deep copy of the file list so the conflict resolution doesn't blow up the editor input
						Collection<File> fileList = new ArrayList<File>();
						for (File file : origFileList) {
							fileList.add(file);
						}

						final TreeMap<String, List<AssociateData>> sugarItemMap = new TreeMap<String, List<AssociateData>>() {
							{
								put(AssociateDataMap.getWeight(SugarType.ACCOUNTS.getParentType()) + SugarType.ACCOUNTS.getParentType(), new ArrayList<AssociateData>());
								put(AssociateDataMap.getWeight(SugarType.CONTACTS.getParentType()) + SugarType.CONTACTS.getParentType(), new ArrayList<AssociateData>());
								put(AssociateDataMap.getWeight(SugarType.OPPORTUNITIES.getParentType()) + SugarType.OPPORTUNITIES.getParentType(), new ArrayList<AssociateData>());
							}
						};

						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								for (Control child : sugarItemAssociationComposite.getChildren()) {
									if (child.getData(ASSOCIATE_DATA_KEY) != null) {
										String sugarType = (String) child.getData(SUGAR_TYPE_KEY);
										List<AssociateData> list = sugarItemMap.get(AssociateDataMap.getWeight(sugarType) + sugarType);										
										list.add((AssociateData) child.getData(ASSOCIATE_DATA_KEY));
									}
								}
							}
						});

						final DocumentUploadOperations duo = new DocumentUploadOperations();
						Map<File, DocumentUploadObject> documentsAfterValidation = duo.buildUploadDocumentList(fileList);

						if (documentsAfterValidation != null && !documentsAfterValidation.isEmpty()) {
							final Composite[] progressComposite = new Composite[1];
							final Label[] progressLabel = new Label[1];
							final ProgressBar[] progressBar = new ProgressBar[1];

							Display.getDefault().asyncExec(new Runnable() {
								@Override
								public void run() {
									copyButton.setEnabled(false);
									progressComposite[0] = new Composite(FileUploadComposite.this, SWT.NONE);
									progressComposite[0].setLayout(GridLayoutFactory.fillDefaults().margins(5, 5).create());
									progressComposite[0].setLayoutData(GridDataFactory.fillDefaults().create());
									progressComposite[0].setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

									progressLabel[0] = new Label(progressComposite[0], SWT.WRAP);
									progressLabel[0].setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FILE_UPLOAD_UPLOADING_FILES));
									progressLabel[0].setLayoutData(GridDataFactory.fillDefaults().grab(false, false).create());

									progressBar[0] = new ProgressBar(progressComposite[0], SWT.INDETERMINATE);
									progressBar[0].setLayoutData(GridDataFactory.fillDefaults().grab(true, false).indent(5, 5).create());

									UiUtils.recursiveSetBackgroundColor(progressComposite[0], JFaceColors.getBannerBackground(Display.getDefault()));
									layout(true);
								}
							});

							final boolean isOK = duo.doUploadDocument(sugarItemMap, documentsAfterValidation);

							Display.getDefault().asyncExec(new Runnable() {
								@Override
								public void run() {
									if (!isOK) {
										String msg = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_UPLOAD_ERROR_MSG, new String[]{duo.getFailedFileNames()});

										msg = msg + "\n\n" + duo.getUploadErrorMsg(); //$NON-NLS-1$

										MessageDialog msgdialog = new MessageDialog(Display.getDefault().getActiveShell(), UtilsPlugin.getDefault().getResourceString(
												UtilsPluginNLSKeys.DOCUMENTS_UPLOAD_ERROR_TITLE), SFAImageManager.getImage(SFAImageManager.SALES_CONNECT), msg, MessageDialog.WARNING,
												new String[]{IDialogConstants.OK_LABEL}, 0);

										msgdialog.open();
									}

									copyButton.setEnabled(true);
									progressComposite[0].dispose();
									progressLabel[0].dispose();
									progressBar[0].dispose();

									uploadViewer.setInput(new HashSet<File>());
									currentlySelectedSugarIds.clear();

									// Dispose all of the buttons from the previous selection
									Control[] children = sugarItemAssociationComposite.getChildren();
									for (Control child : children) {
										child.dispose();
									}
									sugarItemAssociationComposite.layout(true);

									if (isOK) {
										showFinishedPanel = true;
									} else {
										showInstructions = true;
									}
									updateAssociateWidgetVisibility();
									updateUIState();
								}
							});
						}
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}
		});

		cancelButton = new Button(buttonComposite, SWT.PUSH);
		cancelButton.setLayoutData(GridDataFactory.fillDefaults().create());
		cancelButton.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WELCOME_BUTTON_CANCEL));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				currentlySelectedSugarIds.clear();
				for (Control child : sugarItemAssociationComposite.getChildren()) {
					child.dispose();
				}
				updateAssociateWidgetVisibility();
				showInstructions = true;
				updateUIState();
			}
		});

		updateAssociateWidgetVisibility();

		associationComposite.setVisible(false);
		((GridData) associationComposite.getLayoutData()).exclude = true;
		layout(true);
	}

	/**
	 * Creates the section that displays the category combo (Oppty, contact, or acct)
	 * 
	 * @param selectionWidgetComposite
	 */
	private void createCategorySection(Composite selectionWidgetComposite) {
		Label categoryLabel = new Label(selectionWidgetComposite, SWT.NONE);
		categoryLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FILE_UPLOAD_CATEGORY));

		categoryCombo = new SFACombo(selectionWidgetComposite, SWT.BORDER | SWT.READ_ONLY);
		categoryCombo.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		Vector<String> v = new Vector<String>();
		// 36368 - can not upload document to a contact
		// v.add(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CONTACT));
		v.add(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_OPPORTUNITY));
		v.add(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CLIENT));

		categoryCombo.setItems(v.toArray(new String[v.size()]));
		categoryCombo.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));
		categoryCombo.select(0);
		categoryCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent evt) {
				boolean enableCityParts = categoryCombo.getText().equals(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CLIENT));
				cityLabel.setEnabled(enableCityParts);
				cityText.setEnabled(enableCityParts);

				if (enableCityParts && cityText.getText().equalsIgnoreCase(ConstantStrings.EMPTY_STRING)) {
					setCityDefaultText();
				}
				setItemDefaultText(true);
			}
		});
	}

	/**
	 * Create the city filter section
	 * 
	 * @param selectionWidgetComposite
	 */
	private void createCitySection(Composite selectionWidgetComposite) {
		cityLabel = new Label(selectionWidgetComposite, SWT.NONE);
		cityLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FILE_UPLOAD_CITY));
		cityLabel.setEnabled(false);

		cityText = new Text(selectionWidgetComposite, SWT.BORDER);
		cityText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		cityText.setEnabled(false);
		cityText.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

		cityText.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent evt) {
				if (cityText.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CITY_HELP_TEXT))) {
					unsetCityDefaultText();
				}
			}

			@Override
			public void focusLost(FocusEvent evt) {
				if (cityText.getText().equalsIgnoreCase(ConstantStrings.EMPTY_STRING)) {
					setCityDefaultText();
				}
			}
		});
	}

	/**
	 * Create the item selection composite
	 */
	private void createItemSection(Composite selectionWidgetComposite) {
		Label itemLabel = new Label(selectionWidgetComposite, SWT.NONE);
		itemLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FILE_UPLOAD_ITEM));

		itemCombo = new SFACombo(selectionWidgetComposite, SWT.DROP_DOWN | SWT.BORDER);
		itemCombo.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		itemCombo.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent evt) {
				if (itemCombo.getText().equalsIgnoreCase(getItemComboDefaultText())) {
					unsetItemDefaultText();
				}
			}

			@Override
			public void focusLost(FocusEvent evt) {
				if (itemCombo.getText().equalsIgnoreCase(ConstantStrings.EMPTY_STRING)) {
					setItemDefaultText(true);
				}
			}
		});

		itemCombo.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent evt) {
				// If the user presses escape, clear anything in the combo
				if (evt.keyCode == SWT.ESC) {
					itemCombo.removeAll();
					itemCombo.setFocus();
				} else if (evt.keyCode == SWT.CR) {
					addSelectedItemToCopyList();
				} else if (evt.keyCode != SWT.ARROW_UP && evt.keyCode != SWT.ARROW_DOWN) {
					// Don't call the webservice until we have 2 characters
					if (!doingTypeaheadLookup && itemCombo.getText().trim().length() >= 2) {
						lastKeystrokeTime = System.currentTimeMillis();

						Thread t = new Thread() {
							@Override
							public void run() {
								try {
									sleep(500); // Wait a 1/2 second before attempting to process the typeahead web service
									long elapsedTime = System.currentTimeMillis() - lastKeystrokeTime;
									if (elapsedTime >= 500) { // Only process this if 1/2 second has elapsed since the user pressed the last key
										doingTypeaheadLookup = true;
										Job job = new Job("Typeahead lookup") { //$NON-NLS-1$
											@Override
											protected IStatus run(IProgressMonitor arg0) {
												// Stupid hack so I don't have to create instance variables for all of these to reference them from
												// outside the UI thread.
												final String[] city = new String[1];
												final boolean[] cityEnabled = new boolean[1];
												final String[] category = new String[1];
												final String[] item = new String[1];
												final boolean[] myItemsSelected = new boolean[1];

												Display.getDefault().syncExec(new Runnable() {
													@Override
													public void run() {
														city[0] = cityText.getText();
														cityEnabled[0] = cityText.isEnabled();
														category[0] = categoryCombo.getText();
														item[0] = itemCombo.getText();
														myItemsSelected[0] = myItemsButton.getSelection();
													}
												});

												getTypeaheadSuggestions(category[0], cityEnabled[0] && !city[0].equals(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CITY_HELP_TEXT))
														? city[0]
														: ConstantStrings.EMPTY_STRING, item[0], myItemsSelected[0]);
												return Status.OK_STATUS;
											}
										};
										job.schedule();
									}
								} catch (Exception e) {
									UtilsPlugin.getDefault().logException(e, FilesPluginActivator.PLUGIN_ID);
								}
							}
						};
						t.start();
					}
				}
			}
		});

		itemCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				if (!itemCombo.getListVisible()) {
					addSelectedItemToCopyList();
				}
			}
		});
	}

	/**
	 * Adds the selected sugar item to the list of things for the uploaded files to be associated with
	 * 
	 * @param parent
	 */
	private void addSelectedItemToCopyList() {
		int index = itemCombo.getSelectionIndex();
		if (index >= 0) {
			Map<Integer, JSONObject> dataMap = (Map<Integer, JSONObject>) itemCombo.getData();
			if (dataMap != null) {
				try {
					JSONObject jsonObj = dataMap.get(index);
					final String sugarId = jsonObj.getString("id"); //$NON-NLS-1$
					currentlySelectedSugarIds.add(sugarId);
					final SFAButtonWithX[] button = new SFAButtonWithX[1];
					button[0] = new SFAButtonWithX(sugarItemAssociationComposite, SWT.NONE, itemCombo.getText(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_REMOVE),
							new Action() {
								@Override
								public void run() {
									button[0].dispose();
									layout(true);
									currentlySelectedSugarIds.remove(sugarId);
									updateAssociateWidgetVisibility();
								}
							});
					button[0].setData(ASSOCIATE_DATA_KEY, new AssociateData(itemCombo.getText(), itemCombo.getText(), sugarId, false));
					button[0].setData(SUGAR_TYPE_KEY, sugarTypeMap.get(categoryCombo.getText()));
					updateAssociateWidgetVisibility();
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, FilesPluginActivator.PLUGIN_ID);
				}
			}
			itemCombo.setText(ConstantStrings.EMPTY_STRING);
			itemCombo.removeAll();
			itemCombo.setData(null);
			itemCombo.redraw();
		}
	}

	/**
	 * Create the instructions composite
	 * 
	 * @param parent
	 */
	private void createInstructionsComposite() {
		instructionsComposite = new Composite(this, SWT.NONE);
		instructionsComposite.setLayout(GridLayoutFactory.fillDefaults().margins(10, 10).create());
		instructionsComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		Composite borderComposite = new Composite(instructionsComposite, SWT.NONE);
		borderComposite.setLayout(GridLayoutFactory.fillDefaults().margins(5, 5).create());
		borderComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		final Label instructionsLabel = new Label(borderComposite, SWT.WRAP);
		instructionsLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FILE_UPLOAD_INSTRUCTIONS));
		instructionsLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		instructionsLabel.setForeground(SugarItemsDashboard.getInstance().getBusinessCardFieldLabelColor());
		instructionsLabel.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());

		borderComposite.addPaintListener(new PaintListener() {
			LineAttributes attributes = new LineAttributes(3, SWT.CAP_ROUND, SWT.JOIN_ROUND, SWT.LINE_DASH, new float[]{20f}, SWT.LINE_CUSTOM, 10);

			@Override
			public void paintControl(PaintEvent e) {
				Point size = instructionsLabel.getSize();
				int x = size.x;
				int y = size.y;

				e.gc.setLineAttributes(attributes);
				e.gc.setForeground(SugarItemsDashboard.getInstance().getBusinessCardFieldLabelColor());
				e.gc.drawRoundRectangle(1, 1, x + 7, y + 7, 10, 10);
			}
		});
	}

	/**
	 * Creates the drop zone composite
	 * 
	 * @param parent
	 */
	private void createDropComposite() {
		dropComposite = new Composite(this, SWT.NONE);
		dropComposite.setLayout(GridLayoutFactory.fillDefaults().margins(5, 5).create());
		dropComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		UiUtils.recursiveSetBackgroundColor(dropComposite, JFaceColors.getBannerBackground(Display.getDefault()));

		Table uploadTable = new Table(dropComposite, SWT.MULTI);
		uploadTable.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		uploadViewer = new UploadTableViewer(uploadTable);
		uploadViewer.setInput(new HashSet<File>());
		uploadViewer.getPropertyChangeSupport().addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				updateUIState();
			}
		});
	}

	/**
	 * Creates the composite the display that the the update has finished.
	 * 
	 * @param parent
	 */
	private void createUploadCompleteComposite() {
		uploadCompleteComposite = new Composite(this, SWT.NONE);
		uploadCompleteComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).create());
		uploadCompleteComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		Label finishedLabel = new Label(uploadCompleteComposite, SWT.WRAP);
		finishedLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		finishedLabel.setForeground(SugarItemsDashboard.getInstance().getBusinessCardFieldLabelColor());
		finishedLabel.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
		finishedLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FILE_UPLOAD_SUCCESSFUL_COPY));

		ImageHyperlink closeLink = new ImageHyperlink(uploadCompleteComposite, SWT.NONE);
		closeLink.setLayoutData(GridDataFactory.fillDefaults().grab(false, false).align(SWT.BEGINNING, SWT.BEGINNING).create());
		closeLink.setImage(SFAImageManager.getImage(SFAImageManager.DELETE));
		closeLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				showInstructions = true;
				showFinishedPanel = false;
				updateUIState();
			}
		});

		uploadCompleteComposite.setVisible(false);
		((GridData) uploadCompleteComposite.getLayoutData()).exclude = true;
	}

	/**
	 * Updates the enabled state of various widgets on the UI based on the the following rules.
	 * 
	 * 1) If no files are in the drop area, "Copy to SugarCRM" should be disabled and we should show the instructions panel. 2) If there are files in the drop area, show the instructions panel and
	 * disable the "copy to SugarCRM" button.
	 */
	private void updateUIState() {
		Collection input = (Collection) uploadViewer.getInput();
		if (showInstructions == false && !showFinishedPanel) {
			showInstructions = (input.size() == 0);
		}
		copyToSalesConnectItem.setEnabled(input.size() > 0);

		instructionsComposite.setVisible(showInstructions);
		((GridData) instructionsComposite.getLayoutData()).exclude = !showInstructions;

		associationComposite.setVisible(!showInstructions && !showFinishedPanel);
		((GridData) associationComposite.getLayoutData()).exclude = showInstructions || showFinishedPanel;

		uploadCompleteComposite.setVisible(showFinishedPanel);
		((GridData) uploadCompleteComposite.getLayoutData()).exclude = !showFinishedPanel;

		layout(true);
	}

	/**
	 * Set's the visible state of certain widgets based on whether any sugar items have been selected or not.
	 * 
	 * @param parent
	 */
	private void updateAssociateWidgetVisibility() {
		boolean anythingSelected = currentlySelectedSugarIds.size() > 0;
		copyButton.setEnabled(anythingSelected);

		copyToLabel.setVisible(anythingSelected);
		((GridData) copyToLabel.getLayoutData()).exclude = !anythingSelected;

		sugarItemAssociationComposite.setVisible(anythingSelected);
		((GridData) sugarItemAssociationComposite.getLayoutData()).exclude = !anythingSelected;

		sugarItemAssociationComposite.layout(true);
		layout(true);
	}

	public Font getItalicsFont() {
		if (italicsFont == null) {
			FontData fontData = cityText.getFont().getFontData()[0];
			italicsFont = new Font(Display.getDefault(), new FontData(fontData.getName(), fontData.getHeight(), SWT.ITALIC));
		}
		return italicsFont;
	}

	public Font getNormalFont() {
		if (normalFont == null) {
			FontData fontData = cityText.getFont().getFontData()[0];
			normalFont = new Font(Display.getDefault(), new FontData(fontData.getName(), fontData.getHeight(), SWT.NORMAL));
		}
		return normalFont;
	}

	/**
	 * Returns the default instruction text for the item selection combo
	 * 
	 * @return
	 */
	private String getItemComboDefaultText() {
		String text = ConstantStrings.EMPTY_STRING;
		if (categoryCombo.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CONTACT))) {
			text = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOOK_UP_CONTACT_HELP_TEXT);
		} else if (categoryCombo.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_OPPORTUNITY))) {
			text = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOOK_UP_OPPTY_HELP_TEXT);
		} else if (categoryCombo.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CLIENT))) {
			text = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOOK_UP_CLIENT_HELP_TEXT);
		}

		return text;
	}

	/**
	 * Switch the city text to normal edit mode
	 */
	private void unsetCityDefaultText() {
		cityText.setText(ConstantStrings.EMPTY_STRING);
		cityText.setForeground(categoryCombo.getForeground());
		cityText.setBackground(categoryCombo.getBackground());
		cityText.setFont(getNormalFont());
	}

	/**
	 * Show the default edit message in the city text
	 */
	private void setCityDefaultText() {
		// If enabled, set help hint with light grey color, and be sure the background is reset to parent's
		cityText.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CITY_HELP_TEXT));
		cityText.setForeground(JFaceResources.getColorRegistry().get("textFieldPromptColor")); //$NON-NLS-1$
		cityText.setBackground(categoryCombo.getBackground());
		cityText.setFont(getItalicsFont());
	}

	private void unsetItemDefaultText() {
		// The user is ready to enter an item value, we will clear up the help hint, set the foreground to regular color.
		itemCombo.setText(ConstantStrings.EMPTY_STRING);
		itemCombo.setForeground(categoryCombo.getForeground());
		itemCombo.setFont(getNormalFont());
	}

	private void setItemDefaultText(boolean showHelpText) {
		if (showHelpText) {
			itemCombo.setText(getItemComboDefaultText());
			itemCombo.setForeground(JFaceResources.getColorRegistry().get("textFieldPromptColor")); //$NON-NLS-1$      
			itemCombo.setFont(getItalicsFont());
		} else
		// If disabled, empty the help hint
		{
			itemCombo.setText(ConstantStrings.EMPTY_STRING);
		}
	}

	/**
	 * Gets the typeahead suggestions for the user
	 * 
	 * @param category
	 * @param city
	 * @param searchString
	 * @param searchMyItems
	 */
	private void getTypeaheadSuggestions(final String category, final String city, final String searchString, final boolean searchMyItems) {
		Job job = new Job("Getting typeahead suggestions") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				if (category.length() > 0) {

					String searchResults = SugarWebservicesOperations.getInstance().getTypeaheadInfoFromWebservice(sugarTypeMap.get(category), searchString, "30", //$NON-NLS-1$
							Boolean.toString(searchMyItems), city);

					final ArrayList<JSONObject> jsonObjectList = new ArrayList<JSONObject>();
					try {
						JSONObject searchResultsJSON = new JSONObject(searchResults);
						JSONArray resultsArray = searchResultsJSON.getJSONObject(ConstantStrings.RESULTS).getJSONArray(ConstantStrings.DATABASE_FIELDS);

						for (int i = 0; i < resultsArray.length(); i++) {
							JSONObject jsonObject = (JSONObject) resultsArray.get(i);
							// Filter out entries that have already been selected
							if (!currentlySelectedSugarIds.contains(jsonObject.getString("id"))) //$NON-NLS-1$
							{
								jsonObjectList.add(jsonObject);
							}
						}
					} catch (Exception e) {
						UtilsPlugin.getDefault().logException(e, FilesPluginActivator.PLUGIN_ID);
					}
					doingTypeaheadLookup = false;

					// If we got any results back, populate the combo box
					if (jsonObjectList.size() > 0) {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								try {
									String[] displayStrings = new String[jsonObjectList.size()];
									// We'll use this map to store detailed information about each entry in the combo box so
									// we can retrieve it if it used when an item is selected.
									Map<Integer, JSONObject> comboDataMap = new HashMap<Integer, JSONObject>();
									int ctr = 0;
									for (JSONObject result : jsonObjectList) {
										if (sugarTypeMap.get(category).equals(SugarType.ACCOUNTS.getParentType())) {
											displayStrings[ctr] = result.getString("name"); //$NON-NLS-1$
										} else if (sugarTypeMap.get(category).equals(SugarType.CONTACTS.getParentType())) {
											displayStrings[ctr] = result.getString("name"); //$NON-NLS-1$ 
										} else if (sugarTypeMap.get(category).equals(SugarType.OPPORTUNITIES.getParentType())) {
											displayStrings[ctr] = result.getString("name") + " (" + result.getString("description") + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$											
										}
										comboDataMap.put(ctr, result);
										ctr++;
									}
									String originalText = itemCombo.getText();
									Point originalSelection = itemCombo.getSelection();
									itemCombo.setData(comboDataMap);
									itemCombo.setItems(displayStrings);
									itemCombo.setListVisible(true);
									itemCombo.setFocus();
									itemCombo.setText(originalText);
									itemCombo.setSelection(originalSelection);
								} catch (Exception e) {
									UtilsPlugin.getDefault().logException(e, FilesPluginActivator.PLUGIN_ID);
								}
							}
						});
					} else {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								String originalText = itemCombo.getText();
								Point originalSelection = itemCombo.getSelection();
								itemCombo.setItems(new String[]{UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_RELATED_TYPE_NOMATCH_MSG)});
								itemCombo.setListVisible(true);
								itemCombo.setData(null); // Clear out the data map
								itemCombo.setFocus();
								itemCombo.setText(originalText);
								itemCombo.setSelection(originalSelection);
							}
						});
					}
				}
				return Status.OK_STATUS;
			}
		};

		job.schedule();
	}
}
