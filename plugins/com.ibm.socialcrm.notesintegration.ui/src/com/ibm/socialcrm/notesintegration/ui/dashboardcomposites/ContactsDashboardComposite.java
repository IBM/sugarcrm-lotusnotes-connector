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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.ui.utils.WebserviceDataLoadProgressMonitor;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFAHyperlink;

public class ContactsDashboardComposite extends AbstractInfoDashboardComposite {

	final public static String[] CONTACTlineLabels = {UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CLIENT_LABEL),
			UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ADDRESS_LABEL), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.MOBILE_PHONE_LABEL),
			UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OFFICE_PHONE_LABEL), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WORK_EMAIL_LABEL)};
	private static final String CLIENTCOMPOSITE = "CLIENTCOMPOSITE"; //$NON-NLS-1$
	private static final String MOBILEPHONECOMPOSITE = "MOBILEPHONECOMPOSITE"; //$NON-NLS-1$
	private static final String OFFICEPHONECOMPOSITE = "OFFICEPHONECOMPOSITE"; //$NON-NLS-1$

	public ContactsDashboardComposite(Composite parent, int style, String dashboardID, BaseSugarEntry sugarEntry) {
		super(parent, style, dashboardID, sugarEntry);
	}

	@Override
	public void updateInnerComposite(Composite innerComposite) {
		maxLabelWidth = -1;
		maxLabelWidth = getMaxLabelWidth();

		final SugarContact sugarContact = (SugarContact) getSugarEntry();

		innerComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).margins(HEADER_MARGIN, HEADER_MARGIN).spacing(0, 0).create());
		innerComposite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, true).create());

		// Client
		Composite clientComposite = new Composite(innerComposite, SWT.NONE);
		clientComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).create());
		clientComposite.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.BEGINNING).grab(true, false).create());
		createAccountLink(clientComposite, sugarContact, maxLabelWidth);
		updateControlMap(CLIENTCOMPOSITE, clientComposite);

		// Address
		Composite addressComposite = new Composite(innerComposite, SWT.NONE);
		addressComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).create());
		addressComposite.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.BEGINNING).grab(true, false).create());
		new Label(addressComposite, SWT.NONE); // Spacer labels
		new Label(addressComposite, SWT.NONE);
		createAddressComposite(addressComposite, sugarContact, maxLabelWidth);
		updateControlMap(ADDRESSCOMPOSITE, addressComposite);

		// Mobile phone
		Composite phoneComposite = new Composite(innerComposite, SWT.NONE);
		phoneComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).margins(0, 0).create());
		phoneComposite.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.BEGINNING).grab(true, false).create());
		new Label(phoneComposite, SWT.NONE); // Spacer labels
		new Label(phoneComposite, SWT.NONE);
		createClickToCallComposite(phoneComposite, (sugarContact == null || sugarContact.getMobilePhone() == null || sugarContact.getMobilePhone().equals(ConstantStrings.EMPTY_STRING))
				? ConstantStrings.EMPTY_STRING
				: sugarContact.getMobilePhone(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.MOBILE_PHONE_LABEL), maxLabelWidth, sugarContact.isMobilePhoneSuppressed());
		updateControlMap(MOBILEPHONECOMPOSITE, phoneComposite);

		// Office phone
		Composite officePhoneComposite = new Composite(innerComposite, SWT.NONE);
		officePhoneComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).margins(0, 0).create());
		officePhoneComposite.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.BEGINNING).grab(true, false).create());
		new Label(officePhoneComposite, SWT.NONE); // Spacer labels
		new Label(officePhoneComposite, SWT.NONE);
		createClickToCallComposite(officePhoneComposite, (sugarContact == null || sugarContact.getOfficePhone() == null || sugarContact.getOfficePhone().equals(ConstantStrings.EMPTY_STRING))
				? ConstantStrings.EMPTY_STRING
				: sugarContact.getOfficePhone(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OFFICE_PHONE_LABEL), maxLabelWidth, sugarContact.isOfficePhoneSuppressed());
		updateControlMap(OFFICEPHONECOMPOSITE, officePhoneComposite);

		// Email
		Composite emailComposite = new Composite(innerComposite, SWT.NONE);
		emailComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).margins(0, 0).create());
		emailComposite.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.BEGINNING).grab(true, false).create());
		new Label(emailComposite, SWT.NONE); // Spacer labels
		new Label(emailComposite, SWT.NONE);
		createEmailComposite(emailComposite, (sugarContact == null | sugarContact.getEmail() == null) ? ConstantStrings.EMPTY_STRING : sugarContact.getEmail(), UtilsPlugin.getDefault()
				.getResourceString(UtilsPluginNLSKeys.WORK_EMAIL_LABEL), maxLabelWidth, sugarContact.isEmailSuppressed());
		updateControlMap(AbstractInfoDashboardComposite.EMAILCOMPOSITE, emailComposite);

		innerComposite.layout(true);
		innerComposite.getParent().layout(true);
	}

	private void createAccountLink(Composite parent, final SugarContact contact, int maxLabelWidth) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CLIENT_LABEL));
		label.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
		label.setForeground(SugarItemsDashboard.getInstance().getBusinessCardFieldLabelColor());
		label.setLayoutData(GridDataFactory.fillDefaults().hint(maxLabelWidth == -1 ? SWT.DEFAULT : maxLabelWidth, SWT.DEFAULT).create());

		SFAHyperlink companyLink = new SFAHyperlink(parent, SWT.NONE, true);
		companyLink.setText((contact == null || contact.getAccountName() == null) ? ConstantStrings.EMPTY_STRING : contact.getAccountName());
		companyLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
		companyLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
		companyLink.setLayoutData(GridDataFactory.fillDefaults().indent(0, GenericUtils.getPlatformHyperlinkVerticalIndent()).create());
		companyLink.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));
		if (contact != null && contact.getAccountName() != null && !contact.getAccountName().equals(ConstantStrings.EMPTY_STRING)) {
			companyLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					UiUtils.displaySugarItemById13(SugarType.ACCOUNTS, contact.getAccountID(), contact.getAccountName(), new WebserviceDataLoadProgressMonitor(ContactsDashboardComposite.this,
							UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOADING, new String[]{contact.getAccountName()})));
				}
			});
		}

	}

	@Override
	/*
	 * Update the inner composite when data were retrieved
	 */
	public void updateInnerCompositeWithData() {
		final SugarContact contact = (SugarContact) getSugarEntry();
		if (contact != null) {

			if (getControl(CLIENTCOMPOSITE) != null) {
				Composite clientComposite = (Composite) getControl(CLIENTCOMPOSITE);
				Control[] controls = clientComposite.getChildren();
				if (controls != null && controls.length > 0) {
					for (int i = 0; i < controls.length; i++) {
						controls[i].dispose();
					}
				}
				createAccountLink(clientComposite, contact, getMaxLabelWidth());
				clientComposite.layout();
				UiUtils.recursiveSetBackgroundColor(clientComposite, innerComposite.getBackground());
			}

			// address
			if (getControl(AbstractInfoDashboardComposite.ADDRESSCOMPOSITE) != null) {
				Composite addressComposite = (Composite) getControl(AbstractInfoDashboardComposite.ADDRESSCOMPOSITE);
				Control[] controls = addressComposite.getChildren();
				if (controls != null && controls.length > 0) {
					for (int i = 0; i < controls.length; i++) {
						controls[i].dispose();
					}
				}
				new Label(addressComposite, SWT.NONE); // Spacer labels
				new Label(addressComposite, SWT.NONE);
				createAddressComposite(addressComposite, contact, getMaxLabelWidth());
				addressComposite.layout();
				UiUtils.recursiveSetBackgroundColor(addressComposite, innerComposite.getBackground());
			}

			// mobile phone
			if (getControl(MOBILEPHONECOMPOSITE) != null) {
				Composite phoneComposite = (Composite) getControl(MOBILEPHONECOMPOSITE);
				Control[] controls = phoneComposite.getChildren();
				if (controls != null && controls.length > 0) {
					for (int i = 0; i < controls.length; i++) {
						controls[i].dispose();
					}
				}
				if (getSugarEntry() != null && (contact.getMobilePhone() != null && !contact.getMobilePhone().equals(ConstantStrings.EMPTY_STRING))) {

					new Label(phoneComposite, SWT.NONE);
					new Label(phoneComposite, SWT.NONE);

					createClickToCallComposite(phoneComposite, (contact == null || contact.getMobilePhone() == null || contact.getMobilePhone().equals(ConstantStrings.EMPTY_STRING))
							? ConstantStrings.EMPTY_STRING
							: contact.getMobilePhone(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.MOBILE_PHONE_LABEL), getMaxLabelWidth(), contact.isMobilePhoneSuppressed());
					UiUtils.recursiveSetBackgroundColor(phoneComposite, innerComposite.getBackground());
				} else {
					((GridData) phoneComposite.getLayoutData()).exclude = true;
					phoneComposite.setVisible(false);
				}
				phoneComposite.layout();

			}

			// office phone
			if (getControl(OFFICEPHONECOMPOSITE) != null) {
				Composite phoneComposite = (Composite) getControl(OFFICEPHONECOMPOSITE);
				Control[] controls = phoneComposite.getChildren();
				if (controls != null && controls.length > 0) {
					for (int i = 0; i < controls.length; i++) {
						controls[i].dispose();
					}
				}
				if (getSugarEntry() != null && (contact.getOfficePhone() != null && !contact.getOfficePhone().equals(ConstantStrings.EMPTY_STRING))) {

					new Label(phoneComposite, SWT.NONE);
					new Label(phoneComposite, SWT.NONE);

					createClickToCallComposite(phoneComposite, (contact == null || contact.getOfficePhone() == null || contact.getOfficePhone().equals(ConstantStrings.EMPTY_STRING))
							? ConstantStrings.EMPTY_STRING
							: contact.getOfficePhone(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OFFICE_PHONE_LABEL), getMaxLabelWidth(), contact.isOfficePhoneSuppressed());
					UiUtils.recursiveSetBackgroundColor(phoneComposite, innerComposite.getBackground());
				} else {
					((GridData) phoneComposite.getLayoutData()).exclude = true;
					phoneComposite.setVisible(false);
				}
				phoneComposite.layout();
			}

			// email
			if (getControl(AbstractInfoDashboardComposite.EMAILCOMPOSITE) != null) {
				Composite emailComposite = (Composite) getControl(AbstractInfoDashboardComposite.EMAILCOMPOSITE);
				Control[] controls = emailComposite.getChildren();
				if (controls != null && controls.length > 0) {
					for (int i = 0; i < controls.length; i++) {
						controls[i].dispose();
					}
				}

				new Label(emailComposite, SWT.NONE); // Spacer labels
				new Label(emailComposite, SWT.NONE);
				createEmailComposite(emailComposite, (contact == null || contact.getEmail() == null) ? ConstantStrings.EMPTY_STRING : contact.getEmail(), UtilsPlugin.getDefault().getResourceString(
						UtilsPluginNLSKeys.WORK_EMAIL_LABEL), getMaxLabelWidth(), contact.isEmailSuppressed());
				emailComposite.layout();
			}

			UiUtils.recursiveSetBackgroundColor(innerComposite, innerComposite.getBackground());

			// Done update opprotunities dashboard UI
			innerComposite.layout();

		}
	}

	@Override
	public String[] getFieldLabels() {
		return CONTACTlineLabels;
	}

	@Override
	public String getDashboardName() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CONTACT);
	}
}
