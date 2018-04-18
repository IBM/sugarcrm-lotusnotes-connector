package com.ibm.socialcrm.notesintegration.ui.actions;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.ibm.rcp.content.IDocumentContent;
import com.ibm.workplace.noteswc.selection.NotesTextSelection;

public abstract class AbstractSugarAction implements IObjectActionDelegate {
	private String selectedText;

	public enum ActionSearchFilter {
		SEARCH_BY_EMAIL, SEARCH_BY_NAME;
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart part) {
	}

	@Override
	public void run(IAction arg0) {
		MailDocumentSelectionAction.setLastTextSelection(getSelectedText());
		doRun();
	}

	/**
	 * This method does the actual stuff associated with the action
	 */
	public abstract void doRun();

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		IDocumentContent doc = null;

		if (selection instanceof StructuredSelection) {
			Object obj = ((StructuredSelection) selection).getFirstElement();

			if (obj instanceof NotesTextSelection) {
				String text = ((NotesTextSelection) obj).getText();
				if (text != null && text.length() > 0) {
					this.selectedText = text;
				}
			} else {
				obj = ((StructuredSelection) selection).getFirstElement();
				if (obj instanceof IDocumentContent) {
					doc = (IDocumentContent) obj;
					this.selectedText = (String) doc.getProperties().get("contents"); //$NON-NLS-1$          
				}
			}
		}
	}

	public String getSelectedText() {
		return selectedText;
	}

}
