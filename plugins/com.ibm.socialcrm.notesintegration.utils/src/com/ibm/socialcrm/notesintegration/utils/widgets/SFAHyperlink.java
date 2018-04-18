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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.themes.ColorUtil;

import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;

/**
 * Special hyperlink class that overrides the default foreground color that that disabled links actually look disabled.
 * 
 * It also provides the ability to ellipsize text at the end of the link. SWT hyperlinks can ellipsize in the middle if horizontal grab is set to true, but ellipsizing on the end looks better.
 * 
 * @author bcbull
 */
public class SFAHyperlink extends Hyperlink {
	/**
	 * Internal calculated disabled color
	 */
	private static Color disabledColor = null;

	/**
	 * Since the text can be ellipsized, we have to store away the real text.
	 */
	private String realText = ConstantStrings.EMPTY_STRING;

	/**
	 * Flag that tells us to save the realText whenever setText is called.
	 */
	private boolean saveRealText = true;

	/**
	 * The length of the text in pixels
	 */
	private int textLengthInPixels = 0;

	/**
	 * Draw a strikethrough line on the link
	 */
	private boolean strikethrough = false;		

	/**
	 * Creates a new SFAHyperlink
	 * 
	 * @param parent
	 * @param style
	 */
	public SFAHyperlink(Composite parent, int style) {
		this(parent, style, false);
	}

	/**
	 * Creates a new SFAHyperlink whose text will be ellipsized if there isn't enough space in the parent to render the entire link.
	 * 
	 * @param parent
	 * @param style
	 * @param ellipsize
	 */
	public SFAHyperlink(Composite parent, int style, boolean ellipsize) {
		super(parent, style);

		if (ellipsize) {
			getParent().addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent e) {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							ellipsize();
						}
					});
				}
			});
		}
		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (isStrikethrough() && realText.length() > 0) {
					LineAttributes attributes = new LineAttributes(2, SWT.CAP_ROUND, SWT.JOIN_ROUND, SWT.LINE_SOLID, new float[]{20f}, SWT.LINE_CUSTOM, 10);

					Point size = getSize();
					int x = size.x;
					int y = size.y;

					e.gc.setLineAttributes(attributes);
					e.gc.drawLine(0, (y / 2) + 1, x, (y / 2) + 1);
				}
			}
		});
	}

	private void ellipsize() {
		if (!isDisposed()) {
			Rectangle parentBounds = getParent().getBounds();
			int parentXmax = parentBounds.width;
			if (parentBounds.width != 0) {
				String tempValue = realText;
				Rectangle linkBounds = getBounds();
				boolean addEllipsis = linkBounds.x + textLengthInPixels > parentXmax;

				if (addEllipsis) {
					int averageWidth = textLengthInPixels / realText.length();
					int numCharacters = (parentBounds.width - linkBounds.x) / averageWidth;

					numCharacters -= 4; // -4 for the ... It's only 3 characters, but allotting for 4 works better
					String displayString = tempValue.substring(0, Math.max(0, Math.min(numCharacters, tempValue.length())));
					displayString += "..."; //$NON-NLS-1$
					saveRealText = false;
					setText(displayString.trim());
					saveRealText = true;
				} else {
					setText(realText);
				}
			}
		}
	}

	@Override
	public void setText(String text) {
		super.setText(text);
		if (saveRealText) {
			realText = text;
			computeTextLengthInPixels();
		}
	}

	/**
	 * Computes the appropriate length for the link based on the current font
	 */
	private void computeTextLengthInPixels() {
		// 36275
		if (realText == null || realText.equals(ConstantStrings.EMPTY_STRING)) {
		} else {
			GC gc = new GC(SFAHyperlink.this);
			textLengthInPixels = gc.stringExtent(realText).x;
			gc.dispose();
		}
	}

	@Override
	public void setFont(Font font) {
		super.setFont(font);
		computeTextLengthInPixels();
	}

	/**
	 * Override to update the colors when the enabled state changes
	 */
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		redraw();
	}

	/*
	 * Overridden to display as greyed-out hyperlink when disabled (non-Javadoc)
	 * 
	 * @see org.eclipse.swt.widgets.Control#getForeground()
	 */
	@Override
	public Color getForeground() {
		if (disabledColor == null) {
			try {
				Color foreground = getParent().getForeground();
				Color background = getBackground();
				final RGB disabledRGB = ColorUtil.blend(foreground.getRGB(), background.getRGB());
				disabledColor = JFaceResources.getResources().createColor(disabledRGB);
				getDisplay().disposeExec(new Runnable() {
					public void run() {
						JFaceResources.getResources().destroyColor(disabledRGB);
					}
				});
			} catch (Exception e) {
			}
		}

		return getEnabled() ? super.getForeground() : disabledColor;
	}

	public boolean isStrikethrough() {
		return strikethrough;
	}

	public void setStrikethrough(boolean strikethrough) {
		this.strikethrough = strikethrough;
	}

}
