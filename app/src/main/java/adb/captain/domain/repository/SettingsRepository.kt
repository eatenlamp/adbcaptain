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
}
