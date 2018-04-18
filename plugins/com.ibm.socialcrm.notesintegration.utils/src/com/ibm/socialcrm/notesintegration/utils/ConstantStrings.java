package com.ibm.socialcrm.notesintegration.utils;

/****************************************************************
 * IBM Confidential
 * 
 * SFA050-Collaboration Source Materials
 * 
 * (C) Copyright IBM Corp. 2012
 * 
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been deposited with the U.S. Copyright Office
 * 
 ***************************************************************/

public class ConstantStrings {
	public static final String SLASH = System.getProperty("file.separator"); //$NON-NLS-1$
	public static final String SFA_TEMP_DIR = "sfaTemp"; //$NON-NLS-1$
	public static final String FORWARD_SLASH = "/"; //$NON-NLS-1$
	public static final String NEW_LINE = "\n"; //$NON-NLS-1$
	public static final String EMPTY_STRING = ""; //$NON-NLS-1$
	public static final String UTF8 = "UTF-8"; //$NON-NLS-1$
	public static final String HTTP = "HTTP"; //$NON-NLS-1$
	public static final String GREATER_THAN = ">"; //$NON-NLS-1$
	public static final String LESS_THAN = "<"; //$NON-NLS-1$
	public static final String LEFT_PARENTHESIS = "("; //$NON-NLS-1$
	public static final String RIGHT_PARENTHESIS = ")"; //$NON-NLS-1$
	public static final String SEPARATOR = "separator"; //$NON-NLS-1$
	public static final String PERIOD = "."; //$NON-NLS-1$
	public static final String POUND = "#"; //$NON-NLS-1$ 

	public static final String QUESTION = "?"; //$NON-NLS-1$
	public static final String AMPERSAND = "&"; //$NON-NLS-1$
	public static final String EQUALS = "="; //$NON-NLS-1$
	public static final String PIPE = "|"; //$NON-NLS-1$
	public static final String SPACE = " "; //$NON-NLS-1$
	public static final String UNDER_SCORE = "_"; //$NON-NLS-1$
	public static final String COMMA = ","; //$NON-NLS-1$
	public static final String COLON = ":"; //$NON-NLS-1$
	public static final String DOUBLE_QUOTE = "\""; //$NON-NLS-1$
	public static final String DASH = "-"; //$NON-NLS-1$
	public static final String SEMICOLON = ";"; //$NON-NLS-1$

	// Sugar constants
	public static final String ACCOUNTS = "accounts"; //$NON-NLS-1$
	public static final String ACCOUNT = "account"; //$NON-NLS-1$
	public static final String ACCOUNT_NAME = "account_name"; //$NON-NLS-1$
	public static final String ACCOUNT_ID = "account_id"; //$NON-NLS-1$
	public static final String OPPORTUNITIES = "opportunities"; //$NON-NLS-1$
	public static final String CONTACTS = "contacts"; //$NON-NLS-1$
	public static final String MEMBERS = "members"; //$NON-NLS-1$
	public static final String ID = "id"; //$NON-NLS-1$
	public static final String RESULTS = "results"; //$NON-NLS-1$

	// Sugar Login constants
	public static final String USER_NAME = "user_name"; //$NON-NLS-1$
	public static final String USER_PASSWORD = "user_password"; //$NON-NLS-1$
	public static final String MODULE = "module"; //$NON-NLS-1$
	public static final String USERS = "Users"; //$NON-NLS-1$
	public static final String RETURN_ACTION = "return_action"; //$NON-NLS-1$
	public static final String LOGIN = "Login"; //$NON-NLS-1$
	public static final String RETURN_MODULE = "return_module"; //$NON-NLS-1$
	public static final String ACTION = "action"; //$NON-NLS-1$
	public static final String AUTHENTICATE = "Authenticate"; //$NON-NLS-1$
	public static final String LOG_IN = "Log In"; //$NON-NLS-1$
	public static final String USERID = "userid"; //$NON-NLS-1$
	public static final String PASSWORD = "password"; //$NON-NLS-1$

