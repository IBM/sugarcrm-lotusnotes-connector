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

import java.lang.reflect.Constructor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.progress.UIJob;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarAccount;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.DashboardContributionExtensionProcessor;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.ISametimeWidgetBuilder;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.SametimeWidgetContribution;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.SametimeWidgetContributionExtensionProcessor;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.core.utils.UpdateSelectionsBroadcaster;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations.GetInfo13RestulType;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.progress.DashboardCompositeProgressIndicator;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.widgets.EasyScrolledComposite;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFAHyperlink;

/**
 * Specialized dashboard composite that encapsulates some common methods required for the informational tabs for each of the three basic sugar types (Contact, oppty, account).
 */
public abstract class AbstractInfoDashboardComposite extends AbstractDashboardComposite {
	public static final String ADDRESSCOMPOSITE = "ADDRESSCOMPOSITE"; //$NON-NLS-1$
	public static final String EMAILCOMPOSITE = "EMAICOMPOSITE"; //$NON-NLS-1$
	public static final String PHONECOMPOSITE = "PHONECOMPOSITE"; //$NON-NLS-1$

	public int maxLabelWidth = -1;

	public Composite innerComposite;
	private EasyScrolledComposite scrolledComposite;

	private DashboardCompositeProgressIndicator progressIndicator;
	private String progressId;

	public AbstractInfoDashboardComposite(Composite parent, int style, String dashboardID, BaseSugarEntry sugarEntry) {
		super(parent, style, dashboardID, sugarEntry);

		// 41443 - set Shell title to SugarEntry's name
		addPartListener();

	}

	private void addPartListener() {
		final IPartListener partListener = new IPartListener() {

			@Override
			public void partActivated(IWorkbenchPart arg0) {
				if (!Workbench.getInstance().getActiveWorkbenchWindow().getShell().getText().equalsIgnoreCase(getSugarEntry().getName())) {
					if (getSugarEntry() instanceof SugarAccount) {
						Workbench.getInstance().getActiveWorkbenchWindow().getShell().setText(getSugarEntry().getName() + " (" + ((SugarAccount) getSugarEntry()).getClientId() + ")"); //$NON-NLS-1$  //$NON-NLS-2$
					} else {
						Workbench.getInstance().getActiveWorkbenchWindow().getShell().setText(getSugarEntry().getName());
					}
				}
			}

			@Override
			public void partBroughtToTop(IWorkbenchPart arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void partClosed(IWorkbenchPart arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void partDeactivated(IWorkbenchPart arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void partOpened(IWorkbenchPart arg0) {
				// TODO Auto-generated method stub

			}

		};
		Workbench.getInstance().getActiveWorkbenchWindow().getActivePage().addPartListener(partListener);

		this.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				if (partListener != null) {
					if (Workbench.getInstance() != null && Workbench.getInstance().getActiveWorkbenchWindow() != null && Workbench.getInstance().getActiveWorkbenchWindow().getActivePage() != null) {
						Workbench.getInstance().getActiveWorkbenchWindow().getActivePage().removePartListener(partListener);
					}
				}

			}

		});
	}
	@Override
	public void prepareTask() {
		progressIndicator = new DashboardCompositeProgressIndicator(getShell());
		progressId = "progessBar_BasicTab_" + System.currentTimeMillis(); //$NON-NLS-1$
		progressIndicator.populateProgress(getProgressComposite(), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CARD_LOADING), progressId);

