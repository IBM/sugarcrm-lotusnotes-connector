<?php
if (! defined('sugarEntry') || ! sugarEntry)
    die('Not A Valid Entry Point');

require_once ("notesHelper_Abstract.php");

class notesHelper_GetTypeAheadResults extends notesHelper_Abstract
{

    // if result limit isn't specified, need to use a reasonably high one for myitems post-processing
    public static $RESULT_LIMIT = 1000;

    public function getResult($input)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " Input: " . json_encode($input));

        $moduleType = $input['moduleType'];
        $searchString = $input['searchString'];
        $resultLimit = $input['resultLimit'];
        $myItems = $input['myItems'];
        $filter = $input['filter'];

        $output = array();

        global $current_user;

        // default myitems to 'true'
        if (! isset($myItems) || $myItems != 'false')
            $myItems = 'true';

            // verify result limit input
        $inputResultLimit = - 1;
        if (! empty($resultLimit) && settype($resultLimit, 'integer')) {
            $inputResultLimit = $resultLimit;
        } else {
            $resultLimit = "";
            $inputResultLimit = - 1;
        }

        // we have to set resultlimit to a really big number for myitems queries, because we get the results back _then_
        // filter against myitems
        if (empty($resultLimit) || $myItems == 'true') {
            global $sugar_config;
            $bigResultLimit = self::$RESULT_LIMIT;
            if (isset($sugar_config['notes_typeahead_resultlimit'])) {
                $bigResultLimit = $sugar_config['notes_typeahead_resultlimit'];
            }

            $resultLimit = $bigResultLimit;
        }

        if ($moduleType === 'Accounts') {
            $output = $this->getTypeAheadAccounts11($current_user, $searchString, $resultLimit, $myItems, $filter);
        } else
            if ($moduleType === 'Opportunities') {
                $output = $this->getTypeAheadOpportunities11($current_user, $searchString, $resultLimit, $myItems);
            } else
                if ($moduleType === 'Contacts') {
                    $output = $this->getTypeAheadContacts11($current_user, $searchString, $resultLimit, $myItems);
                } else
                    if ($moduleType === 'Users') {
                        $output = $this->getTypeAheadUsers11($current_user, $searchString, $resultLimit);
                    } else {
                        // unknown module type, do nothing
                    }

        // trim results down to match result limit if needed
        if ($inputResultLimit > - 1 && isset($output['totalCount'])) {
            $totalCount = $output['totalCount'];
            if (settype($totalCount, 'integer') && $totalCount > $inputResultLimit) {
                if (isset($output['fields'])) {
                    $fields = $output['fields'];
                    $output['fields'] = array_slice($fields, 0, $inputResultLimit);
                    $output['totalCount'] = $inputResultLimit;
                }
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    public function getCollectedResult($input)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " Input: " . json_encode($input));

        $searchString = $input['searchString'];
        $resultLimit = $input['resultLimit'];
        $myItems = $input['myItems'];

        $output = array();

        global $current_user;

        // default myItems to 'true'
        if (! isset($myItems) || $myItems != 'false')
            $myItems = 'true';

            // divide resultLimit by 3, we'll request that many results per type
        if (is_numeric($resultLimit) && $resultLimit > 0) {
            $resultLimit = round($resultLimit / 3);
            $inputResultLimit = $resultLimit;
        } else {
            $resultLimit = - 1;
            $inputResultLimit = - 1;
        }

        if (empty($resultLimit) || $myItems == 'true') {
            global $sugar_config;
            $bigResultLimit = self::$RESULT_LIMIT;
            if (isset($sugar_config['notes_typeahead_resultlimit'])) {
                $bigResultLimit = $sugar_config['notes_typeahead_resultlimit'];
            }

            $resultLimit = $bigResultLimit;
        }

        // do accounts query
        $output['accounts'] = $this->getTypeAheadAccounts11($current_user, $searchString, $resultLimit, $myItems, null);

        // do opportunities query
        $output['opportunities'] = $this->getTypeAheadOpportunities11($current_user, $searchString, $resultLimit,
            $myItems);

        // do contacts query
        $output['contacts'] = $this->getTypeAheadContacts11($current_user, $searchString, $resultLimit, $myItems);

        // trim results down to match result limit if needed
        $resulttypes = array(
            "accounts",
            "opportunities",
            "contacts"
        );
        foreach ($resulttypes as $resulttype) {
            if ($inputResultLimit > - 1 && isset($output[$resulttype]['totalCount'])) {
                $totalCount = $output[$resulttype]['totalCount'];
                if (settype($totalCount, 'integer') && $totalCount > $inputResultLimit) {
                    if (isset($output[$resulttype]['fields'])) {
                        $fields = $output[$resulttype]['fields'];
                        $output[$resulttype]['fields'] = array_slice($fields, 0, $inputResultLimit);
                        $output[$resulttype]['totalCount'] = $inputResultLimit;
                    }
                }
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    /**
     * Get list of accounts that match search string
     *
     * @param User $current_user
     *            current user's bean
     * @param string $searchString
     *            string to search for
     * @param integer $resultLimit
     *            number of results to return
     * @param string $myItems
     *            whether or not to use user's myitems list -- 'true' or 'false'
     * @param string $filter
     *            string to filter results against -- in this case, expecting city name
     *
     * @return array of typeahead search results
     */
    private function getTypeAheadAccounts11($current_user, $searchString, $resultLimit, $myItems, $filter)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();

        $emptyQueryResult = array(
            'totalCount' => 0,
            'fields' => array()
        );

        // first check and see if the search string matches client ID pattern, if so we just need to look up client for
        // that id
        $accountsQueryResult = null;
        if ($this->checkIfClientID($searchString)) {
            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . "we think $searchString is a client ID, doing lookup");
            $accountsQueryResult = $this->doPartialClientIDQuery($searchString, $resultLimit, $filter);
        }

        // if we don't find hit for client id, go ahead and do elastic search
        if (empty($accountsQueryResult)) {
            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " no client ID hit, doing elastic search");
            // fields: id, name, ccms_id, city, state, country, industry

            $input = array();
            $input['method'] = "get_accounts_array";
            $input['modules'] = array(
                "Accounts"
            );
            $input['group'] = "and";
            $input['field_list'] = array(
                'name',
                'id',
                'ccms_id',
                'indus_class_rollup',
                'billing_address_city',
                'billing_address_state',
                'billing_address_country'
            );
            $input['conditions'] = array(
                array(
                    'name' => 'name',
                    'op' => 'like_custom',
                    'begin' => '%',
                    'end' => '%',
                    'value' => $searchString
                )
            );
            if (isset($filter) && strlen($filter) > 0) {
                // switching this to 'contains' because if 'like_custom' is used, it causes problems downstream (42651)
                $input['conditions'][] = array(
                    'name' => 'billing_address_city',
                    'op' => 'contains',
                    'end' => '%',
                    'value' => $filter
                );
            }
            $input['order'] = "name";
            if (! empty($resultLimit)) {
                $input['limit'] = $resultLimit;
            }

            // For some reason downstream, this isn't set and is messing us up.
            // This particular case only happens when kana results is checked in user prefs
            // This really is a terrible solution.
            // Set the fields we care about, then...
            $unsetAccountFields = false;
            if (! isset($_REQUEST['query'])) {
                $_REQUEST['query'] = $searchString;
                $unsetAccountFields = true;
            }

            $accountsQueryResult = $this->doIBMQuickSearchQuery($input);

            // ...set it back to empty so we've cleaned up after ourselves
            if ($unsetAccountFields)
                unset($_REQUEST['query']);
        }

        if (empty($accountsQueryResult->totalCount) || empty($accountsQueryResult->fields)) {
            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " Empty result");
            $output = $emptyQueryResult;
        } else
            if ($myItems == 'false') {
                $output = array(
                    'totalCount' => $accountsQueryResult->totalCount,
                    'fields' => $accountsQueryResult->fields
                );
            } else {
                // get myitems only if there are results
                $userid = $current_user->id; // get current user's uuid
                $maxMyItems = $this->getMyItemsMaxAccounts();
                $myAccounts = $this->getMyItemsUsingHADR('Accounts', $userid, $maxMyItems, false);
                if (empty($myAccounts)) {
                    $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " User has no myAccounts");
                    $output = $emptyQueryResult;
                }

                $myAccountsResult = array();
                foreach ($accountsQueryResult->fields as $anAccount) {
                    // we want to filter out non-myitem results
                    if (! empty($anAccount->id) && array_key_exists($anAccount->id, $myAccounts)) {
                        $myAccountsResult[] = $anAccount;
                    }
                }

                $output = array(
                    'totalCount' => count($myAccountsResult),
                    'fields' => $myAccountsResult
                );
            }

        // fix up the industry names
        foreach ($output['fields'] as &$anAccount) {
            $industryClassRollup = $anAccount->indus_class_rollup;
            $anAccount->industry = array();
            if (! empty($industryClassRollup)) {
                if (is_string($industryClassRollup)) { // leaving this in for legacy, but looks like they covert to
                                                       // array for us now
                    $industryClassRollup = $this->commaCaretStringToArray($industryClassRollup);
                }
                foreach ($industryClassRollup as $industryClassKey) {
                    if (! empty($industryClassKey)) {
                        $industryClassName = $this->getAVLValue('INDUSTRY_CLASS', $industryClassKey);
                        // store key/string mapping
                        $anAccount->industry[$industryClassKey] = $industryClassName;
                    }
                }
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    /**
     * Get list of opportunities that match search string
     *
     * Instead of using sugar's search, we need to continue to use direct queries,
     * so use the original oppty typeahead method and just fix up the format to match
     * the 1.1 typeahead form.
     *
     * @param User $current_user
     *            current user's bean
     * @param string $searchString
     *            string to search for
     * @param integer $resultLimit
     *            number of results to return
     * @param string $myItems
     *            whether or not to use user's myitems list -- 'true' or 'false'
     *
     * @return array of typeahead search results
     */
    private function getTypeAheadOpportunities11($current_user, $searchString, $resultLimit, $myItems)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $emptyQueryResult = array(
            'totalCount' => 0,
            'fields' => array()
        );

        /*
         * //this comment block uses sugars quicksaerch function, doesn't handle my items so not using currently
         * //fields: id, name, description, title, date_closed $opportunitiesQueryResult = ''; $input = array();
         * $input['method'] = "getOpptyArray"; $input['modules'] = array("Opportunities"); $input['group'] = "or";
         * $input['field_list'] = array('name','id','description','date_closed'); $input['conditions'] = array(
         * array('name' => 'name', 'op' => 'like_custom', 'end' => '%', 'value' => $searchString), array('name' =>
         * 'description', 'op' => 'like_custom', 'end' => '%', 'value' => $searchString) ); $input['order'] = "name";
         * if(!empty($resultLimit)) { $input['limit'] = $resultLimit; } $opportunitiesQueryResult =
         * $this->doIBMQuickSearchQuery($input); //$userid = $current_user->id; //get current user's uuid
         * //$myOpportunities = IBMHelper::getClass('MyItems')->getMyItems('Opportunities', $userid, array());
         */

        $oldMethodResult = $this->getTypeAheadOpportunities($current_user, $searchString, $resultLimit, $myItems);
        if (empty($oldMethodResult)) {
            $opportunitiesQueryResult = $emptyQueryResult;
        } else {
            $opportunitiesQueryResult = array(
                'totalCount' => count($oldMethodResult),
                'fields' => $oldMethodResult
            );
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $opportunitiesQueryResult;
    }

    /**
     * Get list of opportunities that match search string
     *
     * @param User $current_user
     *            current user's bean
     * @param string $searchString
     *            string to search for
     * @param integer $resultLimit
     *            number of results to return
     * @param string $myItems
     *            whether or not to use user's myitems list -- 'true' or 'false'
     *
     * @return array of typeahead search results
     */
    private function getTypeAheadOpportunities($current_user, $searchString, $resultLimit, $myItems)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " $searchString , $resultLimit , $myItems");

        $opportunitiesQueryResult = '';

        if ($myItems == 'true') {
            $userid = $current_user->id; // get current user's uuid
            $maxMyItems = $this->getMyItemsMaxOpptys();
            $myOpportunities = $this->getMyItemsUsingHADR('Opportunities', $userid, $maxMyItems, false);
        } else {
            $myOpportunities = array();
        }

        $selectFields = array(
            'opportunities.id',
            'opportunities.name',
            'opportunities.description',
            'opportunities.date_closed'
        );
        $table = 'opportunities';
        $whereFields = array(
            'opportunities.name',
            'opportunities.description'
        );
        $query = $this->buildTypeAheadQuery($selectFields, $table, $myOpportunities, $whereFields, $searchString,
            $myItems);
        if (! empty($query)) {
            $opportunitiesQueryResult = $this->doDBQuery($query, $current_user, $resultLimit, null);
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $opportunitiesQueryResult;
    }

    /**
     * Get list of contacts that match search string
     *
     * @param User $current_user
     *            current user's bean
     * @param string $searchString
     *            string to search for
     * @param integer $resultLimit
     *            number of results to return
     * @param string $myItems
     *            whether or not to use user's myitems list -- 'true' or 'false'
     *
     * @return array of typeahead search results
     */
    private function getTypeAheadContacts11($current_user, $searchString, $resultLimit, $myItems)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();

        $emptyQueryResult = array(
            'totalCount' => 0,
            'fields' => array()
        );

        // fields: id, first_name, last_name, title, primary_address_city, primary_address_state,
        // primary_address_country
        // additional fields: account name, account id

        $input = array();
        $input['method'] = "get_contact_array";
        $input['modules'] = array(
            "Contacts"
        );
        $input['group'] = "or";
        $input['field_list'] = array(
            'name',
            'id',
            'primary_address_city',
            'primary_address_state',
            'primary_address_country'
        );
        $input['conditions'] = array(
            array(
                'name' => 'name',
                'op' => 'like_custom',
                'end' => '%',
                'value' => $searchString
            )
        );
        $input['order'] = "name";
        if (! empty($resultLimit)) {
            $input['limit'] = $resultLimit;
        }

        if (! isset($GLOBALS["dictionary"]["Contact"]['fields'])) {
            VardefManager::loadVardef('Contacts', 'Contact');
        }

        $contactsQueryResult = $this->doIBMQuickSearchQuery($input);

        if (empty($contactsQueryResult->totalCount) || empty($contactsQueryResult->fields)) {
            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " Empty result");
            $output = $emptyQueryResult;
        } else
            if ($myItems == 'false') {
                $output = array(
                    'totalCount' => $contactsQueryResult->totalCount,
                    'fields' => $contactsQueryResult->fields
                );
            } else {
                // get myitems only if there are results
                $userid = $current_user->id; // get current user's uuid
                $usertier2_top_node = $current_user->tier2_top_node; // tier2_top_node -- used for myContacts
                $maxMyItems = $this->getMyItemsMaxContacts();
                $myContacts = $this->getMyItemsUsingHADR('Contacts', $userid, $maxMyItems, false, $usertier2_top_node);
                if (empty($myContacts)) {
                    $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " User has no mycontacts");
                    $output = $emptyQueryResult;
                } else {
                    $myContactsResult = array();
                    foreach ($contactsQueryResult->fields as $aContact) {
                        // we want to filter out non-myitem results
                        if (! empty($aContact->id) && array_key_exists($aContact->id, $myContacts)) {
                            $myContactsResult[] = $aContact;
                        }
                    }

                    $output = array(
                        'totalCount' => count($myContactsResult),
                        'fields' => $myContactsResult
                    );
                }
            }

        // fix up contacts state/country avls
        foreach ($output['fields'] as $aContact) {
            if (! empty($aContact->primary_address_state)) {
                $stateKey = $aContact->primary_address_state;
                $stateKey = $this->getAVLValue('state_list', $stateKey, 'en_us', true); // note have to lookup by sugar
                                                                                        // avl key, ibm dictionary is
                                                                                        // STATE_ABBREV_SFA
                if (! empty($stateKey)) {
                    $aContact->primary_address_state = $stateKey;
                }
            }

            if (! empty($aContact->primary_address_country)) {
                $countryKey = $aContact->primary_address_country;
                $countryKey = $this->getAVLValue('country_code_list', $countryKey, 'en_us', true); // note have to
                                                                                                   // lookup by sugar
                                                                                                   // avl key, ibm
                                                                                                   // dictionary is
                                                                                                   // STATE_ABBREV_SFA
                if (! empty($countryKey)) {
                    $aContact->primary_address_country = $countryKey;
                }
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getTypeAheadUsers11($current_user, $searchString, $resultLimit)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();

        $input = array();
        $input['method'] = "get_user_array";
        $input['modules'] = "Users";
        $input['group'] = "or";
        $input['field_list'] = array(
            'id',
            'user_name',
            'name'
        );
        $input['conditions'] = array(
            array(
                'name' => 'name',
                'op' => 'like_custom',
                'end' => '%',
                'value' => "$searchString"
            ),
            array(
                'name' => 'inactive_available',
                'op' => 'equals',
                'value' => '1'
            )
        );
        $input['order'] = "name";
        if (! empty($resultLimit)) {
            $input['limit'] = $resultLimit;
        }

        $usersQueryResult = $this->doIBMQuickSearchQuery($input);

        $output = array(
            'totalCount' => $usersQueryResult->totalCount,
            'fields' => $usersQueryResult->fields
        );

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    /**
     * Build SQL query used in typeahead service
     *
     * @param array $selectFields
     *            result fields to return
     * @param string $table
     *            table to search in
     * @param array $myitems
     *            list of my items
     * @param array $whereFields
     *            fields to compare search string against
     * @param
     *            string or array $searchString string to match against, or array of strings to match against
     * @param string $useMyItems
     *            'true' or 'false' to include myitems in query
     *
     * @return string SQL query
     */
    private function buildTypeAheadQuery($selectFields, $table, $myitems, $whereFields, $searchString, $useMyItems)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " searchstring: $searchString");

        $query = '';

        // TODO: beef up input sanity check
        if ((($useMyItems == 'true' && count($myitems) > 0) || ($useMyItems == 'false')) && count($selectFields) > 0) {
            /* first part of query is to match against myitems */
            $query = 'SELECT ';

            foreach ($selectFields as $selectField) {
                $query = $query . $selectField . ',';
            }

            $len = strlen($query);
            $query = substr($query, 0, ($len - 1)); // chop off last ','
            $query = $query . ' FROM ' . $table . ' WHERE (';

            if (isset($myitems) && $useMyItems != 'false' && count($myitems) > 0) {
                $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " filtering on myitems");

                // check myitems array to make sure it's not bigger than our limit,
                // if it is, slice the array to fit
                global $sugar_config;
                $maxMyItems = $this->getMyItemsMaxTypeahead();

                if (count($myitems) > $maxMyItems) {
                    $this->NOTES_LOGGER->logMessage("warn",
                        __METHOD__ . " user has more than $maxMyItems myitems, limiting query");
                    $myitems = array_slice($myitems, 0, $maxMyItems);
                }

                foreach ($myitems as $item) {
                    $query = $query . $table . '.id = \'' . $item['id'] . '\' OR ';
                }

                $len = strlen($query);
                $query = substr($query, 0, ($len - 4)); // chop off last ' OR '
                $query = $query . ')';

                /* second part of query is to match against name */
                $query = $query . ' AND (';
            }

            // defect 12843: we may be submitting multiple search strings in, to handle user/contact names
            if (is_array($searchString)) {
                // we've got multiple search strings we need to handle
                foreach ($searchString as $searchStringPiece) {
                    foreach ($whereFields as $whereField) {
                        $query = $query . $whereField . ' LIKE \'' . db2_escape_string($searchStringPiece) . '%\' OR ';
                    }
                }
            } else {
                // searchString must be a single string
                foreach ($whereFields as $whereField) {
                    $query = $query . $whereField . ' LIKE \'' . db2_escape_string($searchString) . '%\' OR ';
                }
            }

            $len = strlen($query);
            $query = substr($query, 0, ($len - 4)); // chop off last ' OR '
            $query = $query . ')';
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $query;
    }

    /**
     * Check if an input string is a partial/full client ID
     * Returns true if:
     * Begins with S/SC/DC (case-insensitive), followed by a number, followed by 0 or more alphanumerics
     *
     * @param String $clientID
     */
    private function checkIfClientID($clientID)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $ret = false;

        if (! empty($clientID)) {
            $isClientID = preg_match('/^(?:S|s|SC|sc|Sc|sC|DC|dc|Dc|dC)[0-9]{1}[A-Za-z0-9]*$/', $clientID);
            if ($isClientID === 1) {
                $ret = true;
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    /**
     * Given a partial client ID, look up a set accounts that may match the ID.
     */
    private function doPartialClientIDQuery($searchString, $resultLimit, $filter)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $accountsQueryResult = null;

        $input = array();
        $input['method'] = "get_accounts_array";
        $input['modules'] = array(
            "Accounts"
        );
        $input['group'] = "and";
        $input['field_list'] = array(
            'name',
            'id',
            'ccms_id',
            'indus_class_rollup',
            'billing_address_city',
            'billing_address_state',
            'billing_address_country'
        );
        $input['conditions'] = array(
            array(
                'name' => 'ccms_id',
                'op' => 'prefix',
                'end' => '%',
                'value' => $searchString
            )
        );
        if (isset($filter) && strlen($filter) > 0) {
            // switching this to 'contains' because if 'like_custom' is used, it causes problems downstream (42651)
            $input['conditions'][] = array(
                'name' => 'billing_address_city',
                'op' => 'contains',
                'end' => '%',
                'value' => $filter
            );
        }
        $input['order'] = "name";
        if (! empty($resultLimit)) {
            $input['limit'] = $resultLimit;
        }

        // For some reason downstream, this isn't set and is messing us up.
        // This particular case only happens when kana results is checked in user prefs
        // This really is a terrible solution.
        // Set the fields we care about, then...
        $unsetAccountFields = false;
        if (! isset($_REQUEST['query'])) {
            $_REQUEST['query'] = $searchString;
            $unsetAccountFields = true;
        }

        $accountsQueryResult = $this->doIBMQuickSearchQueryNotes($input);

        // ...set it back to empty so we've cleaned up after ourselves
        if ($unsetAccountFields)
            unset($_REQUEST['query']);

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $accountsQueryResult;
    }
}