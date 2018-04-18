package com.ibm.socialcrm.notesintegration.connector.util;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.ibm.notes.java.ui.documents.NotesUIDocument;
import com.ibm.socialcrm.notesintegration.connector.Activator;
import com.ibm.socialcrm.notesintegration.connector.AssociateToolBarControl;
import com.ibm.socialcrm.notesintegration.connector.NotesUIDocumentSaveListenerRepository;
import com.ibm.socialcrm.notesintegration.core.SugarEntrySurrogate;
import com.ibm.socialcrm.notesintegration.core.utils.SugarDashboardPreference;
import com.ibm.socialcrm.notesintegration.ui.actions.MailDocumentSelectionAction;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateDataMap;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class ConnectorUtil {
	public static ConnectorUtil _instance = null;
	private TypeaheadOpportunitiesParser _opportunitiesParser = null;
	private TypeaheadContactsParser _contactsParser = null;
	private TypeaheadLeadsParser _leadsParser = null;
	private TypeaheadAccountsParser _accountsParser = null;

	public static int MAIL_APP = 0;
	public static int CALENDAR_APP = 1;
	public static int CONTACT_APP = 2;

	// Job rule so Jobs/UIJobs following this rule will be executed in the correct order.

	public static final ISchedulingRule UPDATE_ASSOCIATE_JOB_RULE = new ISchedulingRule() {
		public boolean contains(ISchedulingRule rule) {
			return this.equals(rule);
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return this.equals(rule);
		}

		public String toString() {
			return "UPDATE_ASSOCIATE_JOB_RULE"; //$NON-NLS-1$
		}
	};

	public static AssociateDataMap decode(String xml) {
		AssociateDataMap associateDataMap = null;
		if (xml != null) {
			try {
				XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(ConstantStrings.UTF8)));
				associateDataMap = (AssociateDataMap) decoder.readObject();
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}
		}
		return associateDataMap;
	}

	public static String encode(AssociateDataMap associateDataMap) {
		String xml = null;
		if (associateDataMap != null) {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				XMLEncoder xmlEncoder = new XMLEncoder(baos);

				xmlEncoder.writeObject(associateDataMap);
				xmlEncoder.close();
				xmlEncoder.flush();

				xml = baos.toString(ConstantStrings.UTF8);
				baos.close();
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}
		}
		return xml;
	}

	public static SugarEntrySurrogate updateAssociateDataInSugarEntry(SugarEntrySurrogate sugarEntry, String associateDataXML) {
		if (sugarEntry == null) {
			sugarEntry = new SugarEntrySurrogate(MailDocumentSelectionAction.CRM_ASSOCIATE, MailDocumentSelectionAction.getDefaultAssociateSugarType(), associateDataXML, null);
		} else {
			sugarEntry.setAssociateDataMapXML(associateDataXML);
		}
		return sugarEntry;
	}

	public static Color getDarker(Color c) {
		return getDarker(c, 0.75F);
	}

	/** return a color darker than the input c by the given ratio */
	public static Color getDarker(Color c, float ratio) {
		int r = (int) (c.getRed() * ratio);
		int g = (int) (c.getGreen() * ratio);
		int b = (int) (c.getBlue() * ratio);

		RGB rgb = new RGB(r, g, b);
		if (JFaceResources.getColorRegistry().get(rgb.toString()) == null) {
			JFaceResources.getColorRegistry().put(rgb.toString(), rgb);
		}
		return JFaceResources.getColorRegistry().get(rgb.toString());
	}

	public static ConnectorUtil getInstance() {
		if (_instance == null) {
			_instance = new ConnectorUtil();
		}
		return _instance;
	}

	public AbstractTypeaheadResultParser getResultParser(SugarType sugarType) {
		AbstractTypeaheadResultParser parser = null;

		if (sugarType != null && sugarType == SugarType.CONTACTS) {
			if (_contactsParser == null) {
				_contactsParser = new TypeaheadContactsParser();
				parser = _contactsParser;
			}
			parser = _contactsParser;
		} else if (sugarType != null && sugarType == SugarType.LEADS) {
			if (_leadsParser == null) {
				_leadsParser = new TypeaheadLeadsParser();
				parser = _leadsParser;
			}
			parser = _leadsParser;
		} else if (sugarType != null && sugarType == SugarType.ACCOUNTS) {
			if (_accountsParser == null) {
				_accountsParser = new TypeaheadAccountsParser();
			}
			parser = _accountsParser;

		} else if (sugarType != null && sugarType == SugarType.OPPORTUNITIES) {
			if (_opportunitiesParser == null) {
				_opportunitiesParser = new TypeaheadOpportunitiesParser();
			}
			parser = _opportunitiesParser;
		}
		return parser;
	}

	public static String getFormattedName(String s) {
		String text = s;
		String firstName = null;
		String lastName = null;

		if (s != null) {
			String[] pairs = s.split(ConstantStrings.COMMA);
			// firstName=Sam,lastName=Smith
			// 34093 - if client name has common
			if (pairs != null && pairs.length == 2 && (s.indexOf(ConstantStrings.DATABASE_FIRST_NAME) > -1 || s.indexOf(ConstantStrings.DATABASE_LAST_NAME) > -1)) {
				for (int i = 0; i < pairs.length; i++) {
					String[] keyValues = pairs[i].split(ConstantStrings.EQUALS);
					if (keyValues != null && keyValues.length == 2) {

						if (keyValues[0] != null && keyValues[0].equalsIgnoreCase(ConstantStrings.DATABASE_FIRST_NAME)) {
							firstName = keyValues[1];
						} else if (keyValues[0] != null && keyValues[0].equalsIgnoreCase(ConstantStrings.DATABASE_LAST_NAME)) {
							lastName = keyValues[1];
						}

					}
				}

				if (firstName != null && lastName != null) {
					text = SugarDashboardPreference.getInstance().getFormattedNameWithoutSalutation(firstName, lastName);
				}
			} else {
				// for example: name=gd-qqgxb67
				String[] keyValues = s.split(ConstantStrings.EQUALS);
				if (keyValues != null && keyValues.length == 2) {
					text = keyValues[1];
				}
			}
		}

		return text;
	}

	public static boolean isNewDocument(NotesUIDocument doc) {
		boolean isNewDocument = false;

		if (doc != null && doc.isNewDoc()) {
			isNewDocument = true;
		} else {
			isNewDocument = isNewDocument();
		}
		return isNewDocument;
	}

	public static boolean isNewDocument() {
		boolean isNewDocument = false;
		// if a new message, documentActionMap key is the part id
		String partId = getPartId();
		if (partId != null) {
			isNewDocument = (NotesUIDocumentSaveListenerRepository.getDocumentActionMap().containsKey(partId)) ? true : false;
		}

		return isNewDocument;
	}

	public static boolean isNewDocument(String id) {
		boolean isNewDocument = false;
		// part id usually is in this format: com.ibm.workplace.noteswc.views.NotesViewPart@6f286f28
		if (id != null && id.contains("@")) //$NON-NLS-1$
		{
			isNewDocument = true;
		}
		return isNewDocument;
	}

	public static String getPartId() {
		String partId = null;
		if (PlatformUI.getWorkbench() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null
				&& PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart() != null) {
			partId = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().toString();
		}

		return partId;
	}

	public static String getDocId(NotesUIDocument doc) {
		String docId = null;
		if (doc != null && doc.getDocumentData() != null) {
			docId = doc.getDocumentData().getUnid();
		}
		return docId;
	}

	public static Point getPixel(String s) {
		GC gc = new GC(Display.getDefault());
		Point size = gc.textExtent(s); // or textExtent
		gc.dispose();
		return size;
	}

	public static int getAverageCharWidth() {
		GC gc = new GC(Display.getDefault());
		int numChar = gc.getFontMetrics().getAverageCharWidth();
		gc.dispose();
		return numChar;
	}

	public static int getAppType(String form) {
		int appType = MAIL_APP;

		if (form != null) {
			if (form.equalsIgnoreCase("memo") || form.equalsIgnoreCase("draft") || form.equalsIgnoreCase("reply") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					|| form.equalsIgnoreCase("reply with history")) //$NON-NLS-1$
			{
				appType = MAIL_APP;
			} else if (form.equalsIgnoreCase("To Do")) //$NON-NLS-1$
			{
				appType = CONTACT_APP;

			} else if (form.equalsIgnoreCase("_Calendar Entry") /* || form.equalsIgnoreCase("Appointment") */|| AssociateToolBarControl.isCalendar(form)) //$NON-NLS-1$ 
			{
				appType = CALENDAR_APP;

			}
		}
		return appType;
	}

}
