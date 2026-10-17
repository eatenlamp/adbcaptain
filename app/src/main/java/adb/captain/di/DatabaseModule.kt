/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.di

import android.content.Context
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
        ).addMigrations(MIGRATION_1_2).build()
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
