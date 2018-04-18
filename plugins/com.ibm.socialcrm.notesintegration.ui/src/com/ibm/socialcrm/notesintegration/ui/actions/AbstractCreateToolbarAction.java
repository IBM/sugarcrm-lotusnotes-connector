package com.ibm.socialcrm.notesintegration.ui.actions;

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

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.ibm.rcp.browser.service.WebBrowser;
import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;

public abstract class AbstractCreateToolbarAction implements IWorkbenchWindowActionDelegate {

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IWorkbenchWindow arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void run(IAction arg0) {
		final Cookie[][] cookies = new Cookie[1][];
		try {
			HttpClient client = new HttpClient();

			PostMethod post = new PostMethod(NotesAccountManager.getInstance().getCRMServer());
			post.addParameter(ConstantStrings.USER_NAME, NotesAccountManager.getInstance().getCRMUser());
			post.addParameter(ConstantStrings.USER_PASSWORD, NotesAccountManager.getInstance().getCRMPassword());
			post.addParameter(ConstantStrings.MODULE, ConstantStrings.USERS);
			post.addParameter(ConstantStrings.RETURN_ACTION, ConstantStrings.LOGIN);
			post.addParameter(ConstantStrings.RETURN_MODULE, ConstantStrings.USERS);
			post.addParameter(ConstantStrings.ACTION, ConstantStrings.AUTHENTICATE);
			post.addParameter(ConstantStrings.LOGIN, ConstantStrings.LOG_IN);
			client.executeMethod(post);

			cookies[0] = client.getState().getCookies();

			post.releaseConnection();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				final Shell shell = new Shell(Display.getDefault());
				shell.setLayout(GridLayoutFactory.fillDefaults().create());
				try {
					WebBrowser browser = new WebBrowser(shell, SWT.NONE);

					for (int i = 0; i < cookies[0].length; i++) {
						browser.setCookie(NotesAccountManager.getInstance().getCRMServer(), cookies[0][i].toExternalForm());
					}

					browser.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
					browser.setUrl(getUrl());
					shell.open();

					browser.addDisposeListener(new DisposeListener() {
						@Override
						public void widgetDisposed(DisposeEvent arg0) {
							shell.dispose();
						}
					});
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
				}
			}
		});

	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
	}

	public abstract String getUrl();

}
