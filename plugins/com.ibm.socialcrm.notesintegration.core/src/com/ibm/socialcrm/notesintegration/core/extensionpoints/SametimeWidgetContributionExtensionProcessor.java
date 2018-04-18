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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;

public class SametimeWidgetContributionExtensionProcessor {
	private static final String EXTENSION_POINT_ID = "com.ibm.socialcrm.notesintegration.core.sametimeWidgetContribution"; //$NON-NLS-1$ 
	private static final String WIDGET_CONTRIBUTION = "widgetContribution"; //$NON-NLS-1$
	private static final String BUILDER_CLASS = "builderClass"; //$NON-NLS-1$

	/**
	 * A handle to eclipse's extension registry
	 */
	private IExtensionRegistry extensionRegistry;

	/**
	 * A handle the the editorContribution extension point
	 */
	private IExtensionPoint point;

	private static SametimeWidgetContributionExtensionProcessor processor;

	private SametimeWidgetContribution sametimeWidgetContribution;

	private SametimeWidgetContributionExtensionProcessor() {
		extensionRegistry = Platform.getExtensionRegistry();
		point = extensionRegistry.getExtensionPoint(EXTENSION_POINT_ID);
	}

	/**
	 * Returns an instance of SametimeWidgetContributionExtensionProcessor
	 * 
	 * @return
	 */
	public static SametimeWidgetContributionExtensionProcessor getInstance() {
		if (processor == null) {
			processor = new SametimeWidgetContributionExtensionProcessor();
			processor.load();
		}
		return processor;
	}

	private void load() {
		if (point != null) {
			IExtension[] extensions = point.getExtensions();
			IExtension extension;
			if (extensions != null && extensions.length > 0 && (extension = extensions[0]) != null) {
				IConfigurationElement[] elements = extension.getConfigurationElements();
				String id = extension.getUniqueIdentifier();
				for (IConfigurationElement element : elements) {
					if (element.getName().equals(WIDGET_CONTRIBUTION)) {
						if (id != null && !id.trim().equals("")) //$NON-NLS-1$
						{
							try {
								SametimeWidgetContribution contrib = new SametimeWidgetContribution();
								contrib.setBundle(Platform.getBundle(extension.getContributor().getName()));
								contrib.setBuilderClass(element.getAttribute(BUILDER_CLASS));
								sametimeWidgetContribution = contrib;
							} catch (Exception e) {
								UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
							}
						}
					}
				}
			}
		}
	}

	public SametimeWidgetContribution getSametimeWidgetContribution() {
		return sametimeWidgetContribution;
	}
}
