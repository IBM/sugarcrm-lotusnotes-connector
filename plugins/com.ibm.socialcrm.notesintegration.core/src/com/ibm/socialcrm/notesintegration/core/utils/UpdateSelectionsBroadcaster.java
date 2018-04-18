package com.ibm.socialcrm.notesintegration.core.utils;

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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Map;
import java.util.Set;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarEntrySurrogate;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class UpdateSelectionsBroadcaster {

	private static UpdateSelectionsBroadcaster instance = null;
	private PropertyChangeSupport pcs;

	public static final String SELECTION_CHANGED = "selectionChanged"; //$NON-NLS-1$
	public static final String SELECTION_TITLE = "selectionTitle"; //$NON-NLS-1$
	public static final String CONNECTOR_UPDATE = "connectoUpdate"; //$NON-NLS-1$
	public static final String DASHBOARD_UPDATE = "dashboardUpdate"; //$NON-NLS-1$
	public static final String CONNECTOR_UPDATE_IS_DONE = "connectoUpdateIsDone"; //$NON-NLS-1$

	public static final String UPDATE_CERTIFICATE = "updateCertificate"; //$NON-NLS-1$

	/**
	 * Property used when the base tab of the card has retrieved data. This notifies all the other tabs that they can proceed with their web service calls.
	 */
	public static final String BASECARD_DATA_RETRIEVED = "BasecardDataRetrieved"; //$NON-NLS-1$

	private UpdateSelectionsBroadcaster() {
		pcs = new PropertyChangeSupport(this);
	}

	public static UpdateSelectionsBroadcaster getInstance() {
		if (instance == null) {
			instance = new UpdateSelectionsBroadcaster();
		}
		return instance;
	}

	public void registerListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	public void unregisterListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	public void updateOpportunities(Set<SugarEntrySurrogate> opportunities) {
		pcs.firePropertyChange(SugarType.OPPORTUNITIES.toString(), ConstantStrings.EMPTY_STRING, opportunities);
	}

	public void updateAccounts(Set<SugarEntrySurrogate> accounts) {
		pcs.firePropertyChange(SugarType.ACCOUNTS.toString(), ConstantStrings.EMPTY_STRING, accounts);
	}

	public void updateContacts(Set<SugarEntrySurrogate> contacts) {
		pcs.firePropertyChange(SugarType.CONTACTS.toString(), ConstantStrings.EMPTY_STRING, contacts);
	}

	/**
	 * Notifies listeners that the set of selected items has changed
	 * 
	 * @param selectedItems
	 */
	public void updateSelectedItems(Map<SugarType, Set<SugarEntrySurrogate>> selectedItems) {
		pcs.firePropertyChange(SELECTION_CHANGED, null, selectedItems);
	}

	public void updateSelectedTitle(String title) {
		pcs.firePropertyChange(SELECTION_TITLE, null, title);
	}

	/*
	 * 
	 * Object could be instance of one of the following classes: - 
	 * - BaseSugarEntry: when associating email/meeting with a card via the Associate button, the "Associate ... with this document" More
	 * action in card, or the "Associate ... with this meeting" More action in card. 
	 * - BaseSugarEntry[]: When create email/meeting is triggered from SalesConnect 
	 * - SugarDashboardDndEntry: when associating email/meeting with a card via Dnd
	 */

	public void updateConnector(Object obj) {
		pcs.firePropertyChange(CONNECTOR_UPDATE, null, obj);
	}

	// 55883
	/*
	 * 
	 * When association (copyto) process is done, raise this property change event... Currently obj is not used, so could be anything
	 */
	public void updateConnectorIsDone(Object obj) {
		pcs.firePropertyChange(CONNECTOR_UPDATE_IS_DONE, null, obj);
	}

	/*
	 * 
	 * Object could be instance of one of the following classes: - BaseSugarEntry: when document upload is successfully completed
	 */
	public void updateDashboard(Object obj) {
		pcs.firePropertyChange(DASHBOARD_UPDATE, null, obj);
	}

	/**
	 * Notifies listeners that the base tab web service is done
	 * 
	 * @param selectedItems
	 */
	// public void basecardDataRetrieved(boolean isSuccess) {
	public void basecardDataRetrieved(BaseSugarEntry entry) {
		pcs.firePropertyChange(BASECARD_DATA_RETRIEVED, null, entry);
	}

	// 74684
	public void updateCertificate() {
		pcs.firePropertyChange(UPDATE_CERTIFICATE, null, null);
	}
}
