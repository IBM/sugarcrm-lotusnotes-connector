package com.ibm.socialcrm.notesintegration.sametime.widgets;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;

import com.ibm.collaboration.realtime.people.Person;
import com.ibm.rcp.realtime.livenames.ui.HoverBusinessCard;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.sametime.SametimePluginActivator;
import com.ibm.socialcrm.notesintegration.sametime.dashboardComposites.SametimeComposite;
import com.ibm.socialcrm.notesintegration.sametime.utils.AbstractSametimeGroup;
import com.ibm.socialcrm.notesintegration.sametime.utils.Member;
import com.ibm.socialcrm.notesintegration.sametime.utils.Opportunity;
import com.ibm.socialcrm.notesintegration.sametime.utils.SametimeInfo;
import com.ibm.socialcrm.notesintegration.sametime.utils.SametimeListing;
import com.ibm.socialcrm.notesintegration.sametime.utils.SametimeUtils;
import com.ibm.socialcrm.notesintegration.sametime.utils.menus.SametimeMenu;
import com.ibm.socialcrm.notesintegration.sametime.utils.views.SametimeLabelProvider;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class SametimeViewer extends TreeViewer {
	private HoverBusinessCard visibleCard;

	private PropertyChangeListener personChangeListener;

	private SametimeComposite sametimeComposite;

	public SametimeViewer(final Composite parent, int style, SametimeComposite sametimeComposite) {
		super(parent, style);

		this.sametimeComposite = sametimeComposite;
		constructViewer();

		// Need to listen for when the user moves out of the tooltip so that we can dispose the shell.
		final Listener tooltipLabelListener = new Listener() {
			public void handleEvent(Event event) {
				Label label = (Label) event.widget;
				Shell shell = label.getShell();
				if (event.type == SWT.MouseExit) {
					shell.dispose();
				}
			}
		};

		// Listens for mouse events on the tree so that we can display tooltip text where ever necessary.
		Listener treeViewerListener = new Listener() {
			Shell tip = null;
			Label label = null;
			TreeItem item = null;

			public void handleEvent(Event event) {
				switch (event.type) {
					case SWT.Dispose :
					case SWT.MouseMove : {
						TreeItem treeItemOnMouseMove = getTree().getItem(new Point(event.x, event.y));
						// Do not dispose the tooltip if the mouse move was to the same item.
						if (treeItemOnMouseMove == null || treeItemOnMouseMove != item) {
							if (tip != null) {
								// We are here since we moved the mouse to out of the item. Need to dispose the tooltip.
								tip.dispose();
								tip = null;
								label = null;
							}
						}
						break;
					}
					case SWT.MouseHover : {
						item = getTree().getItem(new Point(event.x, event.y));
						if (item != null && item.getData() instanceof Opportunity) {
							Opportunity opp = (Opportunity) item.getData();
							if (tip != null && !tip.isDisposed()) {
								tip.dispose();
							}
							tip = new Shell(Display.getDefault().getShells()[0], SWT.ON_TOP | SWT.TOOL);
							tip.setLayout(new FillLayout());

							label = new Label(tip, SWT.WRAP);
							label.setText(GenericUtils.wrapText(opp.getDescription(), 40));

							label.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
							label.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
							label.addListener(SWT.MouseExit, tooltipLabelListener);

							Rectangle rect = item.getBounds(0);
							Point pt = getTree().toDisplay(rect.x, rect.y);
							// TODO... This is ok for now, but we need to figure out a better way to set the bounds, depending on the location
							// of the item on the screen.
							tip.setBounds(pt.x - 100, pt.y + 10, 0, 0);
							tip.pack();
							tip.setVisible(true);
						}
					}
				}
			}
		};

		getTree().addListener(SWT.Dispose, treeViewerListener);
		getTree().addListener(SWT.MouseMove, treeViewerListener);
		getTree().addListener(SWT.MouseHover, treeViewerListener);
		getTree().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent evt) {
				if (evt.character == SWT.CR) {
					SametimeUtils.startChat(getSelectedPersons());
				}
			}
		});
	}

	private void constructViewer() {
		try {
			setLabelProvider(new SametimeLabelProvider());

			setContentProvider(new SametimeViewerContentProvider());
			createMenu();
			createMouseListeners();
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, SametimePluginActivator.PLUGIN_ID);
		}
	}

	public void handleSelectionChange(final BaseSugarEntry sugarEntry) {
		if (!sametimeComposite.isDisposed()) {
			Job job = new Job("Get sametime listing") //$NON-NLS-1$
			{
				@Override
				protected IStatus run(IProgressMonitor arg0) {
					final SametimeListing sametimeListing = SametimeInfo.getInstance().createSametimeListing(sugarEntry);
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							if (!sametimeComposite.isDisposed() && !getTree().isDisposed()) {
								if (sametimeListing.getAccounts().isEmpty() && sametimeListing.getOpportunities().isEmpty()) {
									sametimeComposite.getNothingToShowLabel().setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SAMETIME_DOCUMENT_CONTAINS_NO_DATA));
									sametimeComposite.getNothingToShowLabel().setVisible(true);
									getTree().setVisible(false);

									((GridData) sametimeComposite.getNothingToShowLabel().getLayoutData()).exclude = false;
									((GridData) getTree().getLayoutData()).exclude = true;
									sametimeComposite.getNothingToShowLabel().getParent().layout();

									setInput(null);
								} else {
									sametimeComposite.getNothingToShowLabel().setText(ConstantStrings.EMPTY_STRING);
									sametimeComposite.getNothingToShowLabel().setVisible(false);
									getTree().setVisible(true);

									((GridData) sametimeComposite.getNothingToShowLabel().getLayoutData()).exclude = true;
									((GridData) getTree().getLayoutData()).exclude = false;

									sametimeComposite.getNothingToShowLabel().getParent().layout();
									setInput(sametimeListing);
									expandAll();
								}

								final Set<Person> persons = sametimeListing.recursiveGetSametimePersons();
								for (Person person : persons) {
									person.addPropertyChangeListener(getPersonChangeListener());
								}

								getTree().addDisposeListener(new DisposeListener() {
									@Override
									public void widgetDisposed(DisposeEvent arg0) {
										for (Person person : persons) {
											person.removePropertyChangeListener(getPersonChangeListener());
										}
									}
								});
							}
						}
					});
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}
	}

	private PropertyChangeListener getPersonChangeListener() {
		if (personChangeListener == null) {
			personChangeListener = new PropertyChangeListener() {
				@Override
				public void propertyChange(final PropertyChangeEvent evt) {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							refresh();
						}
					});
				}
			};
		}
		return personChangeListener;
	}

	private void createMenu() {
		SametimeMenu menu = new SametimeMenu(getControl().getShell(), SWT.POP_UP) {
			@Override
			public Set<Person> getSelectedPersons() {
				return SametimeViewer.this.getSelectedPersons();
			}
		};

		getTree().setMenu(menu.getMenu());
	}

	private Set<Person> getSelectedPersons() {
		IStructuredSelection selection = (IStructuredSelection) getSelection();
		Object[] objects = selection.toArray();
		Set<Person> personSet = new HashSet<Person>();
		for (Object obj : objects) {
			if (obj instanceof Member) {
				Member member = (Member) obj;
				personSet.add(member.getSametimePerson());
			}
			if (obj instanceof AbstractSametimeGroup) {
				personSet.addAll(((AbstractSametimeGroup) obj).recursiveGetSametimePersons());
			}
		}
		return personSet;
	}

	private void createMouseListeners() {
		getTree().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
				IStructuredSelection selection = (IStructuredSelection) getSelection();
				Object obj = selection.getFirstElement();
				if (obj instanceof Member) {
					final Member member = (Member) obj;
					Set<Person> sametimePerson = new TreeSet<Person>() {
						{
							add(member.getSametimePerson());
						}
					};
					SametimeUtils.startChat(sametimePerson);
				}
			}
		});

		getTree().addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseHover(MouseEvent event) {
				TreeItem item = getTree().getItem(new Point(event.x, event.y));
				if (item != null) {
					if (item.getData() instanceof Member) {
						try {
							Member member = (Member) item.getData();
							if (SametimeUtils.isShowBusinessCardEnabled() && member.getSametimePerson() != null && member.getSametimePerson().getContactId() != null
									&& !member.getSametimePerson().getContactId().equals(ConstantStrings.EMPTY_STRING)) {
								getVisibleCard().setLiveName(member.getSametimePerson());
								Point cursorLocation = getTree().getShell().getDisplay().getCursorLocation();

								// Don't show directly at the cursor location since we provide right click menu options.
								getVisibleCard().show(cursorLocation.x + 5, cursorLocation.y);
							}
						} catch (Exception e) {
							UtilsPlugin.getDefault().logException(e, SametimePluginActivator.PLUGIN_ID);
						}
					}
				}
			}
		});
	}

	private HoverBusinessCard getVisibleCard() {
		if (visibleCard == null || visibleCard.isDisposed()) {
			visibleCard = SametimeUtils.getBusinessCard();
		}

		return visibleCard;
	}

	// Private content provider
	private class SametimeViewerContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getChildren(Object obj) {
			return getElements(obj);
		}

		@Override
		public Object getParent(Object obj) {
			return null;
		}

		@Override
		public boolean hasChildren(Object obj) {
			return getElements(obj).length > 0;
		}

		@Override
		public Object[] getElements(Object element) {
			Object[] returnObj = new Object[0];
			if (element instanceof SametimeListing) {
				List<Object> combinedList = new ArrayList<Object>();
				combinedList.addAll(((SametimeListing) element).getOpportunities());
				combinedList.addAll(((SametimeListing) element).getAccounts());
				returnObj = combinedList.toArray();
			} else if (element instanceof AbstractSametimeGroup) {
				List<Object> combinedList = new ArrayList<Object>();
				List<AbstractSametimeGroup> subGroups = ((AbstractSametimeGroup) element).getSubGroups();
				if (subGroups != null) {
					combinedList.addAll(subGroups);
				}
				combinedList.addAll(((AbstractSametimeGroup) element).getMembers());
				returnObj = combinedList.toArray();
			}
			return returnObj;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		}
	}

}
