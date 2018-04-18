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

import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

/*
 * Mainly used to assemble/disassmble a special drop down list line for the "Items" field in the 
 * Copy To dialog (AssociateComposite.java).  Specifically, this is the special line when there are
 * multiple Account items of the same Account name, and when the count is greater than the max
 * (specified in NotesDocumentSugaritems.MAX_ACCOUNT_LINES)
 */
public class AccountRedirectObj {

	private String _accountName = null;
	private int _matchCount = 0;
	private String _matchString = ConstantStrings.EMPTY_STRING;
	private String _redirectString = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_REDIRECT_TEXT);
	private String _assembledMessageText = null;
	private boolean _isRedirect = true;

	// assemble the redirect text in drop down list
	AccountRedirectObj(String a, int m) {
		_accountName = a;
		_matchCount = m;
		setMatchString();
	}

	// disassemble the redirect text, mainly to get the account name
	public AccountRedirectObj(String s) {
		_assembledMessageText = s;
		disassembleText();
	}

	private void disassembleText() {
		if (_assembledMessageText != null && _assembledMessageText.indexOf(_redirectString) > -1) {
			_isRedirect = true;
			String tempText = _assembledMessageText.substring(0, _assembledMessageText.length() - _redirectString.length());
			if (tempText.lastIndexOf(ConstantStrings.DASH) > -1) {
				tempText = tempText.substring(0, tempText.lastIndexOf(ConstantStrings.DASH));
				_accountName = tempText.trim();
			}
		} else {
			_isRedirect = false;
		}
	}

	public String getAccountName() {
		return _accountName;
	}

	public int getMatchCount() {
		return _matchCount;
	}

	private String getMatchString() {
		return _matchString;
	}

	private void setMatchString() {
		if (_matchCount > DocumentSugarItems.MAX_ACCOUNT_RESULTLIMIT) {
			_matchString = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ASSOCIATE_MATCH_COUNT, new String[] { String.valueOf(_matchCount) });
		}
	}

	private String getRedirectString() {
		return _redirectString;
	}

	public boolean isRedirect() {
		return _isRedirect;
	}

	public String getText() {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		sb.append(getAccountName()).append(ConstantStrings.SPACE).append(getMatchString());

		sb.append(ConstantStrings.SPACE).append(getRedirectString());
		_assembledMessageText = sb.toString();
		return _assembledMessageText;
	}

}
