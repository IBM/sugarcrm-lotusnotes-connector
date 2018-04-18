package com.ibm.socialcrm.notesintegration.core.uipluginbridge;

import org.eclipse.swt.widgets.Composite;

import com.ibm.socialcrm.notesintegration.core.IProgressDisplayer;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.DashboardContribution;
import com.ibm.socialcrm.notesintegration.core.ui.views.ISugarDashboardViewPart;

/**
 * This is a strange interface. Here's why it exists. When we moved from the old cards where we manually build all of the tabs to the new perspective based construction, there were two ways we could
 * have gone about doing that.
 * 
 * 1) Retro fit all existing dashboard contributions with a complementary view part and bind those together somehow. 
 * 2) Just create a view automagically to host the existing dashboard composite.
 * 
 * I chose #2 since this allowed us to keep the same programming model and extension points for all and avoid the manual re-work for all existing tabs. The downside is that the views what were being
 * automatically contributed from the DashboardContributionProcessor had to reference a class that existed in the core plugin (since that's where the DashboardContributionProcessor resides). That
 * created some dependencies between the Core and UI plugin that would have been circular (and moving a bunch of UI classes into core would've been a pain). So, I created this bridge interface to
 * allow the core plugin to operate call methods on AbstractDashboardComposite without having to know what an AbstractDashboardComposite really is.
 */
public interface IDashboardComposite {

	public void setDashboardContribution(DashboardContribution contribution);

	public void setLayoutData(Object data);

	public void selectedItemsChanged();

	public void setProgressDisplayer(IProgressDisplayer progressDisplayer);

	public String getDashboardName();

	public boolean hasUncommittedChanges();

	public void rebuildComposite();

	public void layout(boolean refresh);

	public void setParentViewPart(ISugarDashboardViewPart parentView);
	
	public Composite getProgressComposite();
}
