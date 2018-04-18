<?php
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

require_once('service/v4_ibm/SugarWebServiceImplv4_ibm.php');
//require_once('service/core/SugarRestServiceImpl.php'); //switching from v2 to v4
require_once('custom/scrmsugarws/v2_1/sfaError.php');
require_once("NotesLogger.php");

class SugarRestServiceImpl_v2_custom extends SugarWebServiceImplv4_ibm
{
	public $NOTES_LOGGER;
	
	//SFA Notes Protocol Version
	public static $PROTOCOL_VERSION = "1.0.0.0";

	//minimum number of characters search strings must have
	public static $TYPEAHEAD_CHAR_LIMIT = 2;

	//percentage of php max_execution_time to use for getRegexes/getRegex call -- from 0 - 1
	public static $NOTES_MAX_EXECUTION_FACTOR = 0.9;

	//maximum number of myitems we will process
	public static $NOTES_MAX_NUM_MYITEMS_CONTACTS = 5000;
	public static $NOTES_MAX_NUM_MYITEMS_ACCOUNTS = 5000;
	public static $NOTES_MAX_NUM_MYITEMS_TYPEAHEADS = 5000;
	
	//default EU contact filter information
	public static $EU_FILTER_LOCATION = "/opt/filter/filter/bin/filter";
	public static $EU_FILTER_HOST = "localhost";
	public static $EU_FILTER_PORT = "50002";
	
	//if result limit isn't specified, need to use a reasonably high one for myitems post-processing
	public static $RESULT_LIMIT = 20000;
	
	//name of the hadr config name -- should be 'reports'
	public static $HADR_INSTANCE_NAME = 'reports';
	
	private $noteshadr;
	
	function __construct() {
		$this->NOTES_LOGGER = new NotesLogger();
				
		parent::__construct();
	}

