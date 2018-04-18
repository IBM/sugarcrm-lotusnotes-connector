package com.ibm.socialcrm.notesintegration.utils.ui;

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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;

/**
 * Create a single input dialog. Differs from eclipse's InputDialog class in that the label and error message text can be wrapped.
 * 
 * @author Hiren Patel
 */
public class InputDialog extends Dialog {
	private Button okButton;

	private String errorMessage;
	private Label errorLabel;

	private String label;
	private String title;
	private String value;
	private IInputValidator validator;
	private Text inputText;

	/**
	 * Constructor
	 * 
	 * @param parentShell
	 * @param title
	 *            - The text that appears in the title bar.
	 * @param label
	 *            - A descriptive label about what is being collected.
	 * @param initialValue
	 *            - The initial value to be set in the input field.
	 * @param validator
	 *            - A validator for the input
	 */
	public InputDialog(Shell parentShell, String title, String label, String initialValue, IInputValidator validator) {
		super(parentShell);
		this.title = title;
		this.value = initialValue;
		this.label = label;
		this.validator = validator;
	}

	/**
	 * Override to get references to our ok button
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent.setLayout(new GridLayout(1, false));

		if (label != null && !label.equals(ConstantStrings.EMPTY_STRING)) {
			Label labelArea = new Label(parent, SWT.WRAP);
			labelArea.setLayoutData(GridDataFactory.fillDefaults().hint(100, SWT.DEFAULT).create());
			labelArea.setText(label);
		}

		inputText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		inputText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(300, SWT.DEFAULT).create());
		inputText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				value = inputText.getText();
				validateInput();
			}
		});

		errorLabel = new Label(parent, SWT.WRAP);
		errorLabel.setLayoutData(GridDataFactory.fillDefaults().hint(100, SWT.DEFAULT).grab(false, true).create());

		if (value != null) {
			inputText.setText(value);
		}

		return parent;
	}

	private void validateInput() {
		String errorMessage = null;
		if (validator != null) {
			errorMessage = validator.isValid(inputText.getText());
		}
		this.errorMessage = errorMessage;
		errorLabel.setText(this.errorMessage == null ? ConstantStrings.EMPTY_STRING : this.errorMessage);
		updateButtons();
	}

	/**
	 * Enable ok button only value is entered and it's valid.
	 */
	private void updateButtons() {
		boolean complete = value != null && !value.equals(ConstantStrings.EMPTY_STRING) && (errorMessage == null || errorMessage.equals(ConstantStrings.EMPTY_STRING));

		if (okButton != null) {
			okButton.setEnabled(complete);
		}
	}

	/**
	 * Override to set the shell title
	 */
	public void create() {
		super.create();
		if (title != null) {
			getShell().setText(title);
		}
		validateInput();
	}

	public String getValue() {
		return value;
	}
}
