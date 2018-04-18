<?php
if (! defined('sugarEntry') || ! sugarEntry)
    die('Not A Valid Entry Point');

require_once ("notesHelper_Abstract.php");

class notesHelper_GetRegex extends notesHelper_Abstract
{

    // percentage of php max_execution_time to use for getRegexes/getRegex call -- from 0 - 1
    public static $NOTES_MAX_EXECUTION_FACTOR = 0.9;

    // default EU contact filter information
    public static $EU_FILTER_LOCATION = "/opt/filter/filter/bin/filter";

    public static $EU_FILTER_HOST = "localhost";

    public static $EU_FILTER_PORT = "50002";

    public static $EU_INPUT_DELIMITER = "^||^";

    public function getResult($input)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $output = array(
            "",
            array()
        );

        $rustart = getrusage(); // get start time

        $module = $input;

        global $current_user;
        $userid = $current_user->id; // get current user's uuid
        $this->NOTES_LOGGER->logMessage("info", __METHOD__ . " Getting $module regex for user $userid");

        if ($module === 'Contacts') {
            $output = $this->getContactRegex($rustart);
        } else
            if ($module === 'Accounts') {
                $output = $this->getAccountRegex($rustart);
            } else
                if ($module === 'Opportunities') {
                    $output = $this->getOpportunityRegex();
                } else {
                    // unknown module, no work to do
                    $GLOBALS['log']->warn('SFANotes->getRegex(): unsupported module requested, doing nothing');
                    $this->NOTES_LOGGER->logMessage("warn",
                        __METHOD__ . " unspported module requested -- no work to do.");
                }

        $ru = getrusage();
        $runtime = $this->rutimesecs($ru, $rustart, "utime");
        $this->NOTES_LOGGER->logMessage("debug",
            __METHOD__ . " Generated $module regex for user $userid in $runtime seconds");

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getContactRegex($rustart)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $output = array(
            "",
            array()
        );

        global $current_user;
        $userid = $current_user->id; // get current user's uuid
        $usertier2_top_node = $current_user->tier2_top_node; // tier2_top_node -- used for myContacts

        $maxMyItems = $this->getMyItemsMaxContacts();

