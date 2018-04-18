package com.ibm.socialcrm.notesintegration.core;

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

import lotus.domino.Database;
import lotus.domino.NotesFactory;
import lotus.domino.NotesThread;
import lotus.domino.Session;

import org.eclipse.ui.IStartup;

import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class MailDatabaseInfoLoader implements IStartup {
	@Override
	public void earlyStartup() {
		Session notesSession = null;
		Database db = null;

		try {
			NotesThread.sinitThread();
			notesSession = NotesFactory.createSession();

			// Get Mail DB
			Vector vals = notesSession.evaluate("@MailDbName"); //$NON-NLS-1$
			db = notesSession.getDatabase((String) vals.get(0), (String) vals.get(1));
			if (db == null) {
				UtilsPlugin.getDefault().logErrorMessage(
						UtilsPlugin.getDefault().format(UtilsPluginNLSKeys.CORE_UNABLE_TO_RETRIEVE_MAIL_DB, null, new String[] { vals.get(0).toString(), vals.get(1).toString() }),
						CorePluginActivator.PLUGIN_ID);
			} else {

				if (db.isOpen() == false) {

					// defect 23563 - use openWithFailover in case specified server is failed over
					// to the failover server.
					boolean isopen = db.openWithFailover((String) vals.get(0), (String) vals.get(1));

					String[] msgs = new String[] { "Open database: ", (isopen ? "Successful" : "Failed"), //$NON-NLS-1$  //$NON-NLS-2$ //$NON-NLS-3$
							" ... Orig. specified server: ", (String) vals.get(0), ", dbfile: ", (String) vals.get(1), //$NON-NLS-1$ //$NON-NLS-2$
							" ... Result server: ", db.getServer(), ", dbfile: ", db.getFileName() }; //$NON-NLS-1$  //$NON-NLS-2$

					if (isopen) {
						UtilsPlugin.getDefault().logInfoMessage(msgs, CorePluginActivator.PLUGIN_ID);
					} else {
						UtilsPlugin.getDefault().logWarningMessage(msgs, CorePluginActivator.PLUGIN_ID);

					}
				}

				if (db.isOpen()) {
					MailDatabaseInfo.getInstance().setMailDbReplicaId(db.getReplicaID());
				}

			}

		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
		}

		finally {
			try {
				if (db != null) {
					db.recycle();
				}
				if (notesSession != null) {
					notesSession.recycle();
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			} finally {
				NotesThread.stermThread();
			}
		}

	}
}
