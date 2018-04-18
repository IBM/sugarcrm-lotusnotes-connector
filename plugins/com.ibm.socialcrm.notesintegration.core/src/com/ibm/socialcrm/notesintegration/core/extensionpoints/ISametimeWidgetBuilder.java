package com.ibm.socialcrm.notesintegration.core.extensionpoints;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import org.eclipse.swt.widgets.Composite;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;

public interface ISametimeWidgetBuilder {
	public boolean createClickToCallComposite(Composite composite, final String phone, String labelText);

	public boolean createClickToCallComposite(Composite composite, final String phone, String labelText, int labelWidth);

	public boolean createSametimeLinkComposite(Composite composite, BaseSugarEntry baseSugarEntry);

	public boolean createSametimeLinkComposite(Composite composite, String name, String email);
}
