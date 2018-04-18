package com.ibm.socialcrm.notesintegration.utils.widgets;

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

import java.beans.PropertyChangeSupport;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;

/**
 * Custom implementation of an ExpandItem. This was implemented because SWTs ExpandItem doesn't handle scrolling real well and it doesn't provide a lot of flexibility with header.
 * 
 * USAGE:
 * 
 * Create an expand bar
 * 
 * SFAExpandBar bar = new SFAExpandBar(composite, SWT.NONE); bar.setLayoutData(GridDataFactory.fillDefaults().create()); //The layout data and layout can be anything
 * bar.setLayout(GridLayoutFactory.fillDefaults().create());
 * 
 * Then create the expand items and
 * 
 * SFAExpandItem item = new SFAExpandItem(bar, SWT.NONE); item.setLayoutData(GridDataFactory.fillDefaults().create());
 * 
 * Create a composite as you normally would but make it's parent item.getControlComposite(). This is very important!
 * 
 * Composite widgetComposite = new Composite(item.getControlComposite(), SWT.NONE);
 * 
 * Add widgets to widgetComposite as you see fit. This is the equivalent of SWT's ExpandItem.setControl()
 * 
 * Finally, call addExpandItem() to register the expand item with it's parent ExpandBar. Expand items should be registered in the order they are created. The ExpandBar needs to have the items
 * registered so it can manage keyboard traversal.
 * 
 * bar.addExpandItem(item);
 * 
 * @author bcbull
 */
public class SFAExpandItem {
	/**
	 * The header image
	 */
	private Image image;

	/**
	 * The header text
	 */
	private String text = ConstantStrings.EMPTY_STRING;

	/**
	 * The expanded state of the item
	 */
	private boolean expanded = false;

	/**
	 * The main composite for this item. It contains the header composite the control composite.
	 */
	private Composite mainComposite;

	/**
	 * The composite that contains the widgets that the user wants to be part of this item
	 */
	private Composite controlComposite;

	/**
	 * Composite that contains the header (image, text, twistie)
	 */
	private Composite headerComposite;

	/**
	 * Used to draw a border around the header composite when the item has focus
	 */
	private Canvas borderCanvas;

	/**
	 * Label that contains the header image
	 */
	private Label imageLabel;

	/**
	 * Label that renders the header text
	 */
	private Label textLabel;

	/**
	 * The color of the label text and twistie
	 */
	private Color headerColor;

	/**
	 * A private instance of the our twistie
	 */
	private ExpandItemTwistie twistie;

	/**
	 * Flag that indicates if the focus border should be drawn around the twistie
	 */
	private boolean drawBorder = false;

	/**
	 * Flag that indicates if the item is enabled. This is only meaningful if the hasEnablementCheckbox flag is set to "true".
	 */
	private boolean itemEnabled = true;

	/**
	 * Used to fire expand events
	 */
	private PropertyChangeSupport propertyChangeSupport;

	/**
	 * Event constant for subscribing to expand events
	 */
	public static final String EXPAND_PROPERTY = "expandProperty"; //$NON-NLS-1$

	/**
	 * Event constant for subscribing to enabled events
	 */
	public static final String ENABLED_PROPERTY = "enabledProperty"; //$NON-NLS-1$

	/**
	 * A handle to our expand bar
	 */
	private SFAExpandBar expandBar;

	/**
	 * Flag that indicates if this expand item has a checkbox that enables/disables the item.
	 */
	private boolean hasEnablementCheckbox = false;

	/**
	 * The checkbox that enables/disables the expand item.
	 */
	private Button enablementCheckbox;

	/**
	 * Creates a new SFAExpandItem. The style widget is passed to an internal composite that is created to hold all of the widgets.
	 * 
	 * @param bar
	 * @param style
	 */
	public SFAExpandItem(SFAExpandBar bar, int style) {
		this(bar, style, false);
	}

	/**
	 * Creates a new SFAExpandItem. The style widget is passed to an internal composite that is created to hold all of the widgets.
	 * 
	 * If the hasEnableCheckbox boolean is true, a checkbox will be added instead of an image label. I could've used SWT.IMAGE in the style flag to do this, but that would be a bit odd since that
	 * style int is passed to an internal composite. Other classes can subscribe to be notified when the checkbox is checked.
	 * 
	 * @param bar
	 * @param style
	 * @param addEnableCheckbox
	 */
	public SFAExpandItem(SFAExpandBar bar, int style, boolean hasEnableCheckbox) {
		expandBar = bar;
		this.hasEnablementCheckbox = hasEnableCheckbox;
		mainComposite = new Composite(bar, style);
		mainComposite.setLayout(GridLayoutFactory.fillDefaults().create());
		mainComposite.setLayoutData(GridDataFactory.fillDefaults().create());

		createHeader();
		createControlComposite();
	}

