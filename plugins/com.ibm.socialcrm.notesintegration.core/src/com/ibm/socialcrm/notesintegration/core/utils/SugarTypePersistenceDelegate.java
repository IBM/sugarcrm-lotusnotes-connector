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

import java.beans.Encoder;
import java.beans.Expression;
import java.beans.PersistenceDelegate;

import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class SugarTypePersistenceDelegate extends PersistenceDelegate {

	protected boolean mutatesTo(Object oldInstance, Object newInstance) {
		return oldInstance == newInstance;
	}

	protected Expression instantiate(Object oldInstance, Encoder out) {
		SugarType e = (SugarType) oldInstance;
		return new Expression(e, e.getClass(), "valueOf", new Object[] { e.name() }); //$NON-NLS-1$
	}

}
