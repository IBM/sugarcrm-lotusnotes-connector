package com.ibm.socialcrm.notesintegration.sugarwidgetupdate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.progress.UIJob;

import com.ibm.rcp.accounts.Account;
import com.ibm.rcp.accounts.AccountsException;
import com.ibm.rcp.accounts.AccountsManager;
import com.ibm.rcp.accounts.AccountsManagerFactory;
import com.ibm.rcp.security.auth.AuthProperties;
import com.ibm.socialcrm.notesintegration.core.utils.AbderaConnectionsFileOperations;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.sugarwidgetupdate.SugarWidgetUpdater;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class WelcomeDialog extends Dialog {

	private static final String PROGRESS_BAR_ID = "progressBarId"; //$NON-NLS-1$
	private static int login_button_id = 0;
	private static int setting_button_id = 1;
	private static int cancel_button_id = 2;

	private String _progressId = null;
	private Composite _progressComposite = null;

	private String _server = null;
	private String _userName = null;
	private char[] _password = null;

	private String _bkupServer = null;
	private String _bkupUserName = null;
	private char[] _bkupPassword = null;

	private Text _serverControl = null;
	private Text _userNameControl = null;
	private Text _passwordControl = null;

	public WelcomeDialog(Shell shell) {
		super(shell);
		setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE | SWT.MIN | SWT.MAX);

		getCRMCredential();

	}

	private void setShellListeners() {
		// Close the shell when the user presses ESC
		getShell().addListener(SWT.Traverse, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.detail == SWT.TRAVERSE_ESCAPE) {
					cancelPressed();
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
					// Be sure setting shell to gridlayout before hand (for example: at configureShell())
					Point point = getShell().getSize();
					((GridData) getShell().getLayoutData()).widthHint = point.x;
					((GridData) getShell().getLayoutData()).heightHint = point.y;

				}
			}
		});
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		// This will set GridLayout to shell
		newShell.setLayoutData(GridDataFactory.fillDefaults().create());

		Monitor primary = Display.getDefault().getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = newShell.getBounds();

		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;

		newShell.setLocation(x, y);

		newShell.setSize(450, 360);
		newShell.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WELCOME_TITLE));

		newShell.pack(true);

	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Rectangle margins = Geometry.createDiffRectangle(20, 20, 20, 100);

		Composite composite = new Composite(parent, SWT.BORDER);
		composite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(margins).spacing(12, 8).create());
		composite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		composite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

		Label serverLabel = new Label(composite, SWT.NONE);
		serverLabel.setBackground(composite.getBackground());
		serverLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WELCOME_SERVER));
		_serverControl = new Text(composite, SWT.BORDER);
		_serverControl.setLayoutData(GridDataFactory.fillDefaults().hint(400, SWT.DEFAULT).grab(true, false).create());
		_serverControl.setText(getBkupServer());

		Label userNameLabel = new Label(composite, SWT.NONE);
		userNameLabel.setBackground(composite.getBackground());
		userNameLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WELCOME_USER));
		_userNameControl = new Text(composite, SWT.BORDER);
		_userNameControl.setLayoutData(GridDataFactory.fillDefaults().hint(400, SWT.DEFAULT).grab(true, false).create());
		_userNameControl.setText(getBkupUserName());

		Label passwordLabel = new Label(composite, SWT.NONE);
		passwordLabel.setBackground(composite.getBackground());
		passwordLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WELCOME_PASSWORD));
		_passwordControl = new Text(composite, SWT.PASSWORD | SWT.BORDER);
		_passwordControl.setLayoutData(GridDataFactory.fillDefaults().hint(400, SWT.DEFAULT).grab(true, false).create());
		_passwordControl.setText(new String(getBkupPassword()));

		// create the progress bar at the bottom of the shell
		createProgressComposite();

		setShellListeners();

		return composite;
	}

	// Create progress bar composite in the Shell. Will set it to visible when the Progress Bar
	// Indicator is needed.
	private void createProgressComposite() {
		_progressComposite = new Composite(getShell(), SWT.NONE);
		_progressComposite.setLayout(GridLayoutFactory.fillDefaults().create());
		_progressComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		((GridData) (_progressComposite.getLayoutData())).exclude = true;
		_progressComposite.setVisible(false);

		getShell().layout(true);

	}

	@Override
	protected void createButtonsForButtonBar(Composite composite) {
		// login button
		createButton(composite, login_button_id, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WELCOME_BUTTON_LOGIN), true);

		// settings button
		createButton(composite, setting_button_id, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WELCOME_BUTTON_SETTINGS), false);

		// cancel button
		createButton(composite, cancel_button_id, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WELCOME_BUTTON_CANCEL), false);

		// need to update layout; otherwise buttons won't show until resizing
		getShell().layout(true);
		getShell().pack(true);
		getShell().layout(true);
	}

	@Override
	protected void buttonPressed(int i) {
		// disable all the buttons
		enableButtons(false, false, false);

		if (i == login_button_id) {
			// getButton(login_button_id).setSelection(true);
			selectButtons(true, false, false);
			toLogin();
		} else if (i == setting_button_id) {
			// getButton(setting_button_id).setSelection(true);
			selectButtons(false, true, false);
			toSetting();
		} else if (i == cancel_button_id) {
			selectButtons(false, false, true);
			cancelPressed();
		}
	}

	private void selectButtons(boolean isLoginButtonToActive, boolean isSettingButtonToActive, boolean isCancelButtonToActive) {
		getButton(login_button_id).setSelection(isLoginButtonToActive);
		getButton(setting_button_id).setSelection(isSettingButtonToActive);
		getButton(cancel_button_id).setSelection(isCancelButtonToActive);
	}

	private void enableButtons(boolean isLoginButtonToActive, boolean isSettingButtonToActive, boolean isCancelButtonToActive) {
		getButton(login_button_id).setEnabled(isLoginButtonToActive);
		getButton(setting_button_id).setEnabled(isSettingButtonToActive);
		getButton(cancel_button_id).setEnabled(isCancelButtonToActive);
	}

	@Override
	protected void cancelPressed() {
		setPreferenceInfo(getBkupServer(), getBkupUserName(), getBkupPassword());
		super.cancelPressed();
	}

	private void toSettingOrig() {
		_server = _serverControl.getText() == null ? ConstantStrings.EMPTY_STRING : _serverControl.getText();
		_userName = _userNameControl.getText() == null ? ConstantStrings.EMPTY_STRING : _userNameControl.getText();
		_password = (_passwordControl.getText() == null || _passwordControl.getText().equals(ConstantStrings.EMPTY_STRING)) ? ConstantStrings.EMPTY_STRING.toCharArray() : _passwordControl.getText()
				.toCharArray();
		setPreferenceInfo(_server, _userName, _password);

		Account welcomeAccount = NotesAccountManager.getInstance().getCRMAccount();
		welcomeAccount.setAccountPassword(_password);
		welcomeAccount.setProperty(AuthProperties.SERVER, _server);
		welcomeAccount.setProperty(AuthProperties.USER_NAME, _userName);

		PreferenceDialog prefDialog = PreferencesUtil.createPreferenceDialogOn(Display.getDefault().getShells()[0], "com.ibm.socialcrm.notesintegration.ui.preferencePage", //$NON-NLS-1$
				null, welcomeAccount);
		cancelPressed();
		SugarWebservicesOperations.getInstance().setCredentialPromptOpen(false);
		prefDialog.open();
	}

	private void toSetting() {

		_server = _serverControl.getText() == null ? ConstantStrings.EMPTY_STRING : _serverControl.getText();
		_userName = _userNameControl.getText() == null ? ConstantStrings.EMPTY_STRING : _userNameControl.getText();
		_password = (_passwordControl.getText() == null || _passwordControl.getText().equals(ConstantStrings.EMPTY_STRING)) ? ConstantStrings.EMPTY_STRING.toCharArray() : _passwordControl.getText()
				.toCharArray();

		Job job = new Job("Setting from WelcomeDialog") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {

						_progressId = createProgressIndicator(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WELCOME_LOGIN_PROGRESS_MESSAGE));

					}
				});

				// save UI value to CRM so we can use the loadUserData() logic.
				setPreferenceInfo(_server, _userName, _password);
				final boolean[] success = {new SugarWidgetUpdater().loadUserData(false)};

				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						removeProgressIndicator(_progressId);

						PreferenceDialog prefDialog = PreferencesUtil.createPreferenceDialogOn(Display.getDefault().getShells()[0], "com.ibm.socialcrm.notesintegration.ui.preferencePage", //$NON-NLS-1$
								null, null);
						cancelPressed();
						SugarWebservicesOperations.getInstance().setCredentialPromptOpen(false);
						prefDialog.open();

					}
				});

				return Status.OK_STATUS;
			}
		};
		job.schedule();

	}

	private void toLogin() {

		_server = _serverControl.getText();
		_userName = _userNameControl.getText();
		_password = _passwordControl.getText().toCharArray();

		if ((_server == null || _server.equals(ConstantStrings.EMPTY_STRING) || _server.equals(ConstantStrings.SPACE))
				|| (_userName == null || _userName.equals(ConstantStrings.EMPTY_STRING) || _userName.equals(ConstantStrings.SPACE))
				|| (_password == null || _passwordControl.getText().equals(ConstantStrings.EMPTY_STRING) || _passwordControl.getText().equals(ConstantStrings.SPACE))) {
			errorLogin();
			return;
		}

		Job job = new Job("Login from WelcomeDialog") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						getButton(login_button_id).setEnabled(false);

						_progressId = createProgressIndicator(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WELCOME_LOGIN_PROGRESS_MESSAGE));

					}
				});

				// save UI value to CRM so we can use the loadUserData() logic.
				setPreferenceInfo(_server, _userName, _password);
				final boolean[] success = {new SugarWidgetUpdater().loadUserData(false)};

				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						removeProgressIndicator(_progressId);
						if (!success[0]) {
							// restore credential
							errorLogin();

						} else {
							close();
						}

					}
				});

				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private void errorLogin() {
		setPreferenceInfo(getBkupServer(), getBkupUserName(), getBkupPassword());
		errorLoginDialog();

		// re-activate all the buttons
		enableButtons(true, true, true);
		getButton(login_button_id).setSelection(true);
	}

	private void errorLoginDialog() {
		UIJob showErrorUIJob = new UIJob("showErrorUIJob") //$NON-NLS-1$
		{
			@Override
			public IStatus runInUIThread(IProgressMonitor arg0) {
				MessageDialog msgdialog = new MessageDialog(Display.getDefault().getActiveShell(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WELCOME_TITLE), SFAImageManager
						.getImage(SFAImageManager.SALES_CONNECT), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WELCOME_LOGIN_WARNING), MessageDialog.WARNING,
						new String[]{IDialogConstants.OK_LABEL}, 0);

				msgdialog.open();

				return Status.OK_STATUS;
			}

		};

		showErrorUIJob.schedule();

	}

	private void setPreferenceInfo(String server, String userName, char[] password) {
		updateCrmAccountInfo(server, userName, password);
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
				getShell().layout(true);
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
					getShell().layout(true);
					getShell().pack(true);
					getShell().layout(true);
				}
			}
		});
	}

	private void getCRMCredential() {
		_bkupServer = NotesAccountManager.getInstance().getCRMServer();
		_bkupUserName = NotesAccountManager.getInstance().getCRMUser();
		_bkupPassword = NotesAccountManager.getInstance().getCRMPassword() == null ? null : NotesAccountManager.getInstance().getCRMPassword().toCharArray();
	}

	private String getBkupServer() {
		return _bkupServer;
	}

	private String getBkupUserName() {
		return _bkupUserName;
	}

	private char[] getBkupPassword() {
		return _bkupPassword;
	}

	public boolean updateCrmAccountInfo(String server, String user, char[] password) {
		boolean ok = true;
		AccountsManager manager = AccountsManagerFactory.getAccountsManager();
		Account crmAccount = NotesAccountManager.getInstance().getCRMAccount();

		crmAccount.setAccountPassword(password);
		crmAccount.setProperty(AuthProperties.SERVER, server);
		crmAccount.setProperty(AuthProperties.USER_NAME, user);
		try {
			manager.updateAccount(crmAccount);

			// Force our connections stuff to reload the credentials
			AbderaConnectionsFileOperations.resetCredentials();
			SugarWebservicesOperations.getInstance().clearSession();
		} catch (AccountsException e) {
			UtilsPlugin.getDefault().logException(e, SugarWidgetUpdateActivator.PLUGIN_ID);
			ok = false;
		}
		return ok;
	}

}
