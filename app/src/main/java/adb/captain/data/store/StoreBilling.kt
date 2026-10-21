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

/**
 * Store specific part of the purchase flow.
 *
 * The FOSS build has no store to talk to, so its implementation does nothing
 * and reports that purchases are unavailable. The store build talks to the
 * billing client of the store it is published on.
 */
interface StoreBilling {
    /** Products the store offers. */
    val products: Flow<List<ProProduct>>

    /** Purchase results worth reporting. */
    val events: Flow<StoreBillingEvent>

    /** Whether the store app is present and able to handle purchases. */
    fun isAvailable(): Boolean

    /** Starts the purchase flow for [productId]. */
    fun purchase(productId: String)

    /** Looks for purchases that were paid for but never confirmed. */
    fun restore()

    /** Forwards the intent that carries the result of a purchase. */
    fun onNewIntent(intent: Intent?)
}

sealed interface StoreBillingEvent {
    data class Purchased(val productId: String) : StoreBillingEvent
    data class Failed(val message: String) : StoreBillingEvent
}
