package com.ibm.socialcrm.notesintegration.sametime.widgets;

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
import com.ibm.socialcrm.notesintegration.core.extensionpoints.ISametimeWidgetBuilder;

public class SametimeWidgetsBuilder implements ISametimeWidgetBuilder {

	@Override
	public boolean createClickToCallComposite(Composite composite, String phone, String labelText) {
		return new ClickToCallCompositeBuilder().createClickToCallComposite(composite, phone, labelText);
	}

	@Override
	public boolean createClickToCallComposite(Composite composite, String phone, String labelText, int labelWidth) {
		return new ClickToCallCompositeBuilder().createClickToCallComposite(composite, phone, labelText, labelWidth);
	}

	@Override
	public boolean createSametimeLinkComposite(Composite composite, BaseSugarEntry baseSugarEntry) {
		return new SametimeLinkCompositeBuilder().createSametimeLinkComposite(composite, baseSugarEntry);
	}

	public boolean createSametimeLinkComposite(Composite composite, String name, String email) {
		return new SametimeLinkCompositeBuilder().createSametimeLinkComposite(composite, name, email);
	}

}
