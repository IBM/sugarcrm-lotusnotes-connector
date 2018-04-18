package com.ibm.socialcrm.notesintegration.accountPanels.dashboardComposites;

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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
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
import com.ibm.socialcrm.notesintegration.accountPanels.data.AccountContact;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.ISametimeWidgetBuilder;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.SametimeWidgetContribution;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.SametimeWidgetContributionExtensionProcessor;
import com.ibm.socialcrm.notesintegration.core.utils.SugarDashboardPreference;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
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

public class AccountContactsDashboardComposite extends AbstractDashboardComposite {
	private EasyScrolledComposite scrolledComposite;
	private Composite innerComposite;
	private Table table;
	private Label loadingLabel;
	private Label nothingToShowLabel;
	private SFAHyperlink activitiesLink;

	// I didn't use a TableViewer (which would give us sorting for free) because we needed custom widgets
	// in the TableViewer.
	private boolean sortAscending = true; // Sort order for the names on the card
	private List<AccountContact> contactList = new ArrayList<AccountContact>();
	private boolean dataLoaded = false;

	private static final int NAME_COL_WIDTH = 200;
	private static final int EMAIL_COL_WIDTH = 200;
	private static final int PHONE_COL_WIDTH = 200;

	private static final int MAX_CONTACTS = 50;

	int totalContacts = 0;

