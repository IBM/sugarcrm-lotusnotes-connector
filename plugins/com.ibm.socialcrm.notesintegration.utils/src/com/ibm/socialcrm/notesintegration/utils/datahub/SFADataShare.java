package com.ibm.socialcrm.notesintegration.utils.datahub;

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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.beans.PropertyChangeSupport;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;

/**
 * An SFADataShare is named data store used by the SFADataHub. It is basically like a map but provides facilities for registering change listeners on values.
 * 
 * The two generics in the class definition represent the key, value types that will be used in this map. It's not strictly necessary to use anything other than Object/Object. But it may be convenient
 * to use String/String or some other combination in your data share implementations. It is recommended that anything you use as a key has a reasonable implementation of toString().
 * 
 * @author bcbull
 */
public class SFADataShare<KEY_TYPE extends Object, VALUE_TYPE extends Object> {
	/**
	 * The name of this data share
	 */
	private String name;

	/**
	 * This is the internal map that will be used to store the values in this datashare
	 */
	private Map<KEY_TYPE, VALUE_TYPE> internalMap;

	/**
	 * Map of all of the child shares of this SFADataShare
	 */
	private Map<String, SFADataShare> childShares;

	/**
	 * Parent datashare if one exists. Otherwise, this will be null.
	 */
	private SFADataShare parent;

	private PropertyChangeSupport propertyChangeSupport;

	/**
	 * Internal comparator order child shares
	 */
	private Comparator childShareComparator;

	/**
	 * Creates a new SFADataShare with the specified name.
	 * 
	 * @param name
	 */
	public SFADataShare(String name) {
		setName(name);
	}

	/**
	 * Equivalent to a Map.get();
	 * 
	 * @param key
	 * @return
	 */
	public KEY_TYPE get(VALUE_TYPE key) {
		KEY_TYPE value = null;
		if (key != null) {
			value = (KEY_TYPE) getInternalMap().get(key);
		}
		return value;
	}

	/**
	 * Like a Map.put(), but will also notify any listeners that this value has been changed/added.
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public VALUE_TYPE put(KEY_TYPE key, VALUE_TYPE value) {
		VALUE_TYPE val = null;
		if (key != null) {
			VALUE_TYPE oldValue = getInternalMap().get(key);
			val = getInternalMap().put(key, value);
			propagateChangeEvent(new PropertyChangeEvent(this, key.toString(), oldValue, value), false);
		}
		return val;
	}

	/**
	 * Like a Map.put(), but will notify listeners that this value has been removed.
	 * 
	 * @param key
	 * @return
	 */
	public VALUE_TYPE remove(KEY_TYPE key) {
		VALUE_TYPE val = null;
		if (key != null) {
			val = getInternalMap().remove(key);
			propagateChangeEvent(new PropertyChangeEvent(this, key.toString(), val, null), false);
		}
		return val;
	}

	/**
	 * Notifies our listeners of a change event. And propagates the event up the parent hierarchy as necessary.
	 * 
	 * @param event
	 * @param filterSpecificProperties
	 *            - If we're notifying listeners about an event that bubbled up from our children, we only want to notify those listeners who are listening for all events. A property might have the
	 *            same name between the parent and child, but they are logically different namespaces.
	 */
	private void propagateChangeEvent(PropertyChangeEvent event, boolean filterSpecificProperties) {
		// First, notify our listeners.
		if (!filterSpecificProperties) {
			getPropertyChangeSupport().firePropertyChange(event);
		} else
		// Only notify those who are listening to everything
		{
			PropertyChangeListener[] listeners = getPropertyChangeSupport().getPropertyChangeListeners();
			for (PropertyChangeListener listener : listeners) {
				// Ignore those listeners who are listening for a specific property
				if (!(listener instanceof PropertyChangeListenerProxy)) {
					listener.propertyChange(event);
				}
			}
		}
		if (getParent() != null) {
			getParent().propagateChangeEvent(event, true);
		}
	}

