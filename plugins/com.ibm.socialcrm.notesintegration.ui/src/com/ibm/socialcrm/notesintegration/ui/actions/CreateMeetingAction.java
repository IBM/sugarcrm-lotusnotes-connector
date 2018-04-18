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

import java.util.ArrayList;
import java.util.List;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.AbstractToolbarMenuItem;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class CreateMeetingAction extends AbstractToolbarMenuItem {

	public CreateMeetingAction(BaseSugarEntry sugarEntry, String id) {
		super(sugarEntry, id);
	}

	@Override
	public String getItemText() {
		String text = ConstantStrings.EMPTY_STRING;
		SugarType type = getSugarEntry().getSugarType();
		if (type == SugarType.ACCOUNTS || type == SugarType.CONTACTS) {
			text = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SCHEDULE_MEETING_WITH, new String[]{getSugarEntry().getName()});
		} else if (type == SugarType.OPPORTUNITIES) {
			text = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SCHEDULE_MEETING_ABOUT, new String[]{getSugarEntry().getName()});
		}
		return text;
	}

	@Override
	public void onSelection() {
		List<BaseSugarEntry> entries = new ArrayList<BaseSugarEntry>();
		entries.add(getSugarEntry());
		UiUtils.createMeeting(entries);
	}

	@Override
	public boolean shouldEnable() {
		return true;
	}
}
