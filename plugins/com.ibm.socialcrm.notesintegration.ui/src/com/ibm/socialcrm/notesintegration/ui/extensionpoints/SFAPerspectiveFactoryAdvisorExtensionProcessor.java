package com.ibm.socialcrm.notesintegration.ui.extensionpoints;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.ui.advisor.ISFAPerspectiveFactoryAdvisor;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;

public class SFAPerspectiveFactoryAdvisorExtensionProcessor {

	private static final String EXTENSION_POINT_ID = "com.ibm.socialcrm.notesintegration.SFAPerspectiveFactoryAdvisor"; //$NON-NLS-1$ 

	private static final String ADVISOR_INFO = "advisorInfo"; //$NON-NLS-1$

	private static final String VIEW_PART_ID = "viewPartId"; //$NON-NLS-1$
	private static final String ADVISOR = "advisor"; //$NON-NLS-1$

	/**
	 * A handle to eclipse's extension registry
	 */
	private IExtensionRegistry extensionRegistry;

	/**
	 * A handle the the editorContribution extension point
	 */
	private IExtensionPoint point;

	private static SFAPerspectiveFactoryAdvisorExtensionProcessor processor;

	private List<SFAPerspectiveFactoryAdvisorContribution> contributionSet;

	private SFAPerspectiveFactoryAdvisorExtensionProcessor() {
		extensionRegistry = Platform.getExtensionRegistry();
		point = extensionRegistry.getExtensionPoint(EXTENSION_POINT_ID);
	}

	/**
	 * Returns an instance of SFAPerspectiveFactoryAdvisorContribution
	 * 
	 * @return
	 */
	public static SFAPerspectiveFactoryAdvisorExtensionProcessor getInstance() {		
		if (processor == null) {
			processor = new SFAPerspectiveFactoryAdvisorExtensionProcessor();
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
					if (element.getName().equals(ADVISOR_INFO)) {
						if (id != null && !id.trim().equals(ConstantStrings.EMPTY_STRING)) {
							try {
								SFAPerspectiveFactoryAdvisorContribution contrib = new SFAPerspectiveFactoryAdvisorContribution();
								contrib.setViewId(element.getAttribute(VIEW_PART_ID));
								String advisor = element.getAttribute(ADVISOR);

								Bundle bundle = Platform.getBundle(extension.getContributor().getName());

								Class viewClass = bundle.loadClass(advisor);
								Constructor constructor = viewClass.getConstructor();
								ISFAPerspectiveFactoryAdvisor advisorInstance = (ISFAPerspectiveFactoryAdvisor) constructor.newInstance();
								contrib.setAdvisor(advisorInstance);

								internalGetAdvisorList().add(contrib);
							} catch (Exception e) {
								UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
							}
						}
					}
				}
			}
		}
	}

	private List<SFAPerspectiveFactoryAdvisorContribution> internalGetAdvisorList() {
		if (contributionSet == null) {
			contributionSet = new ArrayList<SFAPerspectiveFactoryAdvisorContribution>();
		}
		return contributionSet;
	}

	public List<SFAPerspectiveFactoryAdvisorContribution> getAdvisorList()
	{
		return Collections.unmodifiableList(internalGetAdvisorList());
	}
}
