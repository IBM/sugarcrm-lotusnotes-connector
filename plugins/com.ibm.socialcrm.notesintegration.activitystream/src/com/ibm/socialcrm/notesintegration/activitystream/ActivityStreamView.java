package com.ibm.socialcrm.notesintegration.activitystream;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.apache.commons.json.JSONStringer;
import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;

import com.ibm.rcp.fiesta.service.metadata.GadgetMetadataResult;
import com.ibm.rcp.opensocial.container.CommonContainerViewPart;
import com.ibm.rcp.opensocial.container.NavigateCallback;
import com.ibm.rcp.opensocial.container.renderparams.GadgetRenderParams;
import com.ibm.rcp.toolbox.Widget;
import com.ibm.rcp.toolbox.internal.stores.PalleteStore;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.IProgressDisplayer;
import com.ibm.socialcrm.notesintegration.core.SugarAccount;
import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.core.SugarOpportunity;
import com.ibm.socialcrm.notesintegration.core.ui.views.ISugarDashboardViewPart;
import com.ibm.socialcrm.notesintegration.core.ui.views.SugarDashboardViewPart;
import com.ibm.socialcrm.notesintegration.core.uipluginbridge.IDashboardComposite;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.core.utils.UpdateSelectionsBroadcaster;
import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.ui.dashboardcomposites.AbstractDashboardComposite;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFAHyperlink;

public class ActivityStreamView extends CommonContainerViewPart implements ISugarDashboardViewPart {

	private BaseSugarEntry sugarEntry = null;

	private Composite parent;

	private IMemento input;

	public PropertyChangeListener propertyChangedListener = null;

	/*
	 * A map of controls whose UI value should be updated after card data becomes available
	 */
	private Map<String, Object> controlMap = null;

	/**
	 * All of these dashboard view parts will exist on a card (or perspective) together. When the user clicks the refresh action, we need to refresh all of the cards. Each view part will have a list
	 * of it's siblings so it can refresh everything on the card.
	 */
	private List<ISugarDashboardViewPart> siblings;

	private IAction refreshAction;

	private Widget asWidget = null;

	public ActivityStreamView() {

	}

