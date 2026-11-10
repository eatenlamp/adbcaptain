/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.di

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import adb.captain.data.local.CommanderDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import adb.captain.data.local.SettingsManager
import adb.captain.data.local.dao.CommandDao
import adb.captain.data.local.dao.MacroDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Adds the macros table without touching existing command history. */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS macros (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "commands TEXT NOT NULL, " +
                "createdAt INTEGER NOT NULL)"
        )
    }
}

/**
 * Moves every stored macro from one newline separated blob into the steps
 * table. The old column is left in place: it is still written for old bug
 * packs and for downgrade safety.
 */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS macro_steps (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "macroId INTEGER NOT NULL, " +
                "position INTEGER NOT NULL, " +
                "command TEXT NOT NULL, " +
                "condition TEXT NOT NULL, " +
                "conditionValue TEXT NOT NULL, " +
                "onError TEXT NOT NULL, " +
                "waitMs INTEGER NOT NULL, " +
                "FOREIGN KEY(macroId) REFERENCES macros(id) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_macro_steps_macroId ON macro_steps(macroId)"
        )
        // Read the old rows first: inserting while a cursor is open on the same
        // database is legal but fragile on some SQLite builds.
        val legacy = mutableListOf<Pair<Long, List<String>>>()
        db.query("SELECT id, commands FROM macros").use { cursor ->
            while (cursor.moveToNext()) {
                val macroId = cursor.getLong(0)
                val commands = cursor.getString(1) ?: continue
                val list = commands.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
                if (list.isNotEmpty()) legacy.add(macroId to list)
            }
        }
        legacy.forEach { (macroId, commands) ->
            commands.forEachIndexed { index, command ->
                val values = ContentValues().apply {
                    put("macroId", macroId)
                    put("position", index)
                    put("command", command)
                    put("condition", "ALWAYS")
                    put("conditionValue", "")
                    put("onError", "STOP")
                    put("waitMs", 0L)
                }
                db.insert("macro_steps", SQLiteDatabase.CONFLICT_ABORT, values)
            }
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CommanderDatabase {
        return Room.databaseBuilder(
            context,
            CommanderDatabase::class.java,
            "commander_db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            // Safety net: a wrong or missing migration must never brick the app.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideMacroDao(db: CommanderDatabase): MacroDao = db.macroDao

    @Provides
    fun provideCommandDao(db: CommanderDatabase): CommandDao {
        return db.commandDao
    }

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

}
