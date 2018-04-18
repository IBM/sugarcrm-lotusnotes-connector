package com.ibm.socialcrm.notesintegration.core.ui.views;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.part.ViewPart;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.IProgressDisplayer;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.DashboardContribution;
import com.ibm.socialcrm.notesintegration.core.extensionpoints.DashboardContributionExtensionProcessor;
import com.ibm.socialcrm.notesintegration.core.uipluginbridge.IDashboardComposite;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFAHyperlink;

/**
 * This is a generic view part that is used to house an AbsractDashboardComposite.
 * 
 * When the plugins were first written, AbstractDashboardComposites were added directly to tabs. Later, we had to support view parts directly in the cards to support the activity stream. So, we
 * rebuilt the card using Perspectives and the SwitcherService. This view part acts as a bridge between the old rendering model and the new one. The existing AbstractDashboardComposites are
 * constructed in the context of this view part.
 */
public class SugarDashboardViewPart extends ViewPart implements ISugarDashboardViewPart {

	private BaseSugarEntry sugarEntry;
	private DashboardContribution dashboardContribution;
	private Composite parent;
	private IProgressDisplayer progessDisplayer;
	private IDashboardComposite dashboardComposite;

	public static final String SUGAR_ID = "sugarId"; //$NON-NLS-1$
	public static final String DASHBOARD_CONTRIBUTION_ID = "dashboardContribId"; //$NON-NLS-1$

	private boolean toSkipCreatePartControl = true;
	/**
	 * All of these dashboard view parts will exist on a card (or perspective) together. When the user clicks the refresh action, we need to refresh all of the cards. Each view part will have a list
	 * of it's siblings so it can refresh everything on the card.
	 */
	private List<ISugarDashboardViewPart> siblings;

	private IAction refreshAction;

