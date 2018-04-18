package com.ibm.socialcrm.notesintegration.ui.connector;

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
import java.util.Set;
import java.util.TreeMap;

import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

/**
 * This Object represents all the current association information for a given Notes document ( email / calendar). It is organized by the Sugar Type. In order to display them in the fixed Sugar type
 * order in the Associate Dialog, for now I assigned a weight to each Sugar Type. It should be replaced with a Comparator.
 */
public class AssociateDataMap {

	private TreeMap<String, List<AssociateData>> mymap = null;

	private String assignedSugarId = null;

	// This class is used for XMLEncoder. Be sure every variables have getter/setter.
	public AssociateDataMap() {

	}

	// Create a subset of AssociateDataMap with all the entries are either associated or not associated.
	public AssociateDataMap getSubset(boolean isAssociated) {
		AssociateDataMap newAssociateDataMap = null;

		TreeMap<String, List<AssociateData>> treemap = getMyMap();
		Iterator<String> it = treemap.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			List<AssociateData> list = treemap.get(key);
			Iterator<AssociateData> itAssociateData = list.iterator();
			while (itAssociateData.hasNext()) {
				AssociateData associateData = itAssociateData.next();
				if ((associateData.isAssociated() == isAssociated)) {
					if (newAssociateDataMap == null) {
						newAssociateDataMap = new AssociateDataMap();
					}
					newAssociateDataMap.addAssociateData(key, new AssociateData(associateData.getName(), associateData.getExtended(), associateData.getId(), false), false);
				}
			}

		}
		return newAssociateDataMap;
	}

	public void addAssociateData(String type, AssociateData associateData) {
		addAssociateData(type, associateData, true);
		// assignedSugarId = null;

	}

	public void addAssociateData(String type, AssociateData associateData, boolean isToAddWeight) {

		if (type != null && associateData != null) {

			if (mymap == null) {
				mymap = new TreeMap<String, List<AssociateData>>();
			}

			if (!getMyMap().containsKey(getWeightedType(type, isToAddWeight))) {
				List<AssociateData> associateDataList = new ArrayList<AssociateData>();
				associateDataList.add(associateData);
				getMyMap().put(getWeightedType(type, isToAddWeight), associateDataList);
			} else {
				List<AssociateData> associateDataList = getMyMap().get(getWeightedType(type, isToAddWeight));
				if (!associateDataList.contains(associateData)) {
					associateDataList.add(associateData);
				}
			}

		}
	}

	public boolean isTheSame(AssociateDataMap compareTo) {
		boolean isTheSame = false;
		if (compareTo != null) {
			TreeMap<String, List<AssociateData>> compare2TreeMap = compareTo.getMyMap();
			TreeMap<String, List<AssociateData>> compare1TreeMap = getMyMap();
			if (compare2TreeMap != null && compare1TreeMap != null && compare2TreeMap.size() == compare1TreeMap.size()) {
				Set<String> compare1KeySet = compare1TreeMap.keySet();
				Set<String> compare2KeySet = compare2TreeMap.keySet();
				if (compare1KeySet.equals(compare2KeySet)) {
					Collection<List<AssociateData>> compare1Collection = compare1TreeMap.values();
					Collection<List<AssociateData>> compare2Collection = compare2TreeMap.values();
					if (compare1Collection.size() == compare2Collection.size()) {
						boolean isBad = false;
						Iterator<List<AssociateData>> it = compare1Collection.iterator();
						while (it.hasNext()) {
							List<AssociateData> list = it.next();
							if (!compare2Collection.contains(list)) {
								isBad = true;
							}
						}
						if (!isBad) {
							isTheSame = true;
						}
					}
				}
			}
		}
		return isTheSame;
	}

	public String getWeightedType(String type, boolean isToAddWeight) {
		String weightedType = type;
		if (type != null && isToAddWeight) {
			StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
			sb.append(getWeight(type)).append(type);
			weightedType = sb.toString();
		}
		return weightedType;
	}

	public void removeUnAssociated() {
		if (getMyMap() != null && !getMyMap().isEmpty()) {
			Iterator<List<AssociateData>> it = getMyMap().values().iterator();
			while (it.hasNext()) {
				List<AssociateData> list = it.next();
				Iterator<AssociateData> ciIt = list.iterator();
				while (ciIt.hasNext()) {
					if (!ciIt.next().isAssociated()) {
						ciIt.remove();
					}
				}
			}
		}
	}

	public void setAllIsAssociated(boolean b) {
		if (getMyMap() != null && !getMyMap().isEmpty()) {
			Iterator<List<AssociateData>> it = getMyMap().values().iterator();
			while (it.hasNext()) {
				List<AssociateData> list = it.next();
				Iterator<AssociateData> ciIt = list.iterator();
				while (ciIt.hasNext()) {
					ciIt.next().setAssociated(b);
				}
			}
		}
	}

	// I would like to replace this with a Comparator, but, be sure the Comparator is serializable.
	public static String getWeight(String s) {
		String weight = "0"; //$NON-NLS-1$
		if (s != null) {
			if (s.equalsIgnoreCase(SugarType.ACCOUNTS.getParentType())) {
				weight = "1"; //$NON-NLS-1$
			} else if (s.equals(SugarType.OPPORTUNITIES.getParentType())) {
				weight = "2"; //$NON-NLS-1$
			} else if (s.equalsIgnoreCase(SugarType.CONTACTS.getParentType())) {
				weight = "3"; //$NON-NLS-1$
			} else if (s.equalsIgnoreCase(SugarType.LEADS.getParentType())) {
				weight = "4"; //$NON-NLS-1$
			}

		}
		return weight;
	}

	public TreeMap<String, List<AssociateData>> getMyMap() {
		return mymap;
	}

	public void setMyMap(TreeMap<String, List<AssociateData>> map) {
		this.mymap = map;
	}

	public String getAssignedSugarId() {
		return assignedSugarId;
	}

	public void setAssignedSugarId(String s) {
		assignedSugarId = s;
	}

	public List<String> getIdList(String type) {
		List<String> idList = null;

		if (getMyMap().containsKey(getWeightedType(type, true))) {
			List<AssociateData> list = getMyMap().get(getWeightedType(type, true));
			for (int i = 0; i < list.size(); i++) {
				if (idList == null) {
					idList = new ArrayList<String>();
				}
				idList.add(list.get(i).getId());
			}
		}
		return idList;

	}

	public void removeAssociateData(String type, AssociateData associateData, boolean isToAddWeight) {
		removeAssociateData(type, associateData, isToAddWeight, false);
	}

	public void removeAssociateData(String type, AssociateData associateData, boolean isToAddWeight, boolean isIgnoreAssociateFlag) {
		removeAssociateData(type, associateData, isToAddWeight, false, false);
	}

	public void removeAssociateData(String type, AssociateData associateData, boolean isToAddWeight, boolean isIgnoreAssociateFlag, boolean isIgnoreName) {
		if (type != null && associateData != null) {

			if (mymap == null) {
				mymap = new TreeMap<String, List<AssociateData>>();
			}

			if (getMyMap().containsKey(getWeightedType(type, isToAddWeight))) {
				List<AssociateData> associateDataList = associateDataList = getMyMap().get(getWeightedType(type, isToAddWeight));
				if (associateDataList != null && !associateDataList.isEmpty()) {
					if (isIgnoreAssociateFlag) {
						int index = -1;
						for (int i = 0; i < associateDataList.size(); i++) {
							AssociateData ad = associateDataList.get(i);
							// if (ad.getId() != null && ad.getName() != null && ad.getId().equalsIgnoreCase(associateData.getId()) && ad.getName().equalsIgnoreCase(associateData.getName())) {
							if (ad.getId() != null && ad.getId().equalsIgnoreCase(associateData.getId())) {
								if (isIgnoreName || (!isIgnoreName && (ad.getName() != null && ad.getName().equalsIgnoreCase(associateData.getName())))) {
									index = i;
									break;
								}
							}
						}
						if (index > -1) {
							associateDataList.remove(index);
							getMyMap().put(getWeightedType(type, isToAddWeight), associateDataList);
						}
					} else {
						if (associateDataList.contains(associateData)) {
							associateDataList.remove(associateData);
							getMyMap().put(getWeightedType(type, isToAddWeight), associateDataList);
						}
					}
				}
				// if no more entry in associateDataList, remove this entry from the map
				if (associateDataList.isEmpty()) {
					getMyMap().remove(getWeightedType(type, isToAddWeight));
				}
			}
		}
	}

	public boolean isAnyAssociateWithLead() {
		boolean isAssociateWithLead = false;
		if (getMyMap() != null && !getMyMap().isEmpty()) {
			Iterator<String> it = getMyMap().keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				if (key.substring(1).equalsIgnoreCase(SugarType.LEADS.getParentType())) {
					isAssociateWithLead = true;
					break;
				}
			}
		}
		return isAssociateWithLead;
	}

}
