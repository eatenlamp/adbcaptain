/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.model

/**
 * JSON representation of a macro, used by the export and import actions.
 *
 * The format is deliberately small and self describing so a file can be
 * inspected, edited and shared by hand:
 *
 * ```json
 * {
 *   "format": "adb-captain-macro",
 *   "version": 1,
 *   "name": "Restart app",
 *   "steps": [
 *     { "command": "am force-stop {{package}}", "condition": "ALWAYS", "onError": "STOP", "waitMs": 0 }
 *   ]
 * }
 * ```
 */
object MacroCodec {

    const val FORMAT = "adb-captain-macro"
    const val VERSION = 1

    fun encode(macro: Macro): String = buildString {
        appendLine("{")
        appendLine("  \"format\": ${quote(FORMAT)},")
        appendLine("  \"version\": $VERSION,")
        appendLine("  \"name\": ${quote(macro.name)},")
        appendLine("  \"steps\": [")
        macro.steps.forEachIndexed { index, step ->
            append("    {")
            append("\"command\": ${quote(step.command)}, ")
            append("\"condition\": ${quote(step.condition.name)}, ")
            append("\"conditionValue\": ${quote(step.conditionValue)}, ")
            append("\"onError\": ${quote(step.onError.name)}, ")
            append("\"waitMs\": ${step.waitMs}")
            append("}")
            if (index != macro.steps.lastIndex) append(',')
            appendLine()
        }
        appendLine("  ]")
        append("}")
    }

    /** Returns null when the text is not a macro file this app understands. */
    fun decode(text: String): Macro? {
        val root = runCatching { Json.parse(text) as? Map<*, *> }.getOrNull() ?: return null
        if (root["format"] != FORMAT) return null
        val name = (root["name"] as? String)?.trim().orEmpty()
        if (name.isEmpty()) return null
        val rawSteps = root["steps"] as? List<*> ?: return null
        val steps = rawSteps.mapNotNull { raw ->
            val step = raw as? Map<*, *> ?: return@mapNotNull null
            val command = (step["command"] as? String)?.trim().orEmpty()
            if (command.isEmpty()) return@mapNotNull null
            MacroStep(
                command = command,
                condition = enumOr(step["condition"] as? String, StepCondition.ALWAYS),
                conditionValue = (step["conditionValue"] as? String).orEmpty(),
                onError = enumOr(step["onError"] as? String, StepFailure.STOP),
                waitMs = (step["waitMs"] as? Number)?.toLong() ?: 0L
            )
        }
        if (steps.isEmpty()) return null
        return Macro(name = name, steps = steps)
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

    private fun quote(value: String): String = buildString {
        append('"')
        value.forEach { ch ->
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch < ' ') append("\\u%04x".format(ch.code)) else append(ch)
            }
        }
        append('"')
    }

    /**
     * Tiny JSON reader: objects, arrays, strings, numbers, booleans and null.
     * Kept here so macro files do not pull a serialization library into the app.
     */
    private class Json(private val text: String) {
        private var pos = 0

        companion object {
            fun parse(text: String): Any? = Json(text).readValue().also { /* fully consumed */ }
        }

        private fun readValue(): Any? {
            skipWhitespace()
            if (pos >= text.length) error("unexpected end")
            return when (val ch = text[pos]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't' -> readLiteral("true", true)
                'f' -> readLiteral("false", false)
                'n' -> readLiteral("null", null)
                else -> if (ch == '-' || ch.isDigit()) readNumber() else error("unexpected $ch")
            }
        }

        private fun readObject(): Map<String, Any?> {
            expect('{')
            val result = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (peek() == '}') { pos++; return result }
            while (true) {
                skipWhitespace()
                val key = readString()
                skipWhitespace()
                expect(':')
                result[key] = readValue()
                skipWhitespace()
                when (val ch = next()) {
                    ',' -> continue
                    '}' -> return result
                    else -> error("unexpected $ch")
                }
            }
        }

        private fun readArray(): List<Any?> {
            expect('[')
            val result = mutableListOf<Any?>()
            skipWhitespace()
            if (peek() == ']') { pos++; return result }
            while (true) {
                result.add(readValue())
                skipWhitespace()
                when (val ch = next()) {
                    ',' -> continue
                    ']' -> return result
                    else -> error("unexpected $ch")
                }
            }
        }

        private fun readString(): String {
            expect('"')
            val out = StringBuilder()
            while (true) {
                when (val ch = next()) {
                    '"' -> return out.toString()
                    '\\' -> when (val esc = next()) {
                        '"', '\\', '/' -> out.append(esc)
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000C')
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'u' -> {
                            val hex = text.substring(pos, pos + 4)
                            pos += 4
                            out.append(hex.toInt(16).toChar())
                        }
                        else -> error("bad escape $esc")
                    }
                    else -> out.append(ch)
                }
            }
        }

        private fun readNumber(): Number {
            val start = pos
            if (peek() == '-') pos++
            while (pos < text.length && (text[pos].isDigit() || text[pos] in ".eE+-")) pos++
            val raw = text.substring(start, pos)
            return raw.toLongOrNull() ?: raw.toDouble()
        }

        private fun readLiteral(literal: String, value: Any?): Any? {
            require(text.startsWith(literal, pos)) { "expected $literal" }
            pos += literal.length
            return value
        }

        private fun skipWhitespace() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        private fun peek(): Char? = text.getOrNull(pos)

        private fun next(): Char = text[pos++]

        private fun expect(ch: Char) {
            require(next() == ch) { "expected $ch" }
        }
    }
}