	@Override
	public void init(IViewSite viewSite, IMemento input) throws PartInitException {
		super.init(viewSite, input);

		if (input != null) {
			String sugarId = input.getString(SUGAR_ID);
			sugarEntry = SugarWebservicesOperations.getInstance().getSugarEntryById(sugarId);

			String dashboardContribId = input.getString(DASHBOARD_CONTRIBUTION_ID);
			dashboardContribution = DashboardContributionExtensionProcessor.getInstance().getDashboardContributionById(dashboardContribId);
			createPartControl(parent);
		} else {
			toSkipCreatePartControl = true;
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		if (toSkipCreatePartControl) {
			this.parent = parent;
			toSkipCreatePartControl = false;
		} else {
			try {
				Class viewClass = dashboardContribution.getBundle().loadClass(dashboardContribution.getViewClass());
				Constructor constructor = viewClass.getConstructor(Composite.class, int.class, String.class, BaseSugarEntry.class);

				dashboardComposite = (IDashboardComposite) constructor.newInstance(parent, SWT.NONE, dashboardContribution.getId(), sugarEntry);
				dashboardComposite.setDashboardContribution(dashboardContribution);
				dashboardComposite.setLayoutData(GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 200));
				dashboardComposite.selectedItemsChanged();
				dashboardComposite.setParentViewPart(this);

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
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
	}

	public void refreshAll() {
		if (refreshAction != null) {
			refreshAction.setEnabled(false);

			// Commenting out progress bar for refresh - card loading process is changed, it involves multiple batch jobs and UI jobs, to make the
			// progress bar really reflect the current card loading status, we probably should have each tab fires a property change event when
			// its loading is complete, and remove the progress bar when it receives the change events from all the tabs. Mark this task as TODO.
			// final String progressId = getProgessDisplayer().createProgressIndicator(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_REFRESHING));

			Job job = new Job("Refreshing card") { //$NON-NLS-1$
				@Override
				protected IStatus run(IProgressMonitor arg0) {

					SugarType type = sugarEntry.getSugarType();

					List<String> entryList = new ArrayList<String>();
					entryList.add(sugarEntry.getId());

					// if (type == SugarType.ACCOUNTS) {
					// SugarWebservicesOperations.getInstance().loadSugarInfoFromWebservice(entryList, null, null);
					// } else if (type == SugarType.OPPORTUNITIES) {
					// SugarWebservicesOperations.getInstance().loadSugarInfoFromWebservice(null, entryList, null);
					// } else if (type == SugarType.CONTACTS) {
					// SugarWebservicesOperations.getInstance().loadSugarInfoFromWebservice(null, null, entryList);
					// }

					final boolean userInitiated = true; // TODO: Pass this in for timed refresh
					Display.getDefault().asyncExec(new Runnable() {

						@Override
						public void run() {
							if (userInitiated) {
								if (SugarWebservicesOperations.getInstance().hasConnectionProblem()) {
									MessageDialog errorDialog = new MessageDialog(Display.getDefault().getActiveShell(), UtilsPlugin.getDefault()
											.getResourceString(UtilsPluginNLSKeys.CONNECTION_ERROR), null, UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UNABLE_TO_CONNECT),
											MessageDialog.NONE, new String[]{UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OK)}, 0) {
										@Override
										protected Control createCustomArea(Composite composite) {
											SFAHyperlink settingsLink = new SFAHyperlink(composite, SWT.NONE);
											settingsLink.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).grab(false, false).create());
											settingsLink.setForeground(JFaceColors.getHyperlinkText(Display.getCurrent()));
											settingsLink.setUnderlined(true);
											settingsLink.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OPEN_PREFERENCES));
											settingsLink.addHyperlinkListener(new HyperlinkAdapter() {
												@Override
												public void linkActivated(HyperlinkEvent e) {
													PreferenceDialog prefDialog = PreferencesUtil.createPreferenceDialogOn(Display.getDefault().getShells()[0],
															"com.ibm.socialcrm.notesintegration.ui.preferencePage", //$NON-NLS-1$
															null, null);
													close();
													prefDialog.open();
												}
											});
											return composite;
										}
									};
									errorDialog.open();
								} else {
									List<IDashboardComposite> dashboardComposites = getAllDashboardComposites();
									List<IDashboardComposite> hasPendingChangeList = new ArrayList<IDashboardComposite>();
									for (IDashboardComposite dashboard : dashboardComposites) {
										if (dashboard != null && dashboard.hasUncommittedChanges()) {
											hasPendingChangeList.add(dashboard);
										}
									}
									boolean doRefresh = true;

									if (!hasPendingChangeList.isEmpty()) {
										String tabs = ConstantStrings.EMPTY_STRING;
										for (IDashboardComposite dashboard : hasPendingChangeList) {
											tabs += dashboard.getDashboardName() + "\n"; //$NON-NLS-1$
										}

										MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(),
												UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_REFRESH_TITLE), null, UtilsPlugin.getDefault().getResourceString(
														UtilsPluginNLSKeys.UI_REFRESH_WARNING, new String[]{tabs}), MessageDialog.QUESTION, new String[]{
														UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_REFRESH),
														UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CANCEL)}, 1);
										doRefresh = (dialog.open() == MessageDialog.OK);
									}

									if (doRefresh) {
										refreshSelf();
										for (ISugarDashboardViewPart part : getSiblings()) {
											part.refreshSelf();
										}
									}
								}
							} else if (!userInitiated) // This is an automated refresh. We'll refresh whatever is appropriate
							{
								refreshSelf();
								for (ISugarDashboardViewPart part : getSiblings()) {
									part.refreshSelf();
								}
							}
							// getProgessDisplayer().removeProgressIndicator(progressId);
							refreshAction.setEnabled(true);
						}
					});
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}
	}

	@Override
	public void setFocus() {
	}

	public IProgressDisplayer getProgessDisplayer() {
		return progessDisplayer;
	}

	public void setProgessDisplayer(IProgressDisplayer progessDisplayer) {
		this.progessDisplayer = progessDisplayer;
		if (dashboardComposite != null) {
			dashboardComposite.setProgressDisplayer(getProgessDisplayer());
		}
	}

	public IDashboardComposite getDashboardComposite() {
		return dashboardComposite;
	}

	public List<ISugarDashboardViewPart> getSiblings() {
		if (siblings == null) {
			siblings = new ArrayList<ISugarDashboardViewPart>();
		}
		return siblings;
	}

	public void setSiblings(List<ISugarDashboardViewPart> siblings) {
		this.siblings = siblings;
	}

	/**
	 * Returns a list that contains the IDashboardComposite for this view and all sibling views
	 * 
	 * @return
	 */
	private List<IDashboardComposite> getAllDashboardComposites() {
		List<IDashboardComposite> dashboardList = new ArrayList();
		dashboardList.add(getDashboardComposite());
		for (ISugarDashboardViewPart part : getSiblings()) {
			IDashboardComposite composite = part.getDashboardComposite();
			if (composite != null) {
				dashboardList.add(composite);
			}
		}
		return dashboardList;
	}

	@Override
	public Composite getProgressComposite() {
		return getDashboardComposite().getProgressComposite();
	}

	@Override
	public void refreshSelf() {
		IDashboardComposite dashboardComposite = getDashboardComposite();
		if (dashboardComposite != null) {
			dashboardComposite.rebuildComposite();
			dashboardComposite.selectedItemsChanged();
			dashboardComposite.layout(true);
		}
	}

}
