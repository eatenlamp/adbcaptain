/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс для управления настройками приложения.
 */
interface SettingsRepository {
    fun isDarkTheme(): Flow<Boolean>
    suspend fun setDarkTheme(enabled: Boolean)

    fun getAppTheme(): Flow<String>
    suspend fun setAppTheme(name: String)

    fun isAutoCompleteEnabled(): Flow<Boolean>
    suspend fun setAutoCompleteEnabled(enabled: Boolean)

    fun shouldSaveHistory(): Flow<Boolean>
    suspend fun setSaveHistory(enabled: Boolean)

    fun getLanguage(): Flow<String>
    suspend fun setLanguage(lang: String)

    fun hasSeenTutorial(): Flow<Boolean>
    suspend fun setTutorialSeen(seen: Boolean)

    fun getFavoriteCommands(): Flow<List<String>>
    suspend fun setFavoriteCommands(commands: List<String>)

    fun getLastScreenshotUri(): Flow<String>
    suspend fun setLastScreenshotUri(uri: String)

    fun getRecordQuality(): Flow<String>
    suspend fun setRecordQuality(quality: String)

    fun getRecordMaxFps(): Flow<Boolean>
    suspend fun setRecordMaxFps(enabled: Boolean)

    fun getHideOverlayInCapture(): Flow<Boolean>
    suspend fun setHideOverlayInCapture(enabled: Boolean)
}
