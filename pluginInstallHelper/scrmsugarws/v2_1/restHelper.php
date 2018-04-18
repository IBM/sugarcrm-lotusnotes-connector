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
class restHelper
{

    public function getSFANotesRestURL()
    {
        // return "http://localhost/sugar/custom/scrmsugarws/v2_1/rest.php";
        if (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on') {
            $prefix = 'https://';
        } else {
            $prefix = 'http://';
        }
        return $prefix . 'localhost' . dirname($_SERVER['SCRIPT_NAME']) . '/rest.php';
    }

    public function doRESTCALL($url, $method, $data)
    {
        $ch = curl_init($url);
        $headers = (function_exists('getallheaders')) ? getallheaders() : array();
        $_headers = array();
        foreach ($headers as $k => $v) {
            $_headers[strtolower($k)] = $v;
        }

        // set appropriate options
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $_headers);
        curl_setopt($ch, CURLOPT_HEADER, false);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, 0);

        // 67380 -
        // verifypeer false alone got curl exception, adding
        // verifyhost false to fix the problem.
        curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0);

        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, 0);
        curl_setopt($ch, CURLOPT_HTTP_VERSION, CURL_HTTP_VERSION_1_0);

        // $json = json_encode($data);
        $json = urlencode(json_encode($data)); // sugar expects url encoded input
        $postArgs = 'method=' . $method . '&input_type=JSON&response_type=JSON&rest_data=' . $json;
        // print "POSTARGS: $postArgs\n";

        curl_setopt($ch, CURLOPT_POSTFIELDS, $postArgs);
        $response = curl_exec($ch);
        // print "RESPONSE: $response\n";

        // $http_status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        // print $http_status;

        curl_close($ch);

        // Convert the result from JSON format to a PHP array
        $result = json_decode($response);
        return $result;
    }

    public function doRESTLOGIN($user, $password)
    {
        // make sure input is sane
        if (! isset($user) || ! isset($password) || empty($user) || empty($password)) {
            $error = new sfaError('SFA0001'); // throw invalid parameters error
            print(json_encode($error));
            exit();
        }

        $sfaNotesRestURL = $this->getSFANotesRestURL();

        // connection paramters -- if encyption parameter is removed, password must be run through md5()
        $parameters = array(
            'user_auth' => array(
                'encryption' => 'PLAIN',
                'user_name' => $user,
                'password' => $password
            ),
            'application' => "SFA_NOTES"
        );

        // login to get session id
        $result = $this->doRESTCALL($sfaNotesRestURL, 'login', $parameters);

        if (! isset($result->id)) {
            $error = new sfaError('SFA0002'); // login didn't work, probably bad username/password
            print(json_encode($error));
            exit();
        }

        $sessionID = $result->{'id'};
        return $sessionID;
    }

    /**
     * Make a call to a given REST entry point
     * This method will either use a passed in sessionid, or username/password
     *
     * FLOW:
     * if no sessionid is given, login with userid/password to get a valid sessionid, then
     * make $restMethod call with sessionid, return result
     *
     * OR
     *
     * sessionid was given, use it to make $restMethod call
     * if getSession call worked, return result
     * if getSession call did not work, login with userid/password to get a valid session id, then
     * make $restMethod call with sessionid, return result
     *
     * @param string $sfaNotesRestURL
     *            REST entry point url
     * @param string $sessionID
     *            sugar sessionid
     * @param string $user
     *            sugar username
     * @param string $password
     *            sugar password
     * @param string $restMethod
     *            REST method to call at entry point
     * @param
     *            string array(key->value) $restArguments array of parameters to pass into $restMethod
     */
    public function doCallWithSession($sfaNotesRestURL, $sessionID, $user, $password, $restMethod, $restArguments)
    {
        $result = array();

        // if a session id wasn't passed, we'll login with username/password
        if (! isset($sessionID) || $sessionID == '') {
            $sessionID = $this->doRESTLOGIN($user, $password);
            $preRestArguments = array(); // session needs to be the first argument in the array
            $preRestArguments['session'] = $sessionID;
            $restArguments = array_merge($preRestArguments, $restArguments);
            $result = $this->doRESTCALL($sfaNotesRestURL, $restMethod, $restArguments);
        } else { // if a session WAS passed, lets try the call first, an error will be returned if it's not valid
            $preRestArguments = array(); // session needs to be the first argument in the array
            $preRestArguments['session'] = $sessionID;
            $restArguments = array_merge($preRestArguments, $restArguments);
            $result = $this->doRESTCALL($sfaNotesRestURL, $restMethod, $restArguments);

            // check and see if the session was valid.
            /*
             * invalid response looks like this: {"name":"Invalid Login","number":10,"description":"Login attempt failed
             * please check the username and password"} or this: {"name":"Invalid Session
             * ID","number":11,"description":"The session ID is invalid"}
             */
            
            // adding isset check for defect 6034
            // the session id was not valid, so login, get a new session and rerun the call
            if (isset($result->number) && ($result->number == 10 || $result->number == 11)) {
            	$sessionID = $this->doRESTLOGIN($user, $password);
                unset($restArguments['session']); // remove invalid session from arguments
                $preRestArguments = array(); // and readd the new session id
                $preRestArguments['session'] = $sessionID;
                $restArguments = array_merge($preRestArguments, $restArguments);
                $result = $this->doRESTCALL($sfaNotesRestURL, $restMethod, $restArguments);
            }
        }

        // some of our older methods already build the header, so no need to do it twice
        if (! isset($result->header)) {
            $result->header = $this->buildHeader($sfaNotesRestURL,
                array(
                    'session' => $sessionID
                ));
        }
        return json_encode($result);
    }

    /**
     * Call the custom webservice to get the header array.
     * Contains:
     * authenticated session id
     * protocol version number
     * userid
     * user full name
     * user first/last name
     * user alt first/last name
     *
     * @param string $sfaNotesRestURL
     *            - webservice URL to call
     * @param string $session
     *            - authenticated session id
     * @return header array
     */
    private function buildHeader($sfaNotesRestURL, $restArguments)
    {
        $ret_array = array();

        $ret_array = $this->doRESTCALL($sfaNotesRestURL, "getSFAHeader", $restArguments);

        return $ret_array;
    }
}
