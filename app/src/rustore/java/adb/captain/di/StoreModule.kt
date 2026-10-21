/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.di

import adb.captain.data.store.StoreBilling
import adb.captain.data.store.StoreBillingImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The store build talks to the billing client of the store it is published on.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class StoreModule {

    @Binds
    @Singleton
    abstract fun bindStoreBilling(impl: StoreBillingImpl): StoreBilling
}
