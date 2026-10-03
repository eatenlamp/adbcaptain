/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.util

import adb.captain.domain.model.LogEntry
import adb.captain.domain.model.LogLevel

/**
 * Разбирает строки Logcat в [LogEntry].
 *
 * Поддерживаются форматы:
 *  - threadtime: "MM-DD HH:MM:SS.mmm PID TID LEVEL TAG: message"
 *  - time:       "MM-DD HH:MM:SS.mmm LEVEL/TAG(PID): message"
 *  - legacy:     "LEVEL MM-DD HH:MM:SS.mmm TAG PID:1234 message"
 */
object LogcatParser {

    private val SPACE_REGEX = Regex("\\s+")
    private val PID_REGEX = Regex("""PID:(\d+)""")

    fun parse(line: String): LogEntry? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("------")) return null
        val parts = SPACE_REGEX.split(trimmed)
        if (parts.size < 3) return null
        return try {
            when {
                // threadtime
                parts.size >= 6 && isDateToken(parts[0]) && isTimeToken(parts[1]) &&
                    parts[2].all { it.isDigit() } && parts[3].all { it.isDigit() } &&
                    isLevelChar(parts[4]) -> {
                    val pid = parts[2].toIntOrNull() ?: 0
                    val tid = parts[3].toIntOrNull() ?: 0
                    var tag = parts[5].removeSuffix(":")
                    if (tag.isEmpty()) tag = "Unknown"
                    val message = if (parts.size > 6) parts.drop(6).joinToString(" ") else ""
                    LogEntry("${parts[0]} ${parts[1]}", LogLevel.fromChar(parts[4][0]), tag, message, pid, tid)
                }
                // time
                parts.size >= 3 && isDateToken(parts[0]) && isTimeToken(parts[1]) &&
                    parts[2].length >= 3 && isLevelChar(parts[2]) && parts[2][1] == '/' -> {
                    val levelChar = parts[2][0]
                    val tagAndPid = parts[2].substringAfter('/')
                    val tag = tagAndPid.substringBefore('(').removeSuffix(":").ifEmpty { "Unknown" }
                    val pid = tagAndPid.substringAfter('(', "").substringBefore(')').trim().toIntOrNull() ?: 0
                    val message = if (parts.size > 3) parts.drop(3).joinToString(" ") else ""
                    LogEntry("${parts[0]} ${parts[1]}", LogLevel.fromChar(levelChar), tag, message, pid)
                }
                // legacy
                parts[0].length == 1 && isLevelChar(parts[0]) && parts.size >= 3 &&
                    isDateToken(parts[1]) && isTimeToken(parts[2]) -> {
                    val levelChar = parts[0][0]
                    val timestamp = "${parts[1]} ${parts[2]}"
                    var tag = if (parts.size > 3) parts[3] else ""
                    var idx = 4
                    var pid = 0
                    if (idx < parts.size) {
                        val p = parts[idx]
                        val m = PID_REGEX.find(p)
                        if (m != null) {
                            pid = m.groupValues.getOrNull(1)?.toIntOrNull() ?: 0
                            idx++
                        } else if (p.startsWith("PID:")) {
                            pid = p.substringAfter("PID:").takeWhile { it.isDigit() }.toIntOrNull() ?: 0
                            idx++
                        }
                    }
                    val message = if (idx < parts.size) parts.drop(idx).joinToString(" ") else ""
                    if (tag.isEmpty()) tag = "Unknown"
                    if (tag.endsWith(":")) tag = tag.dropLast(1)
                    LogEntry(timestamp, LogLevel.fromChar(levelChar), tag, message, pid)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isDateToken(token: String): Boolean =
        token.length == 5 && token[2] == '-' &&
            token[0].isDigit() && token[1].isDigit() && token[3].isDigit() && token[4].isDigit()

    private fun isTimeToken(token: String): Boolean =
        token.length >= 8 && token[2] == ':' && token[5] == ':' && token.contains('.')

    private fun isLevelChar(token: String): Boolean =
        token.length == 1 && token[0] in "VDIWEF"
}
