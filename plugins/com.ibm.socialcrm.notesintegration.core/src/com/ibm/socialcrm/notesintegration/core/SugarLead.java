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
import java.util.List;

import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class SugarLead extends BaseSugarEntry {
	private String accountName;
	private String accountID;
	private String jobTitle;
	private List<String> opportunityIDs;
	private String firstName;
	private String lastName;
	private int totalOpportunities;

	public SugarLead(String id, String name) {
		super(id, name);
	}

	public List<String> getOpportunityIDs() {
		if (opportunityIDs == null) {
			opportunityIDs = new ArrayList<String>();
		}
		return opportunityIDs;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
	}

	@Override
	public String getSugarUrl() {
		String aUrl = SugarWebservicesOperations.getInstance().buildV10LeadsSeamlessURL(getId()); //$NON-NLS-1$
		return aUrl;
	}

	@Override
	public SugarType getSugarType() {
		return SugarType.LEADS;
	}

	public void setAccountID(String accountID) {
		this.accountID = accountID;
	}

	public String getAccountID() {
		return accountID;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public int getTotalOpportunities() {
		return totalOpportunities;
	}

	public void setTotalOpportunities(int totalOpportunities) {
		this.totalOpportunities = totalOpportunities;
	}

}