	/**
	 * Build and return the webservice header array.
	 *
	 * @param string $session - authenticated session id
	 */
	function getSFAHeader($session) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);

		if(!$this->checkAccess($session)) return;
		$ret_array = $this->buildHeader($session);

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}

	/**
	 * Get a regular expression composed of a users' My Items, and associated 'tags' for a given module
	 *
	 * This is a public method available through the webservice.
	 *
	 * @param string $session - authenticated session id
	 * @param string $module - the module to return regex for (currently 'Contacts' or 'Accounts')
	 * @return array(regexString, array(moduleTags))
	 */
	function getRegex($session, $module) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);

		if(!$this->checkAccess($session)) return;
		
		require_once('helpers/notesHelper_GetRegex.php');
		$getRegexHelper = new notesHelper_GetRegex($this->NOTES_LOGGER);
		
		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;
		
		$resultsArray = $getRegexHelper->getResult($module);		
		if(!empty($resultsArray)) {
			$ret_array['regexData'] = $resultsArray;
		} else {
			$ret_array['regexData'] = array();
		}
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}

	function getInfo13($session, $input) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);

		if(!$this->checkAccess($session)) return;
		
		require_once('helpers/notesHelper_GetInfo.php');
		$getInfoHelper = new notesHelper_GetInfo($this->NOTES_LOGGER);
		
		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;
		
		$resultsArray = $getInfoHelper->getResult($input);		
		if(!empty($resultsArray)) {
			$ret_array['result'] = $resultsArray;
		} else {
			$ret_array['result'] = array();
		}
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}
	
	function getRelatedActivities($session, $input) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);

		if(!$this->checkAccess($session)) return;
		
		require_once('helpers/notesHelper_GetRelatedActivities.php');
		$getRelatedActivitiesHelper = new notesHelper_GetRelatedActivities($this->NOTES_LOGGER);
		
		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;
		
		$resultsArray = $getRelatedActivitiesHelper->getResult($input);		
		if(!empty($resultsArray)) {
			$ret_array['result'] = $resultsArray;
		} else {
			$ret_array['result'] = array();
		}
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}
	
	/**
	 * This is a public method available thorugh the webservice.
	 *
	 * @param string $session - authenticated session id
	 * @param array @accountids - array of string account uuids
	 * @param array @contactids - array of string contact uuids
	 * @param array @opptyids - array of string oppty uuids
	 * @param array @opptynames - array of string oppty names
	 * @param array @clientids - array of string client ids (for looking up account)
	 * @return output info array
	 */
	function getInfo($session, $accountids, $contactids, $opptyids, $contactnames, $contactemails, $opptynames, $clientids) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);

		if(!$this->checkAccess($session)) return;

		global $current_user;
		$userid = $current_user->id; //get current user's uuid

		$this->NOTES_LOGGER->logMessage("info", __METHOD__." getInfo request from user $userid");

		//array being returned -- will contain accounts/members/opportunities info
		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;

		//get a list of my opportunities to be used as filter on accounts->oppties
		//$this->NOTES_LOGGER->logMessage("debug", __METHOD__." asking ibmhelper for MyItems Opportunities");
		//$myOpptiesArray = IBMHelper::getClass('MyItems')->getMyItems('Opportunities', $userid, array());
		$myOpptiesArray = array();
		
		//get a list of my contacts to be used as filter on searching contacts by name
		//$this->NOTES_LOGGER->logMessage("debug", __METHOD__." asking ibmhelper for MyItems Contacts");
		//$myContactsArray = IBMHelper::getClass('MyItems')->getMyItems('Contacts', $userid, array());
		$myContactsArray = array();
		
		//lookup account uuids by name 11337 - jdjohnso 2/10/12
		//this approach requires loading the beans twice (once for searching, then again to get info) -- might need to be changed
		//TODO: log if no results?
		foreach($clientids as $clientid) {
			$resultList = $this->getBeansByName('Accounts', $clientid);
			if(isset($resultList)) {
				foreach($resultList as $result) {
					if(!is_null($result->id)) {
						$accountids[] = $result->id;
					}
				}
			}
		}

		//process input account IDs
		foreach($accountids as $accountid) {
			$this->NOTES_LOGGER->logMessage("debug", __METHOD__." processing account $accountid");
			if(!is_null($accountid)) {

				$accountBean = $this->getBean($accountid, 'Accounts');

				//assuming that if the bean's name is null, then it's an empty bean
				if(!is_null($accountBean->name)) {
					//35799: &nbsp; decoded makes json_encode unhappy
					$ret_array['accounts'][$accountid]['name'] = html_entity_decode(str_replace('&nbsp;', ' ', $accountBean->name));

					//15622 - industry is more complicated now: there can be multiples, and we need to look up the values
					//$ret_array['accounts'][$accountid]['industry'] = $accountBean->industry;
					$ret_array['accounts'][$accountid]['industry'] = $this->getAccountIndustryData($accountBean);

					$ret_array['accounts'][$accountid]['website'] = $accountBean->website;

					//work item 3491: adding client id in - jdjohnso 2/10/12
					//changing client_id to ccms_id 15937
					$ret_array['accounts'][$accountid]['clientid'] = $accountBean->ccms_id;

					$ret_array['accounts'][$accountid]['clientrep']['id'] = $accountBean->assigned_user_id;
					$ret_array['accounts'][$accountid]['clientrep']['name'] = html_entity_decode($accountBean->assigned_user_name,ENT_QUOTES); //3178

					//14497: changing exisiting address fields to pull primary physical address
					$ret_array['accounts'][$accountid]['pri_physical_street'] = $accountBean->billing_address_street;
					$ret_array['accounts'][$accountid]['pri_physical_city'] = $accountBean->billing_address_city;
					$stateKey = $accountBean->billing_address_state;
					if(!empty($stateKey)) {
						$stateKey = $this->getAVLValue('state_list', $stateKey, 'en_us', true); //note have to lookup by sugar avl key, ibm dictionary is STATE_ABBREV_SFA
					}
					$ret_array['accounts'][$accountid]['pri_physical_state'] = $stateKey;
					$ret_array['accounts'][$accountid]['pri_physical_postalcode'] = $accountBean->billing_address_postalcode;
					$ret_array['accounts'][$accountid]['pri_physical_country'] = $accountBean->billing_address_country;

					$ret_array['accounts'][$accountid]['phone_office'] = $accountBean->phone_office;
					$ret_array['accounts'][$accountid]['phone_fax'] = $accountBean->phone_fax;

					//get account tags #2635 - jdjohnso 7/19/11
					$tagArray = array();
					$this->NOTES_LOGGER->logMessage("debug", __METHOD__." asking ibmhelper for tags for bean $accountid");
					$recordTags = IBMHelper::getClass('Tags')->getRecordTags($accountBean);
					foreach($recordTags as $tag) {
						$tagArray[] = html_entity_decode($tag,ENT_QUOTES); //3178
					}
					$ret_array['accounts'][$accountid]['tags'] = $tagArray;
					//

					$isFollowedResult = $this->checkIfFollowing('Accounts', $accountid);
					$ret_array['accounts'][$accountid]['followed'] = $isFollowedResult;
					
					//get account oppties #2972 - jdjohnso 7/19/11
					//changing logic to get all oppties from account + child accounts 20370 - 8/7/12
					$accountOppties = array();
					$accountOpptiesByDate = array();

					$allOpptyIDs = IBMHelper::getClass('Accounts')->getAllOppIDs($accountBean, false); //get account opportunities

					foreach($allOpptyIDs as $opptyidtemp) {
						if(array_key_exists($opptyidtemp, $myOpptiesArray)) { //filter against my items
							$opptyBean = $this->getBean($opptyidtemp, 'Opportunities'); //load oppty bean
							$opptydate = $opptyBean->date_closed;
							$opptystage = $opptyBean->sales_stage;
							if($opptystage < 7) { //only show open opportunities
								$accountOpptiesByDate[$opptyidtemp] = $opptydate; //remember id to date mapping
							}
						}
					}

					$this->NOTES_LOGGER->logMessage("debug", __METHOD__." sorting account oppties");
					asort($accountOpptiesByDate); //sort based on date

					//add first five (or less) sorted oppties to array //29722 - changing to 50
					$opptyLimit = 50;
					$totaloppties = count($accountOpptiesByDate);
					if($totaloppties < $opptyLimit) {
						$opptyLimit = $totaloppties;
					}

					$accountOpptiesByDate = array_keys($accountOpptiesByDate);

					for($i = 0; $i < $opptyLimit; $i++) {
						$opptyid = $accountOpptiesByDate[$i];
						$accountOppties[] = $opptyid;
						if(!array_key_exists($opptyid, $opptyids)) { //if not already being asked for, (this check might not be needed)
							$opptyids[] = $opptyid; //add the opportunity to the id list to return info for
						}
					}

					$ret_array['accounts'][$accountid]['opportunitiesTotal'] = $totaloppties;
					$ret_array['accounts'][$accountid]['opportunities'] = $accountOppties;
					//

				} else {
					$this->NOTES_LOGGER->logMessage("warn", __METHOD__." $accountid account bean is empty!");
				}
			}
		}

		//lookup contact uuids by name 2653 - jdjohnso 7/20/11
		//this approach requires loading the beans twice (once for searching, then again to get info) -- might need to be changed
		//TODO: log if no results?
		foreach($contactnames as $contactname) {
			$resultList = $this->getBeansByName('Contacts', $contactname);
			if(isset($resultList)) {
				foreach($resultList as $result) {
					if(!is_null($result->id)) {
						//removing the myitems filter per defect 9933 - jdjohnso 1/6/12
						//if(array_key_exists($result->id, $myContactsArray)) { //filter against my items
						$contactids[] = $result->id;
						//}
					}
				}
			}
		}

		//look up contacts by email 2653 - jdjohnso 7/26/11
		//TODO: log if no results?
		foreach($contactemails as $contactemail) {
			$resultList = $this->getIDByEmail('Contacts', $contactemail);
			if(isset($resultList)) {
				foreach($resultList as $result) {
					if(!is_null($result)) {
						//removing the myitems filter per defect 9933 - jdjohnso 1/6/12
						//if(array_key_exists($result, $myContactsArray)) { //defect 3428: filter against my items
						$contactids[] = $result;
						//}
					}
				}
			}
		}

		//process input contact IDs
		foreach($contactids as $contactid) {
			$this->NOTES_LOGGER->logMessage("debug", __METHOD__."  processing contact $contactid");
			if(!is_null($contactid)) {
				$contactBean = $this->getBean($contactid, 'Contacts');

				//assuming that if the bean's name is null, then it's an empty bean
				if(!is_null($contactBean->last_name)) {
					$ret_array['contacts'][$contactid]['first_name'] = $contactBean->first_name;
					$ret_array['contacts'][$contactid]['last_name'] = $contactBean->last_name;

					//adding alt first/last names for work item 2639 - jdjohnso 7/18/11
					$ret_array['contacts'][$contactid]['alt_first_name'] = $contactBean->alt_lang_first_c;
					$ret_array['contacts'][$contactid]['alt_last_name'] = $contactBean->alt_lang_last_c;

					$ret_array['contacts'][$contactid]['title'] = $contactBean->title;
					$ret_array['contacts'][$contactid]['account_name'] = html_entity_decode($contactBean->account_name,ENT_QUOTES); //3178
					$ret_array['contacts'][$contactid]['account_id'] = $contactBean->account_id;
					$ret_array['contacts'][$contactid]['phone_work'] = $contactBean->phone_work;
					$ret_array['contacts'][$contactid]['phone_mobile'] = $contactBean->phone_mobile;
					$ret_array['contacts'][$contactid]['email1'] = $contactBean->email1;
					$ret_array['contacts'][$contactid]['primary_address_street'] = $contactBean->primary_address_street;
					$ret_array['contacts'][$contactid]['primary_address_city'] = $contactBean->primary_address_city;
					$stateKey = $contactBean->primary_address_state;
					if(!empty($stateKey)) {
						$stateKey = $this->getAVLValue('state_list', $stateKey, 'en_us', true); //note have to lookup by sugar avl key, ibm dictionary is STATE_ABBREV_SFA
					}
					$ret_array['contacts'][$contactid]['primary_address_state'] = $stateKey;
					$ret_array['contacts'][$contactid]['primary_address_postalcode'] = $contactBean->primary_address_postalcode;
					$ret_array['contacts'][$contactid]['primary_address_country'] = $contactBean->primary_address_country;

					//14497: need the client website for the contact
					$contact_accountBean = $this->getBean($contactBean->account_id, 'Accounts');
					$ret_array['contacts'][$contactid]['website'] = $contact_accountBean->website;

					//get contact oppties #2972 - jdjohnso 7/19/11
					$contactOppties = array();
					$contactOpptiesByDate = array();
					$opptyBeans = $contactBean->get_linked_beans('opportunities','Opportunity'); //get account opportunities
					foreach($opptyBeans as $opptyBean) {
						$opptyid = $opptyBean->id;
						$opptydate = $opptyBean->date_closed;
						$opptystage = $opptyBean->sales_stage;
						if($opptystage < 7) { //only show open opportunities
							if(array_key_exists($opptyid, $myOpptiesArray)) { //filter against my items
								$contactOpptiesByDate[$opptyid] = $opptydate; //remember id to date mapping
							}
						}
					}
					$this->NOTES_LOGGER->logMessage("debug", __METHOD__." sorting contact oppties");
					asort($contactOpptiesByDate); //sort based on date

					//add first five (or less) sorted oppties to array //29722 - changing to 50
					$opptyLimit = 50;
					$totaloppties = count($contactOpptiesByDate);
					if($totaloppties < $opptyLimit) {
						$opptyLimit = $totaloppties;
					}

					$contactOpptiesByDate = array_keys($contactOpptiesByDate);

					for($i = 0; $i < $opptyLimit; $i++) {
						$opptyid = $contactOpptiesByDate[$i];
						$contactOppties[] = $opptyid;
						if(!array_key_exists($opptyid, $opptyids)) { //if not already being asked for, (this check might not be needed)
							$opptyids[] = $opptyid; //add the opportunity to the id list to return info for
						}
					}
					
					$ret_array['contacts'][$contactid]['opportunitiesTotal'] = $totaloppties;
					$ret_array['contacts'][$contactid]['opportunities'] = $contactOppties;
					//

				} else {
					$this->NOTES_LOGGER->logMessage("warn", __METHOD__." $contactid contact bean is empty!");
				}
			}
		}

		//lookup opportunity IDs for input names (if they exist)
		//this approach requires loading the beans twice (once for searching, then again to get info) -- might need to be changed
		//TODO: log if no results?
		foreach($opptynames as $opptyname) {
			if(!is_null($opptyname)) {
				$opptyBean = $this->getBeanByString(array('name'=>$opptyname), 'Opportunities');

				if(!is_null($opptyBean->id)) {
					$opptyids[] = $opptyBean->id;
				}
			}
		}

		//process input opportunity IDs
		foreach($opptyids as $opptyid) {
			$this->NOTES_LOGGER->logMessage("debug", __METHOD__." processing oppty $opptyid");
			if(!is_null($opptyid)) {
				$opptyBean = $this->getBean($opptyid, 'Opportunities');

				if(!is_null($opptyBean->name)) {
					$ret_array['opportunities'][$opptyid]['name'] = $opptyBean->name;
					$ret_array['opportunities'][$opptyid]['description'] = $opptyBean->description;
					$ret_array['opportunities'][$opptyid]['account_name'] = html_entity_decode(str_replace('&nbsp;', ' ', $opptyBean->account_name),ENT_QUOTES); //3178, 35799
					$ret_array['opportunities'][$opptyid]['account_id'] = $opptyBean->account_id;
					$ret_array['opportunities'][$opptyid]['date_closed'] = $opptyBean->date_closed;

					//14497: need the client industry and website data for the oppty
					$oppty_accountBean = $this->getBean($opptyBean->account_id, 'Accounts');
					$ret_array['opportunities'][$opptyid]['industry'] = $this->getAccountIndustryData($oppty_accountBean);
					$ret_array['opportunities'][$opptyid]['website'] = $oppty_accountBean->website;

					//16021: sales stages should be pulled from AVL
					$opptystage = $opptyBean->sales_stage;
					//$opptystage = $this->doSalesStageMapping($opptystage);
					$opptystage = $this->getAVLValue('S_OPTY_STG', $opptystage);
					$ret_array['opportunities'][$opptyid]['sales_stage'] = $opptystage;

					//begin logic to format currency for defect 3137 -- jdjohnso 7/18/11
					//modifying currency output for 16824
					$currencyID = $opptyBean->currency_id;
					$currencyIDuser = $current_user->getPreference('currency');
					$opptyAmount = $opptyBean->amount;

					$ret_array['opportunities'][$opptyid]['amount'] = $this->getAmountStringForUser($currencyID, $opptyAmount, $currencyIDuser);
					//end currency logic

					$isFollowedResult = $this->checkIfFollowing('Opportunities', $opptyid);
					$ret_array['opportunities'][$opptyid]['followed'] = $isFollowedResult;
					
					//adding opportunity assigned user's email - jdjohnso 7/13/11
					$assignedUserID = $opptyBean->assigned_user_id;
					$assignedBPID = $opptyBean->assigned_bp_id;
					$assignedUserName = html_entity_decode($opptyBean->assigned_user_name,ENT_QUOTES); //3178
					$assignedUserEmail = '';

					//you have to load the user's bean to get at their email address
					if(!empty($assignedUserID)) {
						$this->NOTES_LOGGER->logMessage("debug", __METHOD__." getting assigned user email for user $assignedUserID");
						$assignedUserBean = $this->getBean($assignedUserID, 'Users');
						if(!is_null($assignedUserBean->last_name)) {
							$assignedUserEmail = $assignedUserBean->email1;
						}
					} else if(!empty($assignedBPID)) {
						$this->NOTES_LOGGER->logMessage("debug", __METHOD__." getting assigned bp info for bp $assignedBPID");
						$assignedBPBean = $this->getBean($assignedBPID, 'ibm_BusinessPartners');
						$assignedUserID = $assignedBPID;
						$assignedUserName = $assignedBPBean->name;
					}
					$ret_array['opportunities'][$opptyid]['assigned_user_id'] = $assignedUserID;
					$ret_array['opportunities'][$opptyid]['assigned_user_name'] = $assignedUserName;
					$ret_array['opportunities'][$opptyid]['assigned_user_email'] = $assignedUserEmail;

					//adding primary contact info for item 3494 - 10/3/11 jdjohnso
					//16826: pulling the name from $opptyBean->pcontact_id_c isn't localized, so we need to pull the contact bean
					$ret_array['opportunities'][$opptyid]['primary_contact_id'] = $opptyBean->contact_id_c;
					//$ret_array['opportunities'][$opptyid]['primary_contact_name'] = $opptyBean->pcontact_id_c;
					$opptyContactBean = $this->getBean($opptyBean->contact_id_c, 'Contacts');
					$ret_array['opportunities'][$opptyid]['primary_contact_name'] = $opptyContactBean->full_name;

					/** BEGIN REVENUE LINE ITEMS **/
					$this->NOTES_LOGGER->logMessage("debug", __METHOD__." getting oppty revenuelineitems");
					$lineItems = $opptyBean->get_linked_beans('opportun_revenuelineitems', 'ibm_RevenueLineItems');
					foreach($lineItems as $lineItem) {

						//get relevant line item info
						$lineItemid = $lineItem->id;

						$this->NOTES_LOGGER->logMessage("debug", __METHOD__." processing revenuelineitem $lineItemid");

						$lineItemCurrency = $lineItem->currency_id;
						$lineItemAmount = $lineItem->revenue_amount;
						$lineItemLevel15ID = $lineItem->level15;
						$lineItemOwnerID = $lineItem->assigned_user_id;
						$lineItemOwnerName = $lineItem->assigned_user_name;
						$lineItemBillDate = $lineItem->fcast_date_tran; //29329 previously 'bill_date"
						$lineItemLastModifiedDate = $lineItem->date_modified;

						//convert currency -- 16824
						$lineItemAmount = $this->getAmountStringForUser($lineItemCurrency, $lineItemAmount, $currencyIDuser);

						//need to return line item owner's email
						//first, check and see if the oppty owner also owns the line item
						if($lineItemOwnerID === $assignedUserID) {
							$lineItemOwnerEmail = $assignedUserEmail;
						} else {
							//not the same user, will have to do some work to get the email
							$lineItemOwnerEmail = '';

							//you have to load the user's bean to get at their email address
							if(isset($lineItemOwnerID)) {
								$this->NOTES_LOGGER->logMessage("debug", __METHOD__." getting assigned user email for user $lineItemOwnerID");
								$lineItemOwnerBean = $this->getBean($lineItemOwnerID, 'Users');
								if(!is_null($lineItemOwnerBean->last_name)) {
									$lineItemOwnerEmail = $lineItemOwnerBean->email1;
								}
							}
						}

						//get line item's level 15 product name
						$lineItemLevel15Product = $lineItem->getProduct($lineItemLevel15ID);
						$lineItemLevel15Name = $lineItemLevel15Product->name;

						/* got a bean method to do this now (see above) 15504
						 $lineItemLevel15Name = "";
						 //defect 9920: the table name changed from ibm_revenuelineitems_products
						 $level15query = 'SELECT name FROM ibm_products WHERE id = "'.$lineItemLevel15ID.'"';
						 //return $level15query;
						 $level15queryResult = $this->doDBQuery($level15query, $lineItem, null, null);
						 if(isset($level15queryResult[0]['name'])) {
							$lineItemLevel15Name = $level15queryResult[0]['name'];
							}
							*/

						$ret_array['opportunities'][$opptyid]['revenue_line_items'][$lineItemid]['amount'] = $lineItemAmount;
						$ret_array['opportunities'][$opptyid]['revenue_line_items'][$lineItemid]['level15'] = $lineItemLevel15Name;
						$ret_array['opportunities'][$opptyid]['revenue_line_items'][$lineItemid]['assigned_user_id'] = $lineItemOwnerID;
						$ret_array['opportunities'][$opptyid]['revenue_line_items'][$lineItemid]['assigned_user_name'] = $lineItemOwnerName;
						$ret_array['opportunities'][$opptyid]['revenue_line_items'][$lineItemid]['assigned_user_email'] = $lineItemOwnerEmail;
						$ret_array['opportunities'][$opptyid]['revenue_line_items'][$lineItemid]['bill_date'] = $lineItemBillDate;
						$ret_array['opportunities'][$opptyid]['revenue_line_items'][$lineItemid]['last_modified_date'] = $lineItemLastModifiedDate;
					}
					/** END REVENUE LINE ITEMS **/


				} else {
					$this->NOTES_LOGGER->logMessage("warn", __METHOD__." $opptyid oppty bean is empty!");
				}
			}
		}

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}

	/**
	 * Given an account and/or an opportunity, build a dictionary of associated users
	 *
	 * @param string $session - authenticated session id
	 * @param array @accountids - array of string account uuids
	 * @param array @opptyids - array of string oppty uuids
	 * @return account/oppty/user dictionary array
	 */
	function getUserList($session, $accountids, $opptyids) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);

		if(!$this->checkAccess($session)) return;

		global $current_user;
		$userid = $current_user->id; //get current user's uuid

		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;

		//process input account ids
		foreach($accountids as $accountid) {
			$this->NOTES_LOGGER->logMessage("debug", __METHOD__." processing account $accountid");
			if(!empty($accountid)) {
				$accountBean = $this->getBean($accountid, 'Accounts');
				$collectedAccounts = $this->getAccountAndChildrenIDs($accountBean);
				if(!empty($collectedAccounts)) {
					//build the "IN" clause of query with the input + child account ids
					$inStatement = $this->getSQLInClause($collectedAccounts);
					
					//run db query to get related users
					$userquery = "SELECT DISTINCT users.id, ".
						"LTRIM(RTRIM(NVL(users.first_name,'')||' '||NVL(users.last_name,''))) as name, ".
						"users.user_name ".
						"FROM users INNER JOIN accounts_users ON users.id=accounts_users.user_id ".
						"AND accounts_users.account_id IN $inStatement ".
						"AND accounts_users.deleted=0";
					
					$userResult = $this->doDBQuery($userquery, $current_user, -1, null);
					if(!empty($userResult)) {
						$userIDs = array(); //list of users for account
						foreach($userResult as $aUser) {
							if(isset($aUser['id']) && isset($aUser['name']) && isset($aUser['user_name'])) {
								$aUserID = $aUser['id'];
								$aUserName = $aUser['name'];
								$aUserEmail = $aUser['user_name']; //user_name is email address in SC
								
								//1a. build a list of uuids for the account
								$userIDs[]['id'] = $aUserID;
								
								//1b. add user details to a 'member' dictionary
								if(!isset($ret_array['members']) || !array_key_exists($aUserID, $ret_array['members'])) {
									$ret_array['members'][$aUserID] = array('user_name' => $aUserEmail,
												     					'name' => $aUserName,
												    					'email' => $aUserEmail);
								}
							}
						}
						
						//2. add account to accounts dictionary
						$accountName = $accountBean->name;
						$ret_array['accounts'][$accountid] = array('name' => $accountName,'members' => $userIDs);
					}
				}
			}
		}

		//process input oppty ids
		foreach($opptyids as $opptyid) {
			$this->NOTES_LOGGER->logMessage("debug", __METHOD__."processing opportunity $opptyid");
			if(!is_null($opptyid)) {
				$opptyBean = $this->getBean($opptyid, 'Opportunities');

				if(!is_null($opptyBean->name)) {
					//1. get oppty's parent account
					$parentAccountBean = $this->getBean($opptyBean->account_id, 'Accounts');
					$parentAccountID = $parentAccountBean->id;
					$parentAccountName = $parentAccountBean->name;

					//2. get opportunity users
					$this->NOTES_LOGGER->logMessage("debug", __METHOD__." loading opportunities_users related beans for $opptyid");
					$userBeans = $opptyBean->get_linked_beans('rel_additional_users','User'); //get opportunity users
					$userIDs = array();
					$uniqueUserIDs = array();
					foreach($userBeans as $userBean) {
						//2a. build a list of uuids for the opportunity
						$userid = $userBean->id;

						if(!in_array($userid, $uniqueUserIDs)) {

							$userIDs[]['id'] = $userid;
							$uniqueUserIDs[] = $userid;

							//2b. add user details to a 'member' dictionary
							if(!array_key_exists($userid, $ret_array['members'])) {
								//$ret_array['members'][$user['id']] = $user;
								$ret_array['members'][$userid] = array('user_name' => $userBean->user_name,
										     'name' => html_entity_decode($userBean->full_name,ENT_QUOTES), //16822 - switching to full name, 27386 - un-encoding html chars
										     'email' => $userBean->email1);
							}
						}
					}

					//3. add opportunity to opportunities dictionary
					$ret_array['opportunities'][$opptyid] = array('name' => $opptyBean->name,
										'description' => $opptyBean->description,
										'members' => $userIDs,
                                		'accountId' => $parentAccountID);

					//4. add account and account users to accounts/members dictionaries (if not already added above)
					if(!array_key_exists($parentAccountID, $ret_array['accounts'])) {
						$this->NOTES_LOGGER->logMessage("debug", __METHOD__." loading accounts_users related beans for $parentAccountID");
						$parentAccountUserBeans = $parentAccountBean->get_linked_beans('rel_additional_users','User');
						$userIDs = array();
						foreach($parentAccountUserBeans as $userBean) {
							//4a. build a list of uuids for the account
							$userid = $userBean->id;
							$userIDs[]['id'] = $userid;

							//4b. add account user details to a 'member' dictionary
							if(!array_key_exists($userid, $ret_array['members'])) {
								$ret_array['members'][$userid] = array('user_name' => $userBean->user_name,
										     'name' => html_entity_decode($userBean->full_name,ENT_QUOTES), //16822 - switching to full name, 27386 - un-encoding html chars
										     'email' => $userBean->email1);
							}
						}

						//4c. add account details to account dictionary
						$ret_array['accounts'][$parentAccountID] = array('name' => $parentAccountName,
                                    		'members' => $userIDs);

					}
				} else {
					$this->NOTES_LOGGER->logMessage("warn", __METHOD__." $opptyid opportunity bean is empty!");
				}
			}
		}

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}

	/**
	 * Given an account (client), get up to 50 Contacts associated with that account or its children accounts.
	 * 
	 * Result array contains:
	 * totalCount - total number of results in DB
	 * resultCount - number of results being returned (up to 50)
	 * results - array of results
	 * 
	 * results contains contact:
	 * id, first_name, last_name, phone_work, phone_mobile, email1, email_opt_out
	 *
	 * @param string $session - authenticated session id
	 * @param array $accountid - string account uuid
	 * @return array containing contact results
	 */
	function getContactsForAccountCollected($session, $accountid) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);

		if(!$this->checkAccess($session)) return;
		
		require_once('helpers/notesHelper_GetContactsForAccountCollected.php');
		$getContactsHelper = new notesHelper_GetContactsForAccountCollected($this->NOTES_LOGGER);
		
		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;
		
		$resultsArray = $getContactsHelper->getResult($accountid);		
		if(!empty($resultsArray)) {
			$ret_array['totalCount'] = $resultsArray['totalCount'];
			$ret_array['resultCount'] = $resultsArray['resultCount'];
			$ret_array['result'] = $resultsArray['result'];
		} else {
			//default return, if error or no results
			$ret_array['totalCount'] = 0;
			$ret_array['resultCount'] = 0;
			$ret_array['result'] = array();
		}
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}
	
	/**
	 * Returns the header array with a valid session and the protocol version.
	 *
	 * @param string $session authenticated session
	 * @return header array
	 */
	function getSession($session) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		if(!$this->checkAccess($session)) return;

		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}

	/**
	 * Return the additional call form fields based on the user's preference.
	 *
	 * @param string $session authenticated session
	 * @return additional call template name and form fields
	 */
	function getCallForm($session) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		if(!$this->checkAccess($session)) return;

		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;

		$returnForm = array();

		//create a new Call bean
		global $beanList, $beanFiles;
		$beanType = 'Calls';
		$beanName = $beanList[$beanType];
		require_once $beanFiles[$beanName];
		$bean = new $beanName();

		//field def array
		$callFieldDefs = $bean->field_defs;

		//get default call form -- we will need the translated strings
		require_once('./custom/modules/Calls/metadata/editviewdefs.php');
		$callViewDefs = $viewdefs['Calls']['EditView']['panels'];
		//$callViewDefs = array();

		//get current user and load his call log form preference
		global $current_user;
		$userid = $current_user->id;
		$extendedViewName = IBMHelper::getClass('AdditionalViews')->getUserTemplateName("Calls", $userid);
		$this->NOTES_LOGGER->logMessage("info", __METHOD__." Accessing call form for user $userid"); //do this at info level so we can pull user stats from the logs now that regex is on v10
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." User $userid extended call view name is $extendedViewName");

		//load the extended call view based on user's preference
		if(file_exists("custom/modules/Calls/metadata/extended_layouts/{$extendedViewName}.php")) {
			require("custom/modules/Calls/metadata/extended_layouts/{$extendedViewName}.php");
			$newdefs=$viewdefs['Calls']['EditView']['panels'];
			$callViewDefs=array_merge($callViewDefs,$newdefs);

			$ret_array['callFormName'] = $extendedViewName;
		}

		//we care about order of the form items and
		//since java doesn't guarantee perserved order in JSON, use a explicit key for form items
		$formIndex = 0;

		//loop through the view defs and parse them into our desired output
		foreach($callViewDefs as $panelKey => $panelArray) {
			//since we're only looking for extended layout, only expecting one panel, that being the form label
			//if this changes, might need to adjust this code
			$ret_array['callFormLabel'] = translate($panelKey,'Calls');

			//iterate through form rows
			foreach($panelArray as $rowKey => $rowArray) {
				$fieldArray = array();

				//iterate through each column on a row
				foreach($rowArray as $columnKey => $entry) {
					$returnForm['key'.$formIndex] = $this->processViewDefEntry($entry, $callFieldDefs);
					$formIndex = $formIndex + 1;
				}
			}
		}

		$ret_array['callForm'] = $returnForm;

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}

	/**
	 * Create a new 'Call' bean and save it
	 *
	 * @deprecated this is going away in 1.0 in favor of saveCallMultiAssociate
	 * 
	 * @param Array $callParameters array of call parameters
	 * @param string $session authenticated session
	 */
	function saveCall($session, $callParameters) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." callParameters: ".json_encode($callParameters));
		if(!$this->checkAccess($session)) return;

		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;

		global $beanList, $beanFiles;

		$beanType = 'Calls';
		$beanName = $beanList[$beanType];
		require_once $beanFiles[$beanName];
		$bean = new $beanName();

		//loop through input parameters and set them in the bean
		foreach($callParameters as $key => $value) {

			//multi-selects are stored as ^,-separated lists
			if(is_array($value)) {
				$value = $this->buildCommaCaretStringFromArray($value);
			}

			$bean->$key = $value;
		}

		//sanity check the bean for required values
		if(empty($bean->name) ||
		empty($bean->duration_minutes) ||
		empty($bean->date_start) ||
		//empty($bean->direction) || //removing direction from required value
		empty($bean->status)) {

			$this->NOTES_LOGGER->logMessage("warn", __METHOD__." missing required values for call - unable to save");
			$ret_array['saveResponse'] = new sfaError('SFA0003');
			return $ret_array;
		}

		//get current user
		global $current_user;
		$userid = $current_user->id; //get current user's uuid
		$bean->assigned_user_id = $current_user->id;
		$bean->assigned_user_name = $current_user->full_name;

		//save call to database
		$saveResult = $bean->save();

		//defect 12841: to match sugar behavior, on a call related to a contact,
		//we need to make sure the call-to-contact relationship is being created,
		//so it will appear in the activities panel on the contact's page
		$parentBean = null;
		$parentType = $bean->parent_type;
		$parentId = $bean->parent_id;
		if(!empty($parentType) && !empty($parentId) && $parentType == 'Contacts') {
			//if parent is a contact, set the relationship using the contact
			$parentBean = $this->getBean($parentId, 'Contacts');
		} else {
			//otherwise just use the current user
			$parentBean = $current_user;
		}
		
		if($parentType == 'Contacts') {
     		$bean->load_relationship('contacts');
            $bean->contacts->add($parentId);
        } else if($parentType == 'Accounts') {
            $bean->load_relationship('accounts');
            $bean->accounts->add($parentId);
        } else if($parentType == 'Opportunities') {
        	$bean->load_relationship('opportunities');
        	$bean->opportunities->add($parentId);
        }
			
		//when call is logged in UI, user creating is added and accept state is set to 'none'
		//if this UI behavior changes, might need to change this
		$bean->set_accept_status($parentBean,'none');
			
		//return uid of new call
		$ret_array['saveResponse'] = $saveResult;

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}
	
	/**
	 * Create a new 'Call' bean and save it. This supports mutli-association.
	 * 62946 - add support for Lead module (2015-03-24)
	 * @param Array $callParameters array of call parameters
	 * @param string $session authenticated session
	 * @return uuid of saved call bean
	 */
	public function saveCallMultiAssociate($session, $callParameters) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." callParameters: ".json_encode($callParameters));
		if(!$this->checkAccess($session)) return;

		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;

		global $beanList, $beanFiles;

		$beanType = 'Calls';
		$beanName = $beanList[$beanType];
		require_once $beanFiles[$beanName];
		$bean = new $beanName();
		
		//loop through input parameters and set them in the bean
		foreach($callParameters as $key => $value) {
			//multi-selects are stored as ^,-separated lists
			if(is_array($value)) {
				$value = $this->buildCommaCaretStringFromArray($value);
			}

			$bean->$key = $value;
		}
		
		//sanity check the bean for required values
		if(empty($bean->name) ||
		empty($bean->duration_minutes) ||
		empty($bean->date_start) ||
		//empty($bean->direction) || //removing direction from required value
		empty($bean->status)) {

			$this->NOTES_LOGGER->logMessage("warn", __METHOD__." missing required values for call - unable to save");
			$ret_array['saveResponse'] = new sfaError('SFA0003');
			return $ret_array;
		}
		
		//save call to database
		$saveResult = $bean->save();
				
		//when call is logged in UI, user creating is added and accept state is set to 'none'
		//if this UI behavior changes, might need to change this
		global $current_user;
		$bean->set_accept_status($current_user,'none');
		
		//make sure the assigned user has accept status set (if call is being logged by someone else)
		if($current_user->id != $bean->assigned_user_id) {
			$assigned_user_bean = $this->getBean($bean->assigned_user_id, 'Users');
			$bean->set_accept_status($assigned_user_bean,'none');
		}
		
		//additional users format: 'userid1^^userid2'
		$sfanotes_additional_users = $callParameters['sfanotes_additional_users'];
		if(!empty($sfanotes_additional_users)) {
			$sfanotes_additional_users_array = explode("^^",$sfanotes_additional_users);
			foreach($sfanotes_additional_users_array as $additional_user_id) {
				$bean->load_relationship('additional_assignees_link');
   		        $bean->additional_assignees_link->add($additional_user_id);
			}
		}
		
		//related to format: 'module,id1^^module,id2'
		$sfanotes_related_to = $callParameters['sfanotes_related_to'];
		if(!empty($sfanotes_related_to)) {
			$sfanotes_related_to_array = explode("^^",$sfanotes_related_to);
			foreach($sfanotes_related_to_array as $related_to_pair) {
				if(!empty($related_to_pair)) {
					$related_to_pair_array = explode(",",$related_to_pair);
					$related_to_type = $related_to_pair_array[0];
					$related_to_id = $related_to_pair_array[1];
					if(!empty($related_to_type) && !empty($related_to_id)) {
						if($related_to_type == 'Contacts') {
				     		$bean->load_relationship('contacts');
				            $bean->contacts->add($related_to_id);
				        } else if($related_to_type == 'Accounts') {
				            $bean->load_relationship('accounts');
				            $bean->accounts->add($related_to_id);
				        } else if($related_to_type == 'Opportunities') {
				        	$bean->load_relationship('opportunities');
				        	$bean->opportunities->add($related_to_id);
				        } else if($related_to_type == 'Leads') {
				     		$bean->load_relationship('leads');
				            $bean->leads->add($related_to_id);
				        }
					}
				}
			}
		}
		
		//return uid of new call
		$ret_array['saveResponse'] = $saveResult;
		
		$this->NOTES_LOGGER->logMessage("info", __METHOD__." call logged by ".$current_user->id);

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}
	
	/**
	 * Search for contacts, accounts, opportunities, or users
	 *
	 * @param string $session authenitcated session
	 * @param string $moduleType Accounts, Opportunities, Contacts, or Users
	 * @param string $searchString search string
	 * @param integer $resultLimit number of results to return
	 * @param string $myItems 'true' or 'false' whether or not to use user's myitems
	 * @param string $filter string to filter results against -- currently only used for accounts
	 * @return search results 
	 */
	public function getTypeAheadResults11($session, $moduleType, $searchString, $resultLimit="", $myItems, $filter="") {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		if(!$this->checkAccess($session)) return;

		require_once('helpers/notesHelper_GetTypeAheadResults.php');
		$typeAheadHelper = new notesHelper_GetTypeAheadResults($this->NOTES_LOGGER);
		
		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;

		$input = array(
			'moduleType' => $moduleType,
			'searchString' => $searchString,
			'resultLimit' => $resultLimit,
			'myItems' => $myItems,
			'filter' => $filter
		);
		
		$resultsArray = $typeAheadHelper->getResult($input);		
		if(!empty($resultsArray)) {
			$ret_array['results'] = $resultsArray;
		} else {
			$ret_array['results'] = array();
		}
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}

	/**
	 * Search for contacts, accounts, opportunities, or users
	 *
	 * @param string $session authenitcated session
	 * @param string $moduleType Accounts, Opportunities, Contacts, or Users
	 * @param string $searchString search string
	 * @param integer $resultLimit number of results to return
	 * @param string $myItems 'true' or 'false' whether or not to use user's myitems
	 * @param string $filter string to filter results against -- currently only used for accounts
	 * @return search results
	 * 
	 * @deprecated use $this->getTypeAheadResults11() instead
	 */
	public function getTypeAheadResults($session, $moduleType, $searchString, $resultLimit, $myItems, $filter="") {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." Moduletype: $moduleType Searchstring: $searchString ResultLimit: $resultLimit MyItems: $myItems Filter: $filter");
		if(!$this->checkAccess($session)) return;

		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;

		global $current_user;

		//verify result limit input, and decrease by one to make number make sense
		//for example to show only one result, it should really be set to '0', but webservice wants '1'
		if(is_numeric($resultLimit)) $resultLimit = $resultLimit - 1;
		else $resultLimit = null;

		if(!isset($myItems) || $myItems != 'false') $myItems = 'true';

		if($moduleType === 'Accounts') {
			$ret_array['results'] = $this->getTypeAheadAccounts($current_user, $searchString, $resultLimit, $myItems, $filter);
		} else if($moduleType === 'Opportunities') {
			$ret_array['results'] = $this->getTypeAheadOpportunities($current_user, $searchString, $resultLimit, $myItems);
		} else if($moduleType === 'Contacts') {
			$ret_array['results'] = $this->getTypeAheadContacts($current_user, $searchString, $resultLimit, $myItems);
		} else if($moduleType === 'Users') {
			$ret_array['results'] = $this->getTypeAheadUsers($current_user, $searchString, $resultLimit);
		} else {
			//unknown module type, do nothing
		}

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}
	
	/**
	 * Search for contacts, accounts, and opportunities.
	 *
	 * @param string $session authenitcated session
	 * @param string $searchString search string
	 * @param integer $resultLimit number of results to return -- note this number is divided by 3, and this is how much of each type is returned
	 * @param string $myItems 'true' or 'false' whether or not to use user's myitems -- defaults to true
	 * @return search results
	 */
	public function getTypeAheadResultsCollected11($session, $searchString, $resultLimit, $myItems) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		if(!$this->checkAccess($session)) return;
		
		require_once('helpers/notesHelper_GetTypeAheadResults.php');
		$typeAheadHelper = new notesHelper_GetTypeAheadResults($this->NOTES_LOGGER);
		
		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;

		$input = array(
			'searchString' => $searchString,
			'resultLimit' => $resultLimit,
			'myItems' => $myItems,
		);
		
		$resultsArray = $typeAheadHelper->getCollectedResult($input);		
		if(!empty($resultsArray)) {
			$ret_array['results'] = $resultsArray;
		} else {
			$ret_array['results'] = array();
		}
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}
	
	/**
	 * Search for contacts, accounts, and opportunities.
	 *
	 * @param string $session authenitcated session
	 * @param string $searchString search string
	 * @param integer $resultLimit number of results to return -- note this number is divided by 3, and this is how much of each type is returned
	 * @param string $myItems 'true' or 'false' whether or not to use user's myitems -- defaults to true
	 * @return search results
	 * 
	 * @deprecated use $this->getTypeAheadResultsCollected11() instead
	 */
	public function getTypeAheadResultsCollected($session, $searchString, $resultLimit, $myItems) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." Searchstring: $searchString ResultLimit: $resultLimit MyItems: $myItems");
		if(!$this->checkAccess($session)) return;

		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;

		global $current_user;

		//divide resultLimit by 3, we'll request that many results per type
		if(is_numeric($resultLimit) && $resultLimit > 0) $resultLimit = round($resultLimit / 3);
		else $resultLimit = -1;

		if(!isset($myItems) || $myItems != 'false') $myItems = 'true'; //default myItems to 'true'

		//do accounts query
		$ret_array['results']['accounts'] = $this->getTypeAheadAccounts($current_user, $searchString, $resultLimit, $myItems, null);

		//do opportunities query
		$ret_array['results']['opportunities'] = $this->getTypeAheadOpportunities($current_user, $searchString, $resultLimit, $myItems);

		//do contacts query
		$ret_array['results']['contacts'] = $this->getTypeAheadContacts($current_user, $searchString, $resultLimit, $myItems);

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}


	/**
	 * Return the logged-in user's preferences, including date/time/name/currency format.
	 *
	 * @param string $session authenticated session
	 */
	public function getUserPreferencesForClient($session) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		if(!$this->checkAccess($session)) return;

		global $current_user;
		global $current_language;
		global $sugar_config;
		$current_user->loadPreferences();

		$retArray = array();
		$headerArray = $this->buildHeader($session);
		$retArray['header'] = $headerArray;

		//$retArray['user_id'] = $current_user->id;
		//$retArray['user_name'] = $current_user->user_name;
		$retArray['user_language'] = $current_language;

		$cur_id = $current_user->getPreference('currency');
		$retArray['user_currency_id'] = $cur_id;
		$currencyObject = new Currency();
		$currencyObject->retrieve($cur_id);
		$retArray['user_currency_name'] = $currencyObject->name;
		$retArray['user_currency_symbol'] = $currencyObject->symbol;

		$retArray['user_is_admin'] = is_admin($current_user);
		$retArray['user_default_team_id'] = $current_user->default_team;
		$retArray['user_default_dateformat'] = $current_user->getPreference('datef');
		$retArray['user_default_timeformat'] = $current_user->getPreference('timef');

		$retArray['user_default_locale_name_format'] = $current_user->getPreference('default_locale_name_format');

		$num_grp_sep = $current_user->getPreference('num_grp_sep');
		$dec_sep = $current_user->getPreference('dec_sep');
		$retArray['user_number_seperator'] = empty($num_grp_sep) ? $sugar_config['default_number_grouping_seperator'] : $num_grp_sep;
		$retArray['user_decimal_seperator'] = empty($dec_sep) ? $sugar_config['default_decimal_seperator'] : $dec_sep;

		//pass back the config values for max myitems for client and contact as well as how big the request batches should be
		$maxAccounts = "5000";
		$maxContacts = "5000";
		$batchSize = "500";
		if(isset($sugar_config['notes_max_num_myitems_contacts'])) {
			$maxContacts = $sugar_config['notes_max_num_myitems_contacts'];
		}
		if(isset($sugar_config['notes_max_num_myitems_accounts'])) {
			$maxAccounts = $sugar_config['notes_max_num_myitems_accounts'];
		}
		if(isset($sugar_config['notes_regex_batch_size'])) {
			$batchSize = $sugar_config['notes_regex_batch_size'];
		}
		$retArray['notes_max_num_myitems_contacts'] = $maxContacts;
		$retArray['notes_max_num_myitems_accounts'] = $maxAccounts;
		$retArray['notes_regex_batch_size'] = $batchSize;
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $retArray;
	}
	
	/**
	 * Get the Accounts/Opportunities related to a given document.
	 * 
	 * @param string $session authenticated session
	 * @param array $input input array
	 */
	public function getDocumentRelationships($session, $input) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);

		if(!$this->checkAccess($session)) return;
		
		require_once('helpers/notesHelper_GetDocumentRelationships.php');
		$getDocumentRelationshipsHelper = new notesHelper_GetDocumentRelationships($this->NOTES_LOGGER);
		
		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;
		
		$resultsArray = $getDocumentRelationshipsHelper->getResult($input);		
		if(!empty($resultsArray)) {
			$ret_array['result'] = $resultsArray;
		} else {
			$ret_array['result'] = array();
		}
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}
	
	/**
	 * Return the connections URL from the sugar config.
	 *
	 * @param string $session authenticated session
	 */
	public function getConnectionsURL($session) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		if(!$this->checkAccess($session)) return;
		
		$ret = "null";
		
		global $sugar_config;
		
		if(isset($sugar_config['connections_base_url'])) {
			$ret = $sugar_config['connections_base_url'];
		}
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret;
	}
	
	/** 
	 * Use Collab-web Following API to determine if object is being followed.
	 * 
	 * @return "false" if not, "true" if followed, or "NA" if problem is encountered
	 * 
	 * @deprecated use $this->getInfo13() instead
	 */
	public function cwIsFollowing($session, $moduleType, $id) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		if(!$this->checkAccess($session)) return;
		
		require_once('helpers/notesHelper_CollabWeb.php');
		$cwHelper = new notesHelper_CollabWeb($this->NOTES_LOGGER);
		
		$ret = $cwHelper->cwIsFollowing($moduleType, $id);
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret;
	}
	
	/**
	 * Use Collab-web Following API to follow a given object for a user.
	 * 
	 * Now returns with header like the rest of our notes calls.
	 * 
	 * @return "followed" if operation finishes (note, does NOT guarantee success) or "NA" if problem is encountered
	 */
	public function cwFollow20($session, $moduleType, $id) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		if(!$this->checkAccess($session)) return;
		
		require_once('helpers/notesHelper_CollabWeb.php');
		$cwHelper = new notesHelper_CollabWeb($this->NOTES_LOGGER);
		
		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;
		
		$ret_array['result'] = $cwHelper->cwFollow($moduleType, $id);
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}
	
	/** 
	 * Use Collab-web Following API to follow a given object for a user.
	 * 
	 * @return "followed" if operation finishes (note, does NOT guarantee success) or "NA" if problem is encountered
	 * 
	 * @deprecated use $this->cwFollow20() instead
	 */
	public function cwFollow($session, $moduleType, $id) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		if(!$this->checkAccess($session)) return;
		
		require_once('helpers/notesHelper_CollabWeb.php');
		$cwHelper = new notesHelper_CollabWeb($this->NOTES_LOGGER);
		
		$ret = $cwHelper->cwFollow($moduleType, $id);
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret;
	}
	
	/**
	 * Use Collab-web Following API to unfollow a given object for a user.
	 * 
	 * Now returns with header like the rest of our notes calls.
	 * 
	 * @return "unfollowed" if operation finishes (note, does NOT guarantee success) or "NA" if problem is encountered
	 */
	public function cwUnfollow20($session, $moduleType, $id) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		if(!$this->checkAccess($session)) return;
		
		require_once('helpers/notesHelper_CollabWeb.php');
		$cwHelper = new notesHelper_CollabWeb($this->NOTES_LOGGER);
		
		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;
		
		$ret_array['result'] = $cwHelper->cwUnfollow($moduleType, $id);
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}
	
	/** 
	 * Use Collab-web Following API to unfollow a given object for a user.
	 * 
	 * @return "unfollowed" if operation finishes (note, does NOT guarantee success) or "NA" if problem is encountered
	 * 
	 * @deprecated use $this->cwUnfollow20() instead
	 */
	public function cwUnfollow($session, $moduleType, $id) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		if(!$this->checkAccess($session)) return;
		
		require_once('helpers/notesHelper_CollabWeb.php');
		$cwHelper = new notesHelper_CollabWeb($this->NOTES_LOGGER);
		
		$ret = $cwHelper->cwUnfollow($moduleType, $id);
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret;
	}
	
	/**
	 * Calls collab-web IBMDocumentSharingService to validate if a user can download the given file id.
	 * 
	 * Return format:
	 * array(
	 * 	'response' = true/false 
	 * 	'message' = [response message]
	 * 	'code' = [response code]
	 * )
	 * 
	 * Possible response codes:
	 * 0 - successful call to IBMDocumentSharingService
	 * -100 - something failed in method setup, shouldn't ever hit this
	 * -105 - null response from $svc->validateDownload($documentDecorator)
	 * -104 - empty document bean, trouble loading document bean with given id
	 * -101 - validateDocument method not found in collab-web class
	 * -102 - IBMDocumentSharingService class not found
	 * -103 - IBMDocumentSharingService file not found
	 * others -- collab-web code may throw an exception, their error code/message is passed through
	 * 
	 * @param string $session authenticated session
	 * @param string $documentId document sugar UUID
	 */
	public function cwValidateDownload($session, $documentId) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		if(!$this->checkAccess($session)) return;
		
		require_once('helpers/notesHelper_CollabWeb.php');
		$cwHelper = new notesHelper_CollabWeb($this->NOTES_LOGGER);
		
		$ret = $cwHelper->cwValidateDownload($documentId);
			
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret;
	}
	
	public function cwGetEventsFilter($session, $moduleType, $id) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		if(!$this->checkAccess($session)) return;
		
		require_once('helpers/notesHelper_CollabWeb.php');
		$cwHelper = new notesHelper_CollabWeb($this->NOTES_LOGGER);
		
		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;
		$ret_array['result'] = $cwHelper->cwGetEventsFilter($moduleType, $id);
			
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}
	
	public function cwGetMicroblogFilter($session, $moduleType, $id) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		if(!$this->checkAccess($session)) return;
		
		require_once('helpers/notesHelper_CollabWeb.php');
		$cwHelper = new notesHelper_CollabWeb($this->NOTES_LOGGER);
		
		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;
		$ret_array['result'] = $cwHelper->cwGetMicroblogFilter($moduleType, $id);
			
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}
	
	public function cwPostStatusUpdate($session, $moduleType, $id, $statusUpdate) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		if(!$this->checkAccess($session)) return;
		
		require_once('helpers/notesHelper_CollabWeb.php');
		$cwHelper = new notesHelper_CollabWeb($this->NOTES_LOGGER);
		
		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;
		$ret_array['result'] = $cwHelper->cwPostStatusUpdate($moduleType, $id, $statusUpdate);
			
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}
	
	public function getActivityStreamWidgetName($session) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		if(!$this->checkAccess($session)) return;
		
		require_once('helpers/notesHelper_CollabWeb.php');
		$cwHelper = new notesHelper_CollabWeb($this->NOTES_LOGGER);
		
		$ret_array = array();
		$headerArray = $this->buildHeader($session);
		$ret_array['header'] = $headerArray;
		$ret_array['result'] = $cwHelper->getActivityStreamWidgetName();
			
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}

	/**
	 * Check whether the given session is valid and if the user has module access
	 * Note: this also populates the $current_user object and calls seamlessSession
	 *
	 * @param string $session - authenticated session id
	 * @return true if the session id is valid, false if not
	 */
	private function checkAccess($session) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." Checking session $session");

		$error = new SoapError();
		$module_name = "";
		if (!self::$helperObject->checkSessionAndModuleAccess($session, 'invalid_session', $module_name, 'read', 'no_access', $error)) {
			$error->set_error('invalid_login');
			$this->NOTES_LOGGER->logMessage("fatal", __METHOD__." invalid session $session");
			$this->NOTES_LOGGER->logExit(__METHOD__);
			return false;
		}

		$this->seamlessSession($session);

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return true;
	}

	/**
	 * Make the session seamless, so a user can hit a sugar link with the
	 * given session ID and not have to login.
	 *
	 * @param string $session - authenticated session id
	 * @return the passed session id
	 */
	private function seamlessSession($session) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		//defect 9164: sugar fixed a seamless login defect in 7174, so we need to call their fixed method
		//$_SESSION['seamless_login'] = true;
		$this->seamless_login($session);
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $session;
	}

	/**
	 * Build the protocol header array.
	 * Currently contains the authenticated session and protocol version number
	 *
	 * @param string $session - authenticated session id
	 * @return header array
	 */
	private function buildHeader($session) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);

		global $current_user;

		$ret_array = array();
		$ret_array['sessionid'] = $session;
		$ret_array['protocolVersion'] = self::$PROTOCOL_VERSION;
		$ret_array['user_id'] = $current_user->id;
		$ret_array['user_full_name'] = $current_user->full_name;
		$ret_array['user_first_name'] = $current_user->first_name;
		$ret_array['user_last_name'] = $current_user->last_name;
		$ret_array['user_alt_first_name'] = $current_user->alt_language_first_name;
		$ret_array['user_alt_last_name'] = $current_user->alt_language_last_name;

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret_array;
	}

	/**
	 * Get a SugarBean with the given ID and type
	 *
	 * @param string $beanID UUID of bean being looked up
	 * @param string $beanType bean type being looked up (Accounts, Contacts, Opportunities, etc)
	 * @return bean object wiht fetched data
	 * TODO: there isn't really any sort of ACL on this method currently -- not sure what the answer for this is
	 * @see ./sugar/data/SugarBean.php for the generic bean class
	 */
	private function getBean($beanID, $beanType) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." Get $beanType bean $beanID");

		global  $beanList, $beanFiles;

		$beanName = $beanList[$beanType];
		require_once $beanFiles[$beanName];
		$bean = new $beanName();

		/*
		 * @param string $id Optional, default -1, is set to -1 id value from the bean is used, else, passed value is used
		 * @param boolean $encode Optional, default true, encodes the values fetched from the database.
		 * @param boolean $deleted Optional, default true, if set to false deleted filter will not be added.
		 */
		$bean->retrieve($beanID, false, true); //changing encode to false for defect 3178 - jdjohnso 7/18/11

		if(is_null($bean)) {
			$this->NOTES_LOGGER->logMessage("warn", __METHOD__." $beanType bean $beanID is null");
		}

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $bean;
	}

	/**
	 * Get a Single SugarBean with the given Key/Value Pair array and type
	 *
	 * @param array @KVP array of key/value pairs (used to construct WHERE query)
	 * @param string $beanType bean type being looked up (Accounts, Contacts, Opportunities, etc)
	 * @return bean object with fetched data
	 */
	private function getBeanByString($KVP, $beanType) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." Get $beanType bean ".json_encode($KVP));

		global $beanList, $beanFiles;

		$beanName = $beanList[$beanType];
		require_once $beanFiles[$beanName];
		$bean = new $beanName();

		/*
		 * @param array @KVP  array of name value pairs used to construct query.
		 * @param boolean $encode Optional, default true, encode fetched data.
		 */
		$bean->retrieve_by_string_fields($KVP, true);

		if(is_null($bean)) {
			$this->NOTES_LOGGER->logMessage("warn", __METHOD__." null bean");
		}

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $bean;
	}

	/**
	 * Get a list of SugarBeans of a given type based on a search string
	 *
	 * @param string $beanType type of bean -- currently Accounts and Contacts are supported
	 * @param string $searchString string to search for -- Accounts corresponds to 'client_id', Contacts to 'first_name last_name'
	 * @return list of beans found
	 */
	private function getBeansByName($beanType, $searchString) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." Get $beanType bean $searchString");

		global $beanList, $beanFiles;

		$beanName = $beanList[$beanType];
		require_once $beanFiles[$beanName];
		$bean = new $beanName();

		$returnList;

		if($beanType === 'Accounts') {
			//if bean type is accounts, we're searching on 'client_id'
			//maybe need to switch this to UPPER and LOWER for db2
			//changing client_id to ccms_id 15937
			$whereStatement = "accounts.ccms_id = '".$searchString."'"; //18347 - changing LIKE to = due to LIKE not matching short IDs

			$result = $bean->get_list("", $whereStatement);
			$returnList = $result['list'];

		} else if($beanType === 'Contacts') {
			//currently assuming search string will be 'first_name last_name' or 'last_name first_name'
			$searchStringArray = explode(" ", $searchString); //assuming search string will be 'first_name last_name'
			if(count($searchStringArray) >= 2) {
				$whereStatement = "(contacts.first_name LIKE '%".$searchStringArray[0]."%' AND contacts.last_name LIKE '%".$searchStringArray[1]."%')";
				$whereStatement = $whereStatement." OR (contacts.first_name LIKE '%".$searchStringArray[1]."%' AND contacts.last_name LIKE '%".$searchStringArray[0]."%')";
				$whereStatement = $whereStatement." OR (contacts_cstm.alt_lang_first_c LIKE '%".$searchStringArray[0]."%' AND contacts_cstm.alt_lang_last_c LIKE '%".$searchStringArray[1]."%')";
				$whereStatement = $whereStatement." OR (contacts_cstm.alt_lang_first_c LIKE '%".$searchStringArray[1]."%' AND contacts_cstm.alt_lang_last_c LIKE '%".$searchStringArray[0]."%')";

				$result = $bean->get_list("", $whereStatement);
				$returnList = $result['list'];
			} else {
				$this->NOTES_LOGGER->logMessage("warn", __METHOD__." Search string not in proper form");
			}
		} else {
			$this->NOTES_LOGGER->logMessage("warn", __METHOD__." unsupported bean type");
		}

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $returnList;
	}

	/**
	 * Get ID(s) of either an Account or Contact with a given email
	 *
	 * @param string $module 'Accounts' or 'Contacts'
	 * @param string $searchString email address to search on
	 * @return ids found with the given email address
	 */
	private function getIDByEmail($module, $searchString) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." Get $module id $searchString");

		global $beanList, $beanFiles;

		$beanType = 'EmailAddresses';

		$beanName = $beanList[$beanType];
		require_once $beanFiles[$beanName];
		$bean = new $beanName();

		$linked_ids = $bean->getRelatedId($searchString, $module);

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $linked_ids;
	}

	/**
	 * Perform a Sugar database query, given a SQL statement, a sugar seed bean, and optional max_result/offset
	 *
	 * @param string $main_query - SQL query statement
	 * @param string $seed - Sugar bean where the database conncetion comes from
	 * @param string $max_result - Maximum number of results returned
	 * @param string $offset - Offset number of results returned
	 * @return result-set of the query
	 */
	private function doDBQuery($main_query, $seed, $max_result, $offset) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." query: $main_query");
		
		$result = '';
		if($max_result <= -1) {
			$result = $seed->db->query($main_query);
		} else {
			$result = $seed->db->limitQuery($main_query,
			$offset,
			$max_result + 1);
		}

		$output_list = array();
		while($row = $seed->db->fetchByAssoc($result, false)) { //false turns off encoding of results from DB
			$output_list[] = $row;
		}

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $output_list;
	}

	/**
	 * Given a view def entry, process it into form needed for client
	 *
	 * @param array $entry view def entry
	 * @param array $callFieldDefs call bean field definitions
	 */
	private function processViewDefEntry($entry, $callFieldDefs) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$fieldArray = array();
		$isParentEntry = false;

		$fieldName = $entry['name'];
		$fieldLabel = $entry['label'];

		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." fieldname: $fieldName");

		//make sure field exists in the field defs array
		if(array_key_exists($fieldName, $callFieldDefs)) {
			$fieldDef = $callFieldDefs[$entry['name']];
			$fieldArray['name'] = $fieldDef['name'];

			//give priority to the field label defined in viewdefs
			if(!is_null($fieldLabel)) {
				$fieldArray['label'] = translate($fieldLabel,'Calls');
			} else {
				//$fieldArray['displayName'] = $GLOBALS['app_strings'][$fieldDef['vname']];
				$fieldArray['label'] = translate($fieldDef['vname'],'Calls');
			}

			$fieldArray['type'] = $fieldDef['type'];
			$fieldArray['len'] = $fieldDef['len'];
			$fieldArray['required'] = $fieldDef['required'];

			//need to handle subfields if they exist
			if(isset($entry['fields'])) {
				$isParentEntry = true;
				$subFields = $entry['fields'];
				foreach($subFields as $subFieldKey => $subField) {
					$fieldArray['subfields'][] = $this->processViewDefEntry($subField, $callFieldDefs);
				}
			}

			//if type is enum, need to retrieve options
			if(($fieldDef['type'] === 'enum' || $fieldDef['type'] === 'multienum') && !$isParentEntry) {
				$fieldArray['options'] = $GLOBALS['app_list_strings'][$fieldDef['options']];
			}
		}
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $fieldArray;
	}

	/**
	 * Build SQL query used in typeahead service
	 *
	 * @param array $selectFields result fields to return
	 * @param string $table table to search in
	 * @param array $myitems list of my items
	 * @param array $whereFields fields to compare search string against
	 * @param string or array $searchString string to match against, or array of strings to match against
	 * @param string $useMyItems 'true' or 'false' to include myitems in query
	 *
	 * @return string SQL query
	 */
	private function buildTypeAheadQuery($selectFields, $table, $myitems, $whereFields, $searchString, $useMyItems) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." searchstring: $searchString");

		$query = '';

		//TODO: beef up input sanity check
		if((($useMyItems == 'true' && count($myitems) > 0) || ($useMyItems == 'false')) && count($selectFields) > 0) {
			/* first part of query is to match against myitems */
			$query = 'SELECT ';

			foreach($selectFields as $selectField) {
				$query = $query.$selectField.',';
			}

			$len=strlen($query);
			$query = substr($query,0,($len-1)); //chop off last ','
			$query = $query.' FROM '.$table.' WHERE (';

			if(isset($myitems) && $useMyItems != 'false' && count($myitems) > 0) {
				$this->NOTES_LOGGER->logMessage("debug", __METHOD__." filtering on myitems");
				
				//check myitems array to make sure it's not bigger than our limit,
				//if it is, slice the array to fit
				global $sugar_config;
				$maxMyItems = self::$NOTES_MAX_NUM_MYITEMS_TYPEAHEADS;
				if(isset($sugar_config['notes_max_num_myitems_typeaheads'])) {
					$maxMyItemsProp = $sugar_config['notes_max_num_myitems_typeaheads'];
					if(is_int($maxMyItemsProp) && $maxMyItemsProp > 0) {
						$maxMyItems = $maxMyItemsProp;
					}
				}
				
				if(count($myitems) > $maxMyItems) {
					$this->NOTES_LOGGER->logMessage("warn", __METHOD__." user has more than $maxMyItems myitems, limiting query");
					$myitems = array_slice($myitems, 0, $maxMyItems);
				}

				foreach($myitems as $item) {
					$query = $query.$table.'.id = \''.$item['id'].'\' OR ';
				}
					
				$len=strlen($query);
				$query = substr($query,0,($len-4)); //chop off last ' OR '
				$query = $query.')';
					
				/* second part of query is to match against name */
				$query = $query.' AND (';
			}

			//defect 12843: we may be submitting multiple search strings in, to handle user/contact names
			if(is_array($searchString)) {
				//we've got multiple search strings we need to handle
				foreach($searchString as $searchStringPiece) {
					foreach($whereFields as $whereField) {
						$query = $query.$whereField.' LIKE \''.db2_escape_string($searchStringPiece).'%\' OR ';
					}
				}
			} else {
				//searchString must be a single string
				foreach($whereFields as $whereField) {
					$query = $query.$whereField.' LIKE \''.db2_escape_string($searchString).'%\' OR ';
				}
			}

			$len=strlen($query);
			$query = substr($query,0,($len-4)); //chop off last ' OR '
			$query = $query.')';
		}

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $query;
	}
	
	/**
	 * Use IBMQuickSearch to get results based on the given input array.
	 * 
	 * This method is replacing our original way of getting typeahead reuslts:
	 * ($this->buildTypeAheadQuery())
	 * 
	 * @param $input array containing query parameters
	 * @return string search results, or empty if no results
	 */
	private function doIBMQuickSearchQuery($input) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		
		$queryResult = '';

		//IBMQuickSearchCustom is an IBM class, going to use QuickSearchCustom (sugar class) instead
