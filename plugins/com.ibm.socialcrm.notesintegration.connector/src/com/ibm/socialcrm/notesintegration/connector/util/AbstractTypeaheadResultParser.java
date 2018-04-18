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

import java.util.Map;

import org.apache.commons.json.JSONObject;

import com.ibm.socialcrm.notesintegration.connector.Activator;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;

/**
 * This abstract class helps to parse and construct a type ahead text. Each Sugar type will extend it for an item specific return value.
 * 
 */
public abstract class AbstractTypeaheadResultParser {

	private static final int GET_TYPEAHEAD_TEXT = 1;
	private static final int GET_ASSOCIATED_TEXT = 2;
	private static final int GET_ASSOCIATED_EXTENDED_TEXT = 3;
	private static final int GET_ITEM_FROM_DOCUMENT_TEXT = 4;
	private static final int GET_ITEM_FROM_DOCUMENT_WITH_TYPEAHEAD_TEXT = 5;
	protected static final String FORMAT_NAME_TAG = "#FORMATNAME"; //$NON-NLS-1$

	// Text to be displayed in the typeahead dropdown list
	public String getTypeaheadText(Object object) {
		return getText(object, GET_TYPEAHEAD_TEXT);
	}

	//
	// Method returns text used for either the typeahead UI display or the inf. saved in AssociateData:
	// For typeahead scenario: The text is formatted according to the fields/pattern specified in each item's getTypeaheadTextIndex().
	// It could be the combination of special display characters (for example: dash, space, parentheses), values from
	// the JSONObject, and/or special format tag (#FORMATNAME). If a special format tag is encountered, the subclass
	// should provide the evaluated value.
	// For associated text scenario: The text is the concatination of key/value pairs as of specified in each item's getAssociateTextindex().
	// for example: for contacts, it will be something like first_name=sam,last_name=smith
	private String getText(Object object, int todo) {
		String txt = null;
		int[] textIndex = null;
		boolean isDashInPattern = false;
		boolean isLeftParenthesisInPattern = false;
		boolean isRightParenthesisInPattern = false;

		if (object != null) {
			StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);

			if (todo == GET_TYPEAHEAD_TEXT) {
				textIndex = getTypeaheadTextIndex();
			} else if (todo == GET_ITEM_FROM_DOCUMENT_TEXT) {
				textIndex = getItemFromDocumentTextIndex();
			} else if (todo == GET_ITEM_FROM_DOCUMENT_WITH_TYPEAHEAD_TEXT) {
				textIndex = getItemFromDocumentWithTypeaheadTextIndex();
			} else if (todo == GET_ASSOCIATED_TEXT) {
				textIndex = getAssociatedTextIndex();
			} else if (todo == GET_ASSOCIATED_EXTENDED_TEXT) {
				textIndex = getAssociatedExtendedTextIndex();
			}

			boolean isFirst = true;

			if (textIndex != null && textIndex.length > 0) {
				for (int i = 0; i < textIndex.length; i++) {
					StringBuffer columnSB = new StringBuffer(ConstantStrings.EMPTY_STRING);

					int todoIndex = textIndex[i];

					String key = ConstantStrings.EMPTY_STRING;
					if (todo == GET_ITEM_FROM_DOCUMENT_TEXT) {
						key = getItemFromDocumentWebServiceResultKeys()[todoIndex];
					} else {
						key = getWebServiceResultKeys()[todoIndex];
					}
					if (key != null && key.equalsIgnoreCase(ConstantStrings.DASH)) {
						columnSB.append(ConstantStrings.DASH).append(ConstantStrings.DASH);
						isDashInPattern = true;
					} else if (key != null && (key.equalsIgnoreCase(ConstantStrings.SPACE))) {
						columnSB.append(key);
					} else if (key != null && (key.equalsIgnoreCase(ConstantStrings.RIGHT_PARENTHESIS))) {
						columnSB.append(key);
						isRightParenthesisInPattern = true;
					} else if (key != null && (key.equalsIgnoreCase(ConstantStrings.LEFT_PARENTHESIS))) {
						columnSB.append(key);
						isLeftParenthesisInPattern = true;
					} else if (key != null && (key.startsWith(FORMAT_NAME_TAG))) {
						// formatting name
						columnSB.append(getFormattedName(object, key));
					} else {
						// String value = getJSONValue(jsonObject, key);
						String value = getValue(object, key);
						// If GET_ASSOCIATED_TEXT, use key/value format so the consumer of this text can format it, if necessary, for example: name=firstName, value=Mike
						columnSB.append((value == null) ? ConstantStrings.EMPTY_STRING : getTextFor(key, value, todo, isFirst));
					}

					sb.append(columnSB.toString());

					isFirst = false;

				}

				txt = sb.toString().trim();
				if (isLeftParenthesisInPattern && isRightParenthesisInPattern) {
					txt = txt.replace("()", ConstantStrings.EMPTY_STRING); //$NON-NLS-1$  
				}
				if (isDashInPattern && txt.startsWith("--")) //$NON-NLS-1$
				{
					txt = txt.replace("--", ConstantStrings.EMPTY_STRING); //$NON-NLS-1$ 
				}
				if (isDashInPattern && txt.endsWith("--")) //$NON-NLS-1$
				{
					txt = txt.substring(0, txt.length() - 2);
				}
				txt = txt.trim();
			}
		}

