/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import adb.captain.domain.model.ProFeature
import adb.captain.domain.model.ProProduct
import adb.captain.domain.model.ProStatus
import adb.captain.domain.repository.ProEvent
import adb.captain.domain.repository.ProRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State of the paid features, used by the store build. The FOSS build never
 * shows the section, but the view model stays harmless.
 */
@HiltViewModel
class ProViewModel @Inject constructor(
    private val repository: ProRepository
) : ViewModel() {

    val status: StateFlow<ProStatus> = repository.status
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProStatus.FREE)

    val products: StateFlow<List<ProProduct>> = repository.products
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        repository.refresh()
        viewModelScope.launch {
            repository.events.collect { event ->
                when (event) {
                    is ProEvent.Purchased -> _messages.emit("Purchased")
                    is ProEvent.Failed -> _messages.emit(event.message)
                    is ProEvent.Message -> _messages.emit(event.text)
                }
            }
        }
    }

    fun purchase(productId: String) {
        repository.purchase(productId)
    }

    fun isUnlocked(feature: ProFeature): Boolean = repository.isUnlocked(feature)
}
