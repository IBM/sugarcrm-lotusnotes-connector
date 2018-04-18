package com.ibm.socialcrm.notesintegration.accountPanels.dashboardComposites;

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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONObject;
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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;

import com.ibm.socialcrm.notesintegration.accountPanels.AccountPanelsPluginActivator;
import com.ibm.socialcrm.notesintegration.accountPanels.data.ActivityData;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.ISametimeWidgetBuilder;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.SametimeWidgetContribution;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.SametimeWidgetContributionExtensionProcessor;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.ui.dashboardcomposites.AbstractDashboardComposite;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.widgets.EasyScrolledComposite;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFAHyperlink;

public class AccountActivitiesDashboardComposite extends AbstractDashboardComposite {

	private EasyScrolledComposite scrolledComposite;
	private Composite innerComposite;
	private Table table;
	private Label loadingLabel;;
	private Label nothingToShowLabel;

	private SFAHyperlink activitiesLink;
	private Set<ActivityData> activitiesList = null;

	private static final int MAX_ACTIVITIES_TO_BE_DISPLAYED = 5;
	private static final int SUBJECT_COL_WIDTH = 300;
	private static final int CREATED_COL_WIDTH = 150;
	private static final int ASSIGNED_COL_WIDTH = 150;

	private boolean dataLoaded = false;
	private int _totalActivities = 0;

