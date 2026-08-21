package adb.captain.di

import android.content.Context
import androidx.room.Room
import adb.captain.data.local.CommanderDatabase
import adb.captain.data.local.SettingsManager
import adb.captain.data.local.dao.CommandDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CommanderDatabase {
        return Room.databaseBuilder(
            context,
            CommanderDatabase::class.java,
            "commander_db"
        ).build()
    }

    @Provides
    fun provideCommandDao(db: CommanderDatabase): CommandDao {
        return db.commandDao
    }

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }
}
