package adb.captain.data.local.dao

import androidx.room.*
import adb.captain.data.local.entity.CommandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC")
    fun getAllCommands(): Flow<List<CommandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: CommandEntity)

    @Query("DELETE FROM command_history")
    suspend fun clearAll()

    @Query("DELETE FROM command_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
