package com.ibm.socialcrm.notesintegration.sidebar.composites;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.files.FilesPluginActivator;
import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.utils.CardSummary;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFACombo;

public class RecentlyViewCardsComposite extends Composite {

	private Button myItemsButton;

	private Composite searchComposite;
	private SFACombo categoryCombo;

	private Label cityLabel;
	private Text cityText;

	private SFACombo itemCombo;

	private Composite reloadComposite;
	private Label reloadLabel;
	private Composite loadingComposite;
	private Composite tableComposite;

	private Label recentlyViewedLabel;
	private TableViewer viewer;

	private Font italicsFont;
	private Font normalFont;

	// 43387 - pin and remove actions
	private int fontHeight = 0;
	private Shell tip = null;
	private StyledText styledText = null;
	private StyledText actionStyledText = null;
	// private Label label = null;
	private Image toPinImage;
	private Image toUnPinImage;
	private Image toRemoveImage;
	private Image pinnedImage;
	private String recentlyViewCardsHoverForeGround = "recentlyViewCardsHoverForeGround"; //$NON-NLS-1$
	private int iGapHeight = 4;
	private int iImageGap = 1;
	private final static int UPPER_HALF = 1;
	private final static int LOWER_HALF = -1;

	private boolean doingTypeaheadLookup = false; // Indicates if a web service call to the typeahead service is currently in progress
	private long lastKeystrokeTime = 0; // Used to maintain a 1/2 delay before firing the typeahead calls

