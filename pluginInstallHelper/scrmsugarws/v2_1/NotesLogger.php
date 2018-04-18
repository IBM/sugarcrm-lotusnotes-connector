<?php
require_once ('include/SugarLogger/SugarLogger.php');

class NotesLogger extends SugarLogger
{

    const DEFAULT_LOG_LEVEL = "info";

    public $NOTES_LOG_LEVEL;

    /**
     * properties for the Publish_Logger
     */
    protected $logfile = 'notes';

    protected $ext = '.log';

    protected $dateFormat = '%c';

    protected $logSize = '5MB';

    protected $maxLogs = 5;

    protected $filesuffix = "";

    protected $date_suffix = "";

    protected $log_dir = './';

    /**
     * Constructor
     *
     * Reads the config file for logger settings
     */
    public function __construct()
    {
        global $sugar_config;
        if (isset($sugar_config['notes_log_directory'])) {
            $this->log_dir = $sugar_config['notes_log_directory'];
            // Append a final / if necessary
            if (substr($this->log_dir, - 1) != '/') {
                $this->log_dir .= '/';
            }
        }

        if (isset($sugar_config['notes_log_level'])) {
            $this->NOTES_LOG_LEVEL = $sugar_config['notes_log_level'];
        }

        $this->_doInitialization();
    }

    public function logMessage($level, $message)
    {
        $logLevels = array(
            'debug' => 0,
            'info' => 10,
            'warn' => 20,
            'deprecated' => 30,
            'error' => 40,
            'fatal' => 50,
            'off' => 60
        );

        if (isset($logLevels[$level])) {
            $levelnumber = $logLevels[$level];

            $maxlevel = NotesLogger::DEFAULT_LOG_LEVEL;
            if (! empty($this->NOTES_LOG_LEVEL) && isset($logLevels[$this->NOTES_LOG_LEVEL])) {
                $maxlevel = $this->NOTES_LOG_LEVEL;
            }
            $maxlevelnumber = $logLevels[$maxlevel];

            if ($levelnumber >= $maxlevelnumber) {
                $this->log($level, '' . $message);
            }
        }

        /*
         * log levels [debug] => Debug [info] => Info [warn] => Warn [deprecated] => Deprecated [error] => Error [fatal]
         * => Fatal [security] => Security [off] => Off
         */
    }

    public function logEntry($method)
    {
        $this->logMessage("debug", $method . ' Entry');
    }

    public function logExit($method)
    {
        $this->logMessage("debug", $method . ' Exit');
    }
}

