/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.repository

import adb.captain.domain.model.ProFeature
import adb.captain.domain.model.ProProduct
import adb.captain.domain.model.ProStatus
import kotlinx.coroutines.flow.Flow

/**
 * Access to the paid features of the store build.
 */
interface ProRepository {
    /** Whether the paid features are unlocked right now. */
    val status: Flow<ProStatus>

    /** Products the store currently offers. */
    val products: Flow<List<ProProduct>>

    /** One off notifications about purchases and failures. */
    val events: Flow<ProEvent>

    /** Whether [feature] is available without a purchase. */
    fun isUnlocked(feature: ProFeature): Boolean

    /** Reloads the product list and the list of already owned purchases. */
    fun refresh()

    /** Starts the purchase flow for [productId]. */
    fun purchase(productId: String)

    /** Re-applies a purchase that was made earlier but not confirmed. */
    fun restorePurchases()
}

sealed interface ProEvent {
    /** The purchase went through and the paid features are now unlocked. */
    data class Purchased(val productId: String) : ProEvent

    /** The purchase could not be completed. */
    data class Failed(val message: String) : ProEvent

    /** Something worth telling the user, but not an error. */
    data class Message(val text: String) : ProEvent
}
