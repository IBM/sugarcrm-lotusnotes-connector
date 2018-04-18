package com.ibm.socialcrm.notesintegration.utils.widgets;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/**
 * A custom implementation of an ExpandBar. The SWT ExpandBar/ExpandItem didn't handle scrolling very well and the ability to configure the header is quite limited.
 * 
 * See <code>ExpandItem</code> for sample code on how to use this.
 * 
 * @author bcbull
 */
public class SFAExpandBar extends Composite {
	private List<SFAExpandItem> expandItems;

	/**
	 * Since this type of expand bar will likely be used in an SFA environment, it'll probably reside on an EasyScrolledComposite. Users can set the composite they want to be notified when a child
	 * item is expanded or contracted.
	 */
	private EasyScrolledComposite scrolledComposite;

	/**
	 * If tab traversal isn't working quite the way you want it to, you can set the next and previous components that should receive focus using setForwardTabControl() and setPreviousTabControl().
	 * 
	 * Specifies the control that should receive tab focus when traversing forward out of the expand bar. If nothing is specified, the default BBPExpandBar tab behavior will be used.
	 */
	private Control forwardTabControl;

	/**
	 * Specifies the control that should receive tab focus when traversing backward out of the expand bar. If nothing is specified, the default BBPExpandBar tab behavior will be used.
	 */
	private Control reverseTabControl;

