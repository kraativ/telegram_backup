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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package de.fabianonline.telegram_backup

import com.github.badoualy.telegram.tl.exception.RpcErrorException
import com.github.badoualy.telegram.tl.api.TLAbsMessage
import com.github.badoualy.telegram.tl.api.TLAbsUser
import com.github.badoualy.telegram.tl.api.TLAbsChat
import com.github.badoualy.telegram.api.Kotlogram
import java.io.File
import java.util.Vector
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import com.google.gson.*
import com.github.salomonbrys.kotson.*
import java.net.URL
import org.apache.commons.io.IOUtils
import de.fabianonline.telegram_backup.Version
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Utils {
	@JvmField public val VERSIONS_EQUAL = 0
	@JvmField public val VERSION_1_NEWER = 1
	@JvmField public val VERSION_2_NEWER = 2
	
	var hasSeenFloodWaitMessage = false

	var anonymize = false

	private val logger = LoggerFactory.getLogger(Utils::class.java) as Logger

	fun print_accounts(file_base: String) {
		println("List of available accounts:")
		val accounts = getAccounts(file_base)
		if (accounts.size > 0) {
			for (str in accounts) {
				println(" " + str.anonymize())
			}
			println("Use '--account <x>' to use one of those accounts.")
		} else {
			println("NO ACCOUNTS FOUND")
			println("Use '--login' to login to a telegram account.")
		}
	}

	fun getAccounts(file_base: String): Vector<String> {
		val accounts = Vector<String>()
		val folder = File(file_base)
		val files = folder.listFiles()
		if (files != null)
			for (f in files) {
				if (f.isDirectory() && f.getName().startsWith("+")) {
					accounts.add(f.getName())
				}
			}
		return accounts
	}

	fun getNewestVersion(): Version? {
		try {
			val data_url = "https://api.github.com/repos/fabianonline/telegram_backup/releases"
			logger.debug("Requesting current release info from {}", data_url)
			val json = IOUtils.toString(URL(data_url))
			val parser = JsonParser()
			val root_elm = parser.parse(json)
			if (root_elm.isJsonArray()) {
				val root = root_elm.getAsJsonArray()
				var newest_version: JsonObject? = null
				for (e in root)
					if (e.isJsonObject()) {
						val version = e.getAsJsonObject()
						if (version.getAsJsonPrimitive("prerelease").getAsBoolean() == false) {
							newest_version = version
							break
						}
					}
				if (newest_version == null) return null
				val new_v = newest_version.getAsJsonPrimitive("tag_name").getAsString()
				logger.debug("Found current release version {}", new_v)
				val cur_v = Config.APP_APPVER

				val result = compareVersions(cur_v, new_v)

				return Version(new_v, newest_version.getAsJsonPrimitive("html_url").getAsString(), newest_version.getAsJsonPrimitive("body").getAsString(), result == VERSION_2_NEWER)
			}
			return null
		} catch (e: Exception) {
			return null
		}

	}
	
	fun obeyFloodWait(max_tries: Int = -1, method: () -> Unit) {
		var tries = 0
		while (true) {
			tries++
			if (max_tries>0 && tries>max_tries) throw MaxTriesExceededException()
			logger.trace("This is try ${tries}.")
			try {
				method.invoke()
				// If we reach this, the method has returned successfully -> we are done
				return
			} catch(e: RpcErrorException) {
				// If we got something else than a FLOOD_WAIT error, we just rethrow it
				if (e.getCode() != 420) throw e
				
				val delay = e.getTagInteger()!!.toLong()
				
				if (!hasSeenFloodWaitMessage) {
					println(
						"\n" +
						"Telegram complained about us (okay, me) making too many requests in too short time by\n" +
						"sending us \"${e.getTag()}\" as an error. So we now have to wait a bit. Telegram\n" +
						"asked us to wait for ${delay} seconds.\n" +
						"\n" +
						"So I'm going to do just that for now. If you don't want to wait, you can quit by pressing\n" +
						"Ctrl+C. You can restart me at any time and I will just continue to download your\n" +
						"messages and media. But be advised that just restarting me is not going to change\n" +
						"the fact that Telegram won't talk to me until then." +
						"\n")
				} else {
					print(" Waiting...")
				}
				
				try { TimeUnit.SECONDS.sleep(delay + 1) } catch (e: InterruptedException) { }
				
				if (hasSeenFloodWaitMessage) {
					//    "  W a i t i n g . . ."
					print("\b\b\b\b\b\b\b\b\b\b\b")
				}
				
				hasSeenFloodWaitMessage = true
			} catch (e: TimeoutException) {
				println(
					"\n" +
					"Telegram took too long to respond to our request.\n" +
					"I'm going to wait a minute and then try again." +
					"\n")
				try { TimeUnit.MINUTES.sleep(1) } catch (e: InterruptedException) { }
			}
		}
	}

	@JvmStatic
	fun compareVersions(v1: String, v2: String): Int {
		logger.debug("Comparing versions {} and {}.", v1, v2)
		if (v1.equals(v2)) return VERSIONS_EQUAL

		val v1_p = v1.split("-", limit = 2)
		val v2_p = v2.split("-", limit = 2)

		logger.trace("Parts to compare without suffixes: {} and {}.", v1_p[0], v2_p[0])

		val v1_p2 = v1_p[0].split(".")
		val v2_p2 = v2_p[0].split(".")

		logger.trace("Length of the parts without suffixes: {} and {}.", v1_p2.size, v2_p2.size)

		var i: Int
		i = 0
		while (i < v1_p2.size && i < v2_p2.size) {
			val i_1 = Integer.parseInt(v1_p2[i])
			val i_2 = Integer.parseInt(v2_p2[i])
			logger.trace("Comparing parts: {} and {}.", i_1, i_2)
			if (i_1 > i_2) {
				logger.debug("v1 is newer")
				return VERSION_1_NEWER
			} else if (i_2 > i_1) {
				logger.debug("v2 is newer")
				return VERSION_2_NEWER
			}
			i++
		}
		logger.trace("At least one of the versions has run out of parts.")
		if (v1_p2.size > v2_p2.size) {
			logger.debug("v1 is longer, so it is newer")
			return VERSION_1_NEWER
		} else if (v2_p2.size > v1_p2.size) {
			logger.debug("v2 is longer, so it is newer")
			return VERSION_2_NEWER
		}

		// startsWith
		if (v1_p.size > 1 && v2_p.size == 1) {
			logger.debug("v1 has a suffix, v2 not.")
			if (v1_p[1].startsWith("pre")) {
				logger.debug("v1 is a pre version, so v1 is newer")
				return VERSION_2_NEWER
			} else {
				return VERSION_1_NEWER
			}
		} else if (v1_p.size == 1 && v2_p.size > 1) {
			logger.debug("v1 has no suffix, but v2 has")
			if (v2_p[1].startsWith("pre")) {
				logger.debug("v2 is a pre version, so v1 is better")
				return VERSION_1_NEWER
			} else {
				return VERSION_2_NEWER
			}
		} else if (v1_p.size > 1 && v2_p.size > 1) {
			logger.debug("Both have a suffix")
			if (v1_p[1].startsWith("pre") && !v2_p[1].startsWith("pre")) {
				logger.debug("v1 is a 'pre' version, v2 not.")
				return VERSION_2_NEWER
			} else if (!v1_p[1].startsWith("pre") && v2_p[1].startsWith("pre")) {
				logger.debug("v2 is a 'pre' version, v2 not.")
				return VERSION_1_NEWER
			}
			return VERSIONS_EQUAL
		}
		logger.debug("We couldn't find a real difference, so we're assuming the versions are equal-ish.")
		return VERSIONS_EQUAL
	}
}

