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

import java.util.List;

import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

/**
 * This object represents a sugar object that is recognized in the selected email. It's kind of a less rich version of a BaseSugarEntry (i.e. SugarAccount, SugarOpportunity, or SugarContact). When we
 * do text recognition in an email, we know some of the information about the match (name and type) but we don't know all of the detailed information that is usually stored in the BaseSugarEntry
 * because that comes to us from a different web service call. So this is essentially a lightweight version of a BaseSugarEntry that is passed around the UI.
 * 
 * @author bcbull
 */
public class SugarEntrySurrogate implements Comparable<SugarEntrySurrogate> {
	private String id;
	private String favoriteId;
	private String name;
	private String tagId;
	private SugarType type;
	private String associateDataMapXML;
	private String[] attachmentNames;
	private String summaryText;
	
	private List<String> requiredInvitees;
	private List<String> optionalInvitees;

	// Do not remove. This constructor is needed for XMLEncoder/Decoder
	public SugarEntrySurrogate() {
	}

	/**
	 * Creates a basic SugarEntrySurrogate
	 * 
	 * @param name
	 */
	public SugarEntrySurrogate(String name, SugarType type) {
		setName(name);
		setType(type);
	}

	/**
	 * Creates a SugarEntrySurrogate that can specify if this is the item selected by the iExtension notes connector.
	 * 
	 * @param name
	 * @param isIExtensionSelection
	 */
	public SugarEntrySurrogate(String name, String tagid, SugarType type) {
		setName(name);
		setTagId(tagid);
		setType(type);
	}

	public SugarEntrySurrogate(String name, SugarType type, String associateDataXML, String[] attachmentNames) {
		setName(name);
		setAssociateDataMapXML(associateDataXML);
		setAttachmentNames(attachmentNames);
		setType(type);
	}

	
	public SugarEntrySurrogate(String name, SugarType type, String associateDataXML, String[] attachmentNames, List<String> requiredInvitees, List<String> optionalInvitees) {
		setName(name);
		setAssociateDataMapXML(associateDataXML);
		setAttachmentNames(attachmentNames);
		setType(type);
		setRequiredInvitees(requiredInvitees);
		setOptionalInvitees(optionalInvitees);
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

	public SugarType getType() {
		return type;
	}

	public void setType(SugarType type) {
		this.type = type;
	}

	public String getAssociateDataMapXML() {
		return associateDataMapXML;
	}

	public void setAssociateDataMapXML(String t) {
		associateDataMapXML = t;
	}

	public String[] getAttachmentNames() {
		return attachmentNames;
	}

	public void setAttachmentNames(String[] ss) {
		attachmentNames = ss;
	}

	public List<String> getRequiredInvitees() {
		return requiredInvitees;
	}
	
	public void setRequiredInvitees(List<String> l) {
		requiredInvitees = l;
	}
	
	public List<String> getOptionalInvitees() {
		return optionalInvitees;
	}
	
	
	public void setOptionalInvitees(List<String> l) {
		optionalInvitees = l;
	}
	
	@Override
	public String toString() {
		return getName();
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		boolean equals = false;
		if (other != null && other instanceof SugarEntrySurrogate) {
			equals = getName().equals(((SugarEntrySurrogate) other).getName());
		}
		return equals;
	}

	@Override
	public int compareTo(SugarEntrySurrogate other) {
		int value = 1;
		if (other != null) {
			value = getName().compareTo(other.getName());
		}
		return value;
	}

	public String getTagId() {
		return tagId;
	}

	public void setTagId(String tagId) {
		this.tagId = tagId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFavoriteId() {
		return favoriteId;
	}

	public void setFavoriteId(String favoriteId) {
		this.favoriteId = favoriteId;
	}

	public String getSummaryText() {
		return summaryText;
	}

	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
	}

}
