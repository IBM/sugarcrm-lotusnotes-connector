package com.ibm.socialcrm.notesintegration.ui.perspectives;

import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class ContactPerspectiveFactory extends SFAPerspectiveFactory {

	public static final String ID = "com.ibm.socialcrm.notesintegration.ui.contactPerspective"; //$NON-NLS-1$
	
	@Override
	public SugarType getType() {
         return SugarType.CONTACTS;		
	}

}
