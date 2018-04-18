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
 * sfaRest.php
 * This is the webservice entry point class provided to the SFA notes client.
 * It acts as a middle man between Sugar and custom SFA webservices, and the notes client.
 *
 * INPUTS:
 * userid - sugar user id (IBM intranet ID)
 * password - sugar password
 * sessionid - authenticated session (used in place of userid/password)
 * method - webservice method to call
 * arguments - json_encoded array of arguments to pass into given webservice method
 *
 * OUTPUTS:
 * json_encoded array with header containing service information, and webservice method response
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

    if (array_key_exists('method', $_POST)) {
        $restMethod = trim($_POST['method']);
    }

    if (array_key_exists('arguments', $_POST)) {
        $restArgumentString = trim($_POST['arguments']);
        $restArguments = (array) json_decode($restArgumentString);
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
if (! isset($restMethod)) {
    $restMethod = '';
}
if (! isset($restArguments)) {
    $restArguments = array();
}

$result = $restHelper->doCallWithSession($sfaNotesRestURL, $sessionID, $user, $password, $restMethod, $restArguments);
print($result);
