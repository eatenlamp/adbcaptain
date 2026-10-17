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
import adb.captain.domain.model.Macro
import adb.captain.domain.repository.AdbRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Saved command workflows: store them, list them, run them in order. */
class MacroUseCase @Inject constructor(
    private val dao: MacroDao,
    private val repository: AdbRepository
) {
    fun observe(): Flow<List<Macro>> =
        dao.observeAll().map { list ->
            list.map { Macro(it.id, it.name, it.commandList(), it.createdAt) }
        }

    suspend fun save(name: String, commands: List<String>) {
        val clean = commands.map { it.trim() }.filter { it.isNotEmpty() }
        if (name.isBlank() || clean.isEmpty()) return
        dao.insert(MacroEntity(name = name.trim(), commands = clean.joinToString("\n")))
    }

    suspend fun delete(macro: Macro) {
        dao.delete(
            MacroEntity(id = macro.id, name = macro.name, commands = macro.commands.joinToString("\n"))
        )
    }

    /** Runs every command in order, returning the combined output. */
    suspend fun run(macro: Macro): String {
        val out = StringBuilder()
        macro.commands.forEach { cmd ->
            out.appendLine("$ ${'$'} $cmd")
            runCatching { repository.executeCommand(cmd) }
                .getOrElse { "Error: ${it.message}" }
                .let { out.appendLine(it) }
        }
        return out.toString().trimEnd()
    }
}