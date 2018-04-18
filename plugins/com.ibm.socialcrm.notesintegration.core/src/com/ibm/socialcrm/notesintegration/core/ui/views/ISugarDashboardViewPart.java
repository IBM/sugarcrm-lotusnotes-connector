package com.ibm.socialcrm.notesintegration.core.ui.views;

import java.util.List;

import org.eclipse.swt.widgets.Composite;

import com.ibm.socialcrm.notesintegration.core.IProgressDisplayer;
import com.ibm.socialcrm.notesintegration.core.uipluginbridge.IDashboardComposite;

/**
 * This interface defines the behavior of any view that will be shown in a SC business card.
 * 
 */
public interface ISugarDashboardViewPart {

	public IDashboardComposite getDashboardComposite();

	/**
	 * Refreshes this card and all of the sibling cards
	 */
	public void refreshAll();

	/**
	 * Refreshes just this view part
	 */
	public void refreshSelf();

	/**
	 * Returns all the sibling view parts for this object (view parts that are part of the same dashboard - or business card)
	 * 
	 * @return
	 */
	public List<ISugarDashboardViewPart> getSiblings();

	/**
	 * Sets the progress displayer for this view part
	 * 
	 * @param progessDisplayer
	 */
	public void setProgessDisplayer(IProgressDisplayer progessDisplayer);

	/**
	 * Returns the progress bar composite for this view part
	 * 
	 * @return
	 */
	public Composite getProgressComposite();
}
