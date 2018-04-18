package com.ibm.socialcrm.notesintegration.ui.views;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.part.ViewPart;

import com.ibm.socialcrm.notesintegration.core.extensionpoints.DashboardContributionExtensionProcessor;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.ui.dashboardpanels.SugarItemsDashboard;
import com.ibm.socialcrm.notesintegration.ui.utils.UiUtils;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils;
import com.ibm.socialcrm.notesintegration.utils.NotesAccountManager;
import com.ibm.socialcrm.notesintegration.utils.SFAImageManager;
import com.ibm.socialcrm.notesintegration.utils.UtilsPlugin;
import com.ibm.socialcrm.notesintegration.utils.UtilsPluginNLSKeys;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;
import com.ibm.socialcrm.notesintegration.utils.widgets.SFAHyperlink;

public class NoSugarEntryViewPart extends ViewPart {

	public static final String VIEW_ID = "com.ibm.socialcrm.notesintegration.ui.view.noSugarEntryViewPart"; //$NON-NLS-1$
	public static final String SEARCH_TEXT = "searchText"; //$NON-NLS-1$
	public static final String SUGAR_TYPE = "sugarType"; //$NON-NLS-1$

	private String searchText = null;
	private SugarType type;
	private Composite parent;

	@Override
	public void init(IViewSite viewSite, IMemento input) throws PartInitException {

		super.init(viewSite, input);

		if (input != null) {
			searchText = input.getString(SEARCH_TEXT);
			String typeString = input.getString(SUGAR_TYPE);
			type = SugarType.valueOf(typeString);
			createPartControl(parent);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		if (searchText == null) {
			this.parent = parent;
		} else {
			Composite noEntryComposite = new Composite(this.parent, SWT.NONE);
			noEntryComposite.setLayout(GridLayoutFactory.fillDefaults().create());
			noEntryComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

			Label l = new Label(noEntryComposite, SWT.WRAP);
			l.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
			l.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
			l.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_NO_INFORMATION_IN_CRM_SYSTEM));

			Composite hyperLinkComposite = new Composite(noEntryComposite, SWT.NONE);
			hyperLinkComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).spacing(0, 0).create());
			hyperLinkComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

			SFAHyperlink hyperLink = new SFAHyperlink(hyperLinkComposite, SWT.NONE);
			hyperLink.setLayoutData(GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).create());
			hyperLink.setText(UtilsPlugin.getDefault().getResourceString(UtilsPluginNLSKeys.UI_SEARCH_CRM_SYSTEM));
			hyperLink.setForeground(JFaceColors.getHyperlinkText(Display.getCurrent()));
			hyperLink.setUnderlined(true);
			hyperLink.setFont(SugarItemsDashboard.getInstance().getNormalFontForBusinessCardData());
			hyperLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent arg0) {
					// 72458
					//					GenericUtils.launchUrlInPreferredBrowser(type.getSearchUrl(searchText) + (GenericUtils.isUseEmbeddedBrowserPreferenceSet() ? ConstantStrings.EMPTY_STRING : "&MSID=" //$NON-NLS-1$
					// + SugarWebservicesOperations.getInstance().getSessionId(true)), true);

					String aUrl = type.getSearchUrl(searchText);
					if (!GenericUtils.isUseEmbeddedBrowserPreferenceSet()) {
						aUrl = SugarWebservicesOperations.getInstance().buildV10SeamlessURL(aUrl);
					}
					GenericUtils.launchUrlInPreferredBrowser(aUrl, true);
				}
			});
			Label externalLinkLabel = new Label(hyperLinkComposite, SWT.NONE);
			externalLinkLabel.setImage(SFAImageManager.getImage(SFAImageManager.EXTERNAL_LINK));
			externalLinkLabel.setLayoutData(GridDataFactory.fillDefaults().indent(0, -3).create());

			UiUtils.recursiveSetBackgroundColor(noEntryComposite, JFaceColors.getBannerBackground(Display.getDefault()));
		}
	}

	@Override
	public void setFocus() {

	}

}
