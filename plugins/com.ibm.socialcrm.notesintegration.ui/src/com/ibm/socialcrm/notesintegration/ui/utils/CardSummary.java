package com.ibm.socialcrm.notesintegration.ui.utils;

import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

/**
 * Object used as the data model for the recently viewed cards table.
 * 
 */
public class CardSummary implements Comparable {

	// acw
	public static final int IS_PINNED = 1;
	public static final int IS_NOT_PINNED = 0;
	
	private String name;
	private String description;
	private SugarType type;
	private String id;
	
	// field timestampMillis has 2 types of values:
	// - when updating an individual card summary, this field is updated to reflect current system time, so this card can be sorted as the most
	//   recent card
	// - when updating a list of card summaries (for example: upload all the cards or drag-and-drop) which requires re-ordering list of cards,
	//   each card's timestampMillis is updated to be an one-up number (part of the reason for using an one-up number is because system timestamp might
	//   be the same in this kind of processing)
	private Long timestampMillis;
	private String email;
	private boolean emailSuppressed = false;
	private String mobilePhone;
	private boolean mobilePhoneSuppressed = false;
	private String officePhone;
	private boolean officePhoneSuppressed = false;
	
	// acw
	private int isPinned = 0;

	public CardSummary(String name, String description, SugarType type, String id) {
		setName(name);
		setDescription(description);
		setType(type);
		setId(id);
		setPinned(0);
	}

	
	public Integer isPinned() {
		return Integer.valueOf(isPinned);
	}
	public void setPinned(int i) {
		this.isPinned = i;
	}
	public void setPinned(boolean b) {
		if (b) {
			this.isPinned= IS_PINNED;
		} else {
			this.isPinned = IS_NOT_PINNED;
		}
	}
	
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public SugarType getType() {
		return type;
	}

	public void setType(SugarType type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return getName();
	}

	public String getFullText() {
		return getName() + "\n" + getDescription(); //$NON-NLS-1$
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof CardSummary) {
			return (((CardSummary) other).getId()).equals(getId());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	@Override
	public int compareTo(Object other) {
		if (other instanceof CardSummary) {
			return getId().compareTo(((CardSummary) other).getId());
		}
		return -1;
	}

	public Long getTimestampMillis() {
		if (timestampMillis == null) {
			timestampMillis = System.currentTimeMillis();
		}
		return timestampMillis;
	}

	public void setTimestampMillis(Long timestampMillis) {
		this.timestampMillis = timestampMillis;
	}

	public String getEmail() {
		if (email == null) {
			email = ConstantStrings.EMPTY_STRING;
		}
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isEmailSuppressed() {
		return emailSuppressed;
	}

	public void setEmailSuppressed(boolean emailSuppressed) {
		this.emailSuppressed = emailSuppressed;
	}

	public String getMobilePhone() {
		if (mobilePhone == null) {
			mobilePhone = ConstantStrings.EMPTY_STRING;
		}
		return mobilePhone;
	}

	public void setMobilePhone(String mobilePhone) {
		this.mobilePhone = mobilePhone;
	}

	public boolean isMobilePhoneSuppressed() {
		return mobilePhoneSuppressed;
	}

	public void setMobilePhoneSuppressed(boolean mobilePhoneSuppressed) {
		this.mobilePhoneSuppressed = mobilePhoneSuppressed;
	}

	public String getOfficePhone() {
		if (officePhone == null) {
			officePhone = ConstantStrings.EMPTY_STRING;
		}
		return officePhone;
	}

	public void setOfficePhone(String officePhone) {
		this.officePhone = officePhone;
	}

	public boolean isOfficePhoneSuppressed() {
		return officePhoneSuppressed;
	}

	public void setOfficePhoneSuppressed(boolean officePhoneSuppressed) {
		this.officePhoneSuppressed = officePhoneSuppressed;
	}

}
