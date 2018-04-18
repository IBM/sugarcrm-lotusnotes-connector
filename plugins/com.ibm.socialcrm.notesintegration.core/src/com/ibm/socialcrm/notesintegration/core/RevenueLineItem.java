package com.ibm.socialcrm.notesintegration.core;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
