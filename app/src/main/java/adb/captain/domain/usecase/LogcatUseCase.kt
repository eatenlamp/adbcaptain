/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.usecase

import adb.captain.domain.model.LogEntry
import adb.captain.domain.repository.AdbRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase для работы с логами (Logcat).
 */
class LogcatUseCase @Inject constructor(
    private val repository: AdbRepository
) {
    fun streamLogcat(level: String? = null, filter: String? = null): Flow<LogEntry> {
        return repository.streamLogcat(level, filter)
    }

    suspend fun clearLogcat() = repository.clearLogcat()
}
