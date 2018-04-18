<?php

$manifest = array (
	'acceptable_sugar_versions' => array(
		'regex_matches' => array('[6-7]\..*',),),
	'acceptable_sugar_flavors' => array('CE','PRO','ENT','ULT'),
	'readme' => '',
	'key' => 'scrmws',
	'author' => 'jdjohnso',
	'description' => 'Web service that provides information from Sugar to the SocialCRM clients.',
	'icon' => '',
	'is_uninstallable' => true,
	'name' => 'SugarCMR Lotus Notes Connector',
	'published_date' => '@BUILDLEVEL@',
	'type' => 'module',
	'version' => '@RELEASENAME@_@BUILDLEVEL@',
	'remove_tables' => 'prmopt',
);


$installdefs = array (
	'id' => 'SCRMWebService',
	'pre_execute' => array (
		0 => '<basepath>/scripts/pre_install_notes.php',
 	),
	'post_execute'=>array(
		0 => '<basepath>/scripts/post_install_notes.php',
 	),
	'copy' => array (
		0 => array (
			'from' => '<basepath>/scrmsugarws/',
			'to' => './custom/scrmsugarws',
		),
		1 => array (
			'from' => '<basepath>/plugin/',
			'to' => './custom/scrmsugarws/noteswidget',
		),
	),
);
?>