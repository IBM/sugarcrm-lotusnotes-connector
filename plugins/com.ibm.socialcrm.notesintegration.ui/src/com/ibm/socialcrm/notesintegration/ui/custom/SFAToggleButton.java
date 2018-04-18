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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
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

import com.ibm.rcp.swt.swidgets.SButton;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;

public class SFAToggleButton {
	private SButton button;

	private PaintListener paintListener;

	private String sfaToggleUnpressedEnabledBackground = "sfaToggleUnpressedEnabledBackground"; //$NON-NLS-1$
	private String sfaTogglePressedEnabledBackground = "sfaTogglePressedEnabledBackground";//$NON-NLS-1$
	private String sfaToggleUnpressedDisabledBackground = "sfaToggleUnpressedDisabledBackground";//$NON-NLS-1$
	private String sfaTogglePressedDisabledBackground = "sfaTogglePressedDisabledBackground";//$NON-NLS-1$

	private String sfaToggleUnpressedEnabledText = "sfaToggleUnpressedEnabledText";//$NON-NLS-1$
	private String sfaTogglePressedEnabledText = "sfaTogglePressedEnabledText";//$NON-NLS-1$
	private String sfaToggleUnpressedDisabledText = "sfaToggleUnpressedDisabledText";//$NON-NLS-1$
	private String sfaTogglePressedDisabledText = "sfaTogglePressedDisabledText";//$NON-NLS-1$

	private String sfaToggleUnpressedForeground = "sfaToggleUnpressedForeground"; //$NON-NLS-1$
	private String sfaTogglePressedForeground = "sfaTogglePressedForeground"; //$NON-NLS-1$

	private String sfaToggleHighContrastBackground = "sfaToggleHighContrastBackground"; //$NON-NLS-1$
	private String sfaToggleHighContrastForeground = "sfaToggleHighContrastForeground"; //$NON-NLS-1$
	private String sfaToggleHighContrastDisableBackground = "sfaToggleHighContrastDisableBackground"; //$NON-NLS-1$
	private String sfaToggleHighContrastDisableForeground = "sfaToggleHighContrastDisableForeground"; //$NON-NLS-1$

	private String sfaToggleEnabledBorder = "sfaToggleEnabledBorder"; //$NON-NLS-1$
	private String sfaToggleDisabledBorder = "sfaToggleDisabledBorder"; //$NON-NLS-1$

	private Color unpressedEnabledBackgroundColor = null;
	private Color pressedEnabledBackgroundColor = null;
	private Color unpressedDisabledBackgroundColor = null;
	private Color pressedDisabledBackgroundColor = null;

	private Color unpressedEnabledTextColor = null;
	private Color unpressedDisabledTextColor = null;
	private Color pressedEnabledTextColor = null;
	private Color pressedDisabledTextColor = null;

	private Color unpressedForegroundColor = null;
	private Color pressedForegroundColor = null;

	private Color highContrastBackgroundColor = null;
	private Color highContrastForegroundColor = null;
	private Color highContrastDisableBackgroundColor = null;
	private Color highContrastDisableForegroundColor = null;

	private Color enabledBorderColor = null;
	private Color disabledBorderColor = null;

	private boolean _isHighContrast = false;

	private String text = null;
	// The foreground and background are used to draw the gradients
	private Color backgroundColor = null;
	private Color foregroundColor = null;
	private Color textColor = null;
	private Font font;

	private boolean pressed = false;

	/**
	 * Indicates if the button is being selected in the UI via mouse click or keyboard selection
	 */
	private boolean beingSelectedInUi = false;

	/**
	 * Indicates if the mouse is hovering over the button
	 */
	private boolean mouseOver = false;

	public SFAToggleButton(Composite parent, int style) {
		button = new SButton(parent, style);
		initializeColors();
		setEnabled(true);
		button.addPaintListener(getPaintListener());

		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent arg0) {
				beingSelectedInUi = true;
			}

