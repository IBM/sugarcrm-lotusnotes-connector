package com.ibm.socialcrm.notesintegration.files.dashboardcomposites;

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
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Geometry;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.ConnectionsDocument;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.core.utils.UpdateSelectionsBroadcaster;
import com.ibm.socialcrm.notesintegration.files.FilesPluginActivator;
import com.ibm.socialcrm.notesintegration.files.utils.DocumentDoubleClickedComposite;
import com.ibm.socialcrm.notesintegration.files.utils.DocumentUploadObject;
import com.ibm.socialcrm.notesintegration.files.utils.DocumentUploadOperations;
import com.ibm.socialcrm.notesintegration.files.views.DocumentsFilter;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateData;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateDataMap;
import com.ibm.socialcrm.notesintegration.ui.dashboardcomposites.AbstractDashboardComposite;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.widgets.EasyScrolledComposite;

public class DocumentsComposite extends AbstractDashboardComposite {

	private EasyScrolledComposite scrolledComposite;
	private Composite headerComposite;
	private Composite innerComposite;
	private Table table;
	private Label loadingLabel;
	private Label nothingToShowLabel;
	private Composite documentComposite;
	private TableViewer viewer;

	private String grayColor = "grayColor"; //$NON-NLS-1$
	private String blueColor = "blueColor"; //$NON-NLS-1$
	private static final int NAME_COL_WIDTH = 300;
	private static final int SIZE_COL_WIDTH = 140;
	private static final int CREATED_COL_WIDTH = 190;

	private Text filterTxt;
	private KeyListener filterTxtKeyListener;
	private PropertyChangeListener docPropertyChangedListener;
	private DocumentsFilter filter;
	private Font documentNameFont;
	private Font filterTextItalicFont;
	private Font filterTextNormalFont;

	private TreeMap<String, ConnectionsDocument> downloadDocumentsMap;
	private Map<File, String> uploadDocumentMap = null;
	private boolean dataLoaded = false;
	private TreeMap<String, List<AssociateData>> sugarItemMap = new TreeMap<String, List<AssociateData>>();

	private Map<String, DocumentDoubleClickedComposite> downloadDialogMap;

