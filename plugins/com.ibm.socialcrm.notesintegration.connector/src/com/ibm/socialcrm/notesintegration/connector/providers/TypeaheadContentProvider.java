package com.ibm.socialcrm.notesintegration.connector.providers;

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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONObject;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.ibm.socialcrm.notesintegration.connector.Activator;
import com.ibm.socialcrm.notesintegration.connector.util.CopytoObject;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateData;
import com.ibm.socialcrm.notesintegration.ui.connector.AssociateDataMap;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;

public class TypeaheadContentProvider implements IStructuredContentProvider {
	public static final String TYPEAHEAD_RESULTLIMIT = "300"; //$NON-NLS-1$

	private TypeaheadCollectionModel _model = null;
	private AssociateDataMap _associateDataMap = null;

	public Object[] getElements(Object obj) {

		showTypeaheadSuggestions();

		return new Object[0];

	}

	private void showTypeaheadSuggestions() {

		JSONObject[] jsonObjects = null;

		try {
			String searchTxt = getCollectionModel().getCacheText();
			String searchCity = getCollectionModel().getCacheCity();
			Boolean isMyItemsB = new Boolean(getCollectionModel().isMyItemsOnly());
			String isMyItems = isMyItemsB.toString();

			String searchResults = SugarWebservicesOperations.getInstance().getTypeaheadInfoFromWebservice(getCollectionModel().getCacheSugarType().getParentType(), searchTxt, TYPEAHEAD_RESULTLIMIT,
					isMyItems, searchCity);

			UiUtils.webServicesLog("typeahead", null, searchResults);
			final JSONObject searchResultsJSON = new JSONObject(searchResults);
			JSONArray resultsArray = searchResultsJSON.getJSONObject(ConstantStrings.RESULTS).getJSONArray(ConstantStrings.DATABASE_FIELDS);

			ArrayList<JSONObject> jsonObjectList = new ArrayList<JSONObject>();

			for (int i = 0; i < resultsArray.length(); i++) {
				JSONObject jsonObject = (JSONObject) resultsArray.get(i);
				// Filter out the entry that the document has already been associated with.
				if ((getCurrConnectorItems() == null || !isAssociated(jsonObject)) && !isCopyTo(jsonObject)) {
					jsonObjectList.add(jsonObject);
				}

			}

			jsonObjects = jsonObjectList.toArray(new JSONObject[jsonObjectList.size()]);
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
			jsonObjects = new JSONObject[0];
		}

		getCollectionModel().setWSResults(jsonObjects);
	}

	private boolean isAssociated(JSONObject jsonObject) {
		boolean isAssociated = false;
		try {
			String type = getCollectionModel().getCacheSugarType().getParentType();
			String currId = jsonObject.getString(ConstantStrings.DATABASE_ID);
			if (getCurrConnectorItems().getMyMap().containsKey(getCurrConnectorItems().getWeightedType(type, true))) {
				List<AssociateData> associateDataList = getCurrConnectorItems().getMyMap().get(getCurrConnectorItems().getWeightedType(type, true));
				for (int i = 0; i < associateDataList.size(); i++) {
					if (associateDataList.get(i).getId() != null && associateDataList.get(i).getId().equalsIgnoreCase(currId)) {
						isAssociated = true;
						break;
					}
				}
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}

		return isAssociated;
	}

	private boolean isCopyTo(JSONObject jsonObject) {
		boolean isCopyTo = false;
		try {
			String type = getCollectionModel().getCacheSugarType().getParentType();
			if (getCollectionModel().getCopytoObjectMap() != null) {
				Collection<CopytoObject> c = getCollectionModel().getCopytoObjectMap().values();
				String currId = jsonObject.getString(ConstantStrings.DATABASE_ID);
				Iterator it = c.iterator();
				while (it.hasNext()) {
					CopytoObject co = (CopytoObject) it.next();
					String id = co.getAssociatedData().getId();
					if (id != null && currId != null && id.equalsIgnoreCase(currId)) {
						isCopyTo = true;
						break;
					}
				}
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, Activator.PLUGIN_ID);
		}

		return isCopyTo;
	}

	private AssociateDataMap getCurrConnectorItems() {
		return _associateDataMap;
	}

	public void setAssociateDataMap(AssociateDataMap associateDataMap) {
		_associateDataMap = associateDataMap;
	}

	public void dispose() {
	}

	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
	}

	public void setCollectionModel(TypeaheadCollectionModel model) {
		_model = model;
	}

	public TypeaheadCollectionModel getCollectionModel() {
		return _model;
	}
}
