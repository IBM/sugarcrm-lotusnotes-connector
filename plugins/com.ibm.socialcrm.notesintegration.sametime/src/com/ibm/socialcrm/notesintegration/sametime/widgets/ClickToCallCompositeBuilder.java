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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;

import com.ibm.collaboration.realtime.im.community.CommunityLifecycleEvent;
import com.ibm.collaboration.realtime.im.community.CommunityListener;
import com.ibm.collaboration.realtime.im.community.CommunityLoginEvent;
import com.ibm.collaboration.realtime.im.community.CommunityServiceEvent;
import com.ibm.collaboration.realtime.im.community.CommunityStatusEvent;
import com.ibm.socialcrm.notesintegration.sametime.utils.SametimeUtils;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFAHyperlink;

public class ClickToCallCompositeBuilder {
	public boolean createClickToCallComposite(Composite composite, final String phone, String labelText) {
		return createClickToCallComposite(composite, phone, labelText, 0);
	}

	public boolean createClickToCallComposite(Composite composite, final String phone, String labelText, int labelWidth) {
		boolean created = false;
		if (phone != null && !phone.equals(ConstantStrings.EMPTY_STRING)) {
			if (labelText != null) {
				Label phoneLabel = new Label(composite, SWT.NONE);
				phoneLabel.setFont(SugarItemsDashboard.getInstance().getBusinessCardLabelFont());
				phoneLabel.setForeground(SugarItemsDashboard.getInstance().getBusinessCardFieldLabelColor());
				phoneLabel.setText(labelText);
				if (labelWidth != 0 && labelWidth != -1) {
					phoneLabel.setLayoutData(GridDataFactory.fillDefaults().hint(labelWidth, SWT.DEFAULT).create());
				}
			}

			final SFAHyperlink hyperLink = new SFAHyperlink(composite, SWT.NONE, true);
			hyperLink.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).indent(0,
					GenericUtils.getPlatformHyperlinkVerticalIndent()).create());
			hyperLink.setText(phone);
			hyperLink.setForeground(SugarItemsDashboard.getInstance().getBusinessCardLinkColor());
			hyperLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
			hyperLink.setEnabled(SametimeUtils.areCallsEnabled());
			if (phone != null && !phone.equals(ConstantStrings.EMPTY_STRING)) {
				hyperLink.addHyperlinkListener(new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent arg0) {
						SametimeUtils.startCall(phone);
					}
				});
			}

			final CommunityListener communityListener = new CommunityListener() {
				@Override
				public void handleCommunityLifecycleEvent(CommunityLifecycleEvent arg0) {
					refreshLinkState();
				}

				@Override
				public void handleCommunityLoginEvent(CommunityLoginEvent arg0) {
					refreshLinkState();
				}

				@Override
				public void handleCommunityServiceEvent(CommunityServiceEvent arg0) {
					refreshLinkState();
				}

				@Override
				public void handleCommunityStatusEvent(CommunityStatusEvent arg0) {
					refreshLinkState();
				}

				private void refreshLinkState() {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							hyperLink.setEnabled(SametimeUtils.areCallsEnabled());
						}
					});
				}
			};

			SametimeUtils.getCommunityService().addCommunityListener(communityListener);

			hyperLink.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent arg0) {
					SametimeUtils.getCommunityService().removeCommunityListener(communityListener);
				}
			});
			created = true;
		}
		return created;
	}
}
