package com.ibm.socialcrm.notesintegration.sugarwidgetupdate;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.beans.PropertyChangeSupport;
import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONObject;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.SugarEntrySurrogate;
import com.ibm.socialcrm.notesintegration.core.utils.SugarTypePersistenceDelegate;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

/**
 * The activator class controls the plug-in life cycle
 */
public class SugarWidgetUpdateActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.ibm.socialcrm.notesintegration.sugarwidgetupdate"; //$NON-NLS-1$

	// The shared instance
	private static SugarWidgetUpdateActivator plugin;

	/**
	 * Used to fire an even indicating that startup updates are complete.
	 */
	private PropertyChangeSupport propertyChangeSupport;

	/**
	 * Event constant for subscribing to sugar updates
	 */
	public static final String UPDATE_COMPLETE_PROPERTY = "updateCompleteProperty"; //$NON-NLS-1$

	// The regular expressions for contacts and accounts are stored in the activator so plugins can get
	// to them at runtime without digging through preferences.
	private String contactsRegex = null;
	private String accountsRegex = null;
	private String opportunitiesRegex = null;
	private String accountTags = null;
	private String contactTags = null;
	private String opportunityTags = null;
	private Map<String, Set<String>> accountTagsMap = null;
	private Map<String, Set<String>> contactTagsMap = null;
	private Map<String, Set<String>> opportunityTagsMap = null;

	private Pattern accountsPattern = null;
	private Pattern contactsPattern = null;
	private Pattern opportunitiesPattern = null;

	private String callFormInfo = null;

	private Set<SugarEntrySurrogate> favorites;

	/**
	 * The constructor
	 */
	public SugarWidgetUpdateActivator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static SugarWidgetUpdateActivator getDefault() {
		return plugin;
	}

	public void setContactsRegex(String contactsRegex) {
		this.contactsRegex = contactsRegex;
	}

	public String getContactsRegex() {
		return contactsRegex;
	}

	public void setAccountsRegex(String accountsRegex) {
		this.accountsRegex = accountsRegex;
	}

	public String getAccountsRegex() {
		return accountsRegex;
	}

	public String getOpportunitiesRegex() {
		return opportunitiesRegex;
	}

	public void setOpportunitiesRegex(String opportunitiesRegex) {
		this.opportunitiesRegex = opportunitiesRegex;
	}

	public void setAccountTags(String accountTags) {
		this.accountTags = accountTags;
	}

	public String getAccountTags() {
		return accountTags;
	}

	public String getContactTags() {
		return contactTags;
	}

	public void setContactTags(String contactTags) {
		this.contactTags = contactTags;
	}

	public String getOpportunityTags() {
		return opportunityTags;
	}

	public String getCallFormInfo() {
		return callFormInfo;
	}

	public void setCallFormInfo(String callFormInfo) {
		this.callFormInfo = callFormInfo;
	}

	public void setOpportunityTags(String opportunityTags) {
		this.opportunityTags = opportunityTags;
	}

	public Map<String, Set<String>> getAccountTagsMap() {
		if (accountTagsMap == null) {
			if (getAccountTags() != null) {
				accountTagsMap = new HashMap<String, Set<String>>();
				populateTagMap(accountTagsMap, getAccountTags());
			}
		}
		return accountTagsMap;
	}

	public Map<String, Set<String>> getContactTagsMap() {
		if (contactTagsMap == null) {
			if (getContactTags() != null) {
				contactTagsMap = new HashMap<String, Set<String>>();
				populateTagMap(contactTagsMap, getContactTags());
			}
		}
		return contactTagsMap;
	}

	public Map<String, Set<String>> getOpportunityTagsMap() {
		if (opportunityTagsMap == null) {
			if (getOpportunityTags() != null) {
				opportunityTagsMap = new HashMap<String, Set<String>>();
				populateTagMap(opportunityTagsMap, getOpportunityTags());
			}
		}
		return opportunityTagsMap;
	}

	public Map<String, Set<String>> getTagMapForType(SugarType type) {
		Map<String, Set<String>> tagMap = null;
		if (type == SugarType.ACCOUNTS) {
			tagMap = getAccountTagsMap();
		} else if (type == SugarType.CONTACTS) {
			tagMap = getContactTagsMap();
		} else if (type == SugarType.OPPORTUNITIES) {
			tagMap = getOpportunityTagsMap();
		}
		return tagMap;
	}

	/**
	 * Helper method used to populate a tag map from the json data we get from the web service.
	 * 
	 * @param tagMap
	 * @param jsonTagList
	 */
	private void populateTagMap(Map<String, Set<String>> tagMap, String jsonTagList) {
		try {
			JSONObject tagListObj = new JSONObject(jsonTagList);
			Iterator<String> tagIter = tagListObj.keys();
			while (tagIter.hasNext()) {
				String key = (String) tagIter.next();
				JSONArray tagArray = tagListObj.getJSONArray(key);
				Set<String> entries = new HashSet<String>();
				for (int i = 0; i < tagArray.length(); i++) {
					// Bookend tags with \Q and \E in case tags contain metacharacter, for example: "CLEVELAND*" - defect 17382
					entries.add("(?<!\\S)(" + "\\Q" + tagArray.getString(i).trim().toUpperCase() + "\\E" + ")(?!\\S)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$				
				}
				tagMap.put(key, entries);
			}
		} catch (Exception e) {
			tagMap = null;
		}
	}

	public void resetTagMaps() {
		accountTagsMap = null;
		opportunityTagsMap = null;
		contactTagsMap = null;
	}

	public void setFavorites(Set<SugarEntrySurrogate> favorites) {
		this.favorites = favorites;
	}

	public Set<SugarEntrySurrogate> getFavorites() {
		if (favorites == null) {
			favorites = new HashSet<SugarEntrySurrogate>();
		}
		return favorites;
	}

	public void saveFavorites() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			XMLEncoder encoder = new XMLEncoder(baos);

			encoder.setPersistenceDelegate(SugarType.class, new SugarTypePersistenceDelegate());
			encoder.writeObject(SugarWidgetUpdateActivator.getDefault().getFavorites());

			encoder.close();
			baos.close();

			Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();
			prefs.setValue(SugarWidgetUpdater.FAVORITES_PREF, new String(baos.toByteArray()));
			CorePluginActivator.getDefault().savePluginPreferences();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, PLUGIN_ID);
		}
	}

	public PropertyChangeSupport getPropertyChangeSupport() {
		if (propertyChangeSupport == null) {
			propertyChangeSupport = new PropertyChangeSupport(this);
		}
		return propertyChangeSupport;
	}

	public Pattern getAccountsPattern() {
		if (accountsPattern == null && SugarWidgetUpdateActivator.getDefault().getAccountsRegex() != null) {
			accountsPattern = Pattern.compile(SugarWidgetUpdateActivator.getDefault().getAccountsRegex());
		}
		return accountsPattern;
	}

	public Pattern getContactsPattern() {
		if (contactsPattern == null && SugarWidgetUpdateActivator.getDefault().getContactsRegex() != null) {
			contactsPattern = Pattern.compile(SugarWidgetUpdateActivator.getDefault().getContactsRegex());
		}
		return contactsPattern;
	}

	public Pattern getOpportunitiesPatern() {
		if (opportunitiesPattern == null && SugarWidgetUpdateActivator.getDefault().getOpportunitiesRegex() != null) {
			opportunitiesPattern = Pattern.compile(SugarWidgetUpdateActivator.getDefault().getOpportunitiesRegex());
		}
		return opportunitiesPattern;
	}

	public void resetPatterns() {
		contactsPattern = null;
		accountsPattern = null;
		opportunitiesPattern = null;
		SugarWidgetUpdateActivator.getDefault().resetTagMaps();
	}

}
