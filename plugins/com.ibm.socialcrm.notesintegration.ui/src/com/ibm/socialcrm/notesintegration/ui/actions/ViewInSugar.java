package com.ibm.socialcrm.notesintegration.ui.actions;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarAccount;
import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.core.SugarOpportunity;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.AbstractToolbarMenuItem;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class ViewInSugar extends AbstractToolbarMenuItem {
	private static final String OPEN_SUGAR_STRING = "openinsugar"; //$NON-NLS-1$
	private static final String VIEW_CLIENT_HIERARCHY_STRING = "viewclienthierarchy"; //$NON-NLS-1$
	private static final String VIEW_CLIENT_IBM_SPEND_STRING = "viewclientibmspend"; //$NON-NLS-1$
	private static final String VIEW_INSTALL_BASE_STRING = "viewinstallbase"; //$NON-NLS-1$
	private static final String VIEW_CONTACTS_STRING = "viewcontacts"; //$NON-NLS-1$
	private static final String VIEW_PREDICTIVE_BUYING_ANALYTICS_STRING = "viewpredictivebuyinganalytics"; //$NON-NLS-1$
	private static final String VIEW_CLIENT_TOUCH_POINTS_STRING = "viewclienttouchpoints"; //$NON-NLS-1$
	private static final String VIEW_COMPLAINTS_AND_PMRS_STRING = "viewcomplaintsandpmrs"; //$NON-NLS-1$
	private static final String VIEW_NEWS_STRING = "viewnews"; //$NON-NLS-1$
	private static final String VIEW_INTELLIGENCE_STRING = "viewintelligence"; //$NON-NLS-1$
	private static final String EDIT_CONTACT_STRING = "editcontact"; //$NON-NLS-1$

	public ViewInSugar(BaseSugarEntry sugarEntry, String id) {
		super(sugarEntry, id);
	}

	@Override
	public String getItemText() {
		String itemText = ConstantStrings.EMPTY_STRING;

		if (getId() != null) {
			if (getId().endsWith(OPEN_SUGAR_STRING)) {
				if (getSugarEntry() != null) {
					String viewContact = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_OPEN_CONTACT_IN_CRM_SYSTEM);
					String viewAccount = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_OPEN_CLIENT_IN_CRM_SYSTEM);
					String viewOpportunity = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_OPEN_OPPORTUNITY_IN_CRM_SYSTEM);

					itemText = getSugarEntry() instanceof SugarContact ? viewContact : getSugarEntry() instanceof SugarAccount ? viewAccount
							: getSugarEntry() instanceof SugarOpportunity ? viewOpportunity : ConstantStrings.EMPTY_STRING;
				}
			} else if (getId().endsWith(VIEW_CLIENT_HIERARCHY_STRING)) {
				itemText = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_VIEW_CLIENT_HIERARCHY);
			} else if (getId().endsWith(VIEW_CLIENT_IBM_SPEND_STRING)) {
				itemText = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CLIENT_IBM_SPEND);
			} else if (getId().endsWith(VIEW_INSTALL_BASE_STRING)) {
				itemText = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INSTALL_BASE);
			} else if (getId().endsWith(VIEW_CONTACTS_STRING)) {
				itemText = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_VIEW_CONTACTS);
			} else if (getId().endsWith(VIEW_PREDICTIVE_BUYING_ANALYTICS_STRING)) {
				itemText = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_PREDICTIVE_BUYING_ANALYTICS);
			} else if (getId().endsWith(VIEW_CLIENT_TOUCH_POINTS_STRING)) {
				itemText = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_VIEW_CLIENT_TOUCH_POINTS);
			} else if (getId().endsWith(VIEW_COMPLAINTS_AND_PMRS_STRING)) {
				itemText = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_COMPLAINTS_AND_PMRS);
			} else if (getId().endsWith(VIEW_NEWS_STRING)) {
				itemText = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_NEWS);
			} else if (getId().endsWith(VIEW_INTELLIGENCE_STRING)) {
				itemText = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INTELLIGENCE);
			} else if (getId().endsWith(EDIT_CONTACT_STRING)) {
				itemText = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_EDIT_CONTACT);
			}
		}
		return itemText;
	}

	@Override
	public void onSelection() {
		StringBuffer url = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (getId() != null && getSugarEntry() != null && getSugarEntry().getSugarUrl() != null) {
			if (getId().endsWith(OPEN_SUGAR_STRING)) {
				url.append(getSugarEntry().getSugarUrl());
			} else if (getId().endsWith(VIEW_CLIENT_HIERARCHY_STRING)) {
				url.append(getSugarEntry().getSugarUrl());
			} else if (getId().endsWith(VIEW_CLIENT_IBM_SPEND_STRING)) {
				url.append(getSugarEntry().getSugarUrl()).append("&tab=client_ibm_spend"); //$NON-NLS-1$
			} else if (getId().endsWith(VIEW_INSTALL_BASE_STRING)) {
				url.append(getSugarEntry().getSugarUrl()).append("&tab=install_base"); //$NON-NLS-1$
			} else if (getId().endsWith(VIEW_CONTACTS_STRING)) {
				url.append(getSugarEntry().getSugarUrl()).append("#contact"); //$NON-NLS-1$
			} else if (getId().endsWith(VIEW_PREDICTIVE_BUYING_ANALYTICS_STRING)) {
				url.append(getSugarEntry().getSugarUrl()).append("&tab=predictive_buying_analytics"); //$NON-NLS-1$
			} else if (getId().endsWith(VIEW_CLIENT_TOUCH_POINTS_STRING)) {
				url.append(getSugarEntry().getSugarUrl()).append("&tab=client_touch_points"); //$NON-NLS-1$
			} else if (getId().endsWith(VIEW_COMPLAINTS_AND_PMRS_STRING)) {
				url.append(getSugarEntry().getSugarUrl()).append("&tab=complaints_and_pmrs"); //$NON-NLS-1$
			} else if (getId().endsWith(VIEW_NEWS_STRING)) {
				url.append(getSugarEntry().getSugarUrl()).append("&tab=news"); //$NON-NLS-1$
			} else if (getId().endsWith(VIEW_INTELLIGENCE_STRING)) {
				url.append(getSugarEntry().getSugarUrl()).append("&tab=intelligence"); //$NON-NLS-1$
			} else if (getId().endsWith(EDIT_CONTACT_STRING)) {
				String urlX = url.append(getSugarEntry().getSugarUrl()).toString();
				urlX = urlX.replace("DetailView", "EditView"); //$NON-NLS-1$  //$NON-NLS-2$
				url = new StringBuffer(urlX);
			}
		}

		if (url != null) {
			GenericUtils.launchUrlInPreferredBrowser(url.toString(), true);
		}
	}

	@Override
	public boolean shouldEnable() {
		return true;
	}

}
