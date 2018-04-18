package com.ibm.socialcrm.notesintegration.sugarwidgetupdate;

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
import java.beans.PropertyChangeSupport;
import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.eclipse.core.runtime.ContributorFactoryOSGi;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.osgi.framework.Bundle;

import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.SugarEntrySurrogate;
import com.ibm.socialcrm.notesintegration.core.utils.AbderaConnectionsFileOperations;
import com.ibm.socialcrm.notesintegration.core.utils.SugarDashboardPreference;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.core.utils.UpdateSelectionsBroadcaster;
import com.ibm.socialcrm.notesintegration.core.utils.WebServiceInfoDataShare;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataHub;
import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataShare;
import com.ibm.socialcrm.notesintegration.utils.datahub.calllog.CallFormDataShare;
import com.ibm.socialcrm.notesintegration.utils.datahub.calllog.CurrentSugarEntryDataShare;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFAHyperlink;

public class SugarWidgetUpdater implements IStartup {
	private final String CONTACTS_PREF_KEY = "com.ibm.socialcrm.sugarwidget.contactsRegex"; //$NON-NLS-1$
	private final String ACCOUNTS_PREF_KEY = "com.ibm.socialcrm.sugarwidget.accountsRegex"; //$NON-NLS-1$
	private final String OPPORTUNTIES_PREF_KEY = "com.ibm.socialcrm.sugarwidget.opportunitiesRegex"; //$NON-NLS-1$
	private final String ACCOUNT_TAGS_PREF_KEY = "com.ibm.socialcrm.sugarwidget.accountTags"; //$NON-NLS-1$
	private final String CONTACT_TAGS_PREF_KEY = "com.ibm.socialcrm.sugarwidget.contactTags"; //$NON-NLS-1$
	private final String OPPORTUNITIES_TAGS_PREF_KEY = "com.ibm.socialcrm.sugarwidget.opportunitiesTags"; //$NON-NLS-1$
	private final String CALL_FORM_KEY = "com.ibm.socialcrm.sugarwidget.callForm"; //$NON-NLS-1$
	private final String PROMPTED_FOR_CERT = "com.ibm.socialcrm.sugarwidget.promptedForCert"; //$NON-NLS-1$

	protected final static String FAVORITES_PREF = "com.ibm.socialcrm.sugarupdate.favoritesPref"; //$NON-NLS-1$
	public static final String REFRESH_REGEXES_INTERVAL_PREF_KEY = "com.ibm.socialcrm.sugarupdate.refreshInterval"; //$NON-NLS-1$
	public static final String MAX_SIDEBAR_CARDS_PREF_KEY = "com.ibm.socialcrm.sugarupdate.maxSidebarCards"; //$NON-NLS-1$
	public static final int REFRESH_INTERVAL_DEFAULT = 360;

	private final String CONTACT_RECOGNIZER_EXTENSION_ID = "SugarContactRecognizer"; //$NON-NLS-1$
	private final String ACCOUNT_RECOGNIZER_EXTENSION_ID = "SugarAccountRecognizer"; //$NON-NLS-1$
	private final String OPPORTUNITY_RECOGNIZER_EXTENSION_ID = "SugarOpportunityRecognizer"; //$NON-NLS-1$

	public static final String CONTACT_TYPE_EXTENSION_ID = "SugarContactType"; //$NON-NLS-1$
	public static final String ACCOUNT_TYPE_EXTENSION_ID = "SugarAccountType"; //$NON-NLS-1$
	public static final String OPPORTUNITY_TYPE_EXTENSION_ID = "SugarOpportunityType"; //$NON-NLS-1$

	private static long lastRegexRefreshTime = 0;
	private static boolean isLoading = false;

	private PropertyChangeListener credentialPromptListener;
	private WelcomeDialog welcomeDialog = null;

	private int count = 0;

	@Override
	public void earlyStartup() {
		System.out.print("..... sugarwidgetUpdater early start ...\n");

		PropertyChangeListener _propertyChangedListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {

				if (evt != null) {
					if (evt.getPropertyName() != null && evt.getPropertyName().equals(UpdateSelectionsBroadcaster.UPDATE_CERTIFICATE)) {
						System.out.println(".... got hit ...");
						count++;
						if (count > 1) {
							count = 0;
						} else {

							trustCertAndLoadData();

						}
					}

				}
			}
		};
		System.out.println("create property changed listener");
		UpdateSelectionsBroadcaster.getInstance().registerListener(_propertyChangedListener);

		createDataShares();

		addPropertyChangeListener();

