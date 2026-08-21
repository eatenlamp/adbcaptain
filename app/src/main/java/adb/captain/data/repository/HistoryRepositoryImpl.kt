package adb.captain.data.repository

import adb.captain.data.local.dao.CommandDao
import adb.captain.data.local.entity.toDomain
import adb.captain.data.local.entity.toEntity
import adb.captain.domain.model.Command
import adb.captain.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HistoryRepositoryImpl @Inject constructor(
    private val dao: CommandDao
) : HistoryRepository {
    override fun getHistory(): Flow<List<Command>> = dao.getAllCommands().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun addCommand(command: Command) {
        dao.insertCommand(command.toEntity())
    }

    override suspend fun clearHistory() {
        dao.clearAll()
    }

    override suspend fun deleteCommand(id: Long) {
        dao.deleteById(id)
    }
}
