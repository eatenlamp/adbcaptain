package adb.captain.data.repository

import adb.captain.data.local.SettingsManager
import adb.captain.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val settingsManager: SettingsManager
) : SettingsRepository {
    override fun isDarkTheme(): Flow<Boolean> = settingsManager.darkTheme
    override suspend fun setDarkTheme(enabled: Boolean) = settingsManager.setDarkTheme(enabled)

    override fun getAppTheme(): Flow<String> = settingsManager.appTheme
    override suspend fun setAppTheme(name: String) = settingsManager.setAppTheme(name)

    override fun isAutoCompleteEnabled(): Flow<Boolean> = settingsManager.autoComplete
    override suspend fun setAutoCompleteEnabled(enabled: Boolean) = settingsManager.setAutoComplete(enabled)

    override fun shouldSaveHistory(): Flow<Boolean> = settingsManager.saveHistory
    override suspend fun setSaveHistory(enabled: Boolean) = settingsManager.setSaveHistory(enabled)

    override fun getLanguage(): Flow<String> = settingsManager.language
    override suspend fun setLanguage(lang: String) = settingsManager.setLanguage(lang)

    override fun hasSeenTutorial(): Flow<Boolean> = settingsManager.tutorialSeen
    override suspend fun setTutorialSeen(seen: Boolean) = settingsManager.setTutorialSeen(seen)

    override fun getFavoriteCommands(): Flow<List<String>> =
        settingsManager.favoriteCommandsRaw.map { raw ->
            raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        }

    override suspend fun setFavoriteCommands(commands: List<String>) =
        settingsManager.setFavoriteCommandsRaw(commands.joinToString("\n"))
}
