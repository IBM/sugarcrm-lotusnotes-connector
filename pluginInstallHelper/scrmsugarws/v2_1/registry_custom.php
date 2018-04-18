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

// require_once('service/v2/registry.php');
require_once ('service/v4_ibm/registry.php'); // switching from v2 to v4
class registry_custom extends registry_v4_ibm
{

    public function __construct($serviceClass)
    {
        parent::__construct($serviceClass);
    }

    protected function registerFunction()
    {
        parent::registerFunction();
        $this->serviceClass->registerFunction('search_contacts',
            array(
                'session' => 'xsd:string',
                'search_string' => 'xsd:string',
                'offset' => 'xsd:int',
                'max_results' => 'xsd:int'
            ), array(
                'return' => 'tns:return_search_result'
            ));
    }
}
