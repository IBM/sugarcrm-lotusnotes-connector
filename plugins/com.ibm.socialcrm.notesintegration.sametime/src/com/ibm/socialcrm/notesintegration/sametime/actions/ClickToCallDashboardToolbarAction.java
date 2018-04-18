package com.ibm.socialcrm.notesintegration.sametime.actions;

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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import com.ibm.collaboration.realtime.im.community.CommunityLifecycleEvent;
import com.ibm.collaboration.realtime.im.community.CommunityListener;
import com.ibm.collaboration.realtime.im.community.CommunityLoginEvent;
import com.ibm.collaboration.realtime.im.community.CommunityServiceEvent;
import com.ibm.collaboration.realtime.im.community.CommunityStatusEvent;
import com.ibm.rcp.swt.swidgets.SToolBar;
import com.ibm.rcp.swt.swidgets.SToolItem;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.AbstractToolbarIconContributionAction;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.ToolbarIconContribution;
import com.ibm.socialcrm.notesintegration.sametime.utils.SametimeUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class ClickToCallDashboardToolbarAction extends AbstractToolbarIconContributionAction {

	public ClickToCallDashboardToolbarAction(BaseSugarEntry sugarEntry, ToolbarIconContribution toolbarIconContribution) {
		super(sugarEntry, toolbarIconContribution);
	}

	@Override
	public void build(SToolBar toolbar) {
		if (hasBuildableParts()) {
			if (getMobilePhone() != null && getOfficePhone() != null) {
				final SToolItem callItem = new SToolItem(toolbar, SWT.DROP_DOWN);
				callItem.setImage(getEnabledIconImage());
				if (getDisabledIconImage() != null) {
					callItem.setDisabledImage(getDisabledIconImage());
				}
				refreshCallItemEnabledState(callItem);

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
								refreshCallItemEnabledState(callItem);
							}
						});
					}
				};
				SametimeUtils.getCommunityService().addCommunityListener(communityListener);
				callItem.addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent arg0) {
						SametimeUtils.getCommunityService().removeCommunityListener(communityListener);
					}
				});

				callItem.setToolTipText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CALL));
				callItem.addSelectionListener(new SelectionAdapter() {
					Menu dropMenu = null;

					@Override
					public void widgetSelected(SelectionEvent e) {
						if (dropMenu == null) {
							dropMenu = new Menu(Display.getDefault().getActiveShell(), SWT.POP_UP);
							Display.getDefault().getActiveShell().setMenu(dropMenu);

							MenuItem callOffice = new MenuItem(dropMenu, SWT.NONE);
							callOffice.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CALL_OFFICE));
							callOffice.setImage(SFAImageManager.getImage(SFAImageManager.CLICK_TO_CALL));
							callOffice.addSelectionListener(createClickToCallSelectionAdapter(getOfficePhone()));

							MenuItem callMobile = new MenuItem(dropMenu, SWT.NONE);
							callMobile.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CALL_MOBILE));
							callMobile.setImage(SFAImageManager.getImage(SFAImageManager.MOBILE_PHONE_ICON));
							callMobile.addSelectionListener(createClickToCallSelectionAdapter(getMobilePhone()));
						}

						final SToolItem toolItem = (SToolItem) e.widget;
						final SToolBar toolBar = toolItem.getParent();

						Point point = toolBar.toDisplay(new Point(e.x, e.y));
						dropMenu.setLocation(point.x, point.y);
						dropMenu.setVisible(true);
					}
				});
			} else {
				final SToolItem phoneItem = new SToolItem(toolbar, SWT.PUSH);
				phoneItem.setImage(getEnabledIconImage());
				if (getDisabledIconImage() != null) {
					phoneItem.setDisabledImage(getDisabledIconImage());
				}

				if (getMobilePhone() != null || getOfficePhone() != null) {
					String phone = getMobilePhone() != null ? getMobilePhone() : getOfficePhone();
					phoneItem.setEnabled(SametimeUtils.areCallsEnabled());
					phoneItem.addSelectionListener(createClickToCallSelectionAdapter(phone));
					phoneItem.setToolTipText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CALL));

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
									refreshCallItemEnabledState(phoneItem);
								}
							});
						}
					};
					SametimeUtils.getCommunityService().addCommunityListener(communityListener);
					phoneItem.addDisposeListener(new DisposeListener() {
						@Override
						public void widgetDisposed(DisposeEvent arg0) {
							SametimeUtils.getCommunityService().removeCommunityListener(communityListener);
						}
					});
				} else {
					phoneItem.setEnabled(false);
				}
			}
		}
	}

	private void refreshCallItemEnabledState(SToolItem callItem) {
		callItem.setEnabled(SametimeUtils.areCallsEnabled());
	}

	private SelectionAdapter createClickToCallSelectionAdapter(final String phone) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				SametimeUtils.startCall(phone);
			}
		};
	}

	@Override
	public boolean hasBuildableParts() {
		return getMobilePhone() != null || getOfficePhone() != null || getDisabledIconImage() != null;
	}

	private String getMobilePhone() {
		String mobilePhone = null;
		if (getSugarEntry() instanceof SugarContact) {
			SugarContact sugarContact = (SugarContact) getSugarEntry();
			if (sugarContact.getMobilePhone() != null && !sugarContact.getMobilePhone().equals(ConstantStrings.EMPTY_STRING)) {
				mobilePhone = sugarContact.getMobilePhone();
			}
		}
		return mobilePhone;
	}

	private String getOfficePhone() {
		String officePhone = null;
		if (getSugarEntry() instanceof SugarContact) {
			SugarContact sugarContact = (SugarContact) getSugarEntry();
			if (sugarContact.getOfficePhone() != null && !sugarContact.getOfficePhone().equals(ConstantStrings.EMPTY_STRING)) {
				officePhone = sugarContact.getOfficePhone();
			}
		}
		return officePhone;
	}
}
