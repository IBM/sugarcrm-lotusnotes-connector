package com.ibm.socialcrm.notesintegration.feeds.dashboardcomposites;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.feeds.FeedsPluginActivator;
import com.ibm.socialcrm.notesintegration.ui.dashboardcomposites.AbstractDashboardComposite;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class FeedComposite extends AbstractDashboardComposite {
	private Browser browser;
	private boolean hasUncommittedChanges = false;
	private Composite mainComposite;

	private Cursor waitCursor;

	public FeedComposite(Composite parent, int style, String dashboardID, BaseSugarEntry sugarEntry) {
		super(parent, style, dashboardID, sugarEntry);
	}

	@Override
	public void createInnerComposite() {
		mainComposite = new Composite(this, SWT.NONE);
		mainComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).create());
		mainComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		mainComposite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

		waitCursor = new Cursor(getDisplay(), SWT.CURSOR_WAIT);

		mainComposite.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				waitCursor.dispose();
				if (browser != null) {
					browser.dispose();
					browser = null;
				}
			}
		});

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				createBrowser();
			}
		});
	}

	/**
	 * Creates the browser used by this composite
	 */
	private void createBrowser() {
		try {
			browser = new Browser(mainComposite, SWT.NONE);
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, FeedsPluginActivator.PLUGIN_ID);
		}

		mainComposite.setCursor(waitCursor);
		browser.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		final boolean[] loaded = new boolean[]{false};

		browser.addLocationListener(new LocationListener() {
			@Override
			public void changed(LocationEvent event) {
			}

			@Override
			public void changing(LocationEvent event) {
				String location = event.location.toLowerCase();
				if (location.startsWith("http://donotallowrefresh")) //$NON-NLS-1$
				{
					hasUncommittedChanges = true;
				} else if (location.startsWith("http://allowrefresh")) //$NON-NLS-1$
				{
					hasUncommittedChanges = false;
				}
				if (loaded[0]) {
					event.doit = false;
				}
			}
		});

		browser.addProgressListener(new ProgressListener() {
			@Override
			public void changed(ProgressEvent arg0) {
			}

			@Override
			public void completed(ProgressEvent arg0) {
				loaded[0] = true;
				mainComposite.setCursor(null);
				browser.removeProgressListener(this);
			}
		});

		browser.setUrl(getFeedUrl());

		mainComposite.layout(true);
	}

	@Override
	public boolean hasUncommittedChanges() {
		if (browser != null && !browser.isDisposed()) {
			String javascriptToExecute = "try" + "{" + "if (document.getElementById('data" + getSugarEntry().getId() //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ "').value.trim() != '')" + "{" + " document.location = 'http://donotallowrefresh';" + "}" + "else" + "{" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
					+ "  document.location = 'http://allowrefresh';" + "}" + "}" + "catch(e)" + "{" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
					+ "document.location = 'http://allowrefresh';" + "}"; //$NON-NLS-1$ //$NON-NLS-2$

			browser.execute(javascriptToExecute);
		}
		return hasUncommittedChanges;
	}

	private String getFeedUrl() {
		// Not lazy initializing this in case the seamless session expires
		String viewType = ConstantStrings.EMPTY_STRING;
		if (getSugarEntry().getSugarType().equals(SugarType.OPPORTUNITIES)) {
			viewType = "OpportunityWall"; //$NON-NLS-1$
		} else if (getSugarEntry().getSugarType().equals(SugarType.ACCOUNTS)) {
			viewType = "ClientWall"; //$NON-NLS-1$
		}
		return NotesAccountManager.getInstance().getCRMServer() + "index.php?action=sugarisp&view=" + viewType + "&record=" //$NON-NLS-1$ //$NON-NLS-2$
				+ getSugarEntry().getId() + "&MSID=" + SugarWebservicesOperations.getInstance().getSessionId(true); //$NON-NLS-1$
	}

	@Override
	public String getDashboardName() {
		String name = ConstantStrings.EMPTY_STRING;
		if (getSugarEntry().getSugarType().equals(SugarType.OPPORTUNITIES)) {
			name = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OPPORTUNITY_FEEDS_TAB);
		} else if (getSugarEntry().getSugarType().equals(SugarType.ACCOUNTS)) {
			name = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CLIENT_FEEDS_TAB);
		}
		return name;
	}

	@Override
	public void rebuildComposite() {
		if (browser == null) {
			super.rebuildComposite();
		} else {
			browser.dispose();
			browser = null;
			// createBrowser();
			// mainComposite.layout(true);
			super.rebuildComposite();
		}
	}

	@Override
	public void selectedItemsChanged() {

	}
}
