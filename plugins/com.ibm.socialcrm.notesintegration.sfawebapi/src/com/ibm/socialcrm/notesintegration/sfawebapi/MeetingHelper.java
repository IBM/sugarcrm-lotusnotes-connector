package com.ibm.socialcrm.notesintegration.sfawebapi;

import java.util.ArrayList;
import java.util.List;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.utils.SugarV10APIManager;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations.GetInfo13RestulType;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class MeetingHelper implements Runnable {
	/**
	 * MeetingHelper is used to call the meeting creation hooks in the plugin from a separate thread so the servlet can return quickly to Sugar.
	 */
	private String module;
	private String[] relatedIds;

	public MeetingHelper(String module, String[] relatedIds) {
		this.module = module;
		this.relatedIds = relatedIds;
	}

	@Override
	public void run() {
		List<BaseSugarEntry> entries = new ArrayList<BaseSugarEntry>();
		try {
			// create meeting triggered from Account/Oppty/Contact has only single association
			if (module != null && (module.equalsIgnoreCase("Accounts") || module.equalsIgnoreCase("Contacts") || module.equalsIgnoreCase("Opportunities"))) {
				BaseSugarEntry entry = SugarWebservicesOperations.getInstance().getSugarEntryById(relatedIds[0]);
				if (entry == null) {

					SugarType sugarType = null;
					if (module.equals("Accounts")) { //$NON-NLS-1$
						sugarType = SugarType.ACCOUNTS;
					} else if (module.equals("Contacts")) { //$NON-NLS-1$
						sugarType = SugarType.CONTACTS;
					} else if (module.equals("Opportunities")) { //$NON-NLS-1$
						sugarType = SugarType.OPPORTUNITIES;
					}
					if (sugarType != null) {
						// Switch to new getinfo API, will retrieve only basecard information for now. If this is not enough, we will need to call
						// callSugargetInfo13 again with additional GetInfo13ResultType
						SugarWebservicesOperations.getInstance().callSugarGetInfo13(sugarType, relatedIds[0], GetInfo13RestulType.BASECARD);
					}
				}
				entry = SugarWebservicesOperations.getInstance().getSugarEntryById(relatedIds[0]);
				if (entry != null) {
					entries.add(entry);
				}
			} else // leads can have mass association
			{
				BaseSugarEntry entry = null;

				SugarType sugarType = null;
				if (module.equals("Leads")) { //$NON-NLS-1$
					// build search string
					entries = SugarV10APIManager.getInstance().getInfoFromWebservice("Leads", relatedIds);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// create the meeting
		if (entries != null && !entries.isEmpty()) {
			try {
				UiUtils.createMeeting(entries);
			} catch (Exception e) {
			}
		}

	}

}
