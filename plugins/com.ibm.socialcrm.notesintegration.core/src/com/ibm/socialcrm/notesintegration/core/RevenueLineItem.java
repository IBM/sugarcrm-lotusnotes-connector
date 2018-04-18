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

import com.ibm.socialcrm.notesintegration.core.utils.SugarDashboardPreference;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;

public class RevenueLineItem {
	private String name;
	private String billDate;
	private String lastModifiedDate;
	private String amount;
	private OpportunityOwner owner;

	public RevenueLineItem() {
	}

	public String getName() {
		if (name == null) {
			name = ConstantStrings.EMPTY_STRING;
		}
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBillDate() {
		if (billDate == null) {
			billDate = ConstantStrings.EMPTY_STRING;
		}
		return SugarDashboardPreference.getInstance().getFormattedDate(billDate);
	}

	public void setBillDate(String date) {
		this.billDate = date;
	}

	public String getLastModifiedDate() {
		return SugarDashboardPreference.getInstance().getFormattedDate(lastModifiedDate);
	}

	public void setLastModifiedDate(String lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

	public String getAmount() {
		if (amount == null) {
			amount = ConstantStrings.EMPTY_STRING;
		}
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public OpportunityOwner getOwner() {
		if (owner == null) {
			owner = new OpportunityOwner();
		}
		return owner;
	}

	public void setOwner(OpportunityOwner owner) {
		this.owner = owner;
	}

}
