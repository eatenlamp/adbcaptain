/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import adb.captain.data.local.entity.MacroEntity
import adb.captain.data.local.entity.MacroStepEntity
import adb.captain.data.local.entity.MacroWithSteps
import kotlinx.coroutines.flow.Flow

@Dao
interface MacroDao {
    @Transaction
    @Query("SELECT * FROM macros ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<MacroWithSteps>>

    @Transaction
    @Query("SELECT * FROM macros ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<MacroWithSteps>

    @Transaction
    @Query("SELECT * FROM macros WHERE id = :id")
    suspend fun getById(id: Long): MacroWithSteps?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacro(macro: MacroEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<MacroStepEntity>)

    @Query("DELETE FROM macro_steps WHERE macroId = :macroId")
    suspend fun deleteSteps(macroId: Long)

    @Query("DELETE FROM macros WHERE id = :id")
    suspend fun deleteById(id: Long)
}
