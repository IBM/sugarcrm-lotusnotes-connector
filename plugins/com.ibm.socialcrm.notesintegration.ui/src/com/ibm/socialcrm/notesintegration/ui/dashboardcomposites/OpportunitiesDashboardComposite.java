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

import java.lang.reflect.Constructor;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarOpportunity;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.ISametimeWidgetBuilder;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.SametimeWidgetContribution;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.SametimeWidgetContributionExtensionProcessor;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.ui.utils.WebserviceDataLoadProgressMonitor;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFAHyperlink;

public class OpportunitiesDashboardComposite extends AbstractInfoDashboardComposite {

	private static final String CLIENTLINK = "CLIENTLINK"; //$NON-NLS-1$
	private static final String CONTACTLINK = "CONTACTLINK"; //$NON-NLS-1$
	private static final String CONTACTTEXT = "CONTACTTEXT"; //$NON-NLS-1$
	private static final String SAMETIMECOMPOSITE = "SAMETIMECOMPOSITE"; //$NON-NLS-1$
	final public static String[] OPPTYlineLabels = {};

	public OpportunitiesDashboardComposite(Composite parent, int style, String dashboardID, BaseSugarEntry sugarEntry) {
		super(parent, style, dashboardID, sugarEntry);
	}

	@Override
	/*
	 * be sure to add those controls displaying values from web service to control map, (for example: updateControlMap(CLIENTLINK, clientLink); ) So we can update them in the
	 * updateInnerCompositeWithData() method after web services are done.
	 */
	public void updateInnerComposite(Composite innerComposite) {
		final SugarOpportunity sugarOpportunity = (SugarOpportunity) getSugarEntry();

		innerComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).margins(HEADER_MARGIN, HEADER_MARGIN).equalWidth(false).create());
		innerComposite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, true).create());

		// client
		Label clientLabel = new Label(innerComposite, SWT.NONE);
		clientLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CLIENT_LABEL));
		clientLabel.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
		clientLabel.setForeground(SugarItemsDashboard.getInstance().getBusinessCardFieldLabelColor());

		SFAHyperlink clientLink = new SFAHyperlink(innerComposite, SWT.NONE, true);
		clientLink.setText((sugarOpportunity == null || sugarOpportunity.getAccountName() == null) ? ConstantStrings.EMPTY_STRING : sugarOpportunity.getAccountName());
		clientLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
		clientLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
		clientLink.setLayoutData(GridDataFactory.fillDefaults().indent(0, GenericUtils.getPlatformHyperlinkVerticalIndent()).create());
		if (sugarOpportunity != null && sugarOpportunity.getAccountID() != null && sugarOpportunity.getAccountName() != null && !sugarOpportunity.getAccountID().equals(ConstantStrings.EMPTY_STRING)
				&& !sugarOpportunity.getAccountName().equals(ConstantStrings.EMPTY_STRING)) {
			clientLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					UiUtils.displaySugarItemById13(SugarType.ACCOUNTS, sugarOpportunity.getAccountID(), sugarOpportunity.getAccountName(), new WebserviceDataLoadProgressMonitor(
							OpportunitiesDashboardComposite.this, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOADING, new String[]{sugarOpportunity.getAccountName()})));
				}
			});
		}
		updateControlMap(CLIENTLINK, clientLink);

		createTextValueComposite(innerComposite, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OPPORTUNITY_LABEL), (sugarOpportunity == null || sugarOpportunity.getName() == null)
				? ConstantStrings.EMPTY_STRING
				: sugarOpportunity.getName(), 0);
		createTextValueComposite(innerComposite, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DESCRIPTION_LABEL),
				(sugarOpportunity == null || sugarOpportunity.getDescription() == null) ? ConstantStrings.EMPTY_STRING : sugarOpportunity.getDescription(), 0);
		createTextValueComposite(innerComposite, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.STAGE_LABEL), (sugarOpportunity == null || sugarOpportunity.getSalesStage() == null)
				? ConstantStrings.EMPTY_STRING
				: sugarOpportunity.getSalesStage(), 0);
		createTextValueComposite(innerComposite, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.AMOUNT_LABEL), (sugarOpportunity == null || sugarOpportunity.getTotalRevenue() == null)
				? ConstantStrings.EMPTY_STRING
				: sugarOpportunity.getTotalRevenue(), 0);
		createTextValueComposite(innerComposite, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DATE_LABEL), (sugarOpportunity == null || sugarOpportunity.getDecisionDate() == null)
				? ConstantStrings.EMPTY_STRING
				: sugarOpportunity.getDecisionDate(), 0);

		// Contact
		Label contactLabel = new Label(innerComposite, SWT.NONE);
		contactLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CONTACT_LABEL));
		contactLabel.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
		contactLabel.setForeground(SugarItemsDashboard.getInstance().getBusinessCardFieldLabelColor());

		SFAHyperlink contactLink = new SFAHyperlink(innerComposite, SWT.NONE, true);
		contactLink.setLayoutData(GridDataFactory.fillDefaults().indent(0, GenericUtils.getPlatformHyperlinkVerticalIndent()).create());
		contactLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
		contactLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
		// Leave contactLink text to blank... will fill it in in updateInnerCompositeWithData() when web services are done
		// retrieving data
		if (sugarOpportunity != null && sugarOpportunity.getPrimaryContactID() != null && sugarOpportunity.getPrimaryContact() != null
				&& !sugarOpportunity.getPrimaryContactID().equals(ConstantStrings.EMPTY_STRING) && !sugarOpportunity.getPrimaryContact().equals(ConstantStrings.EMPTY_STRING)) {
			contactLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					UiUtils.displaySugarItemById13(SugarType.CONTACTS, sugarOpportunity.getPrimaryContactID(), sugarOpportunity.getPrimaryContact(), new WebserviceDataLoadProgressMonitor(
							OpportunitiesDashboardComposite.this, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOADING, new String[]{sugarOpportunity.getPrimaryContact()})));
				}
			});
		}
		updateControlMap(CONTACTLINK, contactLink);

		Text contactText = new Text(innerComposite, SWT.NONE);
		contactText.setLayoutData(GridDataFactory.fillDefaults().create());
		contactText.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
		updateControlMap(CONTACTTEXT, contactText);
		updateContact(sugarOpportunity, contactLink, contactText);

		// Owner
		Label ownerLabel = new Label(innerComposite, SWT.NONE);
		ownerLabel.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
		ownerLabel.setForeground(SugarItemsDashboard.getInstance().getBusinessCardFieldLabelColor());
		ownerLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OWNER_LABEL));
		Composite sametimeComposite = new Composite(innerComposite, SWT.NONE);
		sametimeComposite.setLayout(GridLayoutFactory.fillDefaults().create());
		sametimeComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		createSametimeLink(sametimeComposite, sugarOpportunity);
		updateControlMap(SAMETIMECOMPOSITE, sametimeComposite);
	}

	private void updateContact(SugarOpportunity sugarOpportunity, SFAHyperlink contactLink, Text contactText) {
		if (sugarOpportunity != null && sugarOpportunity.getPrimaryContact() != null && sugarOpportunity.getPrimaryContact().trim().length() > 0) {
			contactLink.setText(sugarOpportunity == null ? ConstantStrings.EMPTY_STRING : sugarOpportunity.getPrimaryContact());
			((GridData) contactLink.getLayoutData()).exclude = false;
			contactLink.setVisible(true);
			((GridData) contactText.getLayoutData()).exclude = true;
			contactText.setVisible(false);
		} else
		// primary contact is space, likely this is Data Withheld scenario.
		{
			contactText.setText((sugarOpportunity != null && sugarOpportunity.getAccountID() == null) ? ConstantStrings.EMPTY_STRING : UtilsPlugin.getDefault().getResourceString(
					UtilsPluginNLSKeys.DATA_WITHHELD));
			((GridData) contactText.getLayoutData()).exclude = false;
			contactText.setVisible(true);
			((GridData) contactLink.getLayoutData()).exclude = true;
			contactLink.setVisible(false);
		}
	}
	@Override
	/*
	 * Update the inner composite when data were retrieved
	 */
	public void updateInnerCompositeWithData() {
		final SugarOpportunity sugarOpportunity = (SugarOpportunity) getSugarEntry();
		if (sugarOpportunity != null) {

			// printControlMap();

			if (getControl(CLIENTLINK) != null) {
				((SFAHyperlink) getControl(CLIENTLINK)).setText((sugarOpportunity == null || sugarOpportunity.getAccountName() == null) ? ConstantStrings.EMPTY_STRING : sugarOpportunity
						.getAccountName());
				if (sugarOpportunity != null && sugarOpportunity.getAccountID() != null && sugarOpportunity.getAccountName() != null) {
					((SFAHyperlink) getControl(CLIENTLINK)).addHyperlinkListener(new HyperlinkAdapter() {
						@Override
						public void linkActivated(HyperlinkEvent e) {
							UiUtils.displaySugarItemById13(SugarType.ACCOUNTS, sugarOpportunity.getAccountID(), sugarOpportunity.getAccountName(), new WebserviceDataLoadProgressMonitor(
									OpportunitiesDashboardComposite.this, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOADING, new String[]{sugarOpportunity.getAccountName()})));
						}
					});
				}
			}

			if (getControl(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OPPORTUNITY_LABEL)) != null) {
				((Label) getControl(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OPPORTUNITY_LABEL))).setText((sugarOpportunity == null || sugarOpportunity.getName() == null)
						? ConstantStrings.EMPTY_STRING
						: sugarOpportunity.getName());
			}

			if (getControl(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DESCRIPTION_LABEL)) != null) {
				((Label) getControl(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DESCRIPTION_LABEL))).setText((sugarOpportunity == null || sugarOpportunity.getDescription() == null)
						? ConstantStrings.EMPTY_STRING
						: sugarOpportunity.getDescription());
			}
			if (getControl(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.STAGE_LABEL)) != null) {
				((Label) getControl(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.STAGE_LABEL))).setText((sugarOpportunity == null || sugarOpportunity.getSalesStage() == null)
						? ConstantStrings.EMPTY_STRING
						: sugarOpportunity.getSalesStage());
			}
			if (getControl(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.AMOUNT_LABEL)) != null) {
				((Label) getControl(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.AMOUNT_LABEL))).setText((sugarOpportunity == null || sugarOpportunity.getTotalRevenue() == null)
						? ConstantStrings.EMPTY_STRING
						: sugarOpportunity.getTotalRevenue());
			}
			if (getControl(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DATE_LABEL)) != null) {
				((Label) getControl(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DATE_LABEL))).setText((sugarOpportunity == null || sugarOpportunity.getDecisionDate() == null)
						? ConstantStrings.EMPTY_STRING
						: sugarOpportunity.getDecisionDate());
			}

			if (getControl(CONTACTLINK) != null && getControl(CONTACTTEXT) != null) {
				if (getControl(CONTACTLINK) != null) {
					((SFAHyperlink) getControl(CONTACTLINK)).setText((sugarOpportunity == null || sugarOpportunity.getPrimaryContact() == null) ? ConstantStrings.EMPTY_STRING : sugarOpportunity
							.getPrimaryContact());
					if (sugarOpportunity != null && sugarOpportunity.getPrimaryContactID() != null && sugarOpportunity.getPrimaryContact() != null
							&& !sugarOpportunity.getPrimaryContactID().equals(ConstantStrings.EMPTY_STRING) && !sugarOpportunity.getPrimaryContact().equals(ConstantStrings.EMPTY_STRING)) {
						((SFAHyperlink) getControl(CONTACTLINK)).addHyperlinkListener(new HyperlinkAdapter() {
							@Override
							public void linkActivated(HyperlinkEvent e) {
								UiUtils.displaySugarItemById13(SugarType.CONTACTS, sugarOpportunity.getPrimaryContactID(), sugarOpportunity.getPrimaryContact(), new WebserviceDataLoadProgressMonitor(
										OpportunitiesDashboardComposite.this, UtilsPlugin.getDefault()
												.getResourceString(UtilsPluginNLSKeys.LOADING, new String[]{sugarOpportunity.getPrimaryContact()})));
							}
						});
					}

				}
				if (getControl(CONTACTTEXT) != null) {
					((Text) getControl(CONTACTTEXT)).setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DATA_WITHHELD));
				}

				updateContact(sugarOpportunity, (SFAHyperlink) getControl(CONTACTLINK), (Text) getControl(CONTACTTEXT));
			}

			// =============
			if (getControl(SAMETIMECOMPOSITE) != null) {

				Composite sametimeComposite = (Composite) getControl(SAMETIMECOMPOSITE);
				Control[] controls = sametimeComposite.getChildren();
				if (controls != null && controls.length > 0) {
					for (int i = 0; i < controls.length; i++) {
						controls[i].dispose();
					}
				}
				createSametimeLink(sametimeComposite, sugarOpportunity);
				UiUtils.recursiveSetBackgroundColor(sametimeComposite, innerComposite.getBackground());
				// sametimeComposite.layout();
			}

			// Done update opprotunities dashboard UI
			innerComposite.layout();
		}
	}
	private void createSametimeLink(Composite composite, SugarOpportunity sugarOpportunity) {

		boolean builtViaContribution = false;

		if (sugarOpportunity != null && sugarOpportunity.getAssignedUserName() != null) {
			SametimeWidgetContribution sametimeWidgetContribution = SametimeWidgetContributionExtensionProcessor.getInstance().getSametimeWidgetContribution();

			if (sametimeWidgetContribution != null) {
				try {
					Class builderClass = sametimeWidgetContribution.getBundle().loadClass(sametimeWidgetContribution.getBuilderClass());
					Constructor constructor = builderClass.getConstructor();
					ISametimeWidgetBuilder sametimeWidgetBuilder = (ISametimeWidgetBuilder) constructor.newInstance();
					builtViaContribution = sametimeWidgetBuilder.createSametimeLinkComposite(composite, sugarOpportunity);
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
				}
			}
		}
		if (!builtViaContribution) {
			// Just create a normal label since we have an assigned user name at least.
			Composite assignedUserComposite = new Composite(composite, SWT.NONE);
			assignedUserComposite.setLayout(GridLayoutFactory.fillDefaults().create());
			assignedUserComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
			Label assignedUserLabel = new Label(assignedUserComposite, SWT.NONE);
			assignedUserLabel.setText((sugarOpportunity == null || sugarOpportunity.getAssignedUserName() == null) ? ConstantStrings.EMPTY_STRING : sugarOpportunity.getAssignedUserName());
		}
		composite.layout(true);
	}

	@Override
	public String[] getFieldLabels() {
		return OPPTYlineLabels;
	}

	@Override
	public String getDashboardName() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_OPPORTUNITY);
	}
}
