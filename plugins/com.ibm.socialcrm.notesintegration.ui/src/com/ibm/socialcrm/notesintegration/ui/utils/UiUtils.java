package com.ibm.socialcrm.notesintegration.ui.utils;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import lotus.domino.Document;
import lotus.domino.EmbeddedObject;
import lotus.domino.Item;
import lotus.domino.RichTextItem;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.ibm.notes.java.ui.NotesUIWorkspace;
import com.ibm.notes.java.ui.documents.NotesUIDocument;
import com.ibm.notes.java.ui.documents.NotesUIField;
import com.ibm.socialcrm.notesintegration.core.BaseSugarEntry;
import com.ibm.socialcrm.notesintegration.core.CorePluginActivator;
import com.ibm.socialcrm.notesintegration.core.SugarAccount;
import com.ibm.socialcrm.notesintegration.core.SugarContact;
import com.ibm.socialcrm.notesintegration.core.SugarOpportunity;
import com.ibm.socialcrm.notesintegration.core.utils.LoginException;
import com.ibm.socialcrm.notesintegration.core.utils.SugarV10APIManager;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.core.utils.UpdateSelectionsBroadcaster;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations.GetInfo13RestulType;
import com.ibm.socialcrm.notesintegration.sugarwidgetupdate.SugarWidgetUpdateActivator;
import com.ibm.socialcrm.notesintegration.ui.UiPluginActivator;
import com.ibm.socialcrm.notesintegration.ui.actions.AbstractSugarAction.ActionSearchFilter;
import com.ibm.socialcrm.notesintegration.ui.custom.SugarEntrySelectionComposite;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class UiUtils {

	private static Shell multiSelectionShell;

	private static int PROMPT_THRESHOLD = 10;

	// Job rule so Jobs/UIJobs following this rule will be executed in the correct order.
	public static final ISchedulingRule DISPLAY_SUGAR_ITEM_BY_ID_JOB_RULE = new ISchedulingRule() {
		public boolean contains(ISchedulingRule rule) {
			return this.equals(rule);
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return this.equals(rule);
		}

		public String toString() {
			return "DISPLAY_SUGAR_ITEM_BY_ID_JOB_RULE"; //$NON-NLS-1$
		}
	};

	private static BaseSugarEntry getSugarEntryById(SugarType sugarType, final String id, IProgressMonitor monitor) {

		BaseSugarEntry entry = SugarWebservicesOperations.getInstance().getSugarEntryById(id);
		if (entry == null) {
			if (monitor != null) {
				monitor.beginTask("Loading data from web service", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
			}
			callSugarGetInfoByIdHelper(sugarType, id);
			if (monitor != null) {
				monitor.done();
			}
			entry = SugarWebservicesOperations.getInstance().getSugarEntryById(id);
		}
		return entry;
	}

	/**
	 * Displays the dashboard for a sugar item
	 * 
	 * @param sugarType
	 * @param id
	 */
	public static void displaySugarItemById13(final SugarType sugarType, final String id, final String text, final IProgressMonitor monitor) {
		GenericUtils.establishMainNotesWindow();
		Job job = new Job("Display item for " + id) //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				// final BaseSugarEntry entry = getSugarEntryById(sugarType, id, monitor);
				if (SugarWebservicesOperations.getInstance().getSugarEntryById(id) != null) {
					SugarWebservicesOperations.getInstance().getSugarEntries().remove(SugarWebservicesOperations.getInstance().getSugarEntryById(id));
				}

				final BaseSugarEntry entry = buildSugarEntryShell(sugarType, id, text);
				SugarWebservicesOperations.getInstance().getSugarEntries().add(entry);
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						SugarItemsDashboard.getInstance().displayPanel(entry, sugarType, id);
					}
				});
				return Status.OK_STATUS;
			}
		};
		// Setting job rule so jobs following this rule will be executed in the correct order.
		job.setRule(DISPLAY_SUGAR_ITEM_BY_ID_JOB_RULE);
		job.schedule();
	}

	/**
	 * Displays the dashboard for a sugar item
	 * 
	 * @param sugarType
	 * @param id
	 */
	public static void displaySugarItemById(final SugarType sugarType, final String id, final IProgressMonitor monitor) {
		GenericUtils.establishMainNotesWindow();
		Job job = new Job("Display item for " + id) //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				final BaseSugarEntry entry = getSugarEntryById(sugarType, id, monitor);
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						SugarItemsDashboard.getInstance().displayPanel(entry, sugarType, id);
					}
				});
				return Status.OK_STATUS;
			}
		};
		// Setting job rule so jobs following this rule will be executed in the correct order.
		job.setRule(DISPLAY_SUGAR_ITEM_BY_ID_JOB_RULE);
		job.schedule();
	}

	/**
	 * Displays the dashboard for a sugar item at the specified location
	 * 
	 * @param sugarType
	 * @param id
	 * @param displayLocation
	 */
	public static void displaySugarItemById(final SugarType sugarType, final String id, final Point displayLocation, final IProgressMonitor monitor) {
		Job job = new Job("Display item for " + id) //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				final BaseSugarEntry entry = getSugarEntryById(sugarType, id, monitor);
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						SugarItemsDashboard.getInstance().displayPanel(entry, sugarType, id);
					}
				});
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	/**
	 * Displays the dashboard for a sugar item
	 * 
	 * @param sugarType
	 * @param text
	 * @param isCentered
	 */
	public static void displaySugarItem(SugarType sugarType, String text) {
		displaySugarItem(sugarType, text, null);
	}

	/**
	 * Displays the dashboard for a sugar item
	 * 
	 * @param sugarType
	 * @param text
	 * @param isCentered
	 * @param ActionSearchFilter
	 */
	public static void displaySugarItem(final SugarType sugarType, final String text, final ActionSearchFilter searchFilter) {
		GenericUtils.establishMainNotesWindow();
		Job job = new Job("Display item for " + text) //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				final BaseSugarEntry entry[] = new BaseSugarEntry[1];
				List<String> searchIds = new ArrayList<String>();

				Map<String, Set<String>> tagMap = SugarWidgetUpdateActivator.getDefault().getTagMapForType(sugarType);
				if (tagMap != null) {
					for (String id : tagMap.keySet()) {
						Set<String> tags = tagMap.get(id);
						for (String tag : tags) {
							// Put Pattern.compile in a try/catch block in case our tags are corrupted for any reason. We should just log the exception and continue searching on.
							try {
								Pattern p = Pattern.compile(tag, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
								if (p.matcher(text).matches()) {
									searchIds.add(id);
								}
							} catch (Exception e) {
								UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
							}
						}
					}
				}

				if (searchIds.size() == 0) {
					if (sugarType.equals(SugarType.OPPORTUNITIES)) {
						String id = callGetSugarOpportunityInfoByNameHelper(text);
						if (!SugarWebservicesOperations.getInstance().hasConnectionProblem()) {
							if (id != null) {
								searchIds.add(id);
							}
						}
					} else if (sugarType.equals(SugarType.CONTACTS)) {
						if (searchFilter != null) {
							List<String> ids = null;
							if (searchFilter.equals(ActionSearchFilter.SEARCH_BY_EMAIL)) {
								ids = callGetSugarContactInfoByEmailHelper(text);
							} else if (searchFilter.equals(ActionSearchFilter.SEARCH_BY_NAME)) {
								ids = callGetSugarContactInfoByLiveTextNameHelper(text);
							}
							if (ids != null) {
								searchIds.addAll(ids);
							}
						}
					} else if (sugarType.equals(SugarType.ACCOUNTS)) {
						String id = callGetSugarAccountInfoByIdHelper(text);
						if (!SugarWebservicesOperations.getInstance().hasConnectionProblem()) {
							if (id != null) {
								searchIds.add(id);
							}
						}
					}
				}

				if (searchIds.size() < 2) {

					String id = text;
					if (searchIds.size() == 0) {
						entry[0] = null;

					}
					if (searchIds.size() == 1 && searchIds.get(0) != null) {
						id = searchIds.get(0);

						// build the skeleton of sugar entry to make the perspective building logic happy
						if (SugarWebservicesOperations.getInstance().getSugarEntryById(id) != null) {
							SugarWebservicesOperations.getInstance().getSugarEntries().remove(SugarWebservicesOperations.getInstance().getSugarEntryById(id));
						}

						entry[0] = buildSugarEntryShell(sugarType, id, text);
						SugarWebservicesOperations.getInstance().getSugarEntries().add(entry[0]);
					}

					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							SugarItemsDashboard.getInstance().displayPanel(entry[0], sugarType, text);
						}
					});

				} else if (searchIds.size() >= 2) {
					final List<BaseSugarEntry> entries = new ArrayList<BaseSugarEntry>();
					for (String id : searchIds) {
						callSugarGetInfoByIdHelper(sugarType, id);
						BaseSugarEntry anEntry = SugarWebservicesOperations.getInstance().getSugarEntryById(id);
						if (anEntry != null) {
							entries.add(anEntry);
						}
					}

					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							boolean createdNewShell = false;
							if (multiSelectionShell == null || multiSelectionShell.isDisposed()) {
								multiSelectionShell = new Shell(Display.getDefault(), SWT.RESIZE | SWT.TITLE | SWT.CLOSE | SWT.MODELESS);
								multiSelectionShell.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).create());
								createdNewShell = true;
							}

							// We're replacing the old contents with new stuff
							if (!createdNewShell) {
								Control[] children = multiSelectionShell.getChildren();
								for (Control child : children) {
									child.dispose();
								}
							}

							SugarEntrySelectionComposite selectionComposite = new SugarEntrySelectionComposite(multiSelectionShell, entries);
							selectionComposite.setLayoutData(GridDataFactory.fillDefaults().hint(SWT.DEFAULT, Math.min(350, entries.size() * 50)).grab(true, false).create());
							multiSelectionShell.layout(true);

							if (createdNewShell) {
								multiSelectionShell.pack();
								Monitor primary = Display.getDefault().getPrimaryMonitor();
								Rectangle bounds = primary.getBounds();
								Rectangle rect = multiSelectionShell.getBounds();

								int x = bounds.x + (bounds.width - rect.width) / 2;
								int y = bounds.y + (bounds.height - rect.height) / 2;

								multiSelectionShell.setLocation(new Point(x, y));
								multiSelectionShell.open();
							}
						}
					});
				}

				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private static BaseSugarEntry buildSugarEntryShell(SugarType sugarType, String id, String name) {
		BaseSugarEntry sugarEntry = null;
		if (sugarType != null && id != null) {
			if (sugarType.equals(SugarType.OPPORTUNITIES)) {
				sugarEntry = new SugarOpportunity(id, id);
			} else if (sugarType.equals(SugarType.CONTACTS) && name != null) {
				sugarEntry = new SugarContact(id, name);
			} else if (sugarType.equals(SugarType.ACCOUNTS)) {
				sugarEntry = new SugarAccount(id, name);
			}
		}
		return sugarEntry;
	}

	public static void callSugarGetInfoByIdHelper(SugarType sugarType, final String id) {
		// Switch to new getinfo API, will retrieve only basecard information for now. If this is not enough, we will need to call
		// callSugargetInfo13 again with additional GetInfo13ResultType
		SugarWebservicesOperations.getInstance().callSugarGetInfo13(sugarType, id, GetInfo13RestulType.BASECARD);

	}

	/**
	 * Wrapper for a call to SugarWebserviceOperations to populate the information about an oppty
	 * 
	 * @param name
	 */
	private static String callGetSugarOpportunityInfoByNameHelper(final String name) {
		String idX = null;
		String resultTag = SugarWebservicesOperations.GETINFO13_RESULTTAG;
		String output = SugarWebservicesOperations.getInstance().loadSugarInfo13FromWebservice(name, SugarWebservicesOperations.getInstance().getGetInfo13InputDataType(SugarType.OPPORTUNITIES),
				GetInfo13RestulType.BASECARD, resultTag);

		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(output);
		} catch (JSONException e) {
			// End gracefully.
		}
		if (jsonObject != null) {
			try {
				if (jsonObject.containsKey("result") && !(jsonObject.get("result") instanceof JSONArray) && jsonObject.getJSONObject("result").containsKey(resultTag)) { //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$					
					Map<String, Object> map = GenericUtils.JSONObjectToMap(jsonObject.getJSONObject("result").getJSONObject(resultTag));//$NON-NLS-1$
					if (map != null && !map.isEmpty()) {
						Iterator<String> it = map.keySet().iterator();
						while (it.hasNext()) {
							String tmpid = it.next();
							Object obj = map.get(tmpid);

							if (obj == null || (obj instanceof JSONArray && ((JSONArray) obj).length() == 0)) {
							} else {
								idX = tmpid;
								break;
							}
						}
					}

				}
			} catch (JSONException e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}

		}

		return idX;
	}

	/**
	 * Populates the information about an account by account id
	 * 
	 * @param id
	 */
	private static String callGetSugarAccountInfoByIdHelper(final String id) {

		String idX = null;
		String resultTag = SugarWebservicesOperations.GETINFO13_RESULTTAG;
		String inputDataType = "accountid"; //$NON-NLS-1$

		// 85309 - if the id is 14 or less character long, it's a ccms id and not a sugar db id, we will
		// use v10 api to retrieve the account id.

		// // If the id is less than thirty characters, it's a CCMS id and not a sugar db id
		// if (id.length() < 30) {
		//			inputDataType = "ccmsid"; //$NON-NLS-1$
		// }
		String real_id = id;
		if (id != null && id.length() <= 14) {
			try {
				real_id = SugarV10APIManager.getInstance().getAccountIDFromCCMSId(id);
			} catch (LoginException e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
			if (real_id == null) {
				return idX;
			}
		}

		String output = SugarWebservicesOperations.getInstance().loadSugarInfo13FromWebservice(real_id, inputDataType, GetInfo13RestulType.BASECARD, resultTag);

		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(output);
		} catch (JSONException e) {
			// End gracefully.
		}
		if (jsonObject != null) {
			try {
				if (jsonObject.containsKey("result") && !(jsonObject.get("result") instanceof JSONArray) && jsonObject.getJSONObject("result").containsKey(resultTag)) { //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$
					Map<String, Object> map = GenericUtils.JSONObjectToMap(jsonObject.getJSONObject("result").getJSONObject(resultTag)); //$NON-NLS-1$
					if (map != null && !map.isEmpty()) {
						Iterator<String> it = map.keySet().iterator();
						while (it.hasNext()) {
							String tmpid = it.next();
							Object obj = map.get(tmpid);

							if (obj == null || (obj instanceof JSONArray && ((JSONArray) obj).length() == 0)) {
							} else {
								idX = tmpid;
								break;
							}

						}
					}
				}
			} catch (JSONException e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}

		}

		return idX;
	}

	/**
	 * Calls the webservice given a live text email and returns a list of IDs of all the matches.
	 * 
	 * @param email
	 * @return
	 */
	private static List<String> callGetSugarContactInfoByEmailHelper(final String email) {
		List<String> ids = new ArrayList<String>();
		String resultTag = SugarWebservicesOperations.GETINFO13_RESULTTAG;
		String output = SugarWebservicesOperations.getInstance().loadSugarInfo13FromWebservice(email, "contactemail", GetInfo13RestulType.BASECARD, resultTag); //$NON-NLS-1$

		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(output);
		} catch (JSONException e) {
			// End gracefully.
		}
		if (jsonObject != null) {
			try {
				if (jsonObject.containsKey("result") && !(jsonObject.get("result") instanceof JSONArray) && jsonObject.getJSONObject("result").containsKey(resultTag)) { //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$
					Map<String, Object> map = GenericUtils.JSONObjectToMap(jsonObject.getJSONObject("result").getJSONObject(resultTag)); //$NON-NLS-1$
					if (map != null && !map.isEmpty()) {
						Iterator<String> it = map.keySet().iterator();
						while (it.hasNext()) {
							String id = it.next();
							ids.add(id);
						}
					}
				}
			} catch (JSONException e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
		return ids;
	}

	/**
	 * Calls the webservice given a live text name and returns a list of IDs of all the matches.
	 * 
	 * @param name
	 * @return
	 */
	private static List<String> callGetSugarContactInfoByLiveTextNameHelper(String name) {

		List<String> ids = new ArrayList<String>();
		String resultTag = SugarWebservicesOperations.GETINFO13_RESULTTAG;
		String output = SugarWebservicesOperations.getInstance().loadSugarInfo13FromWebservice(name, "contactname", GetInfo13RestulType.BASECARD, resultTag); //$NON-NLS-1$

		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(output);
		} catch (JSONException e) {
			// End gracefully.
		}
		if (jsonObject != null) {
			try {
				if (jsonObject.containsKey("result") && !(jsonObject.get("result") instanceof JSONArray) && jsonObject.getJSONObject("result").containsKey(resultTag)) { //$NON-NLS-1$ //$NON-NLS-2$  //$NON-NLS-3$
					JSONObject resultObj = jsonObject.getJSONObject("result"); //$NON-NLS-1$

					// Java's JSON libraries are too heavily typed and kinda brittle. If we search for an item that does note exist, the webservice
					// will return an empty [] instead of a JSONObject { }. So, we have to check that what we get back is valid before proceeding.
					boolean validResult = true;
					try {
						resultObj.getJSONArray((resultTag));
						validResult = false;
					} catch (Exception e) {
					}

					if (validResult) {
						Map<String, Object> map = GenericUtils.JSONObjectToMap(resultObj.getJSONObject(resultTag));
						if (map != null && !map.isEmpty()) {
							Iterator<String> it = map.keySet().iterator();
							while (it.hasNext()) {
								String idX = it.next();
								ids.add(idX);
								if (map.size() == 1) {
									SugarWebservicesOperations.getInstance().getLiveTextMatchesCache().put(name, idX);
								}
							}
						}
					}
				}
			} catch (JSONException e) {
				UtilsPlugin.getDefault().logException(e, CorePluginActivator.PLUGIN_ID);
			}
		}
		return ids;
	}

	public static void recursiveSetBackgroundColor(Composite outerComposite, Color color) {
		outerComposite.setBackground(color);

		List<Control> controls = UiUtils.getInnerControls(outerComposite);
		for (Control control : controls) {
			control.setBackground(color);
		}
	}

	public static void recursiveSetForegroundColor(Composite outerComposite, Color color) {
		outerComposite.setForeground(color);

		List<Control> controls = UiUtils.getInnerControls(outerComposite);
		for (Control control : controls) {
			control.setForeground(color);
		}
	}

	public static List<Composite> getInnerComposites(Composite outerComposite) {
		List<Composite> innerComposites = new ArrayList<Composite>();
		for (Control control : outerComposite.getChildren()) {
			if (control instanceof Composite) {
				Composite innerComposite = (Composite) control;
				innerComposites.add(innerComposite);
				innerComposites.addAll(getInnerComposites(innerComposite));
			}
		}
		return innerComposites;
	}

	public static List<Control> getInnerControls(Composite outerComposite) {
		List<Control> innerControls = new ArrayList<Control>();
		for (Control control : outerComposite.getChildren()) {
			innerControls.add(control);
			if (control instanceof Composite) {
				innerControls.addAll(getInnerControls((Composite) control));
			}
		}
		return innerControls;
	}

	/**
	 * Creates an email and populates with the sugar entry, also associates the email.
	 * 
	 * @param sugarEntry
	 */
	public static void createEmailWithAssociate(final List<BaseSugarEntry> sugarEntries) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				internalCreateEmailWithAssociate(sugarEntries);
			}
		});
	}

	private static void internalCreateEmailWithAssociate(final List<BaseSugarEntry> sugarEntries) {
		try {
			// final String emailAddress = sugarEntry.getEmail();
			// concatenate emails with "," as divider
			final String emailAddress = getEmailAddress(sugarEntries);
			System.out.println("emailAddress=" + emailAddress);

			NotesUIWorkspace workspace = new NotesUIWorkspace();
			// The current document before opening the email form
			final NotesUIDocument currentDocBefore = getCurrentDocument();

			// 35211 - force Notes client to the front
			Shell workbenchShell = Display.getDefault().getShells()[0];
			// if (workbenchShell.getMinimized() || !workbenchShell.isFocusControl()) {
			if (workbenchShell.getMinimized()) {
				workbenchShell.setMinimized(false);
			}
			workbenchShell.setFocus();
			workbenchShell.forceActive();
			// }

			workspace.openUrl("Notes:///0000000000000E00/Memo?OpenForm"); //$NON-NLS-1$
			// The current document before opening the email form. It will probably be the same the first time through. Which is why we have the job below.
			NotesUIDocument currentDocAfter = getCurrentDocument();
			if (currentDocAfter != null && currentDocAfter != currentDocBefore) {
				if (sugarEntries.size() > 1) {
					NotesUIField field = currentDocAfter.getField("EnterBlindCopyTo"); //$NON-NLS-1$
					field.setText(emailAddress);
				} else {
					NotesUIField field = currentDocAfter.getField("EnterSendTo"); //$NON-NLS-1$
					field.setText(emailAddress);
				}
				setupAssociate(sugarEntries);
			} else {
				// It takes the email form a while to come up. So we have to wait until the currentDoc changes.
				// It's a bit hackish, but we don't really have a way to to identify the new NotesUIDocument we create
				// by calling the above URL. So we check for a change in the current doc every split second and jump on
				// the first promising looking changes. A mean user could click really fast and break us. But they'd
				// have to be quick. If Notes allowed us to pass in an identifier on the URL, we wouldn't have this problem.
				Job job = new Job("Wait for email form") //$NON-NLS-1$
				{
					@Override
					protected IStatus run(IProgressMonitor arg0) {
						final boolean[] done = new boolean[]{false};
						while (!done[0]) {
							try {
								Thread.sleep(250);
							} catch (Exception e) {
								UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
							}
							Display.getDefault().syncExec(new Runnable() {
								@Override
								public void run() {
									NotesUIDocument currentDoc = getCurrentDocument();
									if (currentDoc != null && currentDoc != currentDocBefore) {
										// Do a cursory check to ensure that the current doc is at least of the proper form.
										if (currentDoc.getField("Form").getText().equals("Memo")) //$NON-NLS-1$ //$NON-NLS-2$
										{
											if (sugarEntries.size() > 1) {
												NotesUIField field = currentDoc.getField("EnterBlindCopyTo"); //$NON-NLS-1$
												field.setText(emailAddress);
											} else {
												NotesUIField field = currentDoc.getField("EnterSendTo"); //$NON-NLS-1$
												field.setText(emailAddress);
											}
											setupAssociate(sugarEntries);
											done[0] = true;
										}
									}
								}
							});
						}
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}

	}

	private static String getEmailAddress(List<BaseSugarEntry> sugarEntries) {
		StringBuffer sb = new StringBuffer("");
		boolean isFirst = true;
		if (sugarEntries != null && !sugarEntries.isEmpty()) {
			Iterator<BaseSugarEntry> it = sugarEntries.iterator();
			while (it.hasNext()) {
				BaseSugarEntry sugarEntry = it.next();
				if (isFirst) {
					isFirst = false;
				} else {
					sb.append(ConstantStrings.COMMA);
				}
				sb.append((sugarEntry.getEmail() == null) ? "" : sugarEntry.getEmail());
			}
		}
		return sb.toString();
	}

	/**
	 * Creates an email and populates with the specified email address.
	 * 
	 * @param emailAddress
	 */
	public static void createEmail(final String emailAddress) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				internalCreateEmail(emailAddress);
			}
		});
	}

	/**
	 * Does the actual work of creating an email
	 * 
	 * @param emailAddress
	 */
	private static void internalCreateEmail(final String emailAddress) {
		try {
			NotesUIWorkspace workspace = new NotesUIWorkspace();
			// The current document before opening the meeting form
			final NotesUIDocument currentDocBefore = getCurrentDocument();
			workspace.openUrl("Notes:///0000000000000E00/Memo?OpenForm"); //$NON-NLS-1$
			// The current document before opening the meeting form. It will probably be the same the first time through. Which is why we have the job below.
			NotesUIDocument currentDocAfter = getCurrentDocument();
			if (currentDocAfter != null && currentDocAfter != currentDocBefore) {
				NotesUIField field = currentDocAfter.getField("EnterSendTo"); //$NON-NLS-1$
				field.setText(emailAddress);
			} else {
				// It takes the email form a while to come up. So we have to wait until the currentDoc changes.
				// It's a bit hackish, but we don't really have a way to to identify the new NotesUIDocument we create
				// by calling the above URL. So we check for a change in the current doc every split second and jump on
				// the first promising looking changes. A mean user could click really fast and break us. But they'd
				// have to be quick. If Notes allowed us to pass in an identifier on the URL, we wouldn't have this problem.
				Job job = new Job("Wait for email form") //$NON-NLS-1$
				{
					@Override
					protected IStatus run(IProgressMonitor arg0) {
						final boolean[] done = new boolean[]{false};
						while (!done[0]) {
							try {
								Thread.sleep(250);
							} catch (Exception e) {
								UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
							}
							Display.getDefault().syncExec(new Runnable() {
								@Override
								public void run() {
									NotesUIDocument currentDoc = getCurrentDocument();
									if (currentDoc != null && currentDoc != currentDocBefore) {
										// Do a cursory check to ensure that the current doc is at least of the proper form.
										if (currentDoc.getField("Form").getText().equals("Memo")) //$NON-NLS-1$ //$NON-NLS-2$
										{
											NotesUIField field = currentDoc.getField("EnterSendTo"); //$NON-NLS-1$
											field.setText(emailAddress);
											done[0] = true;
										}
									}
								}
							});
						}
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}

	}

	/**
	 * Opens the create meeting form and populates it with information appropriate for the given sugar entry
	 * 
	 * @param sugarEntry
	 */
	public static void createMeeting(final List<BaseSugarEntry> sugarEntry) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				if (SugarWebservicesOperations.getInstance().unableToLogin()) {
					SugarWebservicesOperations.getPropertyChangeSupport().firePropertyChange(SugarWebservicesOperations.BRING_UP_CREDENTIAL_PROMPT, true, false);
				} else {
					internalCreateMeeting(sugarEntry);
				}
			}
		});
	}

	/**
	 * Does the actual work of creating the a meeting with the specified sugar entry
	 * 
	 * @param sugarEntry
	 */
	public static void internalCreateMeeting(final List<BaseSugarEntry> sugarEntries) {
		try {
			NotesUIWorkspace workspace = new NotesUIWorkspace();
			// The current document before opening the meeting form
			final NotesUIDocument currentDocBefore = getCurrentDocument();

			Shell workbenchShell = Display.getDefault().getShells()[0];
			if (workbenchShell.getMinimized()) {
				workbenchShell.setMinimized(false);
				workbenchShell.setVisible(true);
				workbenchShell.setActive();
			}
			// 68269 - force uiworkspace to be active
			workbenchShell.setActive();

			workspace.openUrl("notes:///0000000000000E00/Appointment?OpenForm"); //$NON-NLS-1$

			// The current document before opening the meeting form. It will probably be the same the first time through. Which is why we have the job below.
			NotesUIDocument currentDocAfter = getCurrentDocument();
			if (currentDocAfter != null && currentDocAfter != currentDocBefore) {
				convertToMeeting(currentDocAfter, sugarEntries);
			} else {
				// It takes the meeting form a while to come up. So we have to wait until the currentDoc changes.
				// It's a bit hackish, but we don't really have a way to to identify the new NotesUIDocument we create
				// by calling the above URL. So we check for a change in the current doc every split second and jump on
				// the first promising looking changes. A mean user could click really fast and break us. But they'd
				// have to be quick. If Notes allowed us to pass in an identifier on the URL, we wouldn't have this problem.
				Job job = new Job("Wait for meeting form") //$NON-NLS-1$
				{
					@Override
					protected IStatus run(IProgressMonitor arg0) {
						final boolean[] done = new boolean[]{false};
						while (!done[0]) {
							try {
								Thread.sleep(250);
							} catch (Exception e) {
								UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
							}
							Display.getDefault().syncExec(new Runnable() {
								@Override
								public void run() {
									NotesUIDocument currentDoc = getCurrentDocument();
									if (currentDoc != null && currentDoc != currentDocBefore) {
										// Do a cursory check to ensure that the current doc is at least of the proper form.
										if (currentDoc.getField("Form").getText().equals("Appointment")) //$NON-NLS-1$ //$NON-NLS-2$
										{
											convertToMeeting(currentDoc, sugarEntries);
											done[0] = true;
										}
									}
								}
							});
						}
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}
		} catch (Exception e) {
			UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
		}
	}

	/**
	 * Converts the selected document from an appointment into a meeting and sets the appropriate fields.
	 * 
	 * @param document
	 */
	private static void convertToMeeting(final NotesUIDocument document, final List<BaseSugarEntry> sugarEntries) {
		setupAssociate(sugarEntries);

		NotesUIField field = document.getField("Subject"); //$NON-NLS-1$
		if (sugarEntries.get(0).getSugarType() == SugarType.ACCOUNTS) {
			field.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.MEETING_WITH, new String[]{sugarEntries.get(0).getName()}));
		} else if (sugarEntries.get(0).getSugarType() == SugarType.CONTACTS) {
			field.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.MEETING_WITH_CONTACT,
					new String[]{sugarEntries.get(0).getName(), ((SugarContact) sugarEntries.get(0)).getAccountName()}));
		} else if (sugarEntries.get(0).getSugarType() == SugarType.OPPORTUNITIES) {
			field.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.MEETING_ABOUT_OPPTY,
					new String[]{sugarEntries.get(0).getName(), ((SugarOpportunity) sugarEntries.get(0)).getPrimaryContact()}));
		} else if (sugarEntries.get(0).getSugarType() == SugarType.LEADS) {
			StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
			boolean isFirst = true;
			for (int i = 0; i < sugarEntries.size(); i++) {
				if (isFirst) {
					isFirst = false;
				} else {
					sb.append(ConstantStrings.COMMA);
				}

				sb.append(sugarEntries.get(i).getName());
			}
			field.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.MEETING_WITH_LEAD, new String[]{sb.toString()}));

		}

		// These are some magical values that need to be set into the document
		field = document.getField("tmpAppointmentType"); //$NON-NLS-1$
		field.setText("Meeting"); //$NON-NLS-1$

		field = document.getField("AppointmentType"); //$NON-NLS-1$
		field.setText("Meeting"); //$NON-NLS-1$

		// If Meeting is not the default calendar form, we need to refresh uidocument so the form will pick up the
		// meeting calendar type and meeting fields we set above.
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				NotesUIDocument currentDoc = getCurrentDocument();
				if (currentDoc != null) {
					currentDoc.refresh();
				}
			}
		});

		Job job = new Job("Retrieve optional invitees") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor arg0) {
				try {
					SugarType type = sugarEntries.get(0).getSugarType();
					String optionalInvitees = ConstantStrings.EMPTY_STRING;
					final String[] primaryContactId = new String[]{ConstantStrings.EMPTY_STRING};
					if (type == SugarType.ACCOUNTS || type == SugarType.OPPORTUNITIES) {
						String output = ConstantStrings.EMPTY_STRING;
						if (type == SugarType.ACCOUNTS) {
							// Get the ST info the account
							output = SugarWebservicesOperations.getInstance().getSametimeInfoFromWebservice(new ArrayList<String>() {
								{
									add(sugarEntries.get(0).getId());
								}
							}, null, null);
							primaryContactId[0] = ((SugarAccount) sugarEntries.get(0)).getPrimaryContactId();
						} else {
							primaryContactId[0] = ((SugarOpportunity) sugarEntries.get(0)).getPrimaryContactID();
							// Get the ST info the oppty
							output = SugarWebservicesOperations.getInstance().getSametimeInfoFromWebservice(null, new ArrayList<String>() {
								{
									add(sugarEntries.get(0).getId());
								}
							}, null);
						}

						if (SugarWebservicesOperations.getInstance().hasConnectionProblem()) {
							Display.getDefault().asyncExec(new Runnable() {
								@Override
								public void run() {
									MessageDialog.openError(Display.getDefault().getShells()[0], UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.CONNECTION_ERROR), UtilsPlugin
											.getDefault().getResourceString(UtilsPluginNLSKeys.MEETING_CONNECTION_ERROR, new String[]{sugarEntries.get(0).getName()}));
								}
							});
						} else {
							// Add the primary contact for the oppty or client to the required field
							if (primaryContactId[0] != null && primaryContactId[0].trim().length() > 0) {
								BaseSugarEntry contact = SugarWebservicesOperations.getInstance().getSugarEntryById(primaryContactId[0]);
								if (contact == null) {

									// Switch to new getinfo API, will retrieve only basecard information for now. If this is not enough, we will need to call
									// callSugargetInfo13 again with additional GetInfo13ResultType

									// SugarWebservicesOperations.getInstance().loadSugarInfoFromWebservice(null, null, new ArrayList<String>() {
									// {
									// add(primaryContactId[0]);
									// }
									// });
									SugarWebservicesOperations.getInstance().callSugarGetInfo13(SugarType.CONTACTS, primaryContactId[0], GetInfo13RestulType.BASECARD);

								}
								contact = SugarWebservicesOperations.getInstance().getSugarEntryById(primaryContactId[0]);
								if (contact != null) {
									if (contact.getEmail() != null && contact.getEmail().trim().length() > 0) {
										NotesUIField field = document.getField("EnterSendTo"); //$NON-NLS-1$
										field.setText(contact.getEmail());
									}
								}
							}

							JSONObject jsonObject = new JSONObject(output);
							// The data we get back in members will contain both the client and oppty team. We don't want to add
							// all of these people to the meeting. So we'll build a list to filter the global members list against.
							// It's easier to do it this way since the email address is stored in the global members list.
							Set<String> validMembers = new HashSet<String>();

							JSONObject entryObj = jsonObject.getJSONObject(type == SugarType.ACCOUNTS ? ConstantStrings.ACCOUNTS : ConstantStrings.OPPORTUNITIES);

							boolean toContinue = false;

							if (entryObj.keys().hasNext()) {
								String key = (String) entryObj.keys().next();
								JSONObject obj = entryObj.getJSONObject(key);
								JSONArray membersArray = obj.getJSONArray(ConstantStrings.MEMBERS);
								if (toPromptIfToContinue(getMemCountMinusMe(jsonObject, membersArray))) {
									toContinue = true;
									for (int i = 0; i < membersArray.length(); i++) {
										JSONObject memberEntry = (JSONObject) membersArray.get(i);
										validMembers.add(memberEntry.getString(ConstantStrings.ID));
									}
								}
							}

							if (toContinue && output.contains(ConstantStrings.MEMBERS)) {
								// JSONArray membersJson = jsonObject.getJSONArray(ConstantStrings.MEMBERS);
								JSONObject membersJson = jsonObject.getJSONObject(ConstantStrings.MEMBERS);

								Iterator membersIterator = membersJson.keys();
								while (membersIterator.hasNext()) {
									String key = (String) membersIterator.next();
									JSONObject member = membersJson.getJSONObject(key);
									String email = (String) member.get(ConstantStrings.DATABASE_EMAIL);
									// Don't add ourselves to the invite list
									if (!email.trim().equals(NotesAccountManager.getInstance().getCRMUser().trim()) && validMembers.contains(key)) {
										optionalInvitees += email + ","; //$NON-NLS-1$
									}
								}

								if (optionalInvitees.length() > 0) {
									// Trim the last comma
									optionalInvitees = optionalInvitees.substring(0, optionalInvitees.length() - 1);
								}
							}

							// Add the sales team as optional members
							NotesUIField field = document.getField("EnterCopyTo"); //$NON-NLS-1$
							if (optionalInvitees != null && optionalInvitees.length() > 0) {
								field.setText(optionalInvitees);
							}
						}
					} else if (type == SugarType.CONTACTS) {
						String requiredInvitee = sugarEntries.get(0).getEmail();
						// Add the contacts email as the primary attendee
						NotesUIField field = document.getField("EnterSendTo"); //$NON-NLS-1$
						if (requiredInvitee != null && requiredInvitee.length() > 0) {
							field.setText(requiredInvitee);
						}
					} else if (type == SugarType.LEADS) {
						StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
						boolean isFirst = true;
						for (int i = 0; i < sugarEntries.size(); i++) {
							if (isFirst) {
								isFirst = false;
							} else {
								sb.append(ConstantStrings.COMMA);
							}

							sb.append(sugarEntries.get(i).getEmail());

						}
						// Add the lead email as the primary attendee
						NotesUIField field = document.getField("EnterSendTo"); //$NON-NLS-1$
						if (!sb.toString().equalsIgnoreCase(ConstantStrings.EMPTY_STRING)) {
							field.setText(sb.toString());
						}
					}

					// 47912
					document.refresh();

				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
	private static int getMemCountMinusMe(JSONObject dictionaryJSONObject, JSONArray memberIDListForThisAccount) {
		int memCount = 0;
		String myid = null;
		if (dictionaryJSONObject != null && memberIDListForThisAccount != null && dictionaryJSONObject.containsKey(ConstantStrings.MEMBERS)) {
			memCount = memberIDListForThisAccount.length();
			try {
				// first, find my id from members dictionary
				JSONObject dictionaryMembers = dictionaryJSONObject.getJSONObject(ConstantStrings.MEMBERS);
				Iterator<String> it = dictionaryMembers.keys();
				while (it.hasNext()) {
					String dictionaryMemberKey = it.next();
					JSONObject dictionaryMemberObject = dictionaryMembers.getJSONObject(dictionaryMemberKey);
					if (dictionaryMemberObject.containsKey(ConstantStrings.DATABASE_EMAIL) && dictionaryMemberObject.getString(ConstantStrings.DATABASE_EMAIL) != null
							&& dictionaryMemberObject.getString(ConstantStrings.DATABASE_EMAIL).trim().equals(NotesAccountManager.getInstance().getCRMUser().trim())) {
						myid = dictionaryMemberKey;
						break;
					}

				}
				// next, check if my id is in the account's member list... If it is, adjust the member count
				if (myid != null) {
					for (int i = 0; i < memberIDListForThisAccount.length(); i++) {
						JSONObject idObject = memberIDListForThisAccount.getJSONObject(i);
						if (idObject != null && idObject.getString("id") != null && idObject.getString("id").equals(myid)) {
							memCount = memCount - 1;
						}
					}
				}

			} catch (Exception e) {

			}

		}

		return memCount;
	}

	private static boolean toPromptIfToContinue(final int memCount) {
		final int[] results = new int[]{0};
		if (memCount > PROMPT_THRESHOLD) {

			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {

					MessageDialog dialog = new MessageDialog(Display.getDefault().getShells()[0], UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SCHEDULE_MEETING_TITLE),
							SFAImageManager.getImage(SFAImageManager.SALES_CONNECT), UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.SCHEDULE_MEETING_ADDALLMEMBERS,
									new String[]{String.valueOf(memCount)}), MessageDialog.QUESTION, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
					results[0] = dialog.open(); /* yes is 0, no is 1 */
				}
			});
		}
		return results[0] == 0;
	}

	/**
	 * Returns the current document in the Notes workspace
	 * 
	 * @return
	 */
	private static NotesUIDocument getCurrentDocument() {
		NotesUIWorkspace workspace = new NotesUIWorkspace();
		return workspace.getCurrentDocument();
	}

	// private static void setupAssociate(BaseSugarEntry sugarEntry) {
	// UpdateSelectionsBroadcaster.getInstance().updateConnector(sugarEntry);
	//
	// }

	private static void setupAssociate(List<BaseSugarEntry> sugarEntries) {

		UpdateSelectionsBroadcaster.getInstance().updateConnector(sugarEntries);

	}

	public static final String ATTACHMENT_IS_SELECTED = "1";//$NON-NLS-1$
	public static final String ATTACHMENT_IS_NOT_SELECTED = "0"; //$NON-NLS-1$

	public static String[] getNotesAttachmentNames(Document doc) {
		String[] attachmentNames = null;

		if (doc != null) {
			List<String> nameList = new ArrayList<String>();
			try {
				Item item = doc.getFirstItem("Body"); //$NON-NLS-1$

				if (item instanceof RichTextItem) {
					RichTextItem body = (RichTextItem) item;
					if (body != null) {
						Vector v = body.getEmbeddedObjects();
						if (v != null) {
							Enumeration e = v.elements();
							while (e.hasMoreElements()) {
								EmbeddedObject eo = (EmbeddedObject) e.nextElement();
								if (eo.getName() != null) {
									// Use the 1st byte as select flag - used in AssociateDialog class... Lazy way to avoid creating an
									// Object.
									StringBuffer sb = new StringBuffer(ATTACHMENT_IS_SELECTED);
									sb.append(eo.getName());
									nameList.add(sb.toString());
								}
							}
						}
					}
				}
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
			}
			if (!nameList.isEmpty()) {
				attachmentNames = nameList.toArray(new String[nameList.size()]);
			}
		}
		return attachmentNames;
	}

	public static String javaDateToString(Date dt, String pattern) {
		String dtX = null;
		if (dt != null) {
			try {
				SimpleDateFormat outFormatter = new SimpleDateFormat(pattern);
				dtX = outFormatter.format(dt);
			} catch (Exception e) {
				UtilsPlugin.getDefault().logException(e, UiPluginActivator.PLUGIN_ID);
			}
		}
		return dtX;
	}

	// should obsolete this method
	public static void webServicesLog(String msg, String request, String output) {
	}

	public static void log(String msg) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (msg != null) {
			sb.append(msg);
		}
		UtilsPlugin.getDefault().logInfoMessage(sb.toString(), UiPluginActivator.PLUGIN_ID);
		// System.out.println(sb.toString());
	}

	public static void posterLog(String msg, String uri, String contentType, String posterString) {
		StringBuffer sb = new StringBuffer(ConstantStrings.EMPTY_STRING);
		if (msg != null && uri != null && contentType != null && posterString != null) {
			sb.append("... Poster log for ").append(msg).append(" ...\n") //$NON-NLS-1$ //$NON-NLS-2$
					.append("URL: ").append(uri).append("\nContent Type: ").append(contentType).append("\n").append(posterString); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			UtilsPlugin.getDefault().logInfoMessage(sb.toString(), UiPluginActivator.PLUGIN_ID);
		}
	}
}
