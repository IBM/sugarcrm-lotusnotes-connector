package com.ibm.socialcrm.notesintegration.connector.util;

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

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateData;

public class AssociatePrefillManager {

	private static AssociatePrefillManager _instance;

	public AssociateData createAssociateData(BaseSugarEntry baseSugarEntry) {
		AssociateData associateData = null;
		String name = null;
		String extended = null;

		if (baseSugarEntry != null) {
			AbstractTypeaheadResultParser parser = ConnectorUtil.getInstance().getResultParser(baseSugarEntry.getSugarType());
			if (parser != null) {
				name = buildAssociatedText(baseSugarEntry, parser);
				extended = buildAssociatedExtendedText(baseSugarEntry, parser);
			}

			associateData = new AssociateData(name, extended, baseSugarEntry.getId(), false);
		}
		return associateData;
	}

	public String createTypeaheadText(BaseSugarEntry baseSugarEntry) {
		String string = null;
		if (baseSugarEntry != null) {
			AbstractTypeaheadResultParser parser = ConnectorUtil.getInstance().getResultParser(baseSugarEntry.getSugarType());
			if (parser != null) {
				string = buildTypeaheadText(baseSugarEntry, parser);
			}
		}
		return string;
	}

	private String buildTypeaheadText(BaseSugarEntry sugarEntry, AbstractTypeaheadResultParser parser) {
		String string = null;
		if (sugarEntry != null && parser != null) {
			string = parser.getTypeaheadText(sugarEntry);
		}
		return string;
	}

	private String buildAssociatedText(BaseSugarEntry sugarEntry, AbstractTypeaheadResultParser parser) {
		String string = null;
		if (sugarEntry != null && parser != null) {
			string = parser.getAssociatedText(sugarEntry);
		}
		return string;
	}

	private String buildAssociatedExtendedText(BaseSugarEntry sugarEntry, AbstractTypeaheadResultParser parser) {
		String string = null;
		if (sugarEntry != null && parser != null) {
			string = parser.getAssociatedExtendedText(sugarEntry);
		}
		return string;
	}

	public static AssociatePrefillManager getInstance() {

		if (_instance == null) {
			_instance = new AssociatePrefillManager();
		}
		return _instance;
	}

}
