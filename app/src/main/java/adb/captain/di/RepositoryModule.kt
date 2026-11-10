/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.di

import adb.captain.data.repository.AdbRepositoryImpl
import adb.captain.data.repository.HistoryRepositoryImpl
import adb.captain.data.repository.RemoteAdbRepositoryImpl
import adb.captain.data.repository.SettingsRepositoryImpl
import adb.captain.domain.repository.AdbRepository
import adb.captain.domain.repository.HistoryRepository
import adb.captain.domain.repository.RemoteAdbRepository
import adb.captain.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAdbRepository(impl: AdbRepositoryImpl): AdbRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindRemoteAdbRepository(impl: RemoteAdbRepositoryImpl): RemoteAdbRepository
}
