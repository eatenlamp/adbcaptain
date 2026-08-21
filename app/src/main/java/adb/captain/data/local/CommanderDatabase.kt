package adb.captain.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import adb.captain.data.local.dao.CommandDao
import adb.captain.data.local.entity.CommandEntity

@Database(entities = [CommandEntity::class], version = 1, exportSchema = false)
abstract class CommanderDatabase : RoomDatabase() {
    abstract val commandDao: CommandDao
}
