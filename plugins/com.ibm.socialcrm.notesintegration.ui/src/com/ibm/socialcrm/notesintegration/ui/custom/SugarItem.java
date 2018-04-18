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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;

public abstract class SugarItem<T extends BaseSugarEntry> extends Composite {
	private T entry;

	private Composite innerComposite;
	private Canvas borderCanvas;

	private SugarItemList itemList;

	/**
	 * Create the styled text object for this sugar item
	 * 
	 * @return
	 */
	public abstract void buildItem(Composite parent);

	/**
	 * Return the accessible name for this item
	 * 
	 * @return
	 */
	public abstract String getAccessibleName();

	public SugarItem(SugarItemList itemList, T sugarEntry) {
		super(itemList.getInnerComposite(), SWT.NONE);
		setItemList(itemList);
		setEntry(sugarEntry);

		setLayout(GridLayoutFactory.fillDefaults().spacing(0, 0).margins(0, 0).create());
		setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		borderCanvas = new Canvas(this, SWT.FOCUSED) {
			{
				addPaintListener(new PaintListener() {
					LineAttributes attributes = new LineAttributes(1, SWT.CAP_SQUARE, SWT.JOIN_BEVEL, SWT.LINE_DOT, new float[] { .5f }, SWT.LINE_CUSTOM, 10);

					public void paintControl(PaintEvent e) {
						if (getItemList().getFocusItem() == SugarItem.this) {
							// if (UiUtils.isParentOf(getItemList(), Display.getDefault().getFocusControl()))
							// {
							Rectangle rect = getClientArea();
							e.gc.setLineAttributes(attributes);
							e.gc.drawRectangle(rect.x, rect.y, rect.width - 1, rect.height - 1);
							// }
							colorForSelection();
						} else {
							colorForDeselection();
						}
					}
				});
			}
		};
		borderCanvas.setLayout(GridLayoutFactory.fillDefaults().margins(2, 2).create());
		borderCanvas.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		borderCanvas.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		borderCanvas.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					getItemList().traverseInternal(SWT.TRAVERSE_TAB_NEXT);
				} else if (e.keyCode == SWT.ARROW_UP) {
					getItemList().traverseInternal(SWT.TRAVERSE_TAB_PREVIOUS);
				}
				// This doesn't seem to work. For now, we just won't use this widget in a circumstance where there are
				// other things around it we have to tab to.
				// else if (e.keyCode == SWT.TAB)
				// {
				// if (e.stateMask == 0)
				// {
				// getItemList().getParent().traverse(SWT.TRAVERSE_TAB_NEXT);
				// }
				// else if (e.stateMask == SWT.SHIFT)
				// {
				// getItemList().getParent().traverse(SWT.TRAVERSE_TAB_PREVIOUS);
				// }
				// }
				else if (e.keyCode == SWT.CR || e.keyCode == 32) {
					displayDashboard();
				}
			}
		});

		innerComposite = new Composite(borderCanvas, SWT.NONE | SWT.NO_FOCUS);
		innerComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		innerComposite.setLayout(GridLayoutFactory.fillDefaults().margins(1, 1).create());
		innerComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		buildItem(innerComposite);

		getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getDescription(AccessibleEvent e) {
				e.result = getAccessibleName();
			}
		});
	}

	/**
	 * Displays the dashboard for this item
	 */
	public void displayDashboard() {
		if (getEntry() != null) {
			Rectangle monitorBounds = Display.getDefault().getPrimaryMonitor().getBounds();
			Rectangle shellBounds = getShell().getBounds();
			int spaceRight = monitorBounds.width - (shellBounds.x + shellBounds.width);
			int spaceBottom = monitorBounds.height - (shellBounds.y + shellBounds.height);

			boolean showRight = true;
			// TODO: Right now, we're not saving the size of the dashboard panel so it always comes up
			// with size SugarItemsDashboard.DEFAULT_WIDTH x SugarItemsDashboard.DEFAULT_HEIGHT. We may need
			// to change this.
			// If 80% of the window will fit to the right, place it there.
			if (spaceRight > SugarItemsDashboard.DEFAULT_WIDTH * .8) {
				showRight = true;
			} else {
				showRight = false;
			}

			int x = 0;
			if (showRight) {
				x = getShell().getLocation().x + shellBounds.width;
			} else {
				x = getShell().getLocation().x - SugarItemsDashboard.DEFAULT_WIDTH;
			}

			int y = getShell().getLocation().y + 25;
			if (spaceBottom < SugarItemsDashboard.DEFAULT_HEIGHT) {
				y = monitorBounds.height - SugarItemsDashboard.DEFAULT_HEIGHT;
			}

			UiUtils.displaySugarItemById(entry.getSugarType(), entry.getId(), new Point(x, y), null);
		}
	}

	/**
	 * Sets the background color for this item when it is selected
	 */
	public void colorForSelection() {
		UiUtils.recursiveSetForegroundColor(innerComposite, Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		UiUtils.recursiveSetBackgroundColor(innerComposite, Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION));
	}

	/**
	 * Sets the background color for this item when it is deselected
	 */
	public void colorForDeselection() {
		UiUtils.recursiveSetForegroundColor(innerComposite, Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		UiUtils.recursiveSetBackgroundColor(innerComposite, Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
	}

	public SugarItemList getItemList() {
		return itemList;
	}

	public void setItemList(SugarItemList itemList) {
		this.itemList = itemList;
	}

	/**
	 * Redraws this item
	 */
	public void redraw() {
		super.redraw();
		borderCanvas.redraw();
	}

	@Override
	public boolean setFocus() {
		redraw();
		return borderCanvas.setFocus();
	}

	public T getEntry() {
		return entry;
	}

	public void setEntry(T entry) {
		this.entry = entry;
	}
}