package adb.captain.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import adb.captain.domain.model.Command
import java.util.Date

@Entity(tableName = "command_history")
data class CommandEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val timestamp: Long,
    val isSuccess: Boolean,
    val output: String
)

fun CommandEntity.toDomain() = Command(
    id = id,
    text = text,
    timestamp = Date(timestamp),
    isSuccess = isSuccess,
    output = output
)

fun Command.toEntity() = CommandEntity(
    id = id,
    text = text,
    timestamp = timestamp.time,
    isSuccess = isSuccess,
    output = output
)
