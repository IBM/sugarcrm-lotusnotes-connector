package com.ibm.socialcrm.notesintegration.opportunities.dashboardComposites;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.json.JSONArray;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarAccount;
import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.core.SugarOpportunity;
import com.ibm.socialcrm.notesintegration.core.utils.SugarV10APIManager;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.opportunities.OpportunitiesActivator;
import com.ibm.socialcrm.notesintegration.ui.dashboardcomposites.AbstractDashboardComposite;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.ui.utils.WebserviceDataLoadProgressMonitor;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.widgets.EasyScrolledComposite;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFAHyperlink;

public class OpportunitiesComposite extends AbstractDashboardComposite {
	// 81013 - move it to SugarV10APIManager
	// private static final int MAX_OPPORTUNITIES_TO_BE_DISPLAYED = 50;
	private EasyScrolledComposite scrolledComposite;
	private Composite innerComposite;
	private Table table;
	private Label nothingToShowLabel;
	private SFAHyperlink opptyLink;
	private int displayTotal = 0;

	private static final int NAME_COL_WIDTH = 125;
	private static final int AMOUNT_COL_WIDTH = 115;
	private static final int DATE_COL_WIDTH = 115;
	private static final int STAGE_COL_WIDTH = 155;
	private static final int DESCRIPTION_COL_WIDTH = 100;

	private boolean dataLoaded = false;

	private int _maxRowHeight = 55;

	public OpportunitiesComposite(Composite parent, int style, String dashboardID, BaseSugarEntry sugarEntry) {
		super(parent, style, dashboardID, sugarEntry);

	}

	private int getRowHeight(String s) {
		int height = _maxRowHeight;
		GC gc = new GC(Display.getDefault());
		Point size = gc.textExtent(s); // or textExtent
		height = Math.max(_maxRowHeight, size.y * 3 + 5 /* set row height to 3 lines + a bit padding */);
		gc.dispose();
		return height;
	}

