package com.ibm.socialcrm.notesintegration.sametime.utils.views;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.ibm.collaboration.realtime.people.Group;
import com.ibm.collaboration.realtime.people.Person;
import com.ibm.socialcrm.notesintegration.sametime.utils.AbstractSametimeGroup;
import com.ibm.socialcrm.notesintegration.sametime.utils.Member;
import com.ibm.socialcrm.notesintegration.sametime.utils.SametimeUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;

public class SametimeLabelProvider extends LabelProvider implements IColorProvider, IFontProvider {
	private Font boldFont;
	private Color groupLabelColor;

	@Override
	public Color getBackground(Object obj) {
		return null;
	}

	@Override
	public Color getForeground(Object obj) {
		Color color = null;
		if (obj instanceof Member) {
			Person person = ((Member) obj).getSametimePerson();
			if (person.getStatus() == Person.STATUS_AVAILABLE || person.getStatus() == Person.STATUS_AVAILABLE_MOBILE || person.getStatus() == Person.STATUS_AWAY
					|| person.getStatus() == Person.STATUS_AWAY_MOBILE || person.getStatus() == Person.STATUS_IN_MEETING || person.getStatus() == Person.STATUS_IN_MEETING_MOBILE) {
				color = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN);
			}
		} else if (obj instanceof AbstractSametimeGroup || obj instanceof Group) {
			color = getGroupLabelColor();
		}
		return color;
	}

	@Override
	public Font getFont(Object obj) {
		Font font = Display.getDefault().getSystemFont();

		if (obj instanceof Member) {
			Person person = ((Member) obj).getSametimePerson();
			if (person.getStatus() == Person.STATUS_AVAILABLE || person.getStatus() == Person.STATUS_AVAILABLE_MOBILE) {
				font = getBoldFont();
			}
		} else if (obj instanceof AbstractSametimeGroup || obj instanceof Group) {
			font = getBoldFont();
		}
		return font;
	}

	@Override
	public String getText(Object obj) {
		String text = ConstantStrings.EMPTY_STRING;
		if (obj instanceof Member) {
			text = ((Member) obj).getName();
		} else if (obj instanceof AbstractSametimeGroup) {
			AbstractSametimeGroup group = ((AbstractSametimeGroup) obj);
			text = group.getName();
			text += " (" + group.getOnlinePersonCount() + ConstantStrings.FORWARD_SLASH + group.getPersonCount() + ")"; //$NON-NLS-1$//$NON-NLS-2$
		} else {
			text = obj.toString();
		}
		return text;
	}

	@Override
	public Image getImage(Object obj) {
		Image img = SFAImageManager.getImage(SFAImageManager.BLANK_ICON);

		if (obj instanceof Member) {
			Person person = ((Member) obj).getSametimePerson();
			if (person != null) {
				img = SametimeUtils.getSametimeStatusImage(person);
			}
		} else if (obj instanceof AbstractSametimeGroup || obj instanceof Group) {
			img = SFAImageManager.getImage(SFAImageManager.SAMETIME_PUBLIC_GROUP);
		}
		return img;
	}

	private Font getBoldFont() {
		if (boldFont == null) {
			Font font = Display.getDefault().getSystemFont();
			boldFont = new Font(Display.getDefault(), new FontData(font.getFontData()[0].getName(), font.getFontData()[0].getHeight(), SWT.BOLD));
		}
		return boldFont;
	}

	private Color getGroupLabelColor() {
		if (groupLabelColor == null) {
			groupLabelColor = new Color(Display.getDefault(), 57, 87, 122);
		}
		return groupLabelColor;
	}

	@Override
	public void dispose() {
		getBoldFont().dispose();
		getGroupLabelColor().dispose();
	}

}
