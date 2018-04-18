<?php
if (! defined('sugarEntry') || ! sugarEntry)
    die('Not A Valid Entry Point');

require_once ("notesHelper_Abstract.php");

class notesHelper_CollabWeb extends notesHelper_Abstract
{

    public function getResult($input)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        // nothing to do, this class is using other public methods

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $output;
    }

    public function getActivityStreamWidgetName()
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = "";

        global $sugar_config;
        if (isset($sugar_config['notes_aswidget_name'])) {
            $ret = $sugar_config['notes_aswidget_name'];
        }

        if (empty($ret)) {
            $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " 'notes_aswidget_name' not set in config");
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    /**
     * Use Collab-web Following API to determine if object is being followed.
     *
     * @return "false" if not, "true" if followed, or "NA" if problem is encountered
     */
    public function cwIsFollowing($moduleType, $id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = "NA";

        // changed to array in 2.0
        $isFollowingResponse = $this->checkIfFollowing($moduleType, $id);
        $isFollowing = $this->getIfFollowingResult($isFollowingResponse);

        if (! empty($isFollowing)) {
            $ret = $isFollowing;
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    /**
     * Use Collab-web Following API to follow a given object for a user.
     *
     * @return "followed" if operation finishes (note, does NOT guarantee success) or "NA" if problem is encountered
     */
    public function cwFollow($moduleType, $id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = "NA";

        if ($this->checkForConnectionsFollowingService() && method_exists('IBMFollowingService', 'follow')) {
            $bean = $this->getBean($id, $moduleType);
            if (! empty($bean->id)) {
                $followService = new IBMFollowingService();
                $followService->follow($bean);
                $ret = "followed";
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    /**
     * Use Collab-web Following API to unfollow a given object for a user.
     *
     * @return "unfollowed" if operation finishes (note, does NOT guarantee success) or "NA" if problem is encountered
     */
    public function cwUnfollow($moduleType, $id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = "NA";

        if ($this->checkForConnectionsFollowingService() && method_exists('IBMFollowingService', 'unFollow')) {
            $bean = $this->getBean($id, $moduleType);
            if (! empty($bean->id)) {
                $followService = new IBMFollowingService();
                $followService->unFollow($bean);
                $ret = "unfollowed";
            }
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    /**
     * Calls collab-web IBMDocumentSharingService to validate if a user can download the given file id.
     *
     * Return format:
     * array(
     * 'response' = true/false
     * 'message' = [response message]
     * 'code' = [response code]
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
     * @param string $session
     *            authenticated session
     * @param string $documentId
     *            document sugar UUID
     */
    public function cwValidateDownload($documentId)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = array();
        $ret['response'] = false;
        $ret['message'] = 'setup';
        $ret['code'] = - 100;

        if (file_exists('custom/include/IBMConnections/Services/IBMDocumentSharingService.php')) {
            include_once 'custom/include/IBMConnections/Services/IBMDocumentSharingService.php';
            if (class_exists('IBMDocumentSharingService')) {
                if (method_exists('IBMDocumentSharingService', 'validateDownload')) {
                    try {
                        // need to get document bean decorator from $documentID
                        global $beanFiles;
                        require_once $beanFiles['Document'];
                        require_once ("custom/include/Decorator/DecoratorFactory.php");

                        $documentBean = new Document();
                        $documentBean->disable_row_level_security = true;
                        $documentBean->retrieve($documentId);
                        $documentDecorator = DecoratorFactory::getDecorator($documentBean);

                        if (! empty($documentDecorator->id)) {
                            $svc = new IBMDocumentSharingService();
                            $svcresponse = $svc->validateDownload($documentDecorator);
                            if (! is_null($svcresponse)) {
                                $ret['response'] = $svc->validateDownload($documentDecorator);
                                $ret['message'] = "Successful call";
                                $ret['code'] = 0;
                            } else {
                                $ret['message'] = "null response from IBMDocumentSharingService";
                                $ret['code'] = - 105;
                            }
                        } else {
                            $ret['message'] = "Empty document bean";
                            $ret['code'] = - 104;
                        }
                    } catch (IBMWebDocumentException $ex) {
                        // Will contain the reason of the exception, the most likely reason is that the community
                        // is CCH and we cannot autoadd the member
                        $ret['message'] = $ex->getMessage();
                        $ret['code'] = $ex->getCode();
                    }
                } else {
                    $ret['message'] = 'validateDownload method not found.';
                    $ret['code'] = - 101;
                }
            } else {
                $ret['message'] = 'IBMDocumentSharingService class not found.';
                $ret['code'] = - 102;
            }
        } else {
            $ret['message'] = 'IBMDocumentSharingService file not found.';
            $ret['code'] = - 103;
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    public function cwGetEventsFilter($moduleType, $id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = "";

        if ($this->checkForConnectionsASFilterService() && method_exists('ASFilterService', 'getEventsFilter')) {
            $bean = $this->getBean($id, $moduleType);
            if (! empty($bean)) {
                $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " get events filter for $moduleType $id");
                $filterService = new ASFilterService();
                $ret = $filterService->getEventsFilter($bean);
            } else {
                $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " Empty $moduleType bean with id $id");
            }
        } else {
            $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . ' ASFilterService not installed.');
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    public function cwGetMicroblogFilter($moduleType, $id)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = "";

        if ($this->checkForConnectionsASFilterService() && method_exists('ASFilterService', 'getMicroblogFilter')) {
            $bean = $this->getBean($id, $moduleType);
            if (! empty($bean)) {
                $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " get microblog filter for $moduleType $id");
                $filterService = new ASFilterService();
                $ret = $filterService->getMicroblogFilter($bean);
            } else {
                $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " Empty $moduleType bean with id $id");
            }
        } else {
            $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . ' ASFilterService not installed.');
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    public function cwPostStatusUpdate($moduleType, $id, $statusUpdate)
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);

        $ret = "";

        if ($this->checkForConnectionsStatusUpdatesService() && method_exists('StatusUpdatesService',
            'postStatusUpdate')) {
            if (! empty($moduleType) && ! empty($id) && ! empty($statusUpdate)) {
                $this->NOTES_LOGGER->logMessage("debug", __METHOD__ . " posting status update to $moduleType $id");
                $statusUpdatesService = new StatusUpdatesService();
                try {
                    $ret = $statusUpdatesService->postStatusUpdate($moduleType, $id, $statusUpdate);
                } catch (IBMConnectionsApiException $connApiException) {
                    $ret = "error: " . $connApiException->getMessage();
                    $this->NOTES_LOGGER->logMessage("fatal",
                        __METHOD__ . ' Connections API Exception: ' . $connApiException->getMessage());
                }
            } else {
                $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . " Empty input $moduleType $id $statusUpdate");
            }
        } else {
            $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . ' StatusUpdatesService not installed.');
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    private function checkForConnectionsASFilterService()
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $ret = false;

        if (file_exists('custom/include/IBMConnections/Services/ASFilterService.php')) {
            include_once 'custom/include/IBMConnections/Services/ASFilterService.php';
            if (class_exists('ASFilterService')) {
                $ret = true;
            }
        } else {
            $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . ' ASFilterService.php not found.');
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }

    private function checkForConnectionsStatusUpdatesService()
    {
        $this->NOTES_LOGGER->logEntry(__METHOD__);
        $ret = false;

        if (file_exists('custom/include/IBMConnections/Services/StatusUpdatesService.php')) {
            include_once 'custom/include/IBMConnections/Services/StatusUpdatesService.php';
            if (class_exists('StatusUpdatesService')) {
                $ret = true;
            }
        } else {
            $this->NOTES_LOGGER->logMessage("fatal", __METHOD__ . ' StatusUpdatesService.php not found.');
        }

        $this->NOTES_LOGGER->logExit(__METHOD__);
        return $ret;
    }
}