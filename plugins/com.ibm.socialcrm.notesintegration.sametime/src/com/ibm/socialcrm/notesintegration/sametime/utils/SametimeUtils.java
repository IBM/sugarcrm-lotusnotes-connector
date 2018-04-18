package com.ibm.socialcrm.notesintegration.sametime.utils;

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

import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.ibm.collaboration.realtime.buddylist.BuddyListService;
import com.ibm.collaboration.realtime.im.community.CommunityService;
import com.ibm.collaboration.realtime.notifications.AnnouncementService;
import com.ibm.collaboration.realtime.people.IGroupService;
import com.ibm.collaboration.realtime.people.NestableGroup;
import com.ibm.collaboration.realtime.people.PeopleService;
import com.ibm.collaboration.realtime.people.Person;
import com.ibm.collaboration.realtime.people.PrivateGroup;
import com.ibm.collaboration.realtime.servicehub.ServiceException;
import com.ibm.collaboration.realtime.servicehub.ServiceHub;
import com.ibm.collaboration.realtime.telephony.CallFactory;
import com.ibm.collaboration.realtime.telephony.CallManager;
import com.ibm.rcp.realtime.livenames.ui.BusinessCardService;
import com.ibm.rcp.realtime.livenames.ui.HoverBusinessCard;
import com.ibm.socialcrm.notesintegration.sametime.SametimePluginActivator;
import com.ibm.socialcrm.notesintegration.sametime.utils.dialogs.CustomAddContactDialog;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;

public class SametimeUtils {
	private static AnnouncementService announcementService;
	private static CommunityService communityService;
	private static PeopleService peopleService;
	private static BusinessCardService businessCardService;

	/**
	 * This method checks to see if the base Sametime calling feature is supported.
	 * 
	 * @return
	 */
	public static boolean isSamtimeCallSupported() {
		boolean callFunctionSupported = true;
		try {
			// If we're running on ST 8.0, this won't be available
			Class.forName("com.ibm.collaboration.realtime.telephony.CallFactory").getMethod("createCallManager"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (Exception e) {
			callFunctionSupported = false;
		}
		return callFunctionSupported;
	}

	/**
	 * This method checks whether Sametime calling service (SUT) is enabled.
	 */
	public static boolean isSametimeServiceEnabled() {
		boolean isServiceAvailable = false;
		try {
			CallManager callManager = CallFactory.getInstance().createCallManager();

			// This checks that the user's ST is sufficiently new (8.5 or higher)
			callManager.getClass().getMethod("startUiCall", new Class[] { Person[].class, String.class }); //$NON-NLS-1$

			isServiceAvailable = callManager.isServiceAvailable();
		} catch (Exception e) {
			// Ignore
		}
		return isServiceAvailable;
	}

	public static Person getSametimePerson(String email) {
		Person person = null;
		if (email != null) {
			try {
				if (getPeopleService() != null && getCommunityService() != null) {
					person = getPeopleService().getPerson(email, getCommunityService().getDefaultCommunity().getId(), false);
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, SametimePluginActivator.PLUGIN_ID);
			}
		}
		return person;
	}

	public static Image getSametimeStatusImage(Person person) {
		Image img = SFAImageManager.getImage(SFAImageManager.BLANK_ICON);
		if (person != null) {
			// There seems to be a bug in 8.5 where the icons returned by person.getStatusImage() are 31x16. So
			// we provide our own icons
			int status = person.getStatus();
			if (status == Person.STATUS_OFFLINE) {
				img = SFAImageManager.getImage(SFAImageManager.BLANK_ICON);
			} else if (status == Person.STATUS_AVAILABLE) {
				img = SFAImageManager.getImage(SFAImageManager.ST_ACTIVE);
			} else if (status == Person.STATUS_AWAY) {
				img = SFAImageManager.getImage(SFAImageManager.ST_AWAY);
			} else if (status == Person.STATUS_IN_MEETING) {
				img = SFAImageManager.getImage(SFAImageManager.ST_MEETING);
			} else if (status == Person.STATUS_DND) {
				img = SFAImageManager.getImage(SFAImageManager.ST_DND);
			} else if (status == Person.STATUS_AVAILABLE_MOBILE) {
				img = SFAImageManager.getImage(SFAImageManager.ST_MOBILE_ACTIVE);
			} else if (status == Person.STATUS_AWAY_MOBILE) {
				img = SFAImageManager.getImage(SFAImageManager.ST_MOBILE_AWAY);
			} else if (status == Person.STATUS_IN_MEETING_MOBILE) {
				img = SFAImageManager.getImage(SFAImageManager.ST_MOBILE_MEETING);
			} else if (status == Person.STATUS_DND_MOBILE) {
				img = SFAImageManager.getImage(SFAImageManager.ST_MOBILE_DND);
			}
		}
		return img;
	}

	/**
	 * Creates a chat session for the currently selected people
	 */
	public static void startChat(Set<Person> personSet) {
		if (getPeopleService() != null) {
			if (personSet.size() == 1) {
				getPeopleService().createConversation(personSet.iterator().next());
			} else if (personSet.size() > 1) {
				getPeopleService().createNwayInvite(personSet.toArray(new Person[0]));
			}
		}
	}

	public static void openSendAnnouncementDialog(Set<Person> personSet) {
		if (getAnnouncementService() != null) {
			getAnnouncementService().openSendAnnouncementDialog(personSet.toArray(new Person[0]));
		}
	}

	public static void showAddContactsDialog(Set<Person> personSet) {
		CustomAddContactDialog dialog = new CustomAddContactDialog(Display.getDefault().getActiveShell());

		if (dialog.open() == Dialog.OK) {
			try {
				BuddyListService bls = (BuddyListService) ServiceHub.getService(BuddyListService.SERVICE_TYPE);
				IGroupService groupService = (IGroupService) ServiceHub.getService(IGroupService.SVC_TYPE);
				PrivateGroup targetGroup = null;
				if (dialog.getNewGroup() != null && !dialog.getNewGroup().equals(ConstantStrings.EMPTY_STRING)) {
					targetGroup = groupService.getNewPrivateGroup();
					targetGroup.setName(dialog.getNewGroup());
					if (dialog.createAsChild()) {
						if (dialog.getSelectedGroup() instanceof NestableGroup) {
							((NestableGroup) dialog.getSelectedGroup()).addSubGroup(targetGroup);
						}
					} else {
						bls.addRootGroup(targetGroup);
					}
				} else {
					targetGroup = dialog.getSelectedGroup();
				}

				for (Person person : personSet) {
					targetGroup.addPerson(person);
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, SametimePluginActivator.PLUGIN_ID);
			}
		}
	}

	public static void startCall(Set<Person> peopleSet) {
		try {
			CallManager callManager = CallFactory.getInstance().createCallManager();
			callManager.startUiCall(peopleSet.toArray(new Person[0]), null);
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, SametimePluginActivator.PLUGIN_ID);
		}
	}

	public static void startCall(String number) {
		try {
			CallManager callManager = CallFactory.getInstance().createCallManager();
			callManager.startUiCall(number);
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, SametimePluginActivator.PLUGIN_ID);
		}
	}

