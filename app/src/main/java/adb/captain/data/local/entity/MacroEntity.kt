/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A saved command workflow. Commands are stored newline separated so a macro
 * stays readable when exported inside a bug pack.
 */
@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val commands: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun commandList(): List<String> =
        commands.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
}