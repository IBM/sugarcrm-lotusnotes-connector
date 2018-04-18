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
 * saveCall.php
 * this is an entry point provided for the SFA Notes client plugin
 * INPUT: POST variables userid/password and/or sessionid
 * OUTPUT: header array with a valid sessionid and protocolVersion
 *
 * FLOW:
 * first, check POST variables, a userid/password and/or sessionid is required
 *
 * if no sessionid is given, login with userid/password to get a valid sessionid, then
 * make getSession call with sessionid, return result
 *
 * OR
 *
 * sessionid was given, use it to make getSession call
 * if getSession call worked, return result
 * if getSession call did not work, login with userid/password to get a valid session id, then
 * make getSession call with sessionid, return result
 */
require_once ('./restHelper.php');
require_once ('./sfaError.php');
$restHelper = new restHelper();

$sfaNotesRestURL = $restHelper->getSFANotesRestURL();

// will need either a username/password or a session id
if (((array_key_exists('userid', $_POST) && array_key_exists('password', $_POST)) ||
     array_key_exists('sessionid', $_POST)) && array_key_exists('callParameters', $_POST)) {
    if (array_key_exists('sessionid', $_POST)) {
        $sessionID = trim($_POST['sessionid']);
    }

    if (array_key_exists('userid', $_POST) && array_key_exists('password', $_POST)) {
        $user = trim($_POST['userid']);
        $password = trim($_POST['password']);
    }

    $callParameters = json_decode(trim($_POST['callParameters']));
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

$restMethod = 'saveCall';
$restArguments = array(
    'callParameters' => $callParameters
);
$result = $restHelper->doCallWithSession($sfaNotesRestURL, $sessionID, $user, $password, $restMethod, $restArguments);
print($result);

####################################################################
