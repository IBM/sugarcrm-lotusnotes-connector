package com.ibm.socialcrm.notesintegration.connector;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.ibm.notes.java.ui.documents.NotesUIDocument;
import com.ibm.socialcrm.notesintegration.connector.util.ConnectorUtil;
import com.ibm.socialcrm.notesintegration.core.SugarEntrySurrogate;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.workplace.noteswc.swt.NotesSashControl;

/**
 * There are some scenarios where we want to take an action when a document is saved. For example, A user opens a meeting create dialog and then clicks a toolbar action to associate the new calendar
 * entry with a sugar item. We don't want to actually do the association until the save happens. If the user cancels the meeting, then we won't do the association. Other classes can use this class to
 * be listen for save events on a document an perform a given function when that occurs.
 * 
 * If the UI document is disposed before a save occurs, this class will throw away the registered event.
 * 
 */
public class NotesUIDocumentSaveListenerRepository {
	private static NotesUIDocumentSaveListenerRepository repository;

	private static Map<String, SugarEntrySurrogate> documentActionMap = null;
	private static Map<String, IRunnableWithProgress> documentRunnableMap = null;

	private IRunnableWithProgress _runnable = null;
	private IRunnableWithProgress _prevRunnable = null;

	private NotesUIDocumentSaveListenerRepository() {
	}

	public static NotesUIDocumentSaveListenerRepository getInstance() {
		if (repository == null) {
			repository = new NotesUIDocumentSaveListenerRepository();
		}
		return repository;
	}

	public void addDocumentListener(final NotesUIDocument doc, final String mapkey, final SugarEntrySurrogate sugarEntry, final IRunnableWithProgress runnable) {
		_runnable = runnable;

		if (mapkey != null && getDocumentActionMap().containsKey(mapkey)) {
			getDocumentActionMap().remove(mapkey);
			getDocumentActionMap().put(mapkey, sugarEntry);
			getDocumentRunnableMap().remove(mapkey);
			getDocumentRunnableMap().put(mapkey, runnable);
		} else {

			getDocumentActionMap().put(mapkey, sugarEntry);
			getDocumentRunnableMap().put(mapkey, runnable);

			if (doc != null) {
				doc.addEditListener(new Listener() {

					public void handleEvent(Event arg0) {
					}

				});

				// Note that new msg -> save as draft does not go through this listener
				// Note that draft msg -> save as draft does go through this listener
				doc.addModifiedListener(new Listener() {
					public void handleEvent(Event event) {

						try {

							if (_runnable != null && (_prevRunnable == null || _prevRunnable != _runnable)) {

								// If user is composing a new message, Ctrl-S to save it. This will trigger the document modify listener,
								// we want to skip the update logic contained in the runnable. At this stage, because doc has been modified,
								// we can not simply check doc properties ( title, url, isNewDoc()) to determine if this is a new document, use the
								// isNewDocument() method instead.

								if (ConnectorUtil.isNewDocument(doc)) {
								} else
								// if calendar, don't run the runnable here... partclose() will have the proper logic
								if (doc != null && AssociateToolBarControl.isCalendar(doc.getForm())) {
								} else {
									_prevRunnable = _runnable;
									_runnable.run(new NullProgressMonitor());

									_runnable = null;

									doc.removeModifiedListener(this);
								}
							}
						} catch (Exception e) {
							UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
						}

					}
				});

				// When the document is closed, remove the listener from registry
				doc.addCloseListener(new Listener() {
					public void handleEvent(Event evt) {

						// Remove ourselves from the listener list
						doc.removeCloseListener(this);

						_runnable = null;

					}
				});
			}
		}
	}
	public static void updateSugarEntry(String mapkey, SugarEntrySurrogate sugarEntry) {
		getDocumentActionMap().remove(mapkey);
		getDocumentActionMap().put(mapkey, sugarEntry);
	}

	public static Map<String, SugarEntrySurrogate> getDocumentActionMap() {
		if (documentActionMap == null) {
			documentActionMap = new HashMap<String, SugarEntrySurrogate>();
		}
		return documentActionMap;
	}

	public static Map<String, IRunnableWithProgress> getDocumentRunnableMap() {
		if (documentRunnableMap == null) {
			documentRunnableMap = new HashMap<String, IRunnableWithProgress>();
		}
		return documentRunnableMap;
	}

}
