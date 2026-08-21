package adb.captain.data.repository

import adb.captain.data.local.SettingsManager
import adb.captain.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val settingsManager: SettingsManager
) : SettingsRepository {
    override fun isDarkTheme(): Flow<Boolean> = settingsManager.darkTheme
    override suspend fun setDarkTheme(enabled: Boolean) = settingsManager.setDarkTheme(enabled)

    override fun isAutoCompleteEnabled(): Flow<Boolean> = settingsManager.autoComplete
    override suspend fun setAutoCompleteEnabled(enabled: Boolean) = settingsManager.setAutoComplete(enabled)

    override fun shouldSaveHistory(): Flow<Boolean> = settingsManager.saveHistory
    override suspend fun setSaveHistory(enabled: Boolean) = settingsManager.setSaveHistory(enabled)

    override fun getLanguage(): Flow<String> = settingsManager.language
    override suspend fun setLanguage(lang: String) = settingsManager.setLanguage(lang)

    override fun hasSeenTutorial(): Flow<Boolean> = settingsManager.tutorialSeen
    override suspend fun setTutorialSeen(seen: Boolean) = settingsManager.setTutorialSeen(seen)
}
