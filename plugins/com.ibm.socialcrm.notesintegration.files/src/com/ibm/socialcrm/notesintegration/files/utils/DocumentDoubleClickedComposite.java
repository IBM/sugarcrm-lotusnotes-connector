package com.ibm.socialcrm.notesintegration.files.utils;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.ibm.socialcrm.notesintegration.core.ConnectionsDocument;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class DocumentDoubleClickedComposite extends AbstractDialogComposite {

	protected final static int myDefaultWidth = 500;
	protected final static int myDefaultHeight = 300;
	private ConnectionsDocument _cd = null;

	public DocumentDoubleClickedComposite(Display display, ConnectionsDocument[] cds) {
		super(display, true, cds);
	}

	@Override
	public void createDialogArea(Composite parent) {
		Composite headerComposite = new Composite(parent, SWT.NONE);
		Rectangle margins = Geometry.createDiffRectangle(25, 15, 25, 10);

		headerComposite.setLayout(GridLayoutFactory.fillDefaults().extendedMargins(margins).numColumns(2).spacing(5, 10).create());
		headerComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		headerComposite.setBackground(JFaceColors.getBannerBackground(Display.getDefault()));

		// ==============================================
		Label label1 = new Label(headerComposite, SWT.None);
		label1.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(2, 1).create());
		label1.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_HELPTEXT));
		label1.setFont(SugarItemsDashboard.getInstance().getTableHeaderFont());
		label1.setBackground(headerComposite.getBackground());

		// ==============================================
		Label label21 = new Label(headerComposite, SWT.None);
		label21.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(1, 1).grab(false, false).create());
		label21.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_NAME_LABEL));
		label21.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
		label21.setBackground(headerComposite.getBackground());

		Label label22 = new Label(headerComposite, SWT.None);
		label22.setLayoutData(GridDataFactory.fillDefaults().indent(5, SWT.DEFAULT).align(SWT.LEFT, SWT.CENTER).span(1, 1).grab(true, false).create());
		label22.setText(getCurrConnectionsDocument().getFilename());
		label22.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
		label22.setBackground(headerComposite.getBackground());

		// ==============================================
		Label label31 = new Label(headerComposite, SWT.None);
		label31.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(1, 1).grab(false, false).create());
		label31.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_SIZE_LABEL));
		label31.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
		label31.setBackground(headerComposite.getBackground());

		Label label32 = new Label(headerComposite, SWT.None);
		label32.setLayoutData(GridDataFactory.fillDefaults().indent(5, SWT.DEFAULT).align(SWT.LEFT, SWT.CENTER).span(1, 1).grab(true, false).create());
		label32.setText(getCurrConnectionsDocument().getFormattedSize());

		label32.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
		label32.setBackground(headerComposite.getBackground());

		// ==============================================
		Label label41 = new Label(headerComposite, SWT.None);
		label41.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(1, 1).grab(false, false).create());
		label41.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_MODIFIEDDATE_LABEL));
		label41.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
		label41.setBackground(headerComposite.getBackground());

		Label label42 = new Label(headerComposite, SWT.None);
		label42.setLayoutData(GridDataFactory.fillDefaults().indent(5, SWT.DEFAULT).align(SWT.LEFT, SWT.CENTER).span(1, 1).grab(true, false).create());
		label42.setText(getCurrConnectionsDocument().getMDate());
		label42.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
		label42.setBackground(headerComposite.getBackground());

	}

	@Override
	public void createButtonsForButtonBar(Composite parent) {
		Composite buttonComposite = new Composite(parent, SWT.NONE);
		Rectangle margins = Geometry.createDiffRectangle(25, 0, 0, 0);

		buttonComposite.setLayout(GridLayoutFactory.fillDefaults().extendedMargins(margins).spacing(10, SWT.DEFAULT).numColumns(5).equalWidth(false).create());
		buttonComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		// padding button to grab horizontal space when resizing
		Button paddingBtn = new Button(buttonComposite, SWT.NONE);

		// hack to force the end of the cancel button to align with lookup field - might need a better way to do this.
		Rectangle rec = parent.getBounds();
		Point point = computeMaxSize(parent, new String[]{UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_OPEN_OPTION),
				UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_SAVE_OPTION), IDialogConstants.CANCEL_LABEL});
		paddingBtn.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(rec.width - (3 * (point.x + getWidthMargin())) - 35, point.y + getHeightMargin() + 10).create());
		paddingBtn.setVisible(false);

		// Open Button
		Button openBtn = new Button(buttonComposite, SWT.PUSH);
		openBtn.setLayoutData(GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).hint(SWT.DEFAULT, SWT.DEFAULT).create());
		openBtn.setFont(JFaceResources.getDialogFont());
		openBtn.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_OPEN_OPTION));
		addOpenButtonListeners(openBtn);
		openBtn.setSelection(true);

		// Save Button
		Button saveBtn = new Button(buttonComposite, SWT.PUSH);
		saveBtn.setLayoutData(GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).hint(SWT.DEFAULT, SWT.DEFAULT).create());
		saveBtn.setFont(JFaceResources.getDialogFont());
		saveBtn.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_SAVE_OPTION));
		addSaveButtonListeners(saveBtn);

		// Cancel Button
		Button cancelBtn = new Button(buttonComposite, SWT.PUSH);
		cancelBtn.setLayoutData(GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).hint(SWT.DEFAULT, SWT.DEFAULT).create());
		cancelBtn.setFont(JFaceResources.getDialogFont());
		cancelBtn.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_CANCEL_OPTION));
		addCancelButtonListeners(cancelBtn);

	}

	private void addOpenButtonListeners(final Button button) {
		final SelectionListener selectionListener = new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				processDocument(DocumentDownloadOperations.DOCUMENT_DOWNLOAD_OPEN);
			}

		};
		button.addSelectionListener(selectionListener);
		button.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				button.removeSelectionListener(selectionListener);
			}

		});
	}

	private void addSaveButtonListeners(final Button button) {
		final SelectionListener selectionListener = new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				processDocument(DocumentDownloadOperations.DOCUMENT_DOWNLOAD_SAVE);
			}

		};
		button.addSelectionListener(selectionListener);
		button.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				button.removeSelectionListener(selectionListener);
			}

		});
	}

	private void addCancelButtonListeners(final Button button) {
		final SelectionListener selectionListener = new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				close();
			}

		};
		button.addSelectionListener(selectionListener);
		button.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				button.removeSelectionListener(selectionListener);
			}

		});
	}

	private void processDocument(final int option) {
		boolean isOK = true;
		final DocumentDownloadOperations ddOperations = new DocumentDownloadOperations(option, getCurrConnectionsDocument());
		if (option == DocumentDownloadOperations.DOCUMENT_DOWNLOAD_SAVE || option == DocumentDownloadOperations.DOCUMENT_DOWNLOAD_OPEN) {
			isOK = ddOperations.buildTargetFile();
		}
		if (isOK) {
			setInputForTaskWithProgressBar(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_PROCESSING), new Object[]{ddOperations});
		}
	}

	private ConnectionsDocument getCurrConnectionsDocument() {
		if (_cd == null) {
			if (getData() != null && getData().length == 1) {
				_cd = (ConnectionsDocument) getData()[0];
			}
		}
		return _cd;
	}

	@Override
	protected void afterRemoveProgress() {
		close();
	}

	@Override
	protected void toExecuteTaskWithProgressBar(Object[] elements) {
		if (elements != null && elements.length == 1) {
			final DocumentDownloadOperations ddOperations = (DocumentDownloadOperations) elements[0];

			ddOperations.toExecute();
		}
	}

	@Override
	public int getDialogDefaultWidth() {
		return myDefaultWidth;
	}

	@Override
	public int getDialogDefaultHeight() {
		return myDefaultHeight;
	}

	@Override
	public String getTitleText() {
		return UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.DOCUMENTS_DOWNLOAD_DIALOG_TITLE);
	}

}
