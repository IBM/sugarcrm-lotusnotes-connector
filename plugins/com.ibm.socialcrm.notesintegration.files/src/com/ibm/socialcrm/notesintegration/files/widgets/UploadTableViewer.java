package com.ibm.socialcrm.notesintegration.files.widgets;

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
import java.io.File;
import java.util.Collection;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.dialogs.FileSystemElement;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.model.WorkbenchViewerComparator;

import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFAHyperlink;

public class UploadTableViewer extends TableViewer {

	private PropertyChangeSupport propertyChangeSupport;

	public UploadTableViewer(Table table) {
		super(table);

		// When the table is resized, we should refresh the view to update the file name ellipsizing
		getTable().addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent evt) {
				refresh();
			}
		});

		setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				Object[] returnObj = new String[0];
				if (inputElement instanceof Collection) {
					returnObj = ((Collection) inputElement).toArray();
				}
				return returnObj;
			}

			@Override
			public void dispose() {
			}

			@Override
			public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
			}
		});
		setComparator(new WorkbenchViewerComparator() {
		});

		setLabelProvider(new FolderFileViewerLabelProvider());
		addDragAndDropSupport();
		createMenu();
		addKeylisteners();
	}

	private void addDragAndDropSupport() {
		ViewerDropAdapter dropAdapter = new ViewerDropAdapter(this) {
			@Override
			public boolean performDrop(final Object data) {
				String[] droppedItems = null;
				if (data instanceof String[]) {
					droppedItems = (String[]) data;
				} else if (data instanceof String) {
					droppedItems = new String[1];
					droppedItems[0] = (String) data;
				}
				if (droppedItems != null) {
					doRunDrop(droppedItems);
				}
				return true;
			}

			private void doRunDrop(String[] droppedItems) {
				Collection<File> fileList = (Collection<File>) getInput();
				for (String fileName : droppedItems) {
					fileList.add(new File(fileName));
				}
				getPropertyChangeSupport().firePropertyChange("inputChanged", true, false); //$NON-NLS-1$
				refresh();
			}

			@Override
			public boolean validateDrop(Object target, int operation, TransferData transferData) {
				boolean valid = false;
				for (TransferData data : FileTransfer.getInstance().getSupportedTypes()) {
					if (transferData.type == data.type) {
						valid = true;
						break;
					}
				}
				return valid;
			}
		};

		addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, new Transfer[]{FileTransfer.getInstance()}, dropAdapter);
	}

	/**
	 * Creates the popup menu for the table
	 */
	private void createMenu() {
		Menu menu = new Menu(getTable());
		MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UPLOAD_CONFLICT_REMOVE));
		item.setImage(SFAImageManager.getImage(SFAImageManager.DELETE));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evt) {
				Collection input = (Collection) getInput();
				for (TableItem item : getTable().getSelection()) {
					input.remove(item.getData());
				}
				getPropertyChangeSupport().firePropertyChange("inputChanged", true, false); //$NON-NLS-1$
				refresh();
			}
		});

		getTable().setMenu(menu);
	}

	private void addKeylisteners() {
		getTable().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent evt) {
				if (evt.keyCode == SWT.DEL) {
					Collection input = (Collection) getInput();
					for (TableItem item : getTable().getSelection()) {
						input.remove(item.getData());
					}
					getPropertyChangeSupport().firePropertyChange("inputChanged", true, false); //$NON-NLS-1$
					refresh();
				}
			}
		});
	}

	public PropertyChangeSupport getPropertyChangeSupport() {
		if (propertyChangeSupport == null) {
			propertyChangeSupport = new PropertyChangeSupport(this);
		}
		return propertyChangeSupport;
	}

	class FolderFileViewerLabelProvider extends LabelProvider implements ITableLabelProvider {
		private WorkbenchLabelProvider provider = new WorkbenchLabelProvider();

		public String getColumnText(Object obj, int index) {
			String text = getText(obj);
			String displayString = text;

			GC gc = new GC(getTable());
			int textLengthInPixels = gc.stringExtent(text).x;
			int availableTextSpace = (getTable().getSize().x - 40); // Fudge factor for the image and margins
			if (textLengthInPixels > availableTextSpace) {
				int averageWidth = textLengthInPixels / text.length();
				int numCharacters = availableTextSpace / averageWidth;

				// Split the string in half and put the ... in the middle
				String begin = text.substring(0, numCharacters / 2 - 2);
				String end = text.substring(text.length() - (numCharacters / 2 - 2), text.length());
				displayString = begin + "..." + end; //$NON-NLS-1$
			}
			gc.dispose();

			return displayString;
		}

		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}

		public Image getImage(Object obj) {
			File file = (File) obj;
			FileSystemElement fakeFile = new FileSystemElement(file.getName(), null, false);
			Image image = provider.getImage(fakeFile);

			return image;
		}
	}
}