	// Sugar database constants
	public static final String DATABASE_MODULE_ACCOUNTS = "Accounts"; //$NON-NLS-1$
	public static final String DATABASE_MODULE_OPPORTUNITIES = "Opportunities"; //$NON-NLS-1$
	public static final String DATABASE_MODULE_LEADS = "Leads"; //$NON-NLS-1$
	public static final String DATABASE_MODULE_CONTACTS = "Contacts"; //$NON-NLS-1$
	public static final String DATABASE_ID = "id"; //$NON-NLS-1$
	public static final String DATABASE_FAV_ID = "favid"; //$NON-NLS-1$
	public static final String DATABASE_NAME = "name"; //$NON-NLS-1$
	public static final String DATABASE_FIRST_NAME = "first_name"; //$NON-NLS-1$
	public static final String DATABASE_LAST_NAME = "last_name"; //$NON-NLS-1$
	public static final String DATABASE_WEBSITE = "website"; //$NON-NLS-1$
	public static final String DATABASE_BILLING_ADDRESS_STREET = "billing_address_street"; //$NON-NLS-1$
	public static final String DATABASE_BILLING_ADDRESS_CITY = "billing_address_city"; //$NON-NLS-1$
	public static final String DATABASE_BILLING_ADDRESS_STATE = "billing_address_state"; //$NON-NLS-1$
	public static final String DATABASE_BILLING_ADDRESS_POSTAL_CODE = "billing_address_postal_code"; //$NON-NLS-1$
	public static final String DATABASE_BILLING_ADDRESS_COUNTRY = "billing_address_country"; //$NON-NLS-1$
	public static final String DATABASE_PRIMARY_ADDRESS_STREET = "primary_address_street"; //$NON-NLS-1$
	public static final String DATABASE_PRIMARY_ADDRESS_CITY = "primary_address_city"; //$NON-NLS-1$
	public static final String DATABASE_PRIMARY_ADDRESS_STATE = "primary_address_state"; //$NON-NLS-1$
	public static final String DATABASE_PRIMARY_ADDRESS_POSTAL_CODE = "primary_address_postalcode"; //$NON-NLS-1$
	public static final String DATABASE_PRIMARY_ADDRESS_COUNTRY = "primary_address_country"; //$NON-NLS-1$  
	public static final String DATABASE_PHYSICAL_ADDRESS_STREET = "pri_physical_street"; //$NON-NLS-1$
	public static final String DATABASE_PHYSICAL_ADDRESS_CITY = "pri_physical_city"; //$NON-NLS-1$
	public static final String DATABASE_PHYSICAL_ADDRESS_STATE = "pri_physical_state"; //$NON-NLS-1$
	public static final String DATABASE_PHYSICAL_ADDRESS_POSTAL_CODE = "pri_physical_postalcode"; //$NON-NLS-1$
	public static final String DATABASE_PHYSICAL_ADDRESS_COUNTRY = "pri_physical_country"; //$NON-NLS-1$
	public static final String DATABASE_CITY = "city"; //$NON-NLS-1$
	public static final String DATABASE_STATE = "state"; //$NON-NLS-1$
	public static final String DATABASE_COUNTRY = "country"; //$NON-NLS-1$
	public static final String DATABASE_PHONE_OFFICE = "phone_office"; //$NON-NLS-1$
	public static final String DATABASE_PHONE_WORK = "phone_work"; //$NON-NLS-1$
	public static final String DATABASE_PHONE_WORK_OPT_OUT = "phone_work_optout"; //$NON-NLS-1$
	public static final String DATABASE_PHONE_MOBILE = "phone_mobile"; //$NON-NLS-1$
	public static final String DATABASE_PHONE_MOBILE_OPT_OUT = "phone_mobile_optout"; //$NON-NLS-1$ 
	public static final String DATABASE_PHONE_FAX = "phone_fax"; //$NON-NLS-1$
	public static final String DATABASE_EMAIL = "email"; //$NON-NLS-1$
	public static final String DATABASE_EMAIL_ADDRESS = "email1"; //$NON-NLS-1$
	public static final String DATABASE_EMAIL_OPT_OUT = "email1_optout"; //$NON-NLS-1$
	public static final String DATABASE_TITLE = "title"; //$NON-NLS-1$  
	public static final String DATABASE_OPPORTUNITY_OWNER = "opportunityowner"; //$NON-NLS-1$
	public static final String DATABASE_DATE_CLOSED = "date_closed"; //$NON-NLS-1$
	public static final String DATABASE_AMOUNT = "amount"; //$NON-NLS-1$
	public static final String DATABASE_DESCRIPTION = "description"; //$NON-NLS-1$
	public static final String DATABASE_ACCOUNT_ID = "accountId"; //$NON-NLS-1$
	public static final String DATABASE_INDUSTRY = "industry"; //$NON-NLS-1$
	public static final String DATABASE_INDUS_CLASS_ROLLUP = "indus_class_rollup"; //$NON-NLS-1$
	// 80623
	public static final String DATABASE_INDUS_INDUSTRY = "indus_industry"; //$NON-NLS-1$
	public static final String DATABASE_INDUS_CLASS_NAME = "indus_class_name"; //$NON-NLS-1$
	
	public static final String DATABASE_CLIENT_ID = "clientid"; //$NON-NLS-1$
	// Web service returns a different client id constant for the typeahead service
	public static final String DATABASE_CLIENT_ID_WITH_UNDERSCORE = "client_id"; //$NON-NLS-1$
	public static final String DATABASE_CCMS_ID = "ccms_id"; //$NON-NLS-1$
	
