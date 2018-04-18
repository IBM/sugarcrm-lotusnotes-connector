package com.ibm.socialcrm.notesintegration.ui;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.util.Dictionary;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.ibm.pvc.webcontainer.listeners.HttpSettingListener;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;

public class UiPluginActivator extends AbstractUIPlugin implements IStartup {
	// The plug-in ID
	public static final String PLUGIN_ID = "com.ibm.socialcrm.notesintegration.ui"; //$NON-NLS-1$

	// The shared instance
	private static UiPluginActivator plugin;

	private int port;
	private String hostname;

	private HttpSettingListener settingsListener = new HttpSettingListener() {
		public void settingsAdded(String pid, Dictionary properties) {
		}

		public void settingsModified(String pid, Dictionary properties) {
			String scheme = (String) properties.get(HttpSettingListener.SCHEME);
			if (HttpSettingListener.SCHEME_HTTP.equals(scheme)) {
				Object iPort = properties.get(HttpSettingListener.HTTP_PORT);
				if (iPort instanceof Integer[]) {
					port = ((Integer[]) iPort)[0].intValue();
				} else {
					port = ((Integer) iPort).intValue();
				}
				String sHost = (String) properties.get(HttpSettingListener.ADDRESS);
				if (HttpSettingListener.ALL_ADDRESSES.equals(sHost)) {
					hostname = "localhost"; //$NON-NLS-1$
				} else {
					hostname = sHost;
				}
			}
		}

		public void settingsRemoved(String pid) {
		}
	};

	/**
	 * The constructor
	 */
	public UiPluginActivator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		// In case this operation blows up, we still want to execute the rest of the code.
		try {
			super.start(context);
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}

		plugin = this;

		// Start the expeditor web container if it's not already running
		Bundle b = Platform.getBundle("com.ibm.pvc.webcontainer"); //$NON-NLS-1$
		if (b != null && b.getState() != Bundle.UNINSTALLED && b.getState() != Bundle.ACTIVE) {
			b.start();
		}

		context.registerService(HttpSettingListener.class.getName(), settingsListener, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static UiPluginActivator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	/**
	 * Returns the port that the expeditor web container is running on
	 * 
	 * @return
	 */
	public int getHttpPort() {
		return port;
	}

	/**
	 * Returns the hostname of this machine
	 * 
	 * @return
	 */
	public String getHttpHostname() {
		return hostname;
	}

	@Override
	public void earlyStartup() {
		// Used to restore the sidebar views here. Now, it doesn't do anything, but we'll leave the hook here
	}

}
