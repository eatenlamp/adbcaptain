/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.usecase

import adb.captain.data.local.dao.MacroDao
import adb.captain.data.local.entity.MacroEntity
import adb.captain.data.local.entity.MacroStepEntity
import adb.captain.domain.model.Macro
import adb.captain.domain.model.MacroCodec
import adb.captain.domain.model.MacroStep
import adb.captain.domain.model.MacroVariables
import adb.captain.domain.model.StepCondition
import adb.captain.domain.model.StepFailure
import adb.captain.domain.repository.AdbRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Saved command workflows: store them, list them, run them step by step. */
class MacroUseCase @Inject constructor(
    private val dao: MacroDao,
    private val repository: AdbRepository
) {
    fun observe(): Flow<List<Macro>> =
        dao.observeAll().map { rows -> rows.map { it.toModel() } }

    suspend fun all(): List<Macro> = dao.getAll().map { it.toModel() }

    suspend fun find(id: Long): Macro? = dao.getById(id)?.toModel()

    /** Inserts or replaces a macro together with its steps. */
    suspend fun save(macro: Macro): Long {
        val clean = macro.copy(
            name = macro.name.trim(),
            steps = macro.steps.filter { it.command.isNotBlank() }
        )
        if (clean.name.isEmpty() || clean.steps.isEmpty()) return 0L

        val entity = MacroEntity(
            id = clean.id,
            name = clean.name,
            commands = clean.steps.joinToString("\n") { it.command },
            createdAt = clean.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        )
        val id = dao.insertMacro(entity)
        val macroId = id.takeIf { it > 0L } ?: clean.id
        dao.deleteSteps(macroId)
        dao.insertSteps(
            clean.steps.mapIndexed { index, step ->
                MacroStepEntity(
                    macroId = macroId,
                    position = index,
                    command = step.command.trim(),
                    condition = step.condition.name,
                    conditionValue = step.conditionValue,
                    onError = step.onError.name,
                    waitMs = step.waitMs.coerceAtLeast(0L)
                )
            }
        )
        return macroId
    }

    suspend fun save(name: String, commands: List<String>): Long =
        save(Macro(name = name, steps = commands.map { MacroStep(it) }))

    suspend fun delete(macro: Macro) {
        if (macro.id > 0L) dao.deleteById(macro.id)
    }

    fun encode(macro: Macro): String = MacroCodec.encode(macro)

    fun decode(text: String): Macro? = MacroCodec.decode(text)

    /**
     * Runs every step in order. [variables] fills the `{{name}}` placeholders;
     * `{{serial}}` is resolved by the app when the macro asks for it.
     */
    suspend fun run(macro: Macro, variables: Map<String, String> = emptyMap()): MacroRunResult {
        val output = StringBuilder()
        var skipped = 0
        var failedCommand: String? = null
        var previous = ""
        val resolvedVariables = variables.toMutableMap()

        if (MacroVariables.usesSerial(macro.commands) && resolvedVariables[MacroVariables.SERIAL].isNullOrBlank()) {
            resolvedVariables[MacroVariables.SERIAL] = currentSerial()
        }

        for ((index, step) in macro.steps.withIndex()) {
            if (!conditionMet(step, previous)) {
                skipped++
                output.appendLine("-- step ${index + 1} skipped (${step.condition.name.lowercase()})")
                continue
            }
            if (step.waitMs > 0L) delay(step.waitMs)

            val command = MacroVariables.resolve(step.command, resolvedVariables)
            output.appendLine("$ " + command)
            val result = runCatching { repository.executeCommand(command) }
                .getOrElse { "Error: ${it.message}" }
                .trimEnd()
            if (result.isNotEmpty()) output.appendLine(result)
            previous = result

            if (isError(result)) {
                if (step.onError == StepFailure.STOP) {
                    failedCommand = command
                    break
                }
            }
        }

        return MacroRunResult(
            output = output.toString().trimEnd(),
            success = failedCommand == null,
            skipped = skipped,
            failedCommand = failedCommand
        )
    }

    private fun conditionMet(step: MacroStep, previous: String): Boolean = when (step.condition) {
        StepCondition.ALWAYS -> true
        StepCondition.OUTPUT_CONTAINS ->
            previous.contains(step.conditionValue, ignoreCase = true)
        StepCondition.OUTPUT_NOT_CONTAINS ->
            !previous.contains(step.conditionValue, ignoreCase = true)
        StepCondition.OUTPUT_EMPTY -> previous.isBlank()
        StepCondition.OUTPUT_NOT_EMPTY -> previous.isNotBlank()
    }

    private fun isError(result: String): Boolean {
        val head = result.trimStart()
        return head.startsWith("Error") || head.startsWith("Exception")
    }

    private suspend fun currentSerial(): String {
        val fromDevices = runCatching { repository.getDevices().firstOrNull()?.serial }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
        if (fromDevices != null) return fromDevices
        return runCatching { repository.executeCommand("getprop ro.serialno").trim() }
            .getOrDefault("")
    }
}

/** What came out of a macro run, for the output panel. */
data class MacroRunResult(
    val output: String,
    val success: Boolean,
    val skipped: Int = 0,
    val failedCommand: String? = null
)
