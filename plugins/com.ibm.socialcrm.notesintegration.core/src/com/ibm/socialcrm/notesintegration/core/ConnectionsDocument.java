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
import java.text.DecimalFormat;

import com.ibm.socialcrm.notesintegration.core.utils.SugarDashboardPreference;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class ConnectionsDocument {
	static private String separator = ":::"; //$NON-NLS-1$
	private String _sugarId;
	private String _filename;
	private String _cDate;
	private String _mDate;
	private String _author;
	private int _size;
	private String _docUrl;

	public ConnectionsDocument(String id, String filename, String cDate, String mDate, String author, int size, String docUrl) {
		this._sugarId = id;
		this._filename = filename;
		this._cDate = cDate;
		this._mDate = mDate;
		this._author = author;
		this._size = size;
		this._docUrl = docUrl;
	}

	public String getKey() {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		sb.append(getCDate()).append(getFilename());
		return sb.toString();
	}

	public String getCDateAndAuthor() {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		sb.append(getCDate()).append(ConstantStrings.SPACE).append(ConstantStrings.SPACE).append(ConstantStrings.PIPE).append(ConstantStrings.SPACE).append(ConstantStrings.SPACE).append(getAuthor());
		return sb.toString();
	}

	public String getSugarId() {
		String s = ConstantStrings.EMPTY_STRING;
		if (_sugarId != null) {
			s = _sugarId;
		}
		return s;
	}

	public String getFilename() {
		String s = ConstantStrings.EMPTY_STRING;
		if (_filename != null) {
			s = _filename;
		}
		return s;
	}

	public String getCDate() {
		String s = ConstantStrings.EMPTY_STRING;
		if (_cDate != null) {
			s = _cDate;
		}
		return s;
	}

	public String getFormattedCDate() {
		String s = ConstantStrings.EMPTY_STRING;
		if (_cDate != null) {
			s = SugarDashboardPreference.getInstance().getFormattedDate(_cDate);
		}
		return s;
	}

	public String getMDate() {
		String s = ConstantStrings.EMPTY_STRING;
		if (_mDate != null) {
			s = SugarDashboardPreference.getInstance().getFormattedDate(_mDate);
		}
		return s;
	}

	public String getAuthor() {
		String s = ConstantStrings.EMPTY_STRING;
		if (_author != null) {
			s = _author;
		}
		return s;
	}

	public int getSize() {
		return _size;
	}

	public String getFormattedSize() {
		
		String sizeX = ConstantStrings.EMPTY_STRING;

		
		// #,###,###,###,##0
		StringBuffer sb = new StringBuffer(ConstantStrings.POUND);
		String thousandthSeparator = SugarDashboardPreference.getInstance().getSugarNumberOneThousandthSeparatorPreference();
		sb.append(thousandthSeparator).append(ConstantStrings.POUND).append(ConstantStrings.POUND).append(ConstantStrings.POUND).append(thousandthSeparator).append(ConstantStrings.POUND).append(
				ConstantStrings.POUND).append(ConstantStrings.POUND).append(thousandthSeparator).append(ConstantStrings.POUND).append(ConstantStrings.POUND).append(ConstantStrings.POUND).append(
				thousandthSeparator).append(ConstantStrings.POUND).append(ConstantStrings.POUND).append(0);
		DecimalFormat df = new DecimalFormat(sb.toString());
		int size = getSize();
		if (size < 1024) {
			String tempSizeX = df.format(getSize());

			sizeX = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_SIZE_BYTES, new String[]{tempSizeX, ConstantStrings.SPACE});
		} else {
			// divide by 1024 then plus 0.5 to round up the fractional part
			double longsize= getSize() / 1024.0 + 0.5;
			String tempSizeX = df.format( longsize);
			sizeX = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_SIZE_KB, new String[]{tempSizeX, ConstantStrings.SPACE});
		}

		return sizeX;

	}

	public String getDocUrl() {
		String s = ConstantStrings.EMPTY_STRING;
		if (_docUrl != null) {
			s = _docUrl;
		}
		return s;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(getSugarId());
		sb.append(separator).append(getFilename()).append(separator).append(getCDate()).append(separator).append(getMDate()).append(separator).append(getAuthor()).append(separator).append(getSize())
				.append(separator).append(getDocUrl());
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		boolean equals = false;
		if (obj != null && obj instanceof ConnectionsFile) {
			equals = getSugarId().equals(((ConnectionsFile) obj).getId());
		}
		return equals;
	}

	@Override
	public int hashCode() {
		return getSugarId().hashCode();
	}

}