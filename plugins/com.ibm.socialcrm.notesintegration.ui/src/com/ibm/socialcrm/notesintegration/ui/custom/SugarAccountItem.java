package com.ibm.socialcrm.notesintegration.ui.custom;

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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.ibm.socialcrm.notesintegration.core.SugarAccount;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;

public class SugarAccountItem extends SugarItem<SugarAccount> {

	public SugarAccountItem(SugarItemList itemList, SugarAccount sugarEntry) {
		super(itemList, sugarEntry);
	}

	@Override
	public void buildItem(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE | SWT.NO_FOCUS);
		composite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
		composite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText(getEntry().getName());
		nameLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).grab(true, false).create());
		nameLabel.setFont(SugarItemsDashboard.getInstance().getBoldFontForBusinessCardData());

		Label idLabel = new Label(composite, SWT.NONE);
		idLabel.setText(getEntry().getClientId());
		idLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).create());
		idLabel.setFont(SugarItemsDashboard.getInstance().getBoldFontForBusinessCardData());

		Label urlLabel = new Label(composite, SWT.NONE);
		urlLabel.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());
		if (getEntry().getWebsite() != null) {
			urlLabel.setText(getEntry().getWebsite());
		}

		UiUtils.recursiveSetBackgroundColor(composite, Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
	}

	@Override
	public String getAccessibleName() {
		return getEntry().getName() + " " + getEntry().getId() + " " + getEntry().getWebsite(); //$NON-NLS-1$ //$NON-NLS-2$ 
	}

}
