/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.model

/** A hand built intent, translated into an `am start` command. */
data class IntentSpec(
    val action: String = "android.intent.action.VIEW",
    val data: String = "",
    val type: String = "",
    val category: String = "",
    val component: String = "",
    val flags: String = "",
    /** Extra arguments as raw `key=value` pairs, e.g. `android.intent.extra.TEXT=hi`. */
    val extras: List<String> = emptyList()
) {
    val isRunnable: Boolean
        get() = action.isNotBlank() || component.isNotBlank()

    fun toCommand(): String = buildString {
        append("am start")
        if (action.isNotBlank()) append(" -a ").append(action)
        if (category.isNotBlank()) append(" -c ").append(category)
        if (data.isNotBlank()) append(" -d ").append(quote(data))
        if (type.isNotBlank()) append(" -t ").append(type)
        if (component.isNotBlank()) append(" -n ").append(component)
        if (flags.isNotBlank()) append(" -f ").append(flags)
        extras.forEach { pair ->
            val key = pair.substringBefore('=').trim()
            val value = pair.substringAfter('=', "").trim()
            if (key.isNotEmpty()) {
                append(" --es ").append(quote(key)).append(' ').append(quote(value))
            }
        }
    }

    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}