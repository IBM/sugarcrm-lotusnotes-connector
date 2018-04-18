<?php
if (! defined('sugarEntry') || ! sugarEntry)
    die('Not A Valid Entry Point');

require_once ("notesHelper_Abstract.php");

class notesHelper_GetDocumentRelationships extends notesHelper_Abstract
{

    /**
     * Given input document IDs, get their related Accounts/Opportunities.
     *
     * Currently only document ID type supported is "connectionsid"
     *
     * @param [[string,string]] $input
     *            array of arrays containing idtype,id
     * @return array mapping input id to related accounts and opportunities
     */
    public function getResult($input)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $output = array();

        foreach ($input as $item) {
            if (count($item) == 2) {
                $idtype = $item[0];
                $id = $item[1];

                if ($idtype === 'connectionsid') {
                    $sugarids = $this->getDocumentSugarIDsFromConnectionsID($id);
                    $relatedAccounts = $this->getRelatedAccounts($sugarids);
                    $relatedOppties = $this->getRelatedOppties($sugarids);

                    $output[$id]['SugarIDs'] = $sugarids;
                    $output[$id]['Accounts'] = $relatedAccounts;
                    $output[$id]['Opportunities'] = $relatedOppties;
                } else {
                    $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " invalid input type: $idtype, ignoring");
                }
            } else {
                $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " invalid input args, ignoring");
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    /**
     * Get a list of account IDs/names associated with a list of document IDs
     *
     * @param string[] $docids
     *            array of sugar document id(s)
     * @return [string]string array of accountID => accountName
     */
    private function getRelatedAccounts($docids)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $accountsinfo = array();

        if (! empty($docids)) {
            $accountids = array();

            $query = $this->buildRelatedAccountsQuery($docids);

            if (! empty($query)) {
                $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " related accounts query: $query");

                $res = $GLOBALS['db']->query($query);
                if ($res) {
                    while ($row = $GLOBALS['db']->fetchByAssoc($res)) {
                        $accountid = $row['account_id'];
                        if (! empty($accountid) && ! in_array($accountid, $accountids)) {
                            $accountids[] = $accountid;
                        }
                    }
                } else {
                    $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " error during db query: $query");
                }

                $this->NOTES_LOGGER->logMessage("debug",
                    __METHOD__ . " " . count($accountids) . " related accounts found");

                foreach ($accountids as $accoundid) {
                    $accountBean = $this->getLightBean($accoundid, 'Accounts');
                    if (! empty($accountBean->id)) {
                        $accountsinfo[$accoundid] = html_entity_decode(str_replace('&nbsp;', ' ', $accountBean->name));
                    }
                }
            } else {
                $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty query");
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $accountsinfo;
    }

    /**
     * Get a list of oppty IDs associated with a list of document IDs
     *
     * @param string[] $docids
     *            array of sugar document id(s)
     * @return [string]string array of opptyID => opptyID
     */
    private function getRelatedOppties($docids)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $opptiesinfo = array();

        if (! empty($docids)) {
            $opptyids = array();

            $query = $this->buildRelatedOpptiesQuery($docids);

            if (! empty($query)) {
                $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " related oppties query: $query");

                $res = $GLOBALS['db']->query($query);
                if ($res) {
                    while ($row = $GLOBALS['db']->fetchByAssoc($res)) {
                        $opptyid = $row['opportunity_id'];
                        if (! empty($opptyid) && ! in_array($opptyid, $opptyids)) {
                            $opptyids[] = $opptyid;
                        }
                    }
                } else {
                    $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " error during db query: $query");
                }

                $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " " . count($opptyids) . " related oppties found");

                foreach ($opptyids as $opptyid) {
                    // $opptyBean = $this->getLightBean($opptyid, 'Opportunities'); //no need for oppty bean at the
                    // moment
                    if (! empty($opptyid)) {
                        $opptiesinfo[$opptyid] = $opptyid;
                    }
                }
            } else {
                $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty query");
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $opptiesinfo;
    }

    /**
     * Build query to get account IDs related to a given set of document sugar IDs
     *
     * @param string[] $docids
     *            array of sugar document ID(s)
     * @return string DB query or empty if no input is given
     */
    private function buildRelatedAccountsQuery($docids)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $query = "";

        if (! empty($docids)) {
            $query = "SELECT DISTINCT ACCOUNT_ID FROM DOCUMENTS_ACCOUNTS WHERE DOCUMENT_ID IN (";

            $i = 0;
            $len = count($docids);
            foreach ($docids as $docid) {
                if ($i == $len - 1) {
                    $query .= "'$docid'";
                } else {
                    $query .= "'$docid',";
                }

                $i ++;
            }

            $query .= ") AND DELETED = 0 WITH UR";
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty docid list");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $query;
    }

    /**
     * Build query to get oppty IDs related to a given set of document sugar IDs
     *
     * @param string[] $docids
     *            array of sugar document ID(s)
     * @return string DB query or empty if no input is given
     */
    private function buildRelatedOpptiesQuery($docids)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $query = "";

        if (! empty($docids)) {
            $query = "SELECT DISTINCT OPPORTUNITY_ID FROM DOCUMENTS_OPPORTUNITIES WHERE DOCUMENT_ID IN (";

            $i = 0;
            $len = count($docids);
            foreach ($docids as $docid) {
                if ($i == $len - 1) {
                    $query .= "'$docid'";
                } else {
                    $query .= "'$docid',";
                }

                $i ++;
            }

            $query .= ") AND DELETED = 0 WITH UR";
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty docid list");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $query;
    }

    /**
     * Given a Document connections UUID, look up the sugar ID(s) that correspond
     *
     * @param string $connectionsID
     *            connections UUID (maps to DOCUMENTS.DOC_ID)
     * @return string[] array of sugar ID(s) that map to the given connections ID or empty if none
     */
    private function getDocumentSugarIDsFromConnectionsID($connectionsID)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        // there can be multiple rows in the DOCUMENTS table with unique IDs that map to one
        // connections UUID. we need to grab _all_ valid sugar IDs that map to the connections UUID.
        $sugarids = array();

        if (! empty($connectionsID)) {
            $query = "SELECT DISTINCT ID
				FROM DOCUMENTS
				WHERE
					DOC_ID = '$connectionsID'
					AND DELETED = 0
				WITH UR";

            $res = $GLOBALS['db']->query($query);
            if ($res) {
                while ($row = $GLOBALS['db']->fetchByAssoc($res)) {
                    $sugarid = $row['id'];
                    if (! empty($sugarid) && ! in_array($sugarid, $sugarids)) {
                        $sugarids[] = $sugarid;
                    }
                }
            } else {
                $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " error during db query: $query");
            }
        } else {
            $this->NOTES_LOGGER->logMessage("warn", __METHOD__ . " empty connections ID");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);

        return $sugarids;
    }
}