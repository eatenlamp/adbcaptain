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
        val AUTO_COMPLETE = booleanPreferencesKey("auto_complete")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val LANGUAGE = stringPreferencesKey("language")
        val TUTORIAL_SEEN = booleanPreferencesKey("tutorial_seen")
    }

    val tutorialSeen: Flow<Boolean> = context.dataStore.data.map { it[TUTORIAL_SEEN] ?: false }
    suspend fun setTutorialSeen(seen: Boolean = true) {
        context.dataStore.edit { it[TUTORIAL_SEEN] = seen }
    }

    val darkTheme: Flow<Boolean> = context.dataStore.data.map { it[DARK_THEME] ?: true }
    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[DARK_THEME] = enabled }
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
}
