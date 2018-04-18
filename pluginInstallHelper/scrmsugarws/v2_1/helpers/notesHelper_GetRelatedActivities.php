<?php
if (! defined('sugarEntry') || ! sugarEntry)
    die('Not A Valid Entry Point');

require_once ("notesHelper_Abstract.php");

class notesHelper_GetRelatedActivities extends notesHelper_Abstract
{

    public static $INPUT_DATA_TYPES = array(
        'account',
        'contact',
        'opportunity'
    );

    /**
     * New service needs to return related completed:
     * "notes": notes_related_to, all notes are considered completed
     * "emails", emails_related_to,
     * "meetings", meetings_related_to
     * "calls", calls_related_to
     * "tasks", tasks_related_to, status='Completed'
     * Only return 5 newest across all categories, sorted by date created.
     * Also return total number of completed activities.
     *
     * @see notesHelper_Abstract::getResult()
     */
    public function getResult($input)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . ' input is ' . json_encode($input));
        $this->NOTES_LOGGER->logMessage("debug",
            __METHOD__ . ' processing getRelatedActivities request for ' . count($input) . ' items');

        $output = array();

        foreach ($input as $item) {
            $itemActivities = $this->getActivities($item);
            if (! empty($itemActivities)) {
                $output = array_merge($output, $itemActivities);
            }
        }

        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . ' output is ' . json_encode($output));

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    /**
     * Check for valid input, and if it's valid, lookup the requested item's activities
     *
     * @param String[] $item
     *            array of requested datatype and id
     * @return array with id key to array with related activities/count
     */
    private function getActivities($item)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $output = array();

        if ($this->verifyValidItemFormat($item)) {
            $inputDataType = $item[0];
            $inputData = $item[1];

            if ($inputDataType === 'account') {
                $itemid = $inputData;
                $output[$itemid] = $this->getRelatedActivitiesAccount($itemid);
            } else
                if ($inputDataType === 'contact') {
                    $itemid = $inputData;
                    $output[$itemid] = $this->getRelatedActivitiesContact($itemid);
                } else
                    if ($inputDataType === 'opportunity') {
                        $itemid = $inputData;
                        $output[$itemid] = $this->getRelatedActivitiesOpportunity($itemid);
                    }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    /**
     * Verify the input item is valid:
     * 1.
     * Is an array
     * 2. Has 2 members
     * 3. Input datatype is valid
     * 4. ID isn't empty
     *
     * @param String[] $item
     * @return boolean true if valid, false if not
     */
    private function verifyValidItemFormat($item)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = false;
        if (is_array($item)) {
            if (count($item) === 2) {
                if (in_array($item[0], self::$INPUT_DATA_TYPES)) {
                    if (! empty($item[1])) {
                        $ret = true;
                    } else {
                        $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . ' Empty ID');
                    }
                } else {
                    $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . ' Invalid input datatype: ' . $item[0]);
                }
            } else {
                $this->NOTES_LOGGER->logMessage("warn",
                    __METHOD__ . ' Input item array does not have exactly 2 members: ' . json_encode($item));
            }
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . ' Input item is not an array: ' . json_encode($item));
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    /**
     * Given an account id, get its related activities.
     * If it's a non-site (DC/SC), we have to look up activities for children accounts.
     * Return a count for all related activities as well as info on the most recent 5.
     *
     * @param String $itemid
     */
    private function getRelatedActivitiesAccount($itemid)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $output = array();

        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . ' Getting related activities for account ' . $itemid);

        $accountBean = $this->getLightBean($itemid, 'Accounts');
        if (! empty($accountBean)) {
            // need to get account's children IDs if applicable
            $accountids = $this->getAccountAndChildrenIDs($accountBean);

            // and get CCMS levels we're looking for
            $ccmslevels = $this->getAllowedCCMSLevels($accountBean);

            if (! empty($ccmslevels)) {
                // get queries we'll use to get related activities and count
                $relatedActivitiesQuery = $this->getRelatedActivitiesQuery($accountids, $ccmslevels);
                $relatedActivitiesCountQuery = $this->getRelatedActivitiesCountQuery($accountids, $ccmslevels);

                if (! empty($relatedActivitiesQuery) && ! empty($relatedActivitiesCountQuery)) {
                    $relatedActivitiesCount = $this->doRelatedActivitiesQuery($relatedActivitiesCountQuery);
                    $relatedActivitiesCount = $relatedActivitiesCount[0][1];
                    if ($relatedActivitiesCount > 0) {
                        $relatedActivities = $this->doRelatedActivitiesQuery($relatedActivitiesQuery);
                        if (! empty($relatedActivities)) {
                            $this->addUserEmailsToActivitiesResult($relatedActivities);

                            $output['totalActivities'] = $relatedActivitiesCount;
                            $output['activities'] = $relatedActivities;
                        }
                    }
                }
            } else {
                $this->NOTES_LOGGER->logMessage("fatal",
                    __METHOD__ . ' Unable to query for related activities -- unable to get CCMS levels for account ' .
                         $itemid);
            }
        } else {
            $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " Unable to load account bean with id $itemid");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    /**
     * Given a contact id, get its related activities.
     * Return a count for all related activities as well as info on the most recent 5.
     *
     * @param String $itemid
     */
    private function getRelatedActivitiesContact($itemid)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $output = array();

        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . ' Getting related activities for contact ' . $itemid);

        $contactids = array(
            $itemid
        );

        // get queries we'll use to get related activities and count
        $relatedActivitiesQuery = $this->getRelatedActivitiesQuery($contactids);
        $relatedActivitiesCountQuery = $this->getRelatedActivitiesCountQuery($contactids);

        if (! empty($relatedActivitiesQuery) && ! empty($relatedActivitiesCountQuery)) {
            $relatedActivitiesCount = $this->doRelatedActivitiesQuery($relatedActivitiesCountQuery);
            $relatedActivitiesCount = $relatedActivitiesCount[0][1];
            if ($relatedActivitiesCount > 0) {
                $relatedActivities = $this->doRelatedActivitiesQuery($relatedActivitiesQuery);
                if (! empty($relatedActivities)) {
                    $this->addUserEmailsToActivitiesResult($relatedActivities);

                    $output['totalActivities'] = $relatedActivitiesCount;
                    $output['activities'] = $relatedActivities;
                }
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    /**
     * Given an oppty id, get its related activities.
     * Return a count for all related activities as well as info on the most recent 5.
     *
     * @param String $itemid
     */
    private function getRelatedActivitiesOpportunity($itemid)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $output = array();

        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . ' Getting related activities for oppty ' . $itemid);

        $opptyids = array(
            $itemid
        );

        // get queries we'll use to get related activities and count
        $relatedActivitiesQuery = $this->getRelatedActivitiesQuery($opptyids);
        $relatedActivitiesCountQuery = $this->getRelatedActivitiesCountQuery($opptyids);

        if (! empty($relatedActivitiesQuery) && ! empty($relatedActivitiesCountQuery)) {
            $relatedActivitiesCount = $this->doRelatedActivitiesQuery($relatedActivitiesCountQuery);
            $relatedActivitiesCount = $relatedActivitiesCount[0][1];
            if ($relatedActivitiesCount > 0) {
                $relatedActivities = $this->doRelatedActivitiesQuery($relatedActivitiesQuery);
                if (! empty($relatedActivities)) {
                    $this->addUserEmailsToActivitiesResult($relatedActivities);

                    $output['totalActivities'] = $relatedActivitiesCount;
                    $output['activities'] = $relatedActivities;
                }
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    /**
     * Chew through related activities result and look up assigned_user_id's email.
     *
     * @param Array $relatedActivities
     */
    private function addUserEmailsToActivitiesResult(&$relatedActivities)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        if (! empty($relatedActivities)) {
            foreach ($relatedActivities as &$activity) {
                if (! empty($activity['assigned_user_id'])) {
                    $userid = $activity['assigned_user_id'];
                    $userBean = $this->getLightBean($userid, 'Users');
                    if (! empty($userBean) && ! empty($userBean->user_name)) {
                        $userName = $userBean->user_name;
                        $activity['assigned_user_name'] = $userName;
                        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . "Setting $userid to $userName");
                    }
                }
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
    }

    /**
     * Given an account bean, pull the ccms level, and then determine the appropriate
     * ccms_levels to look for in the activity's related_to table
     *
     * @param SugarBean $accountBean
     *            account sugar bean
     * @return array of CCMS level strings
     */
    private function getAllowedCCMSLevels($accountBean)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $allow_ccmsLevel = array();

        if (! empty($accountBean->ccms_level)) {
            switch (strtolower($accountBean->ccms_level)) {
                case 'dc':
                    $allow_ccmsLevel = array(
                        'DC',
                        'SC',
                        'S'
                    );
                    break;
                case 'sc':
                    $allow_ccmsLevel = array(
                        'SC',
                        'S'
                    );
                    break;
                case 's':
                default:
                    $allow_ccmsLevel = array(
                        'S'
                    );
                    break;
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $allow_ccmsLevel;
    }

    /**
     * Execute related activities SQL using primary DB
     *
     * @param String $query
     *            related activities query
     * @return array of found rows
     */
    private function doRelatedActivitiesQuery($query)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $output = array();

        $res = $GLOBALS['db']->query($query);
        if ($res) {
            while ($row = $GLOBALS['db']->fetchByAssoc($res)) {
                $output[] = $row;
            }
        } else {
            $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . "Query failed: $query");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getQueryInClauseFromIDList($relatedids)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $inclause = "";

        if (! empty($relatedids) && is_array($relatedids)) {
            // build the query's inclase for related the input related item(s)
            $inclause = "(";
            $i = 0;
            $total = count($relatedids);
            foreach ($relatedids as $id) {
                $inclause .= "'$id'";
                $final = ($i + 1 == $total) ? true : false;
                if (! $final) {
                    $inclause .= ",";
                }

                $i ++;
            }
            $inclause .= ")";
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $inclause;
    }

    private function getQueryCCMSRestrict($ccmslevels, $tablename)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $ccmsrestrict = "";

        if (! empty($ccmslevels) && ! empty($tablename)) {
            // (tasks_related_to.ccms_level = 'DC' OR tasks_related_to.ccms_level = 'SC' OR tasks_related_to.ccms_level
            // = 'S')
            $ccmsrestrict = "and (";
            $i = 0;
            $total = count($ccmslevels);
            foreach ($ccmslevels as $level) {
                $ccmsrestrict .= $tablename . "_related_to.ccms_level = '$level'";
                $final = ($i + 1 == $total) ? true : false;
                if (! $final) {
                    $ccmsrestrict .= " OR ";
                }

                $i ++;
            }
            $ccmsrestrict .= ")";
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ccmsrestrict;
    }

    private function getRelatedActivitiesCountQuery($relatedids, $ccmslevels = array())
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $query = "";

        $inclause = $this->getQueryInClauseFromIDList($relatedids);

        $ccmsrestricttasks = "";
        $ccmsrestrictnotes = "";
        $ccmsrestrictemails = "";
        $ccmsrestrictmeetings = "";
        $ccmsrestrictcalls = "";
        if (! empty($ccmslevels)) {
            $this->NOTES_LOGGER->logMessage("debug",
                __METHOD__ . " Restricting query to CCMS level(s): " . json_encode($ccmslevels));
            $ccmsrestricttasks = $this->getQueryCCMSRestrict($ccmslevels, 'tasks');
            $ccmsrestrictnotes = $this->getQueryCCMSRestrict($ccmslevels, 'notes');
            $ccmsrestrictemails = $this->getQueryCCMSRestrict($ccmslevels, 'emails');
            $ccmsrestrictmeetings = $this->getQueryCCMSRestrict($ccmslevels, 'meetings');
            $ccmsrestrictcalls = $this->getQueryCCMSRestrict($ccmslevels, 'calls');
        }

        if (! empty($inclause)) {
            $query = "select count(*) from (
				--tasks
				(select
					tasks.id as id
				from
					tasks inner join tasks_related_to
						on tasks.id = task_id
				where
					tasks.deleted = 0
					and tasks_related_to.deleted = 0
					and tasks_related_to.related_id in $inclause
					and tasks.status = 'Completed'
					$ccmsrestricttasks
				with UR)

				union

				--notes
				(select
					notes.id as id
				from
					notes inner join notes_related_to
						on notes.id = note_id
				where
					notes.deleted = 0
					and notes_related_to.deleted = 0
					and notes_related_to.related_id in $inclause
					$ccmsrestrictnotes
				with UR)

				union

				--emails
				(select
					emails.id as id
				from
					emails inner join emails_related_to
						on emails.id = email_id
				where
					emails.deleted = 0
					and emails_related_to.deleted = 0
					and emails_related_to.related_id in $inclause
					and emails.status = 'archived'
					$ccmsrestrictemails
				with UR)

				union

				--meetings
				(select
					meetings.id as id
				from
					meetings inner join meetings_related_to
						on meetings.id = meeting_id
				where
					meetings.deleted = 0
					and meetings_related_to.deleted = 0
					and meetings_related_to.related_id in $inclause
					and meetings.status = 'Held'
					$ccmsrestrictmeetings
				with UR)

				union

				--calls
				(select
					calls.id as id
				from
					calls inner join calls_related_to
						on calls.id = call_id
				where
					calls.deleted = 0
					and calls_related_to.deleted = 0
					and calls_related_to.related_id in $inclause
					and calls.status = 'Held'
					$ccmsrestrictcalls
				with UR)

				)

				with UR;";
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $query;
    }

    /**
     * Returns query to get info about activities related to the given IDs, including:
     * Tasks with status 'Completed'
     * Notes
     * Emails with status 'archived'
     * Meetings with status 'Held'
     * Calls with status 'Held'
     *
     * @param String[] $relatedids
     *            IDs being looked for
     * @param String[] $ccmslevels
     *            optional list of CCMS levels to restrict against
     * @return string related activities SQL query
     */
    private function getRelatedActivitiesQuery($relatedids, $ccmslevels = array())
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $query = "";

        $inclause = $this->getQueryInClauseFromIDList($relatedids);

        $ccmsrestricttasks = "";
        $ccmsrestrictnotes = "";
        $ccmsrestrictemails = "";
        $ccmsrestrictmeetings = "";
        $ccmsrestrictcalls = "";
        if (! empty($ccmslevels)) {
            $this->NOTES_LOGGER->logMessage("debug",
                __METHOD__ . " Restricting query to CCMS level(s): " . json_encode($ccmslevels));
            $ccmsrestricttasks = $this->getQueryCCMSRestrict($ccmslevels, 'tasks');
            $ccmsrestrictnotes = $this->getQueryCCMSRestrict($ccmslevels, 'notes');
            $ccmsrestrictemails = $this->getQueryCCMSRestrict($ccmslevels, 'emails');
            $ccmsrestrictmeetings = $this->getQueryCCMSRestrict($ccmslevels, 'meetings');
            $ccmsrestrictcalls = $this->getQueryCCMSRestrict($ccmslevels, 'calls');
        }

        if (! empty($inclause)) {
            $this->NOTES_LOGGER->logMessage("debug",
                __METHOD__ . " Building related activities query for related ID(s): $inclause");

            $query = "(
				--tasks
				(select
					tasks.id as id,
					tasks.name as name,
					tasks.date_entered as date_entered,
					tasks.assigned_user_id as assigned_user_id,
					'Tasks' as module
				from
					tasks inner join tasks_related_to
						on tasks.id = task_id
				where
					tasks.deleted = 0
					and tasks_related_to.deleted = 0
					and tasks_related_to.related_id in $inclause
					and tasks.status = 'Completed'
					$ccmsrestricttasks
				with UR)

				union

				--notes
				(select
					notes.id as id,
					notes.name as name,
					notes.date_entered as date_entered,
					notes.assigned_user_id as assigned_user_id,
					'Notes' as module
				from
					notes inner join notes_related_to
						on notes.id = note_id
				where
					notes.deleted = 0
					and notes_related_to.deleted = 0
					and notes_related_to.related_id in $inclause
					$ccmsrestrictnotes
				with UR)

				union

				--emails
				(select
					emails.id as id,
					emails.name as name,
					emails.date_entered as date_entered,
					emails.assigned_user_id as assigned_user_id,
					'Emails' as module
				from
					emails inner join emails_related_to
						on emails.id = email_id
				where
					emails.deleted = 0
					and emails_related_to.deleted = 0
					and emails_related_to.related_id in $inclause
					and emails.status = 'archived'
					$ccmsrestrictemails
				with UR)

				union

				--meetings
				(select
					meetings.id as id,
					meetings.name as name,
					meetings.date_entered as date_entered,
					meetings.assigned_user_id as assigned_user_id,
					'Meetings' as module
				from
					meetings inner join meetings_related_to
						on meetings.id = meeting_id
				where
					meetings.deleted = 0
					and meetings_related_to.deleted = 0
					and meetings_related_to.related_id in $inclause
					and meetings.status = 'Held'
					$ccmsrestrictmeetings
				with UR)

				union

				--calls
				(select
					calls.id as id,
					calls.name as name,
					calls.date_entered as date_entered,
					calls.assigned_user_id as assigned_user_id,
					'Calls' as module
				from
					calls inner join calls_related_to
						on calls.id = call_id
				where
					calls.deleted = 0
					and calls_related_to.deleted = 0
					and calls_related_to.related_id in $inclause
					and calls.status = 'Held'
					$ccmsrestrictcalls
				with UR)

				)

				order by date_entered desc
				fetch first 5 rows only
				with UR;";
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . ' No IDs to do work on, returning empty query');
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $query;
    }
}