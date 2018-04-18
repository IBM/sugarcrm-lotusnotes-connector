package com.ibm.socialcrm.notesintegration.ui.personalities;

import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import com.ibm.rcp.personality.framework.IWorkbenchWindowAdvisorFactory;

public class SFANonModalWindowAdvisorFactory implements IWorkbenchWindowAdvisorFactory {
	
	public static final String PERSONALITY_EXTENSION_ID = "com.ibm.socialcrm.notesintegration.ui.nonModalDialogPersonality"; //$NON-NLS-1$
	
	@Override
	public WorkbenchWindowAdvisor create(IWorkbenchWindowConfigurer configurer) {
		return new SFADialogWindowAdvisor(configurer);
	}


}
