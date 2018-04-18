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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;

/*
 * Customized Button widget.  It appends an "X" to the button text, and it implements
 * a click listener which executes the action passed into this widget via constructor.
 */
public class SFAButtonWithX {

	private static final String REMOVE_TEXT = "X"; //$NON-NLS-1$

	private Composite _parent = null;
	private Composite _widgetComposite = null;
	private ImageHyperlink _button;
	private Action _action;

	private String sfaButtonUnpressedText = "sfaButtonUnpressedText"; //$NON-NLS-1$
	private String sfaButtonUnpressedX = "sfaButtonUnpressedX"; //$NON-NLS-1$
	private String sfaButtonBorder = "sfaButtonBorder"; //$NON-NLS-1$
	//  private String sfaXFont = "sfaXFont"; //$NON-NLS-1$

	private PaintListener _paintListener;
	private IHyperlinkListener _hyperlinkListener;
	private MouseListener _mouseListener;
	private KeyListener _keyListener;
	private MouseTrackListener _mouseTrackListener;

	/**
	 * Indicates if the button is being selected in the UI via mouse click or keyboard selection
	 */
	private boolean _beingSelectedInUi = false;

	/**
	 * Indicates if the mouse is hovering over the button
	 */
	private boolean mouseOver = false;

	private boolean _isHighContrast = false;

	public SFAButtonWithX(Composite parent, int style, String textX, String toolTipText, Action action) {

		_parent = parent;

		initColors();

		// Set parent composite with border, and do not paint border in button paintlistener
		// to make button look more roomy.
		_widgetComposite = new Composite(parent, SWT.NO);
		_widgetComposite.setSize(SWT.DEFAULT + 230, SWT.DEFAULT + 230);
		RowLayout layout = new RowLayout();
		layout.wrap = false;
		_widgetComposite.setLayout(layout);
		_widgetComposite.setBackground(parent.getBackground());

		_button = new ImageHyperlink(_widgetComposite, SWT.TOP);

		_button.setBackground(parent.getBackground());
		_button.setText(getButtonText(textX));
		_button.setLayoutData(new RowData());

		_button.pack();

		_button.setToolTipText(toolTipText);

		// use paint listener to paint text in the center and paint "x" in a lighter gray
		_button.addPaintListener(getPaintListener());

		_button.addHyperlinkListener(getHyperlinkListener());

		_button.addMouseListener(getMouseListener());

		_button.addKeyListener(getKeyListener());

		_button.addMouseTrackListener(getMouseTrackListener());

		_widgetComposite.pack();

		_action = action;

	}

	private MouseTrackListener getMouseTrackListener() {
		if (_mouseTrackListener == null) {
			_mouseTrackListener = new MouseTrackAdapter() {
				@Override
				public void mouseEnter(MouseEvent arg0) {
					mouseOver = true;
					_button.redraw();
				}

				@Override
				public void mouseExit(MouseEvent arg0) {
					mouseOver = false;
					_button.redraw();
				}
			};
		}
		return _mouseTrackListener;
	}

	private KeyListener getKeyListener() {

		if (_keyListener == null) {
			_keyListener = new KeyListener() {
				@Override
				public void keyPressed(KeyEvent arg0) {
					_beingSelectedInUi = true;
				}

				@Override
				public void keyReleased(KeyEvent arg0) {
					_beingSelectedInUi = false;
				}
			};
		}
		return _keyListener;
	}

	private MouseListener getMouseListener() {

		if (_mouseListener == null) {
			_mouseListener = new MouseAdapter() {
				@Override
				public void mouseDown(MouseEvent arg0) {
					_beingSelectedInUi = true;
				}

				@Override
				public void mouseUp(MouseEvent arg0) {
					_beingSelectedInUi = false;
				}
			};
		}
		return _mouseListener;
	}

	private IHyperlinkListener getHyperlinkListener() {
		if (_hyperlinkListener == null) {
			_hyperlinkListener = new HyperlinkAdapter()

			{
				@Override
				public void linkActivated(HyperlinkEvent arg0) {

					if (_action != null) {
						_action.run();
					}
				}
			};
		}
		return _hyperlinkListener;
	}

