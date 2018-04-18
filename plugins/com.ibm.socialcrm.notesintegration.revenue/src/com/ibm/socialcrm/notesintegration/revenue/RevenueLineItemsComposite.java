package com.ibm.socialcrm.notesintegration.revenue;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.lang.reflect.Constructor;

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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.RevenueLineItem;
import com.ibm.socialcrm.notesintegration.core.SugarOpportunity;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.ISametimeWidgetBuilder;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.SametimeWidgetContribution;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.SametimeWidgetContributionExtensionProcessor;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.ui.dashboardcomposites.AbstractDashboardComposite;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.widgets.EasyScrolledComposite;

public class RevenueLineItemsComposite extends AbstractDashboardComposite {
	private EasyScrolledComposite scrolledComposite;
	private Composite innerComposite;
	private Table table;

	private static final int NAME_COL_WIDTH = 150;
	private static final int AMOUNT_COL_WIDTH = 150;
	private static final int DATE_COL_WIDTH = 150;
	private static final int CONTACT_COL_WIDTH = 200;

	private boolean dataLoaded = false;

	public RevenueLineItemsComposite(Composite parent, int style, String dashboardID, BaseSugarEntry sugarEntry) {
		super(parent, style, dashboardID, sugarEntry);
	}

	@Override
	public void createInnerComposite() {
		scrolledComposite = new EasyScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).create());
		scrolledComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		innerComposite = new Composite(scrolledComposite, SWT.NONE);
		innerComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).margins(HEADER_MARGIN, HEADER_MARGIN).create());
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
					getShell().setSize(p.x, p.y - 1);
				}
			}
		});
	}

	@Override
	public String getDashboardName() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_REVENUES_TAB);
	}

	/**
	 * Builds the table that contains the line item data
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
							UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
						}
					}

					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							try {
								SugarOpportunity opportunity = (SugarOpportunity) getSugarEntry();
								if (table == null || table.isDisposed()) {
									// Note, this doesn't use a table viewer. To get things to line up vertically with non-default sizing, we had
									// to create custom editors for each of the cells. Since the info on here is static, there's really no advantage
									// to using a TableViewer.
									table = new Table(innerComposite, SWT.BORDER | SWT.HIDE_SELECTION);
									table.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
									table.setHeaderVisible(true);
									table.setFont(SugarItemsDashboard.getInstance().getTableHeaderFont());
									table.setLinesVisible(true);
									table.setForeground(SugarItemsDashboard.getInstance().getBusinessCardFieldTextColor());

									TableColumn column = new TableColumn(table, SWT.LEFT | SWT.BEGINNING, 0);
									column.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.NAME));
									column.setResizable(true);
									column.setWidth(NAME_COL_WIDTH);

									column = new TableColumn(table, SWT.LEFT | SWT.BEGINNING, 1);
									column.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.AMOUNT));
									column.setResizable(true);
									column.setWidth(AMOUNT_COL_WIDTH);

									column = new TableColumn(table, SWT.LEFT | SWT.BEGINNING, 2);
									column.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DATE));
									column.setResizable(true);
									column.setWidth(DATE_COL_WIDTH);

									column = new TableColumn(table, SWT.LEFT | SWT.BEGINNING, 3);
									column.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OWNER));
									column.setResizable(true);
									column.setWidth(CONTACT_COL_WIDTH);

									// Let the name field grow take up excess space
									ControlListener controlListener = new ControlAdapter() {
										public void controlResized(ControlEvent e) {
											if (RevenueLineItemsComposite.this.table != null) {
												TableColumn nameColumn = table.getColumn(0);
												int availableSpace = table.getSize().x - (table.getColumn(1).getWidth() + table.getColumn(2).getWidth() + CONTACT_COL_WIDTH + 5);
												nameColumn.setWidth(Math.max(availableSpace, NAME_COL_WIDTH));
												table.getColumn(3).setWidth(CONTACT_COL_WIDTH);

												table.layout(true);
												table.update();
												table.redraw();
											}
										}
									};
									table.addControlListener(controlListener);

									// Hack to set the row height
									table.addListener(SWT.MeasureItem, new Listener() {
										public void handleEvent(Event event) {
											event.height = 45;
										}
									});

									int ctr = 0;
									for (RevenueLineItem item : opportunity.getRevenueLineItems()) {
										buildItem(item, table, ctr);
										ctr++;
									}
									packTable(table, new int[]{3});

									// the update() might cause text to be half displayed
									// innerComposite.update();
									innerComposite.layout(true, true);
									UiUtils.recursiveSetBackgroundColor(scrolledComposite, JFaceColors.getBannerBackground(Display.getDefault()));
								}
							} catch (java.lang.IllegalArgumentException e) {
								if (!RevenueLineItemsComposite.this.isDisposed()) {
									UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
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
	private void buildItem(RevenueLineItem revenueItem, Table table, int ctr) {
		TableItem tableItem = new TableItem(table, ctr);
		createCellLabelEditor(table, tableItem, revenueItem.getName(), 0);
		createCellLabelEditor(table, tableItem, revenueItem.getAmount(), 1);
		createCellLabelEditor(table, tableItem, revenueItem.getBillDate(), 2);

		SametimeWidgetContribution sametimeWidgetContribution = SametimeWidgetContributionExtensionProcessor.getInstance().getSametimeWidgetContribution();
		if (sametimeWidgetContribution != null) {
			try {
				Composite c = new Composite(table, SWT.NONE);
				c.setLayout(GridLayoutFactory.fillDefaults().margins(5, 5).create());

				Class builderClass = sametimeWidgetContribution.getBundle().loadClass(sametimeWidgetContribution.getBuilderClass());
				Constructor constructor = builderClass.getConstructor();
				ISametimeWidgetBuilder sametimeWidgetBuilder = (ISametimeWidgetBuilder) constructor.newInstance();
				sametimeWidgetBuilder.createSametimeLinkComposite(c, revenueItem.getOwner().getName(), revenueItem.getOwner().getEmail());

				TableEditor editor = new TableEditor(table);
				editor.grabHorizontal = true;
				editor.setEditor(c, tableItem, 3);
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
			}
		} else
		// If Notes embedded sametime was not installed
		{
			createCellLabelEditor(table, tableItem, revenueItem.getOwner().getName(), 3);
		}
	}

	private void createCellLabelEditor(Table table, TableItem item, String labelText, int column) {
		Composite composite = new Composite(table, SWT.NONE);
		composite.setLayout(GridLayoutFactory.fillDefaults().margins(5, 5).create());

		Label label = new Label(composite, SWT.WRAP);
		if (labelText != null) {
			label.setText(labelText);
			label.setToolTipText(labelText);
		}
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
		Job job = new Job("Retrieving Lineitems of " + getSugarEntry().getName()) //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {

				try {
					// get the Line Item information
					boolean isOK = SugarWebservicesOperations.getInstance().callSugarGetInfo13(getSugarEntry().getSugarType(), getSugarEntry().getId(),
							SugarWebservicesOperations.GetInfo13RestulType.RLIS);
					if (isOK) {
						dataLoaded = true;
					}
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
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
