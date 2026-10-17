/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.model

/** One crash taken from the logcat crash buffer. */
data class CrashReport(
    val pid: Int,
    val packageName: String,
    val timestamp: String,
    val summary: String,
    val stackTrace: List<String>
) {
    val fullText: String
        get() = buildString {
            appendLine("$timestamp  $packageName (pid $pid)")
            appendLine(summary)
            stackTrace.forEach { appendLine(it) }
        }
}