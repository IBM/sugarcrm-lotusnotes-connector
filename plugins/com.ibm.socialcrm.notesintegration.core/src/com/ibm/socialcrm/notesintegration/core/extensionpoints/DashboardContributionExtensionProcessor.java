package com.ibm.socialcrm.notesintegration.core.extensionpoints;

/****************************************************************
  * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.eclipse.core.runtime.ContributorFactoryOSGi;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.registry.ViewDescriptor;
import org.eclipse.ui.internal.registry.ViewRegistry;
import org.osgi.framework.Bundle;

import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class DashboardContributionExtensionProcessor {

	private static final String EXTENSION_POINT_ID = "com.ibm.socialcrm.notesintegration.core.sugarDashboardContribution"; //$NON-NLS-1$ 
	private static final String DASHBOARD_CONTRIBUTION = "dashboardContribution"; //$NON-NLS-1$

	private static final String WEIGHT = "weight"; //$NON-NLS-1$
	private static final String VIEW_CLASS = "viewClass"; //$NON-NLS-1$
	private static final String TYPES = "types"; //$NON-NLS-1$
	private static final String SUGAR_TYPE = "sugarType"; //$NON-NLS-1$
	private static final String DOCKED_DISPLAY_NAME = "dockedDisplayName"; //$NON-NLS-1$
	private static final String DISPLAY_NAME = "displayName"; //$NON-NLS-1$
	private static final String TYPE = "type"; //$NON-NLS-1$

	/**
	 * A handle to eclipse's extension registry
	 */
	private IExtensionRegistry extensionRegistry;

	/**
	 * A handle the the editorContribution extension point
	 */
	private IExtensionPoint point;

	private static DashboardContributionExtensionProcessor processor;

	private List<DashboardContribution> dashboardContributionList;

	private DashboardContributionExtensionProcessor() {
		extensionRegistry = Platform.getExtensionRegistry();
		point = extensionRegistry.getExtensionPoint(EXTENSION_POINT_ID);
	}

	/**
	 * Returns an instance of DashboardContributionExtensionProcessor
	 * 
	 * @return
	 */
	public static DashboardContributionExtensionProcessor getInstance() {
		if (processor == null) {
			processor = new DashboardContributionExtensionProcessor();
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
					if (element.getName().equals(DASHBOARD_CONTRIBUTION)) {
						if (id != null && !id.trim().equals("")) //$NON-NLS-1$
						{
							try {
								DashboardContribution contrib = new DashboardContribution();
								contrib.setId(id);
								contrib.setBundle(Platform.getBundle(extension.getContributor().getName()));
								contrib.setWeight(Integer.parseInt(element.getAttribute(WEIGHT)));
								contrib.setViewClass(element.getAttribute(VIEW_CLASS));
								contrib.setDockedDisplayName(element.getAttribute(DOCKED_DISPLAY_NAME));
								contrib.setDisplayName(element.getAttribute(DISPLAY_NAME));
								contrib.setViewPartId(id + "_viewPart"); //$NON-NLS-1$

								// This is a stupid ghetto hack. But for some reason, certain extensions to this extension
								// point don't get their translated data processed properly by eclipse. If we detect that this happens,
								// we'll just manually read in the property from the appropriate plugin.properties file and use that.
								// This just started happening randomly and I gave up on trying to figure out why.
								if (contrib.getDisplayName().startsWith("%")) {
									URL url = Platform.getBundle(extension.getContributor().getName()).getResource("plugin_" + Locale.getDefault() + ".properties"); //$NON-NLS-1$ //$NON-NLS-2$ 
									if (url == null) {
										url = Platform.getBundle(extension.getContributor().getName()).getResource("plugin.properties"); //$NON-NLS-1$
									}
									if (url != null) {
										String key = contrib.getDisplayName().substring(1);
										InputStream is = url.openStream();
										Properties props = new Properties();
										props.load(is);
										String displayName =(String)props.get(key);
										if (displayName != null)
										{
											contrib.setDisplayName(displayName);
										}
									}
								}
								IConfigurationElement typesElement = element.getChildren(TYPES)[0];
								IConfigurationElement[] types = typesElement.getChildren(TYPE);
								for (IConfigurationElement typeElement : types) {
									contrib.getApplicableTypes().add(SugarType.valueOf(typeElement.getAttribute(SUGAR_TYPE)));
								}

								// When we made switched from using custom built tabs to view parts in the card around the 1.3 timeframe
								// we had to create view parts for all of the existing AbstractDashboardComposites. It was easier to
								// create a wrapper around the old code than re-work the existing stuff. To support the new model, we'll
								// automatically create corresponding ViewParts for the existing dashboard contributions. They will be
								// given the id of <dashboard contrib id>_viewPart so that any code that knows the dashboard id can get the
								// view part id.

								String extensionXml = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" + "<?eclipse version=\"3.2\"?>\n" //$NON-NLS-1$//$NON-NLS-2$
										+ "<plugin>\n"; //$NON-NLS-1$
								extensionXml += "<extension  point=\"org.eclipse.ui.views\">" + //$NON-NLS-1$
										"          <view class=\"com.ibm.socialcrm.notesintegration.core.ui.views.SugarDashboardViewPart\" " + //$NON-NLS-1$
										"            id=\"" + contrib.getViewPartId() + "\"" + //$NON-NLS-1$  //$NON-NLS-2$
										"          name=\"" + contrib.getDisplayName() + "\" restorable=\"false\">" + //$NON-NLS-1$ //$NON-NLS-2$
										"           </view>" + //$NON-NLS-1$
										"       </extension>"; //$NON-NLS-1$
								extensionXml += "</plugin>\n"; //$NON-NLS-1$

								// Dynamically contribute this view part
								final IExtensionRegistry reg = Platform.getExtensionRegistry();
								InputStream in = new ByteArrayInputStream(extensionXml.getBytes("UTF-8")); //$NON-NLS-1$
								Bundle bundle = CorePluginActivator.getDefault().getBundle();
								IContributor contr = ContributorFactoryOSGi.createContributor(bundle);
								reg.addContribution(in, contr, false, null, null, null);

								IConfigurationElement[] allConfigElements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.ui.views"); //$NON-NLS-1$
								for (IConfigurationElement configElement : allConfigElements) {
									if (contrib.getViewPartId().equals(configElement.getAttribute("id"))) //$NON-NLS-1$
									{
										ViewDescriptor descriptor = new ViewDescriptor(configElement);
										((ViewRegistry) PlatformUI.getWorkbench().getViewRegistry()).add(descriptor);
									}
								}

								internalGetDashboardContributionSet().add(contrib);
							} catch (Exception e) {
								UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
							}
						}
					}
				}
			}
		}
	}

	private List<DashboardContribution> internalGetDashboardContributionSet() {
		if (dashboardContributionList == null) {
			dashboardContributionList = new ArrayList<DashboardContribution>();
		}
		return dashboardContributionList;
	}

	public List<DashboardContribution> getDashboardContributionList() {
		// This will be a small list, so it's not too expensive to do this considering how infrequently it gets called
		Collections.sort(internalGetDashboardContributionSet());
		return Collections.unmodifiableList(internalGetDashboardContributionSet());
	}

	public DashboardContribution getDashboardContributionById(String id) {
		DashboardContribution contrib = null;
		for (DashboardContribution aContrib : internalGetDashboardContributionSet()) {
			if (aContrib.getId().equals(id)) {
				contrib = aContrib;
				break;
			}
		}
		return contrib;
	}

}
