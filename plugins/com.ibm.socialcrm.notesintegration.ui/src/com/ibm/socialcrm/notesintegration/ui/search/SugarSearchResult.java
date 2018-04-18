package com.ibm.socialcrm.notesintegration.ui.search;

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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Display;

import com.ibm.rcp.search.engines.results.SearchResult;
import com.ibm.siapi.search.Result;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class SugarSearchResult extends SearchResult implements IAdaptable {
	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = -6819351649885263540L;

	private String sugarId;
	private Result result;
	private String name;

	public SugarSearchResult(Result result, String sugarId, String name) {
		super(result);
		this.result = result;
		this.sugarId = sugarId;
		this.name = name;
	}

	@Override
	public Object getAdapter(Class clas) {
		Object obj = null;
		if (clas.equals(IAction.class)) {
			obj = new Action() {
				@Override
				public void run() {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							SugarType sugarType = SugarType.valueOf(result.getProperty(SugarSearchEngineDelegate.TYPE));
							UiUtils.displaySugarItemById(sugarType, sugarId, null);
						}
					});
				}
			};
		}
		return obj;
	}

	/**
	 * Returns the printable name of this item
	 * 
	 * @return
	 */
	public String getName() {
		if (name == null) {
			name = ConstantStrings.EMPTY_STRING;
		}
		return name;
	}
}
