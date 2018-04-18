package com.ibm.socialcrm.notesintegration.sametime.widgets;

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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import com.ibm.collaboration.realtime.im.community.CommunityLifecycleEvent;
import com.ibm.collaboration.realtime.im.community.CommunityListener;
import com.ibm.collaboration.realtime.im.community.CommunityLoginEvent;
import com.ibm.collaboration.realtime.im.community.CommunityServiceEvent;
import com.ibm.collaboration.realtime.im.community.CommunityStatusEvent;
import com.ibm.collaboration.realtime.people.Person;
import com.ibm.rcp.realtime.livenames.ui.HoverBusinessCard;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarOpportunity;
import com.ibm.socialcrm.notesintegration.sametime.SametimePluginActivator;
import com.ibm.socialcrm.notesintegration.sametime.utils.SametimeUtils;
import com.ibm.socialcrm.notesintegration.sametime.utils.menus.SametimeMenu;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFAHyperlink;

public class SametimeLinkCompositeBuilder {
	private HoverBusinessCard visibleCard;
	private PropertyChangeListener personChangeListener;
	private CommunityListener communityListener;
	private Label sametimeStatusLabel;
	private SFAHyperlink sametimeLink;
	private Person sametimePerson;
	private Font boldFont;
	private boolean sametimeItemSelected = false;

	private SametimeMenu sametimeMenu;

	public boolean createSametimeLinkComposite(final Composite composite, BaseSugarEntry entry) {
		boolean created = false;
		if (entry != null) {
			if (entry instanceof SugarOpportunity) {
				SugarOpportunity opportunity = (SugarOpportunity) entry;
				created = createSametimeLinkComposite(composite, opportunity.getAssignedUserName(), opportunity.getAssignedUserEmail());
			}
		}
		return created;
	}