			@Override
			public void mouseUp(MouseEvent arg0) {
				beingSelectedInUi = false;
			}
		});

		button.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent arg0) {
				beingSelectedInUi = true;
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
				beingSelectedInUi = false;
			}
		});

		button.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseEnter(MouseEvent arg0) {
				mouseOver = true;
				button.redraw();
			}

			@Override
			public void mouseExit(MouseEvent arg0) {
				mouseOver = false;
				button.redraw();
			}
		});

	}

	private PaintListener getPaintListener() {
		if (paintListener == null) {
			paintListener = new PaintListener() {
				@Override
				public void paintControl(PaintEvent e) {
					GC gc = e.gc;

					if (button != null) {
						Rectangle rect = button.getBounds();
						int width = rect.width;
						int height = rect.height;

						// Fill the button background
						gc.setForeground(getForegroundColor()); // Set the standard foreground color
						if (mouseOver && !beingSelectedInUi) {
							gc.setBackground(getLighterOrDarker(getBackgroundColor(), 1.10F));
							// If the button is being hovered over, change the foreground so that the fillGradientCall below
							// just makes the button lighter
							gc.setForeground(gc.getBackground());
						} else {
							gc.setBackground(getBackgroundColor());
						}

						if (_isHighContrast) {
							// If high contrast, draw a plain rectangle, should not be fancy.
							gc.fillRectangle(1, 1, width - 2, height - 2);
						} else {
							// This gradient is drawn vertically based on the foreground and background colors
							gc.fillGradientRectangle(1, 1, width - 2, height - 2, true);
						}

						// Draw the button border
						gc.setForeground(getBorderColor());
						gc.setLineWidth(1);
						gc.drawRoundRectangle(0, 0, rect.width - 1, rect.height - 1, 7, 7);

						gc.setForeground(getTextColor());
						gc.setFont(getFont());

						// Center the text
						Point pt = gc.stringExtent(getText());
						int x = (width - pt.x) / 2;
						int y = ((height - pt.y) / 2);

						gc.drawString(getText(), x, y, true);

						// Draw a dotted line if it's the focus control
						if (button.isFocusControl()) {
							LineAttributes lineAttributes = new LineAttributes(1, SWT.CAP_ROUND, SWT.JOIN_ROUND, SWT.LINE_DOT, new float[] { 1.0f }, 1.0f, 1.0f);
							gc.setLineAttributes(lineAttributes);
							gc.drawRoundRectangle(2, 2, rect.width - 5, rect.height - 5, 3, 3);
						}
					}
				}
			};
		}
		return paintListener;
	}

	/**
	 * return a color lighter or darker than the input c by the given ratio
	 **/
	public Color getLighterOrDarker(Color c, float ratio) {
		int r = Math.min(255, (int) (c.getRed() * ratio));
		int g = Math.min(255, (int) (c.getGreen() * ratio));
		int b = Math.min(255, (int) (c.getBlue() * ratio));

		RGB rgb = new RGB(r, g, b);
		if (JFaceResources.getColorRegistry().get(rgb.toString()) == null) {
			JFaceResources.getColorRegistry().put(rgb.toString(), rgb);
		}
		return JFaceResources.getColorRegistry().get(rgb.toString());
	}

	public SButton getButton() {
		return button;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		// Calling the real set text so that the button can properly compute it's size
		button.setText(text);

		this.text = text;
		button.redraw();
	}

	public Font getFont() {
		if (font == null) {
			font = SugarItemsDashboard.getInstance().getBusinessCardLabelFont();
			button.setFont(font); // Again, doing this so it can compute it's size properly
		}
		return font;
	}

	public void setFont(Font font) {
		button.setFont(font);
		this.font = font;
	}

	public void setBackgroundColor(Color bgColor) {
		backgroundColor = bgColor;
	}

	public Color getBackgroundColor() {
		Color color = backgroundColor;
		if (_isHighContrast) {
			if (button.isEnabled()) {
				color = highContrastBackgroundColor;
			} else {
				color = highContrastDisableBackgroundColor;
			}
		}
		return color;
	}

	public Color getForegroundColor() {
		Color color = foregroundColor;
		if (_isHighContrast) {
			if (button.isEnabled()) {
				color = highContrastForegroundColor;
			} else {
				color = highContrastDisableForegroundColor;
			}
		}
		return color;
	}

	public void setForegroundColor(Color foregroundColor) {
		this.foregroundColor = foregroundColor;
	}

	public void setTextColor(Color textColor) {
		this.textColor = textColor;
	}

	public Color getTextColor() {
		Color color = textColor;
		if (_isHighContrast) {
			if (button.isEnabled()) {
				color = highContrastForegroundColor;
			} else {
				color = highContrastDisableForegroundColor;
			}
		}
		return color;
	}

	private Color getBorderColor() {
		Color color = getTextColor();
		if (!_isHighContrast) {
			if (button.isEnabled()) {
				color = enabledBorderColor;
			} else {
				color = disabledBorderColor;
			}
		}
		return color;
	}

	public boolean isPressed() {
		return pressed;
	}

	public void setPressed(boolean pressed) {
		this.pressed = pressed;
		resetColorsBasedOnState();
		button.redraw();
	}

	public void setEnabled(boolean enabled) {
		button.setEnabled(enabled);
		resetColorsBasedOnState();
		button.redraw();
	}

	private void resetColorsBasedOnState() {
		boolean enabled = button.isEnabled();
		if (isPressed()) {
			setBackgroundColor(enabled ? pressedEnabledBackgroundColor : pressedDisabledBackgroundColor);
			setTextColor(enabled ? pressedEnabledTextColor : pressedDisabledTextColor);
			setForegroundColor(pressedForegroundColor);
		} else {
			setBackgroundColor(enabled ? unpressedEnabledBackgroundColor : unpressedDisabledBackgroundColor);
			setTextColor(enabled ? unpressedEnabledTextColor : unpressedDisabledTextColor);
			setForegroundColor(unpressedForegroundColor);
		}
	}

	private void initializeColors() {
		JFaceResources.getColorRegistry().put(sfaToggleUnpressedEnabledBackground, new RGB(219, 225, 236));
		JFaceResources.getColorRegistry().put(sfaTogglePressedEnabledBackground, new RGB(209, 209, 209));
		JFaceResources.getColorRegistry().put(sfaToggleUnpressedDisabledBackground, new RGB(233, 233, 233));
		JFaceResources.getColorRegistry().put(sfaTogglePressedDisabledBackground, new RGB(233, 233, 233));

		JFaceResources.getColorRegistry().put(sfaToggleUnpressedEnabledText, new RGB(64, 96, 112));
		JFaceResources.getColorRegistry().put(sfaTogglePressedEnabledText, new RGB(88, 88, 88));
		JFaceResources.getColorRegistry().put(sfaToggleUnpressedDisabledText, new RGB(155, 155, 155));
		JFaceResources.getColorRegistry().put(sfaTogglePressedDisabledText, new RGB(155, 155, 155));

		JFaceResources.getColorRegistry().put(sfaToggleUnpressedForeground, new RGB(232, 235, 242));
		JFaceResources.getColorRegistry().put(sfaTogglePressedForeground, new RGB(196, 196, 196));

		JFaceResources.getColorRegistry().put(sfaToggleEnabledBorder, new RGB(128, 145, 165));
		JFaceResources.getColorRegistry().put(sfaToggleDisabledBorder, new RGB(187, 190, 194));

		JFaceResources.getColorRegistry().put(sfaToggleHighContrastBackground, Display.getDefault().getSystemColor(SWT.COLOR_BLACK).getRGB());
		JFaceResources.getColorRegistry().put(sfaToggleHighContrastForeground, Display.getDefault().getSystemColor(SWT.COLOR_WHITE).getRGB());
		JFaceResources.getColorRegistry().put(sfaToggleHighContrastDisableBackground, Display.getDefault().getSystemColor(SWT.COLOR_GRAY).getRGB());
		JFaceResources.getColorRegistry().put(sfaToggleHighContrastDisableForeground, Display.getDefault().getSystemColor(SWT.COLOR_WHITE).getRGB());

		unpressedEnabledBackgroundColor = JFaceResources.getColorRegistry().get(sfaToggleUnpressedEnabledBackground);
		pressedEnabledBackgroundColor = JFaceResources.getColorRegistry().get(sfaTogglePressedEnabledBackground);
		unpressedDisabledBackgroundColor = JFaceResources.getColorRegistry().get(sfaToggleUnpressedDisabledBackground);
		pressedDisabledBackgroundColor = JFaceResources.getColorRegistry().get(sfaTogglePressedDisabledBackground);

		unpressedEnabledTextColor = JFaceResources.getColorRegistry().get(sfaToggleUnpressedEnabledText);
		pressedEnabledTextColor = JFaceResources.getColorRegistry().get(sfaTogglePressedEnabledText);
		unpressedDisabledTextColor = JFaceResources.getColorRegistry().get(sfaToggleUnpressedDisabledText);
		pressedDisabledTextColor = JFaceResources.getColorRegistry().get(sfaTogglePressedDisabledText);

		unpressedForegroundColor = JFaceResources.getColorRegistry().get(sfaToggleUnpressedForeground);
		pressedForegroundColor = JFaceResources.getColorRegistry().get(sfaTogglePressedForeground);

		highContrastBackgroundColor = JFaceResources.getColorRegistry().get(sfaToggleHighContrastBackground);
		highContrastForegroundColor = JFaceResources.getColorRegistry().get(sfaToggleHighContrastForeground);
		highContrastDisableBackgroundColor = JFaceResources.getColorRegistry().get(sfaToggleHighContrastDisableBackground);
		highContrastDisableForegroundColor = JFaceResources.getColorRegistry().get(sfaToggleHighContrastDisableForeground);

		enabledBorderColor = JFaceResources.getColorRegistry().get(sfaToggleEnabledBorder);
		disabledBorderColor = JFaceResources.getColorRegistry().get(sfaToggleDisabledBorder);

		_isHighContrast = Display.getDefault().getHighContrast();
	}

}
