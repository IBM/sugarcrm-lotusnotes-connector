package com.ibm.socialcrm.notesintegration.utils;

/****************************************************************
 * IBM OpenSource
 *
 * (C) Copyright IBM Corp. 2012
 *
 * Licensed under the Apache License v2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 ***************************************************************/

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

public class SFAImageManager {
	private ImageRegistry registry;

	private static SFAImageManager instance = null;

	private static final String BASE_DIR = "icons/"; //$NON-NLS-1$
	private static final String INDUSTRY_DIR = BASE_DIR + "industryIcons/"; //$NON-NLS-1$

	public static final String REFRESH_ICON = BASE_DIR + "refresh.gif"; //$NON-NLS-1$
	public static final String PROGRESS_ICON = BASE_DIR + "progress.gif"; //$NON-NLS-1$
	public static final String SYNC_ICON = BASE_DIR + "synced.gif"; //$NON-NLS-1$
	public static final String ACCOUNTS_ICON = BASE_DIR + "account.gif"; //$NON-NLS-1$
	public static final String CONTACT_ICON = BASE_DIR + "contact.gif"; //$NON-NLS-1$
	public static final String OPPORTUNITY_ICON = BASE_DIR + "opportunity.gif"; //$NON-NLS-1$
	public static final String DOWNLOAD_ICON = BASE_DIR + "download.gif"; //$NON-NLS-1$
	public static final String SETTINGS_ICON = BASE_DIR + "settings.gif"; //$NON-NLS-1$
	public static final String BACK_ICON = BASE_DIR + "back.gif"; //$NON-NLS-1$
	public static final String BACK_DISABLED_ICON = BASE_DIR + "back_disabled.gif"; //$NON-NLS-1$
	public static final String FORWARD_ICON = BASE_DIR + "forward.gif"; //$NON-NLS-1$
	public static final String FORWARD_DISABLED_ICON = BASE_DIR + "forward_disabled.gif"; //$NON-NLS-1$
	public static final String STOP_ICON = BASE_DIR + "stop.gif"; //$NON-NLS-1$
	public static final String STOP_DISABLED_ICON = BASE_DIR + "stop_disabled.gif"; //$NON-NLS-1$
	public static final String BLANK_ICON = BASE_DIR + "blank.gif"; //$NON-NLS-1$
	public static final String CHAT_ICON = BASE_DIR + "chat.png"; //$NON-NLS-1$
	public static final String ANNOUNCE_ICON = BASE_DIR + "ST_Announce.png"; //$NON-NLS-1$  
	public static final String SAMETIME_ADD_USER = BASE_DIR + "ST_Add_User.png"; //$NON-NLS-1$
	public static final String SAMETIME_PRIVATE_GROUP = BASE_DIR + "ST_Personal_Group.png"; //$NON-NLS-1$
	public static final String SAMETIME_PUBLIC_GROUP = BASE_DIR + "ST_Public_Group.png"; //$NON-NLS-1$
	public static final String SAMETIME_ICON = BASE_DIR + "sametime_icon.png"; //$NON-NLS-1$
	public static final String CLICK_TO_CALL = BASE_DIR + "click_to_call.png"; //$NON-NLS-1$
	public static final String MOBILE_PHONE_ICON = BASE_DIR + "icon_mobilePhone.png"; //$NON-NLS-1$
	public static final String ADD_TO_FAVORITES = BASE_DIR + "addToFavorites.gif"; //$NON-NLS-1$
	public static final String ADD_TO_FAVORITES_DISABLED = BASE_DIR + "addToFavoritesDisabled.gif"; //$NON-NLS-1$
	public static final String DELETE = BASE_DIR + "delete.gif"; //$NON-NLS-1$
	public static final String DELETE_DISABLED = BASE_DIR + "deleteDisabled.gif"; //$NON-NLS-1$
	public static final String IENTERPRISES = BASE_DIR + "iEnterprise.gif"; //$NON-NLS-1$
	public static final String IENTERPRISES_DISABLED = BASE_DIR + "iEnterprise_disabled.gif"; //$NON-NLS-1$
	public static final String BLANK_PERSON_MALE = BASE_DIR + "blank_person_male.png"; //$NON-NLS-1$
	public static final String ERROR_ICON = BASE_DIR + "error.gif"; //$NON-NLS-1$
	public static final String EXTERNAL_LINK = BASE_DIR + "externalLink.png"; //$NON-NLS-1$  
	public static final String PHONE = BASE_DIR + "phone.gif"; //$NON-NLS-1$
	public static final String SWITCH_VIEWS = BASE_DIR + "Switch_Views.png"; //$NON-NLS-1$
	public static final String SALES_CONNECT = BASE_DIR + "SalesConnect_16.png"; //$NON-NLS-1$
	public static final String LARGE_ERROR = BASE_DIR + "largeError.png"; //$NON-NLS-1$
	public static final String ATTACHMENT = BASE_DIR + "attachment16.png"; //$NON-NLS-1$
	// pin icons
	public static final String PIN = BASE_DIR + "pin.gif"; //$NON-NLS-1$
	public static final String UNPIN = BASE_DIR + "unpin.gif"; //$NON-NLS-1$
	public static final String PINNED = BASE_DIR + "pinned.gif"; //$NON-NLS-1$
	public static final String REMOVE = BASE_DIR + "removeIcon.gif"; //$NON-NLS-1$

