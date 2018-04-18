package com.ibm.socialcrm.notesintegration.ui.custom;

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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.SugarAccount;
import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.core.SugarOpportunity;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.widgets.EasyScrolledComposite;

/**
 * A composite that lets the user select from multiple sugar items when our matching yields multiple results.
 */
public class SugarItemList extends Composite {
	private List<SugarItem> sugarItems;

	private Composite innerComposite;

	private SugarItem focusItem = null;

	private EasyScrolledComposite scrollComposite;

	public SugarItemList(Composite parent, int style) {
		super(parent, style);
		setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).spacing(0, 0).create());

		scrollComposite = new EasyScrolledComposite(this, SWT.V_SCROLL | SWT.H_SCROLL);
		scrollComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).create());
		scrollComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		innerComposite = new Composite(scrollComposite, SWT.NONE);
		innerComposite.setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).spacing(0, 0).create());
		innerComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).hint(300, 150).create());
		innerComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		scrollComposite.setContent(innerComposite);
	}

	public SugarItem addItem(BaseSugarEntry entry) {
		final SugarItem item = createSugarItem(entry);
		if (item != null) {
			item.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).minSize(300, SWT.DEFAULT).create());
			getSugarItems().add(item);
			Label separator = new Label(innerComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
			separator.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
			List<Control> controls = UiUtils.getInnerControls(item);
			for (Control control : controls) {
				control.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent arg0) {
						SugarItem oldFocusItem = focusItem;
						focusItem = item;
						item.redraw();
						item.setFocus();
						if (oldFocusItem != null) {
							oldFocusItem.redraw();
						}
						focusItem.displayDashboard();
					}
				});
			}
		}
		return item;
	}

	public boolean traverseInternal(int traversal) {
		boolean success = true;

		if (traversal == SWT.TRAVERSE_TAB_NEXT) {
			success = traverseForward();
		} else if (traversal == SWT.TRAVERSE_TAB_PREVIOUS) {
			success = traverseBackward();
		}

		return success;
	}

	/**
	 * Helper method to handle forward tab traversal
	 * 
	 * @return
	 */
	private boolean traverseForward() {
		boolean success = true;
		SugarItem nextItem = null;

		int index = getFocusItemIndex();
		if (index == -1) {
			if (getSugarItems().size() > 0) {
				nextItem = getSugarItems().get(0);
			}
		} else {
			if (index < getSugarItems().size() - 1) {
				nextItem = getSugarItems().get(index + 1);
			} else {
				nextItem = getSugarItems().get(0);
			}
		}

		// If we're not at the bottom of the list
		SugarItem currentItem = focusItem;
		if (nextItem != null) {
			focusItem = nextItem;
			nextItem.redraw();
			getScrollComposite().getVerticalBar().setSelection(nextItem.getBounds().y);
			getScrollComposite().setOrigin(0, nextItem.getBounds().y);
		}
		if (currentItem != null) {
			currentItem.redraw();
		}

		return success;
	}

	/**
	 * Helper method to handle backward tab traversal
	 */
	private boolean traverseBackward() {
		boolean success = true;
		SugarItem previousItem = null;

		int index = getFocusItemIndex();
		if (index == -1) {
			if (getSugarItems().size() > 0) {
				previousItem = getSugarItems().get(getSugarItems().size() - 1);
			}
		} else {
			if (index > 0) {
				previousItem = getSugarItems().get(index - 1);
			} else {
				previousItem = getSugarItems().get(getSugarItems().size() - 1);
			}
		}

		SugarItem currentItem = focusItem;
		if (previousItem != null) {
			focusItem = previousItem;
			previousItem.redraw();
			getScrollComposite().getVerticalBar().setSelection(previousItem.getBounds().y);
			getScrollComposite().setOrigin(0, previousItem.getBounds().y);
		}
		if (currentItem != null) {
			currentItem.redraw();
		}

		return success;
	}

	/**
	 * Returns the index of the SFAExpandItem that currently has focus
	 * 
	 * @return
	 */
	private int getFocusItemIndex() {
		int index = -1;

		int ctr = 0;
		for (SugarItem item : getSugarItems()) {
			if (item == focusItem) {
				index = ctr;
				break;
			}
			ctr++;
		}

		return index;
	}

	private SugarItem createSugarItem(BaseSugarEntry entry) {
		SugarItem item = null;
		if (entry instanceof SugarContact) {
			item = new SugarContactItem(this, (SugarContact) entry);
		} else if (entry instanceof SugarAccount) {
			item = new SugarAccountItem(this, (SugarAccount) entry);
		} else if (entry instanceof SugarOpportunity) {
			item = new SugarOpportunityItem(this, (SugarOpportunity) entry);
		}
		return item;
	}

	public EasyScrolledComposite getScrollComposite() {
		return scrollComposite;
	}

	public Composite getInnerComposite() {
		return innerComposite;
	}

	public List<SugarItem> getSugarItems() {
		if (sugarItems == null) {
			sugarItems = new ArrayList<SugarItem>();
		}
		return sugarItems;
	}

	public SugarItem getFocusItem() {
		return focusItem;
	}

	@Override
	public boolean setFocus() {
		boolean focused = true;
		if (focusItem == null) {
			if (getSugarItems().size() > 0) {
				focusItem = getSugarItems().get(0);
				focused = focusItem.setFocus();
			}
		} else {
			focused = focusItem.setFocus();
		}
		return focused;
	}

}
