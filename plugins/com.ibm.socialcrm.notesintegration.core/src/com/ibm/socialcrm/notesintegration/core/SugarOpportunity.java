package com.ibm.socialcrm.notesintegration.core;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.socialcrm.notesintegration.core.utils.SugarDashboardPreference;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class SugarOpportunity extends BaseSugarEntry {
	private String accountName;
	private String accountID;
	private String description;
	private String totalRevenue;
	private String decisionDate;
	private String salesStage;
	private String assignedUserName;
	private String assignedUserEmail;
	private String assignedUserID;
	private String primaryContact;
	private String primaryContactID;
	private Map<String, String> industryMap;
	
	// 80623 - ci simplification
	private String indus_industry;
	
	private List<RevenueLineItem> revenueLineItems;

	public SugarOpportunity(String id, String name) {
		super(id, name);
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTotalRevenue() {
		return totalRevenue;
	}

	public void setTotalRevenue(String totalRevenue) {
		this.totalRevenue = totalRevenue;
	}

	public String getDecisionDate() {
		return SugarDashboardPreference.getInstance().getFormattedDate(decisionDate);
	}

	public void setDecisionDate(String decisionDate) {
		this.decisionDate = decisionDate;
	}

	@Override
	public String getSugarUrl() {
		// 75038 - basic sidecar url
		//String aUrl = NotesAccountManager.getInstance().getCRMServer() + "index.php?module=Opportunities&action=DetailView&record=" + getId(); //$NON-NLS-1$
		String aUrl = NotesAccountManager.getInstance().getCRMServer() + "index.php?Opportunities/" + getId(); //$NON-NLS-1$

		// 75038 - note that calling buildV10SeamlessSidecarURL() for sidecar module
		if (GenericUtils.isUseEmbeddedBrowserPreferenceSet())
			return aUrl;
		else
			return SugarWebservicesOperations.getInstance().buildV10SeamlessSidecarURL(aUrl);
	}

	@Override
	public SugarType getSugarType() {
		return SugarType.OPPORTUNITIES;
	}

	public void setAccountID(String accountID) {
		this.accountID = accountID;
	}

	public String getAccountID() {
		return accountID;
	}

	public void setSalesStage(String salesStage) {
		this.salesStage = salesStage;
	}

	public String getSalesStage() {
		return salesStage;
	}

	public void setAssignedUserName(String assignedUserName) {
		this.assignedUserName = assignedUserName;
	}

	public String getAssignedUserName() {
		return assignedUserName;
	}

	public void setAssignedUserEmail(String assignedUserEmail) {
		this.assignedUserEmail = assignedUserEmail;
	}

	public String getAssignedUserEmail() {
		return assignedUserEmail;
	}

	public String getAssignedUserID() {
		return assignedUserID;
	}

	public void setAssignedUserID(String assignedUserID) {
		this.assignedUserID = assignedUserID;
	}

	public List<RevenueLineItem> getRevenueLineItems() {
		if (revenueLineItems == null) {
			revenueLineItems = new ArrayList<RevenueLineItem>();
		}
		return revenueLineItems;
	}

	public String getPrimaryContact() {
		if (primaryContact == null) {
			primaryContact = ConstantStrings.EMPTY_STRING;
		}
		return primaryContact;
	}

	public void setPrimaryContact(String primaryContact) {
		this.primaryContact = primaryContact;
	}

	public String getPrimaryContactID() {
		if (primaryContactID == null) {
			primaryContactID = ConstantStrings.EMPTY_STRING;
		}
		return primaryContactID;
	}

	public void setPrimaryContactID(String primaryContactID) {
		this.primaryContactID = primaryContactID;
	}

	public Map<String, String> getIndustryMap() {
		if (industryMap == null) {
			industryMap = new HashMap<String, String>();
		}
		return industryMap;
	}

	public void setIndustryMap(Map<String, String> map) {
		this.industryMap = map;
	}

	public String getIndustryString() {
		String industryString = ConstantStrings.EMPTY_STRING;
		Collection<String> industries = getIndustryMap().values();
		if (null != industries) {
			if (industries.size() > 1) {
				industryString = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.MULTIPLE_INDUSTRIES);
			} else if (industries.size() == 1) {
				industryString = industries.iterator().next();
			}
		}

		return industryString;
	}
	public String getIndustryTooltip() {

		String industryTooltip = ConstantStrings.EMPTY_STRING;

		// 80623 - CI simplification, new field as key for industry lookup
		industryTooltip = getIndustryString();

		return industryTooltip;
	}

	public void setIndusIndustry(String indus_industry) {
		this.indus_industry = indus_industry;
	}

	public String getIndusIndustry() {
		return indus_industry;
	}

}
