package com.ibm.socialcrm.notesintegration.files.views;

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

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.ibm.socialcrm.notesintegration.core.ConnectionsDocument;
import com.ibm.socialcrm.notesintegration.core.utils.SugarDashboardPreference;

public class DocumentsFilter extends ViewerFilter {

	String searchString;

	public void setSearchString(String s) {
		// Search must be a substring of the existing value
		this.searchString = ".*" + s + ".*"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (searchString == null || searchString.length() == 0) {
			return true;
		}
		ConnectionsDocument connectionsDocument = (ConnectionsDocument) element;
		if (isValidDateFormat(searchString)) {
			// 41363 - make the search case insensitive
			if (connectionsDocument.getFormattedCDate().toLowerCase().matches(dateTrim(searchString.toLowerCase()))) {
				return true;
			}

		} else {
			// 41363 - make the search case insensitive
			if (connectionsDocument.getFilename().toLowerCase().matches(searchString.trim().toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	private boolean isValidDateFormat(String s) {
		boolean isValid = true;
		try {
			String dtFormat = SugarDashboardPreference.getInstance().getSugarDateFormatPreferenceForJava();
			SimpleDateFormat df = new SimpleDateFormat(dtFormat);
			df.parse(dateTrim(s));
		} catch (Exception e) {
			isValid = false;
		}
		return isValid;
	}

	private String dateTrim(String s) {
		if (s == null) {
			return s;
		}
		String trimX = new String(s);
		trimX = trimX.trim();
		// trim off the .*'s
		trimX = trimX.substring(2, trimX.length() - 2);
		return trimX;
	}
}
