package adb.captain.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import adb.captain.domain.usecase.SettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val useCase: SettingsUseCase
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = useCase.isDarkTheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isAutoCompleteEnabled: StateFlow<Boolean> = useCase.isAutoCompleteEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val language: StateFlow<String> = useCase.getLanguage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { useCase.setDarkTheme(enabled) }
    }

    fun setAutoCompleteEnabled(enabled: Boolean) {
        viewModelScope.launch { useCase.setAutoCompleteEnabled(enabled) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { useCase.setLanguage(lang) }
    }

    fun markTutorialSeen() {
        viewModelScope.launch { useCase.setTutorialSeen(true) }
    }

    suspend fun hasSeenTutorialNow(): Boolean = useCase.hasSeenTutorialNow()
}