	public static final String DATABASE_SALES_STAGE = "sales_stage"; //$NON-NLS-1$
	public static final String DATABASE_ASSIGNED_USER_NAME = "assigned_user_name"; //$NON-NLS-1$
	public static final String DATABASE_ASSIGNED_USER_EMAIL = "assigned_user_email"; //$NON-NLS-1$
	public static final String DATABASE_ASSIGNED_USER_ID = "assigned_user_id"; //$NON-NLS-1$
	public static final String DATABASE_ACCOUNT_OPPORTUNITIES = "opportunities"; //$NON-NLS-1$
	public static final String DATABASE_TAGS = "tags"; //$NON-NLS-1$
	public static final String DATABASE_PRIMARY_CONTACT_ID = "primary_contact_id"; //$NON-NLS-1$
	public static final String DATABASE_PRIMARY_CONTACT_NAME = "primary_contact_name"; //$NON-NLS-1$
	public static final String DATABASE_REVENUE_LINE_ITEMS = "revenue_line_items"; //$NON-NLS-1$
	public static final String DATABASE_LEVEL15 = "level15"; //$NON-NLS-1$
	public static final String DATABASE_BILL_DATE = "bill_date"; //$NON-NLS-1$
	public static final String DATABASE_LAST_MODIFIED_DATE = "last_modified_date"; //$NON-NLS-1$
	public static final String DATABASE_FOLLOWED = "followed"; //$NON-NLS-1$
	public static final String DATABASE_ISPARENT = "isParent"; //$NON-NLS-1$
	public static final String DATABASE_RELATEDCLIENTS = "relatedClients"; //$NON-NLS-1$
	public static final String DATABASE_PARENTFOLLOWINFO = "parentFollowInfo"; //$NON-NLS-1$
	public static final String DATABASE_PARENTLINK = "parentLink"; //$NON-NLS-1$

	public static final String DATABASE_OPPORTUNITIES_TOTAL = "opportunitiesTotal"; //$NON-NLS-1$
	public static final String DATABASE_TOTALCOUNT = "totalCount"; //$NON-NLS-1$
	public static final String DATABASE_FIELDS = "fields"; //$NON-NLS-1$

	// Web service parameter constants
	public static final String FOLLOW = "Follow"; //$NON-NLS-1$
	public static final String UNFOLLOW = "Unfollow"; //$NON-NLS-1$

	// Industry codes (defined by this BDS http://w3.ibm.com/standards/xml/approved/ibmww/ci/bds/Profile_Account/IndustryClass_0100.html)
	public static final String INDUSTRY_AEROSPACE = "V"; //$NON-NLS-1$
	public static final String INDUSTRY_AUTOMOTIVE = "J"; //$NON-NLS-1$
	public static final String INDUSTRY_BANKING = "F"; //$NON-NLS-1$
	public static final String INDUSTRY_CHEMICALS = "P"; //$NON-NLS-1$
	public static final String INDUSTRY_COMPUTER_SERVICES = "B"; //$NON-NLS-1$
	public static final String INDUSTRY_CONSUMER_PRODUCTS = "D"; //$NON-NLS-1$
	public static final String INDUSTRY_CONSULTING = "C"; //$NON-NLS-1$
	public static final String INDUSTRY_EDUCATION = "E"; //$NON-NLS-1$
	public static final String INDUSTRY_ELECTRONICS = "L"; //$NON-NLS-1$
	public static final String INDUSTRY_ENERGY = "U"; //$NON-NLS-1$
	public static final String INDUSTRY_EXCLUSIONS = "Z"; //$NON-NLS-1$
	public static final String INDUSTRY_FINANCE = "S"; //$NON-NLS-1$
	public static final String INDUSTRY_GOVERNMENT_FEDERAL = "Y"; //$NON-NLS-1$
	public static final String INDUSTRY_GOVERNMENT_LOCAL = "G"; //$NON-NLS-1$
	public static final String INDUSTRY_HEALTHCARE = "H"; //$NON-NLS-1$
	public static final String INDUSTRY_INDUSTRIAL = "M"; //$NON-NLS-1$
	public static final String INDUSTRY_INSURANCE = "N"; //$NON-NLS-1$
	public static final String INDUSTRY_LIFE_SCIENCE = "X"; //$NON-NLS-1$
	public static final String INDUSTRY_MEDIA = "K"; //$NON-NLS-1$
	public static final String INDUSTRY_RETAIL = "R"; //$NON-NLS-1$
	public static final String INDUSTRY_TELECOM = "A"; //$NON-NLS-1$
	public static final String INDUSTRY_TRANSPORTATION = "T"; //$NON-NLS-1$
	public static final String INDUSTRY_TRAVEL = ""; //$NON-NLS-1$
	public static final String INDUSTRY_WHOLESALE_DISTRIBUTION = "W"; //$NON-NLS-1$
	public static final String INDUSTRY_OTHER = "Other"; //$NON-NLS-1$

	// HTTP Proxy constants
	public static final int HTTP_PROXY_PORT = 59451;

}
