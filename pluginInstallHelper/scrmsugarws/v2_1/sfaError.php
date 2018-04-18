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
class sfaError
{

    function sfaError($number)
    {
        $errorList = array(
            'SFA0000' => array(
                'name' => 'Unknown Error',
                'number' => 'SFA0000',
                'description' => 'Unknown Error'
            ),
            'SFA0001' => array(
                'name' => 'Invalid Parameters',
                'number' => 'SFA0001',
                'description' => 'Invalid POST parameters -- (can be caused by bad sessionid)'
            ),
            'SFA0002' => array(
                'name' => 'No Session ID',
                'number' => 'SFA0002',
                'description' => 'No session ID -- probably invalid login credentials'
            ),
            'SFA0003' => array(
                'name' => 'Invalid Call Parameters',
                'number' => 'SFA0003',
                'description' => 'Unable to save call -- invalid call parameters'
            ),
            'SFA0004' => array(
                'name' => 'Invalid Module',
                'number' => 'SFA0004',
                'description' => 'This module is not supported for getRegex call'
            )
        );

        if (! isset($errorList[$number])) {
            $number = 'SFA0000'; // set to unknown error
        }

        $this->name = $errorList[$number]['name'];
        $this->number = $errorList[$number]['number'];
        $this->description = $errorList[$number]['description'];
    }
}