	// Industry icons
	public static final String INDUSTRY_OTHER = INDUSTRY_DIR + "Other.png"; //$NON-NLS-1$
	public static final String INDUSTRY_MULTIPLE = INDUSTRY_DIR + "MultipleIndustries.png"; //$NON-NLS-1$
	public static final String INDUSTRY_APPAREL = INDUSTRY_DIR + "Apparel.png"; //$NON-NLS-1$
	public static final String INDUSTRY_AEROSPACE = INDUSTRY_DIR + "Aerospace.png"; //$NON-NLS-1$
	public static final String INDUSTRY_AUTOMOTIVE = INDUSTRY_DIR + "Automotive.png"; //$NON-NLS-1$
	public static final String INDUSTRY_BANKING = INDUSTRY_DIR + "Banking.png"; //$NON-NLS-1$
	public static final String INDUSTRY_BIOTECH = INDUSTRY_DIR + "Biotechnology.png"; //$NON-NLS-1$
	public static final String INDUSTRY_CHEMICAL = INDUSTRY_DIR + "Chemicals.png"; //$NON-NLS-1$
	public static final String INDUSTRY_COMMUNICATIONS = INDUSTRY_DIR + "Communications.png"; //$NON-NLS-1$
	public static final String INDUSTRY_CONSTRUCTION = INDUSTRY_DIR + "Construction.png"; //$NON-NLS-1$
	public static final String INDUSTRY_CONSULTING = INDUSTRY_DIR + "Consulting.png"; //$NON-NLS-1$
	public static final String INDUSTRY_EDUCATION = INDUSTRY_DIR + "Education.png"; //$NON-NLS-1$
	public static final String INDUSTRY_ELECTRONICS = INDUSTRY_DIR + "Electronics.png"; //$NON-NLS-1$
	public static final String INDUSTRY_ENERGY = INDUSTRY_DIR + "Energy.png"; //$NON-NLS-1$
	public static final String INDUSTRY_ENGINEERING = INDUSTRY_DIR + "Engineering.png"; //$NON-NLS-1$
	public static final String INDUSTRY_ENTERTAINMENT = INDUSTRY_DIR + "Entertainment.png"; //$NON-NLS-1$
	public static final String INDUSTRY_ENVIRONMENTAL = INDUSTRY_DIR + "Environmental.png"; //$NON-NLS-1$
	public static final String INDUSTRY_FINANCE = INDUSTRY_DIR + "Finance.png"; //$NON-NLS-1$
	public static final String INDUSTRY_GOVERNMENT = INDUSTRY_DIR + "Government.png"; //$NON-NLS-1$
	public static final String INDUSTRY_HEALTHCARE = INDUSTRY_DIR + "Healthcare.png"; //$NON-NLS-1$
	public static final String INDUSTRY_HOSPITALITY = INDUSTRY_DIR + "Hospitality.png"; //$NON-NLS-1$
	public static final String INDUSTRY_INSURANCE = INDUSTRY_DIR + "Insurance.png"; //$NON-NLS-1$
	public static final String INDUSTRY_MACHINERY = INDUSTRY_DIR + "Machinery.png"; //$NON-NLS-1$
	public static final String INDUSTRY_MANUFACTURING = INDUSTRY_DIR + "Manufacturing.png"; //$NON-NLS-1$
	public static final String INDUSTRY_MEDIA = INDUSTRY_DIR + "Media.png"; //$NON-NLS-1$
	public static final String INDUSTRY_NON_PROFIT = INDUSTRY_DIR + "NotForProfit.png"; //$NON-NLS-1$  
	public static final String INDUSTRY_RECREATION = INDUSTRY_DIR + "Recreation.png"; //$NON-NLS-1$
	public static final String INDUSTRY_RETAIL = INDUSTRY_DIR + "Retail.png"; //$NON-NLS-1$
	public static final String INDUSTRY_SHIPPING = INDUSTRY_DIR + "Shipping.png"; //$NON-NLS-1$
	public static final String INDUSTRY_TECHNOLOGY = INDUSTRY_DIR + "Technology.png"; //$NON-NLS-1$
	public static final String INDUSTRY_TELECOM = INDUSTRY_DIR + "Telecom.png"; //$NON-NLS-1$
	public static final String INDUSTRY_TRANSPORTATION = INDUSTRY_DIR + "Transportation.png"; //$NON-NLS-1$
	public static final String INDUSTRY_UTILITIES = INDUSTRY_DIR + "Utilities.png"; //$NON-NLS-1$

