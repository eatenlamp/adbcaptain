package adb.captain.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс для управления настройками приложения.
 */
interface SettingsRepository {
    fun isDarkTheme(): Flow<Boolean>
    suspend fun setDarkTheme(enabled: Boolean)

    fun isAutoCompleteEnabled(): Flow<Boolean>
    suspend fun setAutoCompleteEnabled(enabled: Boolean)

    fun shouldSaveHistory(): Flow<Boolean>
    suspend fun setSaveHistory(enabled: Boolean)

    fun getLanguage(): Flow<String>
    suspend fun setLanguage(lang: String)

    fun hasSeenTutorial(): Flow<Boolean>
    suspend fun setTutorialSeen(seen: Boolean)
}
