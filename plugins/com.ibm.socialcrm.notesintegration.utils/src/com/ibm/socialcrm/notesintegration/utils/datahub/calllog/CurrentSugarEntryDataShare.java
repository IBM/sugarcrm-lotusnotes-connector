package com.ibm.socialcrm.notesintegration.utils.datahub.calllog;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
