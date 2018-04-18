package com.ibm.socialcrm.notesintegration.activitystream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Composite;

import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;

/**
 * View class for the microblog
 */
public class MicroblogView extends ActivityStreamView {

	/*
	 * Override to get the microblog filter
	 */
	@Override
	protected String getFilterJSON() {
		final String[] filter = new String[1];
		Job job = new Job("Get microblog filter") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor arg0) {

				filter[0] = SugarWebservicesOperations.getInstance().callGetMicroblogFilter(getSugarEntry().getSugarType().getParentType(), getSugarEntry().getId());
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		try {
			job.join();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}
		return filter[0];

	}

	@Override
	public String getTitle() {
		return "Events";
	}

	/**
	 * Override to do nothing. This is where the "post" fields are added in the parent class (a bit stupid, but didn't want to create am AbstractASView class just for this).
	 */
	@Override
	protected void createAdditionalParts(Composite c) {
		// no-op
	}

}
