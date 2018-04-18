package com.ibm.socialcrm.notesintegration.core.utils;

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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.abdera.Abdera;
import org.apache.abdera.protocol.Response.ResponseType;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.eclipse.core.runtime.Preferences;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.DocumentInfo;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;

/**
 * Methods for interacting with Connections Files API
 * 
 */
public class AbderaConnectionsFileOperations {
	public static String connectionsURL;

	private static final String SALES_CONNECT_HTTP_HEADER = "SalesConnect/0.5"; //$NON-NLS-1$

	public static final String CONNECTIONS_SERVER_PREF_KEY = "com.ibm.socialcrm.sugarwidget.connectionsServer"; //$NON-NLS-1$

	public static final String CONNECTIONS_ERROR = "connections error"; //$NON-NLS-1$

	public static int _downloadStatus = 0;

	/**
	 * Get a list of folders
	 * 
	 * @return Collection of folders
	 */
	// public static synchronized List<ConnectionsFolder> getFolders()
	// {
	// int pageSize = 500;
	// int page = 1;
	//
	// boolean moreResults = true;
	//
	// List<ConnectionsFolder> folderList = new ArrayList<ConnectionsFolder>();
	//
	// while (moreResults)
	// {
	// String FILES_URI = NotesAccountManager.getInstance().getSocialServer()
	//          + "/files/basic/api/collections/feed?pageSize=" + pageSize + "&page=" + page;  //$NON-NLS-1$//$NON-NLS-2$
	//
	// Abdera abdera = Abdera.getInstance();
	// AbderaClient client = new AbderaClient(abdera);
	// try
	// {
	//        client.addCredentials(null, null, "basic", getCredentials()); //$NON-NLS-1$
	// ClientResponse resp = client.get(FILES_URI);
	// if (resp.getType() == ResponseType.SUCCESS)
	// {
	// String xml = getResponseData(resp);
	// if (xml != null)
	// {
	//            NodeList list = GenericUtils.getXpathList(xml, "/atom:feed/atom:entry", getDefaultNamespaceContext()); //$NON-NLS-1$
	//
	// if (list.getLength() <= 0)
	// {
	// moreResults = false;
	// }
	//
	// //This gives you a map of folder IDs to dipslay names
	// Map<String, String> idToNameMap = GenericUtils.getXpathValues(list,
	//                "td:uuid/text()", //$NON-NLS-1$
	//                "atom:title/text()", //$NON-NLS-1$
	// getDefaultNamespaceContext());
	//
	// Map<String, String> idToTypeMap = GenericUtils.getXpathValues(list,
	//                "td:uuid/text()", //$NON-NLS-1$
	//                "atom:summary/text()", //$NON-NLS-1$
	// getDefaultNamespaceContext());
	// for (String id : idToNameMap.keySet())
	// {
	// ConnectionsFolder folder = new ConnectionsFolder();
	// folder.setFolderId(id);
	// folder.setFolderName(idToNameMap.get(id));
	// String type = idToTypeMap.get(id);
	// if (type == null || type.length() == 0)
	// {
	// type = SugarType.NONE.toString();
	// }
	// boolean validType = false;
	// for (SugarType folderType : SugarType.values())
	// {
	// if (folderType.toString().equals(type))
	// {
	// validType = true;
	// }
	// }
	// if (validType)
	// {
	// folder.setType(SugarType.valueOf(type));
	// folderList.add(folder);
	// }
	// }
	// }
	// }
	// }
	// catch (Exception e1)
	// {
	// UtilsPlugin.getDefault().logException(e1, CorePluginActivator.PLUGIN_ID);
	// moreResults = false;
	// }
	// page = page + 1;
	// }
	// return folderList;
	// }
	/**
	 * Get a list of files in a given folder
	 * 
	 * @param folderId
	 *        uuid for folder
	 * @return map of files, key=uuid value=filename
	 */
	// public static Map<String, String> getFilesForFolder(String folderId)
	// {
	// Map<String, String> files = new HashMap<String, String>();
	//    String FILES_URI = NotesAccountManager.getInstance().getSocialServer() + "/files/basic/api/collection/" + folderId //$NON-NLS-1$
	//        + "/feed"; //$NON-NLS-1$
	// Abdera abdera = Abdera.getInstance();
	// AbderaClient client = new AbderaClient(abdera);
	// try
	// {
	//      client.addCredentials(null, null, "basic", getCredentials()); //$NON-NLS-1$
	// ClientResponse resp = client.get(FILES_URI);
	// if (resp.getType() == ResponseType.SUCCESS)
	// {
	// String xml = getResponseData(resp);
	// if (xml != null)
	// {
	//          NodeList list = GenericUtils.getXpathList(xml, "/atom:feed/atom:entry", getDefaultNamespaceContext()); //$NON-NLS-1$
	//          files = GenericUtils.getXpathValues(list, "td:uuid/text()", "td:label/text()", getDefaultNamespaceContext()); //$NON-NLS-1$ //$NON-NLS-2$
	// }
	// }
	// }
	// catch (Exception e1)
	// {
	// UtilsPlugin.getDefault().logException(e1, CorePluginActivator.PLUGIN_ID);
	// }
	//
	// return files;
	// }
	/**
	 * Creates a new folder in connections
	 * 
	 * @param folderName
	 *        name of folder
	 * @param folderType
	 *        OPPORTUNITIES, CONTACTS, ACCOUNTS, or NONE
	 * @param publicFolder
	 *        true if you want this folder to be public, false for private
	 * @return new folder's uuid
	 */
	// public static String createFolder(String folderName, String folderType, boolean publicFolder)
	// {
	// String folderId = null;
	//    String FILES_URI = NotesAccountManager.getInstance().getSocialServer() + "/files/basic/api/collections/feed"; //$NON-NLS-1$
	//
	//    String visibility = "public"; //$NON-NLS-1$
	// if (!publicFolder)
	// {
	//      visibility = "private"; //$NON-NLS-1$
	// }
	//
	// Abdera abdera = Abdera.getInstance();
	// AbderaClient client = new AbderaClient(abdera);
	//
	// //TODO: This is a temporary hack
	// folderName = folderName.replaceAll(ConstantStrings.GREATER_THAN, ConstantStrings.EMPTY_STRING);
	//
	// try
	// {
	//      client.addCredentials(null, null, "basic", getCredentials()); //$NON-NLS-1$
	//      String entry = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" //$NON-NLS-1$
	//          + "<atom:entry xmlns:snx=\"http://www.ibm.com/xmlns/prod/sn\" xmlns:td=\"urn:ibm.com/td\" xmlns:atom=\"http://www.w3.org/2005/Atom\">" //$NON-NLS-1$
	//          + "<atom:title type=\"text\"> " + "<![CDATA[" + folderName + "]]>" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	//          + "</atom:title>" //$NON-NLS-1$
	//
	// //TODO: unfortunately, API needs unique labels, and will NOT create one for you. this is stopgap.
	//          + "<td:label>" + "<![CDATA[" + folderName + getCredentials().getUserName() + "]]>" + "</td:label>" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	//
	//          + "<category scheme=\"tag:ibm.com,2006:td/type\" term=\"collection\" label=\"collection\"/>" //$NON-NLS-1$
	//          + "<atom:summary type=\"text\">" + folderType + "</atom:summary>" + "<td:visibility>" + visibility //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	//          + "</td:visibility>" + "</atom:entry>"; //$NON-NLS-1$ //$NON-NLS-2$
	// InputStream is = null;
	//
	// is = new ByteArrayInputStream(entry.getBytes(ConstantStrings.UTF8));
	//      ClientResponse resp = client.post(FILES_URI, new InputStreamRequestEntity(is, "application/atom+xml")); //$NON-NLS-1$
	//
	// if (resp.getType() == ResponseType.SUCCESS)
	// {
	// String xml = getResponseData(resp);
	// if (xml != null)
	// {
	//          folderId = GenericUtils.getXpathValue(xml, "/atom:entry/td:uuid/text()", getDefaultNamespaceContext()); //$NON-NLS-1$
	// }
	// }
	// }
	// catch (Exception e)
	// {
	// UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
	// }
	// return folderId;
	// }
	/**
	 * Upload a file to connections
	 * 
	 * @param fileToUpload
	 *        file being uploaded
	 * @param targetFilename
	 *        filename in connections
	 * @param folderId
	 *        uuid of folder where file is to be kept, if null, no folder
	 * @param publicFile
	 *        true if you want this file to be public, false for private
	 * @return uploaded file's uuid
	 */
	// public static String uploadFile(File fileToUpload, String targetFilename, String folderId, boolean publicFile)
	// {
	// String fileId = null;
	//
	//    String visibility = "public"; //$NON-NLS-1$
	// if (!publicFile)
	//      visibility = "private"; //$NON-NLS-1$
	//
	// String FILES_URI = NotesAccountManager.getInstance().getSocialServer()
	//        + "/files/basic/api/myuserlibrary/feed?visibility=" + visibility; //$NON-NLS-1$
	//
	// Abdera abdera = Abdera.getInstance();
	// AbderaClient client = new AbderaClient(abdera);
	// try
	// {
	//      client.addCredentials(null, null, "basic", getCredentials()); //$NON-NLS-1$
	//
	// RequestOptions options = new RequestOptions();
	// String contentType = new MimetypesFileTypeMap().getContentType(fileToUpload);
	//      if (contentType.equalsIgnoreCase("text/plain")) //$NON-NLS-1$
	// {
	//        contentType = "text/html"; //$NON-NLS-1$
	// }
	// options.setContentType(contentType);
	// //options.setSlug(fileToUpload.getName());
	// options.setSlug(targetFilename);
	//
	// FileInputStream fis = new FileInputStream(fileToUpload);
	//
	// RequestEntity entity = null;
	// if (contentType != null)
	// {
	// entity = new InputStreamRequestEntity(fis, contentType);
	// }
	// else
	// {
	// entity = new InputStreamRequestEntity(fis);
	// }
	//
	// ClientResponse resp = client.post(FILES_URI, entity, options);
	//
	// if (resp.getType() == ResponseType.SUCCESS)
	// {
	// String xml = getResponseData(resp);
	// if (xml != null)
	// {
	//          fileId = GenericUtils.getXpathValue(xml, "/atom:entry/td:uuid/text()", getDefaultNamespaceContext()); //$NON-NLS-1$
	// if (fileId != null && !fileId.equals(ConstantStrings.EMPTY_STRING) && folderId != null)
	// {
	// //Associate this file to a given folder
	// associateFileToFolder(fileId, folderId);
	// }
	// }
	// }
	// }
	// catch (Exception e1)
	// {
	// UtilsPlugin.getDefault().logException(e1, CorePluginActivator.PLUGIN_ID);
	// }
	// return fileId;
	// }
	/**
	 * Update an existing file
	 * 
	 * previously updateFile(String subscriberId, String fileId, File fileToUpload, String filename)
	 * 
	 * @param fileId
	 *        uuid for file in connections
	 * @param fileToUpload
	 *        replacement file
	 * @param targetFilename
	 *        filename in connections
	 * @return true if successful, false if not
	 */
	// public static boolean updateFile(String fileId, File fileToUpload, String targetFilename)
	// {
	// boolean success = false;
	//    String FILES_URI = NotesAccountManager.getInstance().getSocialServer() + "/files/basic/api/myuserlibrary/document/" //$NON-NLS-1$
	//        + fileId + "/media"; //$NON-NLS-1$
	//
	// Abdera abdera = Abdera.getInstance();
	// AbderaClient client = new AbderaClient(abdera);
	// try
	// {
	//      client.addCredentials(null, null, "basic", getCredentials()); //$NON-NLS-1$
	// RequestOptions options = new RequestOptions();
	// String contentType = new MimetypesFileTypeMap().getContentType(fileToUpload);
	//      if (contentType.equalsIgnoreCase("text/plain")) //$NON-NLS-1$
	// {
	//        contentType = "text/html"; //$NON-NLS-1$
	// }
	// options.setContentType(contentType);
	// //options.setContentType("application/atom+xml");
	// //options.setSlug(fileToUpload.getName());
	// options.setSlug(targetFilename);
	// InputStreamRequestEntity entity = new InputStreamRequestEntity(new FileInputStream(fileToUpload));
	// ClientResponse resp = client.put(FILES_URI, entity, options);
	//
	// success = resp.getType() == ResponseType.SUCCESS;
	// }
	// catch (Exception e)
	// {
	// UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
	// }
	// return success;
	// }
	/**
	 * Place a given file into a given folder
	 * 
	 * @param fileId
	 * @param folderId
	 * @return true if successful, false if not
	 */
	// public static boolean associateFileToFolder(String fileId, String folderId)
	// {
	// boolean success = false;
	//    String FILES_URI = NotesAccountManager.getInstance().getSocialServer() + "/files/basic/api/collection/" + folderId //$NON-NLS-1$
	//        + "/feed?itemId=" + fileId; //$NON-NLS-1$
	//
	// Abdera abdera = Abdera.getInstance();
	// AbderaClient client = new AbderaClient(abdera);
	// try
	// {
	//      client.addCredentials(null, null, "basic", getCredentials()); //$NON-NLS-1$
	// String entry = ConstantStrings.EMPTY_STRING;
	// InputStream is = null;
	//
	// try
	// {
	// is = new ByteArrayInputStream(entry.getBytes(ConstantStrings.UTF8));
	// }
	// catch (UnsupportedEncodingException e)
	// {
	// UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
	// }
	//
	// ClientResponse resp = client.post(FILES_URI, new InputStreamRequestEntity(is));
	// success = resp.getType() == ResponseType.SUCCESS;
	//
	// }
	// catch (Exception e1)
	// {
	// UtilsPlugin.getDefault().logException(e1, CorePluginActivator.PLUGIN_ID);
	// }
	//
	// return success;
	// }
	/**
	 * Download a file with a given file uuid
	 * 
	 * previously downloadFile(String subscriberId, String fileId)
	 * 
	 * @param fileId
	 * @return inputstream for file
	 */
	public static InputStream downloadFile(String fileId) {
		InputStream downloadStream = null;

		//    String FILES_URI = NotesAccountManager.getInstance().getSocialServer() + "/files/basic/api/myuserlibrary/document/" //$NON-NLS-1$
		//        + fileId + "/media"; //$NON-NLS-1$

		// acw - joe's test document - for testing
		// fileId = "https://devconnections46.rtp.raleigh.ibm.com/files/basic/api/library/767bcbf7-f190-41f7-92a5-81d8f67bfbf0/document/a7a73d84-d37a-48f3-b859-fe65e35fa2de/media";

		String FILES_URI = fileId;

		Abdera abdera = Abdera.getInstance();
		AbderaClient client = new AbderaClient(abdera);

		ClientResponse resp2 = null;
		try {
			client.addCredentials(null, null, "basic", getCredentials()); //$NON-NLS-1$
			RequestOptions ro = new RequestOptions();
			ro.addHeader("User-Agent", SALES_CONNECT_HTTP_HEADER); //$NON-NLS-1$
			resp2 = client.get(FILES_URI, ro);
			_downloadStatus = resp2.getStatus();

			if (resp2.getType() == ResponseType.SUCCESS) {
				downloadStream = resp2.getInputStream();
			}

		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			_downloadStatus = 999;
		}
		UtilsPlugin.getDefault().logInfoMessage(WebServiceLogUtil.getDebugMsg(resp2, FILES_URI, _downloadStatus), CorePluginActivator.PLUGIN_ID);
		System.out.println(WebServiceLogUtil.getDebugMsg(resp2, FILES_URI, _downloadStatus));
		return downloadStream;
	}

