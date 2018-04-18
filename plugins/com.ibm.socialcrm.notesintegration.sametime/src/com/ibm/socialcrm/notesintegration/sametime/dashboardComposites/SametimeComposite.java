package com.ibm.socialcrm.notesintegration.sametime.dashboardComposites;

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
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.sametime.widgets.SametimeViewer;
import com.ibm.socialcrm.notesintegration.ui.dashboardcomposites.AbstractDashboardComposite;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class SametimeComposite extends AbstractDashboardComposite {
	private SametimeViewer viewer;
	private Label nothingToShowLabel;

	public SametimeComposite(Composite parent, int style, String dashboardID, BaseSugarEntry sugarEntry) {
		super(parent, style, dashboardID, sugarEntry);
	}

	@Override
	public void createInnerComposite() {
		nothingToShowLabel = new Label(this, SWT.WRAP);
		nothingToShowLabel.setLayoutData(GridDataFactory.fillDefaults().indent(10, 0).grab(true, true).create());
		nothingToShowLabel.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));
		((GridData) nothingToShowLabel.getLayoutData()).exclude = true;

		viewer = new SametimeViewer(this, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION, this);
		viewer.getTree().setLayoutData(GridDataFactory.fillDefaults().grab(true, true).indent(0, 10).create());

		setBackground(JFaceColors.getBannerBackground(Display.getDefault()));
	}

	@Override
	public void selectedItemsChanged() {
	}

	public Label getNothingToShowLabel() {
		return nothingToShowLabel;
	}

	@Override
	public String getDashboardName() {
		String dashboardName = ConstantStrings.EMPTY_STRING;
		if (getSugarType().equals(SugarType.ACCOUNTS)) {
			dashboardName = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SAMETIME_CLIENT_TAB);
		} else if (getSugarType().equals(SugarType.OPPORTUNITIES)) {
			dashboardName = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SAMETIME_OPPORTUNITY_TAB);
		}
		return dashboardName;
	}

	@Override
	public void afterBaseCardDataRetrieved() {
		super.afterBaseCardDataRetrieved();
		viewer.handleSelectionChange(getSugarEntry());
	}
}