	public static final String SEARCH = BASE_DIR + "search12.png"; //$NON-NLS-1$

	private static final String[] ALL_IMAGES = new String[] { REFRESH_ICON, PROGRESS_ICON, SYNC_ICON, ACCOUNTS_ICON, CONTACT_ICON, OPPORTUNITY_ICON, DOWNLOAD_ICON, SETTINGS_ICON, BACK_ICON,
			BACK_DISABLED_ICON, FORWARD_ICON, FORWARD_DISABLED_ICON, STOP_ICON, STOP_DISABLED_ICON, BLANK_ICON, CHAT_ICON, ANNOUNCE_ICON, SAMETIME_ADD_USER, SAMETIME_PRIVATE_GROUP,
			SAMETIME_PUBLIC_GROUP, SAMETIME_ICON, CLICK_TO_CALL, MOBILE_PHONE_ICON, ADD_TO_FAVORITES, ADD_TO_FAVORITES_DISABLED, DELETE, DELETE_DISABLED, IENTERPRISES, IENTERPRISES_DISABLED,
			BLANK_PERSON_MALE, ERROR_ICON, EXTERNAL_LINK, PHONE, SWITCH_VIEWS, SALES_CONNECT, LARGE_ERROR, ATTACHMENT, INDUSTRY_OTHER, INDUSTRY_MULTIPLE, INDUSTRY_AEROSPACE, INDUSTRY_APPAREL,
			INDUSTRY_AUTOMOTIVE, INDUSTRY_BANKING, INDUSTRY_BIOTECH, INDUSTRY_CHEMICAL, INDUSTRY_COMMUNICATIONS, INDUSTRY_CONSTRUCTION, INDUSTRY_CONSULTING, INDUSTRY_EDUCATION, INDUSTRY_ELECTRONICS,
			INDUSTRY_ENERGY, INDUSTRY_ENGINEERING, INDUSTRY_ENTERTAINMENT, INDUSTRY_ENVIRONMENTAL, INDUSTRY_FINANCE, INDUSTRY_GOVERNMENT, INDUSTRY_HEALTHCARE, INDUSTRY_HOSPITALITY,
			INDUSTRY_INSURANCE, INDUSTRY_MACHINERY, INDUSTRY_MANUFACTURING, INDUSTRY_MEDIA, INDUSTRY_NON_PROFIT, INDUSTRY_RECREATION, INDUSTRY_RETAIL, INDUSTRY_SHIPPING, INDUSTRY_TECHNOLOGY,
			INDUSTRY_TELECOM, INDUSTRY_TRANSPORTATION, INDUSTRY_UTILITIES, PIN, UNPIN, PINNED, REMOVE, SEARCH };

