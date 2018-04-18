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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public abstract class BaseSugarEntry {
	private String id;
	private String name;
	private String street;
	private String city;
	private String postalCode;
	private String state;
	private String country;
	private String officePhone;
	private String mobilePhone;
	private String email;
	private String website;
	private List<SugarContact> externalContacts;
	private List<String> tags;
	private Calendar timestamp;
	private boolean followed = false;
	private boolean emailSuppressed = false;
	private boolean officePhoneSuppressed = false;
	private boolean mobilePhoneSuppressed = false;

	public abstract String getSugarUrl();

	public abstract SugarType getSugarType();

	public BaseSugarEntry(String id, String name) {
		setId(id);
		setName(name);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getState() {
		return state;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getOfficePhone() {
		return officePhone;
	}

	public void setOfficePhone(String phone) {
		this.officePhone = phone;
	}

	public String getMobilePhone() {
		return mobilePhone;
	}

	public void setMobilePhone(String phone) {
		this.mobilePhone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public void setExternalContacts(List<SugarContact> externalContacts) {
		this.externalContacts = externalContacts;
	}

	public List<SugarContact> getExternalContacts() {
		if (externalContacts == null) {
			externalContacts = new ArrayList<SugarContact>();
		}
		return externalContacts;
	}

	public List<String> getTags() {
		if (tags == null) {
			tags = new ArrayList<String>();
		}
		return tags;
	}

	public Calendar getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Calendar timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isFollowed() {
		return followed;
	}

	public void setFollowed(boolean followed) {
		this.followed = followed;
	}

	public boolean isEmailSuppressed() {
		return emailSuppressed;
	}

	public void setEmailSuppressed(boolean emailSuppressed) {
		this.emailSuppressed = emailSuppressed;
	}

	public boolean isOfficePhoneSuppressed() {
		return officePhoneSuppressed;
	}

	public void setOfficePhoneSuppressed(boolean officePhoneSuppressed) {
		this.officePhoneSuppressed = officePhoneSuppressed;
	}

	public boolean isMobilePhoneSuppressed() {
		return mobilePhoneSuppressed;
	}

	public void setMobilePhoneSuppressed(boolean mobilePhoneSuppressed) {
		this.mobilePhoneSuppressed = mobilePhoneSuppressed;
	}

}