	public DocumentsComposite(Composite parent, int style, String dashboardID, BaseSugarEntry sugarEntry) {
		super(parent, style, dashboardID, sugarEntry);

		this.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				if (docPropertyChangedListener != null) {
					UpdateSelectionsBroadcaster.getInstance().unregisterListener(docPropertyChangedListener);
				}
			}
		});

		// don't need to call loadData() here, because abstract class' constructor calls
		// prepareForRebuild(). We will overload that method and call loadData().
		// loadData();
	}

	@Override
	public void createInnerComposite() {
		this.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

		headerComposite = new Composite(this, SWT.NONE);
		Rectangle margins = Geometry.createDiffRectangle(15, 10, 5, 10);
		headerComposite.setLayout(GridLayoutFactory.fillDefaults().spacing(0, 5).extendedMargins(margins).numColumns(1).equalWidth(false).create());
		headerComposite.setLayoutData(GridDataFactory.fillDefaults().hint(500, SWT.DEFAULT).grab(true, false).create());
		headerComposite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

		// loading label
		loadingLabel = new Label(headerComposite, SWT.NONE);
		loadingLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_LOADING_INFORMATION));
		loadingLabel.setLayoutData(GridDataFactory.fillDefaults().indent(10, 5).create());
		loadingLabel.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

		filterTxt = new Text(headerComposite, SWT.BORDER);
		setFilterDefaultText(true);
		// use 2 so filterTxt text kind of in the center
		filterTxt.setLayoutData(GridDataFactory.fillDefaults().indent(0, 2).hint(300, SWT.DEFAULT).grab(false, false).create());
		filterTxt.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));
		addFilterTextListeners();

		// document table
		scrolledComposite = new EasyScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setLayout(GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 0, 0).numColumns(1).create());
		scrolledComposite.setLayoutData(GridDataFactory.fillDefaults().indent(0, 0).grab(true, true).create());

		innerComposite = new Composite(scrolledComposite, SWT.NONE);
		Rectangle margins1 = Geometry.createDiffRectangle(5, 15, 0, 10);
		innerComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).extendedMargins(margins1).equalWidth(false).create());
		innerComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		innerComposite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

		documentComposite = new Composite(innerComposite, SWT.NONE);
		documentComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).equalWidth(false).create());
		documentComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		documentComposite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

		addPropertyChangeListener();

		scrolledComposite.setContent(innerComposite);
		innerComposite.layout(true);
		scrolledComposite.layout(true);

	}

	@Override
	public String getDashboardName() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_TAB);
	}

	// method is obsoleted, should be removed
	// /**
	// * Build the whole panel
	// */
	// private void buildComposite() {
	// buildTable();
	// }

	@Override
	public void selectedItemsChanged() {
	}

	/**
	 * Loads the contact data from the webservice for this account
	 */
	private void loadData() {
		final JSONArray[] itemArray = new JSONArray[1];
		dataLoaded = false;
		Job job = new Job("Retrieving documents " + getSugarEntry().getName()) //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				getDocumentsMap().clear();

				try {

					String out = SugarWebservicesOperations.getInstance().getDocumentData(getSugarEntry().getSugarType(), getSugarEntry().getId());
					if (out != null && !out.equals(ConstantStrings.EMPTY_STRING)) {
						JSONObject responseJSON = new JSONObject(out);
						final JSONArray[] itemArray = new JSONArray[1];
						if (responseJSON.containsKey("entry_list") && responseJSON.getJSONArray("entry_list").length() > 0) { //$NON-NLS-1$ //$NON-NLS-2$
							itemArray[0] = responseJSON.getJSONArray("entry_list").getJSONObject(0).getJSONArray("related_records"); //$NON-NLS-1$ //$NON-NLS-2$
						}

						if (itemArray[0] != null) {
							for (int i = 0; i < itemArray[0].length(); i++) {
								JSONObject jsonObject = itemArray[0].getJSONObject(i);

								String sugarId = ConstantStrings.EMPTY_STRING;
								if (jsonObject.containsKey("sugar_id")) //$NON-NLS-1$
								{
									if (!jsonObject.isNull("sugar_id")) {
										sugarId = GenericUtils.decodeJSONValue(jsonObject.getString("sugar_id"));//$NON-NLS-1$
									}
								}

								String fileName = ConstantStrings.EMPTY_STRING;
								if (jsonObject.containsKey("field_values") && jsonObject.getJSONObject("field_values").containsKey("document_name")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								{
									fileName = GenericUtils.decodeJSONValue(jsonObject.getJSONObject("field_values").getJSONObject("document_name").getString("value"));//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								}

								String cDate = ConstantStrings.EMPTY_STRING;
								if (jsonObject.containsKey("field_values") && jsonObject.getJSONObject("field_values").containsKey("date_entered")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								{
									cDate = GenericUtils.decodeJSONValue(jsonObject.getJSONObject("field_values").getJSONObject("date_entered").getString("value")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								}

								String mDate = ConstantStrings.EMPTY_STRING;
								if (jsonObject.containsKey("field_values") && jsonObject.getJSONObject("field_values").containsKey("date_modified")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								{
									mDate = GenericUtils.decodeJSONValue(jsonObject.getJSONObject("field_values").getJSONObject("date_modified").getString("value")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								}

								String author = ConstantStrings.EMPTY_STRING;
								if (jsonObject.containsKey("field_values") //$NON-NLS-1$
										&& jsonObject.getJSONObject("field_values").containsKey("assigned_user_name")) //$NON-NLS-1$ //$NON-NLS-2$
								{
									author = GenericUtils.decodeJSONValue(jsonObject.getJSONObject("field_values").getJSONObject("assigned_user_name").getString("value")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								}

								String sizeX = ConstantStrings.EMPTY_STRING;
								if (jsonObject.containsKey("field_values") //$NON-NLS-1$
										&& jsonObject.getJSONObject("field_values").containsKey("file_size")) //$NON-NLS-1$ //$NON-NLS-2$
								{
									sizeX = GenericUtils.decodeJSONValue(jsonObject.getJSONObject("field_values").getJSONObject("file_size").getString("value")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								}
								int size = sizeX.equals(ConstantStrings.EMPTY_STRING) ? 0 : Integer.parseInt(sizeX);

								String docUrl = ConstantStrings.EMPTY_STRING;
								if (jsonObject.containsKey("field_values") //$NON-NLS-1$
										&& jsonObject.getJSONObject("field_values").containsKey("doc_url")) //$NON-NLS-1$ //$NON-NLS-2$
								{
									docUrl = GenericUtils.decodeJSONValue(jsonObject.getJSONObject("field_values").getJSONObject("doc_url").getString("value")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								}

								ConnectionsDocument cd = new ConnectionsDocument(sugarId, fileName, cDate, mDate, author, size, docUrl);
								getDocumentsMap().put(cd.getKey(), cd);
							}
						}
					}

					dataLoaded = true;

				}

				catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, FilesPluginActivator.PLUGIN_ID);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private void addPropertyChangeListener() {
		if (docPropertyChangedListener == null) {
			docPropertyChangedListener = new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					if (evt != null) {
						// "DASHBOARD_UPDATE" + new value is an array of document names: this event is triggered when user D&D documents to the card
						if (evt.getPropertyName() != null && evt.getPropertyName().equals(UpdateSelectionsBroadcaster.DASHBOARD_UPDATE)) {

							if (evt.getNewValue() != null && evt.getNewValue() instanceof String[] && ((String[]) evt.getNewValue()).length > 0 && isForMe(evt.getNewValue()))

							{
								toDoUploadForButtonOrDND((String[]) evt.getNewValue());

							}
							// "DASHBOARD_UPDATE" + new value is the baseSugarEntry: this event is triggered when document(s) upload completed successfully.
							else if (evt.getNewValue() != null && evt.getNewValue() instanceof BaseSugarEntry && ((BaseSugarEntry) evt.getNewValue()).getId().equals(getSugarEntry().getId()))

							{
								doRefresh();
							}
						}
					}
				}
			};
			UpdateSelectionsBroadcaster.getInstance().registerListener(docPropertyChangedListener);
		}
	}

	private boolean isForMe(Object obj) {
		boolean isForMe = false;
		if (obj != null && obj instanceof String[] && ((String[]) obj)[0] instanceof String) {
			String s1 = (String) ((String[]) obj)[0];
			if (s1 != null && getSugarEntry() != null && getSugarEntry().getId() != null && s1.equalsIgnoreCase(getSugarEntry().getId())) {
				isForMe = true;
			}
		}
		return isForMe;
	}

	private void addFilterTextListeners() {

		filterTxtKeyListener = new KeyListener() {
			@Override
			public void keyPressed(KeyEvent arg0) {
				if (filterTxt.getText() != null && filterTxt.getText().equals(getFilterHint())) {
					setFilterDefaultText(false);
				}
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
				filter.setSearchString(filterTxt.getText());
				viewer.refresh();
				if (filterTxt.getText() != null && filterTxt.getText().equals(ConstantStrings.EMPTY_STRING)) {
					setFilterDefaultText(true);
				}

			}

		};
		filterTxt.addKeyListener(filterTxtKeyListener);

		filterTxt.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				if (filterTxt != null && !filterTxt.isDisposed()) {
					if (filterTxtKeyListener != null) {
						filterTxt.removeKeyListener(filterTxtKeyListener);
					}
				}
			}
		});

	}

	private String getFilterHint() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_FILTER_DEFAULT_TEXT);
	}

	private void setFilterDefaultText(boolean b) {
		if (b) {
			filterTxt.setText(getFilterHint());
			Color color = JFaceResources.getColorRegistry().get("sfaToggleUnpressedDisabledText"); //$NON-NLS-1$
			filterTxt.setForeground(color);
			// set italic
			filterTxt.setFont(getFilterTextFont(true));
		} else
		// empty the help hint
		{
			filterTxt.setText(ConstantStrings.EMPTY_STRING);
			Color color = viewer.getTable().getForeground();
			filterTxt.setForeground(color);
			// unset italic
			filterTxt.setFont(getFilterTextFont(false));
		}

	}

	@Override
	protected void prepareForRebuild() {
		super.prepareForRebuild();
	}

	/**
	 * Builds the Activities table. This method asynchronously loads the Activities data and then passes the data to another helper method to build the table.
	 */
	private void buildTable() {
		if (table == null || table.isDisposed()) {

			eraseControl(documentComposite);
			eraseControl(filterTxt);
			revitalizeControl(loadingLabel);

			// ***
			this.layout(true);

			Job job = new Job("Waiting to build Documents table for " + getSugarEntry().getName()) //$NON-NLS-1$
			{
				@Override
				protected IStatus run(IProgressMonitor arg0) {
					while (!dataLoaded) {
						try {
							Thread.sleep(1500);
						} catch (InterruptedException e) {
							UtilsPlugin.getDefault().logException(e, FilesPluginActivator.PLUGIN_ID);
						}
					}

					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							try {
								if (getDocumentsMap() != null && !getDocumentsMap().isEmpty()) {

									revitalizeControl(filterTxt);
									revitalizeControl(documentComposite);

									doBuildTable();

								} else {
									eraseControl(loadingLabel);

									if (nothingToShowLabel == null || nothingToShowLabel.isDisposed()) {
										nothingToShowLabel = new Label(headerComposite, SWT.WRAP);
									} else {
										revitalizeControl(nothingToShowLabel);
									}

									nothingToShowLabel.setLayoutData(GridDataFactory.fillDefaults().indent(10, 10).grab(true, true).create());
									nothingToShowLabel.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
									nothingToShowLabel.setText(getSugarType().equals(SugarType.ACCOUNTS)
											? UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.NO_DOCUMENTS_FOR_CLIENT)
											: getSugarType().equals(SugarType.OPPORTUNITIES)
													? UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.NO_DOCUMENTS_FOR_OPPORTUNITY)
													: UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.NO_DOCUMENTS_FOR_OPPORTUNITY));
									nothingToShowLabel.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

									if (downloadButton != null) {
										downloadButton.getButton().setEnabled(false);
									}

									headerComposite.layout(true);
									headerComposite.update();
									headerComposite.getParent().layout(true);

								}
							}

							catch (java.lang.IllegalArgumentException e) {
								if (DocumentsComposite.this.isDisposed()) {
								} else {
									UtilsPlugin.getDefault().logException(e, FilesPluginActivator.PLUGIN_ID);
								}
							} catch (Exception e) {
								UtilsPlugin.getDefault().logException(e, FilesPluginActivator.PLUGIN_ID);

							}
						}
					});
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}
	}

	private void eraseControl(Control c) {
		if (c != null && !c.isDisposed()) {
			c.setVisible(false);
			((GridData) c.getLayoutData()).exclude = true;
		}
	}

	private void revitalizeControl(Control c) {
		if (c != null && !c.isDisposed()) {
			c.setVisible(true);
			((GridData) c.getLayoutData()).exclude = false;
		}
	}

	/**
	 * Helper method to actually populate the table with the contact data
	 * 
	 */
	private void doBuildTable() {
		// do NOT forget SWT.FULL_SELECTION. This flag will highlight all the cells for a given row when selected.
		viewer = new TableViewer(documentComposite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		table = viewer.getTable();

		Rectangle margins = Geometry.createDiffRectangle(5, 10, 0, 15);
		table.setLayout(GridLayoutFactory.fillDefaults().extendedMargins(margins).create());
		table.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).indent(10, 0).create());

		table.setHeaderVisible(true);
		table.setFont(SugarItemsDashboard.getInstance().getTableHeaderFont());
		table.setForeground(SugarItemsDashboard.getInstance().getBusinessCardHeaderTextColor());
		table.setLinesVisible(false);
		table.setToolTipText(ConstantStrings.EMPTY_STRING);

		addTableListeners();

		filter = new DocumentsFilter();
		viewer.addFilter(filter);

		TableViewerColumn column = new TableViewerColumn(viewer, SWT.SELECTED);
		column.getColumn().setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_NAME_COLUMN_NAME));
		column.getColumn().setWidth(NAME_COL_WIDTH);
		column.setLabelProvider(new ImageCellLabelProvider(column));

		column = new TableViewerColumn(viewer, SWT.SELECTED);
		column.getColumn().setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_SIZE_COLUMN_NAME));
		column.getColumn().setWidth(SIZE_COL_WIDTH);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public Image getImage(Object element) {
				return null;
			}

			@Override
			public String getText(Object element) {
				return ((ConnectionsDocument) element).getFormattedSize();
			}

			@Override
			public Color getBackground(Object arg0) {
				return JFaceColors.getBannerBackground(Display.getDefault());
			}
		});

		column = new TableViewerColumn(viewer, SWT.SELECTED);
		column.getColumn().setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DATE_COLUMN_NAME));
		int availableSpace = documentComposite.getBounds().width - NAME_COL_WIDTH - SIZE_COL_WIDTH;

		column.getColumn().setWidth(CREATED_COL_WIDTH);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public Image getImage(Object element) {
				return null;
			}

			@Override
			public String getText(Object element) {
				String CDate = ((ConnectionsDocument) element).getFormattedCDate();
				return CDate;
			}

			@Override
			public Color getBackground(Object arg0) {
				return JFaceColors.getBannerBackground(Display.getDefault());
			}
		});

		viewer.setContentProvider(new ViewContentProvider());
		viewer.setInput(getSortedConnectionsDocuments());

		packTable(table, new int[]{0});
		eraseControl(loadingLabel);

		headerComposite.layout(true);
		innerComposite.layout(true);
	}

	private void addTableListeners() {
		final Listener measureItemListener = new Listener() {
			public void handleEvent(Event event) {
				// use a default image to calculate the approximate width
				Image image = SFAImageManager.getImage(SFAImageManager.SALES_CONNECT);
				event.height = image.getBounds().y + 35;
			}
		};
		table.addListener(SWT.MeasureItem, measureItemListener);

		final Listener doubleClickListener = new Listener() {

			public void handleEvent(Event event) {
				toDownload();
			}
		};
		table.addListener(SWT.MouseDoubleClick, doubleClickListener);

		final Listener selectionListener = new Listener() {

			public void handleEvent(Event event) {
				if (downloadButton != null) {
					downloadButton.setEnabled(true);
				}
			}
		};
		table.addListener(SWT.Selection, selectionListener);

		// We need this erase listener to make the selection behavior look good on Windows. Seems to make things worse on Mac though.
		if (!GenericUtils.isMac()) {
			final Listener eraseitemListener = new Listener() {
				public void handleEvent(Event event) {
					event.detail &= ~SWT.HOT;
					if ((event.detail & SWT.SELECTED) == 0)
						return; // / item not selected

					Table table = (Table) event.widget;
					int clientWidth = table.getClientArea().width;
					GC gc = event.gc;
					Color oldBackground = gc.getBackground();

					gc.setBackground(getSelectedColor());
					gc.fillRectangle(0, event.y, clientWidth, event.height);
					gc.setBackground(oldBackground);
					event.detail &= ~SWT.SELECTED;
				}
			};
			table.addListener(SWT.EraseItem, eraseitemListener);
		}

		// Hack to set the width of the Subject column - without this logic, you'll see an extra column when resizing
		// the table.
		final ControlListener controlListener = new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				TableColumn nameColumn = table.getColumn(0);
				int availableSpace = table.getSize().x - (table.getColumn(1).getWidth() + table.getColumn(2).getWidth() + 5);
				nameColumn.setWidth(Math.max(NAME_COL_WIDTH, availableSpace));
			}
		};
		table.addControlListener(controlListener);
	}

	// double clicked or pressed the download tool bar menu action
	public void toDownload() {
		TableItem[] selection = viewer.getTable().getSelection();
		if (selection != null && selection.length > 0) {
			Object obj = selection[0].getData();
			if (obj != null && obj instanceof ConnectionsDocument) {
				ConnectionsDocument cd = (ConnectionsDocument) obj;
				doDoubleClick(cd);
			}
		}
	}

	public void toUpload() {
		// Browse and select a file
		FileDialog fileDialog = new FileDialog(Display.getDefault().getActiveShell(), SWT.MULTI);
		final String firstFile = fileDialog.open();
		if (firstFile != null) {
			String[] filesToUpload = fileDialog.getFileNames();
			if (filesToUpload != null && filesToUpload.length > 0 && fileDialog.getFilterPath() != null) {
				for (int i = 0; i < filesToUpload.length; i++) {
					filesToUpload[i] = fileDialog.getFilterPath() + ConstantStrings.SLASH + filesToUpload[i];
				}
				toDoUploadForButtonOrDND(filesToUpload);
			}
		}
	}

	private void toDoUploadForButtonOrDND(final String[] files) {
		if (files != null && files.length > 0) {

			final DocumentUploadOperations duo = new DocumentUploadOperations();

			// Build Sugar items to be related
			List<AssociateData> list = new ArrayList<AssociateData>();
			list.add(new AssociateData(getSugarEntry().getName(), getSugarEntry().getName(), getSugarEntry().getId(), false));
			sugarItemMap.put(AssociateDataMap.getWeight(getSugarEntry().getSugarType().getParentType()) + getSugarEntry().getSugarType().getParentType(), list);

			final String progressId = getProgressDisplayer().createProgressIndicator(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_UPLOADING_PROGRESS_TEXT));

			Job job = new Job("File Upload Conflict Resolution") //$NON-NLS-1$
			{
				@Override
				protected IStatus run(IProgressMonitor monitor) {

					// Build files to be processed
					getUploadDocumentMap().clear();
					for (int i = 0; i < files.length; i++) {
						getUploadDocumentMap().put(new File(files[i]), files[i]);
					}
					Set<File> filesToBeProcessed = getUploadDocumentMap().keySet();
					Map<File, DocumentUploadObject> documentsAfterValidation = duo.buildUploadDocumentList(filesToBeProcessed);

					if (documentsAfterValidation != null) {
						// Really doing upload now
						final boolean isOK = duo.doUploadDocument(sugarItemMap, documentsAfterValidation);

						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								getProgressDisplayer().removeProgressIndicator(progressId);
								if (isOK) {
									// broadcast the refresh request
									UpdateSelectionsBroadcaster.getInstance().updateDashboard(getSugarEntry());
								} else {
									String msg = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_UPLOAD_ERROR_MSG, new String[]{duo.getFailedFileNames()});

									msg = msg + "\n\n" + duo.getUploadErrorMsg();

									MessageDialog msgdialog = new MessageDialog(Display.getDefault().getActiveShell(), UtilsPlugin.getDefault().getResourceString(
											UtilsPluginNLSKeys.DOCUMENTS_UPLOAD_ERROR_TITLE), SFAImageManager.getImage(SFAImageManager.SALES_CONNECT), msg, MessageDialog.WARNING,
											new String[]{IDialogConstants.OK_LABEL}, 0);

									msgdialog.open();

								}
							}
						});
					} else {

						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								getProgressDisplayer().removeProgressIndicator(progressId);
							}
						});

					}
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}
	}

	private void doDoubleClick(ConnectionsDocument cd) {
		if (cd == null)
			return;

		if (getDownloadDialogMap() == null || getDownloadDialogMap().isEmpty() || !getDownloadDialogMap().containsKey(cd.getFilename())) {
			// bring up file download option dialog
			Display display = Display.getCurrent();
			DocumentDoubleClickedComposite dialog = new DocumentDoubleClickedComposite(display, new ConnectionsDocument[]{cd});
			getDownloadDialogMap().put(cd.getFilename(), dialog);

			while (dialog != null && dialog.getShell() != null && !dialog.getShell().isDisposed()) {
				if (display != null && !display.readAndDispatch()) {
					display.sleep();
				}
			}
			dialog = null;
			getDownloadDialogMap().remove(cd.getFilename());
		} else {
			// extract existing dialog from map
			DocumentDoubleClickedComposite dialog = getDownloadDialogMap().get(cd.getFilename());
			Display display = Display.getCurrent();
			dialog.getShell().forceActive();
			while (dialog != null && dialog.getShell() != null && !dialog.getShell().isDisposed()) {
				if (display != null && !display.readAndDispatch()) {
					display.sleep();
				}
			}
			dialog = null;
			getDownloadDialogMap().remove(cd.getFilename());
		}
	}

	private Map<String, DocumentDoubleClickedComposite> getDownloadDialogMap() {
		if (downloadDialogMap == null) {
			downloadDialogMap = new HashMap<String, DocumentDoubleClickedComposite>();
		}
		return downloadDialogMap;
	}
	public Image getFileImage(String s) {
		Image image = SFAImageManager.getImage(SFAImageManager.SALES_CONNECT); // default

		if (s != null) {

			if (s.lastIndexOf(ConstantStrings.PERIOD) > -1) {

				String fileSuffix = s.substring(s.lastIndexOf(ConstantStrings.PERIOD) + 1);
				if (Program.findProgram(fileSuffix) != null) {
					ImageData iconData = Program.findProgram(fileSuffix).getImageData();
					if (iconData != null) {
						image = new Image(Display.getDefault(), iconData);
					}
				}
			}
		}
		return image;
	}

	public Font getDocumentNameFont() {
		if (documentNameFont == null) {
			documentNameFont = SugarItemsDashboard.getInstance().getBusinessCardLabelFont();
			// button.setFont(font); //Again, doing this so it can compute its size properly
		}
		return documentNameFont;
	}

	private Font getFilterTextFont(boolean isItalic) {
		Font font = null;
		if (isItalic) {
			if (filterTextItalicFont == null) {
				String fontName = "filtertext-italic"; //$NON-NLS-1$
				if (JFaceResources.getFontRegistry().hasValueFor(fontName)) {
					filterTextItalicFont = JFaceResources.getFontRegistry().get(fontName);
				} else {
					FontData[] fontData = filterTxt.getFont().getFontData();
					fontData[0].setStyle(SWT.ITALIC);
					JFaceResources.getFontRegistry().put(fontName, fontData);
					filterTextItalicFont = JFaceResources.getFontRegistry().get(fontName);
				}
			}
			font = filterTextItalicFont;
		} else {
			if (filterTextNormalFont == null) {
				String fontName = "filtertext-normal"; //$NON-NLS-1$
				if (JFaceResources.getFontRegistry().hasValueFor(fontName)) {
					filterTextNormalFont = JFaceResources.getFontRegistry().get(fontName);
				} else {
					FontData[] fontData = filterTxt.getFont().getFontData();
					fontData[0].setStyle(SWT.NORMAL);
					JFaceResources.getFontRegistry().put(fontName, fontData);
					filterTextNormalFont = JFaceResources.getFontRegistry().get(fontName);
				}
			}
			font = filterTextNormalFont;
		}
		return font;
	}

	private Color getGrayColor() {
		boolean isHighContrast = Display.getDefault().getHighContrast();
		Color graycolor = JFaceResources.getColorRegistry().get(grayColor);
		if (graycolor == null) {
			if (isHighContrast) {
				graycolor = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
				JFaceResources.getColorRegistry().put(grayColor, graycolor.getRGB());
			} else {
				graycolor = new Color(null, new RGB(243, 243, 243));
				JFaceResources.getColorRegistry().put(grayColor, graycolor.getRGB()); /* gray */
			}
		}
		return graycolor;
	}

	private Color getSelectedColor() {
		boolean isHighContrast = Display.getDefault().getHighContrast();
		Color selectedcolor = JFaceResources.getColorRegistry().get(blueColor);
		if (selectedcolor == null) {
			if (isHighContrast) {
				selectedcolor = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
				JFaceResources.getColorRegistry().put(blueColor, selectedcolor.getRGB());
			} else {
				selectedcolor = new Color(null, new RGB(113, 162, 226));
				JFaceResources.getColorRegistry().put(blueColor, selectedcolor.getRGB()); /* blue */
			}
		}

		return selectedcolor;
	}

	private int getOpacity(double factor) {

		double d = 255 * factor;
		int i = (int) d;
		return i;
	}

	public Map<String, ConnectionsDocument> getDocumentsMap() {
		if (downloadDocumentsMap == null) {
			downloadDocumentsMap = new TreeMap<String, ConnectionsDocument>();
		}

		return downloadDocumentsMap;
	}

	public Map<File, String> getUploadDocumentMap() {
		if (uploadDocumentMap == null) {
			uploadDocumentMap = new HashMap<File, String>();
		}
		return uploadDocumentMap;
	}

	// get list in Date descending order
	public List<ConnectionsDocument> getSortedConnectionsDocuments() {

		List<ConnectionsDocument> c = new ArrayList(getDocumentsMap().values());
		Collections.sort(c, new Comparator<ConnectionsDocument>() {
			@Override
			public int compare(ConnectionsDocument cd1, ConnectionsDocument cd2) {
				int compare = cd2.getCDate().compareTo(cd1.getCDate());

				return compare;
			}
		});
		return c;
	}

	private void doRefresh() {
		// Run it in background to avoid thread violation
		Job job = new Job("Refreshing business card") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				getParentViewPart().refreshAll();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			Object[] objs = null;
			if (parent != null && parent instanceof List) {
				List<ConnectionsDocument> list = (List<ConnectionsDocument>) parent;
				objs = list.toArray(new ConnectionsDocument[list.size()]);
			} else {
				objs = new ConnectionsDocument[0];
			}
			return objs;
		}
	}

	// Mainly to make image and text line up nicely
	private class ImageCellLabelProvider extends OwnerDrawLabelProvider {

		private TableViewerColumn column;

		public ImageCellLabelProvider(TableViewerColumn col) {
			column = col;
		}
		@Override
		protected void measure(Event event, Object obj) {
		}

		@Override
		protected void paint(Event event, Object obj) {
			String documentFileName = ConstantStrings.EMPTY_STRING;
			if (obj != null && obj instanceof ConnectionsDocument) {
				documentFileName = ((ConnectionsDocument) obj).getFilename();
			}
			Rectangle bounds = event.getBounds();

			int columnWidth = column.getColumn().getWidth() - 21; // Hardcoded width of file icon
			int charWidth = event.gc.getFontMetrics().getAverageCharWidth() + 1; // The +1 takes care of the extra pixel between the characters
			int textWidth = event.gc.stringExtent(documentFileName).x;

			// Figure out if we need to truncate the text
			String stringToDraw = documentFileName;
			if (textWidth > columnWidth) {
				int numChars = columnWidth / charWidth;
				stringToDraw = documentFileName.substring(0, numChars);
			}

			event.gc.setAlpha(getOpacity(0.95));
			event.gc.drawImage(getFileImage(documentFileName), bounds.x, bounds.y + 8);
			event.gc.drawText(stringToDraw, bounds.x + 21, bounds.y + 8, true);
		}
	}

	@Override
	public void afterBaseCardDataRetrieved() {
		super.afterBaseCardDataRetrieved();

		if (downloadButton != null) {
			downloadButton.getButton().setEnabled(false);
		}

		loadData();
		buildTable();

		innerComposite.layout(true);

		scrolledComposite.layout(true);

	}
}
