package com.ibm.socialcrm.notesintegration.utils;

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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.json.JSONObject;
import org.apache.commons.lang.WordUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.ibm.rcp.ui.browser.BrowserFactory;
import com.ibm.rcp.ui.browser.EmbeddedBrowser;
import com.ibm.rcp.ui.browser.launcher.BrowserLauncher;
import com.ibm.rcp.ui.launcher.SwitcherGroupInfo;
import com.ibm.rcp.ui.launcher.SwitcherParam;
import com.ibm.rcp.ui.launcher.SwitcherService;

public class GenericUtils {
	// We have to use look ahead to ensure that at least one number appear in the oppty number.
	// This will prevent us from matching hyphenated words.
	private static final String NUMERIC_LOOK_AHEAD = "(?=\\S*\\d)"; //$NON-NLS-1$

	// When we match opportunities in live text, we basically want to match an opportunity pattern surrounded by
	// a word boundary. However, "-" is considered a word boundary character which means that we can trigger matches
	// on phone numbers. So, we have a special word boundary that is a normal word boundary character
	// set without the dash.
	private static final String BOUNDARY_PREFIX = "(?<![A-Za-z0-9-])"; //$NON-NLS-1$
	public static final String BOUNDARY_SUFFIX = "(?![A-Za-z0-9-])"; //$NON-NLS-1$   
	public static final String REQUIRE_NUMBER_BOUNDARY_PREFIX = NUMERIC_LOOK_AHEAD + BOUNDARY_PREFIX;

	private static final String OPPORTUNITY_PATTERN_1 = REQUIRE_NUMBER_BOUNDARY_PREFIX + "[A-Za-z0-9]{2}-[A-Za-z0-9]{7}" //$NON-NLS-1$
			+ BOUNDARY_SUFFIX;
	private static final String OPPORTUNITY_PATTERN_2 = REQUIRE_NUMBER_BOUNDARY_PREFIX + "[A-Za-z0-9]{2}-[a-zA-Z0-9]{5,22}" //$NON-NLS-1$
			+ BOUNDARY_SUFFIX;
	private static final String OPPORTUNITY_PATTERN_3 = REQUIRE_NUMBER_BOUNDARY_PREFIX + "[A-Za-z0-9]{3}-[a-zA-Z0-9]{5,21}" //$NON-NLS-1$
			+ BOUNDARY_SUFFIX;
	private static final String OPPORTUNITY_PATTERN_4 = REQUIRE_NUMBER_BOUNDARY_PREFIX + "[0-9]{1}-[A-Za-z0-9]{6,7}" //$NON-NLS-1$
			+ BOUNDARY_SUFFIX;
	private static final String OPPORTUNITY_PATTERN_5 = REQUIRE_NUMBER_BOUNDARY_PREFIX + "(12KY|255U|12KZ)-[A-Za-z0-9]{5,7}" //$NON-NLS-1$
			+ BOUNDARY_SUFFIX;
	private static final String OPPORTUNITY_PATTERN_6 = REQUIRE_NUMBER_BOUNDARY_PREFIX + "(MOD|COG-S-)[0-9]{6}" + BOUNDARY_SUFFIX; //$NON-NLS-1$
	private static final String OPPORTUNITY_PATTERN_7 = REQUIRE_NUMBER_BOUNDARY_PREFIX + "ISS[0-9]{5}" + BOUNDARY_SUFFIX; //$NON-NLS-1$
	private static final String OPPORTUNITY_PATTERN_8 = REQUIRE_NUMBER_BOUNDARY_PREFIX + "ILOG-1-[A-Za-z0-9]{6}" + BOUNDARY_SUFFIX; //$NON-NLS-1$
	private static final String OPPORTUNITY_PATTERN_9 = REQUIRE_NUMBER_BOUNDARY_PREFIX + "US[0-9]{13}" + BOUNDARY_SUFFIX; //$NON-NLS-1$