	public AccountContactsDashboardComposite(Composite parent, int style, String dashboardID, BaseSugarEntry sugarEntry) {
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

					//For some reason, the standard mac hac doesn't work on this page UNLESS it is run on
					//some delay.  So, we put this in a job.
					Job job = new Job("Mac refresh hack") { //$NON-NLS-1$
						@Override
						protected IStatus run(IProgressMonitor arg0) {
							Display.getDefault().asyncExec(new Runnable() {
								@Override
								public void run() {
									Point p = getShell().getSize();
									getShell().setSize(p.x, p.y + 1);
								}
							});
							return Status.OK_STATUS;
						}
					};
					job.schedule(300);
				}
			}
		});
	}

	/**
	 * Loads the contact data from the webservice for this account
	 */
	private void loadData() {
		dataLoaded = false;
		Job job = new Job("Loading contact data for " + getSugarEntry().getName()) //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				String accountData = SugarWebservicesOperations.getInstance().getAccountWithContactData(getSugarEntry().getId());
				try {
					JSONObject responseJSON = new JSONObject(accountData);
					totalContacts = responseJSON.getInt("totalCount"); //$NON-NLS-1$					
					JSONArray contactArray = responseJSON.getJSONArray("result"); //$NON-NLS-1$
					contactList.clear();
					for (int i = 0; i < contactArray.length(); i++) {
						JSONObject contactObj = contactArray.getJSONObject(i);

						String sugarId = decodeJSONValue(contactObj.getString("id")); //$NON-NLS-1$
						String firstName = decodeJSONValue(contactObj.getString("first_name"));//$NON-NLS-1$
						String lastName = decodeJSONValue(contactObj.getString("last_name"));//$NON-NLS-1$
						String email = decodeJSONValue(contactObj.getString("email1"));//$NON-NLS-1$
						// 60613
						//String officePhone = decodeJSONValue(contactObj.getString("phone_work"));//$NON-NLS-1$
						//String mobilePhone = decodeJSONValue(contactObj.getString("phone_mobile"));//$NON-NLS-1$
						String officePhone = ConstantStrings.EMPTY_STRING;
						if ( !contactObj.isNull("phone_work")) { //$NON-NLS-1$
							officePhone = decodeJSONValue(contactObj.getString("phone_work"));//$NON-NLS-1$
						}
						String mobilePhone = ConstantStrings.EMPTY_STRING;
						if ( !contactObj.isNull("phone_mobile")) { //$NON-NLS-1$
							mobilePhone = decodeJSONValue(contactObj.getString("phone_mobile"));//$NON-NLS-1$
						}
						
						int emailSuppressed = contactObj.getInt("email_opt_out");//$NON-NLS-1$

						AccountContact contact = new AccountContact();
						contact.setSugarId(sugarId);
						contact.setFirstName(firstName);
						contact.setLastName(lastName);
						contact.setEmail(email);
						contact.setOfficePhone(officePhone);
						contact.setMobilePhone(mobilePhone);
						contact.setEmailSuppressed(emailSuppressed == 1);

						contactList.add(contact);
					}
					dataLoaded = true;
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, AccountPanelsPluginActivator.PLUGIN_ID);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	@Override
	public String getDashboardName() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CONTACTS);
	}

	@Override
	public void selectedItemsChanged() {
	}

	/**
	 * Builds the contact table. This method asynchronously loads the contact data and then passes the data to another helper method to build the table.
	 */
	private void buildTable() {
		if (table == null || table.isDisposed()) {
			eraseControl(nothingToShowLabel);

			loadingLabel = new Label(innerComposite, SWT.NONE);
			loadingLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOADING_CONTACT_INFORMATION));
			loadingLabel.setLayoutData(GridDataFactory.fillDefaults().create());
			UiUtils.recursiveSetBackgroundColor(scrolledComposite, JFaceColors.getBannerBackground(Display.getDefault()));

			Job job = new Job("Waiting to build contacts table for " + getSugarEntry().getName()) //$NON-NLS-1$
			{
				@Override
				protected IStatus run(IProgressMonitor arg0) {
					while (!dataLoaded) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							UtilsPlugin.getDefault().logException(e, AccountPanelsPluginActivator.PLUGIN_ID);
						}
					}

					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							try {
								if (contactList != null && !contactList.isEmpty()) {

									// If user presses Refresh BEFORE the initial loading is completed, we need to remove the table first;
									// otherwise you will see 2 tables in UI.
									if (table != null && !table.isDisposed()) {
										eraseControl(table);
										table = null;
									}

									doBuildTable();

									packTable(table, new int[]{2});
									innerComposite.layout(true);
									UiUtils.recursiveSetBackgroundColor(scrolledComposite, JFaceColors.getBannerBackground(Display.getDefault()));

								} else {

									eraseControl(loadingLabel);

									if (nothingToShowLabel == null || nothingToShowLabel.isDisposed()) {
										nothingToShowLabel = new Label(innerComposite, SWT.WRAP);
									} else {
										nothingToShowLabel.setVisible(true);
									}

									nothingToShowLabel.setLayoutData(GridDataFactory.fillDefaults().indent(10, 0).grab(true, true).create());
									nothingToShowLabel.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
									nothingToShowLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.NO_CONTACTS_FOR_CLIENT));
									nothingToShowLabel.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

									innerComposite.layout(true);

								}
							} catch (IllegalArgumentException ie) {
								if (!AccountContactsDashboardComposite.this.isDisposed()) {
									UtilsPlugin.getDefault().logException(ie, AccountPanelsPluginActivator.PLUGIN_ID);
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

	/**
	 * Helper method to actually populate the table with the contact data
	 * 
	 */
	private void doBuildTable() {
		table = new Table(innerComposite, SWT.BORDER | SWT.HIDE_SELECTION);
		table.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		table.setHeaderVisible(true);
		table.setFont(SugarItemsDashboard.getInstance().getTableHeaderFont());
		table.setForeground(SugarItemsDashboard.getInstance().getBusinessCardHeaderTextColor());
		table.setLinesVisible(true);

		if (totalContacts > MAX_CONTACTS) {
			activitiesLink = new SFAHyperlink(innerComposite, SWT.NONE);
			activitiesLink.setLayoutData(GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).create());
			activitiesLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
			activitiesLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
			activitiesLink.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));
			activitiesLink.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ACCOUNTS_SHOW_ALL_CONTACTS, new String[]{String.valueOf(totalContacts)}));
			activitiesLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					GenericUtils.launchUrlInPreferredBrowser(getSugarEntry().getSugarUrl() + "#contacts", true); //$NON-NLS-1$
				}
			});
		}

		// Hack to set the row height
		final Listener measureItemListener = new Listener() {
			public void handleEvent(Event event) {
				Font f = SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData();
				int height = f.getFontData()[0].getHeight();
				// Convert the font size in points to actual pixels
				height = (height * Display.getCurrent().getDPI().y) / 72;
				// The multiplier of 4 allows for 2 phone numbers with appropriate spacing.
				event.height = height * 4;
			}
		};
		table.addListener(SWT.MeasureItem, measureItemListener);

		TableColumn column = new TableColumn(table, SWT.LEFT | SWT.BEGINNING, 0);
		column.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.NAME));
		column.setWidth(NAME_COL_WIDTH);
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				sortAscending = !sortAscending; // Toggle the sort order
				table.dispose();
				doBuildTable();
			}
		});

		column = new TableColumn(table, SWT.LEFT | SWT.BEGINNING, 1);
		column.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.EMAIL));
		column.setWidth(EMAIL_COL_WIDTH);

		column = new TableColumn(table, SWT.LEFT | SWT.BEGINNING, 2);
		column.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.PHONE));
		int availableSpace = innerComposite.getBounds().width - NAME_COL_WIDTH - EMAIL_COL_WIDTH;
		// This is a bit hackish, but gets us a decent initial setting for the final column (in most cases).
		column.setWidth(Math.max(PHONE_COL_WIDTH, availableSpace - (HEADER_MARGIN * 2) - 5));

		populateTable();

		if (loadingLabel != null && !loadingLabel.isDisposed()) {
			loadingLabel.setVisible(false);
			((GridData) loadingLabel.getLayoutData()).exclude = true;
		}
		innerComposite.layout(true);
		UiUtils.recursiveSetBackgroundColor(scrolledComposite, JFaceColors.getBannerBackground(Display.getDefault()));

		// Hack to set the width of the description column
		final ControlListener controlListener = new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				TableColumn phoneColumn = table.getColumn(2);
				int availableSpace = table.getSize().x - (table.getColumn(0).getWidth() + table.getColumn(1).getWidth() + 5);
				phoneColumn.setWidth(Math.max(PHONE_COL_WIDTH, availableSpace));
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
	}

	/**
	 * Helper method to populate the table
	 */
	private void populateTable() {
		table.setRedraw(false);
		Collections.sort(contactList, new Comparator<AccountContact>() {
			@Override
			public int compare(AccountContact contact1, AccountContact contact2) {
				int compare = 0;
				if (sortAscending) {
					compare = contact1.getLastName().compareTo(contact2.getLastName());
				} else {
					compare = contact2.getLastName().compareTo(contact1.getLastName());
				}
				return compare;
			}
		});

		// In order for the TableEditors to be rendered properly on the first paint attempt, we
		// have to add the table items separately from the editors (hence, two loops)
		for (int i = 0; i < contactList.size(); i++) {
			new TableItem(table, i);
		}

		int ctr = 0;
		for (AccountContact contact : contactList) {
			TableItem item = table.getItem(ctr);
			createNameHyperlinkCellEditor(table, item, SugarDashboardPreference.getInstance().getFormattedNameWithoutSalutation(contact.getFirstName(), contact.getLastName()), contact.getSugarId(),
					SugarDashboardPreference.getInstance().getFormattedNameWithoutSalutation(contact.getFirstName(), contact.getLastName()), 0);
			createEmailHyperlinkCellEditor(table, item, contact.getEmail(), 1, contact.isEmailSuppressed());
			createPhoneHyperlinkCellEditor(table, item, contact.getOfficePhone(), contact.getMobilePhone(), 2);
			ctr++;
		}

		table.getColumn(2).pack();

		table.setRedraw(true);
		table.redraw();
	}

	/**
	 * Helper method to decode the data we get back from the SFARest web service. Turns &#XXX; into the appropriate character
	 * 
	 * @param jsonValue
	 * @return
	 */
	private String decodeJSONValue(String jsonValue) {
		String newValue = jsonValue;
		Pattern pattern = Pattern.compile("&#(.*);"); //$NON-NLS-1$
		Matcher m = pattern.matcher(jsonValue);
		while (m.find()) {
			String characterEntity = m.group();
			char newChar = new Character((char) Integer.parseInt(m.group(1)));
			newValue = newValue.replaceAll(characterEntity, newChar + ""); //$NON-NLS-1$
		}
		return newValue;
	}

	private void createNameHyperlinkCellEditor(Table table, TableItem item, final String name, final String sugarId, final String sugarName, int column) {
		final SFAHyperlink hyperLink = createHyperlinkCellEditor(table, item, name, column, false);
		final HyperlinkAdapter hyperlinkListener = new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				UiUtils.displaySugarItemById13(SugarType.CONTACTS, sugarId, sugarName, new WebserviceDataLoadProgressMonitor(AccountContactsDashboardComposite.this, UtilsPlugin.getDefault()
						.getResourceString(UtilsPluginNLSKeys.LOADING, new String[]{name})));
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

	/**
	 * Create an email hyperlink cell editor
	 * 
	 * @param table
	 * @param item
	 * @param email
	 * @param column
	 */
	private void createEmailHyperlinkCellEditor(Table table, TableItem item, final String email, int column, final boolean emailSuppressed) {
		final SFAHyperlink hyperLink = createHyperlinkCellEditor(table, item, email, column, emailSuppressed);
		final HyperlinkAdapter hyperLinkListener = new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				UiUtils.createEmail(email);
			}
		};

		hyperLink.addHyperlinkListener(hyperLinkListener);
		hyperLink.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				if (hyperLinkListener != null) {
					hyperLink.removeHyperlinkListener(hyperLinkListener);
				}
			}
		});

	}

	/**
	 * Create a cell editor for the phone fields
	 * 
	 * @param table
	 * @param item
	 * @param officePhone
	 * @param mobilePhone
	 * @param column
	 */

	private void createPhoneHyperlinkCellEditor(Table table, TableItem item, final String officePhone, final String mobilePhone, int column) {
		SametimeWidgetContribution sametimeWidgetContribution = SametimeWidgetContributionExtensionProcessor.getInstance().getSametimeWidgetContribution();
		if (sametimeWidgetContribution != null) {
			try {
				Class builderClass = sametimeWidgetContribution.getBundle().loadClass(sametimeWidgetContribution.getBuilderClass());
				Constructor constructor = builderClass.getConstructor();

				if ((officePhone != null && officePhone.trim().length() > 0) || (mobilePhone != null && mobilePhone.trim().length() > 0)) {
					Composite composite = new Composite(table, SWT.NONE);
					composite.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).create());
					composite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).grab(true, true).create());
					composite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

					if (officePhone != null && officePhone.trim().length() > 0) {
						Composite composite1 = new Composite(composite, SWT.NONE);
						composite1.setLayout(GridLayoutFactory.fillDefaults().margins(1, 1).create());
						// need layoutdata to display phone in the center of the cell if only 1 phone is available
						composite1.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).grab(true, true).create());
						ISametimeWidgetBuilder sametimeWidgetBuilder = (ISametimeWidgetBuilder) constructor.newInstance();
						sametimeWidgetBuilder.createClickToCallComposite(composite1, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OFFICE_PHONE_WITH_LABEL, new String[]{officePhone}),
								null);
					}
					if (mobilePhone != null && mobilePhone.trim().length() > 0) {
						Composite composite2 = new Composite(composite, SWT.NONE);
						composite2.setLayout(GridLayoutFactory.fillDefaults().margins(1, 1).create());
						composite2.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).grab(true, true).create());
						ISametimeWidgetBuilder sametimeWidgetBuilder = (ISametimeWidgetBuilder) constructor.newInstance();
						sametimeWidgetBuilder.createClickToCallComposite(composite2, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.MOBILE_PHONE_WITH_LABEL, new String[]{mobilePhone}),
								null);

					}
					TableEditor editor = new TableEditor(table);
					editor.grabHorizontal = true;
					editor.setEditor(composite, item, column);
				}

			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
			}

		} else
		// If Notes embedded sametime was not installed
		{
			createCellLabelEditor(table, item, officePhone, mobilePhone, column);
		}

	}

	private void createCellLabelEditor(Table table, TableItem item, final String officePhone, final String mobilePhone, int column) {

		Composite composite = new Composite(table, SWT.NONE);
		composite.setLayout(GridLayoutFactory.fillDefaults().margins(5, 5).create());
		composite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

		if (officePhone != null && officePhone.trim().length() > 0) {
			Label ophone = new Label(composite, SWT.NONE);
			ophone.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).grab(true, true).create());
			ophone.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OFFICE_PHONE_WITH_LABEL, new String[]{officePhone}));
		}
		if (mobilePhone != null && mobilePhone.trim().length() > 0) {
			Label mphone = new Label(composite, SWT.NONE);
			mphone.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).grab(true, true).create());
			mphone.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.MOBILE_PHONE_WITH_LABEL, new String[]{mobilePhone}));
		}

		TableEditor editor1 = new TableEditor(table);
		editor1.grabHorizontal = true;
		editor1.setEditor(composite, item, column);

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
	private SFAHyperlink createHyperlinkCellEditor(Table table, TableItem item, String label, int column, boolean strikethrough) {
		Composite composite = new Composite(table, SWT.NONE);
		composite.setLayout(GridLayoutFactory.fillDefaults().margins(5, 5).create());
		composite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));
		composite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).create());

		if (label == null) {
			label = ConstantStrings.EMPTY_STRING;
		}
		SFAHyperlink hyperLink = new SFAHyperlink(composite, SWT.NONE, true);
		hyperLink.setStrikethrough(strikethrough);
		hyperLink.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).grab(true, true).create());
		hyperLink.setText(label);
		hyperLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
		hyperLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());

		TableEditor editor = new TableEditor(table);
		editor.grabHorizontal = true;
		editor.setEditor(composite, item, column);
		return hyperLink;
	}

	private void eraseControl(Control c) {
		if (c != null && !c.isDisposed()) {
			c.setVisible(false);
			((GridData) c.getLayoutData()).exclude = true;
		}
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
