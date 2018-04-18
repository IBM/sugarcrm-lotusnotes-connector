package com.ibm.socialcrm.notesintegration.ui.custom;

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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;

public class SugarContactItem extends SugarItem<SugarContact> {

	public SugarContactItem(SugarItemList itemList, SugarContact sugarEntry) {
		super(itemList, sugarEntry);
	}

	@Override
	public void buildItem(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE | SWT.NO_FOCUS);
		composite.setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).create());
		composite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText(getEntry().getName());
		nameLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		nameLabel.setFont(SugarItemsDashboard.getInstance().getBoldFontForBusinessCardData());

		Label idLabel = new Label(composite, SWT.NONE);
		String separator = getEntry().getJobTitle().trim().length() > 0 ? ", " : ""; //$NON-NLS-1$  //$NON-NLS-2$
		idLabel.setText(getEntry().getJobTitle() + separator + getEntry().getAccountName());
		idLabel.setLayoutData(GridDataFactory.fillDefaults().indent(10, 0).create());

		UiUtils.recursiveSetBackgroundColor(composite, Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
	}

	@Override
	public String getAccessibleName() {
		SugarContact contact = getEntry();
		return contact.getName() + " " + contact.getJobTitle() + ", " + getEntry().getAccountName(); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
