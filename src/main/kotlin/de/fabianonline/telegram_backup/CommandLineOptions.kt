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

internal object CommandLineOptions {

    private const val S = "-s"
    private const val SHOW_ALL = "--show-all"
	private const val SHOW_CHANNELS = "--show-channels"
	private const val SHOW_SUPERGROUPS = "--show-supergroups"

	public var cmd_console = false
	public var cmd_help = false
	public var cmd_login = false
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
	var cmd_no_pagination = false
	var val_account: String? = null
	var val_limit_messages: Int? = null
	var val_target: String? = null
	var val_export: String? = null
	var val_test: Int? = null
	var val_pagination: Int = Config.DEFAULT_PAGINATION
	@JvmStatic
	fun parseOptions(args: Array<String>) {
		var last_cmd: String? = null
		loop@ for (arg in args) {
			if (last_cmd != null) {
				when (last_cmd) {
					"--account" -> val_account = arg
					"--limit-messages" -> val_limit_messages = Integer.parseInt(arg)
					"--target" -> val_target = arg
					"--export" -> val_export = arg
					"--test" -> val_test = Integer.parseInt(arg)
					"--pagination" -> val_pagination = Integer.parseInt(arg)
				}
				last_cmd = null
				continue
			}
			when (arg) {
				"-a", "--account" -> {
					last_cmd = "--account"
					continue@loop
				}
				"-h", "--help" -> cmd_help = true
				"-l", "--login" -> cmd_login = true
				"--debug" -> cmd_debug = true
				"--trace" -> cmd_trace = true
				"--trace-telegram" -> cmd_trace_telegram = true
				"-A", "--list-accounts" -> cmd_list_accounts = true
				"--limit-messages" -> {
					last_cmd = arg
					continue@loop
				}
				"--console" -> cmd_console = true
				"-t", "--target" -> {
					last_cmd = "--target"
					continue@loop
				}
				"-V", "--version" -> cmd_version = true
				"-e", "--export" -> {
					last_cmd = "--export"
					continue@loop
				}
				"--pagination" -> {
					last_cmd = "--pagination"
					continue@loop
				}
				"--no-pagination" -> cmd_no_pagination = true
				"--license" -> cmd_license = true
				"-d", "--daemon" -> cmd_daemon = true
				"--no-media" -> cmd_no_media = true
				"--test" -> {
					last_cmd = "--test"
					continue@loop
				}
				"--anonymize" -> cmd_anonymize = true
				"--stats" -> cmd_stats = true
                S, SHOW_ALL -> cmd_show_all = true
				"--with-channels" -> cmd_channels = true
                SHOW_CHANNELS -> cmd_channels_show = true
				"--with-supergroups" -> cmd_supergroups = true
                SHOW_SUPERGROUPS -> cmd_supergroups_show = true
				else -> throw RuntimeException("Unknown command " + arg)
			}
		}
		if (last_cmd != null) {
			CommandLineController.show_error("Command $last_cmd had no parameter set.")
		}
	}
}