	private Map<String, SugarType> sugarTypeMap = new HashMap<String, SugarType>() {
		{
			put(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CONTACT), SugarType.CONTACTS);
			put(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_OPPORTUNITY), SugarType.OPPORTUNITIES);
			put(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CLIENT), SugarType.ACCOUNTS);
		}
	};

	public RecentlyViewCardsComposite(Composite parent) {
		super(parent, SWT.NONE);

		JFaceResources.getColorRegistry().put(recentlyViewCardsHoverForeGround, new RGB(61, 61, 61));

		// If we haven't been able to load the previously viewed cards, go ahead and fire that off.
		if (!SugarItemsDashboard.getInstance().previouslyViewedCardsLoaded && !SugarItemsDashboard.getInstance().loadingCards) {
			SugarItemsDashboard.getInstance().loadPreviouslyViewedCards();
		}

		setLayout(GridLayoutFactory.fillDefaults().margins(5, 5).create());

		createTitleLabel();
		createSearchComposite();

		Label separator = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		createRecentlyViewedPart();

		UiUtils.recursiveSetBackgroundColor(this, JFaceColors.getBannerBackground(Display.getDefault()));

		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent evt) {
				getItalicsFont().dispose();
				getNormalFont().dispose();
			}
		});

		categoryCombo.select(0);
	}

	private void createTitleLabel() {
		Label titleLabel = new Label(this, SWT.WRAP);
		titleLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		titleLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_SIDEBAR_TITLE));
	}

	private void createSearchComposite() {
		searchComposite = new Composite(this, SWT.NONE);
		searchComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
		searchComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		new Label(searchComposite, SWT.NONE); // spacer label

		myItemsButton = new Button(searchComposite, SWT.CHECK);
		myItemsButton.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FILE_UPLOAD_MY_ITEMS));
		myItemsButton.setLayoutData(GridDataFactory.fillDefaults().create());
		myItemsButton.setSelection(true);

		createCategorySection(searchComposite);
		createCitySection(searchComposite);
		createItemSection(searchComposite);
	}

	/**
	 * Creates the widgets to let the user choose what type of card they want to search for
	 * 
	 * @param searchComposite
	 */
	private void createCategorySection(Composite searchComposite) {
		Label categoryLabel = new Label(searchComposite, SWT.NONE);
		categoryLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FIND_BY_LABEL_STRING));

		categoryCombo = new SFACombo(searchComposite, SWT.BORDER | SWT.READ_ONLY);
		categoryCombo.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		Vector<String> v = new Vector<String>();
		v.add(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CONTACT));
		v.add(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_OPPORTUNITY));
		v.add(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CLIENT));

		categoryCombo.setItems(v.toArray(new String[v.size()]));
		categoryCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent evt) {
				boolean enableCityParts = categoryCombo.getText().equals(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CLIENT));
				cityLabel.setEnabled(enableCityParts);
				cityText.setEnabled(enableCityParts);

				if (enableCityParts && cityText.getText().equalsIgnoreCase(ConstantStrings.EMPTY_STRING)) {
					setCityDefaultText();
				}
				setItemDefaultText(true);
			}
		});
	}

	/**
	 * Select the city associated with the card
	 * 
	 * @param selectionWidgetComposite
	 */
	private void createCitySection(Composite selectionWidgetComposite) {
		cityLabel = new Label(selectionWidgetComposite, SWT.NONE);
		cityLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.FILE_UPLOAD_CITY));
		cityLabel.setEnabled(false);

		cityText = new Text(selectionWidgetComposite, SWT.BORDER);
		cityText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		cityText.setEnabled(false);

		cityText.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent evt) {
				if (cityText.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CITY_HELP_TEXT))) {
					unsetCityDefaultText();
				}
			}

			@Override
			public void focusLost(FocusEvent evt) {
				if (cityText.getText().equalsIgnoreCase(ConstantStrings.EMPTY_STRING)) {
					setCityDefaultText();
				}
			}
		});
	}

	/**
	 * Create the item selection composite
	 */
	private void createItemSection(Composite selectionWidgetComposite) {
		Label itemLabel = new Label(selectionWidgetComposite, SWT.NONE);
		itemLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOOK_UP_LABEL_STRING));

		itemCombo = new SFACombo(selectionWidgetComposite, SWT.DROP_DOWN | SWT.BORDER);
		itemCombo.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		itemCombo.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent evt) {
				if (itemCombo.getText().equalsIgnoreCase(getItemComboDefaultText())) {
					unsetItemDefaultText();
				}
			}

			@Override
			public void focusLost(FocusEvent evt) {
				if (itemCombo.getText().equalsIgnoreCase(ConstantStrings.EMPTY_STRING)) {
					setItemDefaultText(true);
				}
			}
		});

		itemCombo.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent evt) {
				// If the user presses escape, clear anything in the combo
				if (evt.keyCode == SWT.ESC) {
					itemCombo.removeAll();
					itemCombo.setFocus();
				} else if (evt.keyCode == SWT.CR) {
					showSelectedCardInDropdown();
				} else if (evt.keyCode != SWT.ARROW_UP && evt.keyCode != SWT.ARROW_DOWN) {
					typeaheadSuggestionAction();
				}
			}
		});

		itemCombo.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent arg0) {

				String txt = itemCombo.getText();
				String dropdownselected = null;
				if (itemCombo.getSelectionIndex() > -1) {
					dropdownselected = itemCombo.getItems()[itemCombo.getSelectionIndex()];
				}

				if (txt != null && !txt.equals(dropdownselected)) {
					typeaheadSuggestionAction();
				}

			}

		});

		itemCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				if (!itemCombo.getListVisible()) {
					showSelectedCardInDropdown();
				}
			}
		});
	}

	private void createRecentlyViewedPart() {

		reloadComposite = new Composite(this, SWT.NONE);
		reloadComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		reloadComposite.setLayout(GridLayoutFactory.fillDefaults().create());

		reloadLabel = new Label(reloadComposite, SWT.WRAP);
		reloadLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		Button reloadButton = new Button(reloadComposite, SWT.PUSH);

		reloadButton.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_SIDEBAR_RELOAD));
		reloadButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				SugarItemsDashboard.getInstance().loadPreviouslyViewedCards();
			}
		});

		// If the loading process is in flight, show a loading message
		loadingComposite = new Composite(this, SWT.NONE);
		loadingComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		loadingComposite.setLayout(GridLayoutFactory.fillDefaults().create());

		Label loadingLabel = new Label(loadingComposite, SWT.WRAP);
		loadingLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		loadingLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_SIDEBAR_LOADING_RECENT));

		ProgressBar progress = new ProgressBar(loadingComposite, SWT.INDETERMINATE);
		progress.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

		tableComposite = new Composite(this, SWT.NONE);
		tableComposite.setLayout(GridLayoutFactory.fillDefaults().create());
		tableComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		recentlyViewedLabel = new Label(tableComposite, SWT.NONE);
		recentlyViewedLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_RECENTLY_VIEWED_CARDS));

		// Using SWT.VIRTUAL seems fixed the problem of trailing of the ToRemove images when scroll
		viewer = new TableViewer(tableComposite, SWT.VIRTUAL | SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);

		Table table = viewer.getTable();
		table.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

		updateRecentlyViewedParts();

		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setInput(SugarItemsDashboard.getInstance().getPreviouslyViewedCards().toArray());

		// table item sorts by pinned indicator + timestamp ( descending order)
		viewer.setSorter(new ViewerSorter() {
			@Override
			public int compare(Viewer viewr, Object obj1, Object obj2) {
				if (obj1 instanceof CardSummary && obj2 instanceof CardSummary) {
					int result = (((CardSummary) obj2).isPinned()).compareTo((((CardSummary) obj1).isPinned()));
					if (result != 0) {
						return result;
					}
					return (((CardSummary) obj2).getTimestampMillis()).compareTo((((CardSummary) obj1).getTimestampMillis()));

				}
				return -1;
			}
		});
		if (SugarItemsDashboard.getInstance().getPreviouslyViewedCards().size() > 0) {
			viewer.getTable().setSelection(0);
		}
		viewer.getTable().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent evt) {
				CardSummary entry = (CardSummary) viewer.getTable().getSelection()[0].getData();
				UiUtils.displaySugarItemById13(entry.getType(), entry.getId(), entry.getName(), new NullProgressMonitor());
			}
		});

		// mainly for detecting which image (pin/unpin or remove) user pressed
		viewer.getTable().addMouseListener(new MouseListener() {

			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseDown(MouseEvent arg0) {

			}

			@Override
			public void mouseUp(MouseEvent arg0) {
				Point cursorPt = Display.getCurrent().getCursorLocation();
				CardSummary summary = getMouseoverSummary(arg0, cursorPt);
				if (summary != null) {
					boolean isContinue = true;

					// check if to pin / to unpin image was pushed
					if (summary.isPinned().intValue() == CardSummary.IS_NOT_PINNED) {
						if (imageFocused(toPinImage, summary, 0) != null) {
							isContinue = false;
							executeToPinAction(summary);
						}
					} else {
						if (imageFocused(toUnPinImage, summary, 1) != null) {
							isContinue = false;
							executeToPinAction(summary);
						}
					}
					if (isContinue) {
						// check if to remove image was pushed
						if (imageFocused(toRemoveImage, summary, 2) != null) {
							executeToRemoveAction(summary);
						}
					}

				}

			}

		});

		viewer.getTable().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent evt) {
				if (evt.keyCode == SWT.CR) {
					CardSummary entry = (CardSummary) viewer.getTable().getSelection()[0].getData();
					UiUtils.displaySugarItemById(entry.getType(), entry.getId(), new NullProgressMonitor());
				}
			}
		});

		final Display display = viewer.getTable().getDisplay();
		final TextLayout textLayout = new TextLayout(display);

		pinnedImage = SFAImageManager.getImage(SFAImageManager.PINNED);
		toRemoveImage = SFAImageManager.getImage(SFAImageManager.REMOVE);
		toPinImage = SFAImageManager.getImage(SFAImageManager.PIN);
		toUnPinImage = SFAImageManager.getImage(SFAImageManager.UNPIN);

		viewer.getTable().addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(Event event) {

				TableItem item = (TableItem) event.item;

				if (item.getData() instanceof CardSummary) {
					CardSummary summary = (CardSummary) item.getData();

					// if hovered over
					boolean selected = (event.detail & SWT.HOT) != 0;

					// 1st line text
					String text = summary.getName();
					textLayout.setText(text);

					TextStyle highlightStyle = new TextStyle(null, SugarItemsDashboard.getInstance().getBusinessCardLinkColor(), null);
					textLayout.setStyle(highlightStyle, 0, summary.getName().length());
					TextStyle normalStyle = new TextStyle(null, null, null);
					textLayout.setStyle(normalStyle, summary.getName().length() + iImageGap, text.length());

					int pinnedIconWidth = 0;
					if (summary.isPinned() == CardSummary.IS_PINNED) {
						pinnedIconWidth = pinnedImage.getBounds().width;
					}

					FontMetrics lineMetrics = textLayout.getLineMetrics(0);
					int textHeight = lineMetrics.getHeight();

					int firstLine_x = event.x + pinnedIconWidth + iImageGap;
					int firstLine_y = event.y + iGapHeight;
					textLayout.draw(event.gc, firstLine_x, firstLine_y);

					// to pin/unpin image
					if (selected) {
						int clientWidth = viewer.getTable().getClientArea().width - 2;
						Rectangle itemRect = item.getBounds();
						if (summary.isPinned().intValue() == CardSummary.IS_NOT_PINNED) {
							int toPin_x = clientWidth - toPinImage.getBounds().width - iImageGap;
							int toPin_y = itemRect.y + 1 * iGapHeight;
							event.gc.drawImage(toPinImage, toPin_x, toPin_y);
						} else {
							int toUnpin_x = clientWidth - toUnPinImage.getBounds().width - iImageGap;
							int toUnpin_y = itemRect.y + 1 * iGapHeight;
							event.gc.drawImage(toUnPinImage, toUnpin_x, toUnpin_y);
						}

					}

					// 2nd line
					textLayout.setText(summary.getDescription());
					// Add a strikethrough style to email if the email address is suppressed.
					if (summary.getEmail().length() > 0) {
						if (summary.isEmailSuppressed()) {
							TextStyle strikethroughStyle = new TextStyle(null, null, null);
							strikethroughStyle.strikeout = true;
							int startIndex = text.indexOf(summary.getEmail());
							textLayout.setStyle(strikethroughStyle, startIndex, startIndex + summary.getEmail().length() - 1);
						}
					}
					// Add a strikethrough style for the phone numbers if they are suppressed
					if (summary.getMobilePhone().length() > 0) {
						if (summary.isMobilePhoneSuppressed()) {
							TextStyle strikethroughStyle = new TextStyle(null, null, null);
							strikethroughStyle.strikeout = true;
							int startIndex = text.indexOf(summary.getMobilePhone());
							textLayout.setStyle(strikethroughStyle, startIndex, startIndex + summary.getMobilePhone().length() - 1);
						}
					}
					if (summary.getOfficePhone().length() > 0) {
						if (summary.isOfficePhoneSuppressed()) {
							TextStyle strikethroughStyle = new TextStyle(null, null, null);
							strikethroughStyle.strikeout = true;
							int startIndex = text.indexOf(summary.getOfficePhone());
							textLayout.setStyle(strikethroughStyle, startIndex, startIndex + summary.getOfficePhone().length() - 1);
						}
					}

					int secondLine_x = event.x;
					// int secondLine_y = event.y + iGapHeight + textHeight;
					// 57844 - make 2nd line text lined up with remove image
					int secondLine_y = event.y + event.height - textHeight - (iGapHeight - iImageGap);

					textLayout.draw(event.gc, secondLine_x, secondLine_y);

					// to remove image
					if (selected) {
						int clientWidth = viewer.getTable().getClientArea().width - 2;
						int toRemove_x = clientWidth - toRemoveImage.getBounds().width;

						Rectangle itemRect = item.getBounds();
						int toRemove_y = itemRect.y + itemRect.height - toRemoveImage.getBounds().height - (iGapHeight - iImageGap);

						event.gc.drawImage(toRemoveImage, toRemove_x, toRemove_y);
					}

					// pinned image
					if (summary.isPinned() == CardSummary.IS_PINNED) {

						int pinned_x = event.x;
						// 57844 - move image up a bit
						// int pinned_y = firstLine_y + iGapHeight + iImageGap;
						int pinned_y = firstLine_y + (iGapHeight - iImageGap);

						event.gc.drawImage(pinnedImage, pinned_x, pinned_y);

					}

				}
			}
		});

		createEmulatedHover(viewer);

		createDnD(viewer);

		// Just do this once, not every time we measure
		GC gc = new GC(viewer.getTable());
		fontHeight = gc.getFontMetrics().getHeight();
		gc.dispose();

		viewer.getTable().addListener(SWT.MeasureItem, new Listener() {
			@Override
			public void handleEvent(Event event) {
				TableItem item = (TableItem) event.item;
				CardSummary summary = (CardSummary) item.getData();

				String text = summary.getFullText();
				String[] lines = text.split("\n"); //$NON-NLS-1$
				int sizeX = 0;
				int lineCtr = 0;
				for (String line : lines) {
					Point size = event.gc.textExtent(line);
					if (size.x > sizeX) {
						sizeX = size.x;
					}
					lineCtr++;
				}

				// event.width = sizeX + (2 * TEXT_MARGIN);
				event.width = viewer.getTable().getClientArea().width - 5;
				// 57844 - enforcing min. height: either text heights or 2x icon heights
				// event.height = (fontHeight * lineCtr) + 5;
				int maxPinIconHeight = Math.max(toPinImage.getBounds().height, toUnPinImage.getBounds().height);
				event.height = Math.max(((fontHeight * lineCtr) + 5), (toRemoveImage.getBounds().height + toPinImage.getBounds().height + iGapHeight + iImageGap + 2));
				
			}

		});

		viewer.getTable().addListener(SWT.EraseItem, new Listener() {
			public void handleEvent(Event event) {
				/* indicate that we'll be drawing the foreground in the PaintItem listener */
				event.detail &= ~SWT.FOREGROUND;
			}
		});

		viewer.getTable().addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseScrolled(MouseEvent arg0) {
				Table tbl = (Table) arg0.widget;
				if (tbl != null) {
					tbl.redraw();
				}
				// canvas.redraw();

			}

		});

		// When a card is raised, refresh the viewer
		final PropertyChangeListener cardRaisedListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						viewer.setInput(SugarItemsDashboard.getInstance().getPreviouslyViewedCards().toArray());
					}
				});
			}
		};

		// When the background load is started, make the progress bar visible
		final PropertyChangeListener loadStartedListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				updateRecentlyViewedParts();
			}
		};

		// When the background load finishes, show the appropriate UI state
		final PropertyChangeListener loadCompleteListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				updateRecentlyViewedParts();
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						viewer.setInput(SugarItemsDashboard.getInstance().getPreviouslyViewedCards().toArray());
						if (SugarItemsDashboard.getInstance().getPreviouslyViewedCards().size() > 0) {
							viewer.getTable().setSelection(0);
						}
					}
				});
			}
		};

		SugarItemsDashboard.getInstance().getPropertyChangeSupport().addPropertyChangeListener(SugarItemsDashboard.CARD_RAISED_PROPERTY, cardRaisedListener);
		SugarItemsDashboard.getInstance().getPropertyChangeSupport().addPropertyChangeListener(SugarItemsDashboard.LOAD_STARTED_PROPERTY, loadStartedListener);
		SugarItemsDashboard.getInstance().getPropertyChangeSupport().addPropertyChangeListener(SugarItemsDashboard.LOAD_COMPLETE_PROPERTY, loadCompleteListener);

		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				SugarItemsDashboard.getInstance().getPropertyChangeSupport().removePropertyChangeListener(SugarItemsDashboard.CARD_RAISED_PROPERTY, cardRaisedListener);
				SugarItemsDashboard.getInstance().getPropertyChangeSupport().removePropertyChangeListener(SugarItemsDashboard.LOAD_STARTED_PROPERTY, loadStartedListener);
				SugarItemsDashboard.getInstance().getPropertyChangeSupport().removePropertyChangeListener(SugarItemsDashboard.LOAD_COMPLETE_PROPERTY, loadCompleteListener);
			}
		});
	}

	private void createDnD(final TableViewer viewer) {

		Transfer[] types = new Transfer[]{TextTransfer.getInstance()};
		viewer.addDragSupport(DND.DROP_MOVE, types, new DragSourceAdapter() {

			public void dragStart(DragSourceEvent event) {
				DragSource ds = (DragSource) event.widget;
				Table table = (Table) ds.getControl();
				TableItem[] selection = table.getSelection();
				CardSummary summary = (CardSummary) selection[0].getData();
				// If this is not a pinned card, can not D&D
				if (summary.isPinned().intValue() == CardSummary.IS_NOT_PINNED) {
					event.doit = false; // this will cancel DnD operation
					return;
				}
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				// Get the selected items in the drag source
				DragSource ds = (DragSource) event.widget;
				Table table = (Table) ds.getControl();
				TableItem[] selection = table.getSelection();
				// String name, String description, SugarType type, String id
				CardSummary summary = (CardSummary) selection[0].getData();

				event.data = summary.getId();
			}
		});

		viewer.addDropSupport(DND.DROP_MOVE, types, new ViewerDropAdapter(viewer) {

			public void dragEnter(DropTargetEvent event) {
			}

			@Override
			public boolean validateDrop(Object target, int operation, TransferData transferType) {

				return TextTransfer.getInstance().isSupportedType(transferType);

			}

			@Override
			public boolean performDrop(Object data) {

				return true;
				// return false;
			}

			public void dragOver(DropTargetEvent event) {
				if (event.item == null) {
					event.feedback = DND.DROP_NONE;
					return;
				}

				if (((CardSummary) ((TableItem) event.item).getData()).isPinned().intValue() == CardSummary.IS_NOT_PINNED) {
					event.detail = DND.DROP_NONE;
					return;
				}
				// resetting drop icon (otherwise, if you drop over a not allowed item, then drop over the allowed item, the icon does not reset back)
				event.detail = DND.DROP_MOVE;

				event.feedback = DND.FEEDBACK_SCROLL;

				// Set correct insertion line depending on the pointer position
				// inside a table item
				switch (eventLocation(event)) {
					case UPPER_HALF :
						event.feedback |= DND.FEEDBACK_INSERT_BEFORE;
						break;
					case LOWER_HALF :
						event.feedback |= DND.FEEDBACK_INSERT_AFTER;
						break;
					default :
						break;
				}

			}

			public void drop(DropTargetEvent event) {
				// ((DropTarget)event.widget).getControl()
				if (event.data == null || event.item == null || (event.item instanceof TableItem && ((TableItem) event.item).getData() == null)) {
					event.detail = DND.DROP_NONE;
					return;
				}

				// this stopped drop from happening... but need some other logic to visually indicate that drop is not allowed
				if (((CardSummary) ((TableItem) event.item).getData()).isPinned().intValue() == CardSummary.IS_NOT_PINNED) {
					event.detail = DND.DROP_NONE;
					return;
				}

				// pin order
				long pinOrder = 0;
				boolean toStartUpdate = false;
				int dragUpOrDown = 0; // indicating if the d&d operation direction
				// 1: drag the item up, 2: drag the item down

				// drag card
				String dragId = (String) event.data;
				CardSummary dragSummary = null;
				long dragTimestampMillis = 0;

				// drop source card
				TableItem item = (TableItem) event.item;
				CardSummary dropSummary = (CardSummary) item.getData();

				// get preferences
				try {
					Preferences prefs = UiPluginActivator.getDefault().getPluginPreferences();
					// JSON doesn't honor order, so we keep a bit of extra data around to tell us the proper order
					String recentlyViewedCardsOrder = prefs.getString(SugarItemsDashboard.RECENTLY_VIEWED_CARDS_ORDER_PREF_KEY);

					String[] order = recentlyViewedCardsOrder.split(","); //$NON-NLS-1$
					List<String> orderedList = new ArrayList<String>(Arrays.asList(order));

					TableItem[] items = viewer.getTable().getItems();
					for (int i = items.length - 1; i > -1; i--) {

						CardSummary summary = (CardSummary) items[i].getData();

						if (summary.getId().equals(dragId)) {
							dragSummary = summary;
							if (dragUpOrDown == 0) {
								dragUpOrDown = 1; // when iterating from bottom up, if hit the drag item first, it's dragged up
							}
						} else {
							if (summary.isPinned().intValue() == CardSummary.IS_PINNED) {
								if (summary.getId().equals(dropSummary.getId())) {
									if (dragUpOrDown == 0) {
										dragUpOrDown = 2; // when iterating from bottom up, if hit the drop item first, it's dragged down
									}
									if (dragUpOrDown == 1) {
										pinOrder++;
										summary.setTimestampMillis(pinOrder);
										orderedList.remove(summary.getId()); // Remove it from it's current place in the list and move it to the front.
										orderedList.add(0, summary.getId());

										pinOrder++;
										dragTimestampMillis = pinOrder;
										orderedList.remove(dragId); // Remove it from it's current place in the list and move it to the front.
										orderedList.add(0, dragId);

									} else {
										pinOrder++;
										dragTimestampMillis = pinOrder;
										orderedList.remove(dragId); // Remove it from it's current place in the list and move it to the front.
										orderedList.add(0, dragId);

										pinOrder++;
										summary.setTimestampMillis(pinOrder);
										orderedList.remove(summary.getId()); // Remove it from it's current place in the list and move it to the front.
										orderedList.add(0, summary.getId());
									}
								} else {
									pinOrder++;
									summary.setTimestampMillis(pinOrder);
									orderedList.remove(summary.getId()); // Remove it from it's current place in the list and move it to the front.
									orderedList.add(0, summary.getId());

								}

							}
						}
					}

					prefs.setValue(SugarItemsDashboard.RECENTLY_VIEWED_CARDS_ORDER_PREF_KEY, StringUtils.join(orderedList.toArray(), ",")); //$NON-NLS-1$
					UiPluginActivator.getDefault().savePluginPreferences();

					dragSummary.setTimestampMillis(dragTimestampMillis);

					viewer.setInput(SugarItemsDashboard.getInstance().getPreviouslyViewedCards());

				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
				}

			}

		});
	}

	private void createEmulatedHover(TableViewer viewer) {
		// final Display display = new Display ();
		final Table table = viewer.getTable();
		final Shell shell = new Shell(Display.getCurrent());

		// Disable native tooltip
		table.setToolTipText("");

		// Implement a "fake" tooltip
		Listener tableListener = new Listener() {

			@Override
			public void handleEvent(Event event) {

				final Shell shell = new Shell(Display.getCurrent());

				switch (event.type) {
					case SWT.Dispose :
					case SWT.KeyDown :
					case SWT.MouseMove : {
						if (tip == null)
							break;
						tip.dispose();
						tip = null;
						styledText = null;
						actionStyledText = null;
						break;
					}
					case SWT.MouseHover : {
						final TableItem item = table.getItem(new Point(event.x, event.y));
						if (item != null) {
							if (tip != null && !tip.isDisposed())
								tip.dispose();
							tip = new Shell(shell, SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL /* | SWT.WRAP | SWT.MULTI */);
							tip.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
							FillLayout layout = new FillLayout();
							layout.marginWidth = 2;
							layout.type = SWT.VERTICAL;
							tip.setLayout(layout);
							CardSummary summary = (CardSummary) item.getData();
							Rectangle rectangle = null;
							if (summary != null && summary.isPinned().intValue() == CardSummary.IS_NOT_PINNED && (rectangle = imageFocused(toPinImage, summary, 0)) != null) {
								String txt = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_SIDEBAR_PIN);
								toDisplayImageHover(tip, table, rectangle, txt);
							} else if (summary != null && summary.isPinned().intValue() == CardSummary.IS_PINNED && (rectangle = imageFocused(toUnPinImage, summary, 1)) != null) {
								String txt = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_SIDEBAR_UNPIN);
								toDisplayImageHover(tip, table, rectangle, txt);
							} else if (summary != null && (rectangle = imageFocused(toRemoveImage, summary, 2)) != null) {
								String txt = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_SIDEBAR_DELETE);
								toDisplayImageHover(tip, table, rectangle, txt);
							} else {
								toDisplayCardHover(tip, table, item);
							}
						}
					}

				}
			}
		};
		table.addListener(SWT.Dispose, tableListener);
		table.addListener(SWT.KeyDown, tableListener);
		table.addListener(SWT.MouseMove, tableListener);
		table.addListener(SWT.MouseHover, tableListener);
	}

	private void toDisplayImageHover(Shell shell, Table table, Rectangle imageRect, String txt) {
		if (table == null || imageRect == null) {
			return;
		}

		actionStyledText = new StyledText(tip, SWT.WRAP | SWT.BORDER);
		actionStyledText.setText(txt);

		// make font size a bit smaller
		FontData[] fd = actionStyledText.getFont().getFontData();
		int styledTextHeight = (int) fd[0].height - 3;
		fd[0].setHeight(styledTextHeight);
		final Font hoverFont = new Font(Display.getCurrent(), fd);
		actionStyledText.setFont(hoverFont);
		actionStyledText.setForeground(JFaceResources.getColorRegistry().get(recentlyViewCardsHoverForeGround));

		actionStyledText.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				if (hoverFont != null && !hoverFont.isDisposed()) {
					hoverFont.dispose();
				}

			}
		});

		GC gc = new GC(table);
		gc.setFont(actionStyledText.getFont());
		Point gcpt1 = gc.stringExtent(txt);
		gc.dispose();

		// get hover box width and height
		int tipTextMargin = 15;
		int tipBorderMargin = 8;
		int tipWidth = gcpt1.x + tipTextMargin;
		int tipHeight = gcpt1.y + tipBorderMargin;

		// put hover text next to image ( to the left of the image)
		int tipx = imageRect.x - tipWidth - 3;
		int tipy = imageRect.y;

		tip.setBounds(tipx, tipy, tipWidth, tipHeight);
		tip.setVisible(true);

	}

	private void toDisplayCardHover(Shell shell, Table table, TableItem item) {
		styledText = new StyledText(tip, SWT.WRAP | SWT.BORDER);
		CardSummary summary = (CardSummary) item.getData();
		styledText.setText(summary.getFullText());

		// make font size a bit smaller
		FontData[] fd = styledText.getFont().getFontData();
		int styledTextHeight = (int) fd[0].height - 2;
		fd[0].setHeight(styledTextHeight);
		final Font hoverFont = new Font(Display.getCurrent(), fd);

		styledText.setFont(hoverFont);
		styledText.setForeground(JFaceResources.getColorRegistry().get(recentlyViewCardsHoverForeGround));

		styledText.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				if (hoverFont != null && !hoverFont.isDisposed()) {
					hoverFont.dispose();
				}

			}
		});

		GC gc = new GC(table);
		gc.setFont(styledText.getFont());
		Point gcpt1 = gc.stringExtent(summary.getName());
		Point gcpt2 = gc.stringExtent(summary.getDescription());
		Point gcpt = new Point(Math.max(gcpt1.x, gcpt2.x), gcpt1.y);
		gc.dispose();

		// get hover box width and height
		int tipTextMargin = 15;
		int tipBorderMargin = 5;
		int tipWidth = gcpt.x + tipTextMargin;
		int tipHeight = 2 * gcpt.y + tipTextMargin;

		Rectangle displayRect = Display.getCurrent().getBounds();
		Rectangle itemRect = item.getBounds();
		Point itemPt = table.toDisplay(item.getBounds().x, item.getBounds().y);

		// get hover box start location x
		int shift = 10;
		int tipx = Display.getCurrent().getCursorLocation().x + shift;

		// System.out.println("\nhover(" + summary.getName() + "):  gcpt:" + gcpt + ", tipWidth=" + tipWidth + ", tipHeight=" + tipHeight + ", displayRect=" + displayRect + ", itemRect=" + itemRect +
		// ", itemPt=" + itemPt + ", tipx=" + tipx);

		// if not enough space at right, anchor the right border and move the start point to left
		// System.out.println("(" + tipx + "+" + tipWidth + ") > (" + displayRect.x + " + " + displayRect.width + ") ? (if true, extend to left)");
		if ((tipx + tipWidth) > (displayRect.x + displayRect.width)) {
			tipx = displayRect.x + displayRect.width - tipWidth;

		}

		// get hover box start location y
		int tipy = itemPt.y + itemRect.height + tipBorderMargin;

		tip.setBounds(tipx, tipy, tipWidth, tipHeight);
		tip.setVisible(true);

	}

	/**
	 * Helper method that updates the state of the recently viewed parts based on the load/loading state of the recently viewed parts web service call.
	 */
	private void updateRecentlyViewedParts() {

		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				boolean showLoadingComposite = SugarItemsDashboard.getInstance().loadingCards;
				boolean previouslyViewedCardsLoaded = SugarItemsDashboard.getInstance().previouslyViewedCardsLoaded;

				reloadComposite.setVisible(!showLoadingComposite && !previouslyViewedCardsLoaded);
				((GridData) (reloadComposite.getLayoutData())).exclude = showLoadingComposite || previouslyViewedCardsLoaded;

				if (SugarItemsDashboard.getInstance().errorsLoadingCards) {
					reloadLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_SIDEBAR_RELOAD_ERROR));
				} else {
					reloadLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_SIDEBAR_RELOAD_INSTRUCTIONS));
				}

				loadingComposite.setVisible(showLoadingComposite);
				((GridData) (loadingComposite.getLayoutData())).exclude = !showLoadingComposite;

				tableComposite.setVisible(previouslyViewedCardsLoaded);
				((GridData) (tableComposite.getLayoutData())).exclude = !previouslyViewedCardsLoaded;

				layout(true);
			}
		});

	}

	/**
	 * Show the card selected in the combo box
	 */
	private void showSelectedCardInDropdown() {
		int index = itemCombo.getSelectionIndex();
		if (index >= 0) {
			Map<Integer, JSONObject> dataMap = (Map<Integer, JSONObject>) itemCombo.getData();
			if (dataMap != null) {
				try {
					JSONObject jsonObj = dataMap.get(index);
					final String sugarId = jsonObj.getString("id"); //$NON-NLS-1$					
					UiUtils.displaySugarItemById(sugarTypeMap.get(categoryCombo.getText()), sugarId, new NullProgressMonitor());
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, FilesPluginActivator.PLUGIN_ID);
				}
			}
			itemCombo.setText(ConstantStrings.EMPTY_STRING);
			itemCombo.removeAll();
			itemCombo.setData(null);
			itemCombo.redraw();
		}
	}

	/**
	 * Returns the default instruction text for the item selection combo
	 * 
	 * @return
	 */
	private String getItemComboDefaultText() {
		String text = ConstantStrings.EMPTY_STRING;
		if (categoryCombo.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CONTACT))) {
			text = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOOK_UP_CONTACT_HELP_TEXT);
		} else if (categoryCombo.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_OPPORTUNITY))) {
			text = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOOK_UP_OPPTY_HELP_TEXT);
		} else if (categoryCombo.getText().equalsIgnoreCase(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_INFO_TAB_CLIENT))) {
			text = UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.LOOK_UP_CLIENT_HELP_TEXT);
		}
		return text;
	}

	/**
	 * Gets the typeahead suggestions for the user
	 * 
	 * @param category
	 * @param city
	 * @param searchString
	 * @param searchMyItems
	 */
	private void getTypeaheadSuggestions(final String category, final String city, final String searchString, final boolean searchMyItems) {

		Job job = new Job("Getting typeahead suggestions") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				if (category.length() > 0) {

					String searchResults = SugarWebservicesOperations.getInstance().getTypeaheadInfoFromWebservice(sugarTypeMap.get(category).getParentType(), searchString, "30", //$NON-NLS-1$
							Boolean.toString(searchMyItems), city);

					final ArrayList<JSONObject> jsonObjectList = new ArrayList<JSONObject>();
					try {
						JSONObject searchResultsJSON = new JSONObject(searchResults);
						JSONArray resultsArray = searchResultsJSON.getJSONObject(ConstantStrings.RESULTS).getJSONArray(ConstantStrings.DATABASE_FIELDS);
						for (int i = 0; i < resultsArray.length(); i++) {
							jsonObjectList.add((JSONObject) resultsArray.get(i));

						}
					} catch (Exception e) {
						UtilsPlugin.getDefault().logException(e, FilesPluginActivator.PLUGIN_ID);
					}

					// If we got any results back, populate the combo box
					if (jsonObjectList.size() > 0) {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								try {
									String[] displayStrings = new String[jsonObjectList.size()];
									// We'll use this map to store detailed information about each entry in the combo box so
									// we can retrieve it if it used when an item is selected.
									Map<Integer, JSONObject> comboDataMap = new HashMap<Integer, JSONObject>();
									int ctr = 0;
									for (JSONObject result : jsonObjectList) {
										if (sugarTypeMap.get(category).equals(SugarType.ACCOUNTS)) {
											displayStrings[ctr] = result.getString("name"); //$NON-NLS-1$
										} else if (sugarTypeMap.get(category).equals(SugarType.CONTACTS)) {
											displayStrings[ctr] = result.getString("name"); //$NON-NLS-1$ 
										} else if (sugarTypeMap.get(category).equals(SugarType.OPPORTUNITIES)) {
											displayStrings[ctr] = result.getString("name") + " (" + result.getString("description") + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$											
										}
										comboDataMap.put(ctr, result);
										ctr++;
									}
									String originalText = itemCombo.getText();
									Point originalSelection = itemCombo.getSelection();
									itemCombo.setData(comboDataMap);
									itemCombo.setItems(displayStrings);
									itemCombo.setListVisible(true);
									itemCombo.setText(originalText);
									itemCombo.setSelection(originalSelection);
								} catch (Exception e) {
									UtilsPlugin.getDefault().logException(e, FilesPluginActivator.PLUGIN_ID);
								}

								doingTypeaheadLookup = false;

							}
						});
					} else {
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								String originalText = itemCombo.getText();
								Point originalSelection = itemCombo.getSelection();
								itemCombo.setItems(new String[]{UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.WEB_LOG_CALL_RELATED_TYPE_NOMATCH_MSG)});
								itemCombo.setListVisible(true);
								itemCombo.setData(null); // Clear out the data map
								// itemCombo.setFocus();
								itemCombo.setText(originalText);
								itemCombo.setSelection(originalSelection);

								doingTypeaheadLookup = false;

							}
						});
					}
				}
				return Status.OK_STATUS;
			}
		};

		job.schedule();
	}

	/**
	 * Switch the city text to normal edit mode
	 */
	private void unsetCityDefaultText() {
		cityText.setText(ConstantStrings.EMPTY_STRING);
		cityText.setForeground(categoryCombo.getForeground());
		cityText.setBackground(categoryCombo.getBackground());
		cityText.setFont(getNormalFont());
	}

	/**
	 * Show the default edit message in the city text
	 */
	private void setCityDefaultText() {
		// If enabled, set help hint with light grey color, and be sure the background is reset to parent's
		cityText.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CITY_HELP_TEXT));
		cityText.setForeground(JFaceResources.getColorRegistry().get("textFieldPromptColor")); //$NON-NLS-1$
		cityText.setBackground(categoryCombo.getBackground());
		cityText.setFont(getItalicsFont());
	}

	private void unsetItemDefaultText() {
		// The user is ready to enter an item value, we will clear up the help hint, set the foreground to regular color.
		itemCombo.setText(ConstantStrings.EMPTY_STRING);
		itemCombo.setForeground(categoryCombo.getForeground());
		itemCombo.setFont(getNormalFont());
	}

	private void setItemDefaultText(boolean showHelpText) {
		if (showHelpText) {
			itemCombo.setText(getItemComboDefaultText());
			itemCombo.setForeground(JFaceResources.getColorRegistry().get("textFieldPromptColor")); //$NON-NLS-1$      
			itemCombo.setFont(getItalicsFont());
		} else
		// If disabled, empty the help hint
		{
			itemCombo.setText(ConstantStrings.EMPTY_STRING);
		}
	}

	public Font getItalicsFont() {
		if (italicsFont == null) {
			FontData fontData = cityText.getFont().getFontData()[0];
			italicsFont = new Font(Display.getDefault(), new FontData(fontData.getName(), fontData.getHeight(), SWT.ITALIC));
		}
		return italicsFont;
	}

	public Font getNormalFont() {
		if (normalFont == null) {
			FontData fontData = cityText.getFont().getFontData()[0];
			normalFont = new Font(Display.getDefault(), new FontData(fontData.getName(), fontData.getHeight(), SWT.NORMAL));
		}
		return normalFont;
	}

	private TableItem getItem(CardSummary summary) {
		TableItem item = null;
		if (summary != null && viewer.getTable() != null && viewer.getTable().getItemCount() > 0) {
			TableItem[] items = viewer.getTable().getItems();
			for (int i = 0; i < items.length; i++) {
				if (items[i].getData() instanceof CardSummary) {
					if (((CardSummary) items[i].getData()).getId().equalsIgnoreCase(summary.getId())) {
						item = items[i];
						break;
					}
				}
			}

		}
		return item;
	}

	private Rectangle imageFocused(Image image, CardSummary summary, int actionType) {
		boolean isFocused = false;
		Rectangle imageLocationRect = null;

		if (image != null) {
			Rectangle imageRect = image.getBounds();

			if (summary != null) {

				TableItem focusedItem = getItem(summary);
				Rectangle focusedItemRect = focusedItem.getBounds();

				Rectangle clientareaRect = viewer.getTable().getClientArea();
				Point imageStartingPt = null;
				if (actionType == 0) { // topin
					int imageStartingPt_x = clientareaRect.width - imageRect.width - iImageGap;
					int imageStartingPt_y = focusedItemRect.y + 1 * iGapHeight;
					imageStartingPt = viewer.getTable().toDisplay(imageStartingPt_x, imageStartingPt_y);
				} else if (actionType == 1) { // to unpin
					int imageStartingPt_x = clientareaRect.width - imageRect.width - iImageGap;
					int imageStartingPt_y = focusedItemRect.y + 1 * iGapHeight;
					imageStartingPt = viewer.getTable().toDisplay(imageStartingPt_x, imageStartingPt_y);
				} else if (actionType == 2) { // to remove
					int imageStartingPt_x = clientareaRect.width - imageRect.width;
					int imageStartingPt_y = focusedItemRect.y + focusedItemRect.height - imageRect.height - iGapHeight;
					imageStartingPt = viewer.getTable().toDisplay(imageStartingPt_x, imageStartingPt_y);
				}

				Rectangle imageLocationRect_temp = new Rectangle(imageStartingPt.x, imageStartingPt.y, imageRect.width, imageRect.height);
				if (imageLocationRect_temp.contains(Display.getCurrent().getCursorLocation())) {
					isFocused = true;
					imageLocationRect = imageLocationRect_temp;
				}

			}
		}
		return imageLocationRect;
	}

	private void executeToPinAction(CardSummary summary) {
		if (summary != null) {
			if (summary.isPinned() == CardSummary.IS_PINNED) {
				summary.setPinned(CardSummary.IS_NOT_PINNED);
			} else {
				summary.setPinned(CardSummary.IS_PINNED);
			}
			summary.setTimestampMillis(System.currentTimeMillis());
			viewer.setInput(SugarItemsDashboard.getInstance().getPreviouslyViewedCards().toArray());
			if (SugarItemsDashboard.getInstance().getPreviouslyViewedCards().size() > 0) {
				viewer.getTable().setSelection(0);
			}
			updatePreviouslyViewedCards(summary, true);
		}
	}

	private void executeToRemoveAction(CardSummary summary) {
		if (summary != null) {
			SugarItemsDashboard.getInstance().getPreviouslyViewedCards().remove(summary);
			viewer.setInput(SugarItemsDashboard.getInstance().getPreviouslyViewedCards().toArray());
			if (SugarItemsDashboard.getInstance().getPreviouslyViewedCards().size() > 0) {
				viewer.getTable().setSelection(0);
			}
			updatePreviouslyViewedCards(summary, false);
		}
	}

	// toUpdate: true - update; false - delete
	private void updatePreviouslyViewedCards(CardSummary summary, boolean toUpdate) {
		if (summary == null) {
			return;
		}
		try {
			Preferences prefs = UiPluginActivator.getDefault().getPluginPreferences();
			// id:type, id:type...
			String recentlyViewedCards = prefs.getString(SugarItemsDashboard.RECENTLY_VIEWED_CARDS_PREF_KEY);
			// JSON doesn't honor order, so we keep a bit of extra data around to tell us the proper order
			String recentlyViewedCardsOrder = prefs.getString(SugarItemsDashboard.RECENTLY_VIEWED_CARDS_ORDER_PREF_KEY);

			String recentlyViewedCardsPinned = prefs.getString(SugarItemsDashboard.RECENTLY_VIEWED_CARDS_PINNED_PREF_KEY);

			if (recentlyViewedCards != null && !recentlyViewedCards.equals(ConstantStrings.EMPTY_STRING)) {
				// recent viewed cards
				JSONObject recentCards = new JSONObject(recentlyViewedCards);
				recentCards.remove(summary.getId());
				if (toUpdate) {
					recentCards.put(summary.getId(), summary.getType().toString());
				}
				String jsonString = ConstantStrings.EMPTY_STRING;
				if (!recentCards.isEmpty()) {
					jsonString = recentCards.toString();
				}
				prefs.setValue(SugarItemsDashboard.RECENTLY_VIEWED_CARDS_PREF_KEY, jsonString);

				// pinned cards
				if (recentlyViewedCardsPinned == null) {
					recentlyViewedCardsPinned = ConstantStrings.EMPTY_STRING;
				}
				String[] pinned = recentlyViewedCardsPinned.split(","); //$NON-NLS-1$
				List<String> pinnedList = new ArrayList<String>(Arrays.asList(pinned));
				pinnedList.remove(summary.getId()); // Remove it from it's current place in the list and move it to the front.
				if (toUpdate && summary.isPinned().intValue() == CardSummary.IS_PINNED) {
					pinnedList.add(0, summary.getId());
				}
				prefs.setValue(SugarItemsDashboard.RECENTLY_VIEWED_CARDS_PINNED_PREF_KEY, StringUtils.join(pinnedList.toArray(), ",")); //$NON-NLS-1$

				// card order
				if (recentlyViewedCardsOrder == null) {
					recentlyViewedCardsOrder = ConstantStrings.EMPTY_STRING;
				}
				String[] order = recentlyViewedCardsOrder.split(","); //$NON-NLS-1$
				List<String> orderedList = new ArrayList<String>(Arrays.asList(order));
				orderedList.remove(summary.getId()); // Remove it from it's current place in the list and move it to the front.
				if (toUpdate) {
					orderedList.add(0, summary.getId());
				}
				prefs.setValue(SugarItemsDashboard.RECENTLY_VIEWED_CARDS_ORDER_PREF_KEY, StringUtils.join(orderedList.toArray(), ",")); //$NON-NLS-1$
				UiPluginActivator.getDefault().savePluginPreferences();

			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}
	}

	private static int eventLocation(DropTargetEvent event) {
		TableItem item = (TableItem) event.item;
		Point point = item.getParent().getDisplay().map(null, item.getParent(), event.x, event.y);
		Rectangle bounds = item.getBounds();

		if (point.y < bounds.y + bounds.height / 2) {
			// Pointer is in the upper half of TableItem
			return UPPER_HALF;
		} else if (point.y >= bounds.y + bounds.height / 2) {
			// Pointer is in the lower half of TableItem
			return LOWER_HALF;
		} else {
			return 0;
		}
	}

	private CardSummary getMouseoverSummary(MouseEvent event, Point cursorPt) {
		CardSummary summary = null;
		Table table = viewer.getTable();
		Rectangle clientArea = table.getClientArea();
		Point pt = new Point(event.x, event.y);
		int index = table.getTopIndex();
		while (index < table.getItemCount()) {
			// boolean visible = false;
			TableItem item = table.getItem(index);

			Rectangle rect = item.getBounds();
			if (rect.contains(pt)) {
				summary = (CardSummary) item.getData();
				break;
			}

			index++;
		}
		return summary;
	}

	private void typeaheadSuggestionAction() {
		// Don't call the webservice until we have 2 characters
		if (!doingTypeaheadLookup && itemCombo.getText().trim().length() >= 2 && !itemCombo.getText().equalsIgnoreCase(getItemComboDefaultText())) {
			lastKeystrokeTime = System.currentTimeMillis();

			// 56585 - move flag to later
			// doingTypeaheadLookup = true;

			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						sleep(500); // Wait a 1/2 second before attempting to process the typeahead web service
						long elapsedTime = System.currentTimeMillis() - lastKeystrokeTime;
						if (elapsedTime >= 500) { // Only process this if 1/2 second has elapsed since the user pressed the last key

							// 56585
							doingTypeaheadLookup = true;

							Job job = new Job("Typeahead lookup") { //$NON-NLS-1$
								@Override
								protected IStatus run(IProgressMonitor arg0) {
									// Stupid hack so I don't have to create instance variables for all of these to reference them from
									// outside the UI thread.
									final String[] city = new String[1];
									final boolean[] cityEnabled = new boolean[1];
									final String[] category = new String[1];
									final String[] item = new String[1];
									final boolean[] myItemsSelected = new boolean[1];

									Display.getDefault().syncExec(new Runnable() {
										@Override
										public void run() {
											city[0] = cityText.getText();
											cityEnabled[0] = cityText.isEnabled();
											category[0] = categoryCombo.getText();
											item[0] = itemCombo.getText();
											myItemsSelected[0] = myItemsButton.getSelection();
										}
									});

									getTypeaheadSuggestions(category[0], cityEnabled[0] && !city[0].equals(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CITY_HELP_TEXT))
											? city[0]
											: ConstantStrings.EMPTY_STRING, item[0], myItemsSelected[0]);
									return Status.OK_STATUS;
								}
							};
							job.schedule();
						}
					} catch (Exception e) {
						UtilsPlugin.getDefault().logException(e, FilesPluginActivator.PLUGIN_ID);
					}
				}
			};
			t.start();
		}
	}

}
