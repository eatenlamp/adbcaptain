/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.usecase

import adb.captain.domain.model.Command
import adb.captain.domain.repository.AdbRepository
import adb.captain.domain.repository.HistoryRepository
import adb.captain.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * UseCase для работы с терминалом.
 */
class TerminalUseCase @Inject constructor(
    private val adbRepository: AdbRepository,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository
) {
    fun getHistory(): Flow<List<Command>> = historyRepository.getHistory()

    fun getFavoriteCommands(): Flow<List<String>> = settingsRepository.getFavoriteCommands()

    suspend fun toggleFavorite(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return
        val current = settingsRepository.getFavoriteCommands().first()
        val updated = if (current.contains(trimmed)) current - trimmed else current + trimmed
        settingsRepository.setFavoriteCommands(updated)
    }

    fun isAutoCompleteEnabled(): Flow<Boolean> = settingsRepository.isAutoCompleteEnabled()

    suspend fun executeCommand(commandText: String): Command {
        val output = adbRepository.executeCommand(commandText)
        val isSuccess = !output.startsWith("Error:")
        val command = Command(text = commandText, output = output, isSuccess = isSuccess)
        
        if (settingsRepository.shouldSaveHistory().first()) {
            historyRepository.addCommand(command)
        }
        
        return command
    }

    suspend fun clearHistory() = historyRepository.clearHistory()
}
