package com.ibm.socialcrm.notesintegration.ui.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;

import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class AccountPerspectiveFactory extends SFAPerspectiveFactory {

	public static final String ID = "com.ibm.socialcrm.notesintegration.ui.accountPerspective"; //$NON-NLS-1$
	
	@Override
	public SugarType getType() {
		return SugarType.ACCOUNTS;
	}

	@Override
	public void createInitialLayout(IPageLayout layout) {
		super.createInitialLayout(layout);

		IFolderLayout folder = layout.createFolder("bottom", IPageLayout.BOTTOM, IPageLayout.RATIO_MAX, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$

		String ACTIVITY_STREAM_VIEW_ID = "com.ibm.socialcrm.notesintegration.activitystream.ActivityStreamView"; //$NON-NLS-1$
		String MICROBLOG_VIEW_ID = "com.ibm.socialcrm.notesintegration.activitystream.MicroblogView"; //$NON-NLS-1$

		addView(ACTIVITY_STREAM_VIEW_ID, layout, folder);
		addView(MICROBLOG_VIEW_ID, layout, folder);
	}

}