	/**
	 * Creates the header composite
	 */
	private void createHeader() {
		borderCanvas = new Canvas(mainComposite, hasEnablementCheckbox ? SWT.NO_FOCUS : SWT.NONE) {
			{
				addPaintListener(new PaintListener() {
					LineAttributes attributes = new LineAttributes(1, SWT.CAP_SQUARE, SWT.JOIN_MITER, SWT.LINE_DOT, new float[] { .5f }, SWT.LINE_CUSTOM, 10);

					public void paintControl(PaintEvent e) {
						if (drawBorder) {
							Rectangle rect = getClientArea();
							e.gc.setLineAttributes(attributes);
							e.gc.drawRectangle(rect.x, rect.y, rect.width - 5, rect.height - 1);
						}
					}
				});
			}
		};
		borderCanvas.setLayout(GridLayoutFactory.fillDefaults().margins(5, 2).create());
		borderCanvas.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		headerComposite = new Composite(borderCanvas, hasEnablementCheckbox ? SWT.NO_FOCUS : SWT.NONE);
		headerComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		headerComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).create());

		if (hasEnablementCheckbox) {
			enablementCheckbox = new Button(headerComposite, SWT.CHECK);
			itemEnabled = false;
		} else {
			imageLabel = new Label(headerComposite, SWT.NONE);
			imageLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).create());
		}

		textLabel = new Label(headerComposite, SWT.WRAP);
		textLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		FontData[] data = textLabel.getFont().getFontData();
		String fontName = data[0].getName();
		int height = data[0].getHeight();

		final Font boldFont = new Font(Display.getDefault(), fontName, height, SWT.BOLD);
		final Font normalFont = new Font(Display.getDefault(), fontName, height, SWT.NONE);

		textLabel.setFont(normalFont);

		twistie = new ExpandItemTwistie(headerComposite, SWT.NONE);
		twistie.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).create());

		final Cursor cursor = new Cursor(Display.getDefault(), SWT.CURSOR_HAND);
		final Cursor normalCursor = new Cursor(Display.getDefault(), SWT.CURSOR_ARROW);

		MouseTrackListener mouseTrackListener = new MouseTrackAdapter() {
			public void mouseEnter(MouseEvent e) {
				// Don't bold it if the item is disabled.
				boolean setBold = !hasEnablementCheckbox || enablementCheckbox.getSelection();

				if (setBold) {
					textLabel.setFont(boldFont);
				}

				if (isItemEnabled()) {
					headerComposite.setCursor(cursor);
				} else {
					headerComposite.setCursor(normalCursor);
				}
				twistie.setDrawBorder(true);
				twistie.redraw();
			}

			public void mouseExit(MouseEvent e) {
				textLabel.setFont(normalFont);
				twistie.setDrawBorder(false);
				twistie.redraw();
			}
		};

		headerComposite.addMouseTrackListener(mouseTrackListener);
		textLabel.addMouseTrackListener(mouseTrackListener);
		if (imageLabel != null) {
			imageLabel.addMouseTrackListener(mouseTrackListener);
		}
		if (enablementCheckbox != null) {
			enablementCheckbox.addMouseTrackListener(mouseTrackListener);
		}
		twistie.addMouseTrackListener(mouseTrackListener);

		MouseListener mouseListener = new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				requestFocus();

				if (enablementCheckbox != null) {
					if (isItemEnabled()) {
						toggleExpandState();
					}
				} else {
					toggleExpandState();
				}
			}
		};

		headerComposite.addMouseListener(mouseListener);
		textLabel.addMouseListener(mouseListener);
		if (imageLabel != null) {
			imageLabel.addMouseListener(mouseListener);
		}
		twistie.addMouseListener(mouseListener);

		headerComposite.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.CR || e.character == 32) // No SWT constant for space
				{
					if (enablementCheckbox != null) {
						enablementCheckbox.setSelection(!enablementCheckbox.getSelection());
					}
					toggleExpandState();
				}
			}
		});

		if (enablementCheckbox != null) {
			enablementCheckbox.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setItemEnabled(enablementCheckbox.getSelection());
					setExpanded(itemEnabled);
				}
			});

			// If we have a checkbox, it absorbs focus from the headerComposite so the focus
			// events we rely on to handle border drawing don't work. This listener duplicates
			// what the header composite listeners normally do.
			enablementCheckbox.addFocusListener(new FocusAdapter() {
				public void focusGained(FocusEvent e) {
					drawBorder = true;
					borderCanvas.redraw();
				}

				public void focusLost(FocusEvent e) {
					drawBorder = false;
					borderCanvas.redraw();
				}
			});

			// Delegate traversal events to the expand bar
			enablementCheckbox.addTraverseListener(new TraverseListener() {
				public void keyTraversed(TraverseEvent e) {
					expandBar.traverse(e.detail);
					// Hack! Setting this to false prevents eclipse from resetting the tab focus to the wrong thing
					// after the fall to expandBar.traverse()
					e.doit = false;
				}
			});

			enablementCheckbox.addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent e) {
					if (isItemEnabled()) {
						if (e.keyCode == SWT.ARROW_LEFT || e.keyCode == SWT.ARROW_UP) {
							setExpanded(false);
						} else if (e.keyCode == SWT.ARROW_RIGHT || e.keyCode == SWT.ARROW_DOWN) {
							setExpanded(true);
						}
					}
				}
			});
		}

		// Delegate traversal events to the expand bar
		headerComposite.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				expandBar.traverse(e.detail);
				// Hack! Setting this to false prevents eclipse from resetting the tab focus to the wrong thing
				// after the fall to expandBar.traverse()
				e.doit = false;
			}
		});

		headerComposite.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				drawBorder = true;
				borderCanvas.redraw();
			}

			public void focusLost(FocusEvent e) {
				drawBorder = false;
				borderCanvas.redraw();
			}
		});

		// Dispose of the hand cursor when we're done with it
		mainComposite.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				cursor.dispose();
				boldFont.dispose();
				normalFont.dispose();
				normalCursor.dispose();
			}
		});
	}

	/**
	 * Creates the control composite. This is where the user's widgets reside.
	 */
	private void createControlComposite() {
		if (controlComposite == null) {
			controlComposite = new Composite(mainComposite, SWT.NONE);
			controlComposite.setLayout(GridLayoutFactory.fillDefaults().create());
			controlComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
			updateControlVisibility();
		}
	}

	/**
	 * Sets the layout data for this item
	 * 
	 * @param data
	 */
	public void setLayoutData(Object data) {
		mainComposite.setLayoutData(data);
	}

	/**
	 * Gives this item keyboard focus
	 */
	public void requestFocus() {
		if (enablementCheckbox != null) {
			enablementCheckbox.setFocus();
		} else {
			headerComposite.setFocus();
		}

		drawBorder = true;
		borderCanvas.redraw();
	}

	private void updateControlVisibility() {
		((GridData) controlComposite.getLayoutData()).exclude = !isExpanded();
		controlComposite.setVisible(isExpanded());
		mainComposite.getParent().layout();
		mainComposite.getParent().getParent().layout();

		getPropertyChangeSupport().firePropertyChange(EXPAND_PROPERTY, !isExpanded(), isExpanded());
	}

	/**
	 * Toggles the expand state of the expand item
	 */
	public void toggleExpandState() {
		setExpanded(!isExpanded());
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
		if (imageLabel != null) {
			imageLabel.setImage(image);
			headerComposite.layout(true);
		}
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
		textLabel.setText(text == null ? ConstantStrings.EMPTY_STRING : text);
		// Set the accessible name of the header for a screen reader.
		// CorePlugin.setAccessibleName(headerComposite, text);
	}

	/**
	 * Returns the color of header text/twistie
	 * 
	 * @return
	 */
	public Color getHeaderColor() {
		if (headerColor == null) {
			headerColor = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
		}
		return headerColor;
	}

	/**
	 * Sets the color of the header text/twistie
	 * 
	 * @param headerColor
	 */
	public void setHeaderColor(Color headerColor) {
		if (headerColor != null) {
			this.headerColor = headerColor;
			textLabel.setForeground(headerColor);
			twistie.redraw();
		}
	}

	public boolean isExpanded() {
		return expanded;
	}

	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
		updateControlVisibility();
	}

	/**
	 * If the hasEnablementCheckbox is false, this value will always be true. Otherwise, it follows the selection state of the enablementCheckbox.
	 * 
	 * @return
	 */
	public boolean isItemEnabled() {
		return itemEnabled;
	}

	/**
	 * This setter only works if the hasEnablementCheckbox is true. Otherwise, this has no effect.
	 * 
	 * @param enabled
	 */
	public void setItemEnabled(boolean enabled) {
		if (hasEnablementCheckbox) {
			boolean oldValue = itemEnabled;
			this.itemEnabled = enabled;
			if (enablementCheckbox != null) {
				enablementCheckbox.setSelection(enabled);
				getPropertyChangeSupport().firePropertyChange(ENABLED_PROPERTY, oldValue, enabled);
			}
		}
	}

	/**
	 * Determines if this expand item has focus
	 * 
	 * @return
	 */
	public boolean isFocusItem() {
		return headerComposite.isFocusControl() || (enablementCheckbox != null && enablementCheckbox.isFocusControl());
	}

	/**
	 * 
	 * @return
	 */
	public Composite getControlComposite() {
		return controlComposite;
	}

	public PropertyChangeSupport getPropertyChangeSupport() {
		if (propertyChangeSupport == null) {
			propertyChangeSupport = new PropertyChangeSupport(this);
		}
		return propertyChangeSupport;
	}

	// private class ExpandItemTwistie extends TwistieCanvas
	private class ExpandItemTwistie extends Composite implements PaintListener {
		private boolean drawBorder = false;

		private Point size = new Point(16, 16);

		public ExpandItemTwistie(Composite parent, int style) {
			super(parent, style | SWT.NO_FOCUS);
			addPaintListener(this);
		}

		public Point computeSize(int wHint, int hHint, boolean changed) {
			return size;
		}

		/**
		 * Draws the expand item twistie
		 */
		public void paintControl(PaintEvent e) {
			e.gc.setBackground(getBackground());
			e.gc.fillRectangle(0, 0, size.x, size.y);

			if (itemEnabled) {
				Point center = new Point(size.x / 2, size.y / 2);
				if (size.x > size.y) {
					center.x = size.x - size.y / 2;
				}

				e.gc.setBackground(getForeground());

				// The total height/width of the twistie is horizontalSpan *2/verticalSpan * 2 respectively
				int horizontalSpan = 3;
				int verticalSpan = 3;

				// Vertical spacing between the two arrows in tht twistie
				int arrowDiff = 4;

				e.gc.setAntialias(SWT.ON);
				e.gc.setForeground(getHeaderColor());

				if (isExpanded()) {
					e.gc.drawLine(center.x - horizontalSpan, center.y + verticalSpan, center.x, center.y);
					e.gc.drawLine(center.x - horizontalSpan, center.y + verticalSpan - 1, center.x, center.y - 1);
					e.gc.drawLine(center.x + horizontalSpan, center.y + verticalSpan, center.x, center.y);
					e.gc.drawLine(center.x + horizontalSpan, center.y + verticalSpan - 1, center.x, center.y - 1);

					e.gc.drawLine(center.x - horizontalSpan, center.y + verticalSpan - arrowDiff, center.x, center.y - arrowDiff);
					e.gc.drawLine(center.x - horizontalSpan, center.y + verticalSpan - arrowDiff - 1, center.x, center.y - arrowDiff - 1);
					e.gc.drawLine(center.x + horizontalSpan, center.y + verticalSpan - arrowDiff, center.x, center.y - arrowDiff);
					e.gc.drawLine(center.x + horizontalSpan, center.y + verticalSpan - arrowDiff - 1, center.x, center.y - arrowDiff - 1);
				} else {
					e.gc.drawLine(center.x - horizontalSpan, center.y - verticalSpan, center.x, center.y);
					e.gc.drawLine(center.x - horizontalSpan, center.y - verticalSpan - 1, center.x, center.y - 1);
					e.gc.drawLine(center.x + horizontalSpan, center.y - verticalSpan, center.x, center.y);
					e.gc.drawLine(center.x + horizontalSpan, center.y - verticalSpan - 1, center.x, center.y - 1);

					e.gc.drawLine(center.x - horizontalSpan, center.y + 1, center.x, center.y + arrowDiff);
					e.gc.drawLine(center.x - horizontalSpan, center.y, center.x, center.y + arrowDiff - 1);
					e.gc.drawLine(center.x + horizontalSpan, center.y + 1, center.x, center.y + arrowDiff);
					e.gc.drawLine(center.x + horizontalSpan, center.y, center.x, center.y + arrowDiff - 1);
				}

				if (drawBorder) {
					int x = size.x - 1;
					int y = size.y - 1;

					e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
					e.gc.drawLine(1, 0, 1, y);
					e.gc.drawLine(1, 0, x, 0);
					e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
					e.gc.drawLine(x, 0, x, y);
					e.gc.drawLine(x, y, 1, x);
				}
			}
		}

		public void setDrawBorder(boolean drawBorder) {
			this.drawBorder = drawBorder;
		}
	}

	// Just makes it easier to deal with these in the debugger
	@Override
	public String toString() {
		return getText();
	}

	public void setBackground(Color color) {
		twistie.setBackground(color);
		mainComposite.setBackground(color);
		headerComposite.setBackground(color);
		borderCanvas.setBackground(color);
		textLabel.setBackground(color);
	}

}
