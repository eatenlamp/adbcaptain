/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.repository

import adb.captain.domain.model.Command
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс для работы с историей команд.
 */
interface HistoryRepository {
    fun getHistory(): Flow<List<Command>>
    suspend fun addCommand(command: Command)
    suspend fun clearHistory()
    suspend fun deleteCommand(id: Long)
}