	private PaintListener getPaintListener() {
		if (_paintListener == null) {
			_paintListener = new PaintListener() {
				@Override
				public void paintControl(PaintEvent e) {
					GC gc = e.gc;

					if (getControl() != null) {
						Rectangle rect = getControl().getBounds();
						int width = rect.width;
						int height = rect.height;

						// fill control area
						gc.fillRectangle(1, 1, width - 1, height - 1);
						gc.setForeground(getBorderColor());
						gc.drawRoundRectangle(0, 0, width - 1, height - 1, 3, 3);

						// Set font to 1 size smaller (set in GC, does not affect control's font)
						FontData fd = getControl().getFont().getFontData()[0];
						fd.setHeight(fd.getHeight() - 1);
						Font smallerFont = new Font(getControl().getDisplay(), fd);
						gc.setFont(smallerFont);
						
						// Center the text
						Point pt = gc.stringExtent(getText());
						int xx = (width - pt.x) / 2;
						int yy = (height - pt.y) / 2;
						
						int parentSize = getParent().getSize().x;
						int textSize = gc.stringExtent(getText()).x;
												
						String displayText = getText();
						//If the text is too large, truncate it
						if (textSize > parentSize)
						{							
							int averageWidth = textSize / getText().length();
						    int availableCharacterWidth = (parentSize - 30) /averageWidth;
						    
						    String begin = getText().substring(0, (availableCharacterWidth / 2) -3);
						    String end = getText().substring(getText().length() - (availableCharacterWidth / 2) -3);
						    displayText = begin + "..." + end; //$NON-NLS-1$
						    displayText = getButtonText(displayText);							
						}

						// Get "X" location
						Point pt_x = gc.stringExtent(displayText.substring(0, displayText.length() - 1));

						// set color to the button text
						gc.setForeground(isPressed() ? getPressedTextColor() : getUnpressedTextColor());
						gc.drawString(displayText.substring(0, displayText.length() - 2), xx, yy, true);
						// set different color to "x"
						gc.setForeground(isPressed() ? getPressedXColor() : getUnpressedXColor());
						gc.setAlpha(getOpacity());
						gc.drawString(displayText.substring(displayText.length() - 2), xx + (pt_x.x) - 1, yy, true);
						smallerFont.dispose();
					}
				}
			};
		}
		return _paintListener;
	}

	private boolean isPressed() {
		if (_beingSelectedInUi || mouseOver) {
			return true;
		} else {
			return false;
		}
	}

	private Control getControl() {
		return _button;
	}

	public Color getBackgroundColor() {
		return _parent.getBackground();
	}

	public Color getForegroundColor() {
		return _parent.getForeground();
	}

	private int getOpacity() {
		double factor = 0.45;
		if (isPressed()) {
			factor = 0.55;
		}
		double d = 255 * factor;
		int i = (int) d;
		return i;
	}

	public Color getBorderColor() {
		return JFaceResources.getColorRegistry().get(sfaButtonBorder);
	}

	public Color getUnpressedTextColor() {
		return JFaceResources.getColorRegistry().get(sfaButtonUnpressedText);
	}

	public Color getUnpressedXColor() {
		return JFaceResources.getColorRegistry().get(sfaButtonUnpressedX);
	}

	public Color getPressedTextColor() {
		return getLighterOrDarker(JFaceResources.getColorRegistry().get(sfaButtonUnpressedText), 0.50F);
	}

	public Color getPressedXColor() {
		/* return getLighterOrDarker(JFaceResources.getColorRegistry().get(sfaButtonUnpressedX), 0.50F); */
		return getUnpressedXColor();
	}

	private String getButtonText(String s) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		sb.append((s == null) ? ConstantStrings.EMPTY_STRING : s);
		if (!s.endsWith("  " + REMOVE_TEXT)) //$NON-NLS-1$
		{
			sb.append("  " + REMOVE_TEXT); //$NON-NLS-1$
		}	
		return sb.toString();
	}

	private String getText() {
		String text = ConstantStrings.EMPTY_STRING;
		if (_button != null && !_button.isDisposed()) {
			text = _button.getText();
		}
		return text;
	}

	private void initColors() {
		_isHighContrast = Display.getDefault().getHighContrast();

		if (_isHighContrast) {
			JFaceResources.getColorRegistry().put(sfaButtonUnpressedText, Display.getDefault().getSystemColor(SWT.COLOR_BLACK).getRGB());
		} else {
			JFaceResources.getColorRegistry().put(sfaButtonUnpressedText, new RGB(00, 100, 157)); /* navy blue */
		}

		if (_isHighContrast) {
			JFaceResources.getColorRegistry().put(sfaButtonUnpressedX, Display.getDefault().getSystemColor(SWT.COLOR_GRAY).getRGB());
		} else {
			JFaceResources.getColorRegistry().put(sfaButtonUnpressedX, new RGB(0, 0, 0)); /* black */
		}

		if (_isHighContrast) {
			JFaceResources.getColorRegistry().put(sfaButtonBorder, Display.getDefault().getSystemColor(SWT.COLOR_GRAY).getRGB());
		} else {
			JFaceResources.getColorRegistry().put(sfaButtonBorder, new RGB(204, 204, 204)); /* gray */
		}
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

	public void dispose() {
		if (_button != null && !_button.isDisposed()) {
			_button.removePaintListener(getPaintListener());
			_button.removeHyperlinkListener(getHyperlinkListener());
			_button.removeMouseListener(getMouseListener());
			_button.removeMouseTrackListener(getMouseTrackListener());
			_button.removeKeyListener(getKeyListener());
		}
		_widgetComposite.dispose();
	}

	public Composite getParent() {
		return _parent;
	}

	public void setData(Object obj) {
		_widgetComposite.setData(obj);
	}

	public void setData(String key, Object obj) {
		_widgetComposite.setData(key, obj);
	}

	public Object getData() {
		return _widgetComposite.getData();
	}

	public Object getData(String key) {
		return _widgetComposite.getData(key);
	}
	
	public void addControlListener(ControlListener listener)
	{
		_widgetComposite.addControlListener(listener);
	}
	
	public void redraw()
	{
		_button.redraw();
	}
}
