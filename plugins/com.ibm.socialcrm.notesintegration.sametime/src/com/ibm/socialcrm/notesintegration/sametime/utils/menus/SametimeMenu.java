package com.ibm.socialcrm.notesintegration.sametime.utils.menus;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import com.ibm.collaboration.realtime.im.community.CommunityLifecycleEvent;
import com.ibm.collaboration.realtime.im.community.CommunityListener;
import com.ibm.collaboration.realtime.im.community.CommunityLoginEvent;
import com.ibm.collaboration.realtime.im.community.CommunityServiceEvent;
import com.ibm.collaboration.realtime.im.community.CommunityStatusEvent;
import com.ibm.collaboration.realtime.people.Person;
import com.ibm.socialcrm.notesintegration.sametime.utils.SametimeUtils;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public abstract class SametimeMenu {
	private Menu menu;
	private MenuItem chatItem;
	private MenuItem announceItem;
	private MenuItem addContactItem;
	private MenuItem callItem;

	/**
	 * Returns the set of people this menu should operate on
	 * 
	 * @return
	 */
	public abstract Set<Person> getSelectedPersons();

	public SametimeMenu(Shell shell, int style) {
		menu = new Menu(shell, style);

		chatItem = new MenuItem(menu, SWT.PUSH);
		chatItem.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SAMETIME_CHAT));
		chatItem.setImage(SFAImageManager.getImage(SFAImageManager.CHAT_ICON));
		chatItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				SametimeUtils.startChat(getSelectedPersons());
			}
		});

		announceItem = new MenuItem(menu, SWT.PUSH);
		announceItem.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SAMETIME_SEND_ANNOUNCEMENT));
		announceItem.setImage(SFAImageManager.getImage(SFAImageManager.ANNOUNCE_ICON));
		announceItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				SametimeUtils.openSendAnnouncementDialog(getSelectedPersons());
			}
		});

		addContactItem = new MenuItem(menu, SWT.PUSH);
		addContactItem.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SAMETIME_ADD_TO_CONTACT_LIST));
		addContactItem.setImage(SFAImageManager.getImage(SFAImageManager.SAMETIME_ADD_USER));
		addContactItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				SametimeUtils.showAddContactsDialog(getSelectedPersons());
			}
		});

		if (SametimeUtils.isSamtimeCallSupported()) {
			callItem = new MenuItem(menu, SWT.PUSH);
			callItem.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SAMETIME_CALL));
			callItem.setImage(SFAImageManager.getImage(SFAImageManager.CLICK_TO_CALL));

			// TODO - As long as the SametimeViewer is loaded after Sametime call features are loaded, then
			// this will be set to our desired behavior. We should be OK for now until we start docking
			// business cards.
			callItem.setEnabled(SametimeUtils.isSametimeServiceEnabled());
			callItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					SametimeUtils.startCall(getSelectedPersons());
				}
			});
		}

		updateMenuEnablement(chatItem, announceItem, addContactItem, callItem);

		final CommunityListener communityListener = new CommunityListener() {
			@Override
			public void handleCommunityLifecycleEvent(CommunityLifecycleEvent arg0) {
				refreshItemState();
			}

			@Override
			public void handleCommunityLoginEvent(CommunityLoginEvent arg0) {
				refreshItemState();
			}

			@Override
			public void handleCommunityServiceEvent(CommunityServiceEvent arg0) {
				refreshItemState();
			}

			@Override
			public void handleCommunityStatusEvent(CommunityStatusEvent arg0) {
				refreshItemState();
			}

			private void refreshItemState() {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						updateMenuEnablement(chatItem, announceItem, addContactItem, callItem);
					}
				});
			}
		};
		SametimeUtils.getCommunityService().addCommunityListener(communityListener);
		chatItem.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				SametimeUtils.getCommunityService().removeCommunityListener(communityListener);
			}
		});
	}

	private void updateMenuEnablement(MenuItem chatItem, MenuItem announceItem, MenuItem addContactItem, MenuItem callItem) {
		boolean isLoggedIn = SametimeUtils.isLoggedIn();

		chatItem.setEnabled(isLoggedIn);
		addContactItem.setEnabled(isLoggedIn);
		announceItem.setEnabled(isLoggedIn);
		if (callItem != null) {
			callItem.setEnabled(SametimeUtils.areCallsEnabled());
		}
	}

	public Menu getMenu() {
		return menu;
	}

}
