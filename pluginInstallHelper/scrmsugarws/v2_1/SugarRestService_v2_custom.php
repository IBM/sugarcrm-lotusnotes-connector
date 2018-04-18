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


require_once('service/core/SugarRestService.php');
require_once('custom/scrmsugarws/v2_1/SugarRestServiceImpl_v2_custom.php');

class SugarRestService_v2_custom extends SugarRestService
{
	protected $implementationClass = 'SugarRestServiceImpl_v2_custom';
}

?>
