/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val APP_THEME = stringPreferencesKey("app_theme")
        val AUTO_COMPLETE = booleanPreferencesKey("auto_complete")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val LANGUAGE = stringPreferencesKey("language")
        val TUTORIAL_SEEN = booleanPreferencesKey("tutorial_seen")
        val SAVED_ADB_DEVICES = stringPreferencesKey("saved_adb_devices")
        val FAVORITE_COMMANDS = stringPreferencesKey("favorite_commands")
        val LAST_SCREENSHOT_URI = stringPreferencesKey("last_screenshot_uri")
        val RECORD_QUALITY = stringPreferencesKey("record_quality")
        val RECORD_MAX_FPS = booleanPreferencesKey("record_max_fps")
        val HIDE_OVERLAY_IN_CAPTURE = booleanPreferencesKey("hide_overlay_in_capture")
    }

    val tutorialSeen: Flow<Boolean> = context.dataStore.data.map { it[TUTORIAL_SEEN] ?: false }
    suspend fun setTutorialSeen(seen: Boolean = true) {
        context.dataStore.edit { it[TUTORIAL_SEEN] = seen }
    }

    val darkTheme: Flow<Boolean> = context.dataStore.data.map { it[DARK_THEME] ?: true }
    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[DARK_THEME] = enabled }
    }

    val appTheme: Flow<String> = context.dataStore.data.map { it[APP_THEME] ?: "TealNavy" }
    suspend fun setAppTheme(name: String) {
        context.dataStore.edit { it[APP_THEME] = name }
    }

    val autoComplete: Flow<Boolean> = context.dataStore.data.map { it[AUTO_COMPLETE] ?: true }
    suspend fun setAutoComplete(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_COMPLETE] = enabled }
    }

    val saveHistory: Flow<Boolean> = context.dataStore.data.map { it[SAVE_HISTORY] ?: true }
    suspend fun setSaveHistory(enabled: Boolean) {
        context.dataStore.edit { it[SAVE_HISTORY] = enabled }
    }

    val language: Flow<String> = context.dataStore.data.map { it[LANGUAGE] ?: "en" }
    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[LANGUAGE] = lang }
    }

    val savedAdbDevicesRaw: Flow<String> = context.dataStore.data.map { it[SAVED_ADB_DEVICES] ?: "" }
    suspend fun setSavedAdbDevicesRaw(value: String) {
        context.dataStore.edit { it[SAVED_ADB_DEVICES] = value }
    }

    val favoriteCommandsRaw: Flow<String> = context.dataStore.data.map { it[FAVORITE_COMMANDS] ?: "" }
    suspend fun setFavoriteCommandsRaw(value: String) {
        context.dataStore.edit { it[FAVORITE_COMMANDS] = value }
    }

    val lastScreenshotUri: Flow<String> = context.dataStore.data.map { it[LAST_SCREENSHOT_URI] ?: "" }
    suspend fun setLastScreenshotUri(value: String) {
        context.dataStore.edit { it[LAST_SCREENSHOT_URI] = value }
    }

    val recordQuality: Flow<String> = context.dataStore.data.map { it[RECORD_QUALITY] ?: "native" }
    suspend fun setRecordQuality(value: String) {
        context.dataStore.edit { it[RECORD_QUALITY] = value }
    }

    val recordMaxFps: Flow<Boolean> = context.dataStore.data.map { it[RECORD_MAX_FPS] ?: true }
    suspend fun setRecordMaxFps(value: Boolean) {
        context.dataStore.edit { it[RECORD_MAX_FPS] = value }
    }

    val hideOverlayInCapture: Flow<Boolean> = context.dataStore.data.map { it[HIDE_OVERLAY_IN_CAPTURE] ?: true }
    suspend fun setHideOverlayInCapture(value: Boolean) {
        context.dataStore.edit { it[HIDE_OVERLAY_IN_CAPTURE] = value }
    }
}
