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

/**
 * getInfo.php
 * this is an entry point provided for the SFA Notes client plugin
 * INPUT: POST variables userid/password and/or sessionid, plus accountid, contactid, opptyid, contactemail,
 * contactname, opptyname
 * OUTPUT: header array with a valid sessionid and protocolVersion, data array with info data
 *
 * FLOW:
 * first, check POST variables, a userid/password and/or sessionid is required
 *
 * if no sessionid is given, login with userid/password to get a valid sessionid, then
 * make getInfo call with sessionid, return result
 *
 * OR
 *
 * sessionid was given, use it to make getInfo call
 * if getInfo call worked, return result
 * if getInfo call did not work, login with userid/password to get a valid session id, then
 * make getInfo call with sessionid, return result
 */
require_once ('./restHelper.php');
require_once ('./sfaError.php');
$restHelper = new restHelper();

$sfaNotesRestURL = $restHelper->getSFANotesRestURL();

if ((array_key_exists('userid', $_POST) && array_key_exists('password', $_POST)) || array_key_exists('sessionid',
    $_POST)) {
    if (array_key_exists('sessionid', $_POST)) {
        $sessionID = trim($_POST['sessionid']);
    }

    if (array_key_exists('userid', $_POST) && array_key_exists('password', $_POST)) {
        $user = trim($_POST['userid']);
        $password = trim($_POST['password']);
    }

    $accountidarray = array();
    if (array_key_exists('accountid', $_POST)) {
        $accountids = trim($_POST['accountid']);
        $accountidarray = explode(',', $accountids);
    }

    $contactidarray = array();
    if (array_key_exists('contactid', $_POST)) {
        $contactids = trim($_POST['contactid']);
        $contactidarray = explode(',', $contactids);
    }

    $opptyidarray = array();
    if (array_key_exists('opptyid', $_POST)) {
        $opptyids = trim($_POST['opptyid']);
        $opptyidarray = explode(',', $opptyids);
    }

    $contactnamearray = array();
    if (array_key_exists('contactname', $_POST)) {
        $contactnames = trim($_POST['contactname']);
        $contactnamearray = explode(',', $contactnames);
    }

    $contactemailarray = array();
    if (array_key_exists('contactemail', $_POST)) {
        $contactemails = trim($_POST['contactemail']);
        $contactemailarray = explode(',', $contactemails);
    }

    $opptynamearray = array();
    if (array_key_exists('opptyname', $_POST)) {
        $opptynames = trim($_POST['opptyname']);
        $opptynamearray = explode(',', $opptynames);
    }

    $clientidarray = array();
    if (array_key_exists('clientid', $_POST)) {
        $clientids = trim($_POST['clientid']);
        $clientidarray = explode(',', $clientids);
    }
} else {
    $error = new sfaError('SFA0001'); // throw invalid parameters error
    print(json_encode($error));
    exit();
}

// ##################################################################
if (! isset($sessionID)) {
    $sessionID = '';
}
if (! isset($user)) {
    $user = '';
}
if (! isset($password)) {
    $password = '';
}

$restMethod = 'getInfo';
$restArguments = array(
    'account_id' => $accountidarray,
    'contact_id' => $contactidarray,
    'oppty_id' => $opptyidarray,
    'contact_name' => $contactnamearray,
    'contact_email' => $contactemailarray,
    'oppty_name' => $opptynamearray,
    'client_id' => $clientidarray
);
$result = $restHelper->doCallWithSession($sfaNotesRestURL, $sessionID, $user, $password, $restMethod, $restArguments);
print($result);
####################################################################
