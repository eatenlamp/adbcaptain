package adb.captain.domain.repository

import adb.captain.domain.model.Command
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс для работы с историей команд.
 */
interface HistoryRepository {
    fun getHistory(): Flow<List<Command>>
    suspend fun addCommand(command: Command)
    suspend fun clearHistory()
    suspend fun deleteCommand(id: Long)
}
