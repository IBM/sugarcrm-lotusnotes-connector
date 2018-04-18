<?php
if (!defined('sugarEntry') || !sugarEntry)
    die('Not A Valid Entry Point');

require_once("custom/modules/Home/QuickSearchCustom.php");

class QuickSearchQueryCustomStrategyNotes extends QuickSearchQueryCustomStrategy
{

    /**
     * jdjohnso: the show kana client name boxes us into searching on name,
     * so removing it for use with a client id search
     */
    protected function updateAccountArrayArguments($args)
    {
        global $current_user;

        // accounts must have "ccms_id", "ccms_level", "billing_address_city", "name"
        $args['field_list'] = isset($args['field_list']) ? (array) $args['field_list'] : array();
        if ($current_user->getPreference('show_alt_client_name') == 'on') {
            $args['field_list'][] = "alt_language_name";
        }
        $fields = array(
            "ccms_id",
            "ccms_level",
            "billing_address_city",
            "name"
        );
        $args['field_list'] = array_unique(array_merge($args['field_list'], $fields));
        return $args;
    }
}
