/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import adb.captain.data.local.dao.CommandDao
import adb.captain.data.local.dao.MacroDao
import adb.captain.data.local.entity.CommandEntity
import adb.captain.data.local.entity.MacroEntity

@Database(entities = [CommandEntity::class, MacroEntity::class], version = 2, exportSchema = false)
abstract class CommanderDatabase : RoomDatabase() {
    abstract val commandDao: CommandDao
    abstract val macroDao: MacroDao
}
