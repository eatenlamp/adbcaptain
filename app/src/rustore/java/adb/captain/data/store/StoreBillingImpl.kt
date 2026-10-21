/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.data.store

import android.content.Context
import android.content.Intent
import android.util.Log
import adb.captain.domain.model.ProProduct
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import ru.rustore.sdk.billingclient.RuStoreBillingClientFactory
import ru.rustore.sdk.billingclient.model.purchase.PaymentResult
import ru.rustore.sdk.billingclient.model.purchase.PurchaseState
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Purchase flow of the store build, backed by the billing client of the store
 * the app is published on.
 *
 * The console application id, the deeplink scheme and the product ids come
 * from the build configuration, see the `rustoreBilling` block in the build
 * script and the `rustore.*` properties in gradle.properties.
 */
@Singleton
class StoreBillingImpl @Inject constructor(
    @ApplicationContext context: Context
) : StoreBilling {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client: RuStoreBillingClient = RuStoreBillingClientFactory.create(
        context = context,
        consoleApplicationId = RustoreBilling.CONSOLE_APPLICATION_ID,
        deeplinkScheme = RustoreBilling.DEEPLINK_SCHEME,
        externalPaymentLoggerFactory = { StorePaymentLogger() },
        debugLogs = false
    )

    private val _products = MutableStateFlow<List<ProProduct>>(emptyList())
    override val products: Flow<List<ProProduct>> = _products.asStateFlow()

    private val _events = MutableSharedFlow<StoreBillingEvent>(extraBufferCapacity = 4)
    override val events: Flow<StoreBillingEvent> = _events.asSharedFlow()

    private val available = AtomicBoolean(false)

    init {
        RuStoreBillingClient.checkPurchasesAvailability()
            .addOnSuccessListener { available.set(true) }
            .addOnFailureListener { available.set(false) }
        loadProducts()
        restore()
    }

    override fun isAvailable(): Boolean = available.get()

    override fun purchase(productId: String) {
        client.purchases.purchaseProduct(
            productId = productId,
            developerPayload = RustoreBilling.DEVELOPER_PAYLOAD
        )
            .addOnSuccessListener { result ->
                if (result is PaymentResult.Success) {
                    confirm(result.purchaseId, RustoreBilling.DEVELOPER_PAYLOAD)
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Purchase failed", error)
                _events.tryEmit(
                    StoreBillingEvent.Failed(error.message ?: error.javaClass.simpleName)
                )
            }
    }

    override fun restore() {
        scope.launch {
            runCatching {
                val purchases = client.purchases.getPurchases().await()
                purchases.filter { it.purchaseState == PurchaseState.PAID }
                    .forEach { purchase ->
                        val purchaseId = purchase.purchaseId ?: return@forEach
                        client.purchases.confirmPurchase(purchaseId).await()
                        _events.tryEmit(StoreBillingEvent.Purchased(purchaseId))
                    }
            }.onFailure { Log.w(TAG, "Unable to restore purchases", it) }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        client.onNewIntent(intent)
    }

    private fun loadProducts() {
        scope.launch {
            runCatching {
                val products = client.products.getProducts(RustoreBilling.PRODUCT_IDS).await()
                products.map { product ->
                    ProProduct(
                        id = product.productId,
                        title = product.title,
                        description = "",
                        formattedPrice = product.priceLabel
                    )
                }
            }.onSuccess { _products.value = it }
                .onFailure { Log.w(TAG, "Unable to load products", it) }
        }
    }

    private fun confirm(purchaseId: String, developerPayload: String) {
        client.purchases.confirmPurchase(purchaseId, developerPayload)
            .addOnSuccessListener {
                _events.tryEmit(StoreBillingEvent.Purchased(purchaseId))
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Unable to confirm purchase", error)
                _events.tryEmit(
                    StoreBillingEvent.Failed(error.message ?: error.javaClass.simpleName)
                )
            }
    }

    private companion object {
        const val TAG = "StoreBilling"
    }
}
