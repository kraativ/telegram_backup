/* Telegram_Backup
* Copyright (C) 2016 Fabian Schlenz
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>. */
package de.fabianonline.telegram_backup

import java.util.*

internal object CommandLineOptions {

	private const val A = "-a"
	private const val ACCOUNT = "--account"
    private const val A_ = "-A"
    private const val LIST_ACCOUNTS = "--list-accounts"
    private const val ANONYMIZE = "--anonymize"
	private const val CONSOLE = "--console"
	private const val D = "-d"
	private const val DAEMON = "--daemon"
	private const val DEBUG = "--debug"
	private const val E = "-e"
	private const val EXPORT = "--export"
	private const val H = "-h"
	private const val HELP = "--help"
	private const val L = "-l"
	private const val LOGIN = "--login"
    private const val LICENSE = "--license"
    private const val LIMIT_MESSAGES = "--limit-messages"
	private const val NO_MEDIA = "--no-media"
	private const val NO_PAGINATION = "--no-pagination"
	private const val PAGINATION = "--pagination"
    private const val S = "-s"
    private const val SHOW_ALL = "--show-all"
	private const val SHOW_CHANNELS = "--show-channels"
	private const val SHOW_SUPERGROUPS = "--show-supergroups"
	private const val STATS = "--stats"
	private const val T = "-t"
	private const val TARGET = "--target"
	private const val TEST = "--test"
	private const val TRACE = "--trace"
	private const val TRACE_TELEGRAM = "--trace-telegram"
    private const val V_ = "-V"
    private const val VERSION = "--version"
    private const val W = "-w"
    private const val WITH_OBJECTS_IDS = "--with-objects-ids"
    private const val WITH_CHANNELS = "--with-channels"
    private const val WITH_SUPERGROUPS = "--with-supergroups"

    val help: String by lazy { """
Valid options are:
 $H, $HELP                Shows this help.
 $V_, $VERSION             Show version.
 $A, $ACCOUNT <x>         Use account <x>.
 $L, $LOGIN               Login to an existing telegram account.
     $DEBUG               Shows some debug information.
     $TRACE               Shows lots of debug information. Overrides --debug.
     $TRACE_TELEGRAM      Shows lots of debug messages from the library used to
                               access Telegram.
 $A_, $LIST_ACCOUNTS       List all existing accounts
     $LIMIT_MESSAGES <x>  Downloads at most the most recent <x> messages.
     $NO_MEDIA            Do not download media files.
 $T, $TARGET <x>          Target directory for the files.
 $E, $EXPORT <format>     Export the database. Valid formats are:
                               html - Creates HTML files.
     $PAGINATION <x>      Splits the HTML export into multiple HTML pages with
                               <x> messages per page. Default is 5000.
     $NO_PAGINATION       Disables pagination.
     $LICENSE             Displays the license of this program.
 $D, $DAEMON              Keep running after the backup and automatically save
                               new messages.
     $ANONYMIZE           (Try to) Remove all sensitive information from output.
                               Useful for requesting support.
     $STATS               Print some usage statistics.
 $W, $WITH_OBJECTS_IDS [ids]
                           Backup channels or supergroups with [ids] as well.
 $S, $SHOW_ALL            Show channels and supergroups.
     $SHOW_CHANNELS       Show channels.
     $WITH_CHANNELS       Backup channels as well.
     $SHOW_SUPERGROUPS    Show supergroups.
     $WITH_SUPERGROUPS    Backup supergroups as well.
""" }

	var cmd_console = false
	var cmd_help = false
	var cmd_login = false
	var cmd_debug = false
	var cmd_trace = false
	var cmd_trace_telegram = false
	var cmd_list_accounts = false
	var cmd_version = false
	var cmd_license = false
	var cmd_daemon = false
	var cmd_no_media = false
	var cmd_anonymize = false
	var cmd_stats = false
	var cmd_show_all = false
	var cmd_channels = false
	var cmd_channels_show = false
	var cmd_supergroups = false
	var cmd_supergroups_show = false
    var cmd_ids_array = ArrayList<Int>()
	var cmd_no_pagination = false
	var val_account: String? = null
	var val_limit_messages: Int? = null
	var val_target: String? = null
	var val_export: String? = null
	var val_test: Int? = null
	var val_pagination: Int = Config.DEFAULT_PAGINATION

	fun parseOptions(args: Array<String>) {
		var lastCommand: String? = null
		for (arg in args) {
			if (lastCommand != null) {
                if (lastCommand == WITH_OBJECTS_IDS) {
                    if (arg.isNumber()) {
                        cmd_ids_array.add(arg.toInt())
                        continue
                    } else {
                        lastCommand = null
                    }
                } else {
                    when (lastCommand) {
                        A, ACCOUNT -> val_account = arg
                        LIMIT_MESSAGES -> val_limit_messages = Integer.parseInt(arg)
                        T, TARGET -> val_target = arg
                        E, EXPORT -> val_export = arg
                        TEST -> val_test = Integer.parseInt(arg)
                        PAGINATION -> val_pagination = Integer.parseInt(arg)
                    }
                    lastCommand = null
                    continue
                }
			}
			when (arg) {
				A, ACCOUNT,
                LIMIT_MESSAGES,
                T, TARGET,
                E, EXPORT,
                PAGINATION,
                TEST,
                W, WITH_OBJECTS_IDS -> lastCommand = arg

				H, HELP -> cmd_help = true
				L, LOGIN -> cmd_login = true
				DEBUG -> cmd_debug = true
				TRACE -> cmd_trace = true
				TRACE_TELEGRAM -> cmd_trace_telegram = true
				A_, LIST_ACCOUNTS -> cmd_list_accounts = true
                CONSOLE -> cmd_console = true
				V_, VERSION -> cmd_version = true
				NO_PAGINATION -> cmd_no_pagination = true
				LICENSE -> cmd_license = true
				D, DAEMON -> cmd_daemon = true
				NO_MEDIA -> cmd_no_media = true
				ANONYMIZE -> cmd_anonymize = true
				STATS -> cmd_stats = true
                S, SHOW_ALL -> cmd_show_all = true
				WITH_CHANNELS -> cmd_channels = true
                SHOW_CHANNELS -> cmd_channels_show = true
				WITH_SUPERGROUPS -> cmd_supergroups = true
                SHOW_SUPERGROUPS -> cmd_supergroups_show = true

				else -> throw RuntimeException("Unknown command $arg")
			}
		}
		if (lastCommand != null && (lastCommand != WITH_OBJECTS_IDS || cmd_ids_array.isEmpty())) {
			CommandLineController.show_error("Command $lastCommand had no parameter set.")
		}
	}
}
