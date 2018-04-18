package com.ibm.socialcrm.notesintegration.ui.dashboardcomposites;

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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarAccount;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class AccountsDashboardComposite extends AbstractInfoDashboardComposite {

	final public static String[] ACCOUNTlineLabels = {UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ADDRESS_LABEL),
			UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OFFICE_PHONE_LABEL), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FAX_LABEL),
			UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_TAGS_LABEL)};

	private static final String OFFICEPHONECOMPOSITE = "OFFICEPHONECOMPOSITE"; //$NON-NLS-1$

	public AccountsDashboardComposite(Composite parent, int style, String dashboardID, BaseSugarEntry sugarEntry) {
		super(parent, style, dashboardID, sugarEntry);
	}

	@Override
	public void updateInnerComposite(Composite innerComposite) {
		maxLabelWidth = -1;

		SugarAccount account = (SugarAccount) getSugarEntry();

		innerComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).margins(HEADER_MARGIN, HEADER_MARGIN).spacing(0, 0).create());
		innerComposite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, true).create());

		// address
		Composite addresComposite = new Composite(innerComposite, SWT.NONE);
		addresComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).create());
		addresComposite.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.BEGINNING).grab(true, false).create());

		createAddressComposite(addresComposite, account, getMaxLabelWidth());
		updateControlMap(ADDRESSCOMPOSITE, addresComposite);

		// office phone
		Composite phoneComposite = new Composite(innerComposite, SWT.NONE);
		phoneComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).margins(0, 0).create());
		phoneComposite.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.BEGINNING).grab(true, false).create());
		new Label(phoneComposite, SWT.NONE); // Spacer labels
		new Label(phoneComposite, SWT.NONE);
		createClickToCallComposite(phoneComposite, (account == null || account.getOfficePhone() == null || account.getOfficePhone().equals(ConstantStrings.EMPTY_STRING))
				? ConstantStrings.EMPTY_STRING
				: account.getOfficePhone(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OFFICE_PHONE_LABEL), getMaxLabelWidth(), false);
		updateControlMap(OFFICEPHONECOMPOSITE, phoneComposite);

		// fax
		new Label(innerComposite, SWT.NONE); // Spacer labels
		new Label(innerComposite, SWT.NONE);
		createTextValueComposite(innerComposite, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FAX_LABEL), (account == null || account.getFax() == null)
				? ConstantStrings.EMPTY_STRING
				: account.getFax(), getMaxLabelWidth());

		// tags
		new Label(innerComposite, SWT.NONE); // Spacer labels
		new Label(innerComposite, SWT.NONE);
		String tags = ConstantStrings.EMPTY_STRING;
		int i = 0;
		for (String tag : account.getTags()) {
			tags += tag + (++i < account.getTags().size() ? ", " : ConstantStrings.EMPTY_STRING); //$NON-NLS-1$
		}
		createTextValueComposite(innerComposite, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_TAGS_LABEL), tags, getMaxLabelWidth());

	}

	@Override
	public String getDashboardName() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CLIENT);
	}

	@Override
	/*
	 * Update the inner composite when data were retrieved
	 */
	public void updateInnerCompositeWithData() {
		final SugarAccount sugarAccount = (SugarAccount) getSugarEntry();
		if (sugarAccount != null) {

			if (getControl(AbstractInfoDashboardComposite.ADDRESSCOMPOSITE) != null) {
				Composite addressComposite = (Composite) getControl(AbstractInfoDashboardComposite.ADDRESSCOMPOSITE);
				Control[] controls = addressComposite.getChildren();
				if (controls != null && controls.length > 0) {
					for (int i = 0; i < controls.length; i++) {
						controls[i].dispose();
					}
				}

				createAddressComposite(addressComposite, sugarAccount, getMaxLabelWidth());
				UiUtils.recursiveSetBackgroundColor(addressComposite, innerComposite.getBackground());

				UiUtils.recursiveSetBackgroundColor(addressComposite, innerComposite.getBackground());
			}

			if (getControl(OFFICEPHONECOMPOSITE) != null) {
				Composite phoneComposite = (Composite) getControl(OFFICEPHONECOMPOSITE);
				Control[] controls = phoneComposite.getChildren();
				if (controls != null && controls.length > 0) {
					for (int i = 0; i < controls.length; i++) {
						controls[i].dispose();
					}
				}
				if (getSugarEntry() != null && (sugarAccount.getOfficePhone() != null && !sugarAccount.getOfficePhone().equals(ConstantStrings.EMPTY_STRING))) {

					new Label(phoneComposite, SWT.NONE);
					new Label(phoneComposite, SWT.NONE);

					createClickToCallComposite(phoneComposite, (sugarAccount == null || sugarAccount.getOfficePhone() == null || sugarAccount.getOfficePhone().equals(ConstantStrings.EMPTY_STRING))
							? ConstantStrings.EMPTY_STRING
							: sugarAccount.getOfficePhone(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OFFICE_PHONE_LABEL), getMaxLabelWidth(), false);
					UiUtils.recursiveSetBackgroundColor(phoneComposite, innerComposite.getBackground());
				} else {
					((GridData) phoneComposite.getLayoutData()).exclude = true;
					phoneComposite.setVisible(false);
				}
			}

			if (getControl(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FAX_LABEL)) != null) {

				Label faxLabel = (Label) getControl(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FAX_LABEL));
				faxLabel.setText((sugarAccount == null || sugarAccount.getFax() == null) ? ConstantStrings.EMPTY_STRING : sugarAccount.getFax().trim());
			}

			if (getControl(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_TAGS_LABEL)) != null) {
				String tags = ConstantStrings.EMPTY_STRING;
				int i = 0;
				if (sugarAccount == null || sugarAccount.getTags() == null || sugarAccount.getTags().isEmpty()) {
				} else {
					for (String tag : sugarAccount.getTags()) {
						tags += tag + (++i < sugarAccount.getTags().size() ? ", " : ConstantStrings.EMPTY_STRING); //$NON-NLS-1$
					}
				}
				Label tagsLabel = (Label) getControl(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_TAGS_LABEL));
				tagsLabel.setText(tags);
			}

			// Done update opprotunities dashboard UI
			innerComposite.layout();
		}
	}

	@Override
	public String[] getFieldLabels() {
		return ACCOUNTlineLabels;
	}
}
