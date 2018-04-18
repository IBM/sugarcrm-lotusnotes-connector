package com.ibm.socialcrm.notesintegration.sametime.utils.dialogs;

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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.ibm.collaboration.realtime.buddylist.BuddyList;
import com.ibm.collaboration.realtime.buddylist.BuddyListService;
import com.ibm.collaboration.realtime.people.Group;
import com.ibm.collaboration.realtime.people.NestableGroup;
import com.ibm.collaboration.realtime.people.PrivateGroup;
import com.ibm.collaboration.realtime.servicehub.ServiceHub;
import com.ibm.socialcrm.notesintegration.sametime.SametimePluginActivator;
import com.ibm.socialcrm.notesintegration.sametime.utils.views.SametimeLabelProvider;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;

public class CustomAddContactDialog extends Dialog {
	private TreeViewer viewer;
	private PrivateGroup selectedGroup;
	private String newGroup;
	private boolean createAsChild = false;;

	public CustomAddContactDialog(Shell shell) {
		super(shell);
		setShellStyle(SWT.RESIZE | getShellStyle());
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		try {
			parent = (Composite) super.createDialogArea(parent);
			parent.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).margins(10, 10).create());

			Label existingLabel = new Label(parent, SWT.WRAP);
			existingLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
			existingLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SAMETIME_SELECT_EXISTING_GROUP));

			viewer = new TreeViewer(parent, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
			viewer.getTree().setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(375, SWT.DEFAULT).create());
			viewer.setLabelProvider(new SametimeLabelProvider());
			viewer.setContentProvider(new SametimeGroupContentProvider());

			BuddyListService bls = (BuddyListService) ServiceHub.getService(BuddyListService.SERVICE_TYPE);
			BuddyList list = bls.getBuddyList();
			viewer.setInput(list.getAllGroups());
			viewer.setSorter(new ViewerSorter());
			viewer.setFilters(new ViewerFilter[] { new ViewerFilter() {
				@Override
				public boolean select(Viewer viewer, Object parent, Object element) {
					return (element instanceof PrivateGroup);
				}
			} });
			viewer.expandAll();

			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					StructuredSelection selection = (StructuredSelection) event.getSelection();
					Object obj = selection.getFirstElement();
					if (obj instanceof PrivateGroup) {
						setSelectedGroup((PrivateGroup) obj);
					}
				}
			});

			Label createLabel = new Label(parent, SWT.WRAP);
			createLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).create());
			createLabel.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SAMETIME_CREATE_NEW_GROUP));

			final Text groupText = new Text(parent, SWT.BORDER);
			groupText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
			groupText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					setNewGroup(groupText.getText());
				}
			});

			final Button addAsSubGroupButton = new Button(parent, SWT.CHECK);
			addAsSubGroupButton.setLayoutData(GridDataFactory.fillDefaults().create());
			addAsSubGroupButton.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SAMETIME_ADD_AS_SUBGROUP));
			addAsSubGroupButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					createAsChild = addAsSubGroupButton.getSelection();
				}
			});
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, SametimePluginActivator.PLUGIN_ID);
		}

		return parent;
	}

	/**
	 * Override to set the shell title
	 */
	@Override
	public void create() {
		super.create();
		getShell().setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SAMETIME_ADD_SAMETIME_CONTACT));
	}

	private class SametimeGroupContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getChildren(Object obj) {
			return getElements(obj);
		}

		@Override
		public Object getParent(Object obj) {
			Object parent = null;
			if (obj instanceof Group) {
				parent = ((NestableGroup) obj).getParentGroup();
			}
			return parent;
		}

		@Override
		public boolean hasChildren(Object obj) {
			return getChildren(obj).length > 0;
		}

		@Override
		public Object[] getElements(Object obj) {
			Object[] elements = new Object[0];
			if (obj instanceof NestableGroup) {
				elements = ((NestableGroup) obj).getSubGroups();
			} else if (obj instanceof Object[]) {
				elements = (Object[]) obj;
			}
			return elements;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		}

	}

	public PrivateGroup getSelectedGroup() {
		return selectedGroup;
	}

	private void setSelectedGroup(PrivateGroup selectedGroup) {
		this.selectedGroup = selectedGroup;
	}

	public String getNewGroup() {
		return newGroup;
	}

	private void setNewGroup(String newGroup) {
		this.newGroup = newGroup;
	}

	public boolean createAsChild() {
		return createAsChild;
	}

}
