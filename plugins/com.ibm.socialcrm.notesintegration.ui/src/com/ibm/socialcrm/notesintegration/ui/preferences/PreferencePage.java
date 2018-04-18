package com.ibm.socialcrm.notesintegration.ui.preferences;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.ibm.rcp.accounts.Account;
import com.ibm.rcp.accounts.AccountsException;
import com.ibm.rcp.accounts.AccountsManager;
import com.ibm.rcp.accounts.AccountsManagerFactory;
import com.ibm.rcp.content.ContentType;
import com.ibm.rcp.content.ContentTypeRegistry;
import com.ibm.rcp.managedsettings.ManagedSettingsScope;
import com.ibm.rcp.security.auth.AuthProperties;
import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.utils.AbderaConnectionsFileOperations;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.sugarwidgetupdate.SugarWidgetUpdater;
import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class PreferencePage extends org.eclipse.jface.preference.PreferencePage implements IWorkbenchPreferencePage {
	public static final int REFRESH_INTERVAL_MINIMUM = 60;
	private final String PROMPTED_FOR_CERT = "com.ibm.socialcrm.sugarwidget.promptedForCert"; //$NON-NLS-1$

	public static final int MIN_RECENTLY_VIEWED_CARDS = 20;
	public static final int MAX_RECENTLY_VIEWED_CARDS = 50;

	private static PreferencePage preferencePage = null;

	private Text sugarServerText;
	private Text sugarUserText;
	private Text sugarPasswordText;

	private Composite progressComposite;
	private Spinner regexRefreshIntervalSpinner;

	private Spinner recentlyViewedCardsSpinner;
	private int maxRecentlyViewedCards;

	private Button accountContentTypeCheckBox;
	private Button opptyContentTypeCheckBox;
	private Button contactContentTypeCheckBox;
	private String contentMatchAccountOrig;
	private String contentMatchOpptyOrig;
	private String contentMatchContactOrig;

	private Label connectionErrorImage;
	private Label connectionErrorLabel;

	private String sugarServer;
	private String sugarUser;
	private String sugarPassword;
	private int regexRefreshInterval;

	private String welcomeServer;
	private String welcomeUser;
	private String welcomePassword;

	private ModifyListener listener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent evt) {
			updateValues();
		}
	};

	public PreferencePage() {
		super(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CRM_SERVER_PREFERENCES));

		// save orig. content match preference values
		Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
		contentMatchAccountOrig = prefs.getString(SugarWebservicesOperations.SALESCONNECT_LIVETEXT_MATCH_ACCOUNT_PREF_KEY);
		contentMatchOpptyOrig = prefs.getString(SugarWebservicesOperations.SALESCONNECT_LIVETEXT_MATCH_OPPTY_PREF_KEY);
		contentMatchContactOrig = prefs.getString(SugarWebservicesOperations.SALESCONNECT_LIVETEXT_MATCH_CONTACT_PREF_KEY);

	}

	@Override
	protected Control createContents(Composite parent) {
		parent.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).create());
		createSugarPart(parent);
		createLiveTextRefreshIntervalPart(parent);
		createRecentlyViewedPart(parent);
		createCertificatePart(parent);
		createLiveTextMatchingContentTypesPart(parent);
		createConnectionErrorsPart(parent);
		createProgressComposite(parent);

		this.noDefaultAndApplyButton();

		updateValues();

		return parent;
	}

	private void createProgressComposite(Composite parent) {
		progressComposite = new Composite(parent, SWT.NONE);
		progressComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).create());
		progressComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.END).create());
		progressComposite.setVisible(false);
		((GridData) (progressComposite.getLayoutData())).exclude = true;
	}

	private void createLiveTextRefreshIntervalPart(final Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		group.setLayout(GridLayoutFactory.fillDefaults().margins(5, 5).create());
		group.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_LIVE_TEXT_REFRESH));

		Composite composite = new Composite(group, SWT.NONE);
		composite.setLayout(GridLayoutFactory.fillDefaults().numColumns(4).create());
		composite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		Label refreshLabel = new Label(composite, SWT.NONE);
		refreshLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_REFRESH_EVERY));

		regexRefreshIntervalSpinner = new Spinner(composite, SWT.BORDER);
		regexRefreshIntervalSpinner.setMinimum(REFRESH_INTERVAL_MINIMUM);
		regexRefreshIntervalSpinner.setMaximum(Integer.MAX_VALUE);
		regexRefreshIntervalSpinner.addModifyListener(listener);

		Label minutesLabel = new Label(composite, SWT.NONE);
		minutesLabel.setLayoutData(GridDataFactory.fillDefaults().indent(4, 4).create());
		minutesLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_MINUTES_LABEL));

		Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
		int regexPref = prefs.getInt(SugarWidgetUpdater.REFRESH_REGEXES_INTERVAL_PREF_KEY);
		regexRefreshIntervalSpinner.setSelection(regexPref == 0 ? SugarWidgetUpdater.REFRESH_INTERVAL_DEFAULT : regexPref);

		final Button refreshButton = new Button(composite, SWT.PUSH);
		refreshButton.setLayoutData(GridDataFactory.fillDefaults().indent(5, 0).create());
		refreshButton.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_REFRESH_REGEX_BUTTON));
		refreshButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				Job job = new Job(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_UPDATING_LIVE_TEXT_RECOGNIZERS)) {
					@Override
					protected IStatus run(IProgressMonitor arg0) {
						final Control[][] group = new Control[1][];
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								refreshButton.setEnabled(false);
								group[0] = createProgressGroup();
								((Label) group[0][0]).setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_UPDATING_LIVE_TEXT_RECOGNIZERS));
							}
						});

						// We need to temporarily update the Sugar account information since the user may have modified the
						// fields before hitting refresh and may not want to commit to the changes.
						String storedSugarServer = NotesAccountManager.getInstance().getCRMServer();
						String storedSugarUser = NotesAccountManager.getInstance().getCRMUser();
						String storedSugarPassword = NotesAccountManager.getInstance().getCRMPassword();
						updateCrmAccountInfo(sugarServer, sugarUser, sugarPassword);

						// no need to bring up the credential prompt dialog if invalid credential
						final boolean[] success = {new SugarWidgetUpdater().loadUserData(false, false)};

						// Reset the account information
						updateCrmAccountInfo(storedSugarServer, storedSugarUser, storedSugarPassword);

						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								removeProgressGroup(group[0]);
								refreshConnectionErrorFields(parent);
								if (!success[0]) {
									errorMsgDialog();

								}
								refreshButton.setEnabled(true);
							}
						});

						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}
		});

		final Composite errorComposite = new Composite(composite, SWT.NONE);
		errorComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
		errorComposite.setLayoutData(GridDataFactory.fillDefaults().span(4, 1).create());

		final Label errorLabel = new Label(errorComposite, SWT.NONE);
		errorLabel.setImage(SFAImageManager.getImage(SFAImageManager.ERROR_ICON));
		errorLabel.setLayoutData(GridDataFactory.fillDefaults().create());
		errorLabel.setVisible(false);

		final Label minIntervalLabel = new Label(errorComposite, SWT.WRAP);
		minIntervalLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		minIntervalLabel.setVisible(false);

		// Hide this by default
		((GridData) (errorComposite.getLayoutData())).exclude = true;
		parent.layout(true);

		regexRefreshIntervalSpinner.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				int value = Integer.parseInt(regexRefreshIntervalSpinner.getText());
				if (value < REFRESH_INTERVAL_MINIMUM) {
					errorLabel.setVisible(true);
					minIntervalLabel.setVisible(true);
					((GridData) (errorComposite.getLayoutData())).exclude = false;
					minIntervalLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_REFRESH_INTERVAL_TOO_SMALL,
							new String[]{REFRESH_INTERVAL_MINIMUM + ConstantStrings.EMPTY_STRING}));
					parent.layout(true);
				} else {
					errorLabel.setVisible(true);
					minIntervalLabel.setVisible(false);
					errorLabel.setVisible(false);
					((GridData) (errorComposite.getLayoutData())).exclude = true;
					parent.layout(true);
				}
			}
		});
	}

	/**
	 * Creates the fields to allow the user to select how many cards they want in the recently viewed cards
	 * 
	 * @param parent
	 */
	private void createRecentlyViewedPart(Composite parent) {
		Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);

		group.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_SIDEBAR_SETTINGS));
		group.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).create());
		group.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		Label maxNumberLabel = new Label(group, SWT.NONE);
		maxNumberLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_MAX_CARDS));

		recentlyViewedCardsSpinner = new Spinner(group, SWT.BORDER);
		recentlyViewedCardsSpinner.setMinimum(MIN_RECENTLY_VIEWED_CARDS);
		recentlyViewedCardsSpinner.setMaximum(MAX_RECENTLY_VIEWED_CARDS);

		Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
		int sidebarPref = prefs.getInt(SugarWidgetUpdater.MAX_SIDEBAR_CARDS_PREF_KEY);
		recentlyViewedCardsSpinner.setSelection(sidebarPref == 0 ? MIN_RECENTLY_VIEWED_CARDS : sidebarPref);

		recentlyViewedCardsSpinner.addModifyListener(listener);

		Button clearRecentListButton = new Button(group, SWT.PUSH);
		clearRecentListButton.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CLEAR_RECENT_CARDS));
		clearRecentListButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				boolean confirm = MessageDialog.openConfirm(getShell(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CLEAR_RECENT_CARDS), UtilsPlugin.getDefault()
						.getResourceString(UtilsPluginNLSKeys.UI_CLEAR_RECENT_CARDS_MESSAGE));
				if (confirm) {
					SugarItemsDashboard.getInstance().clearPreviouslyViewedCards();
				}
			}
		});
	}

	/**
	 * Create the widgets that let the user accept the SC certificate
	 * 
	 * @param parent
	 */
	private void createCertificatePart(Composite parent) {
		Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);

		group.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_SALESCONNECT_CERTIFICATE));
		group.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).margins(5, 5).create());
		group.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		Button acceptCertButton = new Button(group, SWT.PUSH);
		acceptCertButton.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ACCEPT_CERTIFICATE));
		acceptCertButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				// 74684 - add dummy query to force the browser not to cache this page
				// GenericUtils.launchUrlInPreferredBrowser("https://localhost:" + ConstantStrings.HTTP_PROXY_PORT + "/sfawebapi/acceptCert.html", false); //$NON-NLS-1$ //$NON-NLS-2$
				GenericUtils.launchUrlInPreferredBrowser("https://localhost:" + ConstantStrings.HTTP_PROXY_PORT + "/sfawebapi/acceptCert.html" + "?ver=1", false); //$NON-NLS-1$ //$NON-NLS-2$
				
				final Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
				prefs.setValue(PROMPTED_FOR_CERT, "true");
				CorePluginActivator.getDefault().savePluginPreferences();
			}
		});
	}

	private void createLiveTextMatchingContentTypesPart(final Composite parent) {

		Group group = new Group(parent, SWT.NONE);
		group.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		group.setLayout(GridLayoutFactory.fillDefaults().margins(5, 5).create());
		group.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_LIVETEXT_MATCH_CONTENTTYPES));

		Composite composite = new Composite(group, SWT.NONE);
		composite.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).create());
		composite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		accountContentTypeCheckBox = new Button(composite, SWT.CHECK);
		accountContentTypeCheckBox.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_LIVETEXT_MATCH_CONTENTTYPES_CLIENT));
		accountContentTypeCheckBox.setSelection((contentMatchAccountOrig == null || contentMatchAccountOrig.equals(ConstantStrings.EMPTY_STRING) || contentMatchAccountOrig.toLowerCase()
				.equals("true")) ? true : false); //$NON-NLS-1$

		contactContentTypeCheckBox = new Button(composite, SWT.CHECK);
		contactContentTypeCheckBox.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_LIVETEXT_MATCH_CONTENTTYPES_CONTACT));
		contactContentTypeCheckBox.setSelection((contentMatchContactOrig == null || contentMatchContactOrig.equals(ConstantStrings.EMPTY_STRING) || contentMatchContactOrig.toLowerCase()
				.equals("true")) ? true : false); //$NON-NLS-1$

		opptyContentTypeCheckBox = new Button(composite, SWT.CHECK);
		opptyContentTypeCheckBox.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_LIVETEXT_MATCH_CONTENTTYPES_OPPTY));
		opptyContentTypeCheckBox.setSelection((contentMatchOpptyOrig == null || contentMatchOpptyOrig.equals(ConstantStrings.EMPTY_STRING) || contentMatchOpptyOrig.toLowerCase().equals("true")) //$NON-NLS-1$
				? true
				: false);

		parent.layout(true);

	}
	/**
	 * Create a place to house any connection errors we want to display.
	 * 
	 * @param parent
	 */
	private void createConnectionErrorsPart(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
		composite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		connectionErrorImage = new Label(composite, SWT.NONE);
		connectionErrorImage.setImage(SFAImageManager.getImage(SFAImageManager.ERROR_ICON));

		connectionErrorLabel = new Label(composite, SWT.WRAP);
		connectionErrorLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		refreshConnectionErrorFields(parent);
	}

	private void refreshConnectionErrorFields(Composite parent) {
		if (!connectionErrorImage.isDisposed()) {
			boolean showError = SugarWebservicesOperations.getInstance().hasConnectionProblem();
			if (showError) {
				connectionErrorImage.setVisible(true);
				connectionErrorLabel.setVisible(true);
				if (SugarWebservicesOperations.getInstance().isServerIncorrect()) {
					connectionErrorLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.INVALID_SERVER));
				} else if (SugarWebservicesOperations.getInstance().unableToLogin()) {
					connectionErrorLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.INVALID_CREDENTIALS));
				} else if (SugarWebservicesOperations.getInstance().unableToConnect()) {
					connectionErrorLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.INVALID_CONNECTION));
				}

			} else {
				connectionErrorImage.setVisible(false);
				connectionErrorLabel.setVisible(false);
			}
		}
	}

	private void createSugarPart(Composite parent) {
		Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
		group.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CRM_SERVER_SETTINGS));
		group.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).create());
		group.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		Label serverLabel = new Label(group, SWT.NONE);
		serverLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CRM_SERVER));

		sugarServerText = new Text(group, SWT.BORDER);
		sugarServerText.setLayoutData(GridDataFactory.fillDefaults().hint(400, SWT.DEFAULT).grab(true, false).create());
		sugarServerText.setText(isFromWelcomePage() ? getWelcomeServer() : NotesAccountManager.getInstance().getCRMServer());
		sugarServerText.addModifyListener(listener);

		Label userLabel = new Label(group, SWT.NONE);
		userLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CRM_SERVER_USER));

		sugarUserText = new Text(group, SWT.BORDER);
		sugarUserText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		sugarUserText.setText(isFromWelcomePage() ? getWelcomeUser() : NotesAccountManager.getInstance().getCRMUser());
		sugarUserText.addModifyListener(listener);

		Label passwordLabel = new Label(group, SWT.NONE);
		passwordLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CRM_SERVER_PASSWORD));

		sugarPasswordText = new Text(group, SWT.PASSWORD | SWT.BORDER);
		sugarPasswordText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		sugarPasswordText.setText(isFromWelcomePage() ? getWelcomePassword() : NotesAccountManager.getInstance().getCRMPassword());
		sugarPasswordText.addModifyListener(listener);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public boolean performOk() {
		boolean ok = true;

		Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
		prefs.setValue(SugarWidgetUpdater.REFRESH_REGEXES_INTERVAL_PREF_KEY, Integer.toString(regexRefreshInterval));
		prefs.setValue(SugarWidgetUpdater.MAX_SIDEBAR_CARDS_PREF_KEY, Integer.toString(maxRecentlyViewedCards));

		// CorePluginActivator.getDefault().savePluginPreferences();
		updateMatchContentTypes();

		Account crmAccount = NotesAccountManager.getInstance().getCRMAccount();

		boolean somethingChanged = false;
		somethingChanged = !sugarServer.equals(crmAccount.getProperty(AuthProperties.SERVER)) || !sugarUser.equals(crmAccount.getProperty(AuthProperties.USER_NAME));

		if (!somethingChanged) {
			char[][] storedPasswords = new char[][]{crmAccount.getAccountPassword()};
			char[][] passwords = new char[][]{sugarPassword.toCharArray()};

			// 26437
			if (((crmAccount.getAccountPassword() == null || crmAccount.getAccountPassword().equals(ConstantStrings.EMPTY_STRING)) && sugarPassword != null)
					|| (crmAccount.getAccountPassword() != null && (sugarPassword == null || sugarPassword.equals(ConstantStrings.EMPTY_STRING)))) {
				somethingChanged = true;
			} else {
				for (int i = 0; i < passwords.length; i++) {
					if (passwords[i].length == storedPasswords[i].length) {
						for (int j = 0; j < passwords[i].length; j++) {
							if (passwords[i][j] != storedPasswords[i][j]) {
								somethingChanged = true;
								break;
							}
						}
					} else {
						somethingChanged = true;
						break;
					}
				}
			}
		}

		updateCrmAccountInfo(sugarServer, sugarUser, sugarPassword);

		if (somethingChanged) {
			Job job = new Job(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_UPDATING_LIVE_TEXT_RECOGNIZERS)) {
				@Override
				protected IStatus run(IProgressMonitor arg0) {
					// Check the credentials on the CRM server
					// no need to bring up the credentail prompt dialog if invalid credential
					boolean success = new SugarWidgetUpdater().loadUserData(false, false);
					if (!success) {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								errorMsgDialog();
							}
						});
					}
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}

		return ok;
	}

	private void updateMatchContentTypes() {
		String contentMatchAccountCurr = accountContentTypeCheckBox.getSelection() ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$
		String contentMatchOpptyCurr = opptyContentTypeCheckBox.getSelection() ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$
		String contentMatchContactCurr = contactContentTypeCheckBox.getSelection() ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$
		if (contentMatchAccountCurr.equals(contentMatchAccountOrig) && contentMatchOpptyCurr.equals(contentMatchOpptyOrig) && contentMatchContactCurr.equals(contentMatchContactOrig)) {
		} else {
			Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
			prefs.setValue(SugarWebservicesOperations.SALESCONNECT_LIVETEXT_MATCH_ACCOUNT_PREF_KEY, contentMatchAccountCurr);
			prefs.setValue(SugarWebservicesOperations.SALESCONNECT_LIVETEXT_MATCH_OPPTY_PREF_KEY, contentMatchOpptyCurr);
			prefs.setValue(SugarWebservicesOperations.SALESCONNECT_LIVETEXT_MATCH_CONTACT_PREF_KEY, contentMatchContactCurr);
			// update corresponding checkbox in LiveText Preference
			CorePluginActivator.getDefault().savePluginPreferences();

			updateLivetextPreferenceContentTypes();
		}
	}

	private void updateLivetextPreferenceContentTypes() {

		SugarWebservicesOperations.getInstance().removePreferenceChangeListener();

		try {

			// update cached preference values
			IEclipsePreferences preferences = new ManagedSettingsScope().getNode("com.ibm.rcp.content");
			preferences.put(SugarWebservicesOperations.LIVETEXT_PREFERENCE_ACCOUNT_CONTENT_TYPE, String.valueOf(accountContentTypeCheckBox.getSelection()));
			preferences.put(SugarWebservicesOperations.LIVETEXT_PREFERENCE_OPPTY_CONTENT_TYPE, String.valueOf(opptyContentTypeCheckBox.getSelection()));
			preferences.put(SugarWebservicesOperations.LIVETEXT_PREFERENCE_CONTACT_CONTENT_TYPE, String.valueOf(contactContentTypeCheckBox.getSelection()));

			preferences.flush();

			// update content type registry... this is where the List Text Preference page gets values from
			ContentTypeRegistry registry = ContentTypeRegistry.getDefault();
			ContentType type = registry.getContentType(SugarWidgetUpdater.ACCOUNT_TYPE_EXTENSION_ID);
			if (type != null) {
				type.setEnabled(accountContentTypeCheckBox.getSelection());
			}
			type = registry.getContentType(SugarWidgetUpdater.OPPORTUNITY_TYPE_EXTENSION_ID);
			if (type != null) {
				type.setEnabled(opptyContentTypeCheckBox.getSelection());
			}
			type = registry.getContentType(SugarWidgetUpdater.CONTACT_TYPE_EXTENSION_ID);
			if (type != null) {
				type.setEnabled(contactContentTypeCheckBox.getSelection());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		SugarWebservicesOperations.getInstance().addPreferenceChangeListener();

	}

	private void errorMsgDialog() {
		if (SugarWebservicesOperations.getInstance().unableToLogin()) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CRM_SERVER_CONNECTION_ERROR_TITLE), UtilsPlugin
					.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CRM_SERVER_CONNECTION_ERROR));

		} else if (SugarWebservicesOperations.getInstance().isServerIncorrect()) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CRM_SERVER_CONNECTION_ERROR_TITLE), UtilsPlugin
					.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CRM_SERVER_CONNECTION_ERROR2));
		}

		else {
			MessageDialog.openError(Display.getDefault().getActiveShell(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CRM_SERVER_CONNECTION_ERROR_TITLE), UtilsPlugin
					.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CRM_SERVER_CONNECTION_ERROR1));
		}
	}

	private boolean updateCrmAccountInfo(String server, String user, String password) {
		boolean ok = true;
		AccountsManager manager = AccountsManagerFactory.getAccountsManager();
		Account crmAccount = NotesAccountManager.getInstance().getCRMAccount();

		crmAccount.setAccountPassword(password.toCharArray());
		crmAccount.setProperty(AuthProperties.SERVER, server);
		crmAccount.setProperty(AuthProperties.USER_NAME, user);
		try {
			manager.updateAccount(crmAccount);

			// Force our connections stuff to reload the credentials
			AbderaConnectionsFileOperations.resetCredentials();
			SugarWebservicesOperations.getInstance().clearSession();
		} catch (AccountsException e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
			ok = false;
		}
		return ok;
	}

	@Override
	protected void performApply() {
		performOk();
	}

	private void setRefreshIntervals() {
		try {
			regexRefreshInterval = Integer.parseInt(regexRefreshIntervalSpinner.getText());
		} catch (Exception e) {
			regexRefreshInterval = SugarWidgetUpdater.REFRESH_INTERVAL_DEFAULT;
		}
	}

	private void setMaxRecentlyViewedCards() {
		try {
			maxRecentlyViewedCards = Integer.parseInt(recentlyViewedCardsSpinner.getText());
		} catch (Exception e) {
			maxRecentlyViewedCards = MIN_RECENTLY_VIEWED_CARDS;
		}
	}

	private void updateValues() {
		sugarServer = sugarServerText.getText().trim();
		sugarUser = sugarUserText.getText().trim();
		sugarPassword = sugarPasswordText.getText().trim();
		setRefreshIntervals();
		setMaxRecentlyViewedCards();
	}

	private Control[] createProgressGroup() {
		Control[] group = new Control[2];
		progressComposite.setLayoutDeferred(true);
		progressComposite.setVisible(true);
		((GridData) (progressComposite.getLayoutData())).exclude = false;

		Label label = new Label(progressComposite, SWT.WRAP);
		label.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		group[0] = label;

		ProgressBar progressBar = new ProgressBar(progressComposite, SWT.INDETERMINATE);
		progressBar.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		group[1] = progressBar;

		progressComposite.setLayoutDeferred(false);
		progressComposite.getParent().layout(true);
		progressComposite.layout(true);

		getShell().pack(true);

		return group;
	}

	private void removeProgressGroup(Control[] group) {
		if (progressComposite != null && !progressComposite.isDisposed()) {
			Control[] controls = progressComposite.getChildren();
			progressComposite.setLayoutDeferred(true);

			for (Control control : group) {
				if (!control.isDisposed()) {
					control.setVisible(false);
					((GridData) control.getLayoutData()).exclude = true;
					control.dispose();
				}
			}

			if (controls.length == 2) {
				progressComposite.setVisible(false);
				((GridData) (progressComposite.getLayoutData())).exclude = true;
			}

			progressComposite.setLayoutDeferred(false);
			progressComposite.getParent().layout(true);
			progressComposite.layout(true);
		}
	}

	private boolean isFromWelcomePage() {
		boolean b = false;
		if (welcomeServer != null || welcomeUser != null || welcomePassword != null) {
			b = true;
		}
		return b;
	}

	private String getWelcomeServer() {
		return welcomeServer;
	}

	private String getWelcomeUser() {
		return welcomeUser;
	}

	private String getWelcomePassword() {
		return welcomePassword;
	}

	public void applyData(Object obj) {

		if (obj != null && obj instanceof Account) {

			char[] password = ((Account) obj).getAccountPassword();
			if (password != null) {
				welcomePassword = new String(password);
			} else {
				welcomePassword = ConstantStrings.EMPTY_STRING;
			}
			welcomeServer = ((Account) obj).getProperty(AuthProperties.SERVER);
			welcomeUser = ((Account) obj).getProperty(AuthProperties.USER_NAME);
		}
	}

	public static PreferencePage getInstance() {
		if (preferencePage == null) {
			preferencePage = new PreferencePage();
		}
		return preferencePage;
	}
}
