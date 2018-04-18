package com.ibm.socialcrm.notesintegration.utils.datahub.calllog;

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

import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataShare;

public class CurrentSugarEntryDataShare extends SFADataShare {
	// There should only be one instance of this share. This is the preferred name.
	public static final String SHARE_NAME = "currentCallLogSugarEntry"; //$NON-NLS-1$

	public static final String CURRENT_SUGAR_ENTRY = "currentSugarEntry"; //$NON-NLS-1$

	public CurrentSugarEntryDataShare() {
		super(SHARE_NAME);
	}

}
