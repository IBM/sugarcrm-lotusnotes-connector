package com.ibm.socialcrm.notesintegration.ui.dashboardcomposites;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import org.apache.commons.json.JSONObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;

import com.ibm.rcp.swt.swidgets.SButton;
import com.ibm.rcp.swt.swidgets.SToolBar;
import com.ibm.rcp.swt.swidgets.SToolItem;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.IProgressDisplayer;
import com.ibm.socialcrm.notesintegration.core.SugarAccount;
import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.core.SugarOpportunity;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.AbstractToolbarIconContributionAction;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.AbstractToolbarMenuItem;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.DashboardContribution;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.MenuItemCategoryContribution;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.ToolbarIconContribution;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.ToolbarIconContributionExtensionProcessor;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.ToolbarMenuItemContribution;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.ToolbarMenuItemContributionExtensionProcessor;
import com.ibm.socialcrm.notesintegration.core.ui.views.ISugarDashboardViewPart;
import com.ibm.socialcrm.notesintegration.core.uipluginbridge.IDashboardComposite;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.core.utils.UpdateSelectionsBroadcaster;
import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.ui.custom.SFAToggleButton;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.dnd.SugarDashboardDropAdapter;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFAHyperlink;

/**
 * This class represents one of the tabs on the business card (or dashboard)
 */
public abstract class AbstractDashboardComposite extends Composite implements IDashboardComposite {

	public static final String OPPTYID = "opptyid"; //$NON-NLS-1$
	public static final String CONTACTID = "contactid"; //$NON-NLS-1$
	public static final String ACCOUNTID = "accountid"; //$NON-NLS-1$

	public static final String ACCOUNTLABEL = "ACCOUNTLABEL"; //$NON-NLS-1$
	public static final String INDUSTRYLABEL = "INDUSTRYLABEL"; //$NON-NLS-1$
	public static final String JOBLABEL = "JOBLABEL"; //$NON-NLS-1$
	public static final String ACCOUNTLINK = "ACCOUNTLINK"; //$NON-NLS-1$
	public static final String MAINIMAGELABEL = "MAINIMAGELABEL"; //$NON-NLS-1$
	public static final String IMPLICITFOLLOWINGCOMPOSITE = "IMPLICITFOLLOWINGCOMPOSITE"; //$NON-NLS-1$
	public static final String JOBLABELSPACER = "JOBLABELSPACER"; //$NON-NLS-1$
	public static final String FOLLOWITEM = "FOLLOWITEM"; //$NON-NLS-1$
	public static final String FOLLOWBUTTON = "FOLLOWBUTTON"; //$NON-NLS-1$

	private BaseSugarEntry sugarEntry;

	protected SFAHyperlink nameLink;

	private String dashboardID;
	private SToolBar sToolBar;
	private SFAToggleButton followButton;
	protected SFAToggleButton downloadButton;
	protected SFAToggleButton uploadButton;
	private Label titleLabel;
	private Composite toolbarComposite;
	private IProgressDisplayer progressDisplayer;
	private ISugarDashboardViewPart parentView;

	private boolean isBaseDataRetrieved = false;
	/*
	 * A map of controls whose UI value should be updated after card data becomes available
	 */
	private Map<String, Object> controlMap = null;

	public PropertyChangeListener propertyChangedListener = null;

	/**
	 * Used to display progress for any long running operations.
	 */
	private Composite progressComposite;

	private SToolItem leftBufferItem;
	private SToolItem followItem;
	private SToolItem downloadItem;
	private SToolItem uploadItem;
	private SToolItem moreActionsItem;

	// They want the follow button to be the same width whether it says "Follow" or "Following". So we have to compute
	// the width based on the longest string (obvious what that is in English)
	private int followButtonWidth = -1;

	// They want the follow button to be the same width whether it says "download" or "download". So we have to compute
	// the width based on the longest string (obvious what that is in English)
	private int downloadButtonWidth = -1;
	private int uploadButtonWidth = -1;

	/**
	 * A list of all menu items that are appropriate for this composite
	 */
	private List<List<ToolbarMenuItemContribution>> allMenuItems;

	/**
	 * A map of industry values to image constants
	 */
	private static Map<String, String> industryIconMap;

	/**
	 * A handle to the dashboard contribution that contributed this class
	 */
	private DashboardContribution dashboardContribution;

	protected String selectedTitle = ConstantStrings.EMPTY_STRING;

	public static final String ALL_DASHBOARDS = "ALL_DASHBOARDS"; //$NON-NLS-1$

	public static final String SFA_WIDGET_ID = "sfaWidgetId"; //$NON-NLS-1$

	public static final int HEADER_MARGIN = 15;

	public Composite _parent;

	private SugarDashboardDropAdapter _dropAdapter;

	// 49021
	private Composite implicitFollowingComposite;

