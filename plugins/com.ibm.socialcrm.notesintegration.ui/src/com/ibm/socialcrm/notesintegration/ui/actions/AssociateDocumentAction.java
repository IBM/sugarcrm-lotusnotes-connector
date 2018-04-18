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

import java.util.Vector;

import lotus.domino.Document;
import lotus.domino.Item;

import com.ibm.notes.java.ui.NotesUIWorkspace;
import com.ibm.notes.java.ui.documents.NotesUIDocument;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.AbstractToolbarMenuItem;
import com.ibm.socialcrm.notesintegration.core.utils.UpdateSelectionsBroadcaster;
import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class AssociateDocumentAction extends AbstractToolbarMenuItem {
	public static final String IEN_ATTACH1 = "IENAttach1"; //$NON-NLS-1$
	public static final String IEN_CREATED_IN_MAIL = "IENCreatedInMail"; //$NON-NLS-1$
	public static final String PARENT_TYPE = "ParentType"; //$NON-NLS-1$
	public static final String IEN_TYPE = "IENType"; //$NON-NLS-1$
	public static final String PARENT_ID = "ParentID"; //$NON-NLS-1$
	public static final String IEN_DESCRIPTION = "IENDescription"; //$NON-NLS-1$
	public static final String IEN_DOCUMENT_UNIQUE_ID = "IENDocumentUniqueID"; //$NON-NLS-1$
	public static final String IEN_CONTACT_DOC_ID = "IENContactDocID"; //$NON-NLS-1$  
	public static final String IEN_CONTACT_F = "IENContact_F"; //$NON-NLS-1$
	public static final String IEN_CONTACT_L = "IENContact_L"; //$NON-NLS-1$
	public static final String ACCOUNT_NAME = "account_name"; //$NON-NLS-1$
	public static final String ASSIGNED_ID = "ASSIGNED_ID"; //$NON-NLS-1$

	public AssociateDocumentAction(BaseSugarEntry sugarEntry, String id) {
		super(sugarEntry, id);
	}

	@Override
	public String getItemText() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ASSOCIATE_DOC_TO_ITEM);
	}

	@Override
	public void onSelection() {
		// use the new connector. Temporarily commenting out the Sugar connector code.
		// It should be removed later.
		setupAssociate(getSugarEntry());
		// try
		// {
		// final NotesUIDocument currentDoc = getCurrentDocument();
		//
		// if (currentDoc != null)
		// {
		// NotesThread.sinitThread();
		// final Session session = NotesFactory.createSession();
		// Database db = session.getDatabase(currentDoc.getDatabaseData().getServer(), currentDoc.getDatabaseData()
		// .getFilePath());
		// final Document doc = db.getDocumentByUNID(currentDoc.getDocumentData().getUnid());
		// //The stuff listed below are just magical steps given to use by Sugar to create the association
		// final BaseSugarEntry entry = getSugarEntry();
		//
		//        setItemValue(doc, IEN_ATTACH1, "1"); //$NON-NLS-1$
		//        setItemValue(doc, IEN_CREATED_IN_MAIL, "Yes"); //$NON-NLS-1$
		// setItemValue(doc, PARENT_TYPE, entry.getSugarType().getParentType());
		// setItemValue(doc, IEN_TYPE, entry.getSugarType().getType());
		// setItemValue(doc, PARENT_ID, entry.getId());
		// setItemValue(doc, ASSIGNED_ID, SugarWebservicesOperations.getInstance().getUserCNUM());
		//
		// if (entry.getSugarType() == SugarType.CONTACTS)
		// {
		// setItemValue(doc, AssociateDocumentAction.IEN_CONTACT_DOC_ID, entry.getId());
		// setItemValue(doc, AssociateDocumentAction.IEN_DESCRIPTION, entry.getName()
		//              + "--->" + ((SugarContact)entry).getAccountName()); //$NON-NLS-1$      
		// }
		// else
		// {
		// setItemValue(doc, AssociateDocumentAction.IEN_DESCRIPTION, entry.getName());
		// }
		// setItemValue(doc, IEN_DOCUMENT_UNIQUE_ID, entry.getId());
		// doc.save();
		//
		// NotesUIWorkspace workspace = new NotesUIWorkspace();
		//        NotesAgentData data = new NotesAgentData(new NotesDatabaseData(db), "Sugar_LinkRecord"); //$NON-NLS-1$
		//
		// workspace.runAgent(data, new NotesDocumentDataCallback()
		// {
		// @Override
		// public void done(final NotesDocumentDataEvent evt)
		// {
		// if (evt.getError() != null)
		// {
		// Display.getDefault().syncExec(new Runnable()
		// {
		// @Override
		// public void run()
		// {
		// MessageDialog.openError(Display.getDefault().getShells()[0], UtilsPlugin.getDefault()
		// .getResourceString(UtilsPluginNLSKeys.CONNECTION_ERROR), UtilsPlugin.getDefault()
		// .getResourceString(UtilsPluginNLSKeys.SUGAR_CONNECTOR_FAILURE, new String[] { entry.getName() }));
		//
		// UtilsPlugin.getDefault().logException(evt.getError(), UiPluginActivator.PLUGIN_ID);
		// }
		// });
		// }
		// }
		// },
		// true);
		// session.recycle();
		// NotesThread.stermThread();
		// }
		// }
		// catch (Exception e)
		// {
		// UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		// }
	}

	private boolean setItemValue(Document doc, String key, String value) {
		boolean set = false;
		Item item = getItemByKey(doc, key);
		if (item != null) {
			try {
				item.setValueString(value);
				set = true;
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
			}
		}
		return set;
	}

	private Item getItemByKey(Document doc, String name) {
		Item item = null;
		try {
			Vector<Item> items = doc.getItems();
			for (Item anItem : items) {
				if (anItem.getName().equals(name)) {
					item = anItem;
					break;
				}
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}
		return item;
	}

	@Override
	public boolean shouldEnable() {
		boolean enable = false;
		NotesUIDocument currentDoc = getCurrentDocument();
		if (currentDoc != null) {
			enable = true;
		}
		return enable;
	}

	private NotesUIDocument getCurrentDocument() {
		NotesUIWorkspace workspace = new NotesUIWorkspace();
		return workspace.getCurrentDocument();
	}

	private static void setupAssociate(BaseSugarEntry sugarEntry) {
		UpdateSelectionsBroadcaster.getInstance().updateConnector(sugarEntry);

	}
}
