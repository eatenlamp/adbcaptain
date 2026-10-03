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
import adb.captain.data.local.entity.CommandEntity

@Database(entities = [CommandEntity::class], version = 1, exportSchema = false)
abstract class CommanderDatabase : RoomDatabase() {
    abstract val commandDao: CommandDao
}
