<?php
if (! defined('sugarEntry') || ! sugarEntry)
    die('Not A Valid Entry Point');

require_once ("notesHelper_Abstract.php");

class notesHelper_GetContactsForAccountCollected extends notesHelper_Abstract
{

    public function getResult($input)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . ' input is ' . json_encode($input));

        $output = $this->getContactsForAccount($input);

        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . ' output is ' . json_encode($output));

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    private function getContactsForAccount($accountid)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret_array = array();

        global $current_user;

        // default return, if error or no results
        $ret_array['totalCount'] = 0;
        $ret_array['resultCount'] = 0;
        $ret_array['result'] = array();

        if (! empty($accountid)) {
            // get the account in question and its child IDs
            $accountBean = $this->getBean($accountid, 'Accounts');
            $collectedAccounts = $this->getAccountAndChildrenIDs($accountBean);

            if (! empty($collectedAccounts)) {
                // build the "IN" clause of query with the input + child account ids
                $inStatement = $this->getSQLInClause($collectedAccounts);

                // first run db query to get total count of contacts for accounts in DB
                $countquery = "SELECT count(DISTINCT contacts.id) " .
                     "FROM contacts LEFT JOIN contacts_cstm ON contacts.id = contacts_cstm.id_c " .
                     "INNER JOIN accounts_contacts ON contacts.id=accounts_contacts.contact_id " .
                     "AND accounts_contacts.account_id IN $inStatement " . "AND accounts_contacts.deleted=0 " .
                     "where contacts.deleted=0";

                $contactCount = 0;
                $countResult = $this->doDBQuery($countquery, $current_user, - 1, null);
                if (! empty($countResult) && isset($countResult[0][1])) {
                    $contactCount = $countResult[0][1];
                    if (! empty($contactCount)) {
                        // then run db query to get 50 of those contacts
                        $contactquery = "SELECT DISTINCT contacts.id, contacts.first_name, contacts.last_name, " .
                            // "LTRIM(RTRIM(NVL(contacts.first_name,'')||' '||NVL(contacts.last_name,''))) as name, ".
                            "contacts.phone_work, contacts.phone_mobile " .
                             "FROM contacts LEFT JOIN contacts_cstm ON contacts.id = contacts_cstm.id_c " .
                             "INNER JOIN accounts_contacts ON contacts.id=accounts_contacts.contact_id " .
                             "AND accounts_contacts.account_id IN $inStatement " . "AND accounts_contacts.deleted=0 " .
                             "where contacts.deleted=0";
                        // "ORDER BY contacts.last_name,contacts.first_name asc"; //sorting tokens doesn't work so much.

                        $contactResult = $this->doDBQuery($contactquery, $current_user, 49, null);

                        if (! empty($contactResult)) {
                            $isDataWithheld = false;

                            // using result contact ids, build "IN" clause for query to get email addresses
                            $emailInStatement = "(";
                            $total = count($contactResult);
                            $i = 0;
                            foreach ($contactResult as $aContact) {
                                if (isset($aContact['id']) && ! empty($aContact['id'])) {
                                    // check first contact returned for 'data withheld'
                                    if ($i == 0) {
                                        $isDataWithheld = $this->checkIfContactWithheld($aContact['id']);
                                        if ($isDataWithheld) {
                                            break;
                                        }
                                    }

                                    $emailInStatement .= "'" . $aContact['id'] . "'";

                                    $isLastOne = ++ $i == $total;
                                    if (! $isLastOne) {
                                        $emailInStatement .= ",";
                                    }
                                }
                            }
                            $emailInStatement .= ")";

                            if (! $isDataWithheld) {
                                // run db query to get primary email address info for contacts we are going to return
                                $emailQuery = "SELECT DISTINCT EMAIL_ADDRESS, BEAN_ID, OPT_OUT FROM EMAIL_ADDR_BEAN_REL " .
                                     "INNER JOIN EMAIL_ADDRESSES ON EMAIL_ADDR_BEAN_REL.EMAIL_ADDRESS_ID = EMAIL_ADDRESSES.ID " .
                                     "WHERE BEAN_MODULE = 'Contacts' " . "AND PRIMARY_ADDRESS = 1 " .
                                     "AND EMAIL_ADDR_BEAN_REL.DELETED = 0 " . "AND EMAIL_ADDRESSES.DELETED = 0 " .
                                     "AND BEAN_ID IN $emailInStatement";

                                $emailResult = $this->doDBQuery($emailQuery, $current_user, - 1, null);

                                // build dictionary of ID -> email info for tacking on to our contact results
                                $emailArray = array();
                                if (! empty($emailResult)) {
                                    foreach ($emailResult as $anEmail) {
                                        if (isset($anEmail['bean_id']) && ! empty($anEmail['bean_id'])) {
                                            $emailArray[$anEmail['bean_id']] = array(
                                                'email_address' => $anEmail['email_address'],
                                                'opt_out' => $anEmail['opt_out']
                                            );
                                        }
                                    }
                                } else {
                                    $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " empty email query result");
                                }

                                // loop through the results to add email information if we have it
                                foreach ($contactResult as &$aContact) {
                                    if (isset($aContact['id'])) {
                                        $aContactID = $aContact['id'];
                                        if (! empty($aContactID) && array_key_exists($aContactID, $emailArray)) {
                                            $aContact['email1'] = $emailArray[$aContactID]['email_address'];
                                            $aContact['email_opt_out'] = intval($emailArray[$aContactID]['opt_out']);
                                        } else {
                                            $aContact['email1'] = "";
                                            $aContact['email_opt_out'] = 0;
                                        }
                                    }
                                }

                                $ret_array['totalCount'] = intval($contactCount);
                                $ret_array['resultCount'] = $total;
                                $ret_array['result'] = $contactResult;
                            } else {
                                $this->NOTES_LOGGER->logMessage("debug",
                                    __METHOD__ . " first contact is data withheld, will not return results to user");
                            }
                        } else {
                            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " empty contact query result");
                        }
                    } else {
                        $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . "  no contacts found");
                    }
                } else {
                    $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " empty contact count query result");
                }
            } else {
                $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty list of accounts");
            }
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty account id");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret_array;
    }

    /**
     * Given a list of IDs, get a string in the form of:
     * "(id1,id2,id3)"
     *
     * Intended for use in SQL queries
     */
    private function getSQLInClause($ids)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        // build the "IN" clause of query with the input + child account ids
        $inStatement = "(";

        if (! empty($ids)) {
            $total = count($ids);
            $i = 0;
            foreach ($ids as $anID) {
                $inStatement .= "'" . $anID . "'";

                $isLastOne = ++ $i == $total;
                if (! $isLastOne) {
                    $inStatement .= ",";
                }
            }
        }

        $inStatement .= ")";

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $inStatement;
    }

    /**
     * Check if a contact is "data withheld' from a user by attempting to load via Decorator.
     * If the bean is null, we'll assume it is "data withheld".
     *
     * @param String $contactid
     * @return boolean true if 'data witheld', false if not
     */
    private function checkIfContactWithheld($contactid)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = false;

        if (! empty($contactid)) {
            $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " checking contact $contactid if is data witheld");
            $contactBean = $this->getBeanFromDecorator($contactid, 'Contacts');

            // when requesting a bean we're not supposed to have, it'll get returned as null
            if (empty($contactBean)) {
                $this->NOTES_LOGGER->logMessage("debug",
                    __METHOD__ . " contact $contactid return with empty bean -- is data witheld");
                $ret = true;
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }
}
