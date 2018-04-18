package com.ibm.socialcrm.notesintegration.ui.search;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.eclipse.swt.graphics.Image;

import com.ibm.rcp.search.engines.SearchEngineDelegate;
import com.ibm.rcp.search.engines.data.SearchLocation;
import com.ibm.rcp.search.engines.data.SearchQuery;
import com.ibm.rcp.search.engines.data.SearchSortOption;
import com.ibm.rcp.search.engines.results.SearchResult;
import com.ibm.rcp.search.engines.results.SearchResultPage;
import com.ibm.rcp.search.ui.ISearchResultLabelProvider;
import com.ibm.rcp.search.ui.SearchResultLabelProvider;
import com.ibm.siapi.search.Result;
import com.ibm.socialcrm.notesintegration.core.utils.SugarV10APIManager;
import com.ibm.socialcrm.notesintegration.core.utils.SugarWebservicesOperations;
import com.ibm.socialcrm.notesintegration.utils.ConstantStrings;
import com.ibm.socialcrm.notesintegration.utils.GenericUtils.SugarType;

public class SugarSearchEngineDelegate implements SearchEngineDelegate, ISearchResultLabelProvider {
	public static final String TYPE = "type"; //$NON-NLS-1$

	// This is a bit stupid, but since we aren't showing images for specific search result types (the UI spec didn't call
	// for them), we have a null image. However, even with a null image, the thing that renders the search results
	// will leave a bit of a gap on the left on the first line. This is just a spacer we place before the text to make
	// it line up. We're not creating the widgets so our options are limited.
	private static final String LEFT_SPACER = " "; //$NON-NLS-1$

