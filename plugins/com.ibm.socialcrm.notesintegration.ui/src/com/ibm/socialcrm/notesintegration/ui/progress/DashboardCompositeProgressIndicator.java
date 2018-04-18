package com.ibm.socialcrm.notesintegration.ui.progress;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import com.ibm.socialcrm.notesintegration.core.IProgressDisplayer;
import com.ibm.socialcrm.notesintegration.core.ui.views.ISugarDashboardViewPart;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;

/**
 * An implementation of IProgressDisplayer that knows how to create/remove progess indicator for all the dashboard composites (or tabs) in a business card.
 * 
 * 
 */
public class DashboardCompositeProgressIndicator implements IProgressDisplayer {

	private List<ISugarDashboardViewPart> viewParts;

	private static final String PROGRESS_BAR_ID = "progressBarId"; //$NON-NLS-1$

	private Shell shell;

	public DashboardCompositeProgressIndicator(Shell shell) {
		this.shell = shell;
	}

	@Override
	public String createProgressIndicator(final String message) {
		final String id = "progessBar_" + System.currentTimeMillis(); //$NON-NLS-1$
		for (ISugarDashboardViewPart viewPart : getViewParts()) {
			final Composite progressComposite = viewPart.getProgressComposite();
			populateProgress(progressComposite, message, id);
		}
		return id;
	}

	public void populateProgress(final Composite progressComposite, final String message, final String id) {
		if (progressComposite != null && !progressComposite.isDisposed()) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					progressComposite.setLayoutDeferred(true);

					if (!progressComposite.isVisible()) {
						progressComposite.setVisible(true);
						((GridData) (progressComposite.getLayoutData())).exclude = false;
					}

					Composite composite = new Composite(progressComposite, SWT.NONE);
					composite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).margins(5, 5).create());
					composite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
					composite.setData(PROGRESS_BAR_ID, id);

					Label label = new Label(composite, SWT.WRAP);
					label.setText(message);
					label.setLayoutData(GridDataFactory.fillDefaults().grab(false, false).create());

					ProgressBar progressBar = new ProgressBar(composite, SWT.INDETERMINATE);
					progressBar.setLayoutData(GridDataFactory.fillDefaults().indent(10, 0).grab(true, false).create());
					UiUtils.recursiveSetBackgroundColor(composite, JFaceColors.getBannerBackground(Display.getDefault()));

					progressComposite.setLayoutDeferred(false);

					progressComposite.layout(true);

					progressComposite.getParent().layout(true);

				}
			});
		}
	}

	@Override
	public Shell getShell() {
		return shell;
	}

	@Override
	public void removeProgressIndicator(final String id) {
		for (ISugarDashboardViewPart viewPart : getViewParts()) {
			final Composite progressComposite = viewPart.getProgressComposite();
			removeProgress(progressComposite, id);
		}

	}

	public void removeProgress(final Composite progressComposite, final String id) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				for (Control control : progressComposite.getChildren()) {
					Object storedId = control.getData(PROGRESS_BAR_ID);
					if (storedId != null && storedId.equals(id)) {
						control.dispose();
					}
				}

				if (progressComposite.getChildren().length == 0) {
					progressComposite.setVisible(false);
					((GridData) (progressComposite.getLayoutData())).exclude = true;
				}

				progressComposite.getParent().layout(true);
			}
		});
	}

	public List<ISugarDashboardViewPart> getViewParts() {
		if (viewParts == null) {
			viewParts = new ArrayList<ISugarDashboardViewPart>();
		}
		return viewParts;
	}

}
