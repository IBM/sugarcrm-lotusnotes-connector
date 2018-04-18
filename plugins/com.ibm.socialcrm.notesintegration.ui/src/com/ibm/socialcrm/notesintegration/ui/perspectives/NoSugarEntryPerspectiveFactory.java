package com.ibm.socialcrm.notesintegration.ui.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.ibm.socialcrm.notesintegration.ui.views.NoSugarEntryViewPart;

public class NoSugarEntryPerspectiveFactory implements IPerspectiveFactory {

	public static final String PERSPECTIVE_ID = "com.ibm.socialcrm.notesintegration.ui.noSugarEntryPerspective"; //$NON-NLS-1$
	
	@Override
	public void createInitialLayout(IPageLayout layout) {
		IFolderLayout folder = layout.createFolder("bottom", IPageLayout.BOTTOM, IPageLayout.RATIO_MAX, IPageLayout.ID_EDITOR_AREA);//$NON-NLS-1$
		
		folder.addView(NoSugarEntryViewPart.VIEW_ID);
		layout.getViewLayout(NoSugarEntryViewPart.VIEW_ID).setCloseable(false);
		layout.getViewLayout(NoSugarEntryViewPart.VIEW_ID).setMoveable(false);
		layout.setEditorAreaVisible(false);
		layout.setFixed(true);
	}
}