	public boolean createSametimeLinkComposite(final Composite composite, String name, String email) {
		boolean created = false;

		if (email != null) {
			if (email == null || email.equals(ConstantStrings.EMPTY_STRING) || (sametimePerson = SametimeUtils.getSametimePerson(email)) == null) {
				// Just create a normal label since we have an assigned user name at least.
				Composite assignedUserComposite = new Composite(composite, SWT.NONE);
				assignedUserComposite.setLayout(GridLayoutFactory.fillDefaults().create());
				assignedUserComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
				Label assignedUserLabel = new Label(assignedUserComposite, SWT.NONE);
				assignedUserLabel.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
				assignedUserLabel.setText(name);
			} else {
				Composite hyperLinkComposite = new Composite(composite, SWT.NONE);
				hyperLinkComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).spacing(0, 0).create());
				hyperLinkComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).indent(0, GenericUtils.getPlatformHyperlinkVerticalIndent()).create());
				sametimeStatusLabel = new Label(hyperLinkComposite, SWT.NONE);
				sametimeStatusLabel.setLayoutData(GridDataFactory.fillDefaults().create());

				sametimeLink = new SFAHyperlink(hyperLinkComposite, SWT.NONE, true);
				sametimeLink.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).grab(false, false).create());
				sametimeLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
				sametimeLink.setText(name);
				sametimeLink.addListener(SWT.MouseUp, mouseUpListener());
				sametimeLink.addListener(SWT.FocusOut, deselectSametimeItemListener());
				sametimeLink.addListener(SWT.KeyDown, keyDownListener());
				sametimeLink.addListener(SWT.MouseHover, new Listener() {
					@Override
					public void handleEvent(Event arg0) {
						try {
							getVisibleCard().setLiveName(sametimePerson);
							Point cursorLocation = composite.getShell().getDisplay().getCursorLocation();

							// Don't show directly at the cursor location since we provide right click menu options.
							getVisibleCard().show(new Point(cursorLocation.x + 5, cursorLocation.y));
						} catch (Exception e) {
							UtilsPlugin.getDefault().logException(e, SametimePluginActivator.PLUGIN_ID);
						}
					}
				});
				sametimeLink.addListener(SWT.MouseDoubleClick, doubleClickListener());
				sametimeLink.addListener(SWT.MenuDetect, new Listener() {
					@Override
					public void handleEvent(Event arg0) {
						createMenu(composite);
					}
				});

				sametimePerson.addPropertyChangeListener(getPersonChangeListener());

				composite.addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent arg0) {
						sametimePerson.removePropertyChangeListener(getPersonChangeListener());
						getBoldFont().dispose();
					}
				});

				SametimeUtils.getCommunityService().addCommunityListener(getCommunityListener());
				sametimeLink.addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent arg0) {
						SametimeUtils.getCommunityService().removeCommunityListener(getCommunityListener());
					}
				});

				refreshSametimeStatus();
			}
			created = true;
		}

		return created;

	}

	private Listener doubleClickListener() {
		return new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				launchChatWindow();
			}
		};
	}

	private Listener keyDownListener() {
		return new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				if (arg0.keyCode == SWT.CR) {
					launchChatWindow();
				} else if (arg0.character == 32) // Space
				{
					selectSametimeItem();
				}
			}
		};
	}

	private void launchChatWindow() {
		SametimeUtils.startChat(new TreeSet() {
			{
				add(sametimePerson);
			}
		});
	}

	private Listener mouseUpListener() {
		return new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				selectSametimeItem();
			}
		};
	}

	private void selectSametimeItem() {
		sametimeLink.setBackground(SugarItemsDashboard.getInstance().getSametimeListingSelectionBackgroundColor());
		sametimeItemSelected = true;
		refreshSametimeStatus();
	}

	private Listener deselectSametimeItemListener() {
		return new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				sametimeLink.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));
				sametimeItemSelected = false;
				refreshSametimeStatus();
			}
		};
	}

	private void refreshSametimeStatus() {
		Image sametimeImage = SametimeUtils.getSametimeStatusImage(sametimePerson);
		if (sametimeImage == SFAImageManager.getImage(SFAImageManager.BLANK_ICON)) {
			sametimeStatusLabel.setVisible(false);
			((GridData) sametimeStatusLabel.getLayoutData()).exclude = true;
		} else {
			sametimeStatusLabel.setVisible(true);
			((GridData) sametimeStatusLabel.getLayoutData()).exclude = false;
		}
		sametimeStatusLabel.getParent().layout();

		sametimeStatusLabel.setImage(SametimeUtils.getSametimeStatusImage(sametimePerson));

		if (!sametimeItemSelected
				&& (sametimePerson.getStatus() == Person.STATUS_AVAILABLE || sametimePerson.getStatus() == Person.STATUS_AVAILABLE_MOBILE || sametimePerson.getStatus() == Person.STATUS_AWAY
						|| sametimePerson.getStatus() == Person.STATUS_AWAY_MOBILE || sametimePerson.getStatus() == Person.STATUS_IN_MEETING || sametimePerson.getStatus() == Person.STATUS_IN_MEETING_MOBILE)) {
			sametimeLink.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
		} else {
			sametimeLink.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		}

		if (sametimePerson.getStatus() == Person.STATUS_AVAILABLE || sametimePerson.getStatus() == Person.STATUS_AVAILABLE_MOBILE) {
			sametimeLink.setFont(getBoldFont());
		} else {
			sametimeLink.setFont(Display.getDefault().getSystemFont());
		}
	}

	private Font getBoldFont() {
		if (boldFont == null) {
			Font font = Display.getDefault().getSystemFont();
			boldFont = new Font(Display.getDefault(), new FontData(font.getFontData()[0].getName(), font.getFontData()[0].getHeight(), SWT.BOLD));
		}
		return boldFont;
	}

	private PropertyChangeListener getPersonChangeListener() {
		if (personChangeListener == null) {
			personChangeListener = new PropertyChangeListener() {
				@Override
				public void propertyChange(final PropertyChangeEvent evt) {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							refreshSametimeStatus();
						}
					});
				}
			};
		}
		return personChangeListener;
	}

	private HoverBusinessCard getVisibleCard() {
		if (visibleCard == null || visibleCard.isDisposed()) {
			visibleCard = SametimeUtils.getBusinessCard();
		}

		return visibleCard;
	}

	public CommunityListener getCommunityListener() {
		if (communityListener == null) {
			communityListener = new CommunityListener() {
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
							refreshSametimeStatus();
						}
					});
				}
			};

		}
		return communityListener;
	}

	private void createMenu(Composite composite) {
		if (sametimeMenu == null) {
			final Set<Person> personSet = new TreeSet<Person>();
			personSet.add(sametimePerson);

			sametimeMenu = new SametimeMenu(composite.getShell(), SWT.POP_UP) {
				@Override
				public Set<Person> getSelectedPersons() {
					return personSet;
				}
			};
		}
		sametimeMenu.getMenu().setVisible(true);
	}
}
