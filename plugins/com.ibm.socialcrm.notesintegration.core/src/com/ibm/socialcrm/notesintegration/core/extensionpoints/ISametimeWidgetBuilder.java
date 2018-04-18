package com.ibm.socialcrm.notesintegration.core.extensionpoints;

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

public interface ISametimeWidgetBuilder {
	public boolean createClickToCallComposite(Composite composite, final String phone, String labelText);

	public boolean createClickToCallComposite(Composite composite, final String phone, String labelText, int labelWidth);

	public boolean createSametimeLinkComposite(Composite composite, BaseSugarEntry baseSugarEntry);

	public boolean createSametimeLinkComposite(Composite composite, String name, String email);
}
