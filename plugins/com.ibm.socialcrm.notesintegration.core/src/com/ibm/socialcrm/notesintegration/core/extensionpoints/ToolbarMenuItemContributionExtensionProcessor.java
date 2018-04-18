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
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
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

public class ToolbarMenuItemContributionExtensionProcessor {
	private static final String EXTENSION_POINT_ID = "com.ibm.socialcrm.notesintegration.core.dashboardToolbarMenuItemContribution"; //$NON-NLS-1$ 
	private static final String TOOLBAR_MENU_ITEM_CONTRIBUTION = "toolbarMenuItemContribution"; //$NON-NLS-1$

	private static final String WEIGHT = "weight"; //$NON-NLS-1$
	private static final String ACTION_CLASS = "actionClass"; //$NON-NLS-1$
	private static final String CATEGORY_ID = "categoryId"; //$NON-NLS-1$

	private static final String TYPES = "types"; //$NON-NLS-1$
	private static final String TYPE = "type"; //$NON-NLS-1$
	private static final String SUGAR_TYPE = "sugarType"; //$NON-NLS-1$

	private static final String DASHBOARDS = "dashboards"; //$NON-NLS-1$
	private static final String DASHBOARD = "dashboard"; //$NON-NLS-1$
	private static final String ID = "id"; //$NON-NLS-1$

	private static final String DOCKED_VISIBILITY = "dockedVisibility"; //$NON-NLS-1$
	private static final String DOCKED = "docked"; //$NON-NLS-1$
	private static final String UNDOCKED = "undocked"; //$NON-NLS-1$

	/**
	 * A handle to eclipse's extension registry
	 */
	private IExtensionRegistry extensionRegistry;

	/**
	 * A handle the toolbarMenuItemContribution extension point
	 */
	private IExtensionPoint point;

	private static ToolbarMenuItemContributionExtensionProcessor processor;

	private SortedMap<MenuItemCategoryContribution, SortedSet<ToolbarMenuItemContribution>> toolbarMenuItemContributionMap;

	private ToolbarMenuItemContributionExtensionProcessor() {
		extensionRegistry = Platform.getExtensionRegistry();
		point = extensionRegistry.getExtensionPoint(EXTENSION_POINT_ID);
	}

	/**
	 * Returns an instance of ToolbarMenuItemContributionExtensionProcessor
	 * 
	 * @return
	 */
	public static ToolbarMenuItemContributionExtensionProcessor getInstance() {
		if (processor == null) {
			processor = new ToolbarMenuItemContributionExtensionProcessor();
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
					if (element.getName().equals(TOOLBAR_MENU_ITEM_CONTRIBUTION)) {
						if (id != null && !id.trim().equals(ConstantStrings.EMPTY_STRING)) {
							try {
								ToolbarMenuItemContribution contrib = new ToolbarMenuItemContribution();
								contrib.setId(id);
								contrib.setWeight(Integer.parseInt(element.getAttribute(WEIGHT)));
								contrib.setActionClass(element.getAttribute(ACTION_CLASS));
								contrib.setCategoryId(element.getAttribute(CATEGORY_ID));
								contrib.setBundle(Platform.getBundle(extension.getContributor().getName()));

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
								internalGetToolbarMenuItemSetForCategory(getCategory(contrib.getCategoryId())).add(contrib);

								if (element.getChildren(DOCKED_VISIBILITY) != null && element.getChildren(DOCKED_VISIBILITY).length > 0) {
									IConfigurationElement visibilityElement = element.getChildren(DOCKED_VISIBILITY)[0];
									contrib.setVisibleWhenDocked(Boolean.valueOf(visibilityElement.getAttribute(DOCKED)));
									contrib.setVisibleWhenUndocked(Boolean.valueOf(visibilityElement.getAttribute(UNDOCKED)));
								}
							} catch (Exception e) {
								UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
							}
						}
					}
				}
			}
		}
	}

	private MenuItemCategoryContribution getCategory(String id) {
		MenuItemCategoryContribution menuItemCategory = null;
		for (MenuItemCategoryContribution category : MenuItemCategoryContributionExtensionProcessor.getInstance().getMenuItemCategories()) {
			if (category.getId().equals(id)) {
				menuItemCategory = category;
			}
		}
		return menuItemCategory;
	}

	private SortedMap<MenuItemCategoryContribution, SortedSet<ToolbarMenuItemContribution>> internalGetToolbarMenuItemMap() {
		if (toolbarMenuItemContributionMap == null) {
			Comparator<MenuItemCategoryContribution> categoriesComparator = new Comparator<MenuItemCategoryContribution>() {
				@Override
				public int compare(MenuItemCategoryContribution contrib1, MenuItemCategoryContribution contrib2) {
					return contrib1.getWeight() - contrib2.getWeight();
				}
			};

			toolbarMenuItemContributionMap = new TreeMap<MenuItemCategoryContribution, SortedSet<ToolbarMenuItemContribution>>(categoriesComparator);

		}
		return toolbarMenuItemContributionMap;
	}

	private SortedSet<ToolbarMenuItemContribution> internalGetToolbarMenuItemSetForCategory(MenuItemCategoryContribution category) {
		SortedSet<ToolbarMenuItemContribution> menuItems;
		if (getToolbarMenuItemContributionMap().containsKey(category)) {
			menuItems = internalGetToolbarMenuItemMap().get(category);
		} else {
			Comparator<ToolbarMenuItemContribution> itemsComparator = new Comparator<ToolbarMenuItemContribution>() {
				@Override
				public int compare(ToolbarMenuItemContribution contrib1, ToolbarMenuItemContribution contrib2) {
					int compare = contrib1.getWeight() - contrib2.getWeight();
					if (compare == 0) { // This is kind of arbitrary, but it breaks ties
						compare = contrib1.getId().compareTo(contrib2.getId());
					}
					return compare;
				}
			};
			menuItems = new TreeSet<ToolbarMenuItemContribution>(itemsComparator);
			internalGetToolbarMenuItemMap().put(category, menuItems);
		}
		return menuItems;
	}

	public SortedMap<MenuItemCategoryContribution, SortedSet<ToolbarMenuItemContribution>> getToolbarMenuItemContributionMap() {
		return Collections.unmodifiableSortedMap(internalGetToolbarMenuItemMap());
	}

}
