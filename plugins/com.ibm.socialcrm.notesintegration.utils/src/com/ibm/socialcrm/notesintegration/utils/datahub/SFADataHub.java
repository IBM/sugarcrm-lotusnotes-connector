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

import java.util.HashMap;
import java.util.Map;

import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;

/**
 * The SFADataHub is essentially a glorified map. It's used as a data storage facility for components that need to share data in SFA. The data hub will contain multiple "data shares." A data share is
 * also nothing more than a glorified map. A data share should be created for each logical data space. What is a data space? Well, you can think of it as namespace for any related data. For example,
 * if 2 or more plugins need to pass around data call logging, they could create a data share with a known (and appropriate) namespace and use that data share to pass information back and forth.
 * 
 * @author bcbull
 */
public class SFADataHub {
	private static SFADataHub instance;

	private Map<String, SFADataShare> shareMap;

	private SFADataHub() {
	}

	public static SFADataHub getInstance() {
		if (instance == null) {
			instance = new SFADataHub();
		}
		return instance;
	}

	/**
	 * Creates a new DataShare with the specified name. If the specified name is already taken, this method will return null.
	 * 
	 * @param name
	 * @return A new DataShare object or null
	 */
	public boolean addDataShare(SFADataShare dataShare) {
		boolean added = false;
		if (dataShare != null && dataShare.getName().length() > 0) {
			if (!getShareMap().containsKey(dataShare.getName())) {
				getShareMap().put(dataShare.getName(), dataShare);
				added = true;
			}
		}
		return added;
	}

	/**
	 * Returns the datashare with the given name
	 * 
	 * @param name
	 * @return
	 */
	public SFADataShare getDataShare(String name) {
		SFADataShare share = null;
		if (name != null) {
			share = getShareMap().get(name);
		}
		return share;
	}

	/**
	 * Returns the datashare with the given name, but waits up to millisToWait if it isn't loaded yet
	 * 
	 * @param name
	 * @return
	 */
	public SFADataShare blockingGetDataShare(String name, int millisToWait) {
		SFADataShare share = null;
		long startTime = System.currentTimeMillis();
		long stopTime = 0L;
		if (name != null) {
			share = getShareMap().get(name);
			boolean ready = (share != null);
			if (ready && share instanceof LoadableSFADataShare) {
				ready &= ((LoadableSFADataShare) share).isLoaded();
			}
			while (!ready) {
				try {
					Thread.sleep(100);
					stopTime = System.currentTimeMillis();
					if (stopTime - startTime >= millisToWait) {
						break;
					}
					share = getShareMap().get(name);
					ready = (share != null);
					if (ready && share instanceof LoadableSFADataShare) {
						ready &= ((LoadableSFADataShare) share).isLoaded();
					}
				} catch (InterruptedException e) {
					UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
				}
			}
		}
		return share;
	}

	/**
	 * Removes the datashare with the given name
	 * 
	 * @param name
	 * @return - true/false depending on if the operation was successful.
	 */
	public boolean removeDataShare(String name) {
		boolean removed = false;
		if (name != null) {
			removed = (getShareMap().remove(name) != null);
		}
		return removed;
	}

	private Map<String, SFADataShare> getShareMap() {
		if (shareMap == null) {
			shareMap = new HashMap<String, SFADataShare>();
		}
		return shareMap;
	}

}
