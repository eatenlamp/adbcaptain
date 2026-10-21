/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.data.store

import android.util.Log
import ru.rustore.sdk.billingclient.provider.logger.ExternalPaymentLogger

/**
 * Routes the payment log of the store client into the app log.
 */
class StorePaymentLogger : ExternalPaymentLogger {
    override fun d(e: Throwable?, message: () -> String) {
        Log.d(TAG, message.invoke(), e)
    }

    override fun e(e: Throwable?, message: () -> String) {
        Log.e(TAG, message.invoke(), e)
    }

    override fun i(e: Throwable?, message: () -> String) {
        Log.i(TAG, message.invoke(), e)
    }

    override fun v(e: Throwable?, message: () -> String) {
        Log.v(TAG, message.invoke(), e)
    }

    override fun w(e: Throwable?, message: () -> String) {
        Log.w(TAG, message.invoke(), e)
    }

    private companion object {
        const val TAG = "StoreBilling"
    }
}