	public AbstractDashboardComposite(Composite parent, int style, String dashboardID, BaseSugarEntry sugarEntry) {
		super(parent, style | SWT.BORDER);

		_parent = parent;

		setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).create());

		setDashboardID(dashboardID);
		setSugarEntry(sugarEntry);

		rebuildComposite();

		addListeners(parent);
	}

	private void addListeners(Composite parent) {

		propertyChangedListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt != null && evt.getPropertyName() != null && evt.getPropertyName().equals(UpdateSelectionsBroadcaster.BASECARD_DATA_RETRIEVED) && evt.getNewValue() != null
						&& getDashboardContribution() != null) {

					// Be sure this change event is for me
					if (evt.getNewValue() != null && evt.getNewValue() instanceof BaseSugarEntry && ((BaseSugarEntry) evt.getNewValue()).getId() != null && sugarEntry != null
							&& sugarEntry.getId() != null && ((BaseSugarEntry) evt.getNewValue()).getId().equals(sugarEntry.getId())) {
						try {
							afterBaseCardDataRetrieved();
						} catch (org.eclipse.swt.SWTException e) {
							if (AbstractDashboardComposite.this.isDisposed()) {
							} else {
								UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
							}

						}
					}
				}
			}
		};

		UpdateSelectionsBroadcaster.getInstance().registerListener(propertyChangedListener);

		parent.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				if (propertyChangedListener != null) {
					UpdateSelectionsBroadcaster.getInstance().unregisterListener(propertyChangedListener);
				}

				List<BaseSugarEntry> sugarEntries = SugarWebservicesOperations.getInstance().getSugarEntries();
				if (sugarEntries.contains(getSugarEntry())) {
					sugarEntries.remove(getSugarEntry());
				}
			}

		});
	}

	public void afterBaseCardDataRetrieved() {

		setBaseDataRetrieved(true);
		setNameAfterRetrieve();

		try {
			// header image and tooltip
			if (getControl(MAINIMAGELABEL) != null) {
				setMainImageLabel((Label) getControl(MAINIMAGELABEL));
			}

			// Oppty card header
			if (getControl(AbstractDashboardComposite.ACCOUNTLABEL) != null) {
				((Label) getControl(AbstractDashboardComposite.ACCOUNTLABEL)).setText((getSugarEntry() == null || ((SugarOpportunity) getSugarEntry()).getAccountName() == null)
						? ConstantStrings.EMPTY_STRING
						: ((SugarOpportunity) getSugarEntry()).getAccountName());
			}
			// Account card header
			if (getControl(AbstractDashboardComposite.INDUSTRYLABEL) != null) {
				String industry = ConstantStrings.EMPTY_STRING;
				SugarAccount account = (SugarAccount) getSugarEntry();
				if (account != null && account.getIndustryMap() != null && !account.getIndustryMap().isEmpty() && account.getIndustryMap().keySet().size() > 0) {
					industry = account.getIndustryString();
				}
				((Label) getControl(AbstractDashboardComposite.INDUSTRYLABEL)).setText(getSugarEntry() == null ? ConstantStrings.EMPTY_STRING : escapeAmpersand(industry));
			}

			// Account card header - implicit following
			if (getControl(AbstractDashboardComposite.IMPLICITFOLLOWINGCOMPOSITE) != null) {
				Composite implicitFollowingComposite = (Composite) getControl(AbstractDashboardComposite.IMPLICITFOLLOWINGCOMPOSITE);
				Control[] controls = implicitFollowingComposite.getChildren();
				if (controls != null && controls.length > 0) {
					for (int i = 0; i < controls.length; i++) {
						controls[i].dispose();
					}
				}
				if (getSugarEntry() instanceof SugarAccount && isImplicitFollowing((SugarAccount) getSugarEntry())) {

					createImplicitFollowingFields(implicitFollowingComposite, (SugarAccount) getSugarEntry());
					UiUtils.recursiveSetBackgroundColor(implicitFollowingComposite, _parent.getBackground());
				} else {
					((GridData) implicitFollowingComposite.getLayoutData()).exclude = true;
					implicitFollowingComposite.setVisible(false);
				}
				implicitFollowingComposite.layout();
				implicitFollowingComposite.getParent().layout();

			}

			// Contact card header
			if (getControl(AbstractDashboardComposite.JOBLABEL) != null) {
				if (((SugarContact) getSugarEntry()).getJobTitle() != null && ((SugarContact) getSugarEntry()).getJobTitle().length() > 0) {
					((Label) getControl(AbstractDashboardComposite.JOBLABEL))
							.setText((getSugarEntry() == null || ((SugarContact) getSugarEntry()).getJobTitle() == null || ((SugarContact) getSugarEntry()).getJobTitle().length() == 0)
									? ConstantStrings.EMPTY_STRING
									: ((SugarContact) getSugarEntry()).getJobTitle());
				} else {
					((GridData) ((Label) getControl(AbstractDashboardComposite.JOBLABELSPACER)).getLayoutData()).exclude = true;
					((Label) getControl(AbstractDashboardComposite.JOBLABELSPACER)).setVisible(false);
					((GridData) ((Label) getControl(AbstractDashboardComposite.JOBLABEL)).getLayoutData()).exclude = true;
					((Label) getControl(AbstractDashboardComposite.JOBLABEL)).setVisible(false);
				}
			}

			if (getControl(AbstractDashboardComposite.ACCOUNTLINK) != null) {
				((SFAHyperlink) getControl(AbstractDashboardComposite.ACCOUNTLINK)).setText(getSugarEntry() == null ? ConstantStrings.EMPTY_STRING : getSugarEntry().getWebsite());

				if (getSugarEntry() != null && getSugarEntry().getWebsite() != null) {
					((SFAHyperlink) getControl(AbstractDashboardComposite.ACCOUNTLINK)).addHyperlinkListener(new HyperlinkAdapter() {
						@Override
						public void linkActivated(HyperlinkEvent e) {
							GenericUtils.launchUrlInPreferredBrowser(getSugarEntry().getWebsite(), false);
						}
					});
				}
			}

			// follow button in toolbar
			if (getControl(AbstractDashboardComposite.FOLLOWBUTTON) != null && getControl(AbstractDashboardComposite.FOLLOWITEM) != null) {
				SToolItem followItem = (SToolItem) getControl(AbstractDashboardComposite.FOLLOWITEM);
				SFAToggleButton followButton = (SFAToggleButton) getControl(AbstractDashboardComposite.FOLLOWBUTTON);
				setFollowButton(followItem, followButton);
			}
			if (toolbarComposite != null && !toolbarComposite.isDisposed()) {
				toolbarComposite.pack();
			}
			getParent().layout(true, true);

			if (_dropAdapter == null) {
				_dropAdapter = new SugarDashboardDropAdapter(_parent, getSugarEntry());
			}

		} catch (Exception e) {
			if (!this.isDisposed()) {
				UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
			}
		}
	};

	/**
	 * Sets the card's name information after the base card information has been retrieved. May be overriden by sublcasses.
	 */
	protected void setNameAfterRetrieve() {
		// Reset this to the proper name. For an account, we may initially set it to a site id if the user clicked on that for the live text.
		// After the data load, we'll have the proper name.
		if (getSugarEntry() instanceof SugarAccount) {
			nameLink.setText(getSugarEntry().getName() + " (" + ((SugarAccount) getSugarEntry()).getClientId() + ")"); //$NON-NLS-1$  //$NON-NLS-2$
			// 54953
			//Workbench.getInstance().getActiveWorkbenchWindow().getShell().setText(getSugarEntry().getName() + " (" + ((SugarAccount) getSugarEntry()).getClientId() + ")"); //$NON-NLS-1$  //$NON-NLS-2$
			this.getShell().setText(getSugarEntry().getName() + " (" + ((SugarAccount) getSugarEntry()).getClientId() + ")"); //$NON-NLS-1$  //$NON-NLS-2$
		} else {
			nameLink.setText(getSugarEntry().getName());
			// 54953
			// Workbench.getInstance().getActiveWorkbenchWindow().getShell().setText(getSugarEntry().getName());
			this.getShell().setText(getSugarEntry().getName());
		}
	}

	public void rebuildComposite() {
		prepareForRebuild();
		createHeaderComposite();
		createInnerComposite();
		createProgressComposite();

		Label separator = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		createToolbarComposite();

		prepareTask();
	}

	public void prepareTask() {
		// subclass should override this method.
	}

	/**
	 * Indicates if this composite is in a state where it should be allowed to be rebuilt. The default is true, subclasses can override.
	 * 
	 * @return
	 */
	public boolean hasUncommittedChanges() {
		return false;
	}

	/**
	 * Whenever the panel is about to be rebuilt, the default behavior will be to clobber all of the existing widgets. Subclasses may override.
	 */
	protected void prepareForRebuild() {

		if (!this.isDisposed()) {
			Control[] children = getChildren();
			for (Control child : children) {
				child.dispose();
			}
		}

		getControlMap().clear();
		setBaseDataRetrieved(false);
	}

	/**
	 * Rebuilds just the toolbar composite
	 */
	public void rebuildToolbarComposite() {
		if (toolbarComposite != null && !toolbarComposite.isDisposed()) {
			for (Control control : toolbarComposite.getChildren()) {
				control.dispose();
			}
		}
		createToolbarComposite();
		toolbarComposite.layout(true);
		layout(true);
	}

	/**
	 * Creates the composite that contains the toolbar
	 */
	public void createToolbarComposite() {
		// ////////////// The toolbar icon stuff is still kinda supported, but it was removed from the UI spec, so there ////////////////
		// ////////////// are no extension points that use this. ////////////////
		List<AbstractToolbarIconContributionAction> toolbarIcons = new ArrayList<AbstractToolbarIconContributionAction>();

		SortedSet<ToolbarIconContribution> contributionSet = ToolbarIconContributionExtensionProcessor.getInstance().getToolbarIconContributionSet();

		if (getSugarEntry() != null) {
			for (ToolbarIconContribution contrib : contributionSet) {
				if ((contrib.getDashboardIds().contains(getDashboardID()) || contrib.getDashboardIds().contains(ALL_DASHBOARDS)) && contrib.getSugarTypes().contains(getSugarType())) {
					try {
						Class actionClass = contrib.getBundle().loadClass(contrib.getActionClass());
						Constructor constructor = actionClass.getConstructor(BaseSugarEntry.class, ToolbarIconContribution.class);
						AbstractToolbarIconContributionAction toolbarIcon = (AbstractToolbarIconContributionAction) constructor.newInstance(sugarEntry, contrib);
						if (toolbarIcon.hasBuildableParts()) {
							toolbarIcons.add(toolbarIcon);
						}
					} catch (Exception e) {
						UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
					}
				}
			}
			// ///////////////////////////////////////////////////////////////////////////////////////////////////////////

			if (!toolbarIcons.isEmpty() || !getAllMenuItems().isEmpty()) {
				if (toolbarComposite == null || toolbarComposite.isDisposed()) {
					toolbarComposite = new Composite(this, SWT.NONE);
					toolbarComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(!toolbarIcons.isEmpty() && !getAllMenuItems().isEmpty() ? 2 : 1).spacing(0, 0).create());
					toolbarComposite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.END).grab(true, false).create());
				}
				if (!toolbarIcons.isEmpty()) {
					SToolBar toolbarIconComposite = new SToolBar(toolbarComposite, SWT.FLAT);
					toolbarIconComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(toolbarIcons.size()).create());
					toolbarIconComposite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).create());

					for (AbstractToolbarIconContributionAction toolbarIcon : toolbarIcons) {
						toolbarIcon.build(toolbarIconComposite);
					}
				}
				createToolbarCompositeForUndocked();
			}
		}
	}

	/**
	 * There used to be a docked view of the card, but we got rid of that. So this method name is a bit of a misnomer now. However, I just left it like this since it's a big chunk of code to roll back
	 * into createToolbarComposite().
	 */
	private void createToolbarCompositeForUndocked() {
		toolbarComposite.addControlListener(new ControlListener() {
			@Override
			public void controlMoved(ControlEvent arg0) {
			}

			@Override
			public void controlResized(ControlEvent arg0) {
				handleToolbarResize(toolbarComposite);
			}
		});

		sToolBar = new SToolBar(toolbarComposite, SWT.FLAT);
		sToolBar.setBackground(toolbarComposite.getBackground());

		// This buffer item is a brutal hack to get the menu items on the right side of the card.
		// Why you can't set layout information on these things is beyond me.
		leftBufferItem = new SToolItem(sToolBar, SWT.NONE);
		Button leftBufferButton = new Button(sToolBar, SWT.NONE);
		leftBufferItem.setControl(leftBufferButton);

		leftBufferItem.setText(ConstantStrings.EMPTY_STRING);
		leftBufferButton.setVisible(false);
		// Will adjust the width in resize listener
		leftBufferItem.setWidth(0);

		// Only create the follow button for accounts and opptys
		if (getSugarType() == SugarType.ACCOUNTS || getSugarType() == SugarType.OPPORTUNITIES) {

			/******************************************************************/
			// create Download Button
			if (this.getClass().getName().equals("com.ibm.socialcrm.notesintegration.files.dashboardcomposites.DocumentsComposite")) { //$NON-NLS-1$
				downloadButton = new SFAToggleButton(sToolBar, SWT.NONE);
				downloadButton.getButton().setVisible(true);

				downloadItem = new SToolItem(sToolBar, SWT.NONE);
				downloadItem.setControl(downloadButton.getButton());

				downloadButton.setText(GenericUtils.padStringWithSpaces(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_BUTTON)));

				GC gc1 = new GC(this);
				gc1.setFont(downloadButton.getFont());
				downloadButtonWidth = gc1.stringExtent(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_BUTTON)).x;
				downloadButtonWidth += 20; // Add 20 pixels for some internal padding
				gc1.dispose();

				downloadItem.setWidth(downloadButtonWidth);

				addDownloadButtonListener();

				// upload
				uploadButton = new SFAToggleButton(sToolBar, SWT.NONE);
				uploadButton.getButton().setVisible(true);

				uploadItem = new SToolItem(sToolBar, SWT.NONE);
				uploadItem.setControl(uploadButton.getButton());

				uploadButton.setText(GenericUtils.padStringWithSpaces(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_UPLOAD_BUTTON)));

				gc1 = new GC(this);
				gc1.setFont(uploadButton.getFont());
				uploadButtonWidth = gc1.stringExtent(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_UPLOAD_BUTTON)).x;
				uploadButtonWidth += 20; // Add 20 pixels for some internal padding
				gc1.dispose();

				uploadItem.setWidth(uploadButtonWidth);

				addUploadButtonListener();
			}

			followButton = new SFAToggleButton(sToolBar, SWT.NONE);
			// /** We want to hide this for 1.0 since they are removing ISP **/
			// 102312 - hide Following button
			followButton.getButton().setVisible(false);
			// followButton.getButton().setVisible(true);
			/******************************************************************/
			followItem = new SToolItem(sToolBar, SWT.NONE);
			followItem.setControl(followButton.getButton());
			setFollowButton(followItem, followButton);

			updateControlMap(FOLLOWITEM, followItem);
			updateControlMap(FOLLOWBUTTON, followButton);

		}

		moreActionsItem = createMoreActionsMenu();

		// Manually set the height of the follow button, otherwise it looks too short next to the more actions menu
		if (followItem != null) {
			Point p = followButton.getButton().getSize();
			followButton.getButton().setSize(p.x, moreActionsItem.getHeight());
		}

		if (downloadItem != null) {
			Point p = downloadButton.getButton().getSize();
			downloadButton.getButton().setSize(p.x, moreActionsItem.getHeight());
		}

		if (uploadItem != null) {
			Point p = uploadButton.getButton().getSize();
			uploadButton.getButton().setSize(p.x, moreActionsItem.getHeight());
		}

		toolbarComposite.pack();
	}

	private void setFollowButton(SToolItem followItem, SFAToggleButton followButton) {
		if (isBaseDataRetrieved) {
			boolean isFollow = isImplicitFollowing(getSugarEntry()) ? !getSugarEntry().isFollowed() : getSugarEntry().isFollowed();
			followButton.setText(GenericUtils.padStringWithSpaces((isFollow ? UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FOLLOWING) : UtilsPlugin.getDefault().getResourceString(
					UtilsPluginNLSKeys.FOLLOW))));

			GC gc = new GC(this);
			gc.setFont(followButton.getFont());
			followButtonWidth = Math.max(gc.stringExtent(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FOLLOWING)).x, gc.stringExtent(UtilsPlugin.getDefault().getResourceString(
					UtilsPluginNLSKeys.FOLLOW)).x);
			followButtonWidth += 20; // Add 20 pixels for some internal padding
			gc.dispose();

			followItem.setWidth(followButtonWidth);

			followButton.setPressed(getSugarEntry().isFollowed());

			addFollowButtonListener();
		} else {
			followButton.setText(ConstantStrings.EMPTY_STRING);
			followItem.setWidth(0);
		}
	}
	private void addFollowButtonListener() {

		followButton.getButton().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {

				boolean isContinue = true;

				if (isToImplicitFollow()) {
					int count = ((SugarAccount) getSugarEntry()).getNumOfRelatedClients();
					MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FOLLOW_DOMESTIC_CLIENT_TITLE), null,
							UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FOLLOW_DOMESTIC_CLIENT_TEXT, new String[]{Integer.toString(count)}), MessageDialog.QUESTION, new String[]{
									UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OK), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CANCEL)}, 1);
					int yesno = dialog.open();
					if (yesno == MessageDialog.OK) {
					} else {
						isContinue = false;
					}
				}

				if (isContinue) {
					final String progressId = getProgressDisplayer().createProgressIndicator(
							getSugarEntry().isFollowed() ? UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UNFOLLOWING_PROGRESS_MESSAGE) : UtilsPlugin.getDefault().getResourceString(
									UtilsPluginNLSKeys.FOLLOWING_PROGRESS_MESSAGE));

					followButton.setEnabled(false);

					Job job = new Job("Call follow service") //$NON-NLS-1$
					{
						@Override
						protected IStatus run(IProgressMonitor arg0) {
							try {
								boolean followed = getSugarEntry().isFollowed();
								StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
								sb.append("userid=").append(NotesAccountManager.getInstance().getCRMUser()).append(ConstantStrings.AMPERSAND); //$NON-NLS-1$
								sb.append("password=").append(NotesAccountManager.getInstance().getCRMPassword()).append((ConstantStrings.AMPERSAND)); //$NON-NLS-1$

								String output = SugarWebservicesOperations.getInstance().callFollowService(getSugarType().getParentType(), getSugarEntry().getId(), followed ? false : true);

								String followX = null;
								if (output != null) {
									try {
										JSONObject json = new JSONObject(output);

										if (json.has("result")) { //$NON-NLS-1$

											followX = json.getString("result"); //$NON-NLS-1$
										}

									} catch (Exception e) {
										e.printStackTrace();
									}
								}

								final boolean newFollowedState = (followX != null && followX.equalsIgnoreCase(ConstantStrings.DATABASE_FOLLOWED)) ? true : false;

								Display.getDefault().asyncExec(new Runnable() {
									@Override
									public void run() {
										getProgressDisplayer().removeProgressIndicator(progressId);
										getSugarEntry().setFollowed(newFollowedState);

										// update followButton of all the sibling tabs
										if (parentView.getSiblings() != null && !parentView.getSiblings().isEmpty()) {
											for (ISugarDashboardViewPart viewPart : parentView.getSiblings()) {
												AbstractDashboardComposite dashboardComposite = (AbstractDashboardComposite) viewPart.getDashboardComposite();
												if (dashboardComposite != null && dashboardComposite.getControlMap() != null && !dashboardComposite.getControlMap().isEmpty()
														&& dashboardComposite.getControlMap().get(FOLLOWBUTTON) != null) {
													SFAToggleButton followButton = (SFAToggleButton) dashboardComposite.getControlMap().get(FOLLOWBUTTON);
													followButton.setPressed(getSugarEntry().isFollowed());
													followButton.setText(GenericUtils.padStringWithSpaces((getSugarEntry().isFollowed() ? UtilsPlugin.getDefault().getResourceString(
															UtilsPluginNLSKeys.FOLLOWING) : UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FOLLOW))));

													followButton.setEnabled(true);
												}
											}
										}

										followButton.setPressed(getSugarEntry().isFollowed());
										followButton.setText(GenericUtils.padStringWithSpaces((getSugarEntry().isFollowed()
												? UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FOLLOWING)
												: UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FOLLOW))));

										followButton.setEnabled(true);
									}
								});
							} catch (Exception e) {
								UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
								getProgressDisplayer().removeProgressIndicator(progressId);
							}
							return Status.OK_STATUS;
						}
					};
					job.schedule();
				}
			}
		});

	}

	private boolean isToImplicitFollow() {
		boolean isToImplicitFollow = false;
		if (getSugarEntry() != null && getSugarEntry() instanceof SugarAccount) {
			// if this client is not followed && it is a DC/SC client
			if (!getSugarEntry().isFollowed() && ((SugarAccount) getSugarEntry()).isParent()) {
				if (((SugarAccount) getSugarEntry()).getNumOfRelatedClients() > 0) {
					isToImplicitFollow = true;
				}

			}
		}
		return isToImplicitFollow;
	}

	private void addDownloadButtonListener() {
		downloadButton.getButton().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {

				toDownload();

			}
		});
	}

	public void toDownload() {
		// subclass should override this method.
	}

	private void addUploadButtonListener() {
		uploadButton.getButton().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {

				toUpload();

			}
		});
	}

	public void toUpload() {
		// subclass should override this method.
	}

	/**
	 * Helper method to create the "More Actions" menu
	 */
	public SToolItem createMoreActionsMenu() {
		SToolItem itemToReturn = null;
		if (!getAllMenuItems().isEmpty()) {
			final SToolItem moreActionsItem = new SToolItem(sToolBar, SWT.DROP_DOWN);
			itemToReturn = moreActionsItem;
			moreActionsItem.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_MORE_ACTIONS));

			moreActionsItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					createDropMenu(e);
				}
			});
		}
		return itemToReturn;
	}

	// Mainly for undock scenario
	private void handleToolbarResize(Composite composite) {

		int toolBarWidth = getParent().getBounds().width - 22;

		int leftBufferWidth = toolBarWidth - (moreActionsItem == null ? 0 : moreActionsItem.getWidth()) - (followItem == null ? 0 : followItem.getWidth())
				- (downloadItem == null ? 0 : downloadItem.getWidth()) - (uploadItem == null ? 0 : uploadItem.getWidth() + 5);
		leftBufferItem.setWidth(leftBufferWidth);
		composite.pack();
		composite.getParent().setBackground(composite.getBackground());
	}

	/**
	 * Method that gets called to handle the toolbar being resized. We have to do some odd things to get the menu items right aligned because you can't set layout data on SToolItems.
	 * 
	 * @param sToolBar
	 * @param leftBufferItem
	 * @param followButton
	 * @param firstToolItem
	 *        - This is the first tool item to appear in the menu. In the normal card, it'll be the More Actions menu. If the card is docked, it's something else.
	 */
	private void handleToolbarResize(final SToolBar sToolBar, final SToolItem leftBufferItem, final SButton[] buttons) {
		if (buttons != null) {
			// adjusting the very right margin of the buttons
			leftBufferItem.setWidth(10);
		} else {
			// adjusting left margin
			((GridData) sToolBar.getLayoutData()).horizontalIndent = -5;
		}

	}

	/**
	 * Helper method to create the menu associated with the "More Action" toolbar item.
	 * 
	 * @param allMenuItems
	 * @param e
	 */
	private void createDropMenu(SelectionEvent e) {
		Menu dropMenu = new Menu(Display.getDefault().getActiveShell(), SWT.POP_UP);
		int categoriesIndex = 1;

		for (List<ToolbarMenuItemContribution> menuItemsForCat : getAllMenuItems()) {
			for (ToolbarMenuItemContribution menuItem : menuItemsForCat) {
				try {
					Class actionClass = menuItem.getBundle().loadClass(menuItem.getActionClass());
					Constructor constructor = actionClass.getConstructor(BaseSugarEntry.class, String.class);
					final AbstractToolbarMenuItem toolbarMenuItem = (AbstractToolbarMenuItem) constructor.newInstance(sugarEntry, menuItem.getId());
					toolbarMenuItem.setProgessDisplayer(getProgressDisplayer());

					MenuItem item = null;
					if (isSeparator(menuItem.getId())) {
						item = new MenuItem(dropMenu, SWT.SEPARATOR);
					} else {
						item = new MenuItem(dropMenu, SWT.NONE);
						item.setEnabled(toolbarMenuItem.shouldEnable());

						item.setText(toolbarMenuItem.getItemText());
						item.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent arg0) {
								toolbarMenuItem.onSelection();
							}
						});
					}
				} catch (Exception e1) {
					UtilsPlugin.getDefault().logException(e1, UiPluginActivator.PLUGIN_ID);
				}
			}
			if (allMenuItems.size() > categoriesIndex++) {
				new MenuItem(dropMenu, SWT.SEPARATOR);
			}
		}

		SToolItem item = (SToolItem) e.widget;
		SToolBar toolBar = item.getParent();

		Point point = toolBar.toDisplay(new Point(item.getBounds().x, item.getBounds().y));
		dropMenu.setLocation(point.x, point.y + item.getBounds().height);
		dropMenu.setVisible(true);
	}

	private List<ToolbarMenuItemContribution> createSeparator() {
		List<ToolbarMenuItemContribution> separatorList = new ArrayList<ToolbarMenuItemContribution>(1);
		ToolbarMenuItemContribution separator = new ToolbarMenuItemContribution();
		separator.setId(ConstantStrings.SEPARATOR);
		separatorList.add(separator);
		return separatorList;
	}

	private boolean isSeparator(String idX) {
		if (idX != null && idX.endsWith(ConstantStrings.SEPARATOR)) {
			return true;
		}
		return false;
	}

	/**
	 * Creates the header composite for this sugar entry
	 */
	public void createHeaderComposite() {
		if (getSugarEntry() != null) {
			Composite headerComposite = new Composite(this, SWT.NONE);
			headerComposite.setLayout(GridLayoutFactory.fillDefaults().spacing(SWT.DEFAULT, 0).numColumns(4).margins(HEADER_MARGIN, HEADER_MARGIN).create());
			headerComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

			Label mainImageLabel = new Label(headerComposite, SWT.NONE);
			mainImageLabel.setLayoutData(GridDataFactory.fillDefaults().span(1, 3).create());
			setMainImageLabel(mainImageLabel);
			updateControlMap(MAINIMAGELABEL, mainImageLabel);

			Label popoutLinkLabel = new Label(headerComposite, SWT.NONE);
			popoutLinkLabel.setImage(SFAImageManager.getImage(SFAImageManager.EXTERNAL_LINK));
			popoutLinkLabel.setLayoutData(GridDataFactory.fillDefaults().indent(10, 0).hint(15, SWT.DEFAULT).create());
			popoutLinkLabel.setToolTipText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OPEN_IN_SUGAR));

			nameLink = new SFAHyperlink(headerComposite, SWT.NONE, true);
			nameLink.setLayoutData(GridDataFactory.fillDefaults().span(1, 1).create());
			nameLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
			nameLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
			nameLink.setText(getSugarEntry().getName());
			nameLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent evt) {
					GenericUtils.launchUrlInPreferredBrowser(getSugarEntry().getSugarUrl(), true);
				}
			});

			new Label(headerComposite, SWT.NONE); // Spacer label , column 4

			if (getSugarType() == SugarType.CONTACTS) {
				createContactHeaderFields(headerComposite);
			} else if (getSugarType() == SugarType.ACCOUNTS) {
				createAccountHeaderFields(headerComposite);
			} else if (getSugarType() == SugarType.OPPORTUNITIES) {
				createOpportunitiesHeaderFields(headerComposite);
			}

			Label separator = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
			separator.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		}
	}

	private void setMainImageLabel(Label mainImageLabel) {
		if (mainImageLabel != null && getSugarEntry() != null) {
			mainImageLabel.setImage(getImageForSugarEntry(getSugarEntry()));
			if (getSugarEntry() instanceof SugarAccount) {
				mainImageLabel.setToolTipText(((SugarAccount) getSugarEntry()).getIndustryTooltip());
			} else if (getSugarEntry() instanceof SugarContact) {
				mainImageLabel.setToolTipText(((SugarContact) getSugarEntry()).getName());
			} else if (getSugarEntry() instanceof SugarOpportunity) {
				mainImageLabel.setToolTipText(((SugarOpportunity) getSugarEntry()).getIndustryTooltip());
			}
		}
	}
	public void createTitleComposite() {
		Composite titleComposite = new Composite(this, SWT.NONE);
		titleComposite.setLayout(GridLayoutFactory.fillDefaults().margins(5, 5).create());
		titleComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		titleComposite.setBackground(SugarItemsDashboard.getInstance().getToolbarBackgroundColor());

		titleLabel = new Label(titleComposite, SWT.BOLD);
		if (getDashboardContribution() != null) {
			if (getDashboardContribution().getDockedDisplayName().length() > 0) {
				titleLabel.setText(getDashboardContribution().getDockedDisplayName());
			} else {
				titleLabel.setText(getDashboardName());
			}
		} else {
			titleLabel.setText(getDashboardName());
		}
		titleLabel.setLayoutData(GridDataFactory.fillDefaults().indent(7, 0).create());
		titleLabel.setBackground(SugarItemsDashboard.getInstance().getToolbarBackgroundColor());
		titleLabel.setForeground(SugarItemsDashboard.getInstance().getToolbarMenuColor());
		titleLabel.setFont(SugarItemsDashboard.getInstance().getTableHeaderFont());
	}

	/**
	 * Creates the progress composite
	 */
	private void createProgressComposite() {
		progressComposite = new Composite(this, SWT.NONE);
		progressComposite.setLayout(GridLayoutFactory.fillDefaults().create());
		progressComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		progressComposite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));
		((GridData) (progressComposite.getLayoutData())).exclude = true;
		progressComposite.setVisible(false);
	}

	/**
	 * 
	 * @return
	 */
	public Composite getProgressComposite() {
		return progressComposite;
	}

	/**
	 * Creates the contact specific fields for the header composite
	 * 
	 * @param headerComposite
	 */
	private void createContactHeaderFields(Composite headerComposite) {
		SugarContact contact = (SugarContact) getSugarEntry();
		// spacer label
		Label spacer = new Label(headerComposite, SWT.NONE);
		spacer.setLayoutData(GridDataFactory.fillDefaults().create());
		updateControlMap(JOBLABELSPACER, spacer);

		Label jobLabel = new Label(headerComposite, SWT.NONE);
		jobLabel.setText((contact == null || contact.getJobTitle() == null || contact.getJobTitle().length() == 0) ? ConstantStrings.EMPTY_STRING : contact.getJobTitle());
		jobLabel.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());
		jobLabel.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
		jobLabel.setForeground(SugarItemsDashboard.getInstance().getBusinessCardHeaderTextColor());
		updateControlMap(JOBLABEL, jobLabel);

		createWebsiteFields(headerComposite, contact);
	}

	/**
	 * Creates the account specific fields for the header composite
	 * 
	 * @param headerComposite
	 */
	private void createAccountHeaderFields(Composite headerComposite) {
		final SugarAccount account = (SugarAccount) getSugarEntry();

		// spacer label (column 2)
		new Label(headerComposite, SWT.NONE);

		// column 3 and 4
		Label industryLabel = new Label(headerComposite, SWT.NONE);
		String industry = ConstantStrings.EMPTY_STRING;
		if (account != null && account.getIndustryMap() != null && !account.getIndustryMap().isEmpty() && account.getIndustryMap().keySet().size() > 0) {
			industry = account.getIndustryString();
		}
		industryLabel.setText(escapeAmpersand(industry));
		industryLabel.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());
		industryLabel.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
		industryLabel.setForeground(SugarItemsDashboard.getInstance().getBusinessCardHeaderTextColor());
		industryLabel.setToolTipText(account.getIndustryTooltip());
		updateControlMap(INDUSTRYLABEL, industryLabel);

		createWebsiteFields(headerComposite, account);

		// spacer label
		new Label(headerComposite, SWT.NONE);
		new Label(headerComposite, SWT.NONE);

		// You are currently following this site via Domestic Client Name (DClient ID)
		// Composite implicitFollowingComposite = new Composite(headerComposite, SWT.NONE);
		implicitFollowingComposite = new Composite(headerComposite, SWT.NONE);

		implicitFollowingComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).create());
		// 47327 Fixing layout
		implicitFollowingComposite.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.BEGINNING).grab(true, false).create());

		// 49021
		implicitFollowingComposite.getParent().addPaintListener(new PaintListener() {

			@Override
			public void paintControl(PaintEvent arg0) {
				if (implicitFollowingComposite != null) {
					int labelLen = 0;
					GC gc = new GC(implicitFollowingComposite);
					Point size = gc.textExtent(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.IMPLICIT_FOLLOWING)); // or textExtent
					labelLen = size.x;
					gc.dispose();
					// set implicitFollowingComposite width to the parent width, so DC client name ellipsize logic will work...
					// It seems the parent size - 1/2 of implicit follow label width works fine for now... might need to adjust.
					implicitFollowingComposite.setBounds(implicitFollowingComposite.getBounds().x, implicitFollowingComposite.getBounds().y, implicitFollowingComposite.getParent().getBounds().width
							- labelLen / 2, implicitFollowingComposite.getBounds().height);
					implicitFollowingComposite.redraw();
				}

			}

		});

		createImplicitFollowingFields(implicitFollowingComposite, account);
		updateControlMap(IMPLICITFOLLOWINGCOMPOSITE, implicitFollowingComposite);

	}

	/**
	 * Creates the oppty specific fields for the header composite
	 * 
	 * @param headerComposite
	 */
	private void createOpportunitiesHeaderFields(Composite headerComposite) {
		final SugarOpportunity oppty = (SugarOpportunity) getSugarEntry();

		// spacer label
		new Label(headerComposite, SWT.NONE);

		Label accountLabel = new Label(headerComposite, SWT.NONE);
		accountLabel.setText((oppty == null || oppty.getAccountName() == null) ? ConstantStrings.EMPTY_STRING : oppty.getAccountName());
		accountLabel.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());
		accountLabel.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
		accountLabel.setForeground(SugarItemsDashboard.getInstance().getBusinessCardHeaderTextColor());
		updateControlMap(ACCOUNTLABEL, accountLabel);

		createWebsiteFields(headerComposite, oppty);
	}

	/**
	 * Utility method that creates a label and a link to a website
	 * 
	 * @param parent
	 */
	private void createWebsiteFields(Composite parent, final BaseSugarEntry sugarEntry) {
		// spacer label (Column 2)
		new Label(parent, SWT.NONE);

		// column 3 and 4
		Composite webAddressComposite = new Composite(parent, SWT.NONE);
		webAddressComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).create());
		webAddressComposite.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).grab(true, false).create());

		Label webAddressLabel = new Label(webAddressComposite, SWT.NONE);
		webAddressLabel.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
		webAddressLabel.setForeground(SugarItemsDashboard.getInstance().getBusinessCardHeaderTextColor());
		webAddressLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_ADDRESS_LABEL));
		// 80623 - for CI Simplification, remove web address
		webAddressLabel.setVisible(false);

		SFAHyperlink accountLink = new SFAHyperlink(webAddressComposite, SWT.NONE, true);
		// accountLink.setLayoutData(GridDataFactory.fillDefaults().span(1, 1).indent(0, GenericUtils.getPlatformHyperlinkVerticalIndent()).create());
		accountLink.setLayoutData(GridDataFactory.fillDefaults().span(1, 1).indent(0, GenericUtils.isMac() ? GenericUtils.MAC_HYPERLINK_VERTICAL_INDENT : 0).create());
		accountLink.setText((sugarEntry == null || sugarEntry.getWebsite() == null) ? ConstantStrings.EMPTY_STRING : sugarEntry.getWebsite());
		accountLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
		accountLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());

		// 80623 - for CI Simplification, remove web address
		accountLink.setVisible(false);
		if (sugarEntry != null && sugarEntry.getWebsite() != null) {
			accountLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					GenericUtils.launchUrlInPreferredBrowser(sugarEntry.getWebsite(), false);
				}
			});
		}
		updateControlMap(ACCOUNTLINK, accountLink);
	}

	private boolean isImplicitFollowing(BaseSugarEntry sugarEntry) {
		boolean isImplicitFollowing = false;
		// if this is a site client and this site client is followed and this client's followingparentid is not null
		if (sugarEntry != null && sugarEntry instanceof SugarAccount && !(((SugarAccount) sugarEntry).isParent()) && ((SugarAccount) sugarEntry).isFollowed()
				&& (((SugarAccount) sugarEntry).getFollowingParentId() != null)) {
			isImplicitFollowing = true;
		}
		return isImplicitFollowing;
	}

	private void createImplicitFollowingFields(Composite parent, final SugarAccount account) {
		if (isImplicitFollowing(account)) {

			Label implicitFollowingX = new Label(parent, SWT.NONE);
			implicitFollowingX.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
			implicitFollowingX.setForeground(SugarItemsDashboard.getInstance().getBusinessCardHeaderTextColor());
			implicitFollowingX.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.IMPLICIT_FOLLOWING) + ConstantStrings.SPACE);

			SFAHyperlink parentAccountLink = new SFAHyperlink(parent, SWT.NONE, true);
			// accountLink.setLayoutData(GridDataFactory.fillDefaults().span(1, 1).indent(0, GenericUtils.getPlatformHyperlinkVerticalIndent()).create());
			parentAccountLink.setLayoutData(GridDataFactory.fillDefaults().span(1, 1).indent(0, GenericUtils.isMac() ? GenericUtils.MAC_HYPERLINK_VERTICAL_INDENT : 0).create());
			StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
			final String parentCCMSId = account.getFollowingParentCCMSId();
			final String parentName = account.getFollowingParentName();
			final String parentId = account.getFollowingParentId();
			sb.append(parentName == null ? ConstantStrings.EMPTY_STRING : parentName);
			if (parentName != null && !parentName.equals(ConstantStrings.EMPTY_STRING)) {
				sb.append(ConstantStrings.SPACE).append(ConstantStrings.LEFT_PARENTHESIS).append(parentCCMSId).append(ConstantStrings.RIGHT_PARENTHESIS);
			}
			parentAccountLink.setText(sb.toString());
			parentAccountLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
			parentAccountLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
			if (parentCCMSId != null) {
				parentAccountLink.addHyperlinkListener(new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent e) {

						StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
						sb.append(NotesAccountManager.getInstance().getCRMServer()).append("/index.php").append("?module=").append("Accounts").append("&action=DetailView&record=").append(parentId); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						if (GenericUtils.isUseEmbeddedBrowserPreferenceSet())
							GenericUtils.launchUrlInPreferredBrowser(sb.toString(), false);
						else
							GenericUtils.launchUrlInPreferredBrowser(SugarWebservicesOperations.getInstance().buildV10SeamlessURL(sb.toString()), false);
					}
				});
			}

			if (followButton != null && followButton.getButton() != null && !followButton.getButton().isDisposed()) {
				// this is implicit follow, set followbutton text to Follow and disable the button
				followButton.setPressed(false);
				followButton.setEnabled(false);
			}
		}

	}
	public void setSelectedTitle(String selectedTitle) {
		this.selectedTitle = selectedTitle;
	}

	protected SFAHyperlink createHyperLinkComposite(Composite parent, String value, boolean externalLink, boolean useBoldFont) {
		SFAHyperlink hyperLink = null;
		if (value != null && !value.equals(ConstantStrings.EMPTY_STRING)) {
			Composite hyperLinkComposite = new Composite(parent, SWT.NONE);
			hyperLinkComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(externalLink ? 2 : 1).equalWidth(false).spacing(0, 0).create());
			hyperLinkComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
			hyperLink = new SFAHyperlink(hyperLinkComposite, SWT.NONE);
			hyperLink.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).grab(false, false).create());
			hyperLink.setText(value);
			hyperLink.setForeground(JFaceColors.getHyperlinkText(Display.getCurrent()));
			hyperLink.setUnderlined(true);
			if (useBoldFont) {
				hyperLink.setFont(SugarItemsDashboard.getInstance().getBoldFontForBusinessCardData());
			} else {
				hyperLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
			}
			if (externalLink) {
				Label externalLinkLabel = new Label(hyperLinkComposite, SWT.NONE);
				externalLinkLabel.setImage(SFAImageManager.getImage(SFAImageManager.EXTERNAL_LINK));
				externalLinkLabel.setLayoutData(GridDataFactory.fillDefaults().indent(0, -3).create());
			}
		}
		return hyperLink;
	}

	protected void createTextValueComposite(Composite parent, String labelText, String value, int labelWidth) // , String widgetId, boolean bold)
	{
		if (labelText != null) {
			Label label = new Label(parent, SWT.NONE);
			label.setLayoutData(GridDataFactory.fillDefaults().hint((labelWidth == -1 || labelWidth == 0) ? SWT.DEFAULT : labelWidth, SWT.DEFAULT).align(SWT.BEGINNING, SWT.BEGINNING).create());
			label.setText(labelText);
			label.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
			label.setForeground(SugarItemsDashboard.getInstance().getBusinessCardFieldLabelColor());

		}

		Label valueLabel = new Label(parent, SWT.WRAP);
		valueLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).grab(true, false).create());
		valueLabel.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
		valueLabel.setText(value == null ? ConstantStrings.EMPTY_STRING : value);
		valueLabel.setForeground(SugarItemsDashboard.getInstance().getBusinessCardFieldTextColor());
		updateControlMap(labelText, valueLabel);

	}

	/**
	 * Called whenever the selected items for this composite change
	 */
	public abstract void selectedItemsChanged();

	/**
	 * Builds the composite
	 */
	public abstract void createInnerComposite();

	/**
	 * Returns the name of the dashboard (or the name that appears in the tab)
	 * 
	 * @return
	 */
	public abstract String getDashboardName();

	/*
	 * Update the inner composite when data were retrieved
	 */
	public void updateInnerCompositeWithData() {
		// Sub classes may override as necessary
	}

	public void setDashboardID(String dashboardID) {
		this.dashboardID = dashboardID;
	}

	public String getDashboardID() {
		return dashboardID;
	}

	public void setSugarEntry(BaseSugarEntry sugarEntry) {
		this.sugarEntry = sugarEntry;
	}

	public BaseSugarEntry getSugarEntry() {
		return sugarEntry;
	}

	public SugarType getSugarType() {
		SugarType type = SugarType.NONE;
		if (getSugarEntry() != null) {
			type = getSugarEntry().getSugarType();
		}
		return type;
	}

	/**
	 * We need to make this toolbar accessible to other classes (namely the SugarEntryDashboardPanel) so that it can add a menu item. We don't need to expose the SWT toolbar in this way.
	 * 
	 * @return
	 */
	public SToolBar getSToolBar() {
		return sToolBar;
	}

	public DashboardContribution getDashboardContribution() {
		return dashboardContribution;
	}

	public void setDashboardContribution(DashboardContribution dashboardContribution) {
		this.dashboardContribution = dashboardContribution;
	}

	public IProgressDisplayer getProgressDisplayer() {
		return progressDisplayer;
	}

	public void setProgressDisplayer(IProgressDisplayer progressDisplayer) {
		this.progressDisplayer = progressDisplayer;
	}

	/**
	 * Returns a list of all menu items that are applicable to this composite. It's a List of Lists where each high level list represents a category.
	 * 
	 * @return
	 */
	public List<List<ToolbarMenuItemContribution>> getAllMenuItems() {
		if (allMenuItems == null) {
			allMenuItems = new ArrayList<List<ToolbarMenuItemContribution>>();
			SortedMap<MenuItemCategoryContribution, SortedSet<ToolbarMenuItemContribution>> menuItemsMap = ToolbarMenuItemContributionExtensionProcessor.getInstance()
					.getToolbarMenuItemContributionMap();

			if (menuItemsMap != null && !menuItemsMap.isEmpty()) {
				int displayCategoryCount = 0;
				for (Set<ToolbarMenuItemContribution> menuItems : menuItemsMap.values()) {
					List<ToolbarMenuItemContribution> itemsForThisCategory = new ArrayList<ToolbarMenuItemContribution>();
					for (ToolbarMenuItemContribution menuItem : menuItems) {
						if ((menuItem.getDashboardIds().contains(getDashboardID()) || menuItem.getDashboardIds().contains(ALL_DASHBOARDS)) && menuItem.getSugarTypes().contains(getSugarType())) {
							itemsForThisCategory.add(menuItem);
						}
					}
					if (!itemsForThisCategory.isEmpty()) {
						// add separator between categories
						if (displayCategoryCount > 1) {
							allMenuItems.add(createSeparator());
						}
						displayCategoryCount++;
						allMenuItems.add(itemsForThisCategory);
					}
				}
			}
		}
		return allMenuItems;
	}

	public void setTitleLabelText(String text) {
		if (titleLabel != null && text != null) {
			titleLabel.setText(text);
		}
	}

	public void setParentViewPart(ISugarDashboardViewPart parentView) {
		this.parentView = parentView;
	}

	public ISugarDashboardViewPart getParentViewPart() {
		return this.parentView;
	}

	/**
	 * Returns a map of industry codes to image constants
	 * 
	 * @return
	 */
	public static Map<String, String> getIndustryIconMap() {
		if (industryIconMap == null) {
			industryIconMap = new HashMap<String, String>();

			industryIconMap.put(ConstantStrings.INDUSTRY_AEROSPACE, SFAImageManager.INDUSTRY_AEROSPACE);
			industryIconMap.put(ConstantStrings.INDUSTRY_AUTOMOTIVE, SFAImageManager.INDUSTRY_AUTOMOTIVE);
			industryIconMap.put(ConstantStrings.INDUSTRY_BANKING, SFAImageManager.INDUSTRY_BANKING);
			industryIconMap.put(ConstantStrings.INDUSTRY_CHEMICALS, SFAImageManager.INDUSTRY_CHEMICAL);
			industryIconMap.put(ConstantStrings.INDUSTRY_COMPUTER_SERVICES, SFAImageManager.INDUSTRY_TECHNOLOGY);

			industryIconMap.put(ConstantStrings.INDUSTRY_CONSUMER_PRODUCTS, SFAImageManager.INDUSTRY_RETAIL);
			industryIconMap.put(ConstantStrings.INDUSTRY_CONSULTING, SFAImageManager.INDUSTRY_CONSULTING);
			industryIconMap.put(ConstantStrings.INDUSTRY_EDUCATION, SFAImageManager.INDUSTRY_EDUCATION);
			industryIconMap.put(ConstantStrings.INDUSTRY_EXCLUSIONS, SFAImageManager.INDUSTRY_OTHER);
			industryIconMap.put(ConstantStrings.INDUSTRY_ELECTRONICS, SFAImageManager.INDUSTRY_ENGINEERING);
			industryIconMap.put(ConstantStrings.INDUSTRY_ENERGY, SFAImageManager.INDUSTRY_ENERGY);
			industryIconMap.put(ConstantStrings.INDUSTRY_FINANCE, SFAImageManager.INDUSTRY_FINANCE);
			industryIconMap.put(ConstantStrings.INDUSTRY_GOVERNMENT_FEDERAL, SFAImageManager.INDUSTRY_GOVERNMENT);
			industryIconMap.put(ConstantStrings.INDUSTRY_GOVERNMENT_LOCAL, SFAImageManager.INDUSTRY_GOVERNMENT);
			industryIconMap.put(ConstantStrings.INDUSTRY_HEALTHCARE, SFAImageManager.INDUSTRY_HEALTHCARE);
			industryIconMap.put(ConstantStrings.INDUSTRY_INDUSTRIAL, SFAImageManager.INDUSTRY_UTILITIES);
			industryIconMap.put(ConstantStrings.INDUSTRY_INSURANCE, SFAImageManager.INDUSTRY_INSURANCE);
			industryIconMap.put(ConstantStrings.INDUSTRY_LIFE_SCIENCE, SFAImageManager.INDUSTRY_ENVIRONMENTAL);
			industryIconMap.put(ConstantStrings.INDUSTRY_MEDIA, SFAImageManager.INDUSTRY_ENTERTAINMENT);
			industryIconMap.put(ConstantStrings.INDUSTRY_RETAIL, SFAImageManager.INDUSTRY_RETAIL);
			industryIconMap.put(ConstantStrings.INDUSTRY_TRAVEL, SFAImageManager.INDUSTRY_TRANSPORTATION);
			industryIconMap.put(ConstantStrings.INDUSTRY_TELECOM, SFAImageManager.INDUSTRY_TELECOM);
			industryIconMap.put(ConstantStrings.INDUSTRY_WHOLESALE_DISTRIBUTION, SFAImageManager.INDUSTRY_SHIPPING);

			industryIconMap.put(ConstantStrings.INDUSTRY_OTHER, SFAImageManager.INDUSTRY_OTHER);
		}
		return industryIconMap;
	}

	/**
	 * Returns an appropriate header image for the given sugar entry
	 * 
	 * @param entry
	 * @return
	 */
	public static Image getImageForSugarEntry(BaseSugarEntry entry) {
		Image image = SFAImageManager.getImage(SFAImageManager.INDUSTRY_OTHER);

		String industryCode = null;
		
		if (entry instanceof SugarContact) {
			image = SFAImageManager.getImage(SFAImageManager.INDUSTRY_CONSULTING);
		} else if (entry instanceof SugarAccount) {
			industryCode = ((SugarAccount) entry).getIndusIndustry();
			} else if (entry instanceof SugarOpportunity) {
			industryCode = ((SugarOpportunity) entry).getIndusIndustry();
		}

		if (industryCode != null) {
			if (industryCode.length() > 1) {
				image = SFAImageManager.getImage(SFAImageManager.INDUSTRY_MULTIPLE);
			} else if (industryCode.length() == 0) {
				image = SFAImageManager.getImage(SFAImageManager.INDUSTRY_OTHER);
			} else if (industryCode.length() == 1) {
				industryCode = industryCode.toUpperCase();
				String imageKey = getIndustryIconMap().get(industryCode);
				if (imageKey == null) {
					image = SFAImageManager.getImage(SFAImageManager.INDUSTRY_OTHER);
				} else {
					image = SFAImageManager.getImage(imageKey);
				}
			}
		}
		return image;
	}

	public Map<String, Object> getControlMap() {
		if (controlMap == null) {
			controlMap = new HashMap<String, Object>();
		}
		return controlMap;
	}
	public void updateControlMap(String controlName, Object control) {
		if (controlName != null && control != null) {
			getControlMap().put(controlName, control);
		}
	}
	public Object getControl(String controlName) {
		Object control = null;
		if (getControlMap().containsKey(controlName)) {
			control = getControlMap().get(controlName);
		}
		return control;
	}

	public void packTable(Table table, int[] columnsToPack) {
		if (table != null && !table.isDisposed() && columnsToPack != null && columnsToPack.length > 0) {
			TableColumn[] columns = table.getColumns();
			for (int i = 0; i < columns.length; i++) {
				for (int j = 0; j < columnsToPack.length; j++) {
					if (i == columnsToPack[j]) {
						columns[i].pack();
						break;
					}
				}

			}
		}

	}

	protected void setBaseDataRetrieved(boolean b) {
		isBaseDataRetrieved = b;
	}

	public boolean isBaseDataRetrieved() {
		return isBaseDataRetrieved;
	}

	protected String escapeAmpersand(String s) {

		if (s.contains("&"))
			s = s.replaceAll("&", "&&");

		return s;
	}
	
}
