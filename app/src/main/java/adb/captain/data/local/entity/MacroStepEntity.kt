/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import adb.captain.domain.model.StepCondition
import adb.captain.domain.model.StepFailure

/**
 * One command of a macro. Steps are kept in their own table so a macro can
 * grow conditions, waits and error handling without rewriting every row.
 */
@Entity(
    tableName = "macro_steps",
    foreignKeys = [
        ForeignKey(
            entity = MacroEntity::class,
            parentColumns = ["id"],
            childColumns = ["macroId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("macroId")]
)
data class MacroStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val macroId: Long,
    val position: Int,
    val command: String,
    val condition: String = StepCondition.ALWAYS.name,
    val conditionValue: String = "",
    val onError: String = StepFailure.STOP.name,
    val waitMs: Long = 0L
)
