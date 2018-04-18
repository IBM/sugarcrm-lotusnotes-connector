package com.ibm.socialcrm.notesintegration.ui.dialogs;

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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class OverwriteContactDialog extends Dialog {
	private SugarContact contact;

	public OverwriteContactDialog(Shell shell, SugarContact contact) {
		super(shell);
		this.contact = contact;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);

		Monitor primary = Display.getDefault().getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = newShell.getBounds();

		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;

		newShell.setLocation(x, y);
		newShell.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.ADD_CONTACT_TITLE));
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).margins(10, 10).spacing(15, 0).create());
		composite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		Label errorLabel = new Label(composite, SWT.NONE);
		errorLabel.setImage(SFAImageManager.getImage(SFAImageManager.LARGE_ERROR));

		Label messageLabel = new Label(composite, SWT.WRAP);
		messageLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.OVERWRITE_CONTACT_MESSAGE, new String[] { contact.getName(), contact.getFirstName() }));
		messageLabel.setLayoutData(GridDataFactory.fillDefaults().grab(false, true).hint(400, SWT.DEFAULT).create());

		return composite;
	}

	@Override
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
		Button button = super.createButton(parent, id, label, defaultButton);
		if (id == Dialog.OK) {
			button.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UPDATE));
		}
		return button;
	}
}
