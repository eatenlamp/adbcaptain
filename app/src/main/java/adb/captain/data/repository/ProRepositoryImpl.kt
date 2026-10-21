/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.data.repository

import adb.captain.BuildConfig
import adb.captain.data.local.SettingsManager
import adb.captain.data.store.StoreBilling
import adb.captain.data.store.StoreBillingEvent
import adb.captain.domain.model.ProFeature
import adb.captain.domain.model.ProProduct
import adb.captain.domain.model.ProStatus
import adb.captain.domain.repository.ProEvent
import adb.captain.domain.repository.ProRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the paid features state and drives the store purchase flow.
 */
@Singleton
class ProRepositoryImpl @Inject constructor(
    private val settings: SettingsManager,
    private val billing: StoreBilling
) : ProRepository {

    private val scope = CoroutineScope(SupervisorJob())

    private val unlocked = MutableStateFlow(false)

    override val status: StateFlow<ProStatus> = combine(
        unlocked,
        settings.proUnlocked
    ) { local, stored ->
        // A build without a store has nothing to sell, so nothing is limited.
        if (!BuildConfig.STORE_FEATURES_ENABLED) ProStatus.PRO
        else if (local || stored) ProStatus.PRO
        else ProStatus.FREE
    }.stateIn(scope, SharingStarted.Eagerly, ProStatus.FREE)

    override val products: Flow<List<ProProduct>> = billing.products

    private val _events = MutableSharedFlow<ProEvent>(extraBufferCapacity = 4)
    override val events: Flow<ProEvent> = _events.asSharedFlow()

    init {
        scope.launch {
            billing.events.collect { event ->
                when (event) {
                    is StoreBillingEvent.Purchased -> {
                        unlocked.value = true
                        settings.setProUnlocked(true)
                        _events.emit(ProEvent.Purchased(event.productId))
                    }

                    is StoreBillingEvent.Failed -> {
                        _events.emit(ProEvent.Failed(event.message))
                    }
                }
            }
        }
    }

    override fun isUnlocked(feature: ProFeature): Boolean = when (feature) {
        ProFeature.UNLIMITED_MACROS -> status.value.isPro
        ProFeature.TERMINAL_MARKDOWN_EXPORT -> status.value.isPro
    }

    override fun refresh() {
        billing.restore()
    }

    override fun purchase(productId: String) {
        if (!billing.isAvailable()) {
            scope.launch {
                _events.emit(ProEvent.Failed("The store app is not installed"))
            }
            return
        }
        billing.purchase(productId)
    }

    override fun restorePurchases() {
        billing.restore()
    }
}
