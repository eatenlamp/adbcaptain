package adb.captain.di

import adb.captain.data.repository.AdbRepositoryImpl
import adb.captain.data.repository.HistoryRepositoryImpl
import adb.captain.data.repository.SettingsRepositoryImpl
import adb.captain.domain.repository.AdbRepository
import adb.captain.domain.repository.HistoryRepository
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
}
