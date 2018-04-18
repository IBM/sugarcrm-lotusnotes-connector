package com.ibm.socialcrm.notesintegration.sametime.widgets;

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
