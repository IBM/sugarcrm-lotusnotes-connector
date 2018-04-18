package com.ibm.socialcrm.notesintegration.sidebar.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

import com.ibm.socialcrm.notesintegration.files.sidebar.FileUploadComposite;
import com.ibm.socialcrm.notesintegration.sidebar.composites.RecentlyViewCardsComposite;
import com.ibm.socialcrm.notesintegration.ui.dashboardcomposites.CallLogComposite;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataHub;
import com.ibm.socialcrm.notesintegration.utils.datahub.SFADataShare;
import com.ibm.socialcrm.notesintegration.utils.datahub.calllog.CurrentSugarEntryDataShare;

public class ConsolidatedSidebarView extends ViewPart {

	public static final String VIEW_ID = "com.ibm.socialcrm.notesintegration.sidebar.consolidatedView"; //$NON-NLS-1$

	private CTabItem filesItem;
	private CTabItem callLogItem;
	private CTabItem recentlyViewedCardsItem;
	private CallLogComposite callLogComposite;

	public ConsolidatedSidebarView() {

	}

	@Override
	public void createPartControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(GridLayoutFactory.fillDefaults().margins(5, 5).create());
		composite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		final CTabFolder folder = new CTabFolder(composite, SWT.TOP | SWT.BORDER);
		folder.setSimple(false);
		folder.setBorderVisible(true);
		folder.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		createRecentlyViewedTab(folder);
		createFilesTab(folder);
		createCallLogTab(folder);

		folder.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent evt) {
				if (evt.item instanceof CTabItem) {
					CTabItem selectedTab = (CTabItem) evt.item;
					// Because of IE suckage, the call log form doesn't get full rendered when the view is
					// activated. Therefore, we have to call Brian's code to kick it to tell it to rebuild itself.
					if (selectedTab == callLogItem) {
						callLogComposite.panelRaised();
					}
				}
			}
		});

		final IPartService service = (IPartService) getSite().getService(IPartService.class);

		// Because of IE suckage, the call log form doesn't get full rendered when the view is
		// activated. Therefore, we have to call Brian's code to kick it to tell it to rebuild itself.
		final IPartListener2 listener = new IPartListener2() {
			@Override
			public void partActivated(IWorkbenchPartReference partRef) {
				if (callLogComposite != null && !callLogComposite.isDisposed() && folder.getSelection() == callLogItem) {
					callLogComposite.panelRaised();
				}
			}

			@Override
			public void partBroughtToTop(IWorkbenchPartReference arg0) {
			}

			@Override
			public void partClosed(IWorkbenchPartReference arg0) {
			}

			@Override
			public void partDeactivated(IWorkbenchPartReference arg0) {
			}

			@Override
			public void partHidden(IWorkbenchPartReference arg0) {
			}

			@Override
			public void partInputChanged(IWorkbenchPartReference arg0) {
			}

			@Override
			public void partOpened(IWorkbenchPartReference arg0) {
			}

			@Override
			public void partVisible(IWorkbenchPartReference arg0) {
			}
		};
		service.addPartListener(listener);

		// When the call log's current item changes, bring the call log to front
		final PropertyChangeListener pcl = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				folder.setSelection(callLogItem);
			}
		};

		Job job = new Job("Add listener for current sugar entry") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				SFADataShare share = SFADataHub.getInstance().blockingGetDataShare(CurrentSugarEntryDataShare.SHARE_NAME, 20000);
				if (share != null) {
					share.addPropertyChangeListener(CurrentSugarEntryDataShare.CURRENT_SUGAR_ENTRY, pcl);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();

		composite.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				service.removePartListener(listener);
				getCurrentSugarEntryDataShare().removePropertyChangeListener(CurrentSugarEntryDataShare.CURRENT_SUGAR_ENTRY, pcl);
			}
		});

		// Also due to IE suckage. Vertical resizing of the sidebar screws up IE's rendering.
		parent.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent arg0) {
				if (callLogComposite != null && !callLogComposite.isDisposed() && folder.getSelection() == callLogItem) {
					callLogComposite.panelRaised();
				}
			}
		});
	}

	private void createRecentlyViewedTab(CTabFolder folder) {
		recentlyViewedCardsItem = new CTabItem(folder, SWT.NONE);
		recentlyViewedCardsItem.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_SIDEBAR_DETAILS_TAB));
		RecentlyViewCardsComposite recentlyViewedComposite = new RecentlyViewCardsComposite(folder);
		recentlyViewedCardsItem.setControl(recentlyViewedComposite);

		folder.setSelection(recentlyViewedCardsItem);
	}

	private void createFilesTab(CTabFolder folder) {
		filesItem = new CTabItem(folder, SWT.NONE);
		filesItem.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_SIDEBAR_FILES_TAB));
		FileUploadComposite uploadComposite = new FileUploadComposite(folder);
		filesItem.setControl(uploadComposite);
	}

	private void createCallLogTab(CTabFolder folder) {
		callLogItem = new CTabItem(folder, SWT.NONE);
		callLogItem.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_LOG_CALL));

		callLogComposite = new CallLogComposite(folder, SWT.NONE);
		callLogItem.setControl(callLogComposite);
	}

	@Override
	public void setFocus() {

	}

	/**
	 * Returns the data share that contains the current BaseSugarEntry (from a call logging perspective)
	 */
	private SFADataShare getCurrentSugarEntryDataShare() {
		return SFADataHub.getInstance().getDataShare(CurrentSugarEntryDataShare.SHARE_NAME);
	}

}
