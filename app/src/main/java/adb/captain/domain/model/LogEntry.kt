/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.model

/**
 * Модель записи в Logcat.
 */
data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val pid: Int = 0,
    val tid: Int = 0,
    /** Monotonic id assigned on arrival; guarantees unique LazyColumn keys. */
    val id: Long = 0
)

enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR, FATAL;

    companion object {
        fun fromChar(c: Char): LogLevel = when (c.uppercaseChar()) {
            'V' -> VERBOSE
            'D' -> DEBUG
            'I' -> INFO
            'W' -> WARN
            'E' -> ERROR
            'F' -> FATAL
            else -> INFO
        }
    }
}
