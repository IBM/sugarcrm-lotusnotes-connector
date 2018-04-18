package com.ibm.socialcrm.notesintegration.accountPanels.data;

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

/**
 * Just a bean object to store information about an account contact
 */
public class AccountContact {
	private String sugarId;
	private String firstName;
	private String lastName;
	private String email;
	private String officePhone;
	private String mobilePhone;
	private boolean emailSuppressed = false;

	public AccountContact() {
	}

	public String getSugarId() {
		return sugarId;
	}

	public void setSugarId(String sugarId) {
		this.sugarId = sugarId;
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getOfficePhone() {
		return officePhone;
	}

	public void setOfficePhone(String officePhone) {
		this.officePhone = officePhone;
	}

	public String getMobilePhone() {
		return mobilePhone;
	}

	public void setMobilePhone(String mobilePhone) {
		this.mobilePhone = mobilePhone;
	}

	public boolean isEmailSuppressed() {
		return emailSuppressed;
	}

	public void setEmailSuppressed(boolean emailSuppressed) {
		this.emailSuppressed = emailSuppressed;
	}

	@Override
	public String toString() {
		return firstName + " " + lastName; //$NON-NLS-1$
	}

}
