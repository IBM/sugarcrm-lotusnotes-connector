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
ob_start(); // start output buffering for internal EU filter. Defect 58504
Chdir('../../..');
require_once ('SugarRestServiceImpl_v2_custom.php');
// $webservice_class = 'SugarRestService_v2_custom'; //switching from v2 to v4
$webservice_class = 'SugarRestService';
// $webservice_path = 'custom/scrmsugarws/v2_1/SugarRestService_v2_custom.php'; //switching from v2 to v4
$webservice_path = 'service/core/SugarRestService.php';
$webservice_impl_class = 'SugarRestServiceImpl_v2_custom';
$registry_class = 'registry_custom';
$registry_path = 'custom/scrmsugarws/v2_1/registry_custom.php';
$location = 'custom/scrmsugarws/v2_1/rest.php';
require_once ('service/core/webservice.php');
// invoke the internal EU filter. Defect 58504
$GLOBALS['logic_hook']->call_custom_logic('', "before_rest_output");