	public static HoverBusinessCard getBusinessCard() {
		HoverBusinessCard card = null;
		if (getBusinessCardService() != null) {
			card = getBusinessCardService().getHoverBusinessCard(Display.getDefault().getActiveShell());
		}
		return card;
	}

	public static AnnouncementService getAnnouncementService() {
		if (announcementService == null) {
			try {
				announcementService = (AnnouncementService) ServiceHub.getService(AnnouncementService.SERVICE_TYPE);
			} catch (ServiceException e) {
				UtilsPlugin.getDefault().logException(e, SametimePluginActivator.PLUGIN_ID);
			}
		}
		return announcementService;
	}

	public static PeopleService getPeopleService() {
		if (peopleService == null) {
			try {
				peopleService = (PeopleService) ServiceHub.getService(PeopleService.SERVICE_TYPE);
			} catch (ServiceException e) {
				UtilsPlugin.getDefault().logException(e, SametimePluginActivator.PLUGIN_ID);
			}
		}
		return peopleService;
	}

	public static CommunityService getCommunityService() {
		if (communityService == null) {
			try {
				communityService = (CommunityService) ServiceHub.getService(CommunityService.SERVICE_TYPE);
			} catch (ServiceException e) {
				UtilsPlugin.getDefault().logException(e, SametimePluginActivator.PLUGIN_ID);
			}
		}
		return communityService;
	}

	public static boolean isShowBusinessCardEnabled() {
		IPreferencesService ipreferencesservice = Platform.getPreferencesService();
		IScopeContext aiscopecontext[] = { new InstanceScope(), new DefaultScope() };
		return ipreferencesservice.getBoolean("com.ibm.collaboration.realtime.imhub", //$NON-NLS-1$
				"showHoverBizCard", //$NON-NLS-1$
				true, aiscopecontext);
	}

	public static BusinessCardService getBusinessCardService() {
		if (businessCardService == null) {
			try {
				businessCardService = (BusinessCardService) ServiceHub.getService(BusinessCardService.SERVICE_TYPE);
			} catch (ServiceException e) {
				UtilsPlugin.getDefault().logException(e, SametimePluginActivator.PLUGIN_ID);
			}
		}
		return businessCardService;
	}

	public static boolean areCallsEnabled() {
		return SametimeUtils.isSamtimeCallSupported() && SametimeUtils.isSametimeServiceEnabled() && isLoggedIn();
	}

	public static boolean isLoggedIn() {
		boolean loggedIn = false;
		CommunityService service = getCommunityService();
		if (service != null) {
			loggedIn = service.getDefaultCommunity().isLoggedIn();
		}
		return loggedIn;
	}

}
