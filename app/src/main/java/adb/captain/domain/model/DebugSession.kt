/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.model

/**
 * A debugging session: what was watched, for how long and what the app in
 * focus was while the problem happened.
 */
data class DebugSession(
    val startedAt: Long,
    val packageName: String = "",
    val commands: List<String> = emptyList(),
    val logTail: List<String> = emptyList(),
    val notes: String = ""
) {
    fun elapsedMillis(now: Long): Long = (now - startedAt).coerceAtLeast(0L)

    /** Plain-text report entry for the bug pack. */
    fun toReport(): String = buildString {
        appendLine("# Debug session")
        appendLine("started: $startedAt")
        appendLine("elapsed_ms: ${elapsedMillis(System.currentTimeMillis())}")
        appendLine("package: ${packageName.ifBlank { "unknown" }}")
        appendLine()
        if (commands.isNotEmpty()) {
            appendLine("## Commands (${commands.size})")
            commands.forEach { appendLine("- $it") }
            appendLine()
        }
        if (logTail.isNotEmpty()) {
            appendLine("## Log tail (${logTail.size} lines)")
            logTail.forEach { appendLine(it) }
            appendLine()
        }
        if (notes.isNotBlank()) {
            appendLine("## Notes")
            appendLine(notes)
        }
    }.trimEnd() + "\n"
}