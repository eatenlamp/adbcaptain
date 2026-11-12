/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.model

/** A named command workflow the user can run in one tap. */
data class Macro(
    val id: Long = 0,
    val name: String,
    val steps: List<MacroStep> = emptyList(),
    val createdAt: Long = 0L
) {
    /** Flat command list, used by summaries and the command palette. */
    val commands: List<String> get() = steps.map { it.command }

    /** Variables referenced by the steps, in order of first appearance. */
    val variables: List<String> get() = MacroVariables.names(this)
}

/** One command inside a macro, with its own guard rails. */
data class MacroStep(
    val command: String,
    val condition: StepCondition = StepCondition.ALWAYS,
    val conditionValue: String = "",
    val onError: StepFailure = StepFailure.STOP,
    val waitMs: Long = 0L
)

/** When to run a step, judged by the output of the previous one. */
enum class StepCondition {
    ALWAYS,
    OUTPUT_CONTAINS,
    OUTPUT_NOT_CONTAINS,
    OUTPUT_EMPTY,
    OUTPUT_NOT_EMPTY
}

/** What to do when a command reports an error. */
enum class StepFailure {
    STOP,
    CONTINUE
}

/**
 * `{{name}}` placeholders in macro commands. `serial` is filled in by the app,
 * everything else is asked from the user right before the run.
 */
object MacroVariables {
    const val SERIAL = "serial"

    // Both braces are escaped on purpose. The JVM accepts a bare `}}`, but
    // Android compiles patterns with ICU, which rejects it with "Syntax error
    // near index ..." and takes the whole object down with ExceptionInInitializerError.
    private val pattern = Regex("""\{\{\s*([a-zA-Z0-9_.-]+)\s*\}\}""")

    fun names(macro: Macro): List<String> = names(macro.steps.map { it.command })

    fun names(commands: List<String>): List<String> {
        val found = linkedSetOf<String>()
        commands.forEach { command ->
            pattern.findAll(command).forEach { match ->
                val name = match.groupValues[1]
                if (name != SERIAL) found.add(name)
            }
        }
        return found.toList()
    }

    fun resolve(command: String, values: Map<String, String>): String =
        pattern.replace(command) { match ->
            values[match.groupValues[1]] ?: match.value
        }

    fun usesSerial(commands: List<String>): Boolean =
        commands.any { pattern.containsMatchIn(it) && pattern.findAll(it).any { m -> m.groupValues[1] == SERIAL } }
}