        // new way of getting contact list with one trip to eu filter, no duplicate eu tokens
        // 1. get list of my contacts
        $myItemsResult = $this->getMyItemsUsingHADR('Contacts', $userid, $maxMyItems, true, $usertier2_top_node);
        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " myitems: " . json_encode($myItemsResult));
        if (! empty($myItemsResult)) {
            // 2. build file1 with list of id->firstname,lastname,altfirstname,altlastname,emailaddress1,emailaddressN
            $euFilterInput = $this->buildEUFilterInput($myItemsResult, $rustart);
            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " eufilter input: " . json_encode($euFilterInput));
            if (! empty($euFilterInput)) {
                // 3. write file1
                $euFilterInputFile = $this->writeEUFilterInput($userid, $euFilterInput);
                $this->NOTES_LOGGER->logMessage("debug",
                    __METHOD__ . " eufilter input file: " . json_encode($euFilterInputFile));
                if (! empty($euFilterInputFile)) {
                    // 4. run eu filter on file1
                    $euFilterOutputFile = $this->runEUFilter($euFilterInputFile);
                    $this->NOTES_LOGGER->logMessage("debug",
                        __METHOD__ . " eufilter output file: " . json_encode($euFilterOutputFile));
                    if (! empty($euFilterOutputFile)) {
                        // 5. read in filtered file1
                        $euFilterOutput = file_get_contents($euFilterOutputFile);
                        unlink($euFilterOutputFile);
                        $this->NOTES_LOGGER->logMessage("debug",
                            __METHOD__ . " eufilter output: " . json_encode($euFilterOutput));
                        if (! empty($euFilterOutput)) {
                            // 6. read eu filter output
                            $contactTags = $this->readEUFilterOutput($euFilterOutput);
                            $this->NOTES_LOGGER->logMessage("debug",
                                __METHOD__ . " filtered contact tags: " . json_encode($contactTags));

                            // 7. build array of contact tokens that need to be turned into regex
                            $contactNames = array(); // list of contact strings to be in regex
                                                     // $contactIDs = array();
                            foreach ($contactTags as $tagList) { // then add contact tags
                                foreach ($tagList as $tag) {
                                    $contactNames[] = $tag;
                                }
                            }

                            // 8. get optimized regex
                            $contactsRegex = $this->getOptimizedRegex($userid, $contactNames, false, false);
                            $output = array(
                                $contactsRegex,
                                $contactTags
                            );
                        } else {
                            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty eu filter output");
                        }
                    } else {
                        $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty eu filter output file");
                    }
                } else {
                    $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty eu filter input file");
                }
            } else {
                $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty eu filter input");
            }
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty myItems result");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getAccountRegex($rustart)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        global $current_user;
        $userid = $current_user->id; // get current user's uuid

        $maxMyItems = $this->getMyItemsMaxAccounts();

        $myItemsResult = $this->getMyItemsUsingHADR('Accounts', $userid, $maxMyItems, true);

        $accountTags = $this->getAccountTags($myItemsResult, $rustart);
        $accountNames = array(); // list of account strings to be in regex
                                 // $accountIDs = array();
        foreach ($accountTags as $tagList) { // then add account tags
            foreach ($tagList as $tag) {
                $accountNames[] = $tag;
            }
        }

        $accountsRegex = $this->getOptimizedRegex($userid, $accountNames, false, false);
        $output = array(
            $accountsRegex,
            $accountTags
        );

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getOpportunityRegex()
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $output = array();

        $query = $this->getPrefixAVLQuery();
        $prefixList = $this->doOpptyPrefixAVLQuery($query);
        $regexStub = ""; // create a spot in the output for a future oppty regex, if we decide to move regex building
                         // work from client to server

        if (empty($prefixList)) {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty oppty prefix list returned");
        }

        $output = array(
            $prefixList,
            $regexStub
        );

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    /**
     * Given accounts array, return an array of accounts and tags associated with an account
     *
     * @param $accountsArray -
     *            array of account records from sugar to get tags for
     * @return array(accountID1 = array(account1tags), accountID2 = array(account2tags), etc)
     */
    private function getAccountTags($accounts, $rustart)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $accountTags = array(); // return array

        if (! empty($accounts)) {
            // process each acconut returned, and add to list
            foreach ($accounts as $accountId => $account) {
                $ru = getrusage();
                $runtime = $this->rutimesecs($ru, $rustart, "utime");
                if ($this->checkIfNearMaxExecutionTime($runtime)) {
                    $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " NEAR EXECUTION LIMIT REACHED - bailing");
                    break;
                }

                $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " processing account " . $account['id']);
                $tagArray = array();
                $tempName = $account['name']; // $accountBean->name;
                $tempID = $accountId; // $accountBean->id;

                $tagArray[] = $tempName;

                // add account alt name to regex 2638 - jdjohnso 7/21/11
                $tempAltName = '';
                if ($account['alt_language_name'] != '') {
                    $tempAltName = $account['alt_language_name']; // $accountBean->alt_language_name;
                }

                if ($tempAltName != '') {
                    $tagArray[] = $tempAltName;
                }
                //

                // 14590 - factor out common prefixes/suffixes
                $tempNameFactored = $this->factorOutCommonPrefixes($tempName);
                $tempNameFactored = $this->factorOutCommonSuffixes($tempNameFactored);
                if ($tempNameFactored != '' && $tempNameFactored != $tempName) {
                    $tagArray[] = $tempNameFactored;
                }

                // get sugar tags
                $recordTags = $this->getSugarTagsUsingHADR('Accounts', $accountId);
                foreach ($recordTags as $tag) {
                    $tagArray[] = html_entity_decode($tag, ENT_QUOTES);
                }

                $accountTags[$tempID] = $tagArray;
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $accountTags;
    }

    private function rutimesecs($ru, $rus, $index)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $this->NOTES_LOGGER->logExit(__METHOD__);
        return ($ru["ru_$index.tv_sec"] + intval($ru["ru_$index.tv_usec"] / 1000000)) -
             ($rus["ru_$index.tv_sec"] + intval($rus["ru_$index.tv_usec"] / 1000000));
    }

    private function checkIfNearMaxExecutionTime($runtime)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $retvalue = false;
        $maxTime = ini_get('max_execution_time');

        global $sugar_config;
        $factor = null;
        if (isset($sugar_config['notes_max_execution_factor'])) {
            $factor = $sugar_config['notes_max_execution_factor'];
        }

        if (empty($factor) || ! ($factor > 0 && $factor <= 1))
            $factor = self::$NOTES_MAX_EXECUTION_FACTOR;
            // $GLOBALS['log']->debug('SFANotes->checkIfNearMaxExecutionTime(): max_execution_time is '.$maxTime);
            // $GLOBALS['log']->debug('SFANotes->checkIfNearMaxExecutionTime(): current run time is '.$runtime);
            // $GLOBALS['log']->debug('SFANotes->checkIfNearMaxExecutionTime(): % of max_exeuction time is '.$factor);
        if (! empty($maxTime) && $runtime >= $factor * $maxTime) {
            $retvalue = true;
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $retvalue;
    }

    /**
     * Shell out to run perl regular expression optimizer to generate a regex from input array
     *
     * @param string $user
     *            - user id of user requesting regex (used for naming temp file)
     * @param
     *            array of strings $inputArray - input converted to a regex (each element being one line of input to
     *            optimizer)
     * @param boolean $addSuffixes
     *            - true if you want common suffixes added to regexp, false if not
     * @param boolean $eufilter
     *            - true to run regex input through EU filter before optimization, false to not
     */
    private function getOptimizedRegex($user, $inputArray, $addSuffixes, $eufilter = true)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $result = "";

        // no sense in shelling out to the regex optimizer if we get an empty input
        if (! isset($inputArray) || count($inputArray) <= 0) {
            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " empty input array, returning empty string");
            return $result;
        }

        exec('pwd', $pwdOutput, $return_var); // get current working directory
        $pwd = $pwdOutput[0];

        // use userid for temporary file for input into regex optimizer
        // adding uniquid()
        $outFileName = $pwd . '/custom/scrmsugarws/v2_1/optimizer/output/' . $user . '-' . uniqid() . '.temp';

        // open temp file for writing
        $outFile = fopen($outFileName, 'w');

        // if we can't open the file for some reason, log the error and return the empty string
        if (is_null($outFile)) {
            $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " Unable to open temporary file for writing");
            return $result;
        }

        // write out each array element to a single line in the file
        foreach ($inputArray as $inputArrayElement) {
            fwrite($outFile, $inputArrayElement . "\n");
        }

        // close written temp file
        fclose($outFile);

        // run EU privacy filter, if needed
        if ($eufilter) {
            $filteredFileName = $this->runEUFilter($outFileName);
        } else {
            $filteredFileName = $outFileName;
        }

        // build perl command
        if ($addSuffixes) { // addSuffixes is currently not being used at all
            $perlCommand = 'perl ' . $pwd . '/custom/scrmsugarws/v2_1/optimizer/regexpOptimizer.pl -s ' .
                 $filteredFileName;
        } else {
            $perlCommand = 'perl ' . $pwd . '/custom/scrmsugarws/v2_1/optimizer/regexpOptimizer.pl ' . $filteredFileName;
        }

        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " Running perl optimizer: $perlCommand");

        // run regex optimizer
        exec($perlCommand, $commandOutput, $return_var);
        $result = $commandOutput[0];

        // remove temporary input files
        unlink($outFileName);
        if ($eufilter) {
            unlink($filteredFileName);
        }

        if (empty($result)) {
            $result = "";
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " Empty regex");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $result;
    }

    /**
     * Given a filename pointing to a list of strings, run that file through the EU contact filter
     * to convert any contact data tokens to actual data.
     *
     * @param string $inputFile
     *            - path/filename containing list of strings to process
     * @return string output path/filename
     */
    private function runEUFilter($inputFile)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = $inputFile; // if filtering isn't successful, we'll just give the input file back to be regex'd

        $filter = self::$EU_FILTER_LOCATION;
        $filterHost = self::$EU_FILTER_HOST;
        $filterPort = self::$EU_FILTER_PORT;
        $outputFile = "$inputFile-filtered";

        // read in and use any config if it exists
        global $sugar_config;
        if (isset($sugar_config['notes_filter_location'])) {
            $filter = $sugar_config['notes_filter_location'];
        }
        if (isset($sugar_config['notes_filter_host'])) {
            $filterHost = $sugar_config['notes_filter_host'];
        }
        if (isset($sugar_config['notes_filter_port'])) {
            $filterPort = $sugar_config['notes_filter_port'];
        }

        if (! file_exists($filter)) { // check if filter is even on filesystem
            $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " Unable to locate filter at $filter");
        } else
            if (! file_exists($inputFile)) { // check if input file is in place
                $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " Unable to locate filter input at $inputFile");
            } else { // run filter
                     // /opt/filter/filter/bin/filter --host localhost --port 50002 <
                     // 9dc9d410-a23f-29b9-55d8-51cb285c7ba7-51d1a7eee4514.temp > temp
                $filterCommand = "$filter --host $filterHost --port $filterPort < $inputFile > $outputFile";
                $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " Running filter: $filterCommand");

                // using proc_open instead of exec so we can grab the stderr stream
                // stdout is the filtered result so we redirect that to a file

                $descriptorspec = array(
                    0 => array(
                        "pipe",
                        "r"
                    ), // stdin
                    1 => array(
                        "pipe",
                        "w"
                    ), // stdout
                    2 => array(
                        "pipe",
                        "w"
                    )
                );

                $process = proc_open($filterCommand, $descriptorspec, $pipes, null, null);
                fclose($pipes[0]);
                $stdout = stream_get_contents($pipes[1]); // read stdout (not expecting anything here)

                fclose($pipes[1]);
                $stderr = stream_get_contents($pipes[2]); // read stderr

                fclose($pipes[2]);
                $return_value = proc_close($process); // close process
                $this->NOTES_LOGGER->logMessage("debug",
                    __METHOD__ . " Filter returnval: $return_value stdout: $stdout stderr: $stderr");

                if (stripos($stderr, "FATAL") !== false) { // if stderr contained "FATAL" output, log it -- probably
                                                           // connection error
                    $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " Filter error: $stderr");
                }

                if (file_exists($outputFile)) {
                    unlink($inputFile); // ok to remove input file
                    $ret = $outputFile;
                }
            }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    /**
     * Given an input string, run through the list of common prefixes and
     * factor out the first one found.
     * If none are found, the input string
     * is returned
     *
     * @param String $inputString
     *            string being checked
     */
    private function factorOutCommonPrefixes($inputString)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = $inputString;

        $prefixList = $this->getCommonPrefixList();
        foreach ($prefixList as $prefix) {
            $ret = preg_replace("/^" . preg_quote($prefix) . " /i", "", $inputString);

            // when we find a match, break
            if ($ret !== $inputString)
                break;
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    /**
     * Return array of prefixes to filter account names against
     */
    private function getCommonPrefixList()
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $this->NOTES_LOGGER->logExit(__METHOD__);
        return array(
            "Town of",
            "City of",
            "County of",
            "State of",
            "Department of",
            "Division of",
            "The"
        );
    }

    /**
     * Given an input string, run through the list of common suffixes and
     * factor out the first one found.
     * If none are found, the input string
     * is returned
     *
     * @param String $inputString
     *            string being checked
     */
    private function factorOutCommonSuffixes($inputString)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = $inputString;

        $suffixList = $this->getCommonSuffixList();
        foreach ($suffixList as $suffix) {
            $ret = preg_replace("/ " . preg_quote($suffix) . "$/i", "", $inputString);

            // when we find a match, break
            if ($ret !== $inputString)
                break;
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    /**
     * Return array of suffixes to filter account names against
     */
    private function getCommonSuffixList()
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $this->NOTES_LOGGER->logExit(__METHOD__);
        return array(
            "Company",
            "Incorporated",
            "Corporation",
            "Limited",
            "Group",
            "GmbH",
            "cyf",
            "ccc",
            "plc",
            "LLC",
            "LLP",
            "Ltd",
            "Ltd.",
            "Corp",
            "Corp.",
            "Co",
            "Co.",
            "Lic",
            "Lic.",
            "Inc",
            "Inc."
        );
    }

    private function buildEUFilterInput($myContacts, $rustart)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $ret = array();

        $delimiter = self::$EU_INPUT_DELIMITER;

        if (! empty($myContacts)) {
            foreach ($myContacts as $contactId => $contact) {
                $ru = getrusage();
                $runtime = $this->rutimesecs($ru, $rustart, "utime");
                if ($this->checkIfNearMaxExecutionTime($runtime)) {
                    $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " NEAR EXECUTION LIMIT REACHED - bailing");
                    break;
                }

                $contactString = "";

                $firstName = $contact['first_name'];
                $lastName = $contact['last_name'];
                $altFirstName = $contact['alt_lang_first_c'];
                $altLastName = $contact['alt_lang_last_c'];

                $contactString .= $contactId . $delimiter;
                $contactString .= $firstName . $delimiter;
                $contactString .= $lastName . $delimiter;
                $contactString .= $altFirstName . $delimiter;
                $contactString .= $altLastName . $delimiter;

                $emailAddresses = $this->getEmailAddressesUsingHADR('Contacts', $contactId);
                foreach ($emailAddresses as $emailAddress) {
                    $contactString .= $emailAddress . $delimiter;
                }

                $ret[] = $contactString;
            }
        } else {
            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " Empty myContacts array, nothing to do.");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    private function writeEUFilterInput($user, $inputArray)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $ret = "";

        if (! empty($user) && ! empty($inputArray)) {
            exec('pwd', $pwdOutput, $return_var); // get current working directory
            $pwd = $pwdOutput[0];

            if (! empty($pwd)) {
                // use userid for temporary file for input into regex optimizer
                // adding uniquid()
                $outFileName = $pwd . '/custom/scrmsugarws/v2_1/optimizer/output/' . $user . '-' . uniqid() . '.euinput';

                // open temp file for writing
                $outFile = fopen($outFileName, 'w');

                // if we can't open the file for some reason, log the error and return the empty string
                if (! empty($outFile)) {
                    $this->NOTES_LOGGER->logMessage("debug",
                        __METHOD__ . " Writing temp euinput file to: " . $outFileName);

                    // write out each array element to a single line in the file
                    foreach ($inputArray as $inputArrayElement) {
                        fwrite($outFile, $inputArrayElement . "\n");
                    }

                    // close written temp file
                    fclose($outFile);

                    $ret = $outFileName;
                } else {
                    $this->NOTES_LOGGER->logMessage("fatal",
                        __METHOD__ . " Unable to open temp euinput file $outFileName for writing");
                }
            } else {
                $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " Unable to get current directory.");
            }
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " Empty input, nothing to do.");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    private function readEUFilterOutput($outputFileContents)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $ret = array();

        if (! empty($outputFileContents)) {
            $outputFileContentsArray = explode("\n", $outputFileContents);

            foreach ($outputFileContentsArray as $outputLine) {
                if (! empty($outputLine)) {
                    $outputLineArray = explode(self::$EU_INPUT_DELIMITER, $outputLine);
                    if (! empty($outputLineArray)) {
                        $explodedOutputLine = $this->explodeEUFilterOutputLine($outputLineArray);
                        if (! empty($explodedOutputLine)) {
                            $ret = array_merge($ret, $explodedOutputLine);
                        }
                    } else {
                        $this->NOTES_LOGGER->logMessage("debug",
                            __METHOD__ . " Empty output file line array, ignoring.");
                    }
                } else {
                    $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " Empty output file line, ignoring.");
                }
            }
        } else {
            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " Empty output file, nothing to do.");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    private function explodeEUFilterOutputLine($outputLineArray)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = array();

        if (! empty($outputLineArray) && count($outputLineArray) >= 5) {
            $tagArray = array();

            $id = $outputLineArray[0];
            if (! empty($id)) {
                $firstName = $outputLineArray[1];
                $lastName = $outputLineArray[2];
                $altFirstName = $outputLineArray[3];
                $altLastName = $outputLineArray[4];

                $tempName = $firstName . ' ' . $lastName;
                $tempNameLF = $lastName . ' ' . $firstName; // 16896
                $tempNameDB = $firstName . "\xE3\x80\x80" . $lastName; // ideographic space: "\xE3\x80\x80"
                $tempNameLFDB = $lastName . "\xE3\x80\x80" . $firstName;

                $tagArray[] = $tempName;
                $tagArray[] = $tempNameLF; // 16896 need to add name in opposite order for localization purposes
                $tagArray[] = $tempNameDB;
                $tagArray[] = $tempNameLFDB;

                // adding alt first/last names for work item 2639 - jdjohnso 7/21/11
                $tempAltName = '';
                $tempAltNameLF = '';
                $tempAltNameDB = '';
                $tempAltNameLFDB = '';
                if ($altFirstName != '') {
                    $tempAltName = $altFirstName;
                    $tempAltNameLF = $altFirstName;
                    $tempAltNameDB = $altFirstName;
                    $tempAltNameLFDB = $altFirstName;
                }

                if ($altLastName != '') {
                    // if the first name was included, append last one with a space, otherwise just use the last name
                    $tempAltName = ($tempAltName != '') ? $tempAltName . ' ' . $altLastName : $altLastName;
                    $tempAltNameLF = ($tempAltNameLF != '') ? $altLastName . ' ' . $tempAltNameLF : $altLastName;
                    $tempAltNameDB = ($tempAltNameDB != '') ? $tempAltNameDB . "\xE3\x80\x80" . $altLastName : $altLastName;
                    $tempAltNameLFDB = ($tempAltNameLFDB != '') ? $altLastName . "\xE3\x80\x80" . $tempAltNameLFDB : $altLastName;
                }

                // don't add to tag array if the alt name is empty, or the same as the primary name
                if ($tempAltName != '' && $tempAltName != $tempName) {
                    $tagArray[] = $tempAltName;
                }
                if ($tempAltNameLF != '' && $tempAltNameLF != $tempNameLF) {
                    $tagArray[] = $tempAltNameLF;
                }
                if ($tempAltNameDB != '' && $tempAltNameDB != $tempNameDB) {
                    $tagArray[] = $tempAltNameDB;
                }
                if ($tempAltNameLFDB != '' && $tempAltNameLFDB != $tempNameLFDB) {
                    $tagArray[] = $tempAltNameLFDB;
                }
                //

                // rest of the array should be email addresses
                for ($i = 5; $i < count($outputLineArray); $i ++) {
                    if (! empty($outputLineArray[$i])) {
                        $tagArray[] = $outputLineArray[$i];
                    }
                }

                $ret[$id] = $tagArray;
            }
        } else {
            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " Invalid output line array");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    /**
     * Execute SQL to pull 'OPPTY_NUMBER_PREFIXES' AVL values out of DB
     *
     * @param String $query
     * @return array of oppty prefixes
     */
    private function doOpptyPrefixAVLQuery($query)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $output = array();

        $res = $GLOBALS['db']->query($query);
        if ($res) {
            while ($row = $GLOBALS['db']->fetchByAssoc($res)) {
                $output[] = $row['ibm_avl_key'];
            }
        } else {
            $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . "Query failed: $query");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getPrefixAVLQuery()
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $query = "SELECT
				IBM_AVL_KEY
			FROM
				IBM_AVLMAP
			WHERE
				IBM_AVLDICTIONARYMAP_ID IN (
					SELECT
						ID
					FROM
						IBM_AVLDICTIONARYMAP
					WHERE
						IBM_DICTIONARY_NAME = 'OPPTY_NUMBER_PREFIXES'
						AND DELETED = 0
					WITH UR
			)
			AND DELETED = 0
			WITH UR";

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $query;
    }
}
