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

import java.util.HashMap;
import java.util.Map;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.NotesFactory;
import lotus.domino.NotesThread;
import lotus.domino.Session;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Display;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.AbstractToolbarMenuItem;
import com.ibm.socialcrm.notesintegration.core.utils.SugarDashboardPreference;
import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.ui.dialogs.OverwriteContactDialog;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class DownloadContactInfo extends AbstractToolbarMenuItem {
	private static final String SUGAR_UUID = "sugarUUID"; //$NON-NLS-1$

	public DownloadContactInfo(BaseSugarEntry sugarEntry, String id) {
		super(sugarEntry, id);
	}

	@Override
	public String getItemText() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ADD_TO_CONTACTS);
	}

	@Override
	public void onSelection() {
		upsertContact((SugarContact) getSugarEntry());
	}

	@Override
	public boolean shouldEnable() {
		return true;
	}

	/**
	 * Adds or updates a contact to names.nsf
	 * 
	 * @param contact
	 */
	public void upsertContact(final SugarContact contact) {
		Job job = new Job("Updating contact information for " + contact.getName()) //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				try {
					String progressId = null;
					if (getProgessDisplayer() != null) {
						progressId = getProgessDisplayer().createProgressIndicator(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ADDING_CONTACT));
					}
					NotesThread.sinitThread();
					final Session session = NotesFactory.createSession();
					// Load the personal address book
					Database db = session.getDatabase(null, "names.nsf"); //$NON-NLS-1$

					// Maintain a map of sugar UUIDs to document objects. This will contain the list of
					// contacts we know are sugar contacts.
					Map<String, Document> sugarContactMap = new HashMap<String, Document>();

					DocumentCollection collection = db.getAllDocuments();
					Document doc = collection.getFirstDocument();
					// Look for all contacts in the address book with the SUAGR_UUID flag. That means we created them.
					while (doc != null) {
						String docType = doc.getItemValueString("Form"); //$NON-NLS-1$
						if (docType.equals("Person") && !doc.isDeleted()) //$NON-NLS-1$
						{
							String sugarId = doc.getItemValueString(SUGAR_UUID);
							if (sugarId != null && sugarId.length() > 0) {
								sugarContactMap.put(sugarId, doc);
							}
						}
						doc = collection.getNextDocument();
					}

					final boolean[] doIt = new boolean[] { true };
					Document docToUpdate = null;
					if (sugarContactMap.containsKey(contact.getId())) {
						// Document to update
						docToUpdate = sugarContactMap.get(contact.getId());

						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								doIt[0] = (new OverwriteContactDialog(getProgessDisplayer().getShell(), contact).open() == Dialog.OK);
							}
						});
					} else {
						// create a new document
						docToUpdate = db.createDocument();
					}

					if (doIt[0]) {
						docToUpdate.replaceItemValue("Form", "Person"); //$NON-NLS-1$ //$NON-NLS-2$
						docToUpdate.replaceItemValue("Type", "Person"); //$NON-NLS-1$ //$NON-NLS-2$
						docToUpdate.replaceItemValue(SUGAR_UUID, contact.getId());
						docToUpdate.replaceItemValue("FirstName", contact.getFirstName()); //$NON-NLS-1$
						docToUpdate.replaceItemValue("LastName", contact.getLastName()); //$NON-NLS-1$
						docToUpdate.replaceItemValue("CompanyName", contact.getAccountName()); //$NON-NLS-1$
						docToUpdate.replaceItemValue("JobTitle", contact.getJobTitle()); //$NON-NLS-1$            

						docToUpdate.replaceItemValue("MailAddress", contact.getEmail()); //$NON-NLS-1$
						docToUpdate.replaceItemValue("InternetAddress", contact.getEmail()); //$NON-NLS-1$
						docToUpdate.replaceItemValue("email_1", contact.getEmail()); //$NON-NLS-1$

						docToUpdate.replaceItemValue("primaryPhoneNumber", contact.getOfficePhone()); //$NON-NLS-1$
						docToUpdate.replaceItemValue("OfficePhoneNumber", contact.getOfficePhone()); //$NON-NLS-1$
						docToUpdate.replaceItemValue("CellPhoneNumber", contact.getMobilePhone()); //$NON-NLS-1$

						docToUpdate.replaceItemValue("OfficeStreetAddress", contact.getStreet()); //$NON-NLS-1$     
						docToUpdate.replaceItemValue("OfficeCity", contact.getCity()); //$NON-NLS-1$
						docToUpdate.replaceItemValue("OfficeState", contact.getState()); //$NON-NLS-1$
						docToUpdate.replaceItemValue("OfficeZIP", contact.getPostalCode()); //$NON-NLS-1$
						docToUpdate.replaceItemValue("OfficeCountry", contact.getCountry()); //$NON-NLS-1$

						docToUpdate.replaceItemValue("FullName", SugarDashboardPreference.getInstance() //$NON-NLS-1$
								.getFormattedNameWithoutSalutation(contact.getFirstName(), contact.getLastName()));

						docToUpdate.save();
					}
					if (getProgessDisplayer() != null && progressId != null) {
						getProgessDisplayer().removeProgressIndicator(progressId);
					}

					session.recycle();
					NotesThread.stermThread();
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
				}

				return Status.OK_STATUS;
			}
		};

		job.schedule();
	}
}