		SugarWebservicesOperations.getInstance().addPreferenceChangeListener();

	}

	private void trustCertAndLoadData() {

		final Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
		// 74684
		final String prompted = prefs.getString(PROMPTED_FOR_CERT);
		System.out.println("prompted for cert value : " + prompted);

		// Put it in a job so it can be displayed in the Notes progress bar.
		Job job = new Job(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_UPDATING_LIVE_TEXT_RECOGNIZERS)) {
			@Override
			protected IStatus run(IProgressMonitor arg0) {

				if (!prompted.equals("true")) //$NON-NLS-1$
				{

					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							if (prompted.equalsIgnoreCase("recert")) {
								MessageDialog.openInformation(Display.getDefault().getShells()[0], UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ESTABLISH_TRUST_TITLE), UtilsPlugin
										.getDefault().getResourceString(UtilsPluginNLSKeys.ESTABLISH_TRUST_MESSAGE1));
								// 74684 - add dummy query to force the browser not to cache this page
								//GenericUtils.launchUrlInPreferredBrowser("https://localhost:" + ConstantStrings.HTTP_PROXY_PORT + "/sfawebapi/acceptCert.html", false); //$NON-NLS-1$ //$NON-NLS-2$
								GenericUtils.launchUrlInPreferredBrowser("https://localhost:" + ConstantStrings.HTTP_PROXY_PORT + "/sfawebapi/acceptCert.html" + "?ver=2", false); //$NON-NLS-1$ //$NON-NLS-2$

							} else {
								MessageDialog.openInformation(Display.getDefault().getShells()[0], UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ESTABLISH_TRUST_TITLE), UtilsPlugin
										.getDefault().getResourceString(UtilsPluginNLSKeys.ESTABLISH_TRUST_MESSAGE));
								// 74684 - add dummy query to force the browser not to cache this page
								//GenericUtils.launchUrlInPreferredBrowser("https://localhost:" + ConstantStrings.HTTP_PROXY_PORT + "/sfawebapi/acceptCert.html", false); //$NON-NLS-1$ //$NON-NLS-2$
								GenericUtils.launchUrlInPreferredBrowser("https://localhost:" + ConstantStrings.HTTP_PROXY_PORT + "/sfawebapi/acceptCert.html" + "?ver=1", false); //$NON-NLS-1$ //$NON-NLS-2$
							}
							prefs.setValue(PROMPTED_FOR_CERT, "true"); //$NON-NLS-1$					
							CorePluginActivator.getDefault().savePluginPreferences();

						}
					});

					// 47997 - sleep 30 sec to let cert process get a haead start
					try {
						Thread.sleep(20000);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				loadUserData(true);

				return Status.OK_STATUS;
			}
		};
		job.schedule();

		// Spawn a separate thread that would call loadPattern
		Thread regexRefreshThread = new Thread() {
			@Override
			public void run() {
				Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();

				while (true) {
					try {
						// Wake up every minute to check to see if we need to reload.
						Thread.sleep(60000);
						long currentTime = System.currentTimeMillis();
						if (lastRegexRefreshTime != 0) {
							long difference = currentTime - lastRegexRefreshTime;
							int regexPref = prefs.getInt(REFRESH_REGEXES_INTERVAL_PREF_KEY);
							regexPref = regexPref == 0 ? REFRESH_INTERVAL_DEFAULT : regexPref;
							int refreshTimeInMillis = regexPref * 60 * 1000;

							// If the time difference from the last refresh time is a "long" time (say, > 6 hours), it may be a result of the
							// user putting their laptop to sleep at night and waking it up again in the morning. If we notice this condition,
							// we'll generate a random time between 1-60 minutes and do the refresh after that time has expired. This will help
							// smooth out the load when a bunch of sellers in the same timezone come online all at once. Note that this does not apply
							// to a fresh restart of Notes.
							if (difference > 1000 * 60 * 60 * 6) // millisec * sec * min * hour = 6 hours
							{
								difference = 0; // Short circuit the check below since we just started up. We'll check the next time this thread fires.
								Random rand = new Random();
								int randomWaitTime = rand.nextInt(60) + 1;
								// Set the last refesh time so that the next one fires in "randomWaitTime" minutes
								lastRegexRefreshTime = currentTime - refreshTimeInMillis + (randomWaitTime * 60 * 1000); // Reset this
							}

							if (difference >= refreshTimeInMillis) {
								// Put it in a job so that it can be displayed in the Notes progress bar.
								Job job = new Job(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_UPDATING_LIVE_TEXT_RECOGNIZERS)) {
									@Override
									protected IStatus run(IProgressMonitor arg0) {
										// 26183 - if user had login problem due to incorrect user/pswd, to avoid excessive
										// login attempt, we will skip calling loadUserData.
										if (!SugarWebservicesOperations.getInstance().unableToLogin()) {
											loadUserData(true);
										}
										return Status.OK_STATUS;
									}
								};
								job.schedule();
							}
						}
					} catch (InterruptedException e) {
						UtilsPlugin.getDefault().logException(e, SugarWidgetUpdateActivator.PLUGIN_ID);
					}
				}
			}
		};
		regexRefreshThread.start();
	}

	private static boolean connectionErrorDialogOpen = false;

	public boolean loadUserData(boolean showErrorPopup) {
		return loadUserData(showErrorPopup, true);
	}

	/**
	 * Loads all data relevant to the SugarCRM user specified in the preferences and updates the internal data stores as necessary. This method refreshes the following things:
	 * 
	 * 1) Regex patterns - The patterns used to match contacts, opptys, and accounts in live text 2) Call log form data - The custom forms used for call logging that are unique to a given user
	 * 
	 * @param showErrorPopup
	 *        - Flag that indicates if an error message should be shown if the pattern load fails
	 * @param showCredentialPrompt
	 *        - Flag indicating if the credential prompt should be shown if the pattern load fails false if we are currently at the Preference page
	 * 
	 * @return
	 */
	public boolean loadUserData(boolean showErrorPopup, final boolean showCredentialPrompt) {
		final boolean successful[] = {true};

		// First check to see if we are already loading from another call.
		// If true, set successful flag and return
		if (isLoading) {
			successful[0] = false;
		} else {
			try {

				SugarWidgetUpdateActivator.getDefault().resetPatterns();
				isLoading = true;
				loadPreferences();

				String contactsRegexLocal = null;
				String accountsRegexLocal = null;
				String opportunitiesRegexLocal = null;
				String accountTagsLocal = null;
				String contactTagsLocal = null;
				String opportunityTagsLocal = null;

				SugarWebservicesOperations.getInstance().resetState();

				Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
				// 
				// set showcredential prompt flag, so for example, if this method was called from Preference page
				// we will not need to bring up the credential prompt dialog.
				SugarWebservicesOperations.getInstance().setToPromptCredential(showCredentialPrompt);

				String formOutput = SugarWebservicesOperations.getInstance().getCallLogForm();

				if (!SugarWebservicesOperations.getInstance().unableToLogin()) {
					if (successful[0] && formOutput != null) {
						SugarWidgetUpdateActivator.getDefault().setCallFormInfo(formOutput);
						successful[0] &= processCallLogFormData(formOutput);
					} else {
						String oldFormInfo = SugarWidgetUpdateActivator.getDefault().getCallFormInfo();
						if (oldFormInfo != null && !oldFormInfo.equals(ConstantStrings.EMPTY_STRING)) {
							processCallLogFormData(oldFormInfo);
						}
						successful[0] = false;
					}
					// Get Sugar Localization Preference - this will run in the background.
					SugarDashboardPreference.getInstance().getSugarPreference();
					String connectionsURL = SugarWebservicesOperations.getInstance().getConnectionsURL();
					if (connectionsURL != null && connectionsURL.trim().length() > 0) {
						AbderaConnectionsFileOperations.setConnectionsURL(connectionsURL);
						prefs.setValue(AbderaConnectionsFileOperations.CONNECTIONS_SERVER_PREF_KEY, connectionsURL);
						CorePluginActivator.getDefault().savePluginPreferences();
					}

					String contactOutput = SugarWebservicesOperations.getInstance().getContactRegexes();
					String accountOutput = SugarWebservicesOperations.getInstance().getAccountRegexes();
					String opptyOutput = SugarWebservicesOperations.getInstance().getOpptyRegexes();

					// Only show an error for credential failures. We'll ignore network issues.
					String pref = prefs.getString(CorePluginActivator.PREFERENCES_SHOW_CONNECTION_ERRORS);
					boolean preferenceSet = (pref == null || pref.equals(ConstantStrings.EMPTY_STRING) || Boolean.parseBoolean(pref));

					// If no hostname is set, don't bother showing the connection error. It likely means the plugin is not configured.
					String crmServer = NotesAccountManager.getInstance().getCRMServer();
					// if (crmServer.equals(ConstantStrings.EMPTY_STRING))
					// {
					// showErrorPopup = false;
					// }

					if (preferenceSet && showErrorPopup && SugarWebservicesOperations.getInstance().hasConnectionProblem() && !connectionErrorDialogOpen) {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								final MessageDialogWithToggle[] dialog = new MessageDialogWithToggle[1];

								dialog[0] = new MessageDialogWithToggle(Display.getDefault().getShells()[0], UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CONNECTION_ERROR), null,
										UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UNABLE_TO_CONNECT), MessageDialog.NONE, new String[]{UtilsPlugin.getDefault().getResourceString(
												UtilsPluginNLSKeys.OK)}, 0, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DO_NOT_SHOW_ME), false) {
									@Override
									protected Control createCustomArea(Composite composite) {
										SFAHyperlink settingsLink = new SFAHyperlink(composite, SWT.NONE);
										settingsLink.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).grab(false, false).create());
										settingsLink.setForeground(JFaceColors.getHyperlinkText(Display.getCurrent()));
										settingsLink.setUnderlined(true);
										settingsLink.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OPEN_PREFERENCES));
										settingsLink.addHyperlinkListener(new HyperlinkAdapter() {
											@Override
											public void linkActivated(HyperlinkEvent e) {
												PreferenceDialog prefDialog = PreferencesUtil.createPreferenceDialogOn(Display.getDefault().getShells()[0],
														"com.ibm.socialcrm.notesintegration.ui.preferencePage", //$NON-NLS-1$
														null, null);
												dialog[0].close();

												Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
												prefs.setValue(CorePluginActivator.PREFERENCES_SHOW_CONNECTION_ERRORS, Boolean.toString(!dialog[0].getToggleState()));
												CorePluginActivator.getDefault().savePluginPreferences();

												prefDialog.open();
											}
										});

										return composite;
									}
								};
								connectionErrorDialogOpen = true;
								if (dialog[0].open() == MessageDialog.OK) {
									Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
									prefs.setValue(CorePluginActivator.PREFERENCES_SHOW_CONNECTION_ERRORS, Boolean.toString(!dialog[0].getToggleState()));
									CorePluginActivator.getDefault().savePluginPreferences();
								}
								connectionErrorDialogOpen = false;
							}
						});
					}

					// TODO: This code will go away once we get a return code from the web service. For now, we just need to check the
					// output we get back from the server.
					try {
						JSONObject outputJson = new JSONObject(contactOutput);
						String name = outputJson.getString("number"); //$NON-NLS-1$
						// added checking for SFA0001 - invalid session
						if (name.equals("SFA0002") || name.equals("SFA0001")) //$NON-NLS-1$ //$NON-NLS-2$
						{
							successful[0] = false;
						}
					} catch (Exception e) {
						try {
							new JSONObject(contactOutput);
						} catch (Exception e2) {
							successful[0] = false;
							UtilsPlugin.getDefault().logException(e, SugarWidgetUpdateActivator.PLUGIN_ID);
						}
					}
					// End ----------------- TODO ----------------------

					if (contactOutput != null && accountOutput != null && opptyOutput != null) {

						try {
							// We get escaped unicode characters back for native characters from the webservice. Creating a new JSONArray will
							// convert the output to native characters. The problem is that Notes needs the regexes stored as escaped unicode.
							// That's why we have to add the extra slash. Since tags will not be in the notes widget as regexes, we should not
							// replace those.
							contactOutput = contactOutput.replaceAll("\\\\u", "\\\\\\\\u"); //$NON-NLS-1$ //$NON-NLS-2$
							accountOutput = accountOutput.replaceAll("\\\\u", "\\\\\\\\u"); //$NON-NLS-1$ //$NON-NLS-2$
							opptyOutput = opptyOutput.replaceAll("\\\\u", "\\\\\\\\u"); //$NON-NLS-1$ //$NON-NLS-2$

							JSONArray contactArray = new JSONObject(contactOutput).getJSONArray("regexData"); //$NON-NLS-1$
							JSONArray accountArray = new JSONObject(accountOutput).getJSONArray("regexData"); //$NON-NLS-1$
							JSONArray opptyArray = new JSONObject(opptyOutput).getJSONArray("regexData"); //$NON-NLS-1$

							contactsRegexLocal = contactArray.getString(0);
							accountsRegexLocal = accountArray.getString(0);
							if (accountsRegexLocal != null && !accountsRegexLocal.equals(ConstantStrings.EMPTY_STRING)) {
								accountsRegexLocal += ConstantStrings.PIPE;
							}
							accountsRegexLocal += GenericUtils.ACCOUNT_PATTERN;

							// Oppty matching is only pattern based. So really, this will never change. If we ever add oppty tags back, we can
							// append those to this.
							// opportunitiesRegexLocal = GenericUtils.OPPORTUNITY_PATTERN.toString();
							opportunitiesRegexLocal = buildOpptyPattern(opptyArray);

							try {
								contactTagsLocal = contactArray.getJSONObject(1).toString().replaceAll("\\\\\\\\u", "\\\\u"); //$NON-NLS-1$ //$NON-NLS-2$
							} catch (JSONException e) {
								// Eat it
							}
							try {
								accountTagsLocal = accountArray.getJSONObject(1).toString().replaceAll("\\\\\\\\u", "\\\\u"); //$NON-NLS-1$ //$NON-NLS-2$
							} catch (JSONException e) {
								// Eat it
							}

						} catch (JSONException jsonException) {
							// Ignore, in case more parameters are added in the webservice that we can ignore.
						}
					} else {
						successful[0] = false;
					}

					if (contactsRegexLocal != null) {
						SugarWidgetUpdateActivator.getDefault().setContactsRegex(contactsRegexLocal);
					}

					if (accountsRegexLocal != null) {
						SugarWidgetUpdateActivator.getDefault().setAccountsRegex(accountsRegexLocal);
					}

					if (opportunitiesRegexLocal != null) {
						SugarWidgetUpdateActivator.getDefault().setOpportunitiesRegex(opportunitiesRegexLocal);
					}

					if (accountTagsLocal != null) {
						SugarWidgetUpdateActivator.getDefault().setAccountTags(accountTagsLocal);
					}

					if (contactTagsLocal != null) {
						SugarWidgetUpdateActivator.getDefault().setContactTags(contactTagsLocal);
					}

					if (opportunityTagsLocal != null) {
						SugarWidgetUpdateActivator.getDefault().setOpportunityTags(opportunityTagsLocal);
					}

					if (SugarWidgetUpdateActivator.getDefault().getContactsRegex() != null || SugarWidgetUpdateActivator.getDefault().getAccountsRegex() != null) {
						String extensionXml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" + "<?eclipse version=\"3.2\"?>\n" //$NON-NLS-1$//$NON-NLS-2$
								+ "<plugin>\n"; //$NON-NLS-1$

						if (SugarWidgetUpdateActivator.getDefault().getContactsRegex() != null) {
							extensionXml += createExtension(CONTACT_RECOGNIZER_EXTENSION_ID, "Sugar Contact Recognizer", //$NON-NLS-1$
									CONTACT_TYPE_EXTENSION_ID, SugarWidgetUpdateActivator.getDefault().getContactsRegex()
											.replaceAll(ConstantStrings.AMPERSAND, "&amp;").replaceAll(ConstantStrings.LESS_THAN, "&lt;").replaceAll("\"", "&quot;")); //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$ //$NON-NLS-4$
						}
						if (SugarWidgetUpdateActivator.getDefault().getAccountsRegex() != null) {
							extensionXml += createExtension(ACCOUNT_RECOGNIZER_EXTENSION_ID, "Sugar Account Recognizer", //$NON-NLS-1$
									ACCOUNT_TYPE_EXTENSION_ID, SugarWidgetUpdateActivator.getDefault().getAccountsRegex()
											.replaceAll(ConstantStrings.AMPERSAND, "&amp;").replaceAll(ConstantStrings.LESS_THAN, "&lt;").replaceAll("\"", "&quot;")); //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$ //$NON-NLS-4$
						}
						if (SugarWidgetUpdateActivator.getDefault().getOpportunitiesRegex() != null) {
							extensionXml += createExtension(OPPORTUNITY_RECOGNIZER_EXTENSION_ID, "Sugar Opportunity Recognizer", //$NON-NLS-1$
									OPPORTUNITY_TYPE_EXTENSION_ID, SugarWidgetUpdateActivator.getDefault().getOpportunitiesRegex()
											.replaceAll(ConstantStrings.AMPERSAND, "&amp;").replaceAll(ConstantStrings.LESS_THAN, "&lt;").replaceAll("\"", "&quot;")); //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$ //$NON-NLS-4$
						}

						extensionXml += "</plugin>\n"; //$NON-NLS-1$

						final IExtensionRegistry reg = Platform.getExtensionRegistry();

						InputStream in = new ByteArrayInputStream(extensionXml.getBytes());
						Bundle bundle = SugarWidgetUpdateActivator.getDefault().getBundle();
						IContributor contr = ContributorFactoryOSGi.createContributor(bundle);
						reg.addContribution(in, contr, false, null, null, null);

						savePreferences();

						// String favoritesOutput = SugarWebservicesOperations.getInstance().getFavoritesFromWebservice();
						// processFavoritiesOutput(favoritesOutput);

						SugarWidgetUpdateActivator.getDefault().saveFavorites();

						SugarWidgetUpdateActivator.getDefault().getPropertyChangeSupport().firePropertyChange(SugarWidgetUpdateActivator.UPDATE_COMPLETE_PROPERTY, false, true);
					}
					lastRegexRefreshTime = System.currentTimeMillis();
				} // if (!SugarWebservicesOperations.getInstance().unableToLogin())
				else {
					successful[0] = false;
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, SugarWidgetUpdateActivator.PLUGIN_ID);
				successful[0] = false;
			}
			isLoading = false;

			// reset promptcredential dialog flag
			SugarWebservicesOperations.getInstance().setToPromptCredential(true);
		}

		return successful[0];
	}

	private String buildOpptyPattern(JSONArray opptyArray) {
		String regexX = ConstantStrings.EMPTY_STRING;

		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		sb.append(GenericUtils.OPPORTUNITY_PATTERNX);
		// sb.append(GenericUtils.OPPORTUNITY_PATTERN_1);

		try {

			boolean isEmptyPrefix = true;
			try {
				opptyArray.getJSONArray(0);
				isEmptyPrefix = false;
			} catch (Exception e) {
			}
			if (!isEmptyPrefix) {
				JSONArray jsonarray = opptyArray.getJSONArray(0);
				List<String> array = new ArrayList<String>();
				Iterator it = jsonarray.iterator();
				while (it.hasNext()) {
					String s = (String) it.next();
					boolean isInPredefinedPattern = false;
					for (int i = 0; i < GenericUtils.OPPORTUNITY_HARDCODED_PATTERNS.length; i++) {
						if (s != null && !s.equals(ConstantStrings.EMPTY_STRING)) {
							if (s.trim().equalsIgnoreCase(GenericUtils.OPPORTUNITY_HARDCODED_PATTERNS[i])) {
								isInPredefinedPattern = true;
								break;
							}
						}
					}
					if (!isInPredefinedPattern) {
						// "+" is a pattern qualifier, quote it
						s = Pattern.quote(s);

						// for BDS prefix, use BDSprefix + "[A-Za-z0-9-]{5,22}" pattern, is this too loose?
						sb.append(ConstantStrings.PIPE).append(GenericUtils.REQUIRE_NUMBER_BOUNDARY_PREFIX).append(s).append("[A-Za-z0-9-]{5,22}").append(GenericUtils.BOUNDARY_SUFFIX);

					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		String p = sb.toString();

		// 45326
		if (p != null) {
			GenericUtils.OPPORTUNITY_PATTERN = Pattern.compile(p);
		}

		regexX = Pattern.compile(p).toString();
		return regexX;
	}

	public void promptForCredential(Shell shell) {
		if (welcomeDialog == null) {
			SugarWebservicesOperations.getInstance().setCredentialPromptOpen(true);
			welcomeDialog = new WelcomeDialog(shell);
			welcomeDialog.open();
			welcomeDialog = null;
			SugarWebservicesOperations.getInstance().setCredentialPromptOpen(false);

		}
	}

	/**
	 * Loads the call form data share
	 * 
	 * @param formJSON
	 * @return
	 */
	private boolean processCallLogFormData(String formJSON) {
		boolean success = true;
		try {
			JSONObject outputJson = new JSONObject(formJSON);
			SFADataHub hub = SFADataHub.getInstance();
			CallFormDataShare callFormShare = (CallFormDataShare) hub.getDataShare(CallFormDataShare.SHARE_NAME);

			// TODO: Need to synchronize this operation somehow
			callFormShare.clear();
			// save the call form label if present
			if (outputJson.has("callFormLabel")) { //$NON-NLS-1$
				callFormShare.put("callFormLabel", outputJson.getString("callFormLabel")); //$NON-NLS-1$  //$NON-NLS-2$
			}
			JSONObject form = outputJson.getJSONObject("callForm"); //$NON-NLS-1$
			success = callFormShare.loadDataShare(form);
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, SugarWidgetUpdateActivator.PLUGIN_ID);
		}
		return success;
	}

	private void processFavoritiesOutput(String favoritesOutput) {
		Set<SugarEntrySurrogate> favorites = null;
		if (favoritesOutput != null) {
			try {
				favorites = new HashSet<SugarEntrySurrogate>();
				JSONObject json = new JSONObject(favoritesOutput);
				if (favoritesOutput.contains(ConstantStrings.DATABASE_MODULE_ACCOUNTS)) {
					JSONArray jsonArray = json.getJSONArray(ConstantStrings.DATABASE_MODULE_ACCOUNTS);
					favorites.addAll(createFavoritesFromJson(jsonArray, SugarType.ACCOUNTS));
				}
				if (favoritesOutput.contains(ConstantStrings.DATABASE_MODULE_OPPORTUNITIES)) {
					JSONArray jsonArray = json.getJSONArray(ConstantStrings.DATABASE_MODULE_OPPORTUNITIES);
					favorites.addAll(createFavoritesFromJson(jsonArray, SugarType.OPPORTUNITIES));
				}
				if (favoritesOutput.contains(ConstantStrings.DATABASE_MODULE_CONTACTS)) {
					JSONArray jsonArray = json.getJSONArray(ConstantStrings.DATABASE_MODULE_CONTACTS);
					favorites.addAll(createFavoritesFromJson(jsonArray, SugarType.CONTACTS));
				}
			} catch (JSONException e) {
				UtilsPlugin.getDefault().logException(e, SugarWidgetUpdateActivator.PLUGIN_ID);
			}
		}
		if (favorites != null) {
			// We will overwrite what's in the current preferences since we got a new set of favorites from the webservice.
			SugarWidgetUpdateActivator.getDefault().setFavorites(favorites);
		}
	}

	/**
	 * Creates any data shares that will be needed by other parts of the application.
	 */
	private void createDataShares() {
		SFADataHub hub = SFADataHub.getInstance();
		// Create the call form data share
		SFADataShare callFormShare = hub.getDataShare(CallFormDataShare.SHARE_NAME);
		if (callFormShare == null) {
			hub.addDataShare(CallFormDataShare.getInstance());
		}

		// Create the data share that contains the current selected sugar entry (current selected entry from a call logign perspective anyway)
		SFADataShare currentSelectedShare = hub.getDataShare(CurrentSugarEntryDataShare.SHARE_NAME);
		if (currentSelectedShare == null) {
			currentSelectedShare = new CurrentSugarEntryDataShare();
			hub.addDataShare(currentSelectedShare);
		}

		// Datashare that holds information about
		SFADataShare webServiceInfoShare = hub.getDataShare(WebServiceInfoDataShare.SHARE_NAME);
		if (webServiceInfoShare == null) {
			hub.addDataShare(WebServiceInfoDataShare.getInstance());
		}
	}

	private Set<SugarEntrySurrogate> createFavoritesFromJson(JSONArray jsonArray, SugarType sugarType) {
		Set<SugarEntrySurrogate> favorites = new HashSet<SugarEntrySurrogate>();
		for (int i = 0; i < jsonArray.length(); i++) {
			try {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				String favid = jsonObject.getString(ConstantStrings.DATABASE_FAV_ID);
				String id = jsonObject.getString(ConstantStrings.DATABASE_ID);
				String name = sugarType.equals(SugarType.ACCOUNTS) || sugarType.equals(SugarType.OPPORTUNITIES) ? jsonObject.getString(ConstantStrings.DATABASE_NAME) : sugarType
						.equals(SugarType.CONTACTS)
						? jsonObject.getString(ConstantStrings.DATABASE_FIRST_NAME) + ConstantStrings.SPACE + jsonObject.getString(ConstantStrings.DATABASE_LAST_NAME)
						: null;

				if (name != null) {
					SugarEntrySurrogate favorite = new SugarEntrySurrogate(name, sugarType);
					favorite.setFavoriteId(favid);
					favorite.setId(id);
					favorites.add(favorite);
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, SugarWidgetUpdateActivator.PLUGIN_ID);
			}
		}
		return favorites;
	}

	private String createExtension(String extensionId, String extensionName, String contentTypeId, String match) {
		String extension = "<extension id=\"" + extensionId + "\" point=\"com.ibm.rcp.annotation.regex.regexTypes\">\n" //$NON-NLS-1$//$NON-NLS-2$
				+ "  <regexTypes contentTypeId=\"" + contentTypeId + "\"  id=\"" + extensionId + "\" match=\"" + match + "\"\n" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
				+ " name=\"" + extensionName + "\">\n" + "    <group contentTypePropertyId=\"contents\" number=\"0\"/>\n" //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				+ "  </regexTypes>\n" + "</extension>\n"; //$NON-NLS-1$//$NON-NLS-2$
		return extension;
	}

	/**
	 * Load the preferences
	 */
	private void loadPreferences() {
		Preferences preferences = CorePluginActivator.getDefault().getPluginPreferences();
		String contacts = preferences.getString(CONTACTS_PREF_KEY);
		if (contacts != null && !contacts.equals(ConstantStrings.EMPTY_STRING)) {
			SugarWidgetUpdateActivator.getDefault().setContactsRegex(contacts);
		}

		String accounts = preferences.getString(ACCOUNTS_PREF_KEY);
		if (accounts != null && !accounts.equals(ConstantStrings.EMPTY_STRING)) {
			SugarWidgetUpdateActivator.getDefault().setAccountsRegex(accounts);
		}

		String opportunities = preferences.getString(OPPORTUNTIES_PREF_KEY);
		if (opportunities != null && !opportunities.equals(ConstantStrings.EMPTY_STRING)) {
			SugarWidgetUpdateActivator.getDefault().setOpportunitiesRegex(opportunities);
		}

		String accountTags = preferences.getString(ACCOUNT_TAGS_PREF_KEY);
		if (accountTags != null && !accountTags.equals(ConstantStrings.EMPTY_STRING)) {
			SugarWidgetUpdateActivator.getDefault().setAccountTags(accountTags);
		}

		String contactTags = preferences.getString(CONTACT_TAGS_PREF_KEY);
		if (contactTags != null && !contactTags.equals(ConstantStrings.EMPTY_STRING)) {
			SugarWidgetUpdateActivator.getDefault().setContactTags(contactTags);
		}

		String opportunityTags = preferences.getString(OPPORTUNITIES_TAGS_PREF_KEY);
		if (opportunityTags != null && !opportunityTags.equals(ConstantStrings.EMPTY_STRING)) {
			SugarWidgetUpdateActivator.getDefault().setOpportunityTags(opportunityTags);
		}

		String callFormData = preferences.getString(CALL_FORM_KEY);
		if (callFormData != null && !callFormData.equals(ConstantStrings.EMPTY_STRING)) {
			SugarWidgetUpdateActivator.getDefault().setCallFormInfo(callFormData);
		}

		String storedFavs = preferences.getString(SugarWidgetUpdater.FAVORITES_PREF);
		Set<SugarEntrySurrogate> favorites = new HashSet<SugarEntrySurrogate>();
		if (storedFavs != null && storedFavs.length() > 0) {
			XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(storedFavs.getBytes()));
			favorites = (Set<SugarEntrySurrogate>) decoder.readObject();
			SugarWidgetUpdateActivator.getDefault().setFavorites(favorites);
		}
	}

	private void savePreferences() {
		Preferences preferences = CorePluginActivator.getDefault().getPluginPreferences();
		if (SugarWidgetUpdateActivator.getDefault().getContactsRegex() != null) {
			preferences.setValue(CONTACTS_PREF_KEY, SugarWidgetUpdateActivator.getDefault().getContactsRegex());
		}
		if (SugarWidgetUpdateActivator.getDefault().getAccountsRegex() != null) {
			preferences.setValue(ACCOUNTS_PREF_KEY, SugarWidgetUpdateActivator.getDefault().getAccountsRegex());
		}
		if (SugarWidgetUpdateActivator.getDefault().getOpportunitiesRegex() != null) {
			preferences.setValue(OPPORTUNTIES_PREF_KEY, SugarWidgetUpdateActivator.getDefault().getOpportunitiesRegex());
		}
		if (SugarWidgetUpdateActivator.getDefault().getAccountTags() != null) {
			preferences.setValue(ACCOUNT_TAGS_PREF_KEY, SugarWidgetUpdateActivator.getDefault().getAccountTags());
		}
		if (SugarWidgetUpdateActivator.getDefault().getContactTags() != null) {
			preferences.setValue(CONTACT_TAGS_PREF_KEY, SugarWidgetUpdateActivator.getDefault().getContactTags());
		}
		if (SugarWidgetUpdateActivator.getDefault().getOpportunityTags() != null) {
			preferences.setValue(OPPORTUNITIES_TAGS_PREF_KEY, SugarWidgetUpdateActivator.getDefault().getOpportunityTags());
		}
		if (SugarWidgetUpdateActivator.getDefault().getCallFormInfo() != null) {
			preferences.setValue(CALL_FORM_KEY, SugarWidgetUpdateActivator.getDefault().getCallFormInfo());
		}
		SugarWidgetUpdateActivator.getDefault().savePluginPreferences();
	}

	public static PropertyChangeSupport getPropertyChangeSupport() {
		return SugarWebservicesOperations.getPropertyChangeSupport();
	}

	private void addPropertyChangeListener() {
		if (credentialPromptListener == null) {
			credentialPromptListener = new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent arg0) {
					Job job = new Job("credentialPromptListener") //$NON-NLS-1$
					{
						@Override
						protected IStatus run(IProgressMonitor arg0) {
							Display.getDefault().asyncExec(new Runnable() {
								@Override
								public void run() {
									promptForCredential(Display.getCurrent().getActiveShell());
								}
							});

							return Status.OK_STATUS;
						}
					};
					job.schedule();
				}
			};
			getPropertyChangeSupport().addPropertyChangeListener(SugarWebservicesOperations.BRING_UP_CREDENTIAL_PROMPT, credentialPromptListener);
		}
	}

}