	/**
	 * Adds a child data share to this data share.
	 * 
	 * @param dataShare
	 * @return
	 */
	public boolean addChildShare(SFADataShare dataShare) {
		boolean success = false;
		if (dataShare != null && dataShare.getParent() == null) {
			if (!getChildShares().containsKey(dataShare.getName())) {
				getChildShares().put(dataShare.getName(), dataShare);
				dataShare.setParent(this);
				success = true;
			}
		}
		return success;
	}

	/**
	 * Returns the child share with the given name
	 * 
	 * @param name
	 * @return
	 */
	public SFADataShare getChildShare(String name) {
		SFADataShare share = null;
		if (name != null) {
			share = getChildShares().get(name);
		}
		return share;
	}

	/**
	 * Remove the child share with the given name
	 * 
	 * @param name
	 * @return boolean - if the share removal operation was successful.
	 */
	public boolean removeChildShare(String name) {
		boolean success = false;
		if (name != null) {
			SFADataShare share = getChildShares().remove(name);
			success = (share != null);
			if (share != null) {
				// Re-parent the share, just in case anyone has a reference to it. Once it's removed,
				// change events fired from it shouldn't notify the parent.
				share.setParent(null);
			}
		}
		return success;
	}

	public String getName() {
		if (name == null) {
			name = ConstantStrings.EMPTY_STRING;
		}
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Analogous to a Map.clear(). Recursively clears all child shares
	 */
	public void clear() {
		getInternalMap().clear();
		for (SFADataShare share : getChildShares().values()) {
			share.clear();
		}
		getChildShares().clear();
	}

	/**
	 * Adds a propertyChangeListener for all properties
	 * 
	 * @param listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		addPropertyChangeListener(null, listener);
	}

	/**
	 * Adds a propertyChangeListener for a specific property.
	 * 
	 * @param propertyName
	 * @param listener
	 */
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		if (listener != null) {
			if (propertyName != null) {
				getPropertyChangeSupport().addPropertyChangeListener(propertyName, listener);
			} else {
				getPropertyChangeSupport().addPropertyChangeListener(listener);
			}
		}
	}

	/**
	 * Removes a propertyChangeListener for all properties
	 * 
	 * @param listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		getPropertyChangeSupport().removePropertyChangeListener(listener);
	}

	/**
	 * Removes a propertyChangeListener for a specific property
	 * 
	 * @param listener
	 */
	public void removePropertyChangeListener(String property, PropertyChangeListener listener) {
		getPropertyChangeSupport().removePropertyChangeListener(property, listener);
	}

	/**
	 * Returns the parent share
	 * 
	 * @return
	 */
	public SFADataShare getParent() {
		return parent;
	}

	protected void setParent(SFADataShare parent) {
		this.parent = parent;
	}

	private PropertyChangeSupport getPropertyChangeSupport() {
		if (propertyChangeSupport == null) {
			propertyChangeSupport = new PropertyChangeSupport(this);
		}
		return propertyChangeSupport;
	}

	private Map<KEY_TYPE, VALUE_TYPE> getInternalMap() {
		if (internalMap == null) {
			// Use a tree map to preserve order
			internalMap = new TreeMap<KEY_TYPE, VALUE_TYPE>();
		}
		return internalMap;
	}

	private Map<String, SFADataShare> getChildShares() {
		if (childShares == null) {
			childShares = new TreeMap<String, SFADataShare>(getChildShareComparator());
		}
		return childShares;
	}

	@Override
	public String toString() {
		return getName();
	}

	/**
	 * Returns the key set for the internal map
	 * 
	 * @return
	 */
	public Set getKeySet() {
		return getInternalMap().keySet();
	}

	/**
	 * Returns the key set for the child share map
	 * 
	 * @return
	 */
	public Set getChildKeySet() {
		return getChildShares().keySet();
	}

	/**
	 * Returns the comparator used to sort child shares
	 * 
	 * @return
	 */
	protected Comparator getChildShareComparator() {
		if (childShareComparator == null) {
			childShareComparator = new Comparator<String>() {
				@Override
				public int compare(String shareKey1, String shareKey2) {
					return shareKey1.compareTo(shareKey2);
				}
			};
		}
		return childShareComparator;
	}

}
