package com.ibm.socialcrm.notesintegration.core;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import org.eclipse.swt.widgets.Shell;

/**
 * This interface name is kinda strange, but here's how it's used. Some of our AbstractToolbarMenuItems and other things that are defined in the core plugin may perform long running operations. As
 * such, they will need to display something in the UI to indicate that a long running operation is happening.
 * 
 * This interface defines two methods that can be called to create and remove a progress indicator from the UI. The UI classes will provide the implementation and the classes in core will just be
 * given a handle to the interface. This will keep the UI and core plugins decoupled while allowing stuff in core to show appropriate progress in the UI classes.
 */
public interface IProgressDisplayer {
	/**
	 * Creates a progress indicator with the specified message. Returns an id that corresponds to this progress indicator.
	 * 
	 * @param message
	 * @return
	 */
	public String createProgressIndicator(String message);

	/**
	 * Removes the progress indicator with the specified id.
	 * 
	 * @param id
	 */
	public void removeProgressIndicator(String id);

	/**
	 * Returns a handle to the shell
	 * 
	 * @return
	 */
	public Shell getShell();
}