	// public static final Pattern OPPORTUNITY_PATTERN = Pattern
	// .compile(OPPORTUNITY_PATTERN_1
	//					+ "|" + OPPORTUNITY_PATTERN_2 + "|" + OPPORTUNITY_PATTERN_3 + "|" + OPPORTUNITY_PATTERN_4 + "|" + OPPORTUNITY_PATTERN_5 + "|" + OPPORTUNITY_PATTERN_6 + "|" + OPPORTUNITY_PATTERN_7 + "|" + OPPORTUNITY_PATTERN_8 + "|" + OPPORTUNITY_PATTERN_9); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$

	// 43746 put all the hard coded pattern prefixes in an array. When building pattern entries for prefixes returned from BDS,
	// we will ignore those prefixes that already covered above (since our version will be more precise than this prefix+boilerplate system).
	public static final String[] OPPORTUNITY_HARDCODED_PATTERNS = new String[]{// expanded from OPPORTUNITY_PATTERN_4
	"0-", "1-", "2-", "3-", "4-", "5-", "6-", "7-", "8-", "9-", //$NON-NLS-1$,//$NON-NLS-2$,//$NON-NLS-3$,//$NON-NLS-4$,//$NON-NLS-5$,//$NON-NLS-6$,//$NON-NLS-7$,//$NON-NLS-8$,//$NON-NLS-9$,//$NON-NLS-10$
			// expanded from OPPORTUNITY_PATTERN_5
			"12KY", "255U", "12KZ", //$NON-NLS-1$,//$NON-NLS-2$,//$NON-NLS-3$,
			// expanded from OPPORTUNITY_PATTERN_6
			"MOD0", "MOD1", "MOD2", "MOD3", "MOD4", "MOD5", "MOD6", "MOD7", "MOD8", "MOD9", //$NON-NLS-1$,//$NON-NLS-2$,//$NON-NLS-3$,//$NON-NLS-4$,//$NON-NLS-5$,//$NON-NLS-6$,//$NON-NLS-7$,//$NON-NLS-8$,//$NON-NLS-9$,//$NON-NLS-10$
			"COG-S-0", "COG-S-1", "COG-S-2", "COG-S-3", "COG-S-4", "COG-S-5", "COG-S-6", "COG-S-7", "COG-S-8", "COG-S-9", //$NON-NLS-1$,//$NON-NLS-2$,//$NON-NLS-3$,//$NON-NLS-4$,//$NON-NLS-5$,//$NON-NLS-6$,//$NON-NLS-7$,//$NON-NLS-8$,//$NON-NLS-9$,//$NON-NLS-10$
			// expanded from OPPORTUNITY_PATTERN_7
			"ISS0", "ISS1", "ISS2", "ISS3", "ISS4", "ISS5", "ISS6", "ISS7", "ISS8", "ISS9", //$NON-NLS-1$,//$NON-NLS-2$,//$NON-NLS-3$,//$NON-NLS-4$,//$NON-NLS-5$,//$NON-NLS-6$,//$NON-NLS-7$,//$NON-NLS-8$,//$NON-NLS-9$,//$NON-NLS-10$
			// expanded from OPPORTUNITY_PATTERN_8
			"ILOG-1", //$NON-NLS-1$
			// expanded from OPPORTUNITY_PATTERN_9
			"US0", "US1", "US2", "US3", "US4", "US5", "US6", "US7", "US8", "US9"}; //$NON-NLS-1$,//$NON-NLS-2$,//$NON-NLS-3$,//$NON-NLS-4$,//$NON-NLS-5$,//$NON-NLS-6$,//$NON-NLS-7$,//$NON-NLS-8$,//$NON-NLS-9$,//$NON-NLS-10$
	public static final String OPPORTUNITY_PATTERNX = OPPORTUNITY_PATTERN_1
			+ "|" + OPPORTUNITY_PATTERN_2 + "|" + OPPORTUNITY_PATTERN_3 + "|" + OPPORTUNITY_PATTERN_4 + "|" + OPPORTUNITY_PATTERN_5 + "|" + OPPORTUNITY_PATTERN_6 + "|" + OPPORTUNITY_PATTERN_7 + "|" + OPPORTUNITY_PATTERN_8 + "|" + OPPORTUNITY_PATTERN_9; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
	public static Pattern OPPORTUNITY_PATTERN = null;

