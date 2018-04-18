package com.ibm.socialcrm.notesintegration.ui.actions;

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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.program.Program;

import com.ibm.rcp.swt.swidgets.SToolBar;
import com.ibm.rcp.swt.swidgets.SToolItem;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.AbstractToolbarIconContributionAction;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.ToolbarIconContribution;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class EmailDashboardToolbarAction extends AbstractToolbarIconContributionAction {

	public EmailDashboardToolbarAction(BaseSugarEntry sugarEntry, ToolbarIconContribution toolbarIconContribution) {
		super(sugarEntry, toolbarIconContribution);
	}

	@Override
	public void build(SToolBar toolbar) {
		if (hasBuildableParts()) {
			SToolItem emailButton = new SToolItem(toolbar, SWT.PUSH);
			emailButton.setImage(getEnabledIconImage());
			if (getDisabledIconImage() != null) {
				emailButton.setDisabledImage(getDisabledIconImage());
			}

			if (getEmailAddress() != null) {
				emailButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent arg0) {
						UiUtils.createEmail(getEmailAddress());
					}
				});
				emailButton.setToolTipText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_SEND_EMAIL));
			} else {
				emailButton.setEnabled(false);
			}
		}
	}

	@Override
	public boolean hasBuildableParts() {
		return getEmailAddress() != null || getDisabledIconImage() != null;
	}

	private String getEmailAddress() {
		String email = null;
		if (getSugarEntry() instanceof SugarContact) {
			SugarContact sugarContact = (SugarContact) getSugarEntry();
			if (sugarContact.getEmail() != null && !sugarContact.getEmail().equals(ConstantStrings.EMPTY_STRING)) {
				email = sugarContact.getEmail();
			}
		}
		return email;
	}
}
