<?php
if (! defined('sugarEntry') || ! sugarEntry)
    die('Not A Valid Entry Point');

abstract class notesHelper_Abstract
{

    public static $NOTES_MAX_NUM_MYITEMS_CONTACTS = 5000;

    public static $NOTES_MAX_NUM_MYITEMS_ACCOUNTS = 5000;

    public static $NOTES_MAX_NUM_MYITEMS_OPPTYS = 5000;

    public static $NOTES_MAX_NUM_MYITEMS_TYPEAHEADS = 5000;

    public $NOTES_LOGGER;

    /* HADR pointers */
    // name of the hadr config name -- should be 'reports'
    public static $HADR_INSTANCE_NAME = 'reports';

    private $noteshadr;

    public function __construct($logger)
    {
        $this->NOTES_LOGGER = $logger;
    }

    abstract function getResult($input);

    protected function commaCaretStringToArray($string)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $ret = array();

        $splitArray = preg_split("/(\^,)+/", $string);
        $ret = str_replace("^", "", $splitArray);

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    /**
     * Check if collab-web IBMFollowingService exists, if so include the class
     * and return true.
     * Otherwise return false.
     */
    protected function checkForConnectionsFollowingService()
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $ret = false;

        if (file_exists('custom/include/IBMConnections/Services/IBMFollowingService.php')) {
            include_once 'custom/include/IBMConnections/Services/IBMFollowingService.php';
            if (class_exists('IBMFollowingService')) {
                $ret = true;
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    protected function getRelatedClientsNumber($moduleType, $id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $ret = "";

        if ($this->checkForConnectionsFollowingService() && method_exists('IBMFollowingService', 'getRelatedClientsNum')) {
            $bean = $this->getBean($id, $moduleType);
            if (! empty($bean->id)) {
                $followService = new IBMFollowingService();
                $ret = $followService->getRelatedClientsNum($bean);
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    protected function checkIfFollowing($moduleType, $id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $ret = "NA";

        if ($this->checkForConnectionsFollowingService() &&
             method_exists('IBMFollowingService', 'isFollowingWithHierarchy')) {
            $bean = $this->getBean($id, $moduleType);
            if (! empty($bean->id)) {
                $followService = new IBMFollowingService();
                $ret = $followService->isFollowingWithHierarchy($bean);
                 if($ret === false)
                  { $ret = "false"; }
                 if($ret === true)
                  { $ret = "true"; }
                if (is_null($ret)) {
                    $ret = "NA";
                }
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    protected function getIfFollowingResult($isFollowingResponse)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = "";

        if (! empty($isFollowingResponse)) {
            if (is_array($isFollowingResponse) && array_key_exists('isFollow', $isFollowingResponse)) {
                $isFollowing = $isFollowingResponse['isFollow'];
                if ($isFollowing === false) {
                    $ret = false;
                }
                if ($isFollowing === true) {
                    $ret = true;
                }
            } else {
                $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " isFollow not in checkIfFollowing response");
            }
        } else {
            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " Empty checkIfFollowing response");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    protected function getIfFollowingIsParent($isFollowingResponse)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = "";

        if (! empty($isFollowingResponse)) {
            if (is_array($isFollowingResponse) && array_key_exists('isParentClient', $isFollowingResponse)) {
                $isFollowing = $isFollowingResponse['isParentClient'];
                if ($isFollowing === false) {
                    $ret = false;
                }
                if ($isFollowing === true) {
                    $ret = true;
                }
            } else {
                $this->NOTES_LOGGER->logMessage("debug",
                    __METHOD__ . " isParentClient not in checkIfFollowing response");
            }
        } else {
            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " Empty checkIfFollowing response");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    protected function getIfFollowingParentFollowInfo($isFollowingResponse)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = array();

        if (! empty($isFollowingResponse)) {
            if (is_array($isFollowingResponse) && array_key_exists('parentFollowInfo', $isFollowingResponse)) {
                $parentFollowInfo = $isFollowingResponse['parentFollowInfo'];

                $parentID = "NA";
                if (is_array($parentFollowInfo) && array_key_exists('ccms_id', $parentFollowInfo)) {
                    $parentCCMSID = $parentFollowInfo['ccms_id'];
                    if (! empty($parentCCMSID)) {
                        $possibleParentID = $this->getAccountIDByClientID($parentCCMSID);
                        if (! empty($possibleParentID)) {
                            $parentID = $possibleParentID;
                        }
                    }
                }
                $parentFollowInfo['id'] = $parentID;

                $ret = $parentFollowInfo;
            } else {
                $this->NOTES_LOGGER->logMessage("debug",
                    __METHOD__ . " parentFollowInfo not in checkIfFollowing response");
            }
        } else {
            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " Empty checkIfFollowing response");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    private function checkUserForTooManyMyItems($hadr, $userid)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $ret = false;

        // we're gating on the max accounts myitem limit
        $maxMyItems = $this->getMyItemsMaxAccounts();

        $numOfItems = 0;
        if (! empty($hadr) && ! empty($userid)) {
            $query = "SELECT COUNT
			FROM ACCOUNTS_USERS
			WHERE
				user_id = '$userid'
				AND deleted = 0";

            $result = $hadr->query($query);
            if (! empty($result)) {
                while ($row = $hadr->fetchByAssoc($result, false)) {
                    if (isset($row[1])) {
                        $numOfItems = $row[1];
                    }
                }

                if ($numOfItems > $maxMyItems) {
                    $this->NOTES_LOGGER->logMessage("fatal",
                        __METHOD__ . " user $userid has $numOfItems accounts_users rows, more than allowed $maxMyItems");
                    $ret = true;
                } else {
                    $this->NOTES_LOGGER->logMessage("debug",
                        __METHOD__ . " user $userid has $numOfItems accounts_users rows");
                }
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    /**
     * Perform a Sugar database query, given a SQL statement, a sugar seed bean, and optional max_result/offset
     *
     * @param string $main_query
     *            - SQL query statement
     * @param string $seed
     *            - Sugar bean where the database conncetion comes from
     * @param string $max_result
     *            - Maximum number of results returned
     * @param string $offset
     *            - Offset number of results returned
     * @return result-set of the query
     */
    protected function doDBQuery($main_query, $seed, $max_result, $offset)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " query: $main_query");

        $result = '';
        if ($max_result <= - 1) {
            $result = $seed->db->query($main_query);
        } else {
            $result = $seed->db->limitQuery($main_query, $offset, $max_result + 1);
        }

        $output_list = array();
        while ($row = $seed->db->fetchByAssoc($result, false)) { // false turns off encoding of results from DB
            $output_list[] = $row;
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output_list;
    }

    /**
     * Use IBMQuickSearch to get results based on the given input array.
     *
     * This method is replacing our original way of getting typeahead reuslts:
     * ($this->buildTypeAheadQuery())
     *
     * @param $input array
     *            containing query parameters
     * @return string search results, or empty if no results
     */
    protected function doIBMQuickSearchQuery($input)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $queryResult = '';

        // IBMQuickSearchCustom is an IBM class, going to use QuickSearchCustom (sugar class) instead
        // if(file_exists('custom/modules/Home/IBMQuickSearchCustom.php')) {
        // require_once('custom/modules/Home/IBMQuickSearchCustom.php');
        // $quicksearchQuery = new QuickSearchQueryCustomIBMStrategy();
        if (file_exists('custom/modules/Home/QuickSearchCustom.php')) {
            require_once ('custom/modules/Home/QuickSearchCustom.php');
            $quicksearchQuery = new QuickSearchQueryCustomStrategy();

            $method = ! empty($input['method']) ? $input['method'] : 'query';
            if (method_exists($quicksearchQuery, $method)) {
                try {
                    $queryResultArray = $quicksearchQuery->$method($input);
                    $queryResult = json_decode($queryResultArray);
                } catch (Exception $e) {
                    $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " Caught exception: " . $e->getMessage());
                }
            } else {
                $this->NOTES_LOGGER->logMessage("fatal",
                    __METHOD__ . " Unable to search, IBMQuickSearch method not found");
            }
        } else {
            $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " Unable to search, IBMQuickSearch class not found");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $queryResult;
    }

    /**
     * Use our custom Notes version of QuickSearchQuery (this is used to get around a bug
     * where we couldn't search for account by ccms id -- original sugar class boxes us into
     * using the name, when 'use kana name' is checked)
     *
     * This method is replacing our original way of getting typeahead reuslts:
     * ($this->buildTypeAheadQuery())
     *
     * @param $input array
     *            containing query parameters
     * @return string search results, or empty if no results
     */
    protected function doIBMQuickSearchQueryNotes($input)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $queryResult = '';

        if (file_exists('custom/scrmsugarws/v2_1/QuickSearchCustomNotes.php')) {
            require_once ('custom/scrmsugarws/v2_1/QuickSearchCustomNotes.php');
            $quicksearchQuery = new QuickSearchQueryCustomStrategyNotes();

            $method = ! empty($input['method']) ? $input['method'] : 'query';
            if (method_exists($quicksearchQuery, $method)) {
                try {
                    $queryResultArray = $quicksearchQuery->$method($input);
                    $queryResult = json_decode($queryResultArray);
                } catch (Exception $e) {
                    $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " Caught exception: " . $e->getMessage());
                }
            } else {
                $this->NOTES_LOGGER->logMessage("fatal",
                    __METHOD__ . " Unable to search, Notes IBMQuickSearch method not found");
            }
        } else {
            $this->NOTES_LOGGER->logMessage("fatal",
                __METHOD__ . " Unable to search, Notes IBMQuickSearch class not found");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $queryResult;
    }

    protected function getAccountIDByClientID($clientid)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $accountid = "";

        if (! empty($clientid)) {
            $accountid = IBMHelper::getClass('Accounts')->getIDFromClientID($clientid);
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $accountid;
    }

    /**
     * Get an array with the given accountbean's ID + the ID of any child accounts.
     * Uses IBMHelperAccountsHierarchy to get list of child IDs.
     *
     * @param
     *            SugarBean accountBean parent Account sugar bean
     * @return array account ids as strings
     */
    protected function getAccountAndChildrenIDs($accountBean)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $collectedAccounts = array();

        if (! empty($accountBean)) {
            // need to get any child accounts the account has (if its a DC for example)
            $ccms_level = $accountBean->ccms_level;
            $accountid = $accountBean->id;
            if (! empty($ccms_level) && ! empty($accountid)) {
                require_once ("custom/include/Helpers/IBMHelperAccountsHierarchy.php");
                $collectedAccounts = IBMHelper::getClass('AccountsHierarchy')->getChildIDsOfClient($accountid,
                    $ccms_level);
            } else {
                $collectedAccounts = array();
            }
            $collectedAccounts[] = $accountid;
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $collectedAccounts;
    }

    /**
     * Get list of industry key/values associated with the given accountBean
     *
     * @param accountBean $accountBean
     *            account to look up industry data
     * @return array industry keys mapped to their string values
     */
    protected function getAccountIndustryData($accountBean)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        // 33000 - industry data is now stored with accounts, removing industry bean relationship
        $accountIndustry = array();

        // industry_class_name is a key, e.g. "KE"
        $industryClassKey = $accountBean->indus_class_name;

        if (! empty($industryClassKey)) {
            $industryClassName = $this->getAVLValue('INDUSTRY_CLASS', $industryClassKey);
            // store key/string mapping
            $accountIndustry[$industryClassKey] = $industryClassName;
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $accountIndustry;
    }

    /**
     * Generate formatted ammount string in this format:
     * [amount] [native currency symbol] ([amount in user's currency] [user's currency symbol])
     * ex: 16k Af (1190k pounds)
     *
     * @param $currencyID native
     *            currency ID
     * @param $amount amount
     *            in native currency
     * @param $currencyIDuser user's
     *            currency ID
     */
    protected function getAmountStringForUser($currencyID, $amount, $currencyIDuser)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = $amount;

        if (isset($currencyID) && isset($amount) && isset($currencyIDuser)) {
            $amountFormatted = $this->getFormattedAmount($currencyID, $amount);

            // 20794 -- shouldn't display the currency twice if the user's currency is same as input
            if ($currencyID === $currencyIDuser) {
                $ret = $amountFormatted;
            } else {
                // convert native amount to amount in user's currency
                $amountUser = IBMHelper::getClass('Currencies')->convertCurrency($amount, $currencyID, $currencyIDuser,
                    0);
                $amountFormattedUser = $this->getFormattedAmount($currencyIDuser, $amountUser);

                $ret = $amountFormatted . ' (' . $amountFormattedUser . ')';
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    /**
     * Look up the AVL value given the AVL name, key, and language.
     * If not found, the input key will be returned.
     *
     * //TODO: right now, language is hardcoded for en_us, might need to address this later
     *
     * @param string $avlName
     *            name corresponding to IBM_AVLDICTIONARYMAP.IBM_DICTIONARY_NAME
     * @param string $key
     *            key corresponding to avl value
     * @param string $language
     *            language of string to lookup
     */
    protected function getAVLValue($avlName, $key, $language = "en_us", $sugaravl = false)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $this->NOTES_LOGGER->logMessage("debug",
            __METHOD__ . " looking up avl value -- avl: $avlName ,key: $key ,language: $language ,issugaravl: $sugaravl");

        require_once ('modules/ibm_AVLMap/ibm_AVLMap.php');
        $retValue = $key;

        $avlMap = new ibm_AVLMap();

        /*
         * public function getAVLs( $dictionaryName, $key, $status = self::STATUS_ACTIVE, $isIBMDictionary = true,
         * $parent_id = null
         */
        if ($sugaravl) {
            $avl = $avlMap->getAVLs($avlName, $key, 0, false);
        } else {
            $avl = $avlMap->getAVLs($avlName, $key);
        }

        if (isset($avl[0]->ibm_avl_value)) {
            $retValue = $avl[0]->ibm_avl_value;
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $retValue;
    }

    /**
     * Get a SugarBean with the given ID and type.
     * Try to use BeanFactory first, and if that returns false, try a bean retrieve.
     *
     * @see ./sugar/data/SugarBean.php for the generic bean class
     * @see ./sugar/data/BeanFactory.php for the BeanFactory class
     *
     * @param String $beanID
     *            UUID of bean being looked up
     * @param String $beanType
     *            bean type being looked up (Accounts, Contacts, Opportunities, Users, etc)
     * @return SugarBean bean with fetched data
     */
    protected function getBean($beanID, $beanType)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $bean = BeanFactory::getBean($beanType, $beanID, false);

        if (! $bean) {
            $this->NOTES_LOGGER->logMessage('debug', __METHOD__ . ' BeanFactory returned false, trying bean retrieve');

            global $beanList, $beanFiles;
            $beanName = $beanList[$beanType];
            require_once $beanFiles[$beanName];

            $bean = new $beanName();
            $bean->retrieve($beanID, false, true);
        }

        if (is_null($bean)) {
            $this->NOTES_LOGGER->logMessage('debug', __METHOD__ . ' null bean');
        }

        if (empty($bean->id)) {
            $this->NOTES_LOGGER->logMessage('warn', __METHOD__ . " $beanType bean loaded for $beanID has empty id");
            $bean = null; // empty bean, return null
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $bean;
    }

    /**
     * Get a SugarBean with the given ID and type.
     * Use Sugar's 'getLightBeanCached' method, and fall back to old bean retrieve if that doesn't work.
     *
     * @see custom/include/Helpers/IBMHelperUtilities.php for the getLightBeanCached method
     *
     * @param String $beanID
     *            UUID of bean being looked up
     * @param String $beanType
     *            bean type being looked up (Accounts, Contacts, Opportunities, Users, etc)
     * @return SugarBean bean with fetched data
     */
    protected function getLightBean($beanID, $beanType)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $bean = IBMHelper::getClass('Utilities')->getLightBeanCached($beanType,
            array(
                'id' => $beanID
            ), false);
        if (empty($bean)) {
            $this->NOTES_LOGGER->logMessage('debug',
                __METHOD__ . ' getLightBeanCached returned empty bean, trying old bean retrieve method');
            $bean = $this->getBean($beanID, $beanType);
        }

        if (is_null($bean)) {
            $this->NOTES_LOGGER->logMessage('debug', __METHOD__ . ' null bean');
        }

        if (empty($bean->id)) {
            $this->NOTES_LOGGER->logMessage('warn', __METHOD__ . " $beanType bean loaded for $beanID has empty id");
            $bean = null; // empty bean, return null
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $bean;
    }

    protected function getBeanFromDecorator($beanID, $beanType)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $bean = DecoratorFactory::getDecorator($beanType, $beanID);

        if (is_null($bean)) {
            $this->NOTES_LOGGER->logMessage('debug', __METHOD__ . ' null bean');
        }

        if (empty($bean->id)) {
            $this->NOTES_LOGGER->logMessage('warn', __METHOD__ . " $beanType bean loaded for $beanID has empty id");
            $bean = null; // empty bean, return null
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $bean;
    }

    /**
     * Given a currency ID and value, format the value accordingly.
     *
     * @see custom/include/Smarty/plugins/function.sugar_currency_format.php
     *
     * @param $currencyID sugar
     *            currency ID
     * @param $amount amount
     *            to format
     */
    protected function getFormattedAmount($currencyID, $amount)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $decimal_pos = strpos($amount, ".");
        if ($decimal_pos !== false) {
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

        // the result of format_number will have currency symbol as prefix,
        // this switches it to suffix
        $amountFormatted = preg_replace('/([^&]+)&nbsp;(.*)/i', '$2 $1', $amountFormatted);

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $amountFormatted;
    }

    /**
     * Get ID(s) of either an Account or Contact with a given email
     *
     * @param string $module
     *            'Accounts' or 'Contacts'
     * @param string $searchString
     *            email address to search on
     * @return ids found with the given email address
     */
    protected function getIDsByEmail($module, $searchString)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " Get $module ids with email address $searchString");

        $linked_ids = array();

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
    protected function getContactIDByName($searchString, $resultLimit = 10)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();

        $emptyQueryResult = array(
            'totalCount' => 0,
            'fields' => array()
        );

        $input = array();
        $input['method'] = "get_contact_array";
        $input['modules'] = array(
            "Contacts"
        );
        $input['group'] = "or";
        $input['field_list'] = array(
            'name',
            'id'
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

        // For some reason downstream, this isn't set and is messing us up.
        // This really is a terrible solution.
        // Set the fields we care about, then...
        $unsetContactFields = false;
        if (! isset($GLOBALS["dictionary"]["Contact"]['fields'])) {
            VardefManager::loadVardef('Contacts', 'Contact');
        }

        // 43952: fix how contact search results are parsed -- we only need the ID
        $quicksearchOutput = $this->doIBMQuickSearchQuery($input);
        if (! empty($quicksearchOutput) && ! empty($quicksearchOutput->fields)) {
            foreach ($quicksearchOutput->fields as $contact) {
                if (! empty($contact) && ! empty($contact->id)) {
                    $output[] = $contact->id;
                }
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    protected function getHADRDB()
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        if (empty($this->noteshadr)) {
            $instance = self::$HADR_INSTANCE_NAME;

            // check if hadr database instance is even configured
            global $sugar_config;
            if (! isset($sugar_config['db'][$instance])) {
                // if it's not, throw a warning, but the db factory should fallback to the main DB anyway
                $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " $instance database instance not configured");
            } else {
                $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " using $instance database instance");
            }

            // get database instance
            $this->noteshadr = DBManagerFactory::getInstance($instance);
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $this->noteshadr;
    }

    protected function getMyItemsMaxContacts()
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $maxMyItems = self::$NOTES_MAX_NUM_MYITEMS_CONTACTS;

        global $sugar_config;

        if (isset($sugar_config['notes_max_num_myitems_contacts'])) {
            $maxMyItemsProp = $sugar_config['notes_max_num_myitems_contacts'];
            if (is_int($maxMyItemsProp) && $maxMyItemsProp > 0) {
                $maxMyItems = $maxMyItemsProp;
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $maxMyItems;
    }

    protected function getMyItemsMaxAccounts()
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $maxMyItems = self::$NOTES_MAX_NUM_MYITEMS_ACCOUNTS;

        global $sugar_config;

        if (isset($sugar_config['notes_max_num_myitems_accounts'])) {
            $maxMyItemsProp = $sugar_config['notes_max_num_myitems_accounts'];
            if (is_int($maxMyItemsProp) && $maxMyItemsProp > 0) {
                $maxMyItems = $maxMyItemsProp;
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $maxMyItems;
    }

    protected function getMyItemsMaxOpptys()
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $maxMyItems = self::$NOTES_MAX_NUM_MYITEMS_OPPTYS;

        global $sugar_config;

        if (isset($sugar_config['notes_max_num_myitems_opptys'])) {
            $maxMyItemsProp = $sugar_config['notes_max_num_myitems_opptys'];
            if (is_int($maxMyItemsProp) && $maxMyItemsProp > 0) {
                $maxMyItems = $maxMyItemsProp;
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $maxMyItems;
    }

    protected function getMyItemsMaxTypeahead()
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $maxMyItems = self::$NOTES_MAX_NUM_MYITEMS_TYPEAHEADS;

        global $sugar_config;

        if (isset($sugar_config['notes_max_num_myitems_typeaheads'])) {
            $maxMyItemsProp = $sugar_config['notes_max_num_myitems_typeaheads'];
            if (is_int($maxMyItemsProp) && $maxMyItemsProp > 0) {
                $maxMyItems = $maxMyItemsProp;
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $maxMyItems;
    }

    protected function getMyItemsUsingHADR($moduleType, $userid, $queryLimit, $extraFields = false, $usertier2_top_node = "")
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = array();

        if (! empty($moduleType) &&
             ($moduleType === 'Accounts' || $moduleType === 'Contacts' || $moduleType === 'Opportunities') &&
             ! empty($userid)) {
            // get database instance
            $hadr = $this->getHADRDB();
            if (! empty($hadr)) {
                if (! $this->checkUserForTooManyMyItems($hadr, $userid)) {
                    $query = "";
                    if ($moduleType === 'Accounts') {
                        $query = $this->getMyItemsQueryAccount($userid, $queryLimit, $extraFields);
                    } else
                        if ($moduleType === 'Contacts') {
                            $query = $this->getMyItemsQueryContact($userid, $usertier2_top_node, $queryLimit,
                                $extraFields);
                        } else
                            if ($moduleType === 'Opportunities') {
                                $query = $this->getMyItemsQueryOppty($userid, $queryLimit, $extraFields);
                            }

                    if (! empty($query)) {
                        $result = $hadr->query($query);
                        if (! empty($result)) {
                            $output_list = array();
                            while ($row = $hadr->fetchByAssoc($result, false)) { // false turns off encoding of results
                                                                                 // from DB
                                                                                 // $output_list[] = $row['id'];
                                if (isset($row['id'])) {
                                    $rowid = $row['id'];
                                    if (! empty($rowid)) {
                                        $ret[$rowid]['id'] = $rowid;
                                        if ($extraFields && $moduleType === 'Accounts') {
                                            $ret[$rowid]['name'] = $row['name'];
                                            $ret[$rowid]['alt_language_name'] = $row['alt_language_name'];
                                        } else
                                            if ($extraFields && $moduleType === 'Contacts') {
                                                $ret[$rowid]['first_name'] = $row['first_name'];
                                                $ret[$rowid]['last_name'] = $row['last_name'];
                                                $ret[$rowid]['alt_lang_first_c'] = $row['alt_lang_first_c'];
                                                $ret[$rowid]['alt_lang_last_c'] = $row['alt_lang_last_c'];
                                            }
                                    }
                                }
                            }
                        } else {
                            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty query result");
                        }
                    } else {
                        $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " unable to get myitems query");
                    }
                } else {
                    $this->NOTES_LOGGER->logMessage("fatal",
                        __METHOD__ . " user $userid has too many myaccounts, bailing");
                }
            } else {
                $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " unable to get database instance");
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    protected function getEmailAddressesUsingHADR($moduleType, $id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = array();

        if (! empty($moduleType) && ! empty($id)) {
            // get database instance
            $hadr = $this->getHADRDB();
            if (! empty($hadr)) {
                $query = "SELECT ea.email_address
					FROM email_addr_bean_rel eabr
					INNER JOIN email_addresses ea
						ON ea.id = eabr.email_address_id
						AND ea.deleted = 0
					WHERE eabr.bean_id = '$id'
						AND eabr.bean_module = '$moduleType'
						AND eabr.deleted = 0";

                $result = $hadr->query($query);
                if (! empty($result)) {
                    while ($row = $hadr->fetchByAssoc($result, false)) {
                        $ret[$row['email_address']] = $row;
                    }
                    $ret = array_keys($ret);
                } else {
                    $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty query result");
                }
            } else {
                $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " unable to get database instance");
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    protected function getSugarTagsUsingHADR($moduleType, $id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = array();

        if (! empty($moduleType) && ! empty($id)) {
            // get database instance
            $hadr = $this->getHADRDB();
            if (! empty($hadr)) {
                $query = "SELECT DISTINCT tag FROM tags WHERE module_name = '$moduleType' AND record_id = '$id' ORDER BY tag WITH UR";
                $result = $hadr->query($query);
                if (! empty($result)) {
                    while ($row = $hadr->fetchByAssoc($result, false)) { // 27386: don't encode results
                        $ret[$row['tag']] = $row;
                    }
                    $ret = array_keys($ret);
                } else {
                    $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty query result");
                }
            } else {
                $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " unable to get database instance");
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    private function getMyItemsQueryAccount($userid, $queryLimit, $extraFields = false)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        if ($extraFields) {
            $selectFields = "accounts.id, accounts.name, accounts.alt_language_name";
        } else
            $selectFields = "accounts.id";

        $query = "SELECT * FROM (
        	SELECT $selectFields
        	FROM accounts LEFT JOIN accounts_cstm
                ON accounts.id = accounts_cstm.id_c
                LEFT JOIN accounts jt0
                ON accounts.parent_id = jt0.id
            	AND jt0.deleted = 0
            	AND jt0.deleted = 0
        	WHERE (
        		( accounts.id IN (
                    SELECT ah2.ACCOUNT_ID
          			FROM ACCOUNTS_USERS au
          			JOIN ACCOUNTS_HIERARCHY ah1
                                ON ah1.ACCOUNT_ID = au.ACCOUNT_ID
                                JOIN ACCOUNTS_HIERARCHY ah2
                                ON ah2.CLIENT_ID = ah1.CLIENT_ID
                        WHERE
                            au.USER_ID = '$userid'
                            AND au.DELETED <> 1
                        WITH UR
                    )
                )
                AND (
                    accounts.ccms_level IN (
                        'SC' ,
                        'DC' ,
                        'GC' ,
                        'GU' ,
                        'DBG' ,
                        'S'
                    )
                )
                AND (
                            accounts.RESTRICTED = 0 OR accounts.RESTRICTED IS NULL
                    )
                )
            AND accounts.deleted = 0
        	WITH UR
    	) LIMIT $queryLimit OPTIMIZE FOR $queryLimit ROWS
    	WITH UR";

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $query;
    }

    private function getMyItemsQueryContact($userid, $usertier2_top_node, $queryLimit, $extraFields = false)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        if ($extraFields) {
            $selectFields = "contacts.id, contacts.first_name, contacts.last_name, contacts_cstm.alt_lang_first_c, contacts_cstm.alt_lang_last_c";
        } else
            $selectFields = "contacts.id";

        $query = "SELECT * FROM
		(
			SELECT $selectFields
   			FROM contacts LEFT JOIN contacts_cstm
	   			ON contacts.id = contacts_cstm.id_c
	     	LEFT JOIN accounts_contacts jtl0
	      		ON contacts.id = jtl0.contact_id
	      		AND jtl0.deleted = 0
	     	LEFT JOIN accounts accounts
	      	 	ON accounts.id = jtl0.account_id
	      	 	AND accounts.deleted = 0
	      	 	AND accounts.deleted = 0
	     	LEFT JOIN sugarfavorites sfav
	      		ON sfav.module = 'Contacts'
	      		AND sfav.record_id = contacts.id
	      		AND sfav.created_by = '$userid'
	      		AND sfav.deleted = 0
  			WHERE
  			(
  			 	(
  			 		contacts.id IN
  			 		(
	  			 		SELECT contacts.id
	          			FROM accounts_users
	           				INNER JOIN accounts
	   						ON accounts.id = accounts_users.account_id
	   						AND accounts.deleted = 0
	            			INNER JOIN accounts_contacts
	              			ON accounts_contacts.account_id = accounts.id
	              			AND accounts_contacts.deleted = 0
	            			INNER JOIN contacts
	              			ON contacts.id = accounts_contacts.contact_id AND contacts.deleted = 0
	          			WHERE
	          				accounts_users.user_id = '$userid'
	          				AND accounts_users.deleted = 0
          				UNION
			          	SELECT contacts.id
			          	FROM opportunities_users INNER JOIN opportunities
			            	ON opportunities.id = opportunities_users.opportunity_id
			            	AND opportunities.deleted = 0
			            	AND opportunities.sales_stage IN ('01', '02', '03', '04', '05', '06')
			            INNER JOIN opportunities_contacts
			            	ON opportunities_contacts.opportunity_id = opportunities.id
			            	AND opportunities_contacts.deleted = 0
			            INNER JOIN contacts
			          		ON contacts.id = opportunities_contacts.contact_id
			          		AND contacts.deleted = 0
			          	WHERE
			            	opportunities_users.user_id = '$userid'
			            	AND opportunities.deleted = 0
			          	UNION
			          	SELECT contacts.id
			          	FROM contacts
			          	WHERE
							contacts.created_by = '$userid'
						WITH UR
					)
				)
				AND
				(
					EXISTS
					(
						(
							(
								(
									SELECT contacts.id
				             		FROM accounts_contacts ac JOIN accounts inneracc
				                 		ON inneracc.id = ac.account_id
				                 		AND inneracc.deleted = 0
				             			WHERE
				               				inneracc.restricted = 0
				               				AND ac.contact_id = contacts.id
				               				AND ac.deleted = 0
               					)
               					INTERSECT
               					(
				            		SELECT contacts.id
				             		FROM accounts_contacts ac JOIN accounts_hierarchy ah
				             			ON ah.account_id = ac.account_id
				               		JOIN clients_fch cfh
				               			ON ah.client_id = cfh.client_id
				               		JOIN users
				               			ON tier2_top_node = cfh.forecast_hierarchy_id
				             		WHERE
				             			users.id = '$userid'
				             			AND ac.contact_id = contacts.id
				             			AND ac.deleted = 0
             					)
             				)
             			)
          				UNION
          				(
          					(
				         		SELECT contacts.id
				          		FROM accounts_contacts ac JOIN
				          		(
				          			SELECT ah1.account_id id
				                	FROM accounts_hierarchy ah1, accounts_hierarchy ah2, accounts_users au
				                 	WHERE
			                    		ah2.client_id = ah1.client_id
			                    		AND ah2.deleted = 0
			                    		AND ah2.account_id = au.account_id
			                    		AND au.deleted = 0
			                    		AND au.user_id = '$userid'
		                      	) AS direct
				              	ON direct.id = ac.account_id
				            	WHERE
				            		ac.contact_id = contacts.id
				            		AND ac.deleted = 0
            				)
           					UNION
			           		(
			           			SELECT contacts1.id
			            		FROM contacts contacts1 LEFT JOIN accounts_contacts ac
			                		ON ac.contact_id = contacts1.id
			                		AND ac.deleted = 0
			              		LEFT JOIN users u_created_by
			                		ON contacts1.created_by = u_created_by.id
			            		WHERE
			            			contacts.id = contacts1.id
			            			AND
			            			(
			            				(
			            					ac.contact_id IS NULL
			            					AND u_created_by.tier2_top_node = '$usertier2_top_node'
			            				)
			            				OR contacts1.created_by = '$userid'
			            			)
			            	)
            			)
            		)
            	)
            )
            AND contacts.deleted = 0
   			WITH UR
   		)
   		LIMIT $queryLimit OPTIMIZE FOR $queryLimit ROWS
   		WITH UR";

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $query;
    }

    private function getMyItemsQueryOppty($userid, $queryLimit, $extraFields = false)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        if ($extraFields) {
            $selectFields = "opportunities.id";
        } else
            $selectFields = "opportunities.id";

        $query = "SELECT * FROM
		(
			SELECT $selectFields
   			FROM opportunities JOIN accounts_opportunities ao
       			ON ao.opportunity_id = opportunities.id
       			AND ao.deleted = 0
     		JOIN accounts a
       			ON a.id = ao.account_id
       			AND a.deleted = 0
       			AND
       			(
       				(
       					(
       						a.RESTRICTED = 0 OR
             				a.RESTRICTED IS NULL
             			)
             			OR
            			EXISTS
              			(
              				SELECT ah2.ACCOUNT_ID
               				FROM ACCOUNTS_HIERARCHY ah1, ACCOUNTS_HIERARCHY ah2, ACCOUNTS_USERS au
               				WHERE
                 				ah1.ACCOUNT_ID = a.ID
                 				AND ah2.CLIENT_ID = ah1.CLIENT_ID
                 				AND ah2.DELETED = 0
                 				AND ah2.ACCOUNT_ID = au.ACCOUNT_ID
                 				AND au.DELETED = 0
                 				AND au.USER_ID = '$userid'
                 			WITH UR
                 		)
                 	)
                 	OR OPPORTUNITIES.ID IN
             		(
             			SELECT opportunities.id
              			FROM opportunities_users INNER JOIN opportunities
                  			ON opportunities.id = opportunities_users.opportunity_id
                  			AND opportunities.deleted = 0
              			WHERE
             				opportunities_users.user_id = '$userid'
             				AND opportunities_users.deleted = 0
             			WITH UR
             		)
             	)
     		LEFT JOIN opportunities_cstm
     			ON opportunities.id = opportunities_cstm.id_c
     		LEFT JOIN accounts_opportunities jtl0
       			ON opportunities.id = jtl0.opportunity_id
       			AND jtl0.deleted = 0
     		LEFT JOIN accounts accounts
       			ON accounts.id = jtl0.account_id
       			AND accounts.deleted = 0
       			AND accounts.deleted = 0
     		LEFT JOIN users jt1
       			ON opportunities.assigned_user_id = jt1.id
       			AND jt1.deleted = 0
          		AND jt1.deleted = 0
     		LEFT JOIN sugarfavorites sfav
       			ON sfav.module = 'Opportunities'
       			AND sfav.record_id = opportunities.id
       			AND sfav.created_by = '$userid'
       			AND sfav.deleted = 0
   			WHERE
     		(
     			(
     				opportunities.id IN
         			(
         				SELECT opportunities.id
          				FROM opportunities
          				WHERE
          					opportunities.assigned_user_id = '$userid'
          					AND opportunities.sales_stage <= 6
          				UNION
          				SELECT opportunities.id
          				FROM opportunities_users INNER JOIN opportunities
              				ON opportunities.id = opportunities_users.opportunity_id
              				AND opportunities.deleted = 0
                 			AND opportunities.sales_stage <= 6
          				WHERE
            				opportunities_users.user_id = '$userid'
            				AND opportunities_users.deleted = 0
            		)
            	)
            	AND
            	(
            		opportunities.restricted = 'RESTNOT'
            	)
            )
            AND opportunities.deleted = 0
            WITH UR
   		)
   		LIMIT $queryLimit OPTIMIZE FOR $queryLimit ROWS
   		WITH UR";

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $query;
    }
}
