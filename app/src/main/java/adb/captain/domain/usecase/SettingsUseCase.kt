package adb.captain.domain.usecase

import adb.captain.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * UseCase для работы с настройками.
 */
class SettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    fun isDarkTheme(): Flow<Boolean> = repository.isDarkTheme()
    suspend fun setDarkTheme(enabled: Boolean) = repository.setDarkTheme(enabled)

    fun isAutoCompleteEnabled(): Flow<Boolean> = repository.isAutoCompleteEnabled()
    suspend fun setAutoCompleteEnabled(enabled: Boolean) = repository.setAutoCompleteEnabled(enabled)

    fun getLanguage(): Flow<String> = repository.getLanguage()
    suspend fun setLanguage(lang: String) = repository.setLanguage(lang)

    fun hasSeenTutorial(): Flow<Boolean> = repository.hasSeenTutorial()
    suspend fun hasSeenTutorialNow(): Boolean = repository.hasSeenTutorial().first()
    suspend fun setTutorialSeen(seen: Boolean) = repository.setTutorialSeen(seen)
}
