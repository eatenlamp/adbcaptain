package adb.captain

import android.app.Application
import adb.captain.domain.repository.RemoteAdbRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Основной класс приложения для инициализации Hilt и других глобальных компонентов.
 */
@HiltAndroidApp
class CommanderApp : Application() {

    @Inject
    lateinit var remoteAdbRepository: RemoteAdbRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        ShizukuManager.init()
        applicationScope.launch {
            runCatching { remoteAdbRepository.autoReconnect() }
        }
    }
}
