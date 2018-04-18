package com.ibm.socialcrm.notesintegration.utils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.UUID;

public class WebSecurityCodeProvider {
	/*
	 * Class responsible for holding and generating a rolling code to secure web panels accessing the Java side of the plugins via Servlets. This code will be verified by any inbound servlet or the
	 * request will be disallowed. This class will handle the creation of the code and the propagation of changes to interested parties through propertyChangeListeners.
	 * 
	 * The code will be passed to each web panel via direct browser object javascript calls (thus secure), not http.
	 */
	private static WebSecurityCodeProvider instance;

	public static WebSecurityCodeProvider getInstance() {
		if (instance == null) {
			instance = new WebSecurityCodeProvider();
		}
		return instance;
	}

	private UUID securityCode;
	private PropertyChangeSupport propertyChangeSupport;

	private WebSecurityCodeProvider() {
		// generate the initial code
		generateSecurityCode();
	}

	public UUID getSecurityCode() {
		return securityCode;
	}

	public void generateSecurityCode() {
		securityCode = UUID.randomUUID();
		propagateChangeEvent(new PropertyChangeEvent(this, "securityCode", null, securityCode));
	}

	public String getSecurityCodeString() {
		return getSecurityCode().toString();

	}

	private PropertyChangeSupport getPropertyChangeSupport() {
		if (propertyChangeSupport == null) {
			propertyChangeSupport = new PropertyChangeSupport(this);
		}
		return propertyChangeSupport;
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

	private void propagateChangeEvent(PropertyChangeEvent event) {
		getPropertyChangeSupport().firePropertyChange(event);
	}

	/**
	 * Removes a propertyChangeListener for all properties
	 * 
	 * @param listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		getPropertyChangeSupport().removePropertyChangeListener(listener);
	}
}
