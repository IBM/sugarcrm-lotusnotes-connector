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
 * typeAhead.php
 * this is an entry point provided for the SFA Notes client plugin
 * INPUT: POST variables userid/password and/or sessionid, module type, search string, result limit, myitems boolean,
 * filter string
 * OUTPUT: header array with a valid sessionid and protocolVersion, typeahead search results
 */
require_once ('./restHelper.php');
require_once ('./sfaError.php');
$restHelper = new restHelper();

$sfaNotesRestURL = $restHelper->getSFANotesRestURL();

// will need either a username/password or a session id
if (((array_key_exists('userid', $_POST) && array_key_exists('password', $_POST)) ||
     array_key_exists('sessionid', $_POST))) {
    if (array_key_exists('sessionid', $_POST)) {
        $sessionID = trim($_POST['sessionid']);
    }
    
    if (array_key_exists('userid', $_POST) && array_key_exists('password', $_POST)) {
        $user = trim($_POST['userid']);
        $password = trim($_POST['password']);
    }
    
    $moduleType = '';
    if (array_key_exists('moduleType', $_POST)) {
        $moduleType = trim($_POST['moduleType']);
    }
    
    $searchString = '';
    if (array_key_exists('searchString', $_POST)) {
        $searchString = trim($_POST['searchString']);
    }
    
    $resultLimit = '';
    if (array_key_exists('resultLimit', $_POST)) {
        $resultLimit = trim($_POST['resultLimit']);
    }
    
    $myItems = '';
    if (array_key_exists('myItems', $_POST)) {
        $myItems = trim($_POST['myItems']);
    }
    
    $filter = '';
    if (array_key_exists('filter', $_POST)) {
        $filter = trim($_POST['filter']);
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

$restMethod = 'getTypeAheadResults';
$restArguments = array(
    'moduleType' => $moduleType,
    'searchString' => $searchString,
    'resultLimit' => $resultLimit,
    'myItems' => $myItems,
    'filter' => $filter
);
$result = $restHelper->doCallWithSession($sfaNotesRestURL, $sessionID, $user, $password, $restMethod, $restArguments);
print($result);

####################################################################
