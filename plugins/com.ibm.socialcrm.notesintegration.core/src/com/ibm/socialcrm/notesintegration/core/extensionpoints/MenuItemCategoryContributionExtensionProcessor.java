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

public class MenuItemCategoryContributionExtensionProcessor {

	private static final String EXTENSION_POINT_ID = "com.ibm.socialcrm.notesintegration.core.menuItemCategories"; //$NON-NLS-1$ 
	private static final String OBJECT_CONTRIBUTION = "objectContribution"; //$NON-NLS-1$

	private static final String WEIGHT = "weight"; //$NON-NLS-1$

	/**
	 * A handle to eclipse's extension registry
	 */
	private IExtensionRegistry extensionRegistry;

	/**
	 * A handle the the editorContribution extension point
	 */
	private IExtensionPoint point;

	private static MenuItemCategoryContributionExtensionProcessor processor;

	private SortedSet<MenuItemCategoryContribution> menuItemCategoryContributionSet;

	private MenuItemCategoryContributionExtensionProcessor() {
		extensionRegistry = Platform.getExtensionRegistry();
		point = extensionRegistry.getExtensionPoint(EXTENSION_POINT_ID);
	}

	/**
	 * Returns an instance of MenuItemCategoryContributionExtensionProcessor
	 * 
	 * @return
	 */
	public static MenuItemCategoryContributionExtensionProcessor getInstance() {
		if (processor == null) {
			processor = new MenuItemCategoryContributionExtensionProcessor();
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
					if (element.getName().equals(OBJECT_CONTRIBUTION)) {
						if (id != null && !id.trim().equals(ConstantStrings.EMPTY_STRING)) {
							try {
								MenuItemCategoryContribution contrib = new MenuItemCategoryContribution();
								contrib.setId(id);
								contrib.setBundle(Platform.getBundle(extension.getContributor().getName()));
								contrib.setWeight(Integer.parseInt(element.getAttribute(WEIGHT)));

								internalGetCategorySet().add(contrib);
							} catch (Exception e) {
								UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
							}
						}
					}
				}
			}
		}
	}

	private SortedSet<MenuItemCategoryContribution> internalGetCategorySet() {
		if (menuItemCategoryContributionSet == null) {
			Comparator<MenuItemCategoryContribution> comparator = new Comparator<MenuItemCategoryContribution>() {
				@Override
				public int compare(MenuItemCategoryContribution contrib1, MenuItemCategoryContribution contrib2) {
					return contrib1.getWeight() - contrib2.getWeight();
				}
			};

			menuItemCategoryContributionSet = new TreeSet<MenuItemCategoryContribution>(comparator);

		}
		return menuItemCategoryContributionSet;
	}

	public SortedSet<MenuItemCategoryContribution> getMenuItemCategories() {
		return Collections.unmodifiableSortedSet(internalGetCategorySet());
	}

}
