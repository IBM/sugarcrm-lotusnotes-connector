package com.ibm.socialcrm.notesintegration.utils;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.rcp.ui.launcher.SwitcherService;

/**
 * The activator class controls the plug-in life cycle
 */
public class UtilsPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.ibm.socialcrm.notesintegration.utils"; //$NON-NLS-1$

	// The shared instance
	private static UtilsPlugin plugin;

	// The shared resource bundle.
	private static ResourceBundle resourceBundle;

	private BundleContext context;

	private SwitcherService ss;

	/**
	 * The constructor
	 */
	public UtilsPlugin() {
		if (plugin == null) {
			String bundle = "com.ibm.socialcrm.notesintegration.utils.UtilsPluginResources"; //$NON-NLS-1$
			try {
				resourceBundle = ResourceBundle.getBundle(bundle);
			} catch (MissingResourceException x) {
				resourceBundle = null;
			}
		}
	}

	/**
	 * Obtain an instance of the SwitcherService from the Expeditor platform
	 * 
	 * @return the ss
	 */
	public SwitcherService getSwitcherService() {
		if (ss == null) {
			ServiceTracker st;

			// retrieve a SwitcherService instance through
			// the ServiceTracker
			st = new ServiceTracker(context, "com.ibm.rcp.ui.launcher.SwitcherService", //$NON-NLS-1$
					null);
			st.open();
			ss = (SwitcherService) st.getService();
			st.close();
		}
		return ss;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		this.context = context;
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
	public static UtilsPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 * 
	 * @param path
	 *        the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	/**
	 * Method log. Convenience method for logging errors or information to the Workspace's persistent .log file.
	 * 
	 * @param status
	 */
	private void log(IStatus status) {
		getLog().log(status);
	}

	/**
	 * Method logErrorMessage. Convenience method for logging an error message to the Workspace's persistent .log file.
	 * 
	 * @param message
	 */
	public void logErrorMessage(String message, String pluginId) {
		log(new Status(IStatus.ERROR, pluginId, IStatus.ERROR, message, null));
	}

	/**
	 * Method logWarningMessage. Convenience method for logging a warning message to the Workspace's persistent .log file.
	 * 
	 * @param message
	 */
	public void logWarningMessage(String message, String pluginId) {
		log(new Status(IStatus.WARNING, pluginId, IStatus.WARNING, message, null));
	}

	/**
	 * Method logWarningMessage. Convenience method for logging a warning message to the Workspace's persistent .log file.
	 * 
	 * @param message
	 */
	public void logWarningMessage(String[] messages, String pluginId) {
		log(new Status(IStatus.WARNING, pluginId, IStatus.WARNING, stringUp(messages), null));
	}

	/**
	 * Method logInfoMessage. Convenience method for logging an informational message to the Workspace's persistent .log file.
	 * 
	 * @param message
	 */
	public void logInfoMessage(String message, String pluginId) {
		log(new Status(IStatus.INFO, pluginId, IStatus.INFO, message, null));
		// System.out.println(message);
	}

	private String stringUp(String[] messages) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (messages != null && messages.length > 0) {
			for (int i = 0; i < messages.length; i++) {
				sb.append(messages[i]);
			}
		}
		return sb.toString();
	}

	/**
	 * Method logInfoMessage. Convenience method for logging an informational message to the Workspace's persistent .log file.
	 * 
	 * @param message
	 */
	public void logInfoMessage(String[] messages, String pluginId) {
		log(new Status(IStatus.INFO, pluginId, IStatus.INFO, stringUp(messages), null));
	}

	/**
	 * Method logException. Convenience method for logging an exception and optional message to the Workspace's persistent .log file.
	 * 
	 * @param e
	 * @param title
	 * @param message
	 * @param pluginID
	 */
	public void logException(Throwable e, final String title, String message, String pluginID) {
		if (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
		}
		IStatus status = null;
		if (e instanceof CoreException) {
			status = ((CoreException) e).getStatus();
		} else {
			if (message == null)
				message = e.getMessage();
			if (message == null)
				message = e.toString();
			status = new Status(IStatus.ERROR, pluginID, IStatus.OK, message, e);
		}

		Writer writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		e.printStackTrace(printWriter);
		logErrorMessage(writer.toString(), pluginID);

		// TODO REMOVE AT SOME POINT
		e.printStackTrace();

		log(status);
	}

	public void logException(Throwable e, String pluginId) {
		logException(e, null, null, pluginId);
	}

	/**
	 * Returns the string from the plugin's resource bundle, or 'key' if not found.
	 */
	public String getResourceString(String key) {
		ResourceBundle bundle = getDefault().getResourceBundle();
		try {
			return bundle.getString(key);
		} catch (Exception e) {
			return "#####" + key + "#####"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Returns the formatted message for the given key in the resource bundle for the default locale.
	 * 
	 * @param key
	 *        the lookup key for resource string
	 * @param args
	 *        the message arguments
	 * @return the string
	 */
	public String getResourceString(String key, Object[] args) {
		return getResourceString(key, Locale.getDefault(), args);
	}

	/**
	 * Returns the formatted message for the given key in the resource bundle for the specified locale.
	 * 
	 * @param key
	 *        the lookup key for resource string
	 * @param args
	 *        the message arguments
	 * @return the string
	 */
	public String getResourceString(String key, Locale locale, Object[] args) {
		String msg = null;
		if (args == null || args.length == 0) {
			msg = getResourceString(key, locale);
		} else {
			msg = format(key, locale, args);
		}
		return msg;
	}

	/**
	 * Returns the string from the plugin's resource bundle, or <code>key</code> if not found
	 * 
	 * @param key
	 *        the lookup key for resource string
	 * @return the resource string
	 */
	public String getResourceString(String key, Locale locale) {
		if (locale == null) {
			locale = Locale.getDefault();
		}

		try {
			return getDefault().getResourceBundle().getString(key);
		} catch (Exception e) {
			if (locale.equals(Locale.getDefault())) {
				logException(e, null, null, PLUGIN_ID);
				return key;
			} else {
				return getResourceString(key, Locale.getDefault());
			}
		}
	}

	public String format(String key, Locale locale, Object[] args) {
		return MessageFormat.format(getResourceString(key, locale).replaceAll("''", "'").replaceAll("'", "''"), args); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

}
