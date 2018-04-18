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

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.ibm.rcp.swt.swidgets.SToolBar;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;

public abstract class AbstractToolbarIconContributionAction {
	private BaseSugarEntry sugarEntry;
	private ToolbarIconContribution toolbarIconContribution;
	private Image enabledIconImage;
	private Image disabledIconImage;

	public AbstractToolbarIconContributionAction(BaseSugarEntry sugarEntry, ToolbarIconContribution toolbarIconContribution) {
		this.setSugarEntry(sugarEntry);
		this.setToolbarIconContribution(toolbarIconContribution);
	}

	public abstract void build(SToolBar parent);

	public abstract boolean hasBuildableParts();

	public Image getEnabledIconImage() {
		if (enabledIconImage == null) {
			String imageId = toolbarIconContribution.getId() + "_enabled"; //$NON-NLS-1$
			enabledIconImage = SFAImageManager.getImage(imageId);
			if (enabledIconImage == null && (toolbarIconContribution.getEnabledIcon() != null && !(toolbarIconContribution.getEnabledIcon().equals(ConstantStrings.EMPTY_STRING)))) {
				try {
					enabledIconImage = new Image(Display.getDefault(), toolbarIconContribution.getBundle().getResource(toolbarIconContribution.getEnabledIcon()).openStream());
					SFAImageManager.getImageRegistry().put(imageId, enabledIconImage);
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
			}
		}
		return enabledIconImage;
	}

	public Image getDisabledIconImage() {
		if (disabledIconImage == null) {
			String imageId = toolbarIconContribution.getId() + "_disabled"; //$NON-NLS-1$
			disabledIconImage = SFAImageManager.getImage(imageId);
			if (disabledIconImage == null && (toolbarIconContribution.getDisabledIcon() != null && !(toolbarIconContribution.getDisabledIcon().equals(ConstantStrings.EMPTY_STRING)))) {
				try {
					disabledIconImage = new Image(Display.getDefault(), toolbarIconContribution.getBundle().getResource(toolbarIconContribution.getDisabledIcon()).openStream());
					SFAImageManager.getImageRegistry().put(imageId, disabledIconImage);
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
				}
			}
		}
		return disabledIconImage;
	}

	public void setSugarEntry(BaseSugarEntry sugarEntry) {
		this.sugarEntry = sugarEntry;
	}

	public BaseSugarEntry getSugarEntry() {
		return sugarEntry;
	}

	public void setToolbarIconContribution(ToolbarIconContribution toolbarIconContribution) {
		this.toolbarIconContribution = toolbarIconContribution;
	}

	public ToolbarIconContribution getToolbarIconContribution() {
		return toolbarIconContribution;
	}
}
