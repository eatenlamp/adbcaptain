package adb.captain.domain.usecase

import adb.captain.domain.model.AppDetails
import adb.captain.domain.model.AppInfo
import adb.captain.domain.repository.AdbRepository
import javax.inject.Inject

/**
 * UseCase для работы со списком приложений.
 */
class AppUseCase @Inject constructor(
    private val repository: AdbRepository
) {
    suspend fun getApps(system: Boolean? = null): List<AppInfo> = repository.getApps(system)
    suspend fun getAppDetails(packageName: String): AppDetails? = repository.getAppDetails(packageName)
    suspend fun forceStop(packageName: String) = repository.forceStopApp(packageName)
    suspend fun clearData(packageName: String) = repository.clearAppData(packageName)
    suspend fun uninstall(packageName: String) = repository.uninstallApp(packageName)
    suspend fun toggle(packageName: String, enable: Boolean) = repository.toggleApp(packageName, enable)
    suspend fun launch(packageName: String, activity: String? = null): String = repository.launchApp(packageName, activity)
    suspend fun exportApk(packageName: String): String = repository.exportApk(packageName)
}
