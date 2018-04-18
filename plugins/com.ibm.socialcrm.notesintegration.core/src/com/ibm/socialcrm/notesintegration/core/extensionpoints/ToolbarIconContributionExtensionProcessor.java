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

import java.util.Collections;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class ToolbarIconContributionExtensionProcessor {
	private static final String EXTENSION_POINT_ID = "com.ibm.socialcrm.notesintegration.core.dashboardToolbarIconContribution"; //$NON-NLS-1$ 
	private static final String TOOLBAR_ICON_CONTRIBUTION = "toolbarIconContribution"; //$NON-NLS-1$

	private static final String WEIGHT = "weight"; //$NON-NLS-1$
	private static final String ACTION_CLASS = "actionClass"; //$NON-NLS-1$
	private static final String ENABLED_ICON = "enabledIcon"; //$NON-NLS-1$
	private static final String DISABLED_ICON = "disabledIcon"; //$NON-NLS-1$

	private static final String TYPES = "types"; //$NON-NLS-1$
	private static final String TYPE = "type"; //$NON-NLS-1$
	private static final String SUGAR_TYPE = "sugarType"; //$NON-NLS-1$

	private static final String DASHBOARDS = "dashboards"; //$NON-NLS-1$
	private static final String DASHBOARD = "dashboard"; //$NON-NLS-1$
	private static final String ID = "id"; //$NON-NLS-1$

	/**
	 * A handle to eclipse's extension registry
	 */
	private IExtensionRegistry extensionRegistry;

	/**
	 * A handle the toolbarIconContribution extension point
	 */
	private IExtensionPoint point;

	private static ToolbarIconContributionExtensionProcessor processor;

	private SortedSet<ToolbarIconContribution> toolbarIconContributionSet;

	private ToolbarIconContributionExtensionProcessor() {
		extensionRegistry = Platform.getExtensionRegistry();
		point = extensionRegistry.getExtensionPoint(EXTENSION_POINT_ID);
	}

	/**
	 * Returns an instance of ToolbarIconContributionExtensionProcessor
	 * 
	 * @return
	 */
	public static ToolbarIconContributionExtensionProcessor getInstance() {
		if (processor == null) {
			processor = new ToolbarIconContributionExtensionProcessor();
			processor.load();
		}
		return processor;
	}

	private void load() {
		if (point != null) {
			IExtension[] extensions = point.getExtensions();
			for (IExtension extension : extensions) {
				IConfigurationElement[] elements = extension.getConfigurationElements();
				String id = extension.getUniqueIdentifier();
				for (IConfigurationElement element : elements) {
					if (element.getName().equals(TOOLBAR_ICON_CONTRIBUTION)) {
						if (id != null && !id.trim().equals(ConstantStrings.EMPTY_STRING)) {
							try {
								ToolbarIconContribution contrib = new ToolbarIconContribution();
								contrib.setId(id);
								contrib.setBundle(Platform.getBundle(extension.getContributor().getName()));
								contrib.setWeight(Integer.parseInt(element.getAttribute(WEIGHT)));
								contrib.setActionClass(element.getAttribute(ACTION_CLASS));
								contrib.setEnabledIcon(element.getAttribute(ENABLED_ICON));
								contrib.setDisabledIcon(element.getAttribute(DISABLED_ICON));

								IConfigurationElement typesElement = element.getChildren(TYPES)[0];
								IConfigurationElement[] types = typesElement.getChildren(TYPE);
								for (IConfigurationElement typeElement : types) {
									contrib.getSugarTypes().add(SugarType.valueOf(typeElement.getAttribute(SUGAR_TYPE)));
								}

								IConfigurationElement dashboardsElement = element.getChildren(DASHBOARDS)[0];
								IConfigurationElement[] dashboards = dashboardsElement.getChildren(DASHBOARD);
								for (IConfigurationElement dashboardElement : dashboards) {
									contrib.getDashboardIds().add(dashboardElement.getAttribute(ID));
								}
								internalGetToolbarIconSet().add(contrib);
							} catch (Exception e) {
								UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
							}
						}
					}
				}
			}
		}
	}

	private SortedSet<ToolbarIconContribution> internalGetToolbarIconSet() {
		if (toolbarIconContributionSet == null) {
			Comparator<ToolbarIconContribution> comparator = new Comparator<ToolbarIconContribution>() {
				@Override
				public int compare(ToolbarIconContribution contrib1, ToolbarIconContribution contrib2) {
					return contrib1.getWeight() - contrib2.getWeight();
				}
			};

			toolbarIconContributionSet = new TreeSet<ToolbarIconContribution>(comparator);

		}
		return toolbarIconContributionSet;
	}

	public SortedSet<ToolbarIconContribution> getToolbarIconContributionSet() {
		return Collections.unmodifiableSortedSet(internalGetToolbarIconSet());
	}

}
