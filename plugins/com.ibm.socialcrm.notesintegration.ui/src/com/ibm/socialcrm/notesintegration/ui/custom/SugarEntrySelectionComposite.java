package com.ibm.socialcrm.notesintegration.ui.custom;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;

/**
 * Composite that lets the user choose from multiple items when our live text matches yield multiple results.
 */
public class SugarEntrySelectionComposite extends Composite {
	private List<BaseSugarEntry> entries;

	private boolean sizeSet = false;

	public SugarEntrySelectionComposite(Shell shell, List<BaseSugarEntry> entries) {
		super(shell, SWT.NONE);

		this.entries = entries;
		createComposite();
	}

	protected void createComposite() {
		setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).spacing(0, 0).create());
		setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		final SugarItemList itemList = new SugarItemList(this, SWT.NONE);

		getShell().setText(entries.get(0).getSugarType().getMultiSelectionTitle());

		final SugarItem[] items = new SugarItem[entries.size()];
		int ctr = 0;
		for (BaseSugarEntry entry : entries) {
			items[ctr] = itemList.addItem(entry);
			ctr++;
		}

		items[0].addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent arg0) {
				if (!sizeSet) {
					sizeSet = true;
					int y = items[0].getSize().y;

					// We don't want to show more than 7 items by default (The 8 roughly accounts for the size of the shell header).
					if (entries.size() > 7) {
						getShell().setSize(getShell().getSize().x, 8 * y);
					}
				}
			}
		});

		itemList.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(300, SWT.DEFAULT).create());
		itemList.setFocus();

	}

}