	@Override
	public SearchLocation[] getLocations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchSortOption[] getSortOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchResultPage search(SearchQuery query) {
		SearchResultPage resultPage = new SearchResultPage();
		try {

			List<SugarSearchResult> resultList = new ArrayList<SugarSearchResult>();
			final Map<SugarSearchResult, String> clientIdMap = new HashMap<SugarSearchResult, String>();
			String searchResults = SugarV10APIManager.getInstance().getGlobalTypeaheadInfoFromWebservice(query.getText());
			JSONObject searchResultsJSON = new JSONObject(searchResults);
			JSONArray records = searchResultsJSON.getJSONArray("records");
			if (records != null && records.length() > 0) {
				Iterator i = records.iterator();
				while (i.hasNext()) {
					JSONObject v10 = (JSONObject) i.next();
					String moduleX = v10.getString("_module");
					if (moduleX != null) {
						if (moduleX.equalsIgnoreCase("contacts")) {
							extractContactResults(v10, resultPage);
						} else if (moduleX.equalsIgnoreCase("opportunities")) {
							extractOpportunityResults(v10, resultPage);
						} else if (moduleX.equalsIgnoreCase("accounts")) {
							extractAccountResults(v10, clientIdMap, resultList);
						}
					}
				}

				if (clientIdMap.size() > 0) {
					{
						Collections.sort(resultList, new Comparator<SugarSearchResult>() {
							// Sort results based on the client id where DC is first, SC second, then S third.
							// Within each group, sort alphabetically on name.
							@Override
							public int compare(SugarSearchResult result1, SugarSearchResult result2) {
								int compare = 0;
								String client1Id = clientIdMap.get(result1);
								String client2Id = clientIdMap.get(result2);

								int client1Type = getValueForClientId(client1Id);
								int client2Type = getValueForClientId(client2Id);

								if (client1Type > client2Type) {
									compare = 1;
								} else if (client1Type < client2Type) {
									compare = -1;
								} else if (client1Type == client2Type) {
									compare = result1.getName().compareTo(result2.getName());
								}

								return compare;
							}

							// Returns 0 for a client id that starts with DC
							// 1 if it starts with SC
							// 2 if it starts with S
							// 3 if there's no prefix (should never happen
							private int getValueForClientId(String clientId) {
								int value = 3;
								if (clientId.toUpperCase().startsWith("DC")) //$NON-NLS-1$
								{
									value = 0;
								} else if (clientId.toUpperCase().startsWith("SC")) //$NON-NLS-1$
								{
									value = 1;
								} else if (clientId.toUpperCase().startsWith("S")) //$NON-NLS-1$
								{
									value = 2;
								}
								return value;
							}
						});

						// After they've been sorted, add them in order
						for (SugarSearchResult aResult : resultList) {
							resultPage.add(aResult);
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultPage;
	}

	/**
	 * Extracts the contact search results and adds entries to the given search page
	 * 
	 * @param resultPage
	 * @param results
	 * @throws JSONException
	 */
	private void extractContactResults(JSONObject result, SearchResultPage resultPage) throws JSONException {
		String id = result.getString(ConstantStrings.DATABASE_ID);
		String firstName = getJSONValue(result, ConstantStrings.DATABASE_FIRST_NAME);
		String lastName = getJSONValue(result, ConstantStrings.DATABASE_LAST_NAME);
		String name = getJSONValue(result, ConstantStrings.DATABASE_NAME);
		String title = getJSONValue(result, ConstantStrings.DATABASE_TITLE);
		String account = getJSONValue(result, ConstantStrings.ACCOUNT_NAME);
		String city = getJSONValue(result, ConstantStrings.DATABASE_PRIMARY_ADDRESS_CITY);
		String state = getJSONValue(result, ConstantStrings.DATABASE_PRIMARY_ADDRESS_STATE);
		String country = getJSONValue(result, ConstantStrings.DATABASE_PRIMARY_ADDRESS_COUNTRY);

		String jobText = LEFT_SPACER;
		if (title != null && title.length() > 0) {
			jobText = LEFT_SPACER + title;
		} else {
			// jobText += account;
		}
		String addressString = buildAddressString(city, state, country);
		String fullName = firstName + " " + lastName; //$NON-NLS-1$

		SearchResult searchResult = new SearchResult(SearchResult.TYPE_CONTACT, name, jobText, ConstantStrings.EMPTY_STRING, null, addressString);
		searchResult.setProperty(TYPE, SugarType.CONTACTS.toString());
		resultPage.add(new SugarSearchResult(searchResult, id, name));
	}

	/**
	 * Extracts the oppty search results and adds entries to the given search page
	 * 
	 * @param resultPage
	 * @param results
	 * @throws JSONException
	 */
	private void extractOpportunityResults(JSONObject result, SearchResultPage resultPage) throws JSONException {

		String id = result.getString(ConstantStrings.DATABASE_ID);
		String name = getJSONValue(result, ConstantStrings.DATABASE_NAME);
		String date = LEFT_SPACER + result.getString(ConstantStrings.DATABASE_DATE_CLOSED);
		String description = LEFT_SPACER + filterLineBreaker(result.getString(ConstantStrings.DATABASE_DESCRIPTION));

		SearchResult searchResult = new SearchResult(SearchResult.TYPE_WEB, name, description, ConstantStrings.EMPTY_STRING, null, date);
		searchResult.setProperty(TYPE, SugarType.OPPORTUNITIES.toString());
		resultPage.add(new SugarSearchResult(searchResult, id, name));

	}

	private String getJSONValue(JSONObject result, String jsonKey) {
		String returnX = ConstantStrings.EMPTY_STRING;
		try {
			if (result != null && jsonKey != null) {
				if (result.containsKey(jsonKey)) {
					returnX = result.getString(jsonKey);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return returnX;
	}

	private String filterLineBreaker(String s) {
		String outString = s;
		if (s != null) {
			outString = s.replaceAll("\\n", ConstantStrings.SPACE); //$NON-NLS-1$
			outString = outString.replaceAll("\\r", ConstantStrings.SPACE); //$NON-NLS-1$
			outString = outString.replaceAll("\\t", ConstantStrings.SPACE); //$NON-NLS-1$
		}
		return outString;
	}

	/**
	 * Extracts the account search results and adds entries to the given search page
	 * 
	 * @param resultPage
	 * @param results
	 * @throws JSONException
	 */
	private void extractAccountResults(JSONObject result, Map<SugarSearchResult, String> clientIdMap, List<SugarSearchResult> resultList) throws JSONException {

		String id = result.getString(ConstantStrings.DATABASE_ID);
		String name = getJSONValue(result, ConstantStrings.DATABASE_NAME).trim();
		if (name.endsWith(ConstantStrings.DASH)) {
			name = name.substring(0, name.lastIndexOf(ConstantStrings.DASH)).trim();
		}
		String clientId = getJSONValue(result, ConstantStrings.DATABASE_CCMS_ID);

		String city = getJSONValue(result, ConstantStrings.DATABASE_BILLING_ADDRESS_CITY);
		String state = getJSONValue(result, ConstantStrings.DATABASE_BILLING_ADDRESS_STATE);
		String country = getJSONValue(result, ConstantStrings.DATABASE_BILLING_ADDRESS_COUNTRY);

		String industryString = LEFT_SPACER;
		
		// 86811
//		// 48763 - if industry is an Object
//		if (result.containsKey(ConstantStrings.DATABASE_INDUS_CLASS_ROLLUP)) {
//			boolean isObject = true;
//			try {
//				JSONObject industryObject = result.getJSONObject(ConstantStrings.DATABASE_INDUS_CLASS_ROLLUP);
//			} catch (Exception e) {
//				isObject = false;
//			}
//			if (isObject) {
//				JSONObject industryObject = result.getJSONObject(ConstantStrings.DATABASE_INDUS_CLASS_ROLLUP);
//				Iterator it = industryObject.keys();
//				while (it.hasNext()) {
//					String key = (String) it.next();
//					industryString += getIndustryEnumValue((String) industryObject.get(key));
//				}
//			} else {
//
//				JSONArray industryArray = result.getJSONArray(ConstantStrings.DATABASE_INDUS_CLASS_ROLLUP);
//
//				// TODO: This is just temporary code. Once the industry web services are in place, we can add
//				// the proper strings
//				// String industryString = LEFT_SPACER;
//				for (int j = 0; j < industryArray.length(); j++) {
//
//					String industryCode = (String) industryArray.get(j);
//					industryString += getIndustryEnumValue(industryCode);
//					if (j < industryArray.length() - 1) {
//						industryString += ", "; //$NON-NLS-1$
//					}
//				}
//			}
//		}
		
		if (result.containsKey(ConstantStrings.DATABASE_INDUS_CLASS_NAME)) {
			String key=result.getString(ConstantStrings.DATABASE_INDUS_CLASS_NAME);
					industryString += getIndustryEnumValue(key);
				 
		}
		

		String addressString = buildAddressString(null, /* city, */state, country);
		SearchResult searchResult = new SearchResult(SearchResult.TYPE_WEB, name, addressString, ConstantStrings.EMPTY_STRING, null, industryString);
		searchResult.setProperty(TYPE, SugarType.ACCOUNTS.toString());
		SugarSearchResult sugarSearchResult = new SugarSearchResult(searchResult, id, name);
		resultList.add(sugarSearchResult);
		clientIdMap.put(sugarSearchResult, clientId);

	}

	/**
	 * Builds a printable address string
	 * 
	 * @param city
	 * @param state
	 * @param country
	 * @return
	 */
	private String buildAddressString(String city, String state, String country) {
		boolean hasCity = (city != null && (city.length() > 0));
		boolean hasState = (state != null && (state.length() > 0));
		boolean hasCountry = (country != null && (country.length() > 0));

		// The space is a hack to make the results line up a little better.
		String address = LEFT_SPACER;
		if (hasCity) {
			address += city;
			if (hasState) {
				address += ", " + state; //$NON-NLS-1$
			}
			if (hasCountry && hasState) {
				address += " " + country; //$NON-NLS-1$
			}
			if (hasCountry && !hasState) // Sugar shouldn't allow this condition. But, just in case
			{
				address += ", " + country; //$NON-NLS-1$
			}
		} else {
			if (hasState) {
				address += state;
				if (hasCountry) {
					address += ", " + country; //$NON-NLS-1$
				}
			} else if (hasCountry) {
				address += country;
			}
		}
		return address;
	}

	@Override
	public void setData(String arg0) {
	}

	private SearchResultLabelProvider defaultLabelProvider = new SearchResultLabelProvider();

	@Override
	public Image getDisplayImage(Result result) {
		return null;
	}

	@Override
	public String getDisplayName(Result result, int arg1) {
		return defaultLabelProvider.getDisplayName(result, arg1);
	}

	@Override
	public int[] getDisplayProperties() {
		return defaultLabelProvider.getDisplayProperties();
	}

	@Override
	public String getDisplayStyle(Result result, int arg1) {
		// Yes, these are some bizarre magic numbers/strings, but I can't find the actual constants that represent them (heck, I'm not even
		// sure what arg1 is supposed to be). But, apparently, we get -2 when rendering the first element so we want to appear in
		// the link blue. So we use the default provider's style.
		if (arg1 == -2) {
			return defaultLabelProvider.getDisplayStyle(result, arg1);
		}
		// For all other cases, we return this. Again, I couldn't find a constant for this.
		return "searchResultDescription"; //$NON-NLS-1$
	}

	@Override
	public String getDisplayValue(Result result, int arg1) {
		return defaultLabelProvider.getDisplayValue(result, arg1);
	}

	@Override
	public String getHTMLText(Result result) {
		return defaultLabelProvider.getHTMLText(result);
	}

	@Override
	public String getResultStyle(Result result) {
		return defaultLabelProvider.getResultStyle(result);
	}

	@Override
	public String getResultStyleID(Result result) {
		return defaultLabelProvider.getResultStyleID(result);
	}

	@Override
	public String getText(Result result) {
		return defaultLabelProvider.getText(result);
	}

	JSONObject industryJSONObject = null;
	private String getIndustryEnumValue(String industryCode) {
		String industryValue = ConstantStrings.EMPTY_STRING;
		if (industryJSONObject == null) {
			try {
				String industryEnumResults = SugarV10APIManager.getInstance().getEnumFromAPI("Accounts", "indus_class_name");
				industryJSONObject = new JSONObject(industryEnumResults);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (industryJSONObject != null && industryJSONObject.containsKey(industryCode)) {
			try {
				industryValue = industryJSONObject.getString(industryCode);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return industryValue;
	}
}
