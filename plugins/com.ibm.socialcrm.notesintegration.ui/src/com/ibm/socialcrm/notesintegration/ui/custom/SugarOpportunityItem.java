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

import com.ibm.socialcrm.notesintegration.core.SugarOpportunity;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;

public class SugarOpportunityItem extends SugarItem<SugarOpportunity> {

	public SugarOpportunityItem(SugarItemList itemList, SugarOpportunity sugarEntry) {
		super(itemList, sugarEntry);
	}

	@Override
	public void buildItem(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE | SWT.NO_FOCUS);
		composite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
		composite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setText(getEntry().getName());
		nameLabel.setFont(SugarItemsDashboard.getInstance().getBoldFontForBusinessCardData());
		nameLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).grab(true, false).create());

		Label dateLabel = new Label(composite, SWT.NONE);
		dateLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).create());
		dateLabel.setFont(SugarItemsDashboard.getInstance().getBoldFontForBusinessCardData());
		dateLabel.setText(getEntry().getDecisionDate());

		Label moneyLabel = new Label(composite, SWT.NONE);
		moneyLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).grab(true, false).create());
		moneyLabel.setText(getEntry().getTotalRevenue());

		Label sellerLabel = new Label(composite, SWT.NONE);
		sellerLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.END, SWT.BEGINNING).create());
		sellerLabel.setText(getEntry().getAssignedUserName());

		UiUtils.recursiveSetBackgroundColor(composite, Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
	}

	@Override
	public String getAccessibleName() {
		return getEntry().getName() + " " + getEntry().getDecisionDate() + " " + getEntry().getTotalRevenue() + " " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ getEntry().getAssignedUserName();
	}

}
