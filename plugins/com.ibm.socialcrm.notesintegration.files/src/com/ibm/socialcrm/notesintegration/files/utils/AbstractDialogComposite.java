package com.ibm.socialcrm.notesintegration.files.utils;

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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.progress.UIJob;

import com.ibm.socialcrm.notesintegration.files.FilesPluginActivator;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public abstract class AbstractDialogComposite {
	private static final String WIDTH_PREFERENCE = "widthPreference"; //$NON-NLS-1$
	private static final String HEIGHT_PREFERENCE = "heightPreference"; //$NON-NLS-1$
	private static final String PROGRESS_BAR_ID = "progressBarId"; //$NON-NLS-1$
	public static final int DEFAULT_WIDTH = 280;
	public static final int DEFAULT_HEIGHT = 313;
	private static final int WIDTH_MARGIN = 15;
	private static final int HEIGHT_MARGIN = 10;
	private static final int TOP_MARGIN = 1;
	private static final int BOTTOM_MARGIN = 8;

	private Display _display = null;
	private Shell _shell = null;
	// Information you want to pass to this class... use getData() to retrieve it
	protected Object[] _data = null;
	private Composite _parentComposite = null;
	private Composite _container = null;
	private Composite _progressComposite = null;
	private boolean _isProgressBarNeeded = false;

	protected AbstractDialogComposite(Display display, boolean isProgressBarNeeded, Object[] data) {
		_display = display;
		_isProgressBarNeeded = isProgressBarNeeded;
		_data = data;

		// UI
		_shell = createShell(_display);

		_parentComposite = createParentComposite(_shell);
		mainCreateDialogArea(_parentComposite);
		mainCreateButtonsForButtonBar(_parentComposite);

		configureShell();
		addShellListeners();

		_shell.layout(true);
		_shell.open();
		_shell.forceActive();

	}

	private Shell createShell(Display display) {
		Shell shell = new Shell(display, SWT.RESIZE | SWT.TITLE | SWT.MIN | SWT.MAX | SWT.CLOSE | SWT.MODELESS);
		shell.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).create());
		shell.setLayoutData(GridDataFactory.fillDefaults().create());

		return shell;
	}

	private Composite createParentComposite(Shell shell) {
		Composite parentComposite = new Composite(shell, SWT.NONE);
		parentComposite.setLayout(GridLayoutFactory.fillDefaults().create());
		parentComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		return parentComposite;
	}

	protected Control mainCreateDialogArea(Composite parent) {

		_container = new Composite(parent, SWT.NONE);

		_container.setLayout(GridLayoutFactory.fillDefaults().create());
		_container.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		GridData data = (GridData) _container.getLayoutData();
		GC gc = new GC(_container.getDisplay());
		data.widthHint = gc.getFontMetrics().getAverageCharWidth() * 100;
		gc.dispose();
		data.horizontalIndent = 0;
		data.verticalIndent = 0;
		data.grabExcessHorizontalSpace = true;
		_container.setLayoutData(data);

		_container.setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).spacing(SWT.DEFAULT, SWT.DEFAULT).numColumns(1).create());
		_container.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

		// ... YOU NEED TO IMPLEMENT THIS ...
		createDialogArea(_container);

		// create the progress bar at the bottom of the shell
		if (_isProgressBarNeeded) {
			createProgressComposite();
		}

		return parent;
	}

	protected void mainCreateButtonsForButtonBar(Composite parent) {
		Composite buttonComposite = new Composite(parent, SWT.NONE);
		Rectangle margins = Geometry.createDiffRectangle(getLeftWidthMargin(), getRightWidthMargin(), getTopMargin(), getBottomMargin());
		buttonComposite.setLayout(GridLayoutFactory.fillDefaults().extendedMargins(margins).numColumns(1).equalWidth(false).create());
		buttonComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		// ... YOU NEED TO IMPLEMENT THIS ...
		createButtonsForButtonBar(buttonComposite);

		parent.layout();
	}

	// Manage the dialog shell
	protected void configureShell() {

		getShell().setText(getTitleText());
		getShell().setFont(getTitleFont());

		if (getTitleColor() != null) {
			getShell().setBackground(getTitleColor());
		}
		getShell().setImage(getTitleImage());

		setDialogSize();
	}

	protected void setDialogSize() {
		Preferences prefs = FilesPluginActivator.getDefault().getPluginPreferences();
		int xPref = prefs.getInt(WIDTH_PREFERENCE);
		int yPref = prefs.getInt(HEIGHT_PREFERENCE);
		if (xPref == 0) {
			xPref = getDialogDefaultWidth();
		}
		if (yPref == 0) {
			yPref = getDialogDefaultHeight();
		}
		getShell().setSize(xPref, yPref);
	}

	public Display getDisplay() {
		return _display;
	}

	public Shell getShell() {
		return _shell;
	}

	public void addShellListeners() {

		// Close the shell when the user presses ESC
		getShell().addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					close();
				}
			}
		});

		// Listener to listen for shell resize
		getShell().addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				// Don't save the preferences when the user maximizes the size
				if (!getShell().getMaximized()) {
					// Set the explicit size of the parent composite so that if
					// we add a progress composite, the shell
					// expands to accommodate the new widget rather than shrink
					// the parentComposite within the shell.
					Point point = getShell().getSize();
					((GridData) getShell().getLayoutData()).widthHint = point.x;
					((GridData) getShell().getLayoutData()).heightHint = point.y;

				}
			}
		});
	}

	// Create progress bar composite in the Shell. Will set it to visible when
	// the Progress Bar
	// Indicator is needed.
	protected void createProgressComposite() {
		_progressComposite = new Composite(getShell(), SWT.NONE);
		_progressComposite.setLayout(GridLayoutFactory.fillDefaults().create());
		_progressComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		((GridData) (_progressComposite.getLayoutData())).exclude = true;
		_progressComposite.setVisible(false);

		getShell().layout(true);

	}

	/*
	 * Wrapper for a task requiring progress bar: - progressText is the text for the progress bar - elements will be passed to the task Implement toExecuteTaskWithProgressBar() method to execute the
	 * task
	 */
	protected void setInputForTaskWithProgressBar(String progressText, final Object[] elements) {

		// create the progress bar
		final String progressId = createProgressIndicator(progressText);

		// execute the task
		Job job = new Job("getDocumentItemsJob") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				toExecuteTaskWithProgressBar(elements);

				return Status.OK_STATUS;
			}
		};
		// Setting job rule so jobs following this rule will be executed in the correct order.
		job.setRule(UiUtils.DISPLAY_SUGAR_ITEM_BY_ID_JOB_RULE);
		job.schedule();

		// remove the progress bar
		UIJob removeProgressIndicatorUIjob = new UIJob("Remove Progress Indicator") //$NON-NLS-1$
		{

			@Override
			public IStatus runInUIThread(IProgressMonitor arg0) {
				removeProgressIndicator(progressId);
				return Status.OK_STATUS;
			}
		};

		// Set rule so the job will be executed in the correct
		// order.
		removeProgressIndicatorUIjob.setRule(UiUtils.DISPLAY_SUGAR_ITEM_BY_ID_JOB_RULE);
		removeProgressIndicatorUIjob.schedule();
	}

	/*
	 * Tell the card to to create a progress section with the given message. This method will return an id of the newly created section. This id should be passed into removeProgressBar when the
	 * operation completes.
	 * 
	 * @param message
	 * 
	 * @return
	 */
	protected String createProgressIndicator(final String message) {
		final String id = "progessBar_" + System.currentTimeMillis(); //$NON-NLS-1$

		_display.syncExec(new Runnable() {
			@Override
			public void run() {
				_progressComposite.setLayoutDeferred(true);

				if (!_progressComposite.isVisible()) {
					_progressComposite.setVisible(true);
					((GridData) (_progressComposite.getLayoutData())).exclude = false;
				}

				Composite composite = new Composite(_progressComposite, SWT.NONE);
				composite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).margins(5, 5).create());
				composite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
				composite.setData(PROGRESS_BAR_ID, id);

				Label label = new Label(composite, SWT.WRAP);
				label.setText(message);
				label.setLayoutData(GridDataFactory.fillDefaults().grab(false, false).create());

				ProgressBar progressBar = new ProgressBar(composite, SWT.INDETERMINATE);
				progressBar.setLayoutData(GridDataFactory.fillDefaults().indent(10, 0).grab(true, false).create());

				_progressComposite.setLayoutDeferred(false);

				_progressComposite.layout(true);
				getShell().layout(true);
				getShell().pack(true);
				getShell().layout(true);
			}
		});

		return id;
	}

	protected void removeProgressIndicator(final String id) {
		_display.asyncExec(new Runnable() {
			@Override
			public void run() {
				if (_progressComposite != null && !_progressComposite.isDisposed()) {
					for (Control control : _progressComposite.getChildren()) {
						Object storedId = control.getData(PROGRESS_BAR_ID);
						if (storedId != null && storedId.equals(id)) {
							control.dispose();
						}
					}

					if (_progressComposite.getChildren().length == 0) {
						_progressComposite.setVisible(false);
						((GridData) (_progressComposite.getLayoutData())).exclude = true;
					}

					_progressComposite.layout(true);
					getShell().pack(true);
					getShell().layout(true);

					// ...Implement this method if more needs to be done...
					afterRemoveProgress();
				}
			}
		});
	}

	protected void close() {

		if (getShell() != null) {
			if (getShell().getListeners(SWT.Traverse) != null) {
				getShell().removeListener(SWT.Traverse, getShell().getListeners(SWT.Traverse)[0]);
			}
			if (getShell().getListeners(SWT.Resize) != null) {
				getShell().removeListener(SWT.Resize, getShell().getListeners(SWT.Resize)[0]);
			}
		}
		getShell().close();

	}

	public int getLeftWidthMargin() {
		return WIDTH_MARGIN;
	}

	public int getRightWidthMargin() {
		return WIDTH_MARGIN + 3;
	}

	public int getWidthMargin() {
		return WIDTH_MARGIN;
	}

	public int getHeightMargin() {
		return HEIGHT_MARGIN;
	}

	public int getTopMargin() {
		return TOP_MARGIN;
	}

	public int getBottomMargin() {
		return BOTTOM_MARGIN;
	}

	public int getDialogDefaultWidth() {
		return DEFAULT_WIDTH;
	}

	public int getDialogDefaultHeight() {
		return DEFAULT_HEIGHT;
	}

	public String getTitleText() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WELCOME_TITLE);
	}

	public Font getTitleFont() {
		return SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData();
	}

	public Color getTitleColor() {
		return null;
	}

	public Image getTitleImage() {
		return SFAImageManager.getImage(SFAImageManager.SALES_CONNECT);
	}

	/*
	 * a helper method for sub-class to use
	 */
	protected Point computeMaxSize(Composite parent, String[] arrays) {
		int width = -1;
		int height = -1;
		if (parent == null || arrays == null || arrays.length == 0) {
			return null;
		}

		GC gc = new GC(parent);
		for (int i = 0; i < arrays.length; i++) {
			Point size = gc.textExtent(arrays[i]); // or textExtent
			width = Math.max(width, size.x);
			height = Math.max(height, size.y);
		}

		gc.dispose();
		return new Point(width, height);
	}

	/*
	 * a helper method for sub-class to use
	 */
	protected void createPaddingControl(int size, Composite composite) {
		createPaddingControl(size, composite, null, true);
	}

	/*
	 * a helper method for sub-class to use
	 */
	protected void createPaddingControl(int size, Composite composite, String text, boolean isVisible) {

		for (int i = 0; i < size; i++) {
			Label l = new Label(composite, SWT.NONE);
			if (text != null) {
				l.setText(text);
			}
			l.setVisible(isVisible);
		}
	}

	/*
	 * retrieve information you passed to this class
	 */
	protected Object[] getData() {
		return _data;
	}

	/*
	 * implement this method if more is to be done after the progress bar is removed
	 */
	protected void afterRemoveProgress() {
	}

	/*
	 * implement this method for the task that requires progress bar
	 */
	protected void toExecuteTaskWithProgressBar(Object[] elements) {
	}

	abstract public void createDialogArea(Composite parent);

	abstract public void createButtonsForButtonBar(Composite parent);

}