		// retrieve base tab information, then broadcast it to notify all the other tabs
		retrieveCardData();
	}

	@Override
	public void createInnerComposite() {
		scrolledComposite = new EasyScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).create());
		scrolledComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		innerComposite = new Composite(scrolledComposite, SWT.NONE);
		innerComposite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));
		updateInnerComposite(innerComposite);

		UiUtils.recursiveSetBackgroundColor(innerComposite, JFaceColors.getBannerBackground(Display.getDefault()));
		scrolledComposite.setContent(innerComposite);

	}

	@Override
	public void selectedItemsChanged() {
	}

	public void retrieveCardData() {
		Job retrieveCardDataJob = new Job("Retrieving card data") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				// *** Retreive card base tab data
				boolean isOK = SugarWebservicesOperations.getInstance().callSugarGetInfo13(getSugarEntry().getSugarType(), getSugarEntry().getId(), GetInfo13RestulType.BASECARD);
				
				// 102312 - remove following/unfollowing feature
//				if (isOK) {
//					// get the oppty/account Follow information
//					if (getSugarEntry().getSugarType().equals(SugarType.ACCOUNTS) || getSugarEntry().getSugarType().equals(SugarType.OPPORTUNITIES)) {
//						isOK = SugarWebservicesOperations.getInstance().callSugarGetInfo13(getSugarEntry().getSugarType(), getSugarEntry().getId(), GetInfo13RestulType.FOLLOWED);
//					}
//				}

				// *** update Sugar Entry Object
				if (isOK) {
					setBaseDataRetrieved(isOK);
					setSugarEntry(SugarWebservicesOperations.getInstance().getSugarEntryById(getSugarEntry().getId()));
				} else {
					// need to tell user the bad news
					if (AbstractInfoDashboardComposite.this.isDisposed()) {
					} else {
						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								if (SugarWebservicesOperations.getInstance().isInvalidSugarEntry()) {
									SugarWebservicesOperations.getInstance().resetInvalidSugarEntry(false);
									// Display an error msg and prompt user for credential... this should cover the case if pswd was changed
									// between the time period a session was created and a card was brought up.
									MessageDialog.openError(Display.getDefault().getShells()[0], UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CARD_RETRIEVE_ERROR), UtilsPlugin
											.getDefault().getResourceString(UtilsPluginNLSKeys.CARD_RETRIEVE_FAILURE_INVALID_SUGARENTRY));
									AbstractInfoDashboardComposite.this.getShell().close();

								} else if (SugarWebservicesOperations.getInstance().hasConnectionProblem()) {

									// Display an error msg and prompt user for credential... this should cover the case if pswd was changed
									// between the time period a session was created and a card was brought up.
									MessageDialog.openError(Display.getDefault().getShells()[0], UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CARD_RETRIEVE_ERROR), UtilsPlugin
											.getDefault().getResourceString(UtilsPluginNLSKeys.UI_CRM_SERVER_CONNECTION_ERROR));
									SugarWebservicesOperations.getPropertyChangeSupport().firePropertyChange(SugarWebservicesOperations.BRING_UP_CREDENTIAL_PROMPT, true, false);
									AbstractInfoDashboardComposite.this.getShell().close();
								} else {
									// A generic error msg
									MessageDialog.openError(Display.getDefault().getShells()[0], UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CARD_RETRIEVE_ERROR), UtilsPlugin
											.getDefault().getResourceString(UtilsPluginNLSKeys.CARD_RETRIEVE_FAILURE, new String[]{getSugarEntry().getName()}));
									AbstractInfoDashboardComposite.this.getShell().close();
								}
							}
						});
					}

				}
				final boolean isOKNow = isOK;

				if (isOKNow) {
					// *** Update base tab UI
					UIJob updateBaseCardUIJob = new UIJob("updateBaseCardUIJob") //$NON-NLS-1$
					{
						@Override
						public IStatus runInUIThread(IProgressMonitor arg0) {
							afterBaseCardDataRetrievedForBaseTab();
							return Status.OK_STATUS;
						}

					};
					updateBaseCardUIJob.schedule();

					// *** Notify other tabs to either proceed or abort. Those tabs should implement afterBaseCardDataRetrieved().
					// Separate broadcast in a separate UIJob so the basecase can be updated and displayed before the other tabs
					// are notified.
					UIJob broadcastBaseCardIsReadyUIJob = new UIJob("broadcastBaseCardIsReady") //$NON-NLS-1$
					{
						@Override
						public IStatus runInUIThread(IProgressMonitor arg0) {
							// UpdateSelectionsBroadcaster.getInstance().basecardDataRetrieved(isOKNow);
							UpdateSelectionsBroadcaster.getInstance().basecardDataRetrieved(getSugarEntry());
							return Status.OK_STATUS;
						}
					};
					broadcastBaseCardIsReadyUIJob.schedule();
				}

				return Status.OK_STATUS;
			}
		};
		retrieveCardDataJob.schedule();
	}
	/*
	 * Method called after basecard data was retrieved, it's called only by the base tab. It should be executed before the broadcast so basecase UI can be updated and displayed before other tabs is
	 * notified.
	 */
	public void afterBaseCardDataRetrievedForBaseTab() {
		// Update card header information
		super.afterBaseCardDataRetrieved();
		updateInnerCompositeWithData();

		if (progressIndicator != null) {
			progressIndicator.removeProgress(getProgressComposite(), progressId);
		}

		getParent().layout(true, true);
	}

	@Override
	public void afterBaseCardDataRetrieved() {
		// Empty this method, because it's been called in the afterBaseCardDataRetrievedForBaseTab() method.
	}

	// Inner composites for business card.
	protected void createAddressComposite(Composite parent, BaseSugarEntry sugarEntry, int maxLabelWidth) {

		Label addressLabel = new Label(parent, SWT.NONE);
		addressLabel.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
		addressLabel.setForeground(SugarItemsDashboard.getInstance().getBusinessCardFieldLabelColor());
		addressLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ADDRESS_LABEL));

		if (isAtLeastOneAddressFieldAvailable(sugarEntry)) {

			int verticalSpan = 0;
			if (sugarEntry.getStreet() != null && !sugarEntry.getStreet().equals(ConstantStrings.EMPTY_STRING)) {
				Label label = new Label(parent, SWT.WRAP);
				label.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
				label.setText(sugarEntry.getStreet());
				label.setToolTipText(sugarEntry.getStreet());
				label.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
				verticalSpan++;
			}

			String cityStatePostal = getCityStatePostal(sugarEntry).trim();
			if (!cityStatePostal.equals(ConstantStrings.EMPTY_STRING)) {
				Label label = new Label(parent, SWT.WRAP);
				label.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
				label.setText(cityStatePostal);
				label.setToolTipText(cityStatePostal);
				label.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
				verticalSpan++;
			}

			if (sugarEntry.getCountry() != null && !sugarEntry.getCountry().equals(ConstantStrings.EMPTY_STRING)) {
				Label label = new Label(parent, SWT.WRAP);
				label.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
				label.setText(sugarEntry.getCountry());
				label.setToolTipText(sugarEntry.getCountry());
				label.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
				verticalSpan++;
			}

			addressLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).hint(maxLabelWidth == -1 ? SWT.DEFAULT : maxLabelWidth, SWT.DEFAULT).span(1, verticalSpan)
					.create());

		} else if (!isBaseDataRetrieved()) {

			int verticalSpan = 1;

			addressLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).hint(maxLabelWidth == -1 ? SWT.DEFAULT : maxLabelWidth, SWT.DEFAULT).span(2, verticalSpan)
					.create());

		}

	}

	protected void createEmailComposite(Composite parent, final String email, String labelText, int labelWidth, boolean suppressed) {
		Label label = new Label(parent, SWT.NONE);
		label.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
		label.setForeground(SugarItemsDashboard.getInstance().getBusinessCardFieldLabelColor());
		label.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).hint((maxLabelWidth == -1 || maxLabelWidth == 0) ? SWT.DEFAULT : maxLabelWidth, SWT.DEFAULT).create());
		label.setText(labelText);

		SFAHyperlink hyperLink = new SFAHyperlink(parent, SWT.NONE, true);
		hyperLink.setLayoutData(GridDataFactory.fillDefaults().indent(0, GenericUtils.getPlatformHyperlinkVerticalIndent()).create());
		hyperLink.setStrikethrough(suppressed);
		hyperLink.setText(email);
		hyperLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
		hyperLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
		if (!suppressed && email != null && !email.equals(ConstantStrings.EMPTY_STRING)) {
			hyperLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent arg0) {
					UiUtils.createEmail(email);
				}
			});
		}
	}

	protected void createClickToCallComposite(Composite parent, final String phone, String labelText, int labelWidth, boolean suppressed) {

		boolean builtViaContribution = false;
		if (phone != null && !phone.equals(ConstantStrings.EMPTY_STRING) && !suppressed) {
			SametimeWidgetContribution sametimeWidgetContribution = SametimeWidgetContributionExtensionProcessor.getInstance().getSametimeWidgetContribution();
			if (sametimeWidgetContribution != null) {
				try {
					Class builderClass = sametimeWidgetContribution.getBundle().loadClass(sametimeWidgetContribution.getBuilderClass());
					Constructor constructor = builderClass.getConstructor();
					ISametimeWidgetBuilder sametimeWidgetBuilder = (ISametimeWidgetBuilder) constructor.newInstance();
					builtViaContribution = sametimeWidgetBuilder.createClickToCallComposite(parent, phone, labelText, labelWidth);
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
				}
			}
		}
		if (!builtViaContribution || suppressed) {
			Label phoneLabel = new Label(parent, SWT.NONE);
			phoneLabel.setText(labelText);
			phoneLabel.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
			phoneLabel.setForeground(SugarItemsDashboard.getInstance().getBusinessCardFieldLabelColor());
			phoneLabel.setLayoutData(GridDataFactory.fillDefaults().hint(labelWidth, SWT.DEFAULT).create());

			if (suppressed) {
				SFAHyperlink hyperLink = new SFAHyperlink(parent, SWT.NONE);
				hyperLink.setLayoutData(GridDataFactory.fillDefaults().indent(0, GenericUtils.getPlatformHyperlinkVerticalIndent()).create());
				hyperLink.setStrikethrough(true);
				hyperLink.setText(phone == null ? ConstantStrings.EMPTY_STRING : phone);
				hyperLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
				hyperLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
			} else {
				Label phoneNumberLabel = new Label(parent, SWT.NONE);
				phoneNumberLabel.setText(phone == null ? ConstantStrings.EMPTY_STRING : phone);
				phoneNumberLabel.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
			}
		}
	}

	private boolean isAtLeastOneAddressFieldAvailable(BaseSugarEntry sugarEntry) {
		return (sugarEntry != null && (sugarEntry.getStreet() != null && !sugarEntry.getStreet().equals(ConstantStrings.EMPTY_STRING))
				|| (sugarEntry.getCity() != null && !sugarEntry.getCity().equals(ConstantStrings.EMPTY_STRING))
				|| (sugarEntry.getState() != null && !sugarEntry.getState().equals(ConstantStrings.EMPTY_STRING))
				|| (sugarEntry.getPostalCode() != null && !sugarEntry.getPostalCode().equals(ConstantStrings.EMPTY_STRING)) || (sugarEntry.getCountry() != null && !sugarEntry.getCountry().equals(
				ConstantStrings.EMPTY_STRING)));
	}

	private String getCityStatePostal(BaseSugarEntry sugarEntry) {
		String cityStatePostal = ConstantStrings.EMPTY_STRING;
		boolean cityAvailable = sugarEntry.getCity() != null && !sugarEntry.getCity().equals(ConstantStrings.EMPTY_STRING);
		boolean stateAvailable = sugarEntry.getState() != null && !sugarEntry.getState().equals(ConstantStrings.EMPTY_STRING);
		boolean postalAvailable = sugarEntry.getPostalCode() != null && !sugarEntry.getPostalCode().equals(ConstantStrings.EMPTY_STRING);

		cityStatePostal = cityAvailable ? sugarEntry.getCity() : ConstantStrings.EMPTY_STRING;
		cityStatePostal += stateAvailable && cityAvailable ? ", " + sugarEntry.getState() : stateAvailable ? sugarEntry //$NON-NLS-1$
				.getState() : ConstantStrings.EMPTY_STRING;
		cityStatePostal += postalAvailable && stateAvailable ? " " + sugarEntry.getPostalCode() : postalAvailable && cityAvailable ? ", " + sugarEntry.getPostalCode() //$NON-NLS-1$ //$NON-NLS-2$
		: postalAvailable ? sugarEntry.getPostalCode() : ConstantStrings.EMPTY_STRING;

		return cityStatePostal;
	}

	protected int getMaxLabelWidth() {
		if (maxLabelWidth == -1) {
			Point point = computeMaxSize(this, getFieldLabels());
			if (point != null) {
				maxLabelWidth = point.x;
				maxLabelWidth += 5; // Add a buffer to improve spacing
			}
		}
		return maxLabelWidth;
	}

	/**
	 * Returns the set of labels used for the various data fields
	 * 
	 * @return
	 */
	public abstract String[] getFieldLabels();

	/**
	 * Since this method is only used to compute label sizes, the getBusinessCardLabelFont() font is hardcoded. We can always change this as necessary.
	 * 
	 * @param parent
	 * @param arrays
	 * @return
	 */
	public Point computeMaxSize(Composite parent, String[] arrays) {
		int width = -1;
		int height = -1;
		if (parent == null || arrays == null || arrays.length == 0) {
			return null;
		}

		GC gc = new GC(parent);
		gc.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
		for (int i = 0; i < arrays.length; i++) {

			Point size = gc.textExtent(arrays[i]); // or textExtent
			width = Math.max(width, size.x);
			height = Math.max(height, size.y);
		}

		gc.dispose();
		return new Point(width, height);
	}

	public abstract void updateInnerComposite(Composite innerComposite);

}
