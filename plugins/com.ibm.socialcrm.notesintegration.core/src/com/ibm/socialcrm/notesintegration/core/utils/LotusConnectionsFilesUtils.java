package com.ibm.socialcrm.notesintegration.core.utils;

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

public class LotusConnectionsFilesUtils {
	// private static FilesServiceIf filesServiceIf;
	//
	// //TODO THIS API WILL NOT CREATE PUBLIC FILES.
	// //TODO NEED TO ADD TO A COLLECTION THAT I DON'T OWN
	// public static String uploadFile(File fileToUpload, String targetFilename, String folderId)
	// {
	// String id = null;
	// try
	// {
	// FileItemIf fileItem = getConnectionsFilesService().create(new FileInputStream(fileToUpload), targetFilename);
	// if (fileItem != null)
	// {
	// Set<CollectionIf> collections = new TreeSet<CollectionIf>();
	// collections.addAll(getConnectionsFilesService().getMyCollections());
	// collections.addAll(getConnectionsFilesService().getCollectionsSharedByMe());
	// collections.addAll(getConnectionsFilesService().getCollectionsSharedWithMe());
	//        
	// for (CollectionIf collection : collections)
	// {
	// if (collection.getId().equals(folderId))
	// {
	// getConnectionsFilesService().addFileToCollection(collection, fileItem);
	// id = fileItem.getId();
	// break;
	// }
	// }
	// }
	// }
	// catch (Exception e)
	// {
	// UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
	// }
	//    
	// return id;
	// }
	//  
	// private static FilesServiceIf getConnectionsFilesService()
	// {
	// if (filesServiceIf == null)
	// {
	// try
	// {
	// SocialServiceIf connectionsServiceIf = new ConnectionsSocialImpl(NotesAccountManager.getInstance()
	// .getSocialServer(), NotesAccountManager.getInstance().getSocialServerUser(), NotesAccountManager
	// .getInstance().getSocialServerPassword());
	// filesServiceIf = connectionsServiceIf.getFilesService();
	// }
	// catch (SocialAPIException e)
	// {
	// UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
	// }
	// }
	// return filesServiceIf;
	// }
	//
	// public static void resetConnectionsFilesService()
	// {
	// filesServiceIf = null;
	// }
}