	private static final String SIX_DIGIT_LOOKAHEAD = "(?=.{0,5}\\d)"; //$NON-NLS-1$
	private static final String NINE_DIGIT_LOOKAHEAD = "(?=.{0,8}\\d)"; //$NON-NLS-1$

	private static final String ACCOUNT_PATTERN_1 = BOUNDARY_PREFIX + "[S|s]" + NINE_DIGIT_LOOKAHEAD + "[A-Za-z0-9]{9}" + BOUNDARY_SUFFIX; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String ACCOUNT_PATTERN_2 = BOUNDARY_PREFIX + "[S|s][C|c]" + SIX_DIGIT_LOOKAHEAD + "[A-Za-z0-9]{6}" + BOUNDARY_SUFFIX; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String ACCOUNT_PATTERN_3 = BOUNDARY_PREFIX + "[D|d][C|c]" + SIX_DIGIT_LOOKAHEAD + "[A-Za-z0-9]{6}" + BOUNDARY_SUFFIX; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String ACCOUNT_PATTERN_4 = BOUNDARY_PREFIX + "[G|g][C|c]" + SIX_DIGIT_LOOKAHEAD + "[A-Za-z0-9]{6}" + BOUNDARY_SUFFIX; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String ACCOUNT_PATTERN_5 = BOUNDARY_PREFIX + "[G|g][U|u]" + SIX_DIGIT_LOOKAHEAD + "[A-Za-z0-9]{6}" + BOUNDARY_SUFFIX; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String ACCOUNT_PATTERN_6 = BOUNDARY_PREFIX + "[D|d][B|b]" + SIX_DIGIT_LOOKAHEAD + "[A-Za-z0-9]{6}" + BOUNDARY_SUFFIX; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String ACCOUNT_PATTERN_7 = BOUNDARY_PREFIX + "[G|g][B|b]" + SIX_DIGIT_LOOKAHEAD + "[A-Za-z0-9]{6}" + BOUNDARY_SUFFIX; //$NON-NLS-1$ //$NON-NLS-2$
	// 80622 - ccms_id patterns
	//	public static final Pattern ACCOUNT_PATTERN = Pattern.compile(ACCOUNT_PATTERN_1 + "|" + ACCOUNT_PATTERN_2 + "|" //$NON-NLS-1$ //$NON-NLS-2$
	//			+ ACCOUNT_PATTERN_3 + "|" + ACCOUNT_PATTERN_4 + "|" + ACCOUNT_PATTERN_5 + "|" + ACCOUNT_PATTERN_6 + "|" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	//			+ ACCOUNT_PATTERN_7);   //$NON-NLS-1$
	private static final String ACCOUNT_PATTERN_8 = BOUNDARY_PREFIX + "[A-Za-z0-9]{6}-[0-9]{3}" + BOUNDARY_SUFFIX; //$NON-NLS-1$ 
	private static final String ACCOUNT_PATTERN_9 = BOUNDARY_PREFIX + "[A-Za-z0-9]{7}-[0-9]{3}" + BOUNDARY_SUFFIX; //$NON-NLS-1$
	// 97087 - add account ccms id pattern for DB-country
//	public static final Pattern ACCOUNT_PATTERN = Pattern.compile(ACCOUNT_PATTERN_1 + "|" + ACCOUNT_PATTERN_2 + "|" //$NON-NLS-1$ //$NON-NLS-2$
//			+ ACCOUNT_PATTERN_3 + '|' + ACCOUNT_PATTERN_8 + "|" + ACCOUNT_PATTERN_9); //$NON-NLS-1$ //$NON-NLS-2$
	private static final String ACCOUNT_PATTERN_10 = BOUNDARY_PREFIX + "[D|d][B|b][A-Za-z0-9]{6}-[A-Za-z0-9]{2}" + BOUNDARY_SUFFIX; //$NON-NLS-1$ //$NON-NLS-2$
	public static final Pattern ACCOUNT_PATTERN = Pattern.compile(ACCOUNT_PATTERN_1 + "|" + ACCOUNT_PATTERN_2 + "|" //$NON-NLS-1$ //$NON-NLS-2$
			+ ACCOUNT_PATTERN_3 + '|' + ACCOUNT_PATTERN_8 + "|" + ACCOUNT_PATTERN_9 + "|" + ACCOUNT_PATTERN_10); //$NON-NLS-1$ //$NON-NLS-2$

	
	private static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac"); //$NON-NLS-1$  //$NON-NLS-2$

