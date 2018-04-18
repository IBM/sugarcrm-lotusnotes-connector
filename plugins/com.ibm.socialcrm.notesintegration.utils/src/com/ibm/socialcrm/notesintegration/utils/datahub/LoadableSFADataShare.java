package com.ibm.socialcrm.notesintegration.utils.datahub;

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

/**
 * Adds the ability of a datashare to be loaded from an external source.
 * 
 */
public class LoadableSFADataShare<KEY_TYPE extends Object, VALUE_TYPE extends Object, LOAD_TYPE extends Object> extends SFADataShare {
	/**
	 * Indicates if the data share has been loaded
	 */
	private boolean loaded = false;

	public LoadableSFADataShare(String name) {
		super(name);
	}

	/**
	 * Initiates the load of a data share from a given object.
	 * 
	 * @param initialObject
	 * @return
	 */
	public final boolean loadDataShare(LOAD_TYPE initialObject) {
		setLoaded(false);
		boolean success = doLoad(initialObject);
		setLoaded(true);
		return success;
	}

	/**
	 * Loads a data share from the given object. Subclasses should override if they wish to provide an actual implementation.
	 * 
	 * @param initialObject
	 * @return success of the operation
	 */
	protected boolean doLoad(LOAD_TYPE initialObject) {
		// Subclasses should override as necessary
		return true;
	}

	public boolean isLoaded() {
		return loaded;
	}

	private void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

}
