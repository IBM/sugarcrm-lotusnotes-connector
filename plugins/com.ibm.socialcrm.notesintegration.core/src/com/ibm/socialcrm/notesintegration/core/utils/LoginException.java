package com.ibm.socialcrm.notesintegration.core.utils;

public class LoginException extends Exception {

	public LoginException() {
	}

	public LoginException(String message) {
		super(message);
	}

	public LoginException(Throwable cause) {
		super(cause);
	}

	public LoginException(String message, Throwable cause) {
		super(message, cause);
	}

}