fun String.anonymize(): String {
	return if (!Utils.anonymize) this else this.replace(Regex("[0-9]"), "1").replace(Regex("[A-Z]"), "A").replace(Regex("[a-z]"), "a") + " (ANONYMIZED)"
}

fun Any.toJson(): String = Gson().toJson(this)
fun Any.toPrettyJson(): String = GsonBuilder().setPrettyPrinting().create().toJson(this)

fun JsonObject.isA(name: String): Boolean = this.contains("_constructor") && this["_constructor"].string.startsWith(name + "#")
fun JsonElement.isA(name: String): Boolean = this.obj.isA(name)

class MaxTriesExceededException(): RuntimeException("Max tries exceeded") {}

fun TLAbsMessage.toJson(): String {
	val json = Gson().toJsonTree(this).obj
	cleanUpMessageJson(json)
	json["api_layer"] = Kotlogram.API_LAYER
	return json.toString()
}

fun TLAbsChat.toJson(): String {
	val json = Gson().toJsonTree(this).obj
	json["api_layer"] = Kotlogram.API_LAYER
	return json.toString()
}

fun TLAbsUser.toJson(): String {
	val json = Gson().toJsonTree(this).obj
	json["api_layer"] = Kotlogram.API_LAYER
	return json.toString()
}

fun cleanUpMessageJson(json : JsonElement) {
	if (json.isJsonArray) {
		json.array.forEach {cleanUpMessageJson(it)}
		return
	} else if (!json.isJsonObject) {
		return
	}
	if (json.obj.has("bytes")) {
		json.obj -= "bytes"
		return
	}
	json.obj.forEach {_: String, elm: JsonElement ->
		if (elm.isJsonObject || elm.isJsonArray) cleanUpMessageJson(elm)
	}
}