	// Hyperlinks on the various platforms don't line up with labels quite the same, so we need to
	// add in these fudge factors in various places.
	public static final int MAC_HYPERLINK_VERTICAL_INDENT = 2;
	public static final int WINDOWS_HYPERLINK_VERTICAL_INDENT = -2;

	private static int browserInstanceCounter = 0;

	// With the new perspective based cards, some of the operations that need a workbenchWindow and call getActiveWorkbenchWindow
	// will fail because the window they get isn't the primary notes workbench window. This variable will hold a reference to that
	// so we have it around when we need it.
	private static IWorkbenchWindow mainNotesWindow = null;

	public enum SugarType {
		OPPORTUNITIES {
			@Override
			public String getDisplayName() {
				return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_OPPORTUNITY);
			}

			public String getSearchUrl(String searchItem) {
				return NotesAccountManager.getInstance().getCRMServer() + "index.php?module=Opportunities&action=index&searchFormTab=basic_search&query=true&name_basic=" + searchItem; //$NON-NLS-1$
			}

			public String getMultiSelectionTitle() {
				return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_OPPORTUNITY_TITLE);
			}

			public String getParentType() {
				return "Opportunities"; //$NON-NLS-1$
			}

			public String getType() {
				return "Opportunity"; //$NON-NLS-1$
			}
		},
		CONTACTS {
			@Override
			public String getDisplayName() {
				return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CONTACT);
			}

			public String getSearchUrl(String searchItem) {
				return NotesAccountManager.getInstance().getCRMServer() + "index.php?module=Contacts&action=index&searchFormTab=basic_search&query=true&search_name_basic=" + searchItem; //$NON-NLS-1$
			}

			public String getMultiSelectionTitle() {
				return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CONTACT_TITLE);
			}

			public String getParentType() {
				return "Contacts"; //$NON-NLS-1$
			}

			public String getType() {
				return "Contact"; //$NON-NLS-1$
			}

		},
		ACCOUNTS {
			@Override
			public String getDisplayName() {
				return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CLIENT);
			}

			public String getSearchUrl(String searchItem) {
				return NotesAccountManager.getInstance().getCRMServer() + "index.php?module=Accounts&action=index&searchFormTab=basic_search&query=true&name_basic=" + searchItem; //$NON-NLS-1$
			}

			public String getMultiSelectionTitle() {
				return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_ACCOUNT_TITLE);
			}

			public String getParentType() {
				return "Accounts"; //$NON-NLS-1$
			}

			public String getType() {
				return "Account"; //$NON-NLS-1$
			}
		},

		LEADS {
			@Override
			public String getDisplayName() {
				return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_LEAD);
			}

			public String getSearchUrl(String searchItem) {
				return NotesAccountManager.getInstance().getCRMServer() + "index.php?module=Leads&action=index&searchFormTab=basic_search&query=true&search_name_basic=" + searchItem; //$NON-NLS-1$
			}

			public String getMultiSelectionTitle() {
				return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_LEAD_TITLE);
			}

			public String getParentType() {
				return "Leads"; //$NON-NLS-1$
			}

			public String getType() {
				return "Lead"; //$NON-NLS-1$
			}

		},
		NONE, ALL;

		public String getDisplayName() {
			return ConstantStrings.EMPTY_STRING;
		}

		public String getSearchUrl(String searchItem) {
			return ConstantStrings.EMPTY_STRING;
		}

		/**
		 * Returns the parent type associated with a sugar type. Usually just a plural form of the base.
		 * 
		 * @return
		 */
		public String getParentType() {
			return ConstantStrings.EMPTY_STRING;
		}

		/**
		 * Returns the singular type associated with a sugar type
		 * 
		 * @return
		 */
		public String getType() {
			return ConstantStrings.EMPTY_STRING;
		}

		/**
		 * Returns the title that will be used for a given type when we find multiple matches and have to bring up a selection dialog. Pushing this function into the SugarType allows us to avoid
		 * having to deal with article subject agreement issues (a vs an).
		 * 
		 * @return
		 */
		public String getMultiSelectionTitle() {
			return ConstantStrings.EMPTY_STRING;
		}
	}

	public static String getDatabaseModuleName(SugarType sugarType) {
		return sugarType.equals(SugarType.OPPORTUNITIES) ? ConstantStrings.DATABASE_MODULE_OPPORTUNITIES : sugarType.equals(SugarType.CONTACTS) ? ConstantStrings.DATABASE_MODULE_CONTACTS : sugarType
				.equals(SugarType.ACCOUNTS) ? ConstantStrings.DATABASE_MODULE_ACCOUNTS :

		sugarType.equals(SugarType.LEADS) ? ConstantStrings.DATABASE_MODULE_LEADS :

		ConstantStrings.EMPTY_STRING;
	}

	public static String getFileContents(InputStream is) {
		String contents = ConstantStrings.EMPTY_STRING;
		if (is != null) {
			Writer writer = new StringWriter();
			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is, ConstantStrings.UTF8));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
				}
			}
			contents = writer.toString();
		}
		return contents;
	}

	public static String getFileContents(File file) {
		StringBuilder contents = new StringBuilder();

		try {
			BufferedReader input = new BufferedReader(new FileReader(file));
			try {
				String line = null; // not declared within while loop
				while ((line = input.readLine()) != null) {
					contents.append(line);
					contents.append(System.getProperty("line.separator")); //$NON-NLS-1$
				}
			} finally {
				input.close();
			}
		} catch (IOException ex) {
			UtilsPlugin.getDefault().logException(ex, UtilsPlugin.PLUGIN_ID);
		}

		return contents.toString().trim();
	}

	public static String getXpathValue(String xmlData, String expression, NamespaceContext namespaceContext) {
		XPathFactory xPathFactory = XPathFactory.newInstance();
		XPath xPath = xPathFactory.newXPath();
		if (namespaceContext != null) {
			xPath.setNamespaceContext(namespaceContext);
		}
		XPathExpression xPathExpression;
		String result = null;
		try {
			xPathExpression = xPath.compile(expression);
			result = xPathExpression.evaluate(new InputSource(new StringReader(xmlData)));
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
		}

		return result;
	}

	public static List<String> getXpathValues(NodeList nodes, String expression, NamespaceContext namespaceContext) {
		List<String> values = new ArrayList<String>();
		try {
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xPath = xPathFactory.newXPath();
			if (namespaceContext != null) {
				xPath.setNamespaceContext(namespaceContext);
			}
			for (int i = 0; i < nodes.getLength(); i++) {
				values.add(xPath.evaluate(expression, nodes.item(i)));
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
		}
		return values;
	}

	public static Map<String, String> getXpathValues(NodeList nodes, String keyExpression, String valueExpression, NamespaceContext namespaceContext) {
		Map<String, String> values = new HashMap<String, String>();
		try {
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xPath = xPathFactory.newXPath();
			if (namespaceContext != null) {
				xPath.setNamespaceContext(namespaceContext);
			}
			for (int i = 0; i < nodes.getLength(); i++) {
				values.put(xPath.evaluate(keyExpression, nodes.item(i)), xPath.evaluate(valueExpression, nodes.item(i)));
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
		}
		return values;
	}

	public static NodeList getXpathList(String xmlData, String expression, NamespaceContext namespaceContext) {
		Document doc = getXmlDocumentFromString(xmlData, true);
		NodeList nodes = null;
		if (doc != null) {
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xPath = xPathFactory.newXPath();
			if (namespaceContext != null) {
				xPath.setNamespaceContext(namespaceContext);
			}
			try {
				nodes = (NodeList) xPath.evaluate(expression, doc.getDocumentElement(), XPathConstants.NODESET);
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
			}
		}
		return nodes;
	}

	public static Document getXmlDocumentFromString(String s, boolean namespaceAware) {
		Document doc = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			if (namespaceAware) {
				dbf.setNamespaceAware(true);
			}
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(new ByteArrayInputStream(s.getBytes(ConstantStrings.UTF8)));
			doc.getDocumentElement().normalize();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
		}
		return doc;
	}

	public static void enableComposite(Composite composite, boolean enabled) {
		if (composite != null) {
			for (int i = 0; i < composite.getChildren().length; i++) {
				if (composite.getChildren()[i] instanceof Composite) {
					enableComposite((Composite) composite.getChildren()[i], enabled);
				} else {
					composite.getChildren()[i].setEnabled(enabled);
				}
			}
			composite.setEnabled(enabled);
		}
	}

	public static String getUniqueTempDir() {
		String targetDir = normalizePath(System.getProperty("java.io.tmpdir") + ConstantStrings.SLASH //$NON-NLS-1$
				+ ConstantStrings.SFA_TEMP_DIR + ConstantStrings.SLASH + System.currentTimeMillis(), true);
		while (new File(targetDir).exists()) {
			targetDir += "0"; //$NON-NLS-1$
		}
		return targetDir;
	}

	public static String normalizePath(String path, boolean appendSlash) {
		if (path != null) {
			path = path.replaceAll("\\\\", ConstantStrings.FORWARD_SLASH); //$NON-NLS-1$ 
			path = path.replaceAll("//", ConstantStrings.FORWARD_SLASH); //$NON-NLS-1$ 

			if (appendSlash && !path.endsWith(ConstantStrings.FORWARD_SLASH)) {
				path = path + ConstantStrings.FORWARD_SLASH;
			}
		}
		return path;
	}

	public static String wrapText(String text, int maxCharactersPerLine) {
		StringBuilder wrappedText = new StringBuilder();
		String delimiter = ConstantStrings.EMPTY_STRING;
		for (String line : text.trim().split(ConstantStrings.NEW_LINE)) {
			wrappedText.append(delimiter);
			delimiter = ConstantStrings.NEW_LINE;
			wrappedText.append(WordUtils.wrap(line, maxCharactersPerLine, ConstantStrings.NEW_LINE, true));
		}

		return wrappedText.toString();
	}

	/**
	 * Wraps the input string with a space on each side
	 * 
	 * @param str
	 * @return
	 */
	public static String padStringWithSpaces(String str) {
		return " " + str + " "; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static boolean isUseEmbeddedBrowserPreferenceSet() {
		IPreferencesService ipreferencesservice = Platform.getPreferencesService();
		IScopeContext aiscopecontext[] = {new InstanceScope(), new DefaultScope()};
		return ipreferencesservice.getBoolean("com.ibm.rcp.ui.browser.launcher", //$NON-NLS-1$
				"USE_EMBEDDED_BROWSER", //$NON-NLS-1$
				true, aiscopecontext);
	}

	/**
	 * Helper method to decode the data we get back from the SFARest web service. Turns &#XXX; into the appropriate character
	 * 
	 * @param jsonValue
	 * @return
	 */
	public static String decodeJSONValue(String jsonValue) {
		String newValue = jsonValue;
		Pattern pattern = Pattern.compile("&#(.*);"); //$NON-NLS-1$
		Matcher m = pattern.matcher(jsonValue);
		while (m.find()) {
			String characterEntity = m.group();
			char newChar = new Character((char) Integer.parseInt(m.group(1)));
			newValue = newValue.replaceAll(characterEntity, newChar + ""); //$NON-NLS-1$
		}
		return newValue;
	}

	/**
	 * Returns true if we're running on mac
	 * 
	 * @return
	 */
	public static boolean isMac() {
		return IS_MAC;
	}

	/**
	 * When working on different platforms, hyperlinks in a grid layout with will have different vertical alignment than labels IF the font sizes are different between the labels and the links. This
	 * can make for some weird looking forms if the UI is slightly off. These fudge factors allow us to account for that.
	 * 
	 * @return
	 */
	public static int getPlatformHyperlinkVerticalIndent() {
		return GenericUtils.isMac() ? GenericUtils.MAC_HYPERLINK_VERTICAL_INDENT : GenericUtils.WINDOWS_HYPERLINK_VERTICAL_INDENT;
	}

	/**
	 * Utility method to convert the given JSONObject into a map
	 * 
	 * @return
	 */
	public static Map<String, Object> JSONObjectToMap(JSONObject jsonObj) {
		Map map = new HashMap<String, Object>();
		if (jsonObj != null) {
			try {
				Iterator<String> iter = jsonObj.keys();
				while (iter.hasNext()) {
					String key = iter.next();
					Object obj = jsonObj.get(key);
					map.put(key, obj);
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
			}
		}
		return map;
	}

	public static void launchUrlInPreferredBrowser(final String url, final boolean setSugarLoginCookie) {
		Job job = new Job("Launching browser") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				if (isUseEmbeddedBrowserPreferenceSet() && setSugarLoginCookie) {
					// If the user has their preference set to launch an embedded browser, then we need to log them in.
					// Create a cookie with the credential information.
					final Cookie[][] cookies = new Cookie[1][];
					try {
						HttpClient client = new HttpClient();

						PostMethod post = new PostMethod(NotesAccountManager.getInstance().getCRMServer());
						post.addParameter(ConstantStrings.USER_NAME, NotesAccountManager.getInstance().getCRMUser());
						post.addParameter(ConstantStrings.USER_PASSWORD, NotesAccountManager.getInstance().getCRMPassword());
						post.addParameter(ConstantStrings.MODULE, ConstantStrings.USERS);
						post.addParameter(ConstantStrings.RETURN_ACTION, ConstantStrings.LOGIN);
						post.addParameter(ConstantStrings.RETURN_MODULE, ConstantStrings.USERS);
						post.addParameter(ConstantStrings.ACTION, ConstantStrings.AUTHENTICATE);
						post.addParameter(ConstantStrings.LOGIN, ConstantStrings.LOG_IN);
						client.executeMethod(post);

						cookies[0] = client.getState().getCookies();

						post.releaseConnection();
					} catch (Exception e) {
						UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
					}

					// A switcher service is a way to launch a view in a separate window or tab. This is an API
					// provided by Lotus Expeditor.
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							SwitcherService switcherservice = UtilsPlugin.getDefault().getSwitcherService();
							if (switcherservice != null) {
								String id = "com.ibm.socialcrm.notesintegration.browser" + browserInstanceCounter++; //$NON-NLS-1$

								SwitcherParam switcherparam = new SwitcherParam(GenericUtils.getMainNotesWindow());
								SwitcherGroupInfo switchergroupinfo = new SwitcherGroupInfo();
								switchergroupinfo.setGroupStyle(2);
								switcherparam.setGroupInfo(switchergroupinfo);
								switcherparam.setViewId("com.ibm.rcp.ui.browser.launcher.APPBrowserView"); //$NON-NLS-1$
								switcherparam.setSecondaryId(id);
								switcherparam.setMode(SwitcherParam.NEW_TAB);
								switcherparam.setPerspectiveId("com.ibm.rcp.ui.browser.launcher.PerspectiveFactory"); //$NON-NLS-1$
								try {
									switcherservice.show(switcherparam);
									EmbeddedBrowser browser = BrowserFactory.getBrowser(id);

									for (int i = 0; i < cookies[0].length; i++) {
										browser.setCookie(NotesAccountManager.getInstance().getCRMServer(), cookies[0][i].toExternalForm());
									}
									browser.setUrl(url);
								} catch (Exception e) {
									UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
								}
							}
						}
					});
				} else {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							String encodedUrl = url.replaceAll(" ", "+"); //$NON-NLS-1$ //$NON-NLS-2$
							BrowserLauncher.getLauncher().launchURLasDefault(encodedUrl);
						}
					});
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	/**
	 * Sets the mainNotesWindow to the current workbench window if it's not already set
	 */
	public static void establishMainNotesWindow() {
		if (mainNotesWindow == null) {
			mainNotesWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		}
	}

	public static IWorkbenchWindow getMainNotesWindow() {
		if (mainNotesWindow == null) {
			establishMainNotesWindow();
		}
		return mainNotesWindow;
	}
}