	public SFAExpandBar(final Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * BBPExpandBar handles tab traversal itself.
	 */
	@Override
	public boolean traverse(int traversal) {
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
		SFAExpandItem currentItem = null;
		SFAExpandItem nextItem = null;

		int index = getFocusItemIndex();
		if (index == -1) {
			if (getExpandItems().size() > 0) {
				nextItem = getExpandItems().get(0);
			}
		} else {
			currentItem = getExpandItems().get(index);
			if (index < getExpandItems().size() - 1) {
				nextItem = getExpandItems().get(index + 1);
			}
		}

		// If we're not at the bottom of the list
		if (nextItem != null) {
			// If the current item has selection and is expanded, delegate to the controlComposite of the item
			if (currentItem != null && currentItem.isExpanded()) {
				success = currentItem.getControlComposite().traverse(SWT.TRAVERSE_TAB_NEXT);
			} else {
				nextItem.requestFocus();
			}
		} else {
			if (getForwardTabControl() != null) {
				getForwardTabControl().setFocus();
			} else {
				// If there isn't a next item, delegate to our parent
				if (currentItem != null && currentItem.isExpanded()) {
					success = currentItem.getControlComposite().traverse(SWT.TRAVERSE_TAB_NEXT);
				} else {
					success = getParent().traverse(SWT.TRAVERSE_TAB_NEXT);
				}
			}
		}

		return success;
	}

	/**
	 * Helper method to handle backward tab traversal
	 */
	private boolean traverseBackward() {
		boolean success = true;
		SFAExpandItem previousItem = null;

		int index = getFocusItemIndex();
		if (index == -1) {
			if (getExpandItems().size() > 0) {
				previousItem = getExpandItems().get(getExpandItems().size() - 1);
			}
		} else {
			if (index > 0) {
				previousItem = getExpandItems().get(index - 1);
			}
		}

		if (previousItem != null) {
			if (previousItem.isExpanded()) {
				// If we have to back up to the previous item, we need to find the last focusable component
				// in the items's control composite
				Control lastFocusable = getLastFocusableControl(previousItem.getControlComposite());

				if (lastFocusable != null) {
					lastFocusable.setFocus();
				} else { // If there are no focusable components, fall back to the previous item
					previousItem.requestFocus();
				}
			} else {
				previousItem.requestFocus();
			}
		} else {
			if (getReverseTabControl() != null) {
				getReverseTabControl().setFocus();
			} else {
				// Relying on our parent to traverse backwards doesn't yield the desired results most of the time.
				// So we'll try our own strategy first. If we don't find a focusable component, so be it.
				Control lastFocusable = getLastFocusableControl(getParent(), this);
				if (lastFocusable != null) {
					lastFocusable.setFocus();
				} else {
					success = getParent().traverse(SWT.TRAVERSE_TAB_PREVIOUS);
				}
			}
		}

		return success;
	}

	/**
	 * Helper method to find the last focusable component.
	 * 
	 * SWT doesn't have an API to indicate if a component is focusable, so our definition is "not a composite and not a label". That's not a very good definition, but it should hold up well enough.
	 * 
	 * @param parent
	 * @return a control or null if no focusable controls exist.
	 */
	private Control getLastFocusableControl(Composite parent) {
		return getLastFocusableControl(parent, null);
	}

	/**
	 * Finds the last focusable control in a Composite starting from the specified control
	 * 
	 * @param parent
	 *            - The parent composite
	 * @param startingControl
	 *            - The "starting" control. Even though this method is recursive, this argument probably only makes sense for the first iteration of the method it will most likely be called with
	 *            "parent" as the parent composite of this SFAExandBar and "startingControl" as this SFAExpandBar. Regardless, it will be passed through for all recursive calls.
	 * @return
	 */
	private Control getLastFocusableControl(Composite parent, Control startingControl) {
		Control lastFocusable = null;

		Control[] children = parent.getChildren();
		int start = children.length - 1;

		if (startingControl != null) {
			for (int i = start; i > 0; i--) {
				if (children[i] == startingControl) {
					start = i - 1;
					break;
				}
			}
		}

		for (int i = start; i >= 0 && lastFocusable == null; i--) {
			if (children[i].isEnabled() && !(children[i] instanceof Composite) && !(children[i] instanceof Label)) {
				lastFocusable = children[i];
			} else if (children[i] instanceof Composite) {
				lastFocusable = getLastFocusableControl((Composite) children[i], startingControl);
			}
		}
		return lastFocusable;
	}

	/**
	 * Returns the index of the SFAExpandItem that currently has focus
	 * 
	 * @return
	 */
	private int getFocusItemIndex() {
		int index = -1;

		int ctr = 0;
		for (SFAExpandItem item : getExpandItems()) {
			if (item.isFocusItem()) {
				index = ctr;
				break;
			}
			ctr++;
		}

		return index;
	}

	/**
	 * Returns the list of expand items associated with this bar
	 * 
	 * @return
	 */
	private List<SFAExpandItem> getExpandItems() {
		if (expandItems == null) {
			expandItems = new ArrayList<SFAExpandItem>();
		}
		return expandItems;
	}

	/**
	 * Adds an expand item to this SFAExpandBar. It is VERY important that all SFAExpandItem's be added to a SFAExpandBar. Otherwise, keyboard navigation won't work!
	 * 
	 * @param item
	 */
	public void addExpandItem(SFAExpandItem item) {
		if (item != null) {
			getExpandItems().add(item);
			item.getPropertyChangeSupport().addPropertyChangeListener(SFAExpandItem.EXPAND_PROPERTY, new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					if (scrolledComposite != null) {
						scrolledComposite.updateScrollbars();
					}
				}
			});
		}
	}

	/**
	 * Sets the scrolled composite that will be notified when a child item is expanded/contracted
	 * 
	 * @param scrolledComposite
	 */
	public void setScrolledComposite(EasyScrolledComposite scrolledComposite) {
		this.scrolledComposite = scrolledComposite;
	}

	/**
	 * Returns the control that should receive focus when forward tabbing out of the expand bar.
	 * 
	 * @return
	 */
	public Control getForwardTabControl() {
		return forwardTabControl;
	}

	/**
	 * Sets the control that should receive focus when forward tabbing out of the expand bar.
	 * 
	 * @return
	 */
	public void setForwardTabControl(Control forwardTabControl) {
		this.forwardTabControl = forwardTabControl;
	}

	/**
	 * Returns the control that should receive focus when reverse tabbing out of the expand bar.
	 * 
	 * @return
	 */
	public Control getReverseTabControl() {
		return reverseTabControl;
	}

	/**
	 * Sets the control that should receive focus when reverse tabbing out of the expand bar.
	 * 
	 * @return
	 */
	public void setReverseTabControl(Control reverseTabControl) {
		this.reverseTabControl = reverseTabControl;
	}

}
