<?php
/****************************************************************
* IBM Confidential
*
* OCO Source Materials
*
* (C) Copyright IBM Corp. 2012
*
* The source code for this program is not published or otherwise
* divested of its trade secrets, irrespective of what has been
* deposited with the U.S. Copyright Office
*
***************************************************************/
post_install_notes();

function post_install_notes() {
	
	$GLOBALS['log']->info('IBMNotes->Beginning Notes module post install script');
	
	$siteXMLURL = $GLOBALS['sugar_config']['site_url'].'/custom/scrmsugarws/noteswidget/site.xml';
	$GLOBALS['log']->info('IBMNotes->URL to plugin site.xml: '.$siteXMLURL);
	
	global $current_instance_path; //this variable set by the upgrade install path
	if(!isset($current_instance_path) || empty($current_instance_path)) {
		//if it's not set, must be UI/old install path
		$GLOBALS['log']->info('IBMNotes->current_instance_path is not set, must be UI install');
		
		//should start out something like '/var/www/htdocs/sugarcrm571/index.php' so we need to remove index.php
		$current_instance_path = substr($GLOBALS['_SERVER']['SCRIPT_FILENAME'], 0, -10);
	} else {
		$GLOBALS['log']->info('IBMNotes->current_instance_path is set, must be upgrade path');
	}
	
	$pluginXMLFilename = $current_instance_path.'/custom/scrmsugarws/noteswidget/SugarCRMNotesWidget.xml';
	$GLOBALS['log']->info('IBMNotes->Path on filesystem to plugin XML file: '.$pluginXMLFilename);
	
	//insert siteXMLURL into site.xml
	$GLOBALS['log']->info('IBMNotes->Inserting siteXMLURL into plugin XML file');
    $pluginXMLFile = file_get_contents($pluginXMLFilename);
    $pluginXMLFile = str_replace('@SITEXMLURL@', $siteXMLURL, $pluginXMLFile);
    file_put_contents($pluginXMLFilename, $pluginXMLFile);
	
	$GLOBALS['log']->info('IBMNotes->Ending Notes module post install script');
}

?>