	public AccountActivitiesDashboardComposite(Composite parent, int style, String dashboardID, BaseSugarEntry sugarEntry) {
		super(parent, style, dashboardID, sugarEntry);

		// don't need to call loadData() here, because abstract class' constructor calls
		// prepareForRebuild(). We will overload that method and call loadData().
		// loadData();
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
					getShell().setSize(p.x, p.y - 1);
				}
			}
		});

	}

	@Override
	public String getDashboardName() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ACTIVITIES_TAB);
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
		Job job = new Job("Loading activities data for " + getSugarEntry().getName()) //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				Set<ActivityData> tempActivitiesList = new TreeSet<ActivityData>();

				List<String> assignedIdList = new ArrayList<String>();

				try {
					String out = SugarWebservicesOperations.getInstance().getActivitiesData(getSugarEntry().getSugarType(), getSugarEntry().getId());

					if (out != null && !out.equals(ConstantStrings.EMPTY_STRING)) {
						JSONObject responseJSON = new JSONObject(out);

						int totalCompleted = 0;

						JSONObject resultJSON = responseJSON.getJSONObject("result"); //$NON-NLS-1$
						boolean hasResults = true;
						try {
							resultJSON.getJSONArray(getSugarEntry().getId());
							hasResults = false; // If we get here without blowing up, then it means the return value was not an object.
						} catch (Exception e) {
							// Eat it. This means that the results were ok.
						}

						if (hasResults) {
							JSONObject entryResultJSON = resultJSON.getJSONObject(getSugarEntry().getId());

							// 45240
							totalCompleted = getTotalCount(entryResultJSON);

							JSONArray activityArray = entryResultJSON.getJSONArray("activities"); //$NON-NLS-1$
							for (int i = 0; i < activityArray.length(); i++) {
								JSONObject activityJSON = activityArray.getJSONObject(i);
								String sugarId = ConstantStrings.EMPTY_STRING;
								if (activityJSON.containsKey("id")) //$NON-NLS-1$
								{
									sugarId = decodeJSONValue(activityJSON.getString("id"));//$NON-NLS-1$
								}

								String module_name = ConstantStrings.EMPTY_STRING;
								if (activityJSON.containsKey("module")) //$NON-NLS-1$
								{
									module_name = decodeJSONValue(activityJSON.getString("module"));//$NON-NLS-1$
								}

								String name = ConstantStrings.EMPTY_STRING;
								if (activityJSON.containsKey("name")) //$NON-NLS-1$ 
								{
									name = decodeJSONValue(activityJSON.getString("name"));//$NON-NLS-1$
								}

								String dateCreated = ConstantStrings.EMPTY_STRING;
								if (activityJSON.containsKey("date_entered")) //$NON-NLS-1$
								{
									dateCreated = decodeJSONValue(activityJSON.getString("date_entered"));//$NON-NLS-1$
								}

								String assignedUserName = ConstantStrings.EMPTY_STRING;
								if (activityJSON.containsKey("assigned_user_name")) //$NON-NLS-1$									
								{
									assignedUserName = decodeJSONValue(activityJSON.getString("assigned_user_name"));//$NON-NLS-1$
								}

								String assignedId = ConstantStrings.EMPTY_STRING;
								if (activityJSON.containsKey("assigned_user_id")) //$NON-NLS-1$									
								{
									assignedId = decodeJSONValue(activityJSON.getString("assigned_user_id"));//$NON-NLS-1$
								}

								ActivityData data = new ActivityData();
								data.setSugarId(sugarId);
								data.setModuleName(module_name);
								data.setSubject(name);
								data.setDateCreated(dateCreated);
								data.setAssignedUserName(assignedUserName);

								data.setAssignedId(assignedId);

								if (!assignedIdList.contains(assignedId)) {
									assignedIdList.add(assignedId);
								}

								tempActivitiesList.add(data);
								// 45240 totalCompleted++;
							}
						}
						_totalActivities = totalCompleted;
					}
					if (getActivitiesList() != null) {
						getActivitiesList().clear();
					}
					getActivitiesList().addAll(tempActivitiesList);
					dataLoaded = true;
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, AccountPanelsPluginActivator.PLUGIN_ID);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private int getTotalCount(JSONObject entryResultJSON) {
		int totalCompleted = 0;
		if (entryResultJSON != null) {
			try {
				String totalX = entryResultJSON.getString("totalActivities"); //$NON-NLS-1$
				if (totalX != null) {
					totalCompleted = Integer.valueOf(totalX).intValue();
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, AccountPanelsPluginActivator.PLUGIN_ID);
			}

		}
		return totalCompleted;
	}

	private boolean isCompletedActivity(JSONObject jsonObject) {
		boolean isCompleted = false;
		if (jsonObject != null) {
			try {
				if (!jsonObject.getJSONObject("field_values").containsKey("status")) //$NON-NLS-1$ //$NON-NLS-2$
				{
					isCompleted = true;
				} else {

					String status = jsonObject.getJSONObject("field_values").getJSONObject("status").getString("value"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
					if (status != null && (status.toLowerCase().equals("archived") || status.toLowerCase().equals("held"))) //$NON-NLS-1$ //$NON-NLS-2$
					{
						isCompleted = true;
					}
				}

			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, AccountPanelsPluginActivator.PLUGIN_ID);
			}
		}

		return isCompleted;
	}

	private Set<ActivityData> getActivitiesList() {
		if (activitiesList == null) {
			activitiesList = new TreeSet<ActivityData>();
		}
		return activitiesList;
	}

	/**
	 * Builds the Activities table. This method asynchronously loads the Activities data and then passes the data to another helper method to build the table.
	 */
	private void buildTable() {
		if (table == null || table.isDisposed()) {
			eraseControl(nothingToShowLabel);

			if (loadingLabel == null || loadingLabel.isDisposed()) {
				loadingLabel = new Label(innerComposite, SWT.NONE);
			} else {
				loadingLabel.setVisible(true);
			}
			loadingLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOADING_ACTIVITIES_INFORMATION));
			loadingLabel.setLayoutData(GridDataFactory.fillDefaults().create());
			UiUtils.recursiveSetBackgroundColor(scrolledComposite, JFaceColors.getBannerBackground(Display.getDefault()));

			Job job = new Job("Waiting to build Activites table for " + getSugarEntry().getName()) //$NON-NLS-1$
			{
				@Override
				protected IStatus run(IProgressMonitor arg0) {
					while (!AccountActivitiesDashboardComposite.this.isDisposed() && !dataLoaded) {
						try {
							Thread.sleep(1500 /* 1000 */);
						} catch (InterruptedException e) {
							UtilsPlugin.getDefault().logException(e, AccountPanelsPluginActivator.PLUGIN_ID);
						}
					}

					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							try {
								if (getActivitiesList() != null && !getActivitiesList().isEmpty()) {

									// If user presses Refresh BEFORE the initial loading is completed, we need to remove the table first;
									// otherwise you will see 2 tables in UI.
									if (table != null && !table.isDisposed()) {
										eraseControl(table);
										table = null;
									}

									doBuildTable();

									// pack the assigned column
									// There is some weird behavior with the table sizing. Because we have custom row heights, they don't seem to take effect if the table is built when it isn't
									// visible (that statement isn't entirely
									// true, it depends on how fast you click over to this tab). It seems pack the column along with
									// measureItemListener fixed this problem.
									// 
									packTable(table, new int[]{2});

									buildActivitiesLink();
								} else {
									eraseControl(loadingLabel);

									if (nothingToShowLabel == null || nothingToShowLabel.isDisposed()) {
										nothingToShowLabel = new Label(innerComposite, SWT.WRAP);
									} else {
										nothingToShowLabel.setVisible(true);
									}

									nothingToShowLabel.setLayoutData(GridDataFactory.fillDefaults().indent(10, 0).grab(true, true).create());
									nothingToShowLabel.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
									nothingToShowLabel.setText(getSugarType().equals(SugarType.ACCOUNTS)
											? UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.NO_ACTIVITIES_FOR_CLIENT)
											: getSugarType().equals(SugarType.CONTACTS) ? UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.NO_ACTIVITIES_FOR_CONTACT) : UtilsPlugin
													.getDefault().getResourceString(UtilsPluginNLSKeys.NO_ACTIVITIES_FOR_OPPORTUNITY));
									nothingToShowLabel.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

									innerComposite.layout(true);

								}
							} catch (java.lang.IllegalArgumentException e) {
								if (AccountActivitiesDashboardComposite.this.isDisposed()) {
								} else {
									UtilsPlugin.getDefault().logException(e, AccountPanelsPluginActivator.PLUGIN_ID);
								}
							} catch (Exception e) {
								UtilsPlugin.getDefault().logException(e, AccountPanelsPluginActivator.PLUGIN_ID);
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

	/**
	 * Helper method to actually populate the table with the contact data
	 * 
	 */
	private void doBuildTable() {
		try {
			table = new Table(innerComposite, SWT.BORDER | SWT.HIDE_SELECTION);
			table.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
			table.setHeaderVisible(true);
			table.setFont(SugarItemsDashboard.getInstance().getTableHeaderFont());
			table.setForeground(SugarItemsDashboard.getInstance().getBusinessCardHeaderTextColor());
			table.setLinesVisible(true);

			final Listener measureItemListener = new Listener() {
				public void handleEvent(Event event) {
					event.height = 55;
				}
			};
			table.addListener(SWT.MeasureItem, measureItemListener);

			TableColumn column = new TableColumn(table, SWT.LEFT | SWT.BEGINNING, 0);
			column.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ACTIVITIES_SUBJECT));
			column.setWidth(SUBJECT_COL_WIDTH);

			column = new TableColumn(table, SWT.LEFT | SWT.BEGINNING, 1);
			column.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ACTIVITIES_CREATED));
			column.setWidth(CREATED_COL_WIDTH);

			column = new TableColumn(table, SWT.LEFT | SWT.BEGINNING, 2);
			column.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ACTIVITIES_ASSIGNED));
			int availableSpace = innerComposite.getBounds().width - SUBJECT_COL_WIDTH - CREATED_COL_WIDTH;
			// This is a bit hackish, but gets us a decent initial setting for the final column (in most cases).
			column.setWidth(Math.max(ASSIGNED_COL_WIDTH, availableSpace - (HEADER_MARGIN * 2) - 5));

			populateTable();

			if (loadingLabel != null && !loadingLabel.isDisposed()) {
				loadingLabel.setVisible(false);
				((GridData) loadingLabel.getLayoutData()).exclude = true;
			}
			innerComposite.layout(true);
			UiUtils.recursiveSetBackgroundColor(scrolledComposite, JFaceColors.getBannerBackground(Display.getDefault()));

			// Hack to set the width of the Subject column - without this logic, you'll see an extra column when resizing
			// the table.
			final ControlListener controlListener = new ControlAdapter() {
				public void controlResized(ControlEvent e) {
					TableColumn assignedColumn = table.getColumn(2);
					int availableSpace = table.getSize().x - (table.getColumn(0).getWidth() + table.getColumn(1).getWidth() + 5);
					assignedColumn.setWidth(Math.max(ASSIGNED_COL_WIDTH, availableSpace));
					table.layout(true);
					table.update();
					table.redraw();
				}
			};
			table.addControlListener(controlListener);

			table.addDisposeListener(new DisposeListener() {

				@Override
				public void widgetDisposed(DisposeEvent arg0) {
					if (measureItemListener != null) {
						table.removeListener(SWT.MeasureItem, measureItemListener);
					}
					if (controlListener != null) {
						table.removeControlListener(controlListener);
					}

				}

			});
		} catch (java.lang.IllegalArgumentException e) {
			if (AccountActivitiesDashboardComposite.this.isDisposed()) {
			} else {
				UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
			}
		}

	}

	/**
	 * Helper method to populate the table
	 */
	private void populateTable() {
		table.setRedraw(false);
		int ctr = 0;
		for (ActivityData activity : getActivitiesList()) {
			// Hit the limit, stop sending activities to UI
			if (ctr == MAX_ACTIVITIES_TO_BE_DISPLAYED) {
				break;
			}
			TableItem item = new TableItem(table, ctr);
			createSubjectHyperlinkCellEditor(table, item, activity.getSubject(), activity.getSugarId(), 0);
			createCellLabelEditor(table, item, activity.getDateCreated(), 1);
			createAssignedCellEditor(table, item, activity.getAssignedUserName(), activity.getAssignedUserName(), 2);
			ctr++;
		}
		table.setRedraw(true);
		table.redraw();
	}

	private void createSubjectHyperlinkCellEditor(Table table, TableItem item, final String name, final String sugarId, int column) {
		final SFAHyperlink hyperLink = createHyperlinkCellEditor(table, item, name, column);
		final HyperlinkAdapter hyperlinkListener = new HyperlinkAdapter() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				String module_name = getModule(sugarId);
				if (module_name != null && sugarId != null) {
					String decoratedUrl;
					String s1 = NotesAccountManager.getInstance().getCRMServer() + "index.php";  //$NON-NLS-1$
					StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
					sb.append(s1).append("?module=").append(module_name).append("&action=DetailView&record=").append(sugarId); //$NON-NLS-1$ //$NON-NLS-2$
					if (GenericUtils.isUseEmbeddedBrowserPreferenceSet()) {
						decoratedUrl = sb.toString();
					} else {
						decoratedUrl = SugarWebservicesOperations.getInstance().buildV10SeamlessURL(sb.toString());
					}					
					GenericUtils.launchUrlInPreferredBrowser(decoratedUrl, true);
				}
			}
		};
		hyperLink.addHyperlinkListener(hyperlinkListener);

		hyperLink.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				if (hyperlinkListener != null) {
					hyperLink.removeHyperlinkListener(hyperlinkListener);
				}

			}

		});

	}

	private String getModule(String sugarId) {
		String module_name = null;
		Iterator<ActivityData> it = getActivitiesList().iterator();
		while (it.hasNext()) {
			ActivityData ad = it.next();
			if (ad.getSugarId().equals(sugarId)) {
				module_name = ad.getModuleName();
				break;
			}
		}
		return module_name;
	}

	/**
	 * Create a generic hyperlink cell editor
	 * 
	 * @param table
	 * @param item
	 * @param label
	 * @param column
	 * @return
	 */
	private SFAHyperlink createHyperlinkCellEditor(Table table, TableItem item, String label, int column) {
		Composite composite = new Composite(table, SWT.NONE);
		composite.setLayout(GridLayoutFactory.fillDefaults().margins(5, 5).create());
		composite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));
		composite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).create());

		if (label == null) {
			label = ConstantStrings.EMPTY_STRING;
		}
		SFAHyperlink hyperLink = new SFAHyperlink(composite, SWT.NONE, true);
		hyperLink.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).grab(true, true).create());
		hyperLink.setText(label);
		hyperLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
		hyperLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());

		TableEditor editor = new TableEditor(table);
		editor.grabHorizontal = true;
		editor.setEditor(composite, item, column);
		return hyperLink;
	}

	private void createAssignedCellEditor(Table table, TableItem item, String name, String email, int column) {
		SametimeWidgetContribution sametimeWidgetContribution = SametimeWidgetContributionExtensionProcessor.getInstance().getSametimeWidgetContribution();
		if (sametimeWidgetContribution != null) {
			try {
				Composite composite = new Composite(table, SWT.NONE);
				composite.setLayout(GridLayoutFactory.fillDefaults().margins(5, 15).create());

				Class builderClass = sametimeWidgetContribution.getBundle().loadClass(sametimeWidgetContribution.getBuilderClass());
				Constructor constructor = builderClass.getConstructor();
				ISametimeWidgetBuilder sametimeWidgetBuilder = (ISametimeWidgetBuilder) constructor.newInstance();
				sametimeWidgetBuilder.createSametimeLinkComposite(composite, name, email);

				TableEditor editor = new TableEditor(table);
				editor.grabHorizontal = true;
				editor.setEditor(composite, item, column);
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
			}
		} else
		// If Notes embedded sametime was not installed
		{
			createCellLabelEditor(table, item, name, 2);
		}
	}

	private void createCellLabelEditor(Table table, TableItem item, String labelText, int column) {
		Composite composite = new Composite(table, SWT.NONE);
		composite.setLayout(GridLayoutFactory.fillDefaults().margins(5, 5).create());

		Label label = new Label(composite, SWT.WRAP | SWT.CENTER);
		label.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).grab(true, true).create());
		// 60613
		label.setText( labelText==null? ConstantStrings.EMPTY_STRING : labelText);
		label.setToolTipText(labelText);
		label.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
		label.setForeground(SugarItemsDashboard.getInstance().getBusinessCardFieldTextColor());

		TableEditor editor = new TableEditor(table);
		editor.grabHorizontal = true;
		editor.setEditor(composite, item, column);
	}

	/**
	 * Helper method to decode the data we get back from the SFARest web service. Turns &#XXX; into the appropriate character
	 * 
	 * @param jsonValue
	 * @return
	 */
	private String decodeJSONValue(String jsonValue) {
		String newValue = jsonValue;
		Pattern pattern = Pattern.compile("&#([0-9]*);"); //$NON-NLS-1$
		Matcher m = pattern.matcher(jsonValue);
		while (m.find()) {
			try {
				String characterEntity = m.group();
				char newChar = new Character((char) Integer.parseInt(m.group(1)));
				newValue = newValue.replaceAll(characterEntity, newChar + ""); //$NON-NLS-1$
			} catch (Exception e) {
				// Just eat and log any exceptions out of here. Shouldn't be any, but just in case
				UtilsPlugin.getDefault().logException(e, AccountPanelsPluginActivator.PLUGIN_ID);
			}
		}
		newValue = newValue.replaceAll("&lt;", "<"); //$NON-NLS-1$ //$NON-NLS-2$
		newValue = newValue.replaceAll("&gt;", ">"); //$NON-NLS-1$ //$NON-NLS-2$
		newValue = newValue.replaceAll("&quot;", "\""); //$NON-NLS-1$ //$NON-NLS-2$
		return newValue;
	}

	/**
	 * Builds the link to show all of the users activities in sugar
	 */
	private void buildActivitiesLink() {
		if (activitiesLink == null || activitiesLink.isDisposed()) {
			// show the link only if there is more activities than the ones shown on the screen
			if (_totalActivities != Math.min(MAX_ACTIVITIES_TO_BE_DISPLAYED, getActivitiesList().size())) {
				// Yeah, this is a bit clunky. But wasn't going to create an interface just for this...

				activitiesLink = new SFAHyperlink(innerComposite, SWT.NONE);
				activitiesLink.setLayoutData(GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).create());
				activitiesLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
				activitiesLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
				activitiesLink.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));
				activitiesLink.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ACTIVITIES_SHOW_ALL_COMPLETED, new String[]{String.valueOf(_totalActivities)}));
				final HyperlinkAdapter activitiesHyperlinkListener = new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent e) {
				
						// 78043, trailing #opportunities prevents navigation to oppty in browser
						GenericUtils.launchUrlInPreferredBrowser(getSugarEntry().getSugarUrl(), true);
						//	GenericUtils.launchUrlInPreferredBrowser(getSugarEntry().getSugarUrl() + "#opportunities", true); //$NON-NLS-1$			
					}
				};

				activitiesLink.addHyperlinkListener(activitiesHyperlinkListener);

				activitiesLink.addDisposeListener(new DisposeListener() {

					@Override
					public void widgetDisposed(DisposeEvent arg0) {
						if (activitiesHyperlinkListener != null) {
							activitiesLink.removeHyperlinkListener(activitiesHyperlinkListener);
						}

					}

				});

				innerComposite.layout(true);
			}
		}

	}

	@Override
	public void afterBaseCardDataRetrieved() {
		super.afterBaseCardDataRetrieved();

		loadData();
		buildTable();

		innerComposite.redraw();
		innerComposite.update();
		innerComposite.layout(true, true);
	}
}
