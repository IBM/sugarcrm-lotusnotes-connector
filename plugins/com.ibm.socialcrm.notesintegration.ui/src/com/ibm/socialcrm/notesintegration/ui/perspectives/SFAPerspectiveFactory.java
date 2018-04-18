package com.ibm.socialcrm.notesintegration.ui.perspectives;

import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.ibm.socialcrm.notesintegration.core.extensionpoints.DashboardContribution;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.DashboardContributionExtensionProcessor;
import com.ibm.socialcrm.notesintegration.ui.advisor.ISFAPerspectiveFactoryAdvisor;
import com.ibm.socialcrm.notesintegration.ui.extensionpoints.SFAPerspectiveFactoryAdvisorContribution;
import com.ibm.socialcrm.notesintegration.ui.extensionpoints.SFAPerspectiveFactoryAdvisorExtensionProcessor;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

/**
 * Defines the perspectives that will be used to define the tabs shown for all of the business cards.
 */
public abstract class SFAPerspectiveFactory implements IPerspectiveFactory {

	public static final String REFRESH_ACTION_ID = "com.ibm.socialcrm.notesintegration.ui.refreshCard"; //$NON-NLS-1$

	public abstract SugarType getType();

	@Override
	public void createInitialLayout(IPageLayout layout) {

		IFolderLayout folder = layout.createFolder("bottom", IPageLayout.BOTTOM, IPageLayout.RATIO_MAX, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$

		List<DashboardContribution> contributionSet = DashboardContributionExtensionProcessor.getInstance().getDashboardContributionList();
		for (DashboardContribution contrib : contributionSet) {
			if (contrib.getApplicableTypes().contains(getType())) {
				folder.addView(contrib.getViewPartId());
				layout.getViewLayout(contrib.getViewPartId()).setCloseable(false);
				layout.getViewLayout(contrib.getViewPartId()).setMoveable(false);
			}
		}

		layout.setEditorAreaVisible(false);
		layout.setFixed(true);
	}
	
	protected void addView(String viewId, IPageLayout layout, IFolderLayout folder) {
		List<SFAPerspectiveFactoryAdvisorContribution> advisorList = SFAPerspectiveFactoryAdvisorExtensionProcessor.getInstance().getAdvisorList();

		IConfigurationElement[] allConfigElements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.ui.views"); //$NON-NLS-1$
		for (IConfigurationElement configElement : allConfigElements) {
			if (configElement.getAttribute("id").equals(viewId)) { //$NON-NLS-1$		
				for (SFAPerspectiveFactoryAdvisorContribution contribution : advisorList) {
					if (contribution.getViewId().equals(viewId)) {
						ISFAPerspectiveFactoryAdvisor advisor = contribution.getAdvisor();
						if (advisor.isVisible()) {
							folder.addView(viewId);
							layout.getViewLayout(viewId).setCloseable(false);
							layout.getViewLayout(viewId).setMoveable(false);
						}
					}
				}
			}
		}
	}
}
