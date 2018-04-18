package com.ibm.socialcrm.notesintegration.core.utils;

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

/**
 * This data share will house any information related to the webservice connection to sugar
 * 
 * @author bcbull
 * 
 */
public class WebServiceInfoDataShare extends SFADataShare {
	// There should only be one instance of this share. This is the preferred name.
	public static final String SHARE_NAME = "webServiceInfoData"; //$NON-NLS-1$

	public static final String USER_CNUM = "userCNUM"; //$NON-NLS-1$
	public static final String USER_FULL_NAME = "userFullName"; //$NON-NLS-1$

	private static WebServiceInfoDataShare instance;

	public  /* 56713 private */ WebServiceInfoDataShare() {
		super(SHARE_NAME);
	}

	public static WebServiceInfoDataShare getInstance() {
		if (instance == null) {
			instance = new WebServiceInfoDataShare();
		}
		return instance;
	}

}