		return txt;
	}

	private String getTextFor(String key, String value, int todo, boolean isFirst) {
		String txt = value;

		if (todo == GET_ASSOCIATED_TEXT || todo == GET_ASSOCIATED_EXTENDED_TEXT) {
			// Return key/value pair, for example: first_name=sam
			StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
			if (!isFirst) {
				sb.append(ConstantStrings.COMMA);
			}
			sb.append(key).append("=").append(value); //$NON-NLS-1$
			txt = sb.toString();
		}
		return txt;
	}

	public String getValue(Object object, String key) {
		String string = null;
		if (object != null && key != null) {
			if (object instanceof JSONObject) {
				string = getJSONValue((JSONObject) object, key);
			} else if (object instanceof BaseSugarEntry) {
				string = getSugarValue((BaseSugarEntry) object, key);
			} else if (object instanceof Map) {
				string = (String) ((Map) object).get((Object) key);
			}

		}
		return string;
	}

	public String getJSONValue(JSONObject jsonObject, String jsonKey) {

		String value = null;
		if (jsonObject != null && jsonKey != null) {
			try {
				if (jsonObject.get(jsonKey) instanceof JSONObject && jsonObject.getJSONObject(jsonKey) != null) {
					value = jsonObject.getJSONObject(jsonKey).getString("value");
				} else {
					value = jsonObject.getString(jsonKey);
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			}
		}
		return value;
	}

	// Text to be saved in AssociateData
	public String getAssociatedText(Object object) {
		return getText(object, GET_ASSOCIATED_TEXT);
	}

	public String getItemFromDocumentWithTypeaheadText(Object object) {
		return getText(object, GET_ITEM_FROM_DOCUMENT_WITH_TYPEAHEAD_TEXT);
	}

	public String getItemFromDocumentText(Object object) {

		return getText(object, GET_ITEM_FROM_DOCUMENT_TEXT);
	}

	public String getAssociatedExtendedText(Object object) {
		return getText(object, GET_ASSOCIATED_EXTENDED_TEXT);
	}

	// Override me if you need formatted name.
	public String getFormattedName(Object object, String key) {
		return null;
	}

	abstract public String[] getWebServiceResultKeys();

	abstract public String[] getItemFromDocumentWebServiceResultKeys();

	abstract public int[] getTypeaheadTextIndex();

	abstract public int[] getItemFromDocumentTextIndex();

	abstract public int[] getItemFromDocumentWithTypeaheadTextIndex();

	abstract public int[] getAssociatedTextIndex();

	abstract public int[] getAssociatedExtendedTextIndex();

	abstract public String getSugarValue(BaseSugarEntry sugarEntry, String key);
}