	@Override
	public void createInnerComposite() {
		scrolledComposite = new EasyScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).create());
		scrolledComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		innerComposite = new Composite(scrolledComposite, SWT.NONE);
		innerComposite.setLayout(GridLayoutFactory.fillDefaults().margins(HEADER_MARGIN, HEADER_MARGIN).create());
		innerComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		scrolledComposite.setContent(innerComposite);
		innerComposite.layout(true);

		// For tables that where we have a custom TableEditor AND something larger than the
		// default size, Mac just doesn't paint those correctly. So, we have a hack to bump
		// the shell size to force it to repaint (note: Just calling repaint(), refresh(), or
		// redraw() doesn't work).
		final boolean[] macHackImplemented = new boolean[]{false};
		innerComposite.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent arg0) {
				if (GenericUtils.isMac() && !macHackImplemented[0]) {
					macHackImplemented[0] = true;
					Point p = getShell().getSize();
					getShell().setSize(p.x, p.y + 1);
				}
			}
		});
	}

	@Override
	public String getDashboardName() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_OPPORTUNITIES_TAB);
	}

	/**
	 * Builds the table that contains the opportunity data
	 */
	private void buildTable() {
		if (table == null || table.isDisposed()) {
			Job job = new Job("Waiting to build Line Item table for " + getSugarEntry().getName()) //$NON-NLS-1$
			{
				@Override
				protected IStatus run(IProgressMonitor arg0) {
					while (!dataLoaded) {
						try {
							Thread.sleep(1500);
						} catch (InterruptedException e) {
							UtilsPlugin.getDefault().logException(e, OpportunitiesActivator.PLUGIN_ID);
						}
					}

					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {

							try {
								if (table == null || table.isDisposed()) {
									final List<String> opportunityIDs = new ArrayList<String>();
									if (getSugarType().equals(SugarType.ACCOUNTS)) {
										SugarAccount account = (SugarAccount) getSugarEntry();
										opportunityIDs.addAll(account.getOpportunityIDs());
									} else if (getSugarType().equals(SugarType.CONTACTS)) {
										SugarContact contact = (SugarContact) getSugarEntry();
										opportunityIDs.addAll(contact.getOpportunityIDs());
									}

									displayTotal = Math.min(SugarV10APIManager.MAX_OPPORTUNITIES_TO_BE_DISPLAYED, opportunityIDs.size());

									if (!opportunityIDs.isEmpty()) {
										// Note, this doesn't use a table viewer. To get things to line up vertically with non-default sizing, we had
										// to create custom editors for each of the cells. Since the info on here is static, there's really no advantage
										// to using a TableViewer.
										table = new Table(innerComposite, SWT.BORDER | SWT.HIDE_SELECTION);
										table.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
										table.setHeaderVisible(true);
										table.setFont(SugarItemsDashboard.getInstance().getTableHeaderFont());
										table.setForeground(SugarItemsDashboard.getInstance().getBusinessCardHeaderTextColor());
										table.setLinesVisible(true);

										// Hack to set the row height
										table.addListener(SWT.MeasureItem, new Listener() {
											public void handleEvent(Event event) {
												// 60613
												// event.height = 55;
												event.height = _maxRowHeight;
											}
										});

										TableColumn column = new TableColumn(table, SWT.LEFT | SWT.BEGINNING, 0);
										column.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OPPORTUNITY));
										column.setWidth(NAME_COL_WIDTH);
										column.setWidth(NAME_COL_WIDTH);

										column = new TableColumn(table, SWT.LEFT | SWT.BEGINNING, 1);
										column.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.AMOUNT));
										column.setWidth(AMOUNT_COL_WIDTH);

										column = new TableColumn(table, SWT.LEFT | SWT.BEGINNING, 2);
										column.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DATE));
										column.setWidth(DATE_COL_WIDTH);

										column = new TableColumn(table, SWT.LEFT | SWT.BEGINNING, 3);
										column.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.STAGE));
										column.setWidth(STAGE_COL_WIDTH);

										column = new TableColumn(table, SWT.LEFT | SWT.BEGINNING, 4);
										column.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DESCRIPTION));
										column.setWidth(DESCRIPTION_COL_WIDTH);

										// 60613
										if (column.getText() != null) {
											_maxRowHeight = getRowHeight(column.getText());
										}

										int ctr = 0;
										for (String opptyId : opportunityIDs) {

											// hit the limit, stop sending to UI
											if (ctr == SugarV10APIManager.MAX_OPPORTUNITIES_TO_BE_DISPLAYED) {
												break;
											}

											final SugarOpportunity opportunity = (SugarOpportunity) SugarWebservicesOperations.getInstance().getSugarEntryById(opptyId);
											TableItem item = new TableItem(table, ctr);

											// Create the link to the oppty card
											Composite linkComposite = new Composite(table, SWT.NONE);
											linkComposite.setLayout(GridLayoutFactory.fillDefaults().margins(0, 5).create());

											Hyperlink opptyLink = new Hyperlink(linkComposite, SWT.NONE);
											opptyLink.setText(opportunity.getName());
											opptyLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
											opptyLink.setFont(SugarItemsDashboard.getInstance().getTableItemFont());
											opptyLink.addHyperlinkListener(new HyperlinkAdapter() {
												@Override
												public void linkActivated(HyperlinkEvent e) {
													UiUtils.displaySugarItemById13(opportunity.getSugarType(), opportunity.getId(), opportunity.getId(), new WebserviceDataLoadProgressMonitor(
															OpportunitiesComposite.this, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOADING, new String[]{opportunity.getName()})));
												}
											});

											TableEditor editor = new TableEditor(table);
											editor.grabHorizontal = true;
											editor.setEditor(linkComposite, item, 0);

											// Create the amount label
											createCellLabelEditor(table, item, opportunity.getTotalRevenue(), 1);
											createCellLabelEditor(table, item, opportunity.getDecisionDate(), 2);
											createCellLabelEditor(table, item, opportunity.getSalesStage(), 3);
											createCellLabelEditor(table, item, opportunity.getDescription(), 4);

											ctr++;

										}

										innerComposite.layout(true);
										UiUtils.recursiveSetBackgroundColor(scrolledComposite, JFaceColors.getBannerBackground(Display.getDefault()));

										// Hack to set the width of the description column
										ControlListener controlListener = new ControlAdapter() {
											public void controlResized(ControlEvent e) {

												TableColumn descriptionColumn = table.getColumn(4);
												int availableSpace = table.getSize().x
														- (table.getColumn(0).getWidth() + table.getColumn(1).getWidth() + table.getColumn(2).getWidth() + table.getColumn(3).getWidth() + 5);
												descriptionColumn.setWidth(Math.max(DESCRIPTION_COL_WIDTH, availableSpace));

												table.layout(true);
												table.update();
												table.redraw();
											}
										};
										table.addControlListener(controlListener);

										packTable(table, new int[]{3, 4});
										innerComposite.layout(true);
										UiUtils.recursiveSetBackgroundColor(scrolledComposite, JFaceColors.getBannerBackground(Display.getDefault()));

									} else if (nothingToShowLabel == null || nothingToShowLabel.isDisposed()) {
										nothingToShowLabel = new Label(innerComposite, SWT.WRAP);
										nothingToShowLabel.setLayoutData(GridDataFactory.fillDefaults().indent(10, 0).grab(true, true).create());
										nothingToShowLabel.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
										nothingToShowLabel.setText(getSugarType().equals(SugarType.ACCOUNTS) ? UtilsPlugin.getDefault().getResourceString(
												UtilsPluginNLSKeys.OPPORTUNITIES_NO_OPPORTUNITIES_FOR_CLIENT) : UtilsPlugin.getDefault().getResourceString(
												UtilsPluginNLSKeys.OPPORTUNITIES_NO_OPPORTUNITIES_FOR_CONTACT));

										innerComposite.layout(true);
										UiUtils.recursiveSetBackgroundColor(scrolledComposite, JFaceColors.getBannerBackground(Display.getDefault()));
									}
								}
							} catch (java.lang.IllegalArgumentException e) {
								if (OpportunitiesComposite.this.isDisposed()) {
								} else {
									UtilsPlugin.getDefault().logException(e, OpportunitiesActivator.PLUGIN_ID);
								}
							}
						}

					});
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}
	}

	/**
	 * Builds the link to show all of the users opptys in sugar
	 */
	private void buildOpptyLink() {
		if (opptyLink == null || opptyLink.isDisposed()) {
			// Yeah, this is a bit clunky. But wasn't going to create an interface just for this...
			if (getSugarEntry() instanceof SugarContact || getSugarEntry() instanceof SugarAccount) {
				int totalOpptys;
				if (getSugarEntry() instanceof SugarContact) {
					totalOpptys = ((SugarContact) getSugarEntry()).getTotalOpportunities();
				} else {
					totalOpptys = ((SugarAccount) getSugarEntry()).getTotalOpportunities();
				}

				if (totalOpptys > displayTotal) {
					opptyLink = new SFAHyperlink(innerComposite, SWT.NONE);
					opptyLink.setLayoutData(GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).create());
					opptyLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
					opptyLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
					opptyLink.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SHOW_ALL_OPPORTUNITIES,
							new String[]{totalOpptys + "", ((getSugarEntry() == null || getSugarEntry().getName() == null) ? ConstantStrings.EMPTY_STRING : getSugarEntry().getName())})); //$NON-NLS-1$
					if (getSugarEntry() != null && getSugarEntry().getName() != null && !getSugarEntry().getName().equals(ConstantStrings.EMPTY_STRING)) {
						opptyLink.addHyperlinkListener(new HyperlinkAdapter() {
							@Override
							public void linkActivated(HyperlinkEvent e) {
								GenericUtils.launchUrlInPreferredBrowser(getSugarEntry().getSugarUrl() + "#opportunities", true); //$NON-NLS-1$
							}
						});
					}
				}
			}
		}
	}
	private void createCellLabelEditor(Table table, TableItem item, String labelText, int column) {
		Composite composite = new Composite(table, SWT.NONE);
		composite.setLayout(GridLayoutFactory.fillDefaults().margins(5, 5).create());

		if (labelText == null) {
			labelText = ConstantStrings.EMPTY_STRING;
		}

		Label label = new Label(composite, SWT.WRAP);

		label.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		label.setText(labelText);

		label.setToolTipText(labelText);
		label.setFont(SugarItemsDashboard.getInstance().getTableItemFont());
		label.setForeground(SugarItemsDashboard.getInstance().getBusinessCardFieldTextColor());

		TableEditor editor = new TableEditor(table);
		editor.grabHorizontal = true;
		editor.setEditor(composite, item, column);

	}

	@Override
	public void selectedItemsChanged() {
	}

	/**
	 * Loads the contact data from the webservice for this account
	 */
	private void loadData() {
		final JSONArray[] itemArray = new JSONArray[1];
		dataLoaded = false;
		Job job = new Job("Retrieving Opportunities of " + getSugarEntry().getName()) //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {

				try {
					// get the Line Item information

					boolean isOK = SugarWebservicesOperations.getInstance().callSugarGetInfo13(getSugarEntry().getSugarType(), getSugarEntry().getId(),
							SugarWebservicesOperations.GetInfo13RestulType.OPPTIES);
					if (isOK) {
						dataLoaded = true;
					}
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, OpportunitiesActivator.PLUGIN_ID);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	@Override
	public void afterBaseCardDataRetrieved() {
		super.afterBaseCardDataRetrieved();
		loadData();
		buildTable();
		scrolledComposite.setContent(innerComposite);
		innerComposite.layout(true);
	}
}
