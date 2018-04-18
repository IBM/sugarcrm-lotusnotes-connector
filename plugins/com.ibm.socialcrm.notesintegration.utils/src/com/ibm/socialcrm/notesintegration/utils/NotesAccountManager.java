package com.ibm.socialcrm.notesintegration.utils;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import com.ibm.rcp.accounts.Account;
import com.ibm.rcp.accounts.AccountsManager;
import com.ibm.rcp.accounts.AccountsManagerFactory;
import com.ibm.rcp.security.auth.AuthProperties;

public class NotesAccountManager {
	private Account crmAccount;
	// private Account socialServerAccount;

	private static final String CRM_ACCOUNT_NAME = "salesConnect.crmServerAccount"; //$NON-NLS-1$
	//  private static final String SOCIAL_SERVER_ACCOUNT_NAME = "SugarCRM.socialServerAccount"; //$NON-NLS-1$

	private static final String CONFIG_FILE = "/config/config.properties"; //$NON-NLS-1$
	private static final String DEFAULT_SALES_CONNECT_URL = "defaultSalesConnectURL"; //$NON-NLS-1$

	private static NotesAccountManager instance;

	private NotesAccountManager() {
	}

	public static NotesAccountManager getInstance() {
		if (instance == null) {
			instance = new NotesAccountManager();
		}
		return instance;
	}

	public Account getCRMAccount() {
		if (crmAccount == null) {
			try {
				AccountsManager manager = AccountsManagerFactory.getAccountsManager();
				crmAccount = manager.getAccountByName(CRM_ACCOUNT_NAME);

				if (crmAccount == null) {
					crmAccount = new Account();
					crmAccount.setName(CRM_ACCOUNT_NAME);

					crmAccount.setProperty(AuthProperties.AUTH_TYPE, ConstantStrings.HTTP);
					crmAccount.setProperty(AuthProperties.TYPE, ConstantStrings.HTTP);
					crmAccount.setProperty(AuthProperties.DESCRIPTION, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UTILS_CRM_SERVER));
					manager.addAccount(crmAccount);
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
				crmAccount = null;
			}
		}
		return crmAccount;
	}

	// TODO: Re-work this. I just found out that our CRM and Sugar logins will both use intranet ids/pw so
	// long term, we only need one id/pw. For sprint 1, we don't want to expose the files plugin or feeds view
	// so for now, just return the CRM account. We'll need to add this account back in some form when the connections
	// server comes back, but it won't exactly be as it's currently written since the user id/pw still need to be
	// shared with the CRM server.
	public Account getSocialServerAccount() {
		return getCRMAccount();
		// if (socialServerAccount == null)
		// {
		// try
		// {
		// AccountsManager manager = AccountsManagerFactory.getAccountsManager();
		// socialServerAccount = manager.getAccountByName(SOCIAL_SERVER_ACCOUNT_NAME);
		// if (socialServerAccount == null)
		// {
		// socialServerAccount = new Account();
		// socialServerAccount.setName(SOCIAL_SERVER_ACCOUNT_NAME);
		//
		// socialServerAccount.setProperty(AuthProperties.AUTH_TYPE, ConstantStrings.HTTP);
		// socialServerAccount.setProperty(AuthProperties.TYPE, ConstantStrings.HTTP);
		// socialServerAccount.setProperty(AuthProperties.DESCRIPTION, UtilsPlugin.getDefault()
		// .getResourceString(UtilsPluginNLSKeys.UTILS_SOCIAL_SERVER));
		// manager.addAccount(socialServerAccount);
		// }
		// }
		// catch (Exception e)
		// {
		// UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
		// socialServerAccount = null;
		// }
		// }
		// return socialServerAccount;
	}

	/**
	 * Yes, it is insecure to pass around passwords as a string. However, some of the APIs we use require the password as a string. So, it's going to have to be converted to a String at some point.
	 * 
	 * @return
	 */
	public String getCRMPassword() {
		char[] password = getCRMAccount().getAccountPassword();
		String returnPassword = ConstantStrings.EMPTY_STRING;

		if (password != null) {
			returnPassword = new String(password);
		}
		return returnPassword;
	}

	/**
	 * As above... it is a string... yes, this is not good.
	 * 
	 * @return
	 */
	public String getSocialServerPassword() {
		char[] password = getSocialServerAccount().getAccountPassword();
		String returnPassword = ConstantStrings.EMPTY_STRING;

		if (password != null) {
			returnPassword = new String(password);
		}
		return returnPassword;
	}

	public String getCRMServer() {
		String server = getCRMAccount().getProperty(AuthProperties.SERVER);
		if (server == null || server.trim().length() == 0) {
			try {
				URL url = UtilsPlugin.getDefault().getBundle().getEntry(CONFIG_FILE);
				Properties props = new Properties();
				props.load(url.openStream());
				server = props.getProperty(DEFAULT_SALES_CONNECT_URL);

				// If the server is still null after loading the config file
				if (server == null) {
					server = ConstantStrings.EMPTY_STRING;
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, UtilsPlugin.getDefault().PLUGIN_ID);
			}
		}

		// 33949 - strip off *.php at end
		if (server != null && server.length() > 0 && server.endsWith(".php") && server.indexOf(ConstantStrings.FORWARD_SLASH) > -1) //$NON-NLS-1$  
		{
			server = server.substring(0, server.lastIndexOf(ConstantStrings.FORWARD_SLASH) + 1);
		}

		if (server.length() > 0 && !server.endsWith(ConstantStrings.FORWARD_SLASH)) {
			server += ConstantStrings.FORWARD_SLASH;
		}
		return server.trim();
	}

	/**
	 * Returns the base sugar rest url
	 * 
	 * @return
	 */
	public String getSugarRestURL() {
		return getCRMServer() + "service/v4_ibm/rest.php"; //$NON-NLS-1$
	}

	/**
	 * Returns the url to our custom rest.php on the sugar server
	 * 
	 * @return
	 */
	public String getCustomRestURL() {
		return getCRMServer() + "custom/scrmsugarws/v2_1/rest.php"; //$NON-NLS-1$
	}
	/**
	 * Returns the url to the oauth2 token url in the v10 api
	 * 
	 * @return
	 */
	public String getV10TokenURL() {
		return getCRMServer() + "rest/v10/oauth2/token"; //$NON-NLS-1$
	}
	/**
	 * Returns the url to the v10 api
	 * 
	 * @return
	 */
	public String getV10ApiURL() {
		return getCRMServer() + "rest/v10"; //$NON-NLS-1$
	}

	/**
	 * Returns the url to SFARest.php on the sugar server. SFARest is a convenience method for service calls that handles login on our behalf and proxies our webservice calls.
	 * 
	 * @return
	 */
	public String getSFARestServiceURL() {
		return getCRMServer() + "custom/scrmsugarws/v2_1/sfaRest.php"; //$NON-NLS-1$
	}

	public String getSocialServer() {
		String server = getSocialServerAccount().getProperty(AuthProperties.SERVER);
		if (server == null) {
			server = ConstantStrings.EMPTY_STRING;
		}
		return server.trim();
	}

	public String getCRMUser() {
		String user = getCRMAccount().getProperty(AuthProperties.USER_NAME);
		if (user == null) {
			user = ConstantStrings.EMPTY_STRING;
		}
		return user.trim();
	}

	public String getSocialServerUser() {
		String user = getSocialServerAccount().getProperty(AuthProperties.USER_NAME);
		if (user == null) {
			user = ConstantStrings.EMPTY_STRING;
		}
		return user.trim();
	}

}