	public static final String SAMETIME_STATUS_FOLDER = BASE_DIR + "sametimeStatus/"; //$NON-NLS-1$

	public static final String ST_ACTIVE = SAMETIME_STATUS_FOLDER + "active.png"; //$NON-NLS-1$
	public static final String ST_AWAY = SAMETIME_STATUS_FOLDER + "away.png"; //$NON-NLS-1$
	public static final String ST_MEETING = SAMETIME_STATUS_FOLDER + "meeting.png"; //$NON-NLS-1$
	public static final String ST_DND = SAMETIME_STATUS_FOLDER + "dnd.png"; //$NON-NLS-1$
	public static final String ST_MOBILE_ACTIVE = SAMETIME_STATUS_FOLDER + "mobileActive.png"; //$NON-NLS-1$
	public static final String ST_MOBILE_AWAY = SAMETIME_STATUS_FOLDER + "mobileAway.png"; //$NON-NLS-1$
	public static final String ST_MOBILE_MEETING = SAMETIME_STATUS_FOLDER + "mobileMeeting.png"; //$NON-NLS-1$
	public static final String ST_MOBILE_DND = SAMETIME_STATUS_FOLDER + "mobileDnd.png"; //$NON-NLS-1$

	private static final String[] ST_STATUS_IMAGES = new String[] { ST_ACTIVE, ST_AWAY, ST_MEETING, ST_DND, ST_MOBILE_ACTIVE, ST_MOBILE_AWAY, ST_MOBILE_MEETING, ST_MOBILE_DND };

	// Singleton constructor
	private SFAImageManager() {
	}

	/**
	 * Singleton accessor
	 * 
	 * @return instance
	 */
	private static SFAImageManager getInstance() {
		if (instance == null) {
			instance = new SFAImageManager();
		}
		return instance;
	}

	private void setImageRegistry(ImageRegistry registry) {
		this.registry = registry;
	}

	/**
	 * Gets (and initializes, if necessary) the map of images
	 * 
	 * @return images store
	 */
	public static ImageRegistry getImageRegistry() {
		if (getInstance().registry == null) {
			ImageRegistry registry = new ImageRegistry(Display.getDefault());
			getInstance().setImageRegistry(registry);

			for (String image : ALL_IMAGES) {
				try {
					registry.put(image, new Image(Display.getDefault(), UtilsPlugin.getDefault().getBundle().getResource(image).openStream()));
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
				}
			}

			for (String image : ST_STATUS_IMAGES) {
				try {
					Image baseImage = new Image(Display.getDefault(), UtilsPlugin.getDefault().getBundle().getResource(image).openStream());
					registry.put(image, createSametimeStatusImage(baseImage));
					baseImage.dispose();
				} catch (Exception e) {
					UtilsPlugin.getDefault().logException(e, UtilsPlugin.PLUGIN_ID);
				}
			}

		}
		return getInstance().registry;
	}

	/**
	 * Returns the image resource identified by the given name. If the given name is not recognized, a blank image is returned.
	 * 
	 * @param name
	 * @return Image resource
	 */
	public static Image getImage(String name) {
		return (Image) getInstance().getImageRegistry().get(name);
	}

	/**
	 * We need 16x16 status images, but we don't want the base image to be 16x16. So we have to scale the images down to size on a blank background.
	 * 
	 * @param baseImage
	 * @return
	 */
	private static Image createSametimeStatusImage(final Image baseImage) {
		return new CompositeImageDescriptor() {
			protected void drawCompositeImage(int width, int height) {
				drawImage(getImage(BLANK_ICON).getImageData(), 0, 0);
				drawImage(baseImage.getImageData().scaledTo(8, 8), 4, 4);
			}

			protected Point getSize() {
				return new Point(16, 16);
			}
		}.createImage();
	}

}
