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

import java.util.Comparator;

import com.ibm.socialcrm.notesintegration.core.utils.SugarDashboardPreference;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;

public class ActivityData implements Comparator<ActivityData>, Comparable<ActivityData> {

	// Default Date strng format from web services
	private static final String DEFAULT_DATE_STRING_PATTERN = "yyyy-MM-dd"; //$NON-NLS-1$

	String _subject = null;
	String _moduleName = null; /* could be "Meetings", "Emails", "Notes" or "Calls" */
	String _dateCreated = null;
	String _assignedUserName = null;
	String _assignedId = null;	
	String _sugarId = null;

	public ActivityData() {

	}

	public void setSugarId(String s) {
		_sugarId = s;
	}

	public String getSugarId() {
		return _sugarId;
	}

	public void setModuleName(String s) {
		_moduleName = s;
	}

	public String getModuleName() {
		return _moduleName;
	}

	public void setSubject(String s) {
		_subject = s;
	}

	public String getSubject() {
		return _subject;
	}

	public void setDateCreated(String s) {
		_dateCreated = s;
	}

	public String getDateCreated() {
		return SugarDashboardPreference.getInstance().getFormattedDate(_dateCreated);
	}

	public void setAssignedUserName(String s) {
		_assignedUserName = s;
	}

	public String getAssignedId() {
		return _assignedId;
	}

	public void setAssignedId(String s) {
		_assignedId = s;
	}

	public String getAssignedUserName() {
		return _assignedUserName;
	}

	@Override
	public int compare(ActivityData arg0, ActivityData arg1) {
		int compare = -1;

		if (arg0 != null && arg1 != null) {
			// descending order
			compare = arg1._dateCreated.compareToIgnoreCase(arg0._dateCreated);
		}
		return compare;
	}

	@Override
	public int compareTo(ActivityData arg0) {
		return compare(this, arg0);
	}

}
