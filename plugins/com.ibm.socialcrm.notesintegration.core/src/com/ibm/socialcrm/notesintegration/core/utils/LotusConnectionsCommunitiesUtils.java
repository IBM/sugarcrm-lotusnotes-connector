package com.ibm.socialcrm.notesintegration.core.utils;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 ***************************************************************/

public class LotusConnectionsCommunitiesUtils {
	//  public static String ACCOUNTS_COMMUNITY_ID = "991055d1-49ca-4d31-86f5-bfd4f4390917"; //$NON-NLS-1$
	//  public static String OPPORTUNITIES_COMMUNITY_ID = "6b85fc62-4e94-4066-a799-a9ee3137b842"; //$NON-NLS-1$
	//  
	// private static ConnectionsSocialImpl connectionsSocialImpl;
	// private static String photosTempLocation;
	// private static Map<String, Post> cachedPosts;
	//
	// public static CommunityServiceIf getAccountsCommunity()
	// {
	// CommunityServiceIf accountsCommunity = null;
	// try
	// {
	// accountsCommunity = getConnectionsSocialImpl().getCommunitiesService()
	// .retrieve(ACCOUNTS_COMMUNITY_ID);
	// }
	// catch (SocialAPIException e1)
	// {
	// UtilsPlugin.getDefault().logException(e1, CorePluginActivator.PLUGIN_ID);
	// }
	// return accountsCommunity;
	// }
	//
	// public static CommunityServiceIf getOpportunitiesCommunity()
	// {
	// CommunityServiceIf opportunitiesCommunity = null;
	// try
	// {
	// opportunitiesCommunity = getConnectionsSocialImpl().getCommunitiesService()
	// .retrieve(OPPORTUNITIES_COMMUNITY_ID);
	// }
	// catch (SocialAPIException e1)
	// {
	// UtilsPlugin.getDefault().logException(e1, CorePluginActivator.PLUGIN_ID);
	// }
	// return opportunitiesCommunity;
	// }
	//
	// //TODO: Add a cache/refresh strategy for some of these operations
	// public static List<ForumTopicIf> getTopics(SugarType type)
	// {
	// List<ForumTopicIf> topics = new ArrayList<ForumTopicIf>();
	//
	// if (type == SugarType.ACCOUNTS || type == SugarType.ALL)
	// {
	// CommunityServiceIf accountsCommunity = getAccountsCommunity();
	// List<ForumTopicIf> accountTopics = null;
	// try
	// {
	// if (accountsCommunity != null
	// && (accountTopics = accountsCommunity.getForumService().listTopics(0, -1)) != null)
	// {
	// topics.addAll(accountTopics);
	// }
	// }
	// catch (SocialAPIException e)
	// {
	// UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
	// }
	// }
	// if (type == SugarType.OPPORTUNITIES || type == SugarType.ALL)
	// {
	// CommunityServiceIf opportunitiesCommunity = getOpportunitiesCommunity();
	// List<ForumTopicIf> opportunityTopics = null;
	// try
	// {
	// if (opportunitiesCommunity != null
	// && (opportunityTopics = opportunitiesCommunity.getForumService().listTopics(0, -1)) != null)
	// {
	// topics.addAll(opportunityTopics);
	// }
	// }
	// catch (SocialAPIException e)
	// {
	// UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
	// }
	// }
	// return topics;
	// }
	//
	// /**
	// * Returns the first topic found with the given name.
	// * @param name
	// * @return
	// */
	// public static ForumTopicIf getTopic(String name)
	// {
	// ForumTopicIf theTopic = null;
	// for (ForumTopicIf topic : getTopics(SugarType.ALL))
	// {
	// if (topic.getTitle().equals(name))
	// {
	// theTopic = topic;
	// break;
	// }
	// }
	// return theTopic;
	// }
	//
	// /**
	// * Returns the topic type.
	// * @param name
	// * @return SugarType.OPPORTUNITIES, SugarType.ACCOUNTS or null if the topic was not found.
	// */
	// public static SugarType getTopicType(String name)
	// {
	// SugarType type = null;
	// for (ForumTopicIf topic : getTopics(SugarType.ACCOUNTS))
	// {
	// if (topic.getTitle().equals(name))
	// {
	// type = SugarType.ACCOUNTS;
	// }
	// }
	// if (type == null)
	// {
	// for (ForumTopicIf topic : getTopics(SugarType.OPPORTUNITIES))
	// {
	// if (topic.getTitle().equals(name))
	// {
	// type = SugarType.OPPORTUNITIES;
	// }
	// }
	// }
	// return type;
	// }
	//
	// public static void createTopic(SugarType type, String title, String content, boolean isPinned)
	// {
	// try
	// {
	// ForumServiceIf forumServiceIf = type == SugarType.OPPORTUNITIES ? getOpportunitiesCommunity().getForumService()
	// : getAccountsCommunity().getForumService();
	// forumServiceIf.createTopic(title, content, isPinned);
	// }
	// catch (SocialAPIException e)
	// {
	// UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
	// }
	// }
	//
	// public static ForumItemIf postToServer(SugarType type, ForumItemIf parent, String title, String content)
	// {
	// ForumItemIf item = null;
	// try
	// {
	// ForumServiceIf forumServiceIf = type == SugarType.OPPORTUNITIES ? getOpportunitiesCommunity().getForumService()
	// : getAccountsCommunity().getForumService();
	//      item = forumServiceIf.createReply(parent, title, content, "text/plain"); //Note, the last argument can also be HTML //$NON-NLS-1$
	// }
	// catch (SocialAPIException e)
	// {
	// UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
	// }
	// return item;
	// }
	//
	// /**
	// * Returns posts for a particular topic.
	// * @param topicName - The name of the topic
	// * @param getUpdates - if true, then get the latest posts from the server, otherwise return the cached posts.
	// * @return
	// */
	// public static Post getPosts(String topicName, boolean getUpdates)
	// {
	// Post post = null;
	// if (!getUpdates && getCachedPosts().containsKey(topicName))
	// {
	// post = getCachedPosts().get(topicName);
	// }
	// else
	// {
	// ForumTopicIf topic = getTopic(topicName);
	// SugarType type = getTopicType(topicName);
	//
	// if (topic != null && type != null)
	// {
	// List<Post> posts = new ArrayList<Post>();
	// post = LotusConnectionsCommunitiesUtils.createNewPostObject(topic, type);
	// posts.add(post);
	//
	// try
	// {
	// ForumServiceIf forumService = type == SugarType.OPPORTUNITIES ? getOpportunitiesCommunity().getForumService()
	// : getAccountsCommunity().getForumService();
	//
	// List<ForumReplyIf> replies = forumService.listReplies(topic, -1, 500);
	//
	// for (ForumReplyIf reply : replies)
	// {
	// ForumItemIf parentItem = forumService.getParentItem(reply);
	// if (parentItem != null)
	// {
	// Post parentPost = getPostFromForumItem(posts, parentItem);
	// if (parentPost == null)
	// {
	// parentPost = LotusConnectionsCommunitiesUtils.createNewPostObject(parentItem, type);
	// posts.add(parentPost);
	// }
	//
	// Post childPost = getPostFromForumItem(posts, reply);
	// if (childPost == null)
	// {
	// childPost = LotusConnectionsCommunitiesUtils.createNewPostObject(reply, type);
	// posts.add(childPost);
	// }
	// parentPost.getChildPosts().add(childPost);
	// }
	// }
	// getCachedPosts().put(topicName, post);
	// }
	// catch (SocialAPIException e)
	// {
	// UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
	// }
	//
	// }
	// }
	//
	// return post;
	// }
	//
	// public static void printPosts(Post post)
	// {
	// if (post != null)
	// {
	// for (Post childPost : post.getChildPosts())
	// {
	// printPosts(childPost);
	// }
	// }
	// }
	//
	// public static Post getPostFromForumItem(List<Post> posts, ForumItemIf item)
	// {
	// Post post = null;
	// if (posts != null)
	// {
	// for (Post p : posts)
	// {
	// if (p.getForumItem().getId().equals(item.getId()))
	// {
	// post = p;
	// break;
	// }
	// }
	// }
	// return post;
	// }
	//
	// public static void downloadProfilePhoto(String userid)
	// {
	// String downloadLocation = getPhotosTempLocation() + userid;
	// File photo = new File(downloadLocation);
	// if (!photo.exists())
	// {
	//      String FILES_URI = NotesAccountManager.getInstance().getSocialServer() + "/profiles/photo.do?userid=" + userid; //$NON-NLS-1$
	//
	// Abdera abdera = Abdera.getInstance();
	// AbderaClient client = new AbderaClient(abdera);
	// try
	// {
	//        client.addCredentials(null, null, "basic", AbderaConnectionsFileOperations.getCredentials()); //$NON-NLS-1$
	// ClientResponse resp = client.get(FILES_URI);
	// if (resp.getType() == ResponseType.SUCCESS)
	// {
	// InputStream in = resp.getInputStream();
	//
	// FileOutputStream out = null;
	// try
	// {
	// if (in != null)
	// {
	// out = new FileOutputStream(downloadLocation);
	//
	// byte[] buffer = new byte[4096];
	// int bytesRead = 0;
	// while ((bytesRead = in.read(buffer)) != -1)
	// {
	// out.write(buffer, 0, bytesRead);
	// }
	// out.flush();
	// out.close();
	// }
	// }
	// catch (Exception e)
	// {
	// UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
	// }
	// finally
	// {
	// try
	// {
	// if (in != null)
	// {
	// in.close();
	// }
	// if (out != null)
	// {
	// out.close();
	// }
	// }
	// catch (Exception e)
	// {
	// //Eat it
	// }
	// }
	// }
	// }
	// catch (Exception e)
	// {
	// UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
	// }
	// }
	// }
	//
	// public static String getPhotosTempLocation()
	// {
	// if (photosTempLocation == null)
	// {
	// photosTempLocation = GenericUtils.getUniqueTempDir();
	// new File(photosTempLocation).mkdirs();
	// }
	// return photosTempLocation;
	// }
	//
	// public static List<CommunityServiceIf> getCommunities()
	// {
	// List<CommunityServiceIf> communities = null;
	// try
	// {
	// communities = getConnectionsSocialImpl().getCommunitiesService().list();
	// }
	// catch (SocialAPIException e)
	// {
	// UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
	// }
	// return communities;
	// }
	//
	// private static ConnectionsSocialImpl getConnectionsSocialImpl()
	// {
	// if (connectionsSocialImpl == null)
	// {
	// connectionsSocialImpl = new ConnectionsSocialImpl(NotesAccountManager.getInstance().getSocialServer(),
	// NotesAccountManager.getInstance().getSocialServerUser(),
	// NotesAccountManager.getInstance().getSocialServerPassword());
	// }
	// return connectionsSocialImpl;
	// }
	//
	// public static void resetConnectionsSocialImpl()
	// {
	// connectionsSocialImpl = null;
	// }
	//
	// public static Map<String, Post> getCachedPosts()
	// {
	// if (cachedPosts == null)
	// {
	// cachedPosts = new HashMap<String, Post>();
	// }
	// return cachedPosts;
	// }
	//
	// public static Post createNewPostObject(ForumItemIf forumItem, SugarType type)
	// {
	// LotusConnectionsCommunitiesUtils.downloadProfilePhoto(forumItem.getAuthor().getId());
	// Post newPost = new Post(forumItem, type);
	// return newPost;
	// }
}