//		if(file_exists('custom/modules/Home/IBMQuickSearchCustom.php')) {
//			require_once('custom/modules/Home/IBMQuickSearchCustom.php');
//			$quicksearchQuery = new QuickSearchQueryCustomIBMStrategy();
		if(file_exists('custom/modules/Home/QuickSearchCustom.php')) {
			require_once('custom/modules/Home/QuickSearchCustom.php');
			$quicksearchQuery = new QuickSearchQueryCustomStrategy();
			
			$method = !empty($input['method']) ? $input['method'] : 'query';
			if(method_exists($quicksearchQuery, $method)) {
			 	try {
					$queryResultArray = $quicksearchQuery->$method($input);	
			  		$queryResult = json_decode($queryResultArray);
			 	} catch(Exception $e) {
			 		$this->NOTES_LOGGER->logMessage("fatal", __METHOD__." Caught exception: ".$e->getMessage());
			 	}   
			} else {
				$this->NOTES_LOGGER->logMessage("fatal", __METHOD__." Unable to search, IBMQuickSearch method not found");
			}
		} else {
			$this->NOTES_LOGGER->logMessage("fatal", __METHOD__." Unable to search, IBMQuickSearch class not found");
		}
			
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $queryResult;
	}
	
	/**
	 * Get list of accounts that match search string
	 *
	 * @param User $current_user current user's bean
	 * @param string $searchString string to search for
	 * @param integer $resultLimit number of results to return
	 * @param string $myItems whether or not to use user's myitems list -- 'true' or 'false'
	 * @param string $filter string to filter results against -- in this case, expecting city name
	 *
	 * @deprecated use $this->getTypeAheadAccounts11() instead
	 *
	 * @return array of typeahead search results
	 */
	private function getTypeAheadAccounts($current_user, $searchString, $resultLimit, $myItems, $filter) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." $searchString , $resultLimit , $myItems");

		$accountsQueryResult = '';

		$userid = $current_user->id; //get current user's uuid
		//$myAccounts = IBMHelper::getClass('MyItems')->getMyItems('Accounts', $userid, array());
		$myAccounts = array();
		
		
		//changing client_id to ccms_id 15937
		$selectFields = array('accounts.id','accounts.name','accounts.ccms_id');
		$table = 'accounts';
		$whereFields = array('accounts.name','accounts.ccms_id');
		$query = $this->buildTypeAheadQuery($selectFields, $table, $myAccounts, $whereFields, $searchString, $myItems);
		$accountsQueryResult = $this->doDBQuery($query, $current_user, $resultLimit, null);

		if(isset($filter) && strlen($filter) > 0) {
			$accountsQueryResult = $this->filterAccountResults($accountsQueryResult, $filter);
		}

		$accountsQueryResult = $this->getTypeAheadAccountsAdditionalInfo($accountsQueryResult);

		//16172: need to make sure client_id is set -- will just duplicate ccms_id value
		foreach($accountsQueryResult as &$accountResult) {
			$accountResult['client_id'] = $accountResult['ccms_id'];
		}
			
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $accountsQueryResult;
	}

	/**
	 * Given a list of accounts (from $this->getTypeAheadAccounts), get more information about the accounts
	 * including the account's primary physical address and the list of industries associated with the account.
	 * @param array $accounts array of account info to be added to
	 * @return arrray of account info with address and industry added
	 */
	private function getTypeAheadAccountsAdditionalInfo($accounts) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);

		$numOfAccounts = count($accounts);
		for($i = 0; $i < $numOfAccounts; $i++) {
			$account = &$accounts[$i];
			$accountBean = $this->getBean($account['id'], "Accounts");

			/*BEGIN ACCOUNT ADDRESS*/
			$relationship_name = 'accounts_ibm_addresses';
			$accountBean->load_relationship($relationship_name);
			$addressBeans = $accountBean->$relationship_name->getBeans();
			$accountCity = "";
			$accountState = "";
			$accountCountry = "";
			foreach($addressBeans as $addressBean) {
				//we will use the first address that is marked as physical and primary
				if($addressBean->address_type == 'PHY' && $addressBean->is_primary == 1) {
					$accountCity = $addressBean->city;
					$accountState = $addressBean->state;
					$accountCountry = $addressBean->country;
					break;
				}
			}
			$account['city'] = $accountCity; //note we're overriding the city set by the $this->filterAccountResults
			$account['state'] = $accountState;
			$account['country'] = $accountCountry;
			/*END ACCOUNT ADDRESS*/
			 	
			/*BEGIN ACCOUNT INDUSTRY*/
			$account['industry'] = $this->getAccountIndustryData($accountBean);
			/*END ACCOUNT INDUSTRY*/
		}

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $accounts;
	}

	/**
	 * Filter a set of account results (from $this->getTypeAheadAccounts) by input city filter string
	 * Since we just have the account ID, we load the account bean, then load the related address beans.
	 * If the account doesn't have an address city that begins with the filter, we remove it from the results
	 *
	 * @param array $accounts account typeahead search results
	 * @param string $filter city filter
	 *
	 * @return array of filtered typeahead search results
	 */
	private function filterAccountResults($accounts, $filter) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);

		$numOfAccounts = count($accounts);

		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." filter is $filter and input set size is $numOfAccounts");
		
		for($i = 0; $i < $numOfAccounts; $i++) {
			$account = &$accounts[$i];
			$filterPassed = false;
			$accountBean = $this->getBean($account['id'], "Accounts");
			$relationship_name = 'accounts_ibm_addresses';
			$accountBean->load_relationship($relationship_name);
			$addressBeans = $accountBean->$relationship_name->getBeans();
			$accountAddresses = array();
			foreach($addressBeans as $addressBean) {
				$accountAddresses[] = $addressBean->city;
				if(stripos($addressBean->city, $filter, 0) === 0) {
					$account['city'] = $addressBean->city;
					$filterPassed = true;
					break;
				}
			}

			//if city doesn't match filter, remove it from result set
			if(!$filterPassed) {
				unset($accounts[$i]);
			}
		}

		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." output set size is ".count($accounts));
		$this->NOTES_LOGGER->logExit(__METHOD__);
		
		//if we removed any from the set, we need to reorder to remove gaps, to make JSON output neater
		return array_values($accounts);
	}
	
	/**
	 * Get list of opportunities that match search string
	 *
	 * @param User $current_user current user's bean
	 * @param string $searchString string to search for
	 * @param integer $resultLimit number of results to return
	 * @param string $myItems whether or not to use user's myitems list -- 'true' or 'false'
	 *
	 * @return array of typeahead search results
	 */
	private function getTypeAheadOpportunities($current_user, $searchString, $resultLimit, $myItems) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." $searchString , $resultLimit , $myItems");
		
		$opportunitiesQueryResult = '';

		if($myItems == 'true') {
			$userid = $current_user->id; //get current user's uuid
			//$myOpportunities = IBMHelper::getClass('MyItems')->getMyItems('Opportunities', $userid, array());
			$myOpportunities = array();
		} else {
			$myOpportunities = array();
		}
		
		$selectFields = array('opportunities.id','opportunities.name','opportunities.description','opportunities.date_closed');
		$table = 'opportunities';
		$whereFields = array('opportunities.name','opportunities.description');
		$query = $this->buildTypeAheadQuery($selectFields, $table, $myOpportunities, $whereFields, $searchString, $myItems);
		if(!empty($query)) { 
			$opportunitiesQueryResult = $this->doDBQuery($query, $current_user, $resultLimit, null);
		}

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $opportunitiesQueryResult;
	}

	/**
	 * Get list of contacts that match search string
	 *
	 * @param User $current_user current user's bean
	 * @param string $searchString string to search for
	 * @param integer $resultLimit number of results to return
	 * @param string $myItems whether or not to use user's myitems list -- 'true' or 'false'
	 *
	 * @return array of typeahead search results
	 * 
	 * @deprecated use $this->getTypeAheadContacts11() instead
	 */
	private function getTypeAheadContacts($current_user, $searchString, $resultLimit, $myItems) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." $searchString , $resultLimit , $myItems");
		
		$contactsQueryResult = '';

		$userid = $current_user->id; //get current user's uuid
		//$myContacts = IBMHelper::getClass('MyItems')->getMyItems('Contacts', $userid, array());
		$myContacts = array();

		//defect 12843: we need to split on spaces for people names and search both first/last names against
		//the individual pieces. this means the input into $this->buildTypeAheadQuery() will be an array
		$searchStringPieces = explode(" ", $searchString);
		$searchStringPiecesOriginal = $searchStringPieces; //keep a copy of search string pieces for filtering later
		$searchStringPiecesSize = sizeof($searchStringPieces);
		for($i=0; $i < $searchStringPiecesSize; $i++) {
			if(strlen($searchStringPieces[$i]) < self::$TYPEAHEAD_CHAR_LIMIT) unset($searchStringPieces[$i]);
		}
		//if we removed any array members, need to reorder to remove gaps
		$searchStringPieces = array_values($searchStringPieces);

		$selectFields = array('contacts.id','contacts.first_name','contacts.last_name', 'contacts.title', 'contacts.primary_address_city', 'contacts.primary_address_state', 'contacts.primary_address_country');
		$table = 'contacts';
		$whereFields = array('contacts.first_name','contacts.last_name');
		$query = $this->buildTypeAheadQuery($selectFields, $table, $myContacts, $whereFields, $searchStringPieces, $myItems);
		$contactsQueryResult = $this->doDBQuery($query, $current_user, $resultLimit, null);

		//if the original input was separated by a space, we need to post process and make sure returned results make sense
		//only handling one space currently, so should only be 2 pieces
		if(count($searchStringPiecesOriginal) >= 2) {
			$piece1 = $searchStringPiecesOriginal[0];
			$piece2 = $searchStringPiecesOriginal[1];
			
			$contactsQueryResultSize = sizeof($contactsQueryResult);
			for($i=0; $i < $contactsQueryResultSize; $i++) {
				$first = $contactsQueryResult[$i]['first_name'];
				$last = $contactsQueryResult[$i]['last_name'];
				$combinedFL = mb_strtolower("$first $last");
				$combinedLF = mb_strtolower("$last $first");
				$searchStringLowerCased = mb_strtolower($searchString);

				if($this->stringBeginsWith($combinedFL, $searchStringLowerCased)) {
					//do nothing
				} else if($this->stringBeginsWith($combinedLF, $searchStringLowerCased)) {
					//do nothing
				} else if($this->stringBeginsWith($first, $piece1) && $this->stringBeginsWith($last, $piece2)) {
					//do nothing
				} else if($this->stringBeginsWith($last, $piece1) && $this->stringBeginsWith($first, $piece2)) {
					//do nothing
				} else {
					//we should filter this result
					unset($contactsQueryResult[$i]);
				}
			}
			
			//if we removed any array members, need to reorder to remove gaps
			$contactsQueryResult = array_values($contactsQueryResult);
		}
		
		//need to return contact's account name, using bean method, may need to change this for performance?
		foreach($contactsQueryResult as &$contact) {
			$contactBean = $this->getBean($contact['id'], 'Contacts');
			$contact['account_name'] = html_entity_decode($contactBean->account_name,ENT_QUOTES);
			$contact['account_id'] = $contactBean->account_id;
		}

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $contactsQueryResult;
	}
	
	/**
	 * Get list of users that match search string
	 *
	 * @param User $current_user current user's bean
	 * @param string $searchString string to search for
	 * @param integer $resultLimit number of results to return
	 * @param string $myItems whether or not to use user's myitems list -- 'true' or 'false'
	 *
	 * @deprecated use $this->getTypeAheadUsers11() instead
	 * 
	 * @return array of typeahead search results
	 */
	private function getTypeAheadUsers($current_user, $searchString, $resultLimit) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." $searchString , $resultLimit , $myItems");
		
		$usersQueryResult = '';

		//no concept of 'myusers'
		$myUsers = array();
		$myItems = 'false';

		//defect 12843: we need to split on spaces for people names and search both first/last names against
		//the individual pieces. this means the input into $this->buildTypeAheadQuery() will be an array
		$searchStringPieces = explode(" ", $searchString);
		$searchStringPiecesOriginal = $searchStringPieces; //keep a copy of search string pieces for filtering later
		$searchStringPiecesSize = sizeof($searchStringPieces);
		for($i=0; $i < $searchStringPiecesSize; $i++) {
			if(strlen($searchStringPieces[$i]) < self::$TYPEAHEAD_CHAR_LIMIT) unset($searchStringPieces[$i]);
		}
		//if we removed any array members, need to reorder to remove gaps
		$searchStringPieces = array_values($searchStringPieces);

		$selectFields = array('users.id','users.first_name','users.last_name','users.user_name');
		$table = 'users';
		$whereFields = array('users.first_name','users.last_name');
		$query = $this->buildTypeAheadQuery($selectFields, $table, $myUsers, $whereFields, $searchStringPieces, $myItems);
		$usersQueryResult = $this->doDBQuery($query, $current_user, $resultLimit, null);

		//if the original input was separated by a space, we need to post process and make sure returned results make sense
		//only handling one space currently, so should only be 2 pieces
		if(count($searchStringPiecesOriginal) >= 2) {
			$piece1 = $searchStringPiecesOriginal[0];
			$piece2 = $searchStringPiecesOriginal[1];
			
			$usersQueryResultSize = sizeof($usersQueryResult);
			for($i=0; $i < $usersQueryResultSize; $i++) {
				$first = $usersQueryResult[$i]['first_name'];
				$last = $usersQueryResult[$i]['last_name'];
				$combinedFL = mb_strtolower("$first $last");
				$combinedLF = mb_strtolower("$last $first");
				$searchStringLowerCased = mb_strtolower($searchString);

				if($this->stringBeginsWith($combinedFL, $searchStringLowerCased)) {
					//do nothing
				} else if($this->stringBeginsWith($combinedLF, $searchStringLowerCased)) {
					//do nothing
				} else if($this->stringBeginsWith($first, $piece1) && $this->stringBeginsWith($last, $piece2)) {
					//do nothing
				} else if($this->stringBeginsWith($last, $piece1) && $this->stringBeginsWith($first, $piece2)) {
					//do nothing
				} else {
					//we should filter this result
					unset($usersQueryResult[$i]);
				}
			}
			
			//if we removed any array members, need to reorder to remove gaps
			$usersQueryResult = array_values($usersQueryResult);
		}
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $usersQueryResult;
	}

	/**
	 * Get list of industry key/values associated with the given accountBean
	 *
	 * @param accountBean $accountBean account to look up industry data
	 * @return array industry keys mapped to their string values
	 */
	private function getAccountIndustryData($accountBean) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);

		//33000 - industry data is now stored with accounts, removing industry bean relationship
		$accountIndustry = array();
		
		//industry_class_name is stored as a key -- "KE" for example
		//$industryClassKey = $accountBean->indus_class_name;
		$industryClassRollup = $accountBean->indus_class_rollup;
		if(!empty($industryClassRollup)) {
			$industryClassArray = $this->commaCaretStringToArray($industryClassRollup);
			foreach($industryClassArray as $industryClassKey) {
				if(!empty($industryClassKey)) {
					$industryClassName = $this->getAVLValue('INDUSTRY_CLASS', $industryClassKey);
					//store key/string mapping
					$accountIndustry[$industryClassKey] = $industryClassName;
				}		
			}
		}
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $accountIndustry;
	}

	/**
	 * Get an array with the given accountbean's ID + the ID of any child accounts.
	 * Uses IBMHelperAccountsHierarchy to get list of child IDs.
	 * 
	 * @param SugarBean accountBean parent Account sugar bean
	 * @return array account ids as strings
	 */
	private function getAccountAndChildrenIDs($accountBean) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		
		$collectedAccounts = array();
		
		if(!empty($accountBean)) {
			//need to get any child accounts the account has (if its a DC for example)
			$ccms_level = $accountBean->ccms_level;
			$accountid = $accountBean->id;
			if(!empty($ccms_level) && !empty($accountid)) {
				require_once("custom/include/Helpers/IBMHelperAccountsHierarchy.php");
				$collectedAccounts = IBMHelper::getClass('AccountsHierarchy')->getChildIDsOfClient($accountid, $ccms_level);
			} else {
				$collectedAccounts = array();
			}
			$collectedAccounts[] = $accountid;
		}
				
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $collectedAccounts;
	}
	
	/**
	 * Look up the AVL value given the AVL name, key, and language.
	 * If not found, the input key will be returned.
	 *
	 * //TODO: right now, language is hardcoded for en_us, might need to address this later
	 *
	 * @param string $avlName name corresponding to IBM_AVLDICTIONARYMAP.IBM_DICTIONARY_NAME
	 * @param string $key key corresponding to avl value
	 * @param string $language language of string to lookup
	 */
	private function getAVLValue($avlName, $key, $language="en_us", $sugaravl=false) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logMessage("debug", __METHOD__." looking up avl value -- avl: $avlName ,key: $key ,language: $language ,issugaravl: $sugaravl");

		require_once('modules/ibm_AVLMap/ibm_AVLMap.php');
		$retValue = $key;

		$avlMap = new ibm_AVLMap();

		/*
		public function getAVLs(
        $dictionaryName,
        $key,
        $status = self::STATUS_ACTIVE,
        $isIBMDictionary = true,
        $parent_id = null
		 */
		if($sugaravl) {
			$avl = $avlMap->getAVLs($avlName, $key, 0, false);
		} else {
			$avl = $avlMap->getAVLs($avlName, $key);
		}
		
		if(isset($avl[0]->ibm_avl_value)) {
			$retValue = $avl[0]->ibm_avl_value;
		}

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $retValue;
	}

	/**
	 * Generate formatted ammount string in this format:
	 * [amount] [native currency symbol] ([amount in user's currency] [user's currency symbol])
	 * ex: 16k Af (1190k pounds)
	 *
	 * @param $currencyID native currency ID
	 * @param $amount amount in native currency
	 * @param $currencyIDuser user's currency ID
	 */
	private function getAmountStringForUser($currencyID, $amount, $currencyIDuser) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);

		$ret = $amount;

		if(isset($currencyID) && isset($amount) && isset($currencyIDuser)) {
			$amountFormatted = $this->getFormattedAmount($currencyID, $amount);

			//20794 -- shouldn't display the currency twice if the user's currency is same as input
			if($currencyID === $currencyIDuser) {
				$ret = $amountFormatted;
			} else {		
				//convert native amount to amount in user's currency
				$amountUser = IBMHelper::getClass('Currencies')->convertCurrency($amount, $currencyID, $currencyIDuser, 0);
				$amountFormattedUser = $this->getFormattedAmount($currencyIDuser, $amountUser);

				$ret = $amountFormatted.' ('.$amountFormattedUser.')';
			}
		}

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret;
	}

	/**
	 * Given a currency ID and value, format the value accordingly.
	 *
	 * @see custom/include/Smarty/plugins/function.sugar_currency_format.php
	 *
	 * @param $currencyID sugar currency ID
	 * @param $amount amount to format
	 */
	private function getFormattedAmount($currencyID, $amount) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
			
		$decimal_pos = strpos($amount, ".");
		if($decimal_pos !== false){
			$amount = substr($amount, 0, $decimal_pos);
		}
			
		$real_round = 3;
		$real_decimals = 3;
			
		$currency_settings = array(
			"human" => true, 
			"currency_id" => $currencyID, 
			"currency_symbol" => true, 
			"symbol_space" => true
		);

		$amountFormatted = format_number($amount, $real_round, $real_decimals, $currency_settings);

		//the result of format_number will have currency symbol as prefix,
		//this switches it to suffix
		$amountFormatted = preg_replace('/([^&]+)&nbsp;(.*)/i', '$2 $1', $amountFormatted);

		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $amountFormatted;
	}

	/**
	 * Given an array, build ,^-delimited string:
	 * ^Item1^,^Item2^,^Item3^ for example.
	 *
	 * @param array $array
	 */
	private function buildCommaCaretStringFromArray($array) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
			
		$output = "";
			
		foreach($array as $value) {
			$output .= "^".$value."^,";
		}
			
		$output = trim($output, ",");
			
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $output;
	}
	
	private function commaCaretStringToArray($string) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$ret = array();
		
		$splitArray = preg_split("/(\^,)+/", $string);
		$ret = str_replace("^", "", $splitArray);
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret;
	}
	
	/**
	 * Given a list of IDs, get a string in the form of:
	 * "(id1,id2,id3)"
	 * 
	 * Intended for use in SQL queries
	 */
	private function getSQLInClause($ids) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		
		//build the "IN" clause of query with the input + child account ids
		$inStatement = "(";
		
		if(!empty($ids)) {
			$total = count($ids);
			$i = 0;
			foreach($ids as $anID) {
				$inStatement .= "'".$anID."'";
				
				$isLastOne = ++$i == $total;
				if(!$isLastOne) {
					$inStatement .= ",";
				}
			}
		}
		
		$inStatement .= ")";
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $inStatement;
	}
	
	private function stringBeginsWith($string, $prefix) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$this->NOTES_LOGGER->logExit(__METHOD__);
    	return (strncmp($string, $prefix, strlen($prefix)) == 0);
	}
	
	/**
	 * Check if collab-web IBMFollowingService exists, if so include the class
	 * and return true. Otherwise return false.
	 */
	private function checkForConnectionsFollowingService() {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$ret = false;
		
		if(file_exists('custom/include/IBMConnections/Services/IBMFollowingService.php')) {
			include_once 'custom/include/IBMConnections/Services/IBMFollowingService.php';
			if(class_exists('IBMFollowingService')) {
				$ret = true;
			}
		}
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret;
	}
	
	private function checkIfFollowing($moduleType, $id) {
		$this->NOTES_LOGGER->logEntry(__METHOD__);
		$ret = "NA";

		if($this->checkForConnectionsFollowingService() && method_exists('IBMFollowingService', 'isFollowing')) {
			$bean = $this->getBean($id, $moduleType);
			if(!empty($bean->id)) {
				$followService = new IBMFollowingService();
				$ret = $followService->isFollowing($bean);
				
				//if($ret === false) { $ret = "false"; }
				//if($ret === true) { $ret =  "true"; }
				if(is_null($ret)) { $ret = "NA"; }
			}
		}
		
		$this->NOTES_LOGGER->logExit(__METHOD__);
		return $ret;
	}
}

?>
