package com.ibm.socialcrm.notesintegration.core.utils;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
