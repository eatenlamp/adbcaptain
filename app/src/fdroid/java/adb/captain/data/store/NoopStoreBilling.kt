/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.data.store

import android.content.Intent
import adb.captain.domain.model.ProProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The FOSS build has no store to buy from, so there is nothing to offer and
 * nothing to pay for. Every paid feature stays free here.
 */
@Singleton
class NoopStoreBilling @Inject constructor() : StoreBilling {

    override val products: Flow<List<ProProduct>> = emptyFlow()

    override val events: Flow<StoreBillingEvent> = emptyFlow()

    override fun isAvailable(): Boolean = false

    override fun purchase(productId: String) = Unit

    override fun restore() = Unit

    override fun onNewIntent(intent: Intent?) = Unit
}
