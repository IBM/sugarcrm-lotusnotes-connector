<?php
if (! defined('sugarEntry') || ! sugarEntry)
    die('Not A Valid Entry Point');

require_once ("notesHelper_Abstract.php");

class notesHelper_GetInfo extends notesHelper_Abstract
{

    // $accountids, $contactids, $opptyids, $contactnames, $contactemails, $opptynames, $clientids
    public static $INPUT_DATA_TYPES = array(
        'accountid',
        'contactid',
        'opptyid',
        'contactname',
        'contactemail',
        'ccmsid'
    );

    public static $RESULT_TYPES = array(
        'basecard',
        'followed',
        'oppties',
        'rlis',
        'recentlist'
    );

    public function getResult($input)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . ' input is ' . json_encode($input));
        $this->NOTES_LOGGER->logMessage("debug",
            __METHOD__ . ' processing getInfo request for ' . count($input) . ' items');

        $output = array();

        foreach ($input as $item) {
            $itemInfo = $this->getItemInfo($item);
            if (! empty($itemInfo)) {
                $output = array_merge($output, $itemInfo);
            }
        }

        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . ' output is ' . json_encode($output));

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    /**
     * Given an input array item, figure out what information is needed and return the info.
     */
    private function getItemInfo($item)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();

        if ($this->verifyValidItemFormat($item)) {
            $inputDataType = $item[0];
            $inputData = $item[1];
            $resultType = $item[2];
            $outputKey = $item[3];

            $this->NOTES_LOGGER->logMessage("debug",
                __METHOD__ . " Fetching $resultType data for $inputDataType $inputData");

            $accountLookup = false;
            $contactLookup = false;
            $opptyLookup = false;
            $itemids = array(); // one or more item ids to look up -- only possible multiples come from contact name or
                                // email

            if ($inputDataType === 'accountid') {
                $itemids = array(
                    $inputData
                );
                $accountLookup = true;
            } else
                if ($inputDataType === 'contactid') {
                    $itemids = array(
                        $inputData
                    );
                    $contactLookup = true;
                } else
                    if ($inputDataType === 'opptyid') {
                        $itemids = array(
                            $inputData
                        );
                        $opptyLookup = true;
                    } else
                        if ($inputDataType === 'contactname') {
                            // TODO: haven't been able to unit test this because contact typeahead isn't working for
                            // some reason
                            $itemids = $this->getContactIDByName($inputData);
                            $this->NOTES_LOGGER->logMessage("debug",
                                __METHOD__ . " $inputDataType $inputData maps to " . json_encode($itemids));
                            $contactLookup = true;
                        } else
                            if ($inputDataType === 'contactemail') {
                                // if contact email, look up contact id
                                $itemids = $this->getIDsByEmail('Contacts', $inputData);
                                $this->NOTES_LOGGER->logMessage("debug",
                                    __METHOD__ . " $inputDataType $inputData maps to " . json_encode($itemids));
                                $contactLookup = true;
                            } else
                                if ($inputDataType === 'ccmsid') {
                                    // if ccms id, look up account id
                                    $itemid = $this->getAccountIDByClientID($inputData);
                                    $itemids = array(
                                        $itemid
                                    );
                                    $this->NOTES_LOGGER->logMessage("debug",
                                        __METHOD__ . " $inputDataType $inputData maps to " . json_encode($itemids));
                                    $accountLookup = true;
                                }

            if (! empty($itemids)) {
                foreach ($itemids as $itemid) {
                    if ($accountLookup) {
                        $output[$outputKey][$itemid] = $this->getInfoAccount($itemid, $resultType);
                    } else
                        if ($contactLookup) {
                            $output[$outputKey][$itemid] = $this->getInfoContact($itemid, $resultType);
                        } else
                            if ($opptyLookup) {
                                $output[$outputKey][$itemid] = $this->getInfoOppty($itemid, $resultType);
                            }
                }
            } else {
                $this->NOTES_LOGGER->logMessage("warn",
                    __METHOD__ . " Nothing to lookup -- empty item IDs from $inputDataType $inputData");
            }
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . ' Invalid item input format, skipping');
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getInfoAccount($id, $resultType)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();
        if (! empty($id)) {
            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " getting $resultType for $id");
            if ($resultType === 'basecard') {
                $basecard = $this->getBasecardAccount($id);
                if (! empty($basecard)) {
                    $output = $basecard;
                }
            } else
                if ($resultType === 'followed') {
                    $followed = $this->getFollowedAccount($id);
                    if (! empty($followed)) {
                        $output = $followed;
                    }
                } else
                    if ($resultType === 'oppties') {
                        $oppties = $this->getOpptiesAccount($id);
                        if (! empty($oppties)) {
                            $output = $oppties;
                        }
                    } else
                        if ($resultType === 'recentlist') {
                            $recentlist = $this->getRecentListAccount($id);
                            if (! empty($recentlist)) {
                                $output = $recentlist;
                            }
                        } else {
                            $this->NOTES_LOGGER->logMessage("warn",
                                __METHOD__ . " invalid account result type: $resultType");
                        }
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty account id");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getBasecardAccount($id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        // we want to log basecard requests at info for tracking purposes
        $this->logGetInfoRequest();

        $output = array();

        $accountBean = $this->getBean($id, 'Accounts');
        if (! empty($accountBean)) {

            // initially pull address directly from account bean
            $priPhysicalStreet = $accountBean->billing_address_street;
            $priPhysicalCity = $accountBean->billing_address_city;
            $priPhysicalState = $accountBean->billing_address_state;
            $priPhysicalPostal = $accountBean->billing_address_postalcode;
            $priPhysicalCountry = $accountBean->billing_address_country;

            // 80623 - CI simplification, single char key
            $indus_industry = $accountBean->indus_industry;

             /*
             * industry data stored in DB depending on the ccms level go up in hiearchy to the site if needed to get
             * industry
             */
            if ((strtolower($accountBean->ccms_level) === 'dc')) {

                $this->NOTES_LOGGER->logMessage(
                    "debug",
                    __METHOD__ . " ccms_level is 'dc', getting industry via site: ");

                $defaultSiteCCMSID = $accountBean->default_site_id;
                $defaultSiteID = $this->getAccountIDByClientID($defaultSiteCCMSID);

                $this->NOTES_LOGGER->logMessage(
                    "debug",
                    __METHOD__ . " get bean for default site: " . $defaultSiteID . ' - ' . $defaultSiteCCMSID);
                $defaultSiteBean = $this->getLightBean($defaultSiteID, 'Accounts');

                if (! empty($defaultSiteBean)) {
                    $this->NOTES_LOGGER->logMessage(
                        "debug",
                        __METHOD__ . " get industry using default site: " . $defaultSiteID . ' - ' . $defaultSiteCCMSID);
                    $industry = $this->getAccountIndustryData($defaultSiteBean);
                } else {
                    $this->NOTES_LOGGER->logMessage(
                        "debug",
                        __METHOD__ . " site bean not found for id: " . $defaultSiteID . ' - ' . $defaultSiteCCMSID);
                }
            } else {
                $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " get industry from account itself");
                // gets key/value pair of industry classname key and cooresponding string value
                $industry = $this->getAccountIndustryData($accountBean);
            }

            // get state, look up by sugar avl key, ibm dictionary is STATE_ABBREV_SFA
            if (! empty($priPhysicalState)) {
                $priPhysicalState = $this->getAVLValue('state_list', $priPhysicalState, 'en_us', true);
            }

            // get account tags
            $tagArray = array();
            $recordTags = IBMHelper::getClass('Tags')->getRecordTags($accountBean);
            foreach ($recordTags as $tag) {
                $tagArray[] = html_entity_decode($tag, ENT_QUOTES); // 3178
            }

            // OUTPUT
            $output['name'] = html_entity_decode(str_replace('&nbsp;', ' ', $accountBean->name));
            $output['industry'] = $industry;
            $output['indus_industry'] = $indus_industry;
            $output['website'] = $accountBean->website;
            $output['clientid'] = $accountBean->ccms_id;
            $output['clientrep']['id'] = $accountBean->leadclient_rep;
            $output['clientrep']['name'] = html_entity_decode($accountBean->leadclient_rep_name, ENT_QUOTES);
            $output['pri_physical_street'] = $priPhysicalStreet;
            $output['pri_physical_city'] = $priPhysicalCity;
            $output['pri_physical_state'] = $priPhysicalState;
            $output['pri_physical_postalcode'] = $priPhysicalPostal;
            $output['pri_physical_country'] = $priPhysicalCountry;
            $output['phone_office'] = $accountBean->phone_office;
            $output['phone_fax'] = $accountBean->phone_fax;
            $output['tags'] = $tagArray;
            $output['defaultSiteCCMSID'] = $defaultSiteCCMSID;
            $output['defaultSiteID'] = $defaultSiteID;
            //
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty account bean");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getFollowedAccount($id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();

        // first call the collab web service
        $isFollowedResult = $this->checkIfFollowing('Accounts', $id);

        // then parse the results for isFollowed
        $isFollowed = $this->getIfFollowingResult($isFollowedResult);
        if (($isFollowed !== true && $isFollowed !== false) && empty($isFollowed)) {
            $isFollowed = false;
        }

        // and isParent
        $isParent = $this->getIfFollowingIsParent($isFollowedResult);
        if (($isParent !== true && $isParent !== false) && empty($isParent)) {
            $isParent = false;
        }

        // and parentFollowInfo
        $parentFollowInfo = $this->getIfFollowingParentFollowInfo($isFollowedResult);
        if (empty($parentFollowInfo)) {
            $parentFollowInfo = array();
        }

        // next call the collab web service to get number of related accounts
        $numOfRelatedAccounts = $this->getRelatedClientsNumber('Accounts', $id);
        if (empty($numOfRelatedAccounts)) {
            $numOfRelatedAccounts = "0";
        }

        // OUTPUT
        $output['followed'] = $isFollowed;
        $output['isParent'] = $isParent;
        $output['relatedClients'] = $numOfRelatedAccounts;
        $output['parentFollowInfo'] = $parentFollowInfo;
        //

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getOpptiesAccount($id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();

        $accountBean = $this->getBean($id, 'Accounts');
        if (! empty($accountBean)) {
            global $current_user;
            $userid = $current_user->id;

            $maxMyItems = $this->getMyItemsMaxOpptys();
            $myOpptiesArray = $this->getMyItemsUsingHADR('Opportunities', $userid, $maxMyItems, false);

            if (! empty($myOpptiesArray)) {
                $accountOppties = array();
                $accountOpptiesByDate = array();

                $allOpptyIDs = IBMHelper::getClass('Accounts')->getAllOppIDs($accountBean, false); // get account
                                                                                                   // opportunities

                $this->NOTES_LOGGER->logMessage("debug",
                    __METHOD__ . " total oppties related to $id: " . count($allOpptyIDs));

                foreach ($allOpptyIDs as $opptyidtemp) {
                    if (array_key_exists($opptyidtemp, $myOpptiesArray)) { // filter against my items
                        $opptyBean = $this->getLightBean($opptyidtemp, 'Opportunities'); // load oppty bean
                        if (! empty($opptyBean)) {
                            $opptydate = $opptyBean->date_closed;
                            $opptystage = $opptyBean->sales_stage;
                            if ($opptystage < 7) { // only show open opportunities
                                $accountOpptiesByDate[$opptyidtemp] = $opptydate; // remember id to date mapping
                            }
                        } else {
                            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " (sort) empty oppty bean");
                        }
                    }
                }

                asort($accountOpptiesByDate); // sort based on date

                // add first five (or less) sorted oppties to array //29722 - changing to 50
                $opptyLimit = 50;
                $totaloppties = count($accountOpptiesByDate);
                if ($totaloppties < $opptyLimit) {
                    $opptyLimit = $totaloppties;
                }

                $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " total MyOppties related to $id: $totaloppties");

                $accountOpptiesByDate = array_keys($accountOpptiesByDate);
                $accountOppties = $this->getOpptyTabInfo($accountOpptiesByDate, $opptyLimit);

                // OUTPUT
                $output['opportunitiesTotal'] = $totaloppties;
                $output['opportunities'] = $accountOppties;
                //
            } else {
                $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " no MyOppties");
            }
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty account bean");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getRecentListAccount($id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();

        $accountBean = $this->getLightBean($id, 'Accounts');
        if (! empty($accountBean)) {
            // get state
            $stateKey = "";
            $stateKey = $accountBean->billing_address_state;
            if (! empty($stateKey)) {
                //  note have to lookup by sugar avl key, ibm dictionary is STATE_ABBREV_SFA
                $stateKey = $this->getAVLValue('state_list', $stateKey, 'en_us', true);
            }

            // OUTPUT
            $output['name'] = html_entity_decode(str_replace('&nbsp;', ' ', $accountBean->name));
            $output['clientid'] = $accountBean->ccms_id;
            $output['pri_physical_street'] = $accountBean->billing_address_street;
            $output['pri_physical_city'] = $accountBean->billing_address_city;
            $output['pri_physical_state'] = $stateKey;
            $output['pri_physical_postalcode'] = $accountBean->billing_address_postalcode;
            $output['pri_physical_country'] = $accountBean->billing_address_country;
            //
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty account bean");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getInfoContact($id, $resultType)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();
        if (! empty($id)) {
            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " getting $resultType for $id");
            if ($resultType === 'basecard') {
                $basecard = $this->getBasecardContact($id);
                if (! empty($basecard)) {
                    $output = $basecard;
                }
            } else
                if ($resultType === 'oppties') {
                    $oppties = $this->getOpptiesContact($id);
                    if (! empty($oppties)) {
                        $output = $oppties;
                    }
                } else
                    if ($resultType === 'recentlist') {
                        $recentlist = $this->getRecentListContact($id);
                        if (! empty($recentlist)) {
                            $output = $recentlist;
                        }
                    } else {
                        $this->NOTES_LOGGER->logMessage("warn",
                            __METHOD__ . " invalid contact result type: $resultType");
                    }
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty contact id");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getBasecardContact($id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        // we want to log basecard requests at info for tracking purposes
        $this->logGetInfoRequest();

        $output = array();

        $contactBean = $this->getBean($id, 'Contacts');
        if (! empty($contactBean)) {
            // get state
            $stateKey = "";
            $stateKey = $contactBean->primary_address_state;
            if (! empty($stateKey)) {
                 //  note have to lookup by sugar avl key, ibm dictionary is STATE_ABBREV_SFA
                $stateKey = $this->getAVLValue('state_list', $stateKey, 'en_us', true);
            }

            // get website via account bean
            $website = "";
            $contact_accountBean = $this->getLightBean($contactBean->account_id, 'Accounts');
            if (! empty($contact_accountBean)) {
                $website = $contact_accountBean->website;
            } else {
                $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty contact_account bean");
            }

            // OUTPUT
            $output['first_name'] = $contactBean->first_name;
            $output['last_name'] = $contactBean->last_name;
            $output['alt_first_name'] = $contactBean->alt_lang_first_c;
            $output['alt_last_name'] = $contactBean->alt_lang_last_c;
            $output['title'] = $contactBean->title;
            $output['account_name'] = html_entity_decode($contactBean->account_name, ENT_QUOTES); // 3178
            $output['account_id'] = $contactBean->account_id;
            $output['phone_work'] = $contactBean->phone_work;
            $output['phone_work_optout'] = $contactBean->phone_work_suppressed;
            $output['phone_mobile'] = $contactBean->phone_mobile;
            $output['phone_mobile_optout'] = $contactBean->phone_mobile_suppressed;
            $output['email1'] = $contactBean->email1;
            $output['email1_optout'] = $contactBean->email_opt_out;
            $output['primary_address_street'] = $contactBean->primary_address_street;
            $output['primary_address_city'] = $contactBean->primary_address_city;
            $output['primary_address_state'] = $stateKey;
            $output['primary_address_postalcode'] = $contactBean->primary_address_postalcode;
            $output['primary_address_country'] = $contactBean->primary_address_country;
            $output['website'] = $website;
            //
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty contact bean");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getOpptiesContact($id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();

        $contactBean = $this->getBean($id, 'Contacts');
        if (! empty($contactBean)) {
            global $current_user;
            $userid = $current_user->id;

            $maxMyItems = $this->getMyItemsMaxOpptys();
            $myOpptiesArray = $this->getMyItemsUsingHADR('Opportunities', $userid, $maxMyItems, false);

            if (! empty($myOpptiesArray)) {
                $contactOppties = array();
                $contactOpptiesByDate = array();

                $opptyBeans = $contactBean->get_linked_beans('opportunities', 'Opportunity');

                $this->NOTES_LOGGER->logMessage("debug",
                    __METHOD__ . " total oppties related to $id: " . count($opptyBeans));

                foreach ($opptyBeans as $opptyBean) {
                    if (! empty($opptyBean)) {
                        $opptyid = $opptyBean->id;
                        $opptydate = $opptyBean->date_closed;
                        $opptystage = $opptyBean->sales_stage;
                        if ($opptystage < 7) {
                          // only show open opportunities
                          // filter against my items
                          // remember id to date mapping
                            if (array_key_exists($opptyid, $myOpptiesArray)) {
                                $contactOpptiesByDate[$opptyid] = $opptydate;
                            }
                        }
                    } else {
                        $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " (sort) empty oppty bean");
                    }
                }

                asort($contactOpptiesByDate); // sort based on date

                // add first five (or less) sorted oppties to array -29722 - changing to 50
                $opptyLimit = 50;
                $totaloppties = count($contactOpptiesByDate);
                if ($totaloppties < $opptyLimit) {
                    $opptyLimit = $totaloppties;
                }

                $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " total MyOppties related to $id: $totaloppties");

                $contactOpptiesByDate = array_keys($contactOpptiesByDate);
                $contactOppties = $this->getOpptyTabInfo($contactOpptiesByDate, $opptyLimit);

                // OUTPUT
                $output['opportunitiesTotal'] = $totaloppties;
                $output['opportunities'] = $contactOppties;

            }
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty contact bean");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getRecentListContact($id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();

        $contactBean = $this->getBean($id, 'Contacts'); // can't use light bean because we need email address info
        if (! empty($contactBean)) {
            // get state
            $stateKey = "";
            $stateKey = $contactBean->primary_address_state;
            if (! empty($stateKey)) {
               // get state, look up by sugar avl key, ibm dictionary is STATE_ABBREV_SFA
                $stateKey = $this->getAVLValue('state_list', $stateKey, 'en_us', true);
            }

            // OUTPUT
            $output['first_name'] = $contactBean->first_name;
            $output['last_name'] = $contactBean->last_name;
            $output['alt_first_name'] = $contactBean->alt_lang_first_c;
            $output['alt_last_name'] = $contactBean->alt_lang_last_c;
            $output['account_name'] = html_entity_decode($contactBean->account_name, ENT_QUOTES); // 3178
            $output['account_id'] = $contactBean->account_id;
            $output['phone_work'] = $contactBean->phone_work;
            $output['phone_work_optout'] = $contactBean->phone_work_suppressed;
            $output['phone_mobile'] = $contactBean->phone_mobile;
            $output['phone_mobile_optout'] = $contactBean->phone_mobile_suppressed;
            $output['email1'] = $contactBean->email1;
            $output['email1_optout'] = $contactBean->email_opt_out;
            //
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty contact bean");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getInfoOppty($id, $resultType)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();
        if (! empty($id)) {
            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " getting $resultType for $id");
            if ($resultType === 'basecard') {
                $basecard = $this->getBasecardOppty($id);
                if (! empty($basecard)) {
                    $output = $basecard;
                }
            } else
                if ($resultType === 'followed') {
                    $followed = $this->getFollowedOppty($id);
                    if (! empty($followed)) {
                        $output = $followed;
                    }
                } else
                    if ($resultType === 'rlis') {
                        $followed = $this->getRLIsOppty($id);
                        if (! empty($followed)) {
                            $output = $followed;
                        }
                    } else
                        if ($resultType === 'recentlist') {
                            $recentlist = $this->getRecentListOppty($id);
                            if (! empty($recentlist)) {
                                $output = $recentlist;
                            }
                        } else {
                            $this->NOTES_LOGGER->logMessage("warn",
                                __METHOD__ . " invalid oppty result type: $resultType");
                        }
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty oppty id");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getBasecardOppty($id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        // we want to log basecard requests at info for tracking purposes
        $this->logGetInfoRequest();

        $output = array();
        global $current_user;

        $opptyBean = $this->getBean($id, 'Opportunities');
        if (! empty($opptyBean)) {
            // get website/industry info from account
            $website = "";
            $industry = array();
            $oppty_accountBean = $this->getLightBean($opptyBean->account_id, 'Accounts');
            if (! empty($oppty_accountBean)) {
                $website = $oppty_accountBean->website;
                $industry = $this->getAccountIndustryData($oppty_accountBean);

                // 80623, Ci simplification, single character key
                $indus_industry = $oppty_accountBean->indus_industry;
            } else {
                $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty oppty_account bean");
            }

            // get sales stage value
            $opptystage = $opptyBean->sales_stage;
            $opptystage = $this->getAVLValue('S_OPTY_STG', $opptystage);
            //

            // get currency string
            $currencyID = $opptyBean->currency_id;
            $currencyIDuser = $current_user->getPreference('currency');
            $opptyAmount = $opptyBean->amount;
            $opptyAmountForUser = $this->getAmountStringForUser($currencyID, $opptyAmount, $currencyIDuser);

            // get assigned user id/name/email
            $assignedUserID = $opptyBean->assigned_user_id;
            $assignedBPID = $opptyBean->assigned_bp_id;
            $assignedUserName = html_entity_decode($opptyBean->assigned_user_name, ENT_QUOTES); // 3178
            $assignedUserEmail = '';
            if (! empty($assignedUserID)) {
                $this->NOTES_LOGGER->logMessage("debug",
                    __METHOD__ . " getting assigned user email for user $assignedUserID");
                $assignedUserBean = $this->getBean($assignedUserID, 'Users');
                if (! is_null($assignedUserBean->last_name)) {
                    $assignedUserEmail = $assignedUserBean->email1;
                }
            } else
                if (! empty($assignedBPID)) {
                    $this->NOTES_LOGGER->logMessage("debug",
                        __METHOD__ . " getting assigned bp info for bp $assignedBPID");
                    $assignedBPBean = $this->getBean($assignedBPID, 'ibm_BusinessPartners');
                    $assignedUserID = $assignedBPID;
                    $assignedUserName = $assignedBPBean->name;
                }

            // get primary contact id/localized full name
            $primaryContactID = $opptyBean->contact_id_c;
            $primaryContactName = "";
            $primaryContactDataWithheld = $opptyBean->pcontact_id_c_withheld;

            if (! $primaryContactDataWithheld) {
                $oppty_contactBean = $this->getLightBean($primaryContactID, 'Contacts');
                if (! empty($oppty_contactBean)) {
                    $primaryContactName = $oppty_contactBean->full_name;
                } else {
                    $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty oppty_contact bean");
                }
            } else {
                $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " primary contact is data withheld");
                $primaryContactID = "";
            }

            // OUTPUT
            $output['name'] = $opptyBean->name;
            $output['description'] = $opptyBean->description;
            $output['account_name'] = html_entity_decode(str_replace('&nbsp;', ' ', $opptyBean->account_name),
                ENT_QUOTES); // 3178,
                             // 35799
            $output['account_id'] = $opptyBean->account_id;
            $output['date_closed'] = $opptyBean->date_closed;
            $output['industry'] = $industry;
            $output['indus_industry'] = $indus_industry;
            $output['website'] = $website;
            $output['sales_stage'] = $opptystage;
            $output['amount'] = $opptyAmountForUser;
            $output['assigned_user_id'] = $assignedUserID;
            $output['assigned_user_name'] = $assignedUserName;
            $output['assigned_user_email'] = $assignedUserEmail;
            $output['primary_contact_id'] = $primaryContactID;
            $output['primary_contact_name'] = $primaryContactName;
            //
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty oppty bean");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getFollowedOppty($id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();

        $isFollowedResult = $this->checkIfFollowing('Opportunities', $id);
        $isFollowed = $this->getIfFollowingResult($isFollowedResult);
        if (($isFollowed !== true && $isFollowed !== false) && empty($isFollowed)) {
            $isFollowed = false;
        }

        // OUTPUT
        $output['followed'] = $isFollowed;

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getRLIsOppty($id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();
        global $current_user;

        $opptyBean = $this->getLightBean($id, 'Opportunities');
        if (! empty($opptyBean)) {
            $lineItems = $opptyBean->get_linked_beans('opportun_revenuelineitems', 'ibm_RevenueLineItems');
            if (! empty($lineItems)) {
                foreach ($lineItems as $lineItem) {
                    $lineItemid = $lineItem->id;

                    $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " processing revenuelineitem $lineItemid");

                    // get currency string
                    $lineItemCurrency = $lineItem->currency_id;
                    $currencyIDuser = $current_user->getPreference('currency');
                    $lineItemAmount = $lineItem->revenue_amount;
                    $lineItemAmountForUser = $this->getAmountStringForUser($lineItemCurrency, $lineItemAmount,
                        $currencyIDuser);

                    // get lvl15 product name
                    $lineItemLevel15ID = $lineItem->level15;
                    $lineItemLevel15Product = $lineItem->getProduct($lineItemLevel15ID);
                    $lineItemLevel15Name = $lineItemLevel15Product->name;
                    //

                    // get assigned user id/name/email
                    $lineItemOwnerID = $lineItem->assigned_user_id;
                    $lineItemOwnerName = $lineItem->assigned_user_name;
                    $lineItemOwnerEmail = '';

                    // you have to load the user's bean to get at their email address
                    if (isset($lineItemOwnerID)) {
                        $this->NOTES_LOGGER->logMessage("debug",
                            __METHOD__ . " getting assigned user email for user $lineItemOwnerID");
                        $lineItemOwnerBean = $this->getBean($lineItemOwnerID, 'Users');
                        if (! empty($lineItemOwnerBean)) {
                            $lineItemOwnerEmail = $lineItemOwnerBean->email1;
                        }
                    }

                    // OUTPUT
                    $output[$lineItemid]['amount'] = $lineItemAmountForUser;
                    $output[$lineItemid]['level15'] = $lineItemLevel15Name;
                    $output[$lineItemid]['assigned_user_id'] = $lineItemOwnerID;
                    $output[$lineItemid]['assigned_user_name'] = $lineItemOwnerName;
                    $output[$lineItemid]['assigned_user_email'] = $lineItemOwnerEmail;
                    $output[$lineItemid]['bill_date'] = $lineItem->fcast_date_tran; // 29329 previously 'bill_date"
                    $output[$lineItemid]['last_modified_date'] = $lineItem->date_modified;
                    //
                }
            } else {
                $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty rli list");
            }
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty oppty bean");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getRecentListOppty($id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();
        global $current_user;

        $opptyBean = $this->getBean($id, 'Opportunities');
        if (! empty($opptyBean)) {
            // get sales stage value
            $opptystage = $opptyBean->sales_stage;
            $opptystage = $this->getAVLValue('S_OPTY_STG', $opptystage);

            // get currency string
            $currencyID = $opptyBean->currency_id;
            $currencyIDuser = $current_user->getPreference('currency');
            $opptyAmount = $opptyBean->amount;
            $opptyAmountForUser = $this->getAmountStringForUser($currencyID, $opptyAmount, $currencyIDuser);

            // OUTPUT
            $output['name'] = $opptyBean->name;
            $output['description'] = $opptyBean->description;
            $output['account_name'] = html_entity_decode(str_replace('&nbsp;', ' ', $opptyBean->account_name),
                ENT_QUOTES); // 3178,
                             // 35799
            $output['account_id'] = $opptyBean->account_id;
            $output['date_closed'] = $opptyBean->date_closed;
            $output['sales_stage'] = $opptystage;
            $output['amount'] = $opptyAmountForUser;

        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty oppty bean");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getOpptyTabInfo($opptiesByDate, $opptyLimit)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();
        global $current_user;

        for ($i = 0; $i < $opptyLimit; $i ++) {
            $opptyid = $opptiesByDate[$i];
            $opptyBean = $this->getLightBean($opptyid, 'Opportunities'); // load oppty bean
            if (! empty($opptyBean)) {
                // get sales stage value
                $opptystage = $opptyBean->sales_stage;
                $opptystage = $this->getAVLValue('S_OPTY_STG', $opptystage);

                // get currency string
                $currencyID = $opptyBean->currency_id;
                $currencyIDuser = $current_user->getPreference('currency');
                $opptyAmount = $opptyBean->amount;
                $opptyAmountForUser = $this->getAmountStringForUser($currencyID, $opptyAmount, $currencyIDuser);

                // OUTPUT
                $output[$opptyid]['name'] = $opptyBean->name;
                $output[$opptyid]['description'] = $opptyBean->description;
                $output[$opptyid]['date_closed'] = $opptyBean->date_closed;
                $output[$opptyid]['sales_stage'] = $opptystage;
                $output[$opptyid]['amount'] = $opptyAmountForUser;
                //
            } else {
                $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " (info) empty oppty bean");
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    /**
     * Verify the input item is valid:
     * 1.
     * Is an array
     * 2. Has 4 members
     * 3. Input datatype is valid
     * 4. Result type is valid
     * 5. ALl input members are non-null
     *
     * @param String[] $item
     * @return boolean true if valid, false if not
     */
    private function verifyValidItemFormat($item)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = false;
        if (is_array($item)) {
            if (count($item) === 4) {
                if (in_array($item[0], self::$INPUT_DATA_TYPES)) {
                    if (in_array($item[2], self::$RESULT_TYPES)) {
                        if (! empty($item[0]) && ! empty($item[1]) && ! empty($item[2]) && ! empty($item[3])) {
                            $ret = true;
                        } else {
                            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . ' Empty input array members');
                        }
                    } else {
                        $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . ' Invalid result type: ' . $item[2]);
                    }
                } else {
                    $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . ' Invalid input datatype: ' . $item[0]);
                }
            } else {
                $this->NOTES_LOGGER->logMessage("warn",
                    __METHOD__ . ' Input item array does not have exactly 4 members: ' . json_encode($item));
            }
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . ' Input item is not an array: ' . json_encode($item));
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    private function logGetInfoRequest()
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        global $current_user;
        $userid = $current_user->id; // get current user's uuid
        $this->NOTES_LOGGER->logMessage("info", __METHOD__ . " getInfo request from user $userid");

        $this->NOTES_LOGGER->logExit(__METHOD__);
    }
}