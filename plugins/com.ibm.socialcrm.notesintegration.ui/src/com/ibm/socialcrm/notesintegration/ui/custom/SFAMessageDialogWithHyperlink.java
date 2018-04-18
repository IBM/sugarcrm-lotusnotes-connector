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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;

import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFAHyperlink;

/*
 * Customized Button widget.  It appends an "X" to the button text, and it implements
 * a click listener which executes the action passed into this widget via constructor.
 */
public class SFAMessageDialogWithHyperlink {

	private static String HTML_HYPERLINK_START_TAG = "<a ";

	public static void open(String title, Image image, final String msg) {

		// acw - test messages
		// final String msg =
		// "You are not authorized to download the document as you are not a member of the CollaborationHub. For more information on CollaborationHubs see here: <a href=\"https://www.google.com\" target=\"_blank\">Visit CCH</a>.";
		// final String msg = "You are not provisioned.";
		// final String msg = "You are not authorized to download the document as you are not a member of the CollaborationHub. For more information on CollaborationHubs see your manager.";
		// final String msg =
		// "You are not authorized to download the document as you are not a member of the CollaborationHub. This is a long message so I can put some padding here to test the dialog length. If you can not see the bottom of this message, let me know.  For more information on CollaborationHubs see here: <a href=\"https://www.google.com\" target=\"_blank\">Visit CCH</a>.  This is the end of the message.";
		// final String msg =
		// "You are not authorized to download the document as you are not a member of the CollaborationHub. This is a long message so I can put some padding here to test the dialog length. If you can not see the bottom of this message, let me know.  For more information on CollaborationHubs see here: <a href=\"https://www.google.com\" target=\"_blank\">Visit CCH</a>.  This part is after the link, you might wonder why it is here.  Well, it is here to test if the text after the link will be display properly, and if it is properly displayed, you should see this part correctly.  I will stop right here, and this is the end of the message.";
		// final String msg =
		// "You are not authorized to upload the document as you are not a member of the CollaborationHub. You need to request access. <a href=https://w3.ibm.com/w3login.html?returnUrl=https://w3.ibm.com/workplace/myportal/odw/cch/public?hubuid=b63488f6-06e2-4d10-9ee1-8d570280236b target=_blank>Begin Access Request...</a>.";
		MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(), title, SFAImageManager.getImage(SFAImageManager.SALES_CONNECT), null, MessageDialog.WARNING,
				new String[]{IDialogConstants.OK_LABEL}, 0) {

			// 38857 - allow re-sizable in case message is very long
			@Override
			protected boolean isResizable() {
				return true;
			}

			@Override
			protected Control createCustomArea(Composite composite) {

				String msg1 = msg;

				// ... extract url and url text
				final List linkUrls = new ArrayList();
				final List linkTexts = new ArrayList();
				Pattern p = Pattern.compile("href=\"(.*?)\".*>(.*)</a>");
				Matcher m = p.matcher(msg1);
				if (m.find()) {

					linkUrls.add(m.group(1)); // link URL
					if (m.groupCount() > 1) {
						linkTexts.add(m.group(2)); // link text
					}
				} else {
					Pattern p2 = Pattern.compile("href=(.*?)>(.+?)</a>");
					Matcher m2 = p2.matcher(msg1);
					if (m2.find()) {
						linkUrls.add(m2.group(1)); // link URL
						if (m2.groupCount() > 1) {
							linkTexts.add(m2.group(2)); // link text
						}
					} else {
						Pattern p3 = Pattern.compile("href=(.*?)>(.+?)<\\/a>");
						Matcher m3 = p3.matcher(msg1);
						if (m3.find()) {
							linkUrls.add(m3.group(1)); // link URL
							if (m3.groupCount() > 1) {
								linkTexts.add(m3.group(2)); // link text
							}
						}
					}
				}

				// ... split msg
				String stringToBeProcessed = msg1.substring(0);
				String[] msgFragments = msg1.split("</a>");
				int indexOfFragmentToStart = 0;

				int hrefIndex = stringToBeProcessed.indexOf(HTML_HYPERLINK_START_TAG);

				// ... Text BEFORE first link url
				if (linkUrls.isEmpty() || hrefIndex > 0) {
					Text text = new Text(composite, SWT.None | SWT.WRAP);
					// 38857 - add hint to control the shell size
					text.setLayoutData(GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.BEGINNING).grab(true, false).hint(512, SWT.DEFAULT).create());
					text.setBackground(composite.getParent().getBackground());
					String textX = stringToBeProcessed;
					if (hrefIndex > 0) {
						textX = msgFragments[0].substring(0, msgFragments[0].indexOf(HTML_HYPERLINK_START_TAG));

					}
					text.setText(textX);
					indexOfFragmentToStart++;
				}

				if (!linkUrls.isEmpty()) {
					for (int i = 0; i < linkUrls.size(); i++) {
						// link
						SFAHyperlink settingsLink = new SFAHyperlink(composite, SWT.NONE | SWT.WRAP);
						settingsLink.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).grab(true, false).hint(512, SWT.DEFAULT).create());
						settingsLink.setForeground(JFaceColors.getHyperlinkText(Display.getCurrent()));
						settingsLink.setUnderlined(true);
						settingsLink.setText((String) linkTexts.get(i));
						final String linkUrl = (String) linkUrls.get(i);
						settingsLink.addHyperlinkListener(new HyperlinkAdapter() {
							@Override
							public void linkActivated(HyperlinkEvent e) {
								GenericUtils.launchUrlInPreferredBrowser(linkUrl, true);
							}
						});

						// text after link
						if (indexOfFragmentToStart < msgFragments.length) {
							Text text1 = new Text(composite, SWT.NONE | SWT.WRAP);
							text1.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).grab(true, false).hint(512, SWT.DEFAULT).create());
							text1.setBackground(composite.getBackground());
							int length = msgFragments[indexOfFragmentToStart].length();
							if (msgFragments[indexOfFragmentToStart].indexOf(HTML_HYPERLINK_START_TAG) != -1) {
								length = msgFragments[indexOfFragmentToStart].indexOf(HTML_HYPERLINK_START_TAG);
							}
							String textX = msgFragments[indexOfFragmentToStart].substring(0, length);
							text1.setText(textX);
							indexOfFragmentToStart++;
						}

					}
				}

				return composite;
			}
		};
		dialog.open();
	}
}