	@Override
	public void init(IViewSite viewSite, IMemento input) throws PartInitException {
		super.init(viewSite, input);
		setPartName(getTitle());
		if (input != null) {
			String sugarId = input.getString(SugarDashboardViewPart.SUGAR_ID);
			sugarEntry = SugarWebservicesOperations.getInstance().getSugarEntryById(sugarId);
			this.input = input;
			createPartControl(parent);

			refreshAction = new Action() {

				@Override
				public String getText() {
					return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.REFRESH);
				}

				@Override
				public ImageDescriptor getImageDescriptor() {
					return ImageDescriptor.createFromImage(SFAImageManager.getImage(SFAImageManager.REFRESH_ICON));
				}

				@Override
				public void run() {
					refreshAll();
				}
			};

			IActionBars actionBars = getViewSite().getActionBars();
			IMenuManager dropDownMenu = actionBars.getMenuManager();
			dropDownMenu.add(refreshAction);
		}
	}

	public void createPartControl(Composite parent) {

		if (input == null) {
			this.parent = parent;
		} else {
			Composite c = new Composite(parent, SWT.NONE);
			c.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
			c.setLayout(GridLayoutFactory.fillDefaults().create());

			createHeaderComposite(c);

			addListeners(parent);

			Composite streamComposite = new Composite(c, SWT.NONE);

			streamComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
			streamComposite.setLayout(GridLayoutFactory.fillDefaults().margins(5, 0).create());

			super.createPartControl(streamComposite);
			streamComposite.getChildren()[0].setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

			Widget widgets[] = PalleteStore.getInstance().getPalleteItems();

			String activityStreamWidgetName = SugarWebservicesOperations.getInstance().getActivityStreamWidgetName();
			String[] serverWidgets = activityStreamWidgetName.split("\\|"); //$NON-NLS-1$
			for (Widget installedWidget : widgets) {
				for (String serverWidget : serverWidgets) {
					if (installedWidget.getTitle().equals(serverWidget)) {
						asWidget = installedWidget;
						break;
					}
				}
			}
			if (asWidget == null) {
				// The ActivityStream widget is not installed
				// Show an error or maybe don't even show the tab
			} else {
				this.navigateGadget(asWidget.getUrl(), getRenderParams(), getViewParams(), new NavigateCallback() {
					@Override
					public void onNavigate(GadgetMetadataResult metadata) {
						// This function will be called when the gadget renders passing some basic metadata about the gadget
						// It is good practice to make sure there are no errors in the metadata
						if (metadata == null || (metadata != null && metadata.hasError())) {
							// There was some kind of error rendering the gadget show some error UI
						}
					}
				});
			}
			createAdditionalParts(c);
		}
	}

	/**
	 * For the base AS view, this adds a post button
	 * 
	 * @param c
	 */
	protected void createAdditionalParts(Composite c) {
		Composite updateComposite = new Composite(c, SWT.NONE);
		updateComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).create());
		updateComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		final Text commentArea = new Text(updateComposite, SWT.MULTI | SWT.BORDER | SWT.WRAP);
		commentArea.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 30).create());

		final Button postButton = new Button(updateComposite, SWT.PUSH);
		postButton.setText("Post update");
		postButton.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).create());

		commentArea.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent arg0) {
				postButton.setEnabled(commentArea.getText().trim().length() > 0);
			}
		});

		postButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				String postResponse = SugarWebservicesOperations.getInstance().callPostStatusUpdate(getSugarEntry().getSugarType().getParentType(), getSugarEntry().getId(), commentArea.getText());
				// TODO: Interrogate the post response and decide what to do. We should probably refresh the AS and clear the post field on successful post.
			}
		});
		postButton.setEnabled(commentArea.getText().trim().length() > 0);
	}

	private void addListeners(final Composite parent) {

		propertyChangedListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt != null && evt.getPropertyName() != null && evt.getPropertyName().equals(UpdateSelectionsBroadcaster.BASECARD_DATA_RETRIEVED) && evt.getNewValue() != null) {
					// Be sure this change event is for me
					if (evt.getNewValue() != null && evt.getNewValue() instanceof BaseSugarEntry && ((BaseSugarEntry) evt.getNewValue()).getId() != null && sugarEntry != null
							&& sugarEntry.getId() != null && ((BaseSugarEntry) evt.getNewValue()).getId().equals(sugarEntry.getId())) {
						try {
							afterBaseCardDataRetrieved();
						} catch (SWTException e) {
							if (parent.isDisposed()) {
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
		try {
			// header image and tooltip
			if (getControl(AbstractDashboardComposite.MAINIMAGELABEL) != null) {
				setMainImageLabel((Label) getControl(AbstractDashboardComposite.MAINIMAGELABEL));
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
				((Label) getControl(AbstractDashboardComposite.INDUSTRYLABEL)).setText(getSugarEntry() == null ? ConstantStrings.EMPTY_STRING : industry);
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
			parent.layout(true, true);
		} catch (Exception e) {
			if (!parent.isDisposed()) {
				UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
			}
		}
	};

	private GadgetRenderParams getRenderParams() {
		GadgetRenderParams renderParams = new GadgetRenderParams();
		try {
			// The AS gadget also has a profile view which is what is show when rendered in the sidebar of Notes
			// The canvas view is what is show when rendered in a tab in Notes
			renderParams.setView("canvas"); //$NON-NLS-1$
			renderParams.setHeight("100%"); //$NON-NLS-1$
			renderParams.setWidth("100%"); //$NON-NLS-1$
		} catch (JSONException e) {
			e.printStackTrace();
			// Maybe you even want to throw this exception
		}
		return renderParams;
	}

	protected JSONObject getViewParams() {

		String output = getFilterJSON();

		JSONObject params = null;
		try {
			final JSONObject responseObj = new JSONObject(output);
			String connectionsServer = SugarWebservicesOperations.getInstance().getConnectionsURL();

			JSONStringer stringer = new JSONStringer();
			stringer.object().key("definitionUrl").value(connectionsServer + "/common/resources/web/com.ibm.social.as/gadget/ActivityStreamNotes.xml"); //$NON-NLS-1$ //$NON-NLS-2$
			stringer.key("placement").value("paymentDom"); //$NON-NLS-1$ //$NON-NLS-2$
			stringer.key("componentType").value("gadget"); //$NON-NLS-1$ //$NON-NLS-2$
			stringer.key("instanceData").object(); //$NON-NLS-1$
			stringer.key("renderType").value("default"); //$NON-NLS-1$ //$NON-NLS-2$
			stringer.key("renderParams").object(); //$NON-NLS-1$ 
			stringer.key("view").value("profile"); //$NON-NLS-1$ //$NON-NLS-2$
			stringer.key("height").value("300"); //$NON-NLS-1$ //$NON-NLS-2$
			stringer.key("width").value("400"); //$NON-NLS-1$ //$NON-NLS-2$
			stringer.key("userPrefs").object(); //$NON-NLS-1$ 
			stringer.key("showSharebox").value(false); //$NON-NLS-1$ 

			stringer.endObject();
			stringer.endObject();
			stringer.endObject();

			stringer.key("viewParams").object(); //$NON-NLS-1$ 
//			stringer.key("asConfig").value(StringEscapeUtils.unescapeJavaScript(responseObj.get("result").toString())); //$NON-NLS-1$ //$NON-NLS-2$

//			stringer.key("asFeed").value("/oauth/rest/activitystreams/@public/@all/@all?rollup=true");			
//			stringer.key("asConfig").value(StringEscapeUtils.unescapeJavaScript("{\"defaultUrlTemplate\":\"/oauth/rest/activitystreams/@public/@all/@all?rollup=true\",\"views\":{\"main\":{}}}"));			
			
			stringer.endObject();
			stringer.endObject();
			params = new JSONObject(stringer.toString());

		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}
		return params;
	}

	/**
	 * Calls the service appropriate view filter service. Can be overridden by subclasses
	 * 
	 * @return
	 */
	protected String getFilterJSON() {

		final String[] filter = new String[1];
		Job job = new Job("Get events filter") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor arg0) {

				filter[0] = SugarWebservicesOperations.getInstance().callGetEventsFilter(getSugarEntry().getSugarType().getParentType(), getSugarEntry().getId());
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		try {
			job.join();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}

		return filter[0];
	}

	private void createHeaderComposite(Composite parent) {
		Composite headerComposite = new Composite(parent, SWT.NONE);

		headerComposite.setLayout(GridLayoutFactory.fillDefaults().spacing(SWT.DEFAULT, 0).numColumns(4).margins(AbstractDashboardComposite.HEADER_MARGIN, AbstractDashboardComposite.HEADER_MARGIN)
				.create());
		headerComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		Label mainImageLabel = new Label(headerComposite, SWT.NONE);
		mainImageLabel.setLayoutData(GridDataFactory.fillDefaults().span(1, 3).create());
		setMainImageLabel(mainImageLabel);
		updateControlMap(AbstractDashboardComposite.MAINIMAGELABEL, mainImageLabel);

		Label popoutLinkLabel = new Label(headerComposite, SWT.NONE);
		popoutLinkLabel.setImage(SFAImageManager.getImage(SFAImageManager.EXTERNAL_LINK));
		popoutLinkLabel.setLayoutData(GridDataFactory.fillDefaults().indent(10, 0).hint(15, SWT.DEFAULT).create());
		popoutLinkLabel.setToolTipText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OPEN_IN_SUGAR));

		SFAHyperlink nameLink = new SFAHyperlink(headerComposite, SWT.NONE, true);
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

		new Label(headerComposite, SWT.NONE); // Spacer label

		if (getSugarType() == SugarType.ACCOUNTS) {
			createAccountHeaderFields(headerComposite);
		} else if (getSugarType() == SugarType.OPPORTUNITIES) {
			createOpportunitiesHeaderFields(headerComposite);
		}

		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
	}

	private void setMainImageLabel(Label mainImageLabel) {
		if (mainImageLabel != null && getSugarEntry() != null) {
			mainImageLabel.setImage(AbstractDashboardComposite.getImageForSugarEntry(getSugarEntry()));
			if (getSugarEntry() instanceof SugarAccount) {
				mainImageLabel.setToolTipText(((SugarAccount) getSugarEntry()).getIndustryTooltip());
			} else if (getSugarEntry() instanceof SugarContact) {
				mainImageLabel.setToolTipText(((SugarContact) getSugarEntry()).getName());
			} else if (getSugarEntry() instanceof SugarOpportunity) {
				mainImageLabel.setToolTipText(((SugarOpportunity) getSugarEntry()).getIndustryTooltip());
			}
		}
	}

	/**
	 * Creates the account specific fields for the header composite
	 * 
	 * @param headerComposite
	 */
	private void createAccountHeaderFields(Composite headerComposite) {
		final SugarAccount account = (SugarAccount) getSugarEntry();

		// spacer label
		new Label(headerComposite, SWT.NONE);

		Label industryLabel = new Label(headerComposite, SWT.NONE);
		String industry = ConstantStrings.EMPTY_STRING;
		if (account != null && account.getIndustryMap() != null && !account.getIndustryMap().isEmpty() && account.getIndustryMap().keySet().size() > 0) {
			industry = account.getIndustryString();
		}
		industryLabel.setText(industry);
		industryLabel.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());
		industryLabel.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
		industryLabel.setForeground(SugarItemsDashboard.getInstance().getBusinessCardHeaderTextColor());
		industryLabel.setToolTipText(account.getIndustryTooltip());
		updateControlMap(AbstractDashboardComposite.INDUSTRYLABEL, industryLabel);

		createWebsiteFields(headerComposite, account);
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
		updateControlMap(AbstractDashboardComposite.ACCOUNTLABEL, accountLabel);

		createWebsiteFields(headerComposite, oppty);
	}

	/**
	 * Utility method that creates a label and a link to a website
	 * 
	 * @param parent
	 */
	private void createWebsiteFields(Composite parent, final BaseSugarEntry sugarEntry) {
		// spacer label
		new Label(parent, SWT.NONE);

		Composite webAddressComposite = new Composite(parent, SWT.NONE);
		webAddressComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).create());
		webAddressComposite.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).grab(true, false).create());

		Label webAddressLabel = new Label(webAddressComposite, SWT.NONE);
		webAddressLabel.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
		webAddressLabel.setForeground(SugarItemsDashboard.getInstance().getBusinessCardHeaderTextColor());
		webAddressLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_ADDRESS_LABEL));

		SFAHyperlink accountLink = new SFAHyperlink(webAddressComposite, SWT.NONE, true);
		accountLink.setLayoutData(GridDataFactory.fillDefaults().span(1, 1).indent(0, GenericUtils.isMac() ? GenericUtils.MAC_HYPERLINK_VERTICAL_INDENT : 0).create());
		accountLink.setText((sugarEntry == null || sugarEntry.getWebsite() == null) ? ConstantStrings.EMPTY_STRING : sugarEntry.getWebsite());
		accountLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
		accountLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
		if (sugarEntry != null && sugarEntry.getWebsite() != null) {
			accountLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					GenericUtils.launchUrlInPreferredBrowser(sugarEntry.getWebsite(), false);
				}
			});
		}
		updateControlMap(AbstractDashboardComposite.ACCOUNTLINK, accountLink);
	}

	public void updateControlMap(String controlName, Object control) {
		if (controlName != null && control != null) {
			getControlMap().put(controlName, control);
		}
	}

	public Map<String, Object> getControlMap() {
		if (controlMap == null) {
			controlMap = new HashMap<String, Object>();
		}
		return controlMap;
	}

	public Object getControl(String controlName) {
		Object control = null;
		if (getControlMap().containsKey(controlName)) {
			control = getControlMap().get(controlName);
		}
		return control;
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

	public String getTitle() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.RECENT_DISCUSSION_TITLE);
	}

	// ////////////////// Methods defined by ISugarDashboardViewPart /////////////////
	@Override
	public void refreshAll() {
		refreshAction.setEnabled(false);

		List<ISugarDashboardViewPart> siblings = getSiblings();
		for (ISugarDashboardViewPart part : siblings) {
			part.refreshSelf();
		}

		refreshSelf();
		refreshAction.setEnabled(true);
	}

	@Override
	public void refreshSelf() {
		if (asWidget != null) {
			this.navigateGadget(asWidget.getUrl(), getRenderParams(), getViewParams(), new NavigateCallback() {
				@Override
				public void onNavigate(GadgetMetadataResult metadata) {
					// This function will be called when the gadget renders passing some basic metadata about the gadget
					// It is good practice to make sure there are no errors in the metadata
					if (metadata == null || (metadata != null && metadata.hasError())) {
						// There was some kind of error rendering the gadget show some error UI
					}
				}
			});
		}
	}

	@Override
	public IDashboardComposite getDashboardComposite() {
		return null;
	}

	@Override
	public Composite getProgressComposite() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ISugarDashboardViewPart> getSiblings() {
		if (siblings == null) {
			siblings = new ArrayList<ISugarDashboardViewPart>();
		}
		return siblings;
	}
	@Override
	public void setProgessDisplayer(IProgressDisplayer progessDisplayer) {
		// TODO Auto-generated method stub

	}

	// /////////////////////////////////////////////////////////////////////////////////

}