	/**
	 * Returns a list of file names that are "my files"
	 * 
	 * @return - Returns null if there is a connection error. A list of strings otherwise
	 */
	public static HashMap<String, DocumentInfo> getMyFiles() {
		InputStream myFilesStream = null;
		HashMap<String, DocumentInfo> files = new HashMap<String, DocumentInfo>();

		int batchSize = 100;
		int fileCtr = 0;
		int totalFiles = 0;
		int startIndex = 1;

		Abdera abdera = Abdera.getInstance();
		AbderaClient client = new AbderaClient(abdera);
		try {
			do {
				String URI = getConnectionsURL() + "/files/basic/api/myuserlibrary/feed?pageSize=" + batchSize + "&sI=" + startIndex; //$NON-NLS-1$ //$NON-NLS-2$
				client.addCredentials(null, null, "basic", getCredentials()); //$NON-NLS-1$
				RequestOptions ro = new RequestOptions();
				ro.addHeader("User-Agent", SALES_CONNECT_HTTP_HEADER); //$NON-NLS-1$
				ClientResponse resp2 = client.get(URI, ro);

				connectionsLog(new String[]{
						"\nConnecting to Connections ...\n", "URI:", URI, "\nResponseType:", ((resp2 == null || resp2.getType() == null || resp2.getType().name() == null) ? ConstantStrings.EMPTY_STRING : resp2.getType().name()), "\nResponseStatus:", ((resp2 == null) ? ConstantStrings.EMPTY_STRING : String.valueOf(resp2.getStatus())), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						"\nResponseStatusText:", ((resp2 == null || resp2.getStatusText() == null) ? ConstantStrings.EMPTY_STRING : resp2.getStatusText()), "\nUserID:", NotesAccountManager.getInstance().getSocialServerUser()}); //$NON-NLS-1$

				if (resp2.getType() == ResponseType.SUCCESS) {
					myFilesStream = resp2.getInputStream();
				} else
				// 41823 - if Connections returned error, save the error msg in the map and return
				{

					files.put(CONNECTIONS_ERROR, new DocumentInfo(((resp2 == null || resp2.getStatusText() == null) ? ConstantStrings.EMPTY_STRING : resp2.getStatusText()), null, URI));
					return files;
				}

				if (myFilesStream != null) {
					// Parse out the files from the response.
					DocumentBuilderFactory DOMFactory = DocumentBuilderFactory.newInstance();
					DOMFactory.setNamespaceAware(true);
					DocumentBuilder builder = DOMFactory.newDocumentBuilder();

					Document doc = builder.parse(myFilesStream);

					XPathFactory factory = XPathFactory.newInstance();
					XPath xpath = factory.newXPath();
					xpath.setNamespaceContext(getDefaultNamespaceContext());

					XPathExpression expr = xpath.compile("/*/*[local-name()='totalResults']/text()"); //$NON-NLS-1$
					Object result = expr.evaluate(doc, XPathConstants.NODE);
					Node node = (Node) result;
					totalFiles = Integer.parseInt(node.getNodeValue());
					connectionsLog(new String[]{"\nNumber of total files:", String.valueOf(totalFiles)}); //$NON-NLS-1$

					// Extract Connections UUID (td:uuid), file name ( td:label ), version ( td:versionLabel )
					expr = xpath.compile("/*/*[local-name()='entry']/td:label/text() |" + //$NON-NLS-1$ 
							" /*/*[local-name()='entry']/td:versionLabel/text() |" + //$NON-NLS-1$ 
							//	" /*/*[local-name()='entry']/author/text() |" + //$NON-NLS-1$ 
							" /*/*[local-name()='entry']/td:uuid/text()"); //$NON-NLS-1$  

					result = expr.evaluate(doc, XPathConstants.NODESET);
					NodeList nodes = (NodeList) result;
					for (int i = 0; i < nodes.getLength(); i += 3) {
						int index = i;
						DocumentInfo documentInfo = new DocumentInfo(nodes.item(index + 1).getNodeValue(), nodes.item(index + 2).getNodeValue(), nodes.item(index).getNodeValue());
						files.put(nodes.item(index + 1).getNodeValue(), documentInfo);
						fileCtr++;
						
						connectionsLog(new String[]{String.valueOf(fileCtr),
								". file name:", nodes.item(index + 1).getNodeValue(), ", version:", nodes.item(index + 2).getNodeValue(), ", UUID:", nodes.item(index).getNodeValue()}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
					}
				}
				startIndex += batchSize;
			} while (fileCtr < totalFiles && totalFiles > 0);
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			files = null;
		}
		return files;
	}

	/**
	 * Connections is strange about the way it handles files. Rather than having a real file system model, have a flat representation of all a users files. Folders aren't really folders, they're just
	 * tags on a file. So, this method will check the input list of files against the user's "My Files" in Connections. The return list will contain any duplicates. The caller can deal with that as
	 * appropriate.
	 * 
	 * @return List<File> - The list of duplicate files
	 */
	public static List<File> checkConnectionsForExistingFiles(List<File> inputFiles) {
		List<File> duplicates = new ArrayList<File>();

		HashMap<String, DocumentInfo> myFiles = getMyFiles();
		if (myFiles != null && !isErrorInMyFiles(myFiles)) {
			for (File file : inputFiles) {
				if (myFiles.containsKey(file.getName())) {
					duplicates.add(file);
				}
			}
		}

		return duplicates;
	}

	public static boolean isErrorInMyFiles(HashMap<String, DocumentInfo> myFiles) {
		boolean isError = false;
		if (myFiles != null && myFiles.size() == 1 && myFiles.containsKey(CONNECTIONS_ERROR)) {
			isError = true;
		}
		return isError;
	}
	/**
	 * Return default namespace context -- added NS URIs for connections
	 * 
	 * TODO: there are some URIs used for LotusLive but not for connections that should be removed
	 * 
	 * @return default namespace context
	 */
	public static NamespaceContext getDefaultNamespaceContext() {
		return new NamespaceContext() {
			public String getNamespaceURI(String prefix) {
				String namespaceUri = null;
				if (prefix.equals("snx")) //$NON-NLS-1$
				{
					namespaceUri = "http://www.ibm.com/xmlns/prod/sn"; //$NON-NLS-1$
				} else if (prefix.equals("cmisra")) //$NON-NLS-1$
				{
					namespaceUri = "http://docs.oasis-open.org/ns/cmis/restatom/200908/"; //$NON-NLS-1$
				} else if (prefix.equals("cmism")) //$NON-NLS-1$
				{
					namespaceUri = "http://docs.oasis-open.org/ns/cmis/messaging/200908/"; //$NON-NLS-1$
				} else if (prefix.equals("lcmis")) //$NON-NLS-1$
				{
					namespaceUri = "http://www.ibm.com/xmlns/prod/sn/cmis"; //$NON-NLS-1$
				} else if (prefix.equals("cmis")) //$NON-NLS-1$
				{
					namespaceUri = "http://docs.oasis-open.org/ns/cmis/core/200908/"; //$NON-NLS-1$
				} else if (prefix.equals("app")) //$NON-NLS-1$
				{
					namespaceUri = "http://www.w3.org/2007/app"; //$NON-NLS-1$
				} else if (prefix.equals("atom")) //$NON-NLS-1$
				{
					namespaceUri = "http://www.w3.org/2005/Atom"; //$NON-NLS-1$
				} else if (prefix.equals("td")) //$NON-NLS-1$
				{
					namespaceUri = "urn:ibm.com/td"; //$NON-NLS-1$
				} else if (prefix.equals("thr")) //$NON-NLS-1$
				{
					namespaceUri = "http://purl.org/syndication/thread/1.0"; //$NON-NLS-1$
				} else if (prefix.equals("opensearch")) //$NON-NLS-1$
				{
					namespaceUri = "http://a9.com/-/spec/opensearch/1.1/"; //$NON-NLS-1$
				}
				return namespaceUri;
			}

			public String getPrefix(String namespaceURI) {
				return null;
			}

			public Iterator getPrefixes(String namespaceURI) {
				return null;
			}
		};
	}

	private static UsernamePasswordCredentials credentials;

	/**
	 * Returns credentials to use for connections
	 * 
	 * @return connecitons credentials
	 */
	public static UsernamePasswordCredentials getCredentials() {
		if (credentials == null) {
			credentials = new UsernamePasswordCredentials(NotesAccountManager.getInstance().getSocialServerUser(), NotesAccountManager.getInstance().getSocialServerPassword());
		}
		return credentials;
	}

	/**
	 * Resets the credentials from the plugin preferences
	 */
	public static void resetCredentials() {
		credentials = null;
		getCredentials();
	}

	/**
	 * unchanged
	 * 
	 * @param response
	 * @return
	 */
	// private static String getResponseData(ClientResponse response)
	// {
	// String responseData = null;
	// InputStream is = null;
	// try
	// {
	// is = response.getInputStream();
	// if (is != null)
	// {
	// Writer writer = new StringWriter();
	//
	// char[] buffer = new char[1024];
	//
	// Reader reader = new BufferedReader(new InputStreamReader(is, ConstantStrings.UTF8));
	// int n;
	// while ((n = reader.read(buffer)) != -1)
	// {
	// writer.write(buffer, 0, n);
	// }
	// responseData = writer.toString();
	// }
	// }
	// catch (Exception e)
	// {
	// UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
	// }
	// finally
	// {
	// if (is != null)
	// {
	// try
	// {
	// is.close();
	// }
	// catch (Exception e)
	// {
	// }
	// }
	// }
	// return responseData;
	// }
	public static String getConnectionsURL() {
		if (connectionsURL == null) {
			Preferences preferences = CorePluginActivator.getDefault().getPluginPreferences();
			connectionsURL = preferences.getString(CONNECTIONS_SERVER_PREF_KEY);
		}
		return connectionsURL;
	}

	public static void setConnectionsURL(String connectionsURL) {
		AbderaConnectionsFileOperations.connectionsURL = connectionsURL;
	}

	public static void connectionsLog(String[] msgXs) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (msgXs != null && msgXs.length > 0) {
			for (int i = 0; i < msgXs.length; i++) {
				sb.append(msgXs[i]);
			}
		}
		UtilsPlugin.getDefault().logInfoMessage(sb.toString(), CorePluginActivator.PLUGIN_ID);
		// System.out.println(sb.toString());
	}
	
	public static void main(String[] args) {
	}
}
