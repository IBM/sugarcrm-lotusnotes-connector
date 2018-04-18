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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.internal.forms.widgets.FormUtil;

public class EasyScrolledComposite extends ScrolledComposite {

	private ControlListener controlListener = new ControlAdapter() {
		public void controlResized(ControlEvent e) {
			updateScrollbars();
		}
	};

	private FocusListener focusListener = new FocusAdapter() {
		public void focusGained(FocusEvent e) {
			if (e.widget instanceof Control) {
				ensureVisible((Control) e.widget);
			}
		}
	};

	public EasyScrolledComposite(Composite parent, int style) {
		super(parent, style);
		addControlListener(controlListener);
		setExpandHorizontal(true);
		setExpandVertical(true);
	}

	public void setContent(Control control) {
		if (getContent() != null) {
			setListening(getContent(), false);
		}

		if (control != null) {
			setListening(control, true);
		}

		if (getVerticalBar() != null) {
			// Set the default vertical scroll increment to 30 to make scrolling a little faster
			getVerticalBar().setIncrement(30);
		}

		super.setContent(control);
	}

	private void setListening(Control control, boolean listening) {
		if (listening) {
			control.addFocusListener(focusListener);
		} else {
			control.removeFocusListener(focusListener);
		}

		if (control instanceof Composite) {
			for (Control child : ((Composite) control).getChildren()) {
				setListening(child, listening);
			}
		}
	}

	public void layout(boolean changed, boolean all) {
		updateScrollbars();
		super.layout(changed, all);
	}

	public void layout(Control[] changed) {
		updateScrollbars();
		super.layout(changed);
	}

	public void updateScrollbars() {
		Rectangle r = getClientArea();

		if (getContent() != null) {
			setMinSize(getContent().computeSize(r.width, SWT.DEFAULT));
			getContent().redraw();
		}
	}

	public void ensureVisible(Control control) {
		Point controlOrigin = FormUtil.getControlLocation(this, control);
		Point controlSize = control.getSize();

		Rectangle area = getClientArea();
		Point origin = getOrigin();

		int x = origin.x;
		int y = origin.y;

		// horizontal right
		if (controlOrigin.x + controlSize.x > origin.x + area.width) {
			x = controlOrigin.x + controlSize.x - area.width;
		}
		// horizontal left
		if (controlOrigin.x < x) {
			x = controlOrigin.x;
		}
		// vertical bottom
		if (controlOrigin.y + controlSize.y > origin.y + area.height) {
			y = controlOrigin.y + controlSize.y - area.height;
		}
		// vertical top
		if (controlOrigin.y < y) {
			y = controlOrigin.y;
		}

		// Snap
		if (x <= 10) {
			x = 0;
		} else if (x + area.width + 10 >= getMinWidth()) {
			x = getMinWidth() - area.width;
		}

		if (y <= 10) {
			y = 0;
		} else if (y + area.height + 10 >= getMinHeight()) {
			y = getMinHeight() - area.height;
		}

		if (origin.x != x || origin.y != y) {
			setOrigin(x, y);
		}
	}
}
