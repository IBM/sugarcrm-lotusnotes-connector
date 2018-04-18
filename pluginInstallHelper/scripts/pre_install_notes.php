<?php

pre_install_notes();

function pre_install_notes() {
	$GLOBALS['log']->info('IBMNotes->Beginning Notes module pre install script');
	print("Running pre-install checks:<br/>");
	
	$preInstallSuccess = true;
	
	print("Checking for Regexp::Optimizer perl module...<br/>");	
	$output = exec("perldoc -l Regexp::Optimizer 2>&1");
	if(strpos($output, "No documentation found") !== false) {
		$GLOBALS['log']->fatal('IBMNotes->Regexp::Optimizer perl module not found.');
		print("Regexp::Optimizer perl module not found.<br/>");
		$preInstallSuccess = false;
	}
	
	if(!$preInstallSuccess) {
		$GLOBALS['log']->fatal('IBMNotes->Pre-install checks failed, unable to install module.');
		print("Pre-install checks failed, unable to install module.<br/>");
		exit(1);
	}
	
	$GLOBALS['log']->info('IBMNotes->Ending Notes module pre install script');
	print("Pre-install checks passed.<br/>");
}

?>