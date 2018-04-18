package com.ibm.socialcrm.notesintegration.ui.dashboardpanels;

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
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.XMLMemento;

import com.ibm.rcp.ui.launcher.SwitcherGroupInfo;
import com.ibm.rcp.ui.launcher.SwitcherParam;
import com.ibm.rcp.ui.launcher.SwitcherResult;
import com.ibm.rcp.ui.launcher.SwitcherService;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.SugarAccount;
import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.core.SugarOpportunity;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.DashboardContribution;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.DashboardContributionExtensionProcessor;
import com.ibm.socialcrm.notesintegration.core.ui.views.ISugarDashboardViewPart;
import com.ibm.socialcrm.notesintegration.core.ui.views.SugarDashboardViewPart;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.core.utils.UpdateSelectionsBroadcaster;
import com.ibm.socialcrm.notesintegration.sugarwidgetupdate.SugarWidgetUpdateActivator;
import com.ibm.socialcrm.notesintegration.sugarwidgetupdate.SugarWidgetUpdater;
import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.ui.personalities.SFANonModalWindowAdvisorFactory;
import com.ibm.socialcrm.notesintegration.ui.perspectives.AccountPerspectiveFactory;
import com.ibm.socialcrm.notesintegration.ui.perspectives.ContactPerspectiveFactory;
import com.ibm.socialcrm.notesintegration.ui.perspectives.NoSugarEntryPerspectiveFactory;
import com.ibm.socialcrm.notesintegration.ui.perspectives.OpportunityPerspectiveFactory;
import com.ibm.socialcrm.notesintegration.ui.preferences.PreferencePage;
import com.ibm.socialcrm.notesintegration.ui.progress.DashboardCompositeProgressIndicator;
import com.ibm.socialcrm.notesintegration.ui.utils.CardSummary;
import com.ibm.socialcrm.notesintegration.ui.views.NoSugarEntryViewPart;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class SugarItemsDashboard {
	private static SugarItemsDashboard instance;

	private Font boldFont;
	private Font normalFont;
	private Font headerFont;
	private Font tableItemFont;
	private Font tableHeaderFont;
	private Font businessCardLabelFont;
	private Color sametimeListingSelectionBackgroundColor;
	private Color businessCardLinkColor;
	private Color businessCardHeaderTextColor;
	private Color businessCardFieldLabelColor;
	private Color businessCardFieldTextColor;
	private Color toolbarMenuColor;
	private Color toolbarBackgroundColor;

	public static final String WIDTH_PREFERENCE = "widthPreference"; //$NON-NLS-1$
	public static final String HEIGHT_PREFERENCE = "heightPreference"; //$NON-NLS-1$
	public static final String RECENTLY_VIEWED_CARDS_PREF_KEY = "recentlyViewedCards"; //$NON-NLS-1$	
	public static final String RECENTLY_VIEWED_CARDS_ORDER_PREF_KEY = "recentlyViewedCardsOrder"; //$NON-NLS-1$
	public static final String RECENTLY_VIEWED_CARDS_PINNED_PREF_KEY = "recentlyViewedCardspinned"; //$NON-NLS-1$

	public static final int DEFAULT_WIDTH = 700;
	public static final int DEFAULT_HEIGHT = 500;

	// Maps sugar ids to existing shells. If a shell is still active, we'll do a bring to front on it instead of recreating it.
	private Map<String, Shell> bringToFrontMap = new HashMap<String, Shell>();

	private PropertyChangeSupport propertyChangeSupport;
	// Event to listen to when a card is raised
	public static final String CARD_RAISED_PROPERTY = "cardRaised"; //$NON-NLS-1$
	public static final String LOAD_STARTED_PROPERTY = "loadStarted"; //$NON-NLS-1$
	public static final String LOAD_COMPLETE_PROPERTY = "loadComplete"; //$NON-NLS-1$

	// This list should never be serialized because of privacy reasons. European privacy laws
	// don't let us store contact data on the client's machine in a persistent way. Even encrypting
	// it is a hazy area. So we only keep an in memory copy.
	public transient SortedSet<CardSummary> previouslyViewedCards;

	public boolean previouslyViewedCardsLoaded = false; // Keeps track of whether we've successfully loaded the previously viewed cards. Only need to do this once
	public boolean loadingCards = false;
	public boolean errorsLoadingCards = false;

	public long lastSidbarRefreshTime = 0;

	private SugarItemsDashboard() {

		// Listener to update the card summary and then to fire the card raised event when card's base tab is ready
		PropertyChangeListener propertyChangedListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt != null && evt.getPropertyName() != null && evt.getPropertyName().equals(UpdateSelectionsBroadcaster.BASECARD_DATA_RETRIEVED) && evt.getNewValue() != null) {
					if (evt.getNewValue() instanceof BaseSugarEntry) {
						updateCardSummary((BaseSugarEntry) evt.getNewValue());
					}
				}
			}
		};
		UpdateSelectionsBroadcaster.getInstance().registerListener(propertyChangedListener);

	}

	public void displayPanel(final BaseSugarEntry entry, SugarType sugarType, String searchText) {
		try {
			IWorkbenchWindow win = null;
			SwitcherService switcherservice = UtilsPlugin.getDefault().getSwitcherService();
			SwitcherParam param = new SwitcherParam(null);
			param.setMode(SwitcherParam.NEW_WINDOW);
			param.setShowTitleBar(false);

			SwitcherGroupInfo sgi = new SwitcherGroupInfo();
			sgi.setGroupStyle(SwitcherGroupInfo.GROUPSTYLE_NODROPDOWN_MENU | SwitcherGroupInfo.GROUPSTYLE_NOROOT);
			param.setGroupInfo(sgi);
			param.setPersonalityId(SFANonModalWindowAdvisorFactory.PERSONALITY_EXTENSION_ID);
			if (entry != null) {
				if (bringToFrontMap.get(entry.getId()) == null) {
					String perspectiveId = ConstantStrings.EMPTY_STRING;
					if (entry.getSugarType().equals(SugarType.ACCOUNTS)) {
						perspectiveId = AccountPerspectiveFactory.ID;
					} else if (entry.getSugarType().equals(SugarType.OPPORTUNITIES)) {
						perspectiveId = OpportunityPerspectiveFactory.ID;
					} else if (entry.getSugarType().equals(SugarType.CONTACTS)) {
						perspectiveId = ContactPerspectiveFactory.ID;
					}
					param.setPerspectiveId(perspectiveId);
					param.setTitle(entry.getName());

					int ctr = 0;
					// Maps the view id to the DashboardContribution
					Map<String, DashboardContribution> viewIdMap = new HashMap<String, DashboardContribution>();
					List<DashboardContribution> contributionSet = DashboardContributionExtensionProcessor.getInstance().getDashboardContributionList();
					for (DashboardContribution contrib : contributionSet) {
						if (contrib.getApplicableTypes().contains(entry.getSugarType())) {
							if (ctr == 0) {
								param.setViewId(contrib.getViewPartId()); // Set the first view id since the switcher service seems to need this
								ctr++;
							}
							viewIdMap.put(contrib.getViewPartId(), contrib);
						}
					}

					// show() calls SugarViewPart.init() with null input
					SwitcherResult result = switcherservice.show(param);
					win = result.getWindow();

					DashboardCompositeProgressIndicator progressIndicator = new DashboardCompositeProgressIndicator(win.getShell());
					IViewReference[] references = win.getPages()[0].getViewReferences();
					final List<ISugarDashboardViewPart> allViewParts = new ArrayList<ISugarDashboardViewPart>();
					for (IViewReference reference : references) {
						XMLMemento xmlMemento = XMLMemento.createWriteRoot("root"); //$NON-NLS-1$						
						xmlMemento.putString(SugarDashboardViewPart.SUGAR_ID, entry.getId());
						//Not all ISugarDashboardViewParts will contain dashboard contributions.  For things where we're hosting external views (external
						//to our codebase anyway) like the ActivityStreams, a standard dashboard contribution will not exist.
						if (viewIdMap.get(reference.getId()) != null) {
							xmlMemento.putString(SugarDashboardViewPart.DASHBOARD_CONTRIBUTION_ID, viewIdMap.get(reference.getId()).getId());
						}
						IViewPart part = reference.getView(true);
						part.init(part.getViewSite(), xmlMemento);
						if (part instanceof ISugarDashboardViewPart) // Should always be the case, but just in case
						{
							ISugarDashboardViewPart dashboardViewPart = (ISugarDashboardViewPart) part;
							dashboardViewPart.setProgessDisplayer(progressIndicator);
							progressIndicator.getViewParts().add(dashboardViewPart);
							allViewParts.add(dashboardViewPart);
						}
					}

					// Establish the relationships between the view parts at a card level by setting the sibling relationships
					// for each view part.
					for (ISugarDashboardViewPart part : allViewParts) {
						for (ISugarDashboardViewPart innerPart : allViewParts) {
							if (innerPart != part) {
								part.getSiblings().add(innerPart);
							}
						}
					}
				} else {
					Shell shell = bringToFrontMap.get(entry.getId());
					shell.forceActive();
				}
			} else {
				param.setPerspectiveId(NoSugarEntryPerspectiveFactory.PERSPECTIVE_ID);
				param.setTitle(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.NO_CRM_ENTRY));
				param.setViewId(NoSugarEntryViewPart.VIEW_ID);
				SwitcherResult result = switcherservice.show(param);
				win = result.getWindow();
				IViewReference reference = win.getPages()[0].getViewReferences()[0];
				XMLMemento xmlMemento = XMLMemento.createWriteRoot("root"); //$NON-NLS-1$			
				xmlMemento.putString(NoSugarEntryViewPart.SEARCH_TEXT, searchText);
				xmlMemento.putString(NoSugarEntryViewPart.SUGAR_TYPE, sugarType.toString());
				IViewPart part = reference.getView(true);

				part.init(part.getViewSite(), xmlMemento);
			}

			if (win != null) {
				final Shell shell = win.getShell();
				if (entry != null) {
					shell.setText(entry.getName());
					if (bringToFrontMap.get(entry) == null) {
						bringToFrontMap.put(entry.getId(), shell);
						shell.addDisposeListener(new DisposeListener() {
							@Override
							public void widgetDisposed(DisposeEvent disposeEvt) {
								bringToFrontMap.remove(entry.getId());
							}
						});
						shell.addListener(SWT.Resize, new Listener() {
							public void handleEvent(Event e) {
								// Don't save the preferences when the user maximizes the size
								if (!shell.getMaximized()) {
									Preferences prefs = UiPluginActivator.getDefault().getPluginPreferences();
									prefs.setValue(WIDTH_PREFERENCE, shell.getSize().x);
									prefs.setValue(HEIGHT_PREFERENCE, shell.getSize().y);
									UiPluginActivator.getDefault().savePluginPreferences();
								}
							}
						});
					}
				}
				shell.setImage(SFAImageManager.getImage(SFAImageManager.SALES_CONNECT));

				Preferences prefs = UiPluginActivator.getDefault().getPluginPreferences();
				int xPref = prefs.getInt(WIDTH_PREFERENCE);
				int yPref = prefs.getInt(HEIGHT_PREFERENCE);
				if (xPref == 0) {
					xPref = DEFAULT_WIDTH;
				}
				if (yPref == 0) {
					yPref = DEFAULT_HEIGHT;
				}
				shell.setSize(xPref, yPref + 1); // The +1 is a stupid hack to get the first tab in the card to draw properly
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}
	}

	private void updateCardSummary(final BaseSugarEntry entry) {
		try {
			Preferences prefs = UiPluginActivator.getDefault().getPluginPreferences();
			// id:type, id:type...
			String recentlyViewedCards = prefs.getString(RECENTLY_VIEWED_CARDS_PREF_KEY);
			// JSON doesn't honor order, so we keep a bit of extra data around to tell us the proper order
			String recentlyViewedCardsOrder = prefs.getString(RECENTLY_VIEWED_CARDS_ORDER_PREF_KEY);
			
			String recentlyViewedCardsPinned = prefs.getString(RECENTLY_VIEWED_CARDS_PINNED_PREF_KEY);
			
			
			if (recentlyViewedCards != null && !recentlyViewedCards.equals(ConstantStrings.EMPTY_STRING)) {

				JSONObject recentCards = new JSONObject(recentlyViewedCards);
				recentCards.put(entry.getId(), entry.getSugarType().toString());
				String jsonString = recentCards.toString();
				prefs.setValue(RECENTLY_VIEWED_CARDS_PREF_KEY, jsonString);
				if (recentlyViewedCardsOrder == null) {
					recentlyViewedCardsOrder = ConstantStrings.EMPTY_STRING;
				}
				String[] order = recentlyViewedCardsOrder.split(","); //$NON-NLS-1$
				List<String> orderedList = new ArrayList<String>(Arrays.asList(order));
				orderedList.remove(entry.getId()); // Remove it from it's current place in the list and move it to the front.
				orderedList.add(0, entry.getId());
				prefs.setValue(RECENTLY_VIEWED_CARDS_ORDER_PREF_KEY, StringUtils.join(orderedList.toArray(), ",")); //$NON-NLS-1$
				UiPluginActivator.getDefault().savePluginPreferences();

			} else {
				JSONObject recentCards = new JSONObject();
				recentCards.put(entry.getId(), entry.getSugarType().toString());
				String jsonString = recentCards.toString();
				prefs.setValue(RECENTLY_VIEWED_CARDS_PREF_KEY, jsonString);
				prefs.setValue(RECENTLY_VIEWED_CARDS_ORDER_PREF_KEY, entry.getId());
				UiPluginActivator.getDefault().savePluginPreferences();
			}
			CardSummary summary = null;
			if (entry.getSugarType() == SugarType.ACCOUNTS) {
				String address = getAddressString(entry.getStreet(), entry.getCity(), entry.getPostalCode(), entry.getCountry());
				summary = new CardSummary(entry.getName() + " - " + ((SugarAccount) entry).getClientId(), address, SugarType.ACCOUNTS, entry.getId()); //$NON-NLS-1$
			} else if (entry.getSugarType() == SugarType.CONTACTS) {
				SugarContact contact = (SugarContact) entry;
				String description = ConstantStrings.EMPTY_STRING;
				description = buildContactDescription(contact.getEmail(), contact.getOfficePhone(), contact.getMobilePhone());
				summary = new CardSummary(entry.getName() + " - " + contact.getAccountName(), description, SugarType.CONTACTS, entry.getId());//$NON-NLS-1$
				summary.setEmail(contact.getEmail());
				summary.setEmailSuppressed(contact.isEmailSuppressed());
				summary.setMobilePhone(contact.getMobilePhone());
				summary.setMobilePhoneSuppressed(contact.isMobilePhoneSuppressed());
				summary.setOfficePhone(contact.getOfficePhone());
				summary.setOfficePhoneSuppressed(contact.isOfficePhoneSuppressed());
			} else if (entry.getSugarType() == SugarType.OPPORTUNITIES) {
				SugarOpportunity oppty = (SugarOpportunity) entry;
				String description = getOpportunityDescription(oppty.getTotalRevenue(), oppty.getDecisionDate(), oppty.getSalesStage(), oppty.getDescription());
				summary = new CardSummary(entry.getName() + " - " + oppty.getAccountName(), description, SugarType.OPPORTUNITIES, entry.getId()); //$NON-NLS-1$
			}

			if (summary != null) {
				summary.setTimestampMillis(System.currentTimeMillis());
				int pinFlagInPreviouslyViewedCards = getPreviouslyViewedCardsPinValue(summary);
				if ( pinFlagInPreviouslyViewedCards  == CardSummary.IS_PINNED) {
					summary.setPinned(pinFlagInPreviouslyViewedCards);
				}
				if (getPreviouslyViewedCards().contains(summary)) {		
					getPreviouslyViewedCards().remove(summary);
				}

				Preferences corePrefs = CorePluginActivator.getDefault().getPluginPreferences();
				int maxCards = corePrefs.getInt(SugarWidgetUpdater.MAX_SIDEBAR_CARDS_PREF_KEY);
				if (maxCards == 0) {
					maxCards = PreferencePage.MIN_RECENTLY_VIEWED_CARDS;
					corePrefs.setValue(SugarWidgetUpdater.MAX_SIDEBAR_CARDS_PREF_KEY, maxCards);
					CorePluginActivator.getDefault().savePluginPreferences();
				}
				if (getPreviouslyViewedCards().size() >= maxCards) {
					CardSummary last = getPreviouslyViewedCards().last();
					getPreviouslyViewedCards().remove(last);
				}

				getPreviouslyViewedCards().add(summary);
			}
			getPropertyChangeSupport().firePropertyChange(CARD_RAISED_PROPERTY, null, entry);
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}
	}
	
	
	private int getPreviouslyViewedCardsPinValue(CardSummary summary) {
		int pinValue = 0;
		if ( getPreviouslyViewedCards() != null && !getPreviouslyViewedCards().isEmpty() && summary != null) {
			Iterator<CardSummary> it=getPreviouslyViewedCards().iterator();
			while (it.hasNext()) {
				CardSummary c=it.next();
				if ( c.getId().equalsIgnoreCase(summary.getId()) ) {
					pinValue = c.isPinned().intValue();
				}
			}
		}
		return pinValue;
	}
	
	
	public static SugarItemsDashboard getInstance() {
		if (instance == null) {
			instance = new SugarItemsDashboard();

			Thread sidebarRefreshThread = new Thread() {
				@Override
				public void run() {
					Preferences prefs = CorePluginActivator.getDefault().getPluginPreferences();

					while (true) {
						try {
							// Wake up every minute to check to see if we need to reload.
							Thread.sleep(60000);
							long currentTime = System.currentTimeMillis();
							if (SugarItemsDashboard.getInstance().lastSidbarRefreshTime != 0) {
								long difference = currentTime - SugarItemsDashboard.getInstance().lastSidbarRefreshTime;
								// Use the same refresh preference as the regex
								int refreshPref = prefs.getInt(SugarWidgetUpdater.REFRESH_REGEXES_INTERVAL_PREF_KEY);
								refreshPref = refreshPref == 0 ? SugarWidgetUpdater.REFRESH_INTERVAL_DEFAULT : refreshPref;
								int refreshTimeInMillis = refreshPref * 60 * 1000;

								if (difference >= refreshTimeInMillis) {
									// Put it in a job so that it can be displayed in the Notes progress bar.
									Job job = new Job("Refreshing SalesConnect sidebar") { //$NON-NLS-1$
										@Override
										protected IStatus run(IProgressMonitor monitor) {
											if (!SugarWebservicesOperations.getInstance().unableToLogin()) {
												SugarItemsDashboard.getInstance().refreshPreviouslyViewedCards();
											}
											return Status.OK_STATUS;
										}
									};
									job.schedule();
								}
							}
						} catch (InterruptedException e) {
							UtilsPlugin.getDefault().logException(e, SugarWidgetUpdateActivator.PLUGIN_ID);
						}
					}
				}
			};
			sidebarRefreshThread.start();
		}
		return instance;
	}
	
	//Font look tiny in Mac for some reason.  So if we're on a Mac, bump the font size up
	private int macFontSizeAdjustment = GenericUtils.isMac() ? 4 : 0;

	public Font getBoldFontForBusinessCardData() {
		if (boldFont == null) {
			String fontName = "Arial-11-bold"; //$NON-NLS-1$
			if (JFaceResources.getFontRegistry().hasValueFor(fontName)) {
				boldFont = JFaceResources.getFontRegistry().get(fontName);
			} else {
				JFaceResources.getFontRegistry().put(fontName, new FontData[]{new FontData("Arial", 11 + macFontSizeAdjustment, SWT.BOLD)}); //$NON-NLS-1$
				boldFont = JFaceResources.getFontRegistry().get(fontName);
			}
		}
		return boldFont;
	}

	public Font getNormalFontForBusinessCardData() {
		if (normalFont == null) {
			String fontName = "Arial-11-normal"; //$NON-NLS-1$
			if (JFaceResources.getFontRegistry().hasValueFor(fontName)) {
				normalFont = JFaceResources.getFontRegistry().get(fontName);
			} else {
				JFaceResources.getFontRegistry().put(fontName, new FontData[]{new FontData("Arial", 11 + macFontSizeAdjustment, SWT.NORMAL)}); //$NON-NLS-1$
				normalFont = JFaceResources.getFontRegistry().get(fontName);
			}
		}
		return normalFont;
	}

	public Font getHeaderFont() {
		if (headerFont == null) {
			String fontName = "Arial-18-normal"; //$NON-NLS-1$
			if (JFaceResources.getFontRegistry().hasValueFor(fontName)) {
				headerFont = JFaceResources.getFontRegistry().get(fontName);
			} else {
				JFaceResources.getFontRegistry().put(fontName, new FontData[]{new FontData("Arial", 18 + macFontSizeAdjustment, SWT.NORMAL)}); //$NON-NLS-1$
				headerFont = JFaceResources.getFontRegistry().get(fontName);
			}
		}
		return headerFont;
	}

	public Font getTableItemFont() {
		if (tableItemFont == null) {
			String fontName = "tableItemFont"; //$NON-NLS-1$
			if (JFaceResources.getFontRegistry().hasValueFor(fontName)) {
				tableItemFont = JFaceResources.getFontRegistry().get(fontName);
			} else {
				JFaceResources.getFontRegistry().put(fontName, new FontData[]{new FontData("Arial", 10 + macFontSizeAdjustment, SWT.NORMAL)}); //$NON-NLS-1$
				tableItemFont = JFaceResources.getFontRegistry().get(fontName);
			}
		}
		return tableItemFont;
	}

	public Font getTableHeaderFont() {
		if (tableHeaderFont == null) {
			String fontName = "tableHeaderFont"; //$NON-NLS-1$
			if (JFaceResources.getFontRegistry().hasValueFor(fontName)) {
				tableHeaderFont = JFaceResources.getFontRegistry().get(fontName);
			} else {
				JFaceResources.getFontRegistry().put(fontName, new FontData[]{new FontData("Arial", 10 + macFontSizeAdjustment, SWT.BOLD)}); //$NON-NLS-1$
				tableHeaderFont = JFaceResources.getFontRegistry().get(fontName);
			}
		}
		return tableHeaderFont;
	}

	public Font getBusinessCardLabelFont() {
		if (businessCardLabelFont == null) {
			String fontName = "businessCardLabelFont"; //$NON-NLS-1$
			if (JFaceResources.getFontRegistry().hasValueFor(fontName)) {
				businessCardLabelFont = JFaceResources.getFontRegistry().get(fontName);
			} else {
				JFaceResources.getFontRegistry().put(fontName, new FontData[]{new FontData("Arial", 10 + macFontSizeAdjustment, SWT.NORMAL)}); //$NON-NLS-1$
				businessCardLabelFont = JFaceResources.getFontRegistry().get(fontName);
			}
		}
		return businessCardLabelFont;
	}

	public Color getSametimeListingSelectionBackgroundColor() {
		if (sametimeListingSelectionBackgroundColor == null) {
			String colorName = SugarItemsDashboard.class.getName() + "sametimeBackground"; //$NON-NLS-1$
			sametimeListingSelectionBackgroundColor = JFaceResources.getColorRegistry().get(colorName);
			if (sametimeListingSelectionBackgroundColor == null) {
				JFaceResources.getColorRegistry().put(colorName, new RGB(51, 153, 255));
				sametimeListingSelectionBackgroundColor = JFaceResources.getColorRegistry().get(colorName);
			}
		}
		return sametimeListingSelectionBackgroundColor;
	}

	public Color getBusinessCardLinkColor() {
		if (businessCardLinkColor == null) {
			if (Display.getDefault().getHighContrast()) {
				businessCardLinkColor = JFaceColors.getActiveHyperlinkText(Display.getCurrent());
			} else {
				String colorName = SugarItemsDashboard.class.getName() + "businessCardLinkColor"; //$NON-NLS-1$
				businessCardLinkColor = JFaceResources.getColorRegistry().get(colorName);
				if (businessCardLinkColor == null) {
					JFaceResources.getColorRegistry().put(colorName, new RGB(25, 112, 176));
					businessCardLinkColor = JFaceResources.getColorRegistry().get(colorName);
				}
			}
		}
		return businessCardLinkColor;
	}

	public Color getBusinessCardHeaderTextColor() {
		if (businessCardHeaderTextColor == null) {
			if (Display.getDefault().getHighContrast()) {
				businessCardHeaderTextColor = JFaceColors.getBannerForeground(Display.getCurrent());
			} else {
				String colorName = SugarItemsDashboard.class.getName() + "businessCardHeaderTextColor"; //$NON-NLS-1$
				businessCardHeaderTextColor = JFaceResources.getColorRegistry().get(colorName);
				if (businessCardHeaderTextColor == null) {
					JFaceResources.getColorRegistry().put(colorName, new RGB(68, 68, 68));
					businessCardHeaderTextColor = JFaceResources.getColorRegistry().get(colorName);
				}
			}
		}
		return businessCardHeaderTextColor;
	}

	public Color getBusinessCardFieldLabelColor() {
		if (businessCardFieldLabelColor == null) {
			if (Display.getDefault().getHighContrast()) {
				businessCardFieldLabelColor = JFaceColors.getBannerForeground(Display.getCurrent());
			} else {
				String colorName = SugarItemsDashboard.class.getName() + "businessCardFieldLabelColor"; //$NON-NLS-1$
				businessCardFieldLabelColor = JFaceResources.getColorRegistry().get(colorName);
				if (businessCardFieldLabelColor == null) {
					JFaceResources.getColorRegistry().put(colorName, new RGB(102, 102, 102));
					businessCardFieldLabelColor = JFaceResources.getColorRegistry().get(colorName);
				}
			}
		}
		return businessCardFieldLabelColor;
	}

	public Color getBusinessCardFieldTextColor() {
		if (businessCardFieldTextColor == null) {
			if (Display.getDefault().getHighContrast()) {
				businessCardFieldTextColor = JFaceColors.getBannerForeground(Display.getCurrent());
			} else {
				String colorName = SugarItemsDashboard.class.getName() + "businessCardFieldTextColor"; //$NON-NLS-1$
				businessCardFieldTextColor = JFaceResources.getColorRegistry().get(colorName);
				if (businessCardFieldTextColor == null) {
					JFaceResources.getColorRegistry().put(colorName, new RGB(34, 34, 34));
					businessCardFieldTextColor = JFaceResources.getColorRegistry().get(colorName);
				}
			}
		}
		return businessCardFieldTextColor;
	}

	public Color getToolbarMenuColor() {
		if (toolbarMenuColor == null) {
			if (Display.getDefault().getHighContrast()) {
				toolbarMenuColor = JFaceColors.getBannerForeground(Display.getCurrent());
			} else {
				String colorName = SugarItemsDashboard.class.getName() + "toolbarMenuColor"; //$NON-NLS-1$
				toolbarMenuColor = JFaceResources.getColorRegistry().get(colorName);
				if (toolbarMenuColor == null) {
					JFaceResources.getColorRegistry().put(colorName, new RGB(57, 87, 122));
					toolbarMenuColor = JFaceResources.getColorRegistry().get(colorName);
				}
			}
		}
		return toolbarMenuColor;
	}

	public Color getToolbarBackgroundColor() {
		if (toolbarBackgroundColor == null) {
			if (Display.getDefault().getHighContrast()) {
				toolbarBackgroundColor = JFaceColors.getBannerBackground(Display.getCurrent());
			} else {
				String colorName = SugarItemsDashboard.class.getName() + "toolbarBackgroundColor"; //$NON-NLS-1$
				toolbarBackgroundColor = JFaceResources.getColorRegistry().get(colorName);
				if (toolbarBackgroundColor == null) {
					JFaceResources.getColorRegistry().put(colorName, new RGB(221, 227, 236));
					toolbarBackgroundColor = JFaceResources.getColorRegistry().get(colorName);
				}
			}
		}
		return toolbarBackgroundColor;
	}

	public PropertyChangeSupport getPropertyChangeSupport() {
		if (propertyChangeSupport == null) {
			propertyChangeSupport = new PropertyChangeSupport(this);
		}
		return propertyChangeSupport;
	}

	public SortedSet<CardSummary> getPreviouslyViewedCards() {
		if (previouslyViewedCards == null) {
			previouslyViewedCards = new TreeSet<CardSummary>(new Comparator<CardSummary>() {
				@Override
				public int compare(CardSummary summary1, CardSummary summary2) {
					return summary2.getId().compareTo(summary1.getId());
				}
			});
		}
		return previouslyViewedCards;
	}

	/**
	 * Clears out the previously viewed cards
	 */
	public void clearPreviouslyViewedCards() {
		previouslyViewedCards = null;
		Preferences prefs = UiPluginActivator.getDefault().getPluginPreferences();

		prefs.setValue(RECENTLY_VIEWED_CARDS_PREF_KEY, ConstantStrings.EMPTY_STRING);
		prefs.setValue(RECENTLY_VIEWED_CARDS_ORDER_PREF_KEY, ConstantStrings.EMPTY_STRING);
		prefs.setValue(RECENTLY_VIEWED_CARDS_PINNED_PREF_KEY, ConstantStrings.EMPTY_STRING);

		UiPluginActivator.getDefault().savePluginPreferences();
		getPropertyChangeSupport().firePropertyChange(LOAD_COMPLETE_PROPERTY, true, false);
	}

	public void refreshPreviouslyViewedCards() {
		previouslyViewedCardsLoaded = false;
		loadPreviouslyViewedCards();
	}

	public void loadPreviouslyViewedCards() {
		Job loadCardsJob = new Job("Loading previously viewed cards") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				synchronized (this) {
					lastSidbarRefreshTime = System.currentTimeMillis();
					if (!loadingCards && !previouslyViewedCardsLoaded) {
						loadingCards = true;
						errorsLoadingCards = false;
						getPropertyChangeSupport().firePropertyChange(LOAD_STARTED_PROPERTY, true, false);

						try {
							Preferences prefs = UiPluginActivator.getDefault().getPluginPreferences();
							String recentlyViewedCards = prefs.getString(RECENTLY_VIEWED_CARDS_PREF_KEY);
							String recentlyViewedCardsOrder = prefs.getString(RECENTLY_VIEWED_CARDS_ORDER_PREF_KEY);
							String recentlyViewedCardsPinned = prefs.getString(RECENTLY_VIEWED_CARDS_PINNED_PREF_KEY);
							if (recentlyViewedCards != null && !recentlyViewedCards.equals(ConstantStrings.EMPTY_STRING) && recentlyViewedCardsOrder != null) {
								JSONObject recentCards = new JSONObject(recentlyViewedCards);
								Iterator<String> iter = recentCards.keys();
								// HashMap< cardid, cardtype>
								Map<String, String> idMap = new HashMap<String, String>();
								while (iter.hasNext()) {
									String key = iter.next();
									idMap.put(key, recentCards.getString(key));
								}

								String resultString = SugarWebservicesOperations.getInstance().getRecentlyViewedCards(idMap);
								JSONObject responseJSON = new JSONObject(resultString);
								JSONObject resultJSON = responseJSON.getJSONObject("result"); //$NON-NLS-1$
								String[] order = recentlyViewedCardsOrder.split(ConstantStrings.COMMA);  
								List<String> pinnedList = null;
								if (recentlyViewedCardsPinned != null && !recentlyViewedCardsPinned.isEmpty() ) {
									pinnedList= Arrays.asList(recentlyViewedCardsPinned.split(ConstantStrings.COMMA)); 
									
								}
								
								long orderCtr = order.length;
								for (String id : order) {
									if (id != null && id.length() > 0) {
										// The response contains a wrapper element with the id as the key. Underneath that will be the real element (also
										// with the id as the key).
										JSONObject wrapperElement = resultJSON.getJSONObject(id);
										JSONObject dataJSON = null;

										try {
											dataJSON = wrapperElement.getJSONObject(id);
										} catch (Exception e) {
											// It's possible we get an empty element back if the object has been deleted. Eat the exception and move on.
											// Eventually, it will roll out of our list and we'll stop trying to process it.
											UtilsPlugin.getDefault().logErrorMessage("No data returned in the webservice for " + id, UiPluginActivator.PLUGIN_ID); //$NON-NLS-1$
											UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
										}
										if (dataJSON != null) {
											if (idMap.get(id).equals(SugarType.CONTACTS.toString())) {
												loadContactSummary(dataJSON, id, orderCtr, isPinned(pinnedList, id));
											} else if (idMap.get(id).equals(SugarType.ACCOUNTS.toString())) {
												loadAccountSummary(dataJSON, id, orderCtr , isPinned(pinnedList, id));
											} else if (idMap.get(id).equals(SugarType.OPPORTUNITIES.toString())) {
												loadOpportunitySummary(dataJSON, id, orderCtr , isPinned(pinnedList, id));
											}
											orderCtr--;
										}

									}
								}
							}
						} catch (Exception e) {
							UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
							// In case anything blows up, catch it so we don't forget to reset the loadingCards flag
							errorsLoadingCards = true;
							previouslyViewedCardsLoaded = false;
						}

						loadingCards = false;
						if (!errorsLoadingCards) {
							previouslyViewedCardsLoaded = true;
						}
						// Notify listeners that we've loaded
						getPropertyChangeSupport().firePropertyChange(LOAD_COMPLETE_PROPERTY, true, false);
					}
				}
				return Status.OK_STATUS;
			}
		};
		loadCardsJob.schedule();
	}
	
	private boolean isPinned(List<String> pinnedList, String id) {
		boolean isPinned = false;
		if ( pinnedList != null && !pinnedList.isEmpty() && id != null && pinnedList.contains(id)) {
			isPinned = true;
		}
		return isPinned;
	}
	
	
	
	/**
	 * Creates a contact card summary object from the JSON returned by the recently viewed card web service
	 * 
	 * @param summaryJSON
	 * @param id
	 */
	private void loadContactSummary(JSONObject summaryJSON, String id, long order) {
		loadContactSummary(summaryJSON, id, order, false);
	}
		private void loadContactSummary(JSONObject summaryJSON, String id, long order, boolean isPinned) {
		try {
			String name = summaryJSON.getString(ConstantStrings.DATABASE_FIRST_NAME) + " " + summaryJSON.getString(ConstantStrings.DATABASE_LAST_NAME) + " - " //$NON-NLS-1$ //$NON-NLS-2$
					+ summaryJSON.getString(ConstantStrings.ACCOUNT_NAME);
			String description = buildContactDescription(summaryJSON.getString(ConstantStrings.DATABASE_EMAIL_ADDRESS), summaryJSON.getString(ConstantStrings.DATABASE_PHONE_WORK), summaryJSON
					.getString(ConstantStrings.DATABASE_PHONE_MOBILE));
			CardSummary summary = new CardSummary(name, description, SugarType.CONTACTS, id);

			summary.setEmail(summaryJSON.getString(ConstantStrings.DATABASE_EMAIL_ADDRESS));
			summary.setEmailSuppressed(summaryJSON.getString(ConstantStrings.DATABASE_EMAIL_OPT_OUT).equals("1")); //$NON-NLS-1$
			summary.setMobilePhone(summaryJSON.getString(ConstantStrings.DATABASE_PHONE_MOBILE));
			summary.setMobilePhoneSuppressed(summaryJSON.getString(ConstantStrings.DATABASE_PHONE_MOBILE_OPT_OUT).equals("1")); //$NON-NLS-1$
			summary.setOfficePhone(summaryJSON.getString(ConstantStrings.DATABASE_PHONE_WORK));
			summary.setOfficePhoneSuppressed(summaryJSON.getString(ConstantStrings.DATABASE_PHONE_WORK_OPT_OUT).equals("1")); //$NON-NLS-1$
			summary.setTimestampMillis(order);
			if ( isPinned) {
				summary.setPinned(isPinned);
			}
			// Remove the old one from the set so the new one can be added
			getPreviouslyViewedCards().remove(summary);
			getPreviouslyViewedCards().add(summary);
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}
	}

	/**
	 * Build the card summary description for a contact
	 * 
	 * @return
	 */
	private String buildContactDescription(String email, String officePhone, String mobilePhone) {
		String description = ConstantStrings.EMPTY_STRING;
		if (email != null && email.length() > 0) {
			// TODO: Handle suppression here
			description += email + " "; //$NON-NLS-1$
		}
		if (officePhone != null && officePhone.length() > 0) {
			description += UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OFFICE_PHONE_WITH_LABEL, new String[]{officePhone}) + " "; //$NON-NLS-1$
		}
		if (mobilePhone != null && mobilePhone.length() > 0) {
			description += UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.MOBILE_PHONE_WITH_LABEL, new String[]{mobilePhone});
		}
		return description;
	}

	/**
	 * Creates an account card summary object from the JSON returned by the recently viewed card web service
	 * 
	 * @param summaryJSON
	 * @param id
	 */
	private void loadAccountSummary(JSONObject summaryJSON, String id, long order) {
		loadAccountSummary(summaryJSON, id, order, false);
	}
		private void loadAccountSummary(JSONObject summaryJSON, String id, long order, boolean isPinned) {

		try {
			String name = summaryJSON.getString(ConstantStrings.DATABASE_NAME) + " - " + summaryJSON.getString(ConstantStrings.DATABASE_CLIENT_ID); //$NON-NLS-1$
			String description = getAddressString(summaryJSON.getString(ConstantStrings.DATABASE_PHYSICAL_ADDRESS_STREET), summaryJSON.getString(ConstantStrings.DATABASE_PHYSICAL_ADDRESS_CITY),
					summaryJSON.getString(ConstantStrings.DATABASE_PHYSICAL_ADDRESS_POSTAL_CODE), summaryJSON.getString(ConstantStrings.DATABASE_PHYSICAL_ADDRESS_COUNTRY));

			CardSummary summary = new CardSummary(name, description, SugarType.ACCOUNTS, id);
			summary.setTimestampMillis(order);
			if ( isPinned) {
				summary.setPinned(isPinned);
			}
			// Remove the old one from the set so the new one can be added
			getPreviouslyViewedCards().remove(summary);
			getPreviouslyViewedCards().add(summary);
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}
	}

	/**
	 * Helper method to build an address string
	 * 
	 * @return
	 */
	public String getAddressString(String street, String city, String postalCode, String country) {
		String addressString = ConstantStrings.EMPTY_STRING;
		if (street != null && street.length() > 0) {
			addressString += street + ", "; //$NON-NLS-1$
		}
		if (city != null && city.length() > 0) {
			addressString += city + ", "; //$NON-NLS-1$
		}
		if (postalCode != null && postalCode.length() > 0) {
			addressString += postalCode + " "; //$NON-NLS-1$
		}
		if (country != null && country.length() > 0) {
			addressString += country;
		}

		return addressString;
	}

	/**
	 * Creates an oppty card summary object from the JSON returned by the recently viewed card web service
	 * 
	 * @param summaryJSON
	 * @param id
	 */
	private void loadOpportunitySummary(JSONObject summaryJSON, String id, long order) {
		loadOpportunitySummary(summaryJSON, id, order, false);
	}
		private void loadOpportunitySummary(JSONObject summaryJSON, String id, long order, boolean isPinned) {
		try {
			String name = summaryJSON.getString(ConstantStrings.DATABASE_NAME);
			String description = getOpportunityDescription(summaryJSON.getString(ConstantStrings.DATABASE_AMOUNT), summaryJSON.getString(ConstantStrings.DATABASE_DATE_CLOSED), summaryJSON
					.getString(ConstantStrings.DATABASE_SALES_STAGE), summaryJSON.getString(ConstantStrings.DATABASE_DESCRIPTION));

			CardSummary summary = new CardSummary(name, description, SugarType.OPPORTUNITIES, id);
			summary.setTimestampMillis(order);
			if ( isPinned) {
				summary.setPinned(isPinned);
			}
			// Remove the old one from the set so the new one can be added
			getPreviouslyViewedCards().remove(summary);
			getPreviouslyViewedCards().add(summary);
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}
	}

	private String getOpportunityDescription(String revenue, String descisionDate, String salesStage, String opptyDescription) {
		String description = ConstantStrings.EMPTY_STRING;
		if (revenue != null && revenue.length() > 0) {
			description += revenue + " "; //$NON-NLS-1$
		}
		if (descisionDate != null && descisionDate.length() > 0) {
			description += descisionDate + " "; //$NON-NLS-1$
		}
		if (salesStage != null && salesStage.length() > 0) {
			description += salesStage + " "; //$NON-NLS-1$
		}
		if (opptyDescription != null && opptyDescription.length() > 0) {
			description += opptyDescription + " "; //$NON-NLS-1$
		}
		return description;
	}
	
	
		
}
