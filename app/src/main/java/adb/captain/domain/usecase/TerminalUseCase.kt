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
