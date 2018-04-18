package com.ibm.socialcrm.notesintegration.ui.dashboardcomposites;

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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.ibm.rcp.browser.service.WebBrowser;
import com.ibm.rcp.swt.swidgets.SToolBar;
import com.ibm.rcp.swt.swidgets.SToolItem;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.WebSecurityCodeProvider;
import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataHub;
import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataShare;
import com.ibm.socialcrm.notesintegration.utils.datahub.calllog.CurrentSugarEntryDataShare;

public class CallLogComposite extends AbstractDashboardComposite {
	private Composite innerComposite;
	private WebBrowser browser;

	public CallLogComposite(Composite parent, int style) {
		super(parent, style, null, null);
	}

	@Override
	public void createInnerComposite() {
		innerComposite = new Composite(this, SWT.NONE);
		innerComposite.setLayout(GridLayoutFactory.fillDefaults().create());
		innerComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).create());
		innerComposite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

		try {
			browser = new WebBrowser(innerComposite, SWT.NONE, WebBrowser.IE_SERVICE);
		} catch (Exception e) {
			try {
				browser = new WebBrowser(innerComposite, SWT.NONE, WebBrowser.XULRUNNER_SERVICE);
			} catch (Exception e2) {
				UtilsPlugin.getDefault().logException(e2, UiPluginActivator.PLUGIN_ID);
			}
		}
		browser.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.FILL).create());

		String host = UiPluginActivator.getDefault().getHttpHostname();
		String port = Integer.toString(UiPluginActivator.getDefault().getHttpPort());

		String url = "http://" + host + ":" + port + "/socialcrm/callLog.html"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		browser.setUrl(url);

		// Add listener for web security code
		final PropertyChangeListener securityPCL = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				final String theCode = (String) arg0.getNewValue().toString();
				Display.getDefault().syncExec(new Runnable() {
					public void run() {/* System.out.println("setting security code via change to:" + theCode); */
						browser.execute("setSecurityCode('" + theCode + "')");}});//$NON-NLS-1$    
			}
		};
		WebSecurityCodeProvider.getInstance().addPropertyChangeListener(securityPCL);

		final PropertyChangeListener pcl = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				browser.execute("logCallFromCard()"); //$NON-NLS-1$
			}
		};

		Job job = new Job("Add listener for current sugar entry") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				SFADataShare share = SFADataHub.getInstance().blockingGetDataShare(CurrentSugarEntryDataShare.SHARE_NAME, 20000);
				if (share != null) {
					share.addPropertyChangeListener(CurrentSugarEntryDataShare.CURRENT_SUGAR_ENTRY, pcl);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();

		browser.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				SFADataShare share = SFADataHub.getInstance().getDataShare(CurrentSugarEntryDataShare.SHARE_NAME);
				if (share != null) {
					share.removePropertyChangeListener(CurrentSugarEntryDataShare.CURRENT_SUGAR_ENTRY, pcl);
				}
				WebSecurityCodeProvider.getInstance().removePropertyChangeListener(securityPCL);
			}
		});

		innerComposite.layout(true);
	}

	@Override
	public void createToolbarComposite() {
		SToolBar toolbar = new SToolBar(this, SWT.FLAT);
		toolbar.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		SToolItem item = new SToolItem(toolbar, SWT.PUSH);
		item.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_OPEN_CALL_LOG_FORM));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// 72458
				//GenericUtils.launchUrlInPreferredBrowser(NotesAccountManager.getInstance().getCRMServer() + "index.php?module=Calls&action=EditView&return_module=Calls&return_action=DetailView" //$NON-NLS-1$ 
				//		+ (GenericUtils.isUseEmbeddedBrowserPreferenceSet() ? ConstantStrings.EMPTY_STRING : "&MSID=" + SugarWebservicesOperations.getInstance().getSessionId(true)), true); //$NON-NLS-1$

				String aUrl = NotesAccountManager.getInstance().getCRMServer() + "index.php?module=Calls&action=EditView"; //$NON-NLS-1$ 
				if (!GenericUtils.isUseEmbeddedBrowserPreferenceSet()) {
					aUrl = SugarWebservicesOperations.getInstance().buildV10SeamlessURL(aUrl);
				}
				GenericUtils.launchUrlInPreferredBrowser(aUrl, true);
			}
		});
	}

	/**
	 * Called whenever the sidebar view that contains this is activated. This is here so we can tell IE to repaint.
	 */
	public void panelRaised() {
		if (browser != null && !browser.isDisposed()) {
			// set the current security code to make sure it gets there initially
			// System.out.println("setting security code to:" + WebSecurityCodeProvider.getInstance().getSecurityCodeString());
			//browser.execute("setSecurityCode('" + WebSecurityCodeProvider.getInstance().getSecurityCodeString() + "')"); //$NON-NLS-1$
			browser.execute("panelRaised('" + WebSecurityCodeProvider.getInstance().getSecurityCodeString() + "')"); //$NON-NLS-1$
		}
	}

	@Override
	public String getDashboardName() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_LOG_CALL);
	}

	@Override
	public void selectedItemsChanged() {

	}

	@Override
	public void createTitleComposite() {
		// Overriding to do nothing
	}

}
