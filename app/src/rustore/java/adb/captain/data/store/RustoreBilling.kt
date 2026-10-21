/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.data.store

import adb.captain.BuildConfig

/**
 * Billing configuration of the store build.
 *
 * The values come from the `rustore.*` properties in gradle.properties and can
 * be overridden on the command line with `-PrustoreConsoleApplicationId=...`.
 * They are placeholders until the app is registered in the store console.
 */
object RustoreBilling {
    val CONSOLE_APPLICATION_ID: String = BuildConfig.RUSTORE_CONSOLE_APPLICATION_ID

    val DEEPLINK_SCHEME: String = BuildConfig.RUSTORE_DEEPLINK_SCHEME

    val DEVELOPER_PAYLOAD: String = BuildConfig.RUSTORE_DEVELOPER_PAYLOAD

    val PRODUCT_IDS: List<String> = BuildConfig.RUSTORE_PRODUCT_IDS
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}
