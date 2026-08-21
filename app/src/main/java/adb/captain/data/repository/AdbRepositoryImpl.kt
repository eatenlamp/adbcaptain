package adb.captain.data.repository

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import adb.captain.domain.model.*
import adb.captain.domain.repository.AdbRepository
import adb.captain.domain.repository.BatteryDetails
import adb.captain.domain.repository.CpuInfo
import adb.captain.domain.repository.MemoryInfo
import adb.captain.domain.repository.ProcessInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Реализация репозитория ADB с использованием Shizuku.
 */
class AdbRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AdbRepository {

    override suspend fun executeCommand(command: String): String = executeCommand(command, timeoutSeconds = 5)

    private suspend fun executeCommand(command: String, timeoutSeconds: Long): String = withContext(Dispatchers.IO) {
        try {
            val process = startShizukuProcess(command)
            
            coroutineScope {
                val out = async { process.inputStream.bufferedReader().use { it.readText() } }
                val err = async { process.errorStream.bufferedReader().use { it.readText() } }
                
                val result = withTimeoutOrNull(timeoutSeconds * 1000) {
                    withContext(Dispatchers.IO) { process.waitFor() }
                    val errorText = err.await()
                    if (errorText.isNotEmpty()) "Error: $errorText" else out.await()
                }
                
                if (result == null) {
                    process.destroy()
                    "Error: Timeout"
                } else {
                    result
                }
            }
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
    }

    override fun executeStreamingCommand(command: String): Flow<String> = flow {
        val process = startShizukuProcess(command)
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { emit(it) }
        }
        process.waitFor()
    }.flowOn(Dispatchers.IO)

    override suspend fun getDevices(): List<Device> = withContext(Dispatchers.IO) {
        try {
            val model = executeCommand("getprop ro.product.model").trim()
            val version = executeCommand("getprop ro.build.version.release").trim()
            val api = executeCommand("getprop ro.build.version.sdk").trim().toIntOrNull() ?: 0
            val battery = getBatteryLevel()
            
            listOf(Device(
                serial = executeCommand("getprop ro.serialno").trim().ifBlank { "Local Device" },
                model = if (model.isBlank()) "Android Device" else model,
                androidVersion = version,
                apiLevel = api,
                status = DeviceStatus.ONLINE,
                batteryLevel = battery
            ))
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getApps(system: Boolean?): List<AppInfo> = withContext(Dispatchers.IO) {
        val filter = when (system) {
            true -> "-s"
            false -> "-3"
            else -> ""
        }
        
        try {
            val output = executeCommand("pm list packages $filter")
            if (output.startsWith("Exception") || output.startsWith("Error")) return@withContext emptyList()
            
            val pm = context.packageManager
            output.lines()
                .filter { it.isNotBlank() && it.startsWith("package:") }
                .map { line ->
                    val packageName = line.substringAfter("package:").trim()
                    
                    // Try to get real label and version using local PackageManager
                    var label = packageName
                    var version = "Unknown"
                    var isSystemApp = false
                    var isEnabled = true
                    
                    try {
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        label = pm.getApplicationLabel(appInfo).toString()
                        val packageInfo = pm.getPackageInfo(packageName, 0)
                        version = packageInfo.versionName ?: "Unknown"
                        isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                        val enabledState = pm.getApplicationEnabledSetting(packageName)
                        isEnabled = enabledState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED &&
                            enabledState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER &&
                            enabledState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
                    } catch (e: Exception) {
                        // Fallback to heuristic label
                        label = packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                    }
                    
                    AppInfo(
                        packageName = packageName, 
                        label = label, 
                        versionName = version,
                        isSystem = isSystemApp,
                        enabled = isEnabled,
                        safetyStatus = categorizePackage(packageName)
                    )
                }.sortedBy { it.label.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun isLikelySystem(pkg: String): Boolean {
        return pkg.startsWith("android") || pkg.startsWith("com.android") || 
               pkg.startsWith("com.google.android.overlay") || pkg.contains(".miui.") ||
               pkg.contains(".samsung.") || pkg.contains(".huawei.")
    }

    private fun categorizePackage(pkg: String): BloatwareStatus {
        val safe = listOf(
            "com.miui.msa.global", "com.miui.analytics", "com.miui.daemon", "com.xiaomi.joyose", 
            "com.miui.systemAdSolution", "com.miui.player", "com.miui.videoplayer", "com.android.browser",
            "com.miui.notes", "com.miui.weather2", "com.miui.yellowpage", "com.miui.cleanmaster",
            "com.miui.compass", "com.miui.calculator", "com.miui.android.fashiongallery",
            "com.samsung.android.bixby.agent", "com.samsung.android.app.spage", "com.sec.android.app.shealth",
            "com.huawei.appmarket", "com.huawei.music", "com.huawei.video", "com.heytap.market", "com.heytap.themestore"
        )
        val critical = listOf(
            "com.miui.securitycenter", "com.miui.home", "com.xiaomi.finddevice", "com.android.updater",
            "com.sec.android.app.launcher", "com.huawei.android.launcher", "com.coloros.safecenter", "com.android.settings"
        )
        val needed = listOf(
            "com.google.android.apps.photos", "com.google.android.youtube", "com.microsoft.skydrive", "com.facebook.system"
        )

        return when {
            pkg in safe -> BloatwareStatus.SAFE_TO_DELETE
            pkg in critical -> BloatwareStatus.NOT_SAFE_TO_DELETE
            pkg in needed -> BloatwareStatus.DELETE_IF_NEEDED
            else -> BloatwareStatus.UNKNOWN
        }
    }

    override fun streamLogcat(level: String?, filter: String?): Flow<LogEntry> = flow {
        val cmd = buildString {
            append("logcat -v time")
            if (level != null) append(" *:$level")
            if (filter != null) append(" | grep \"$filter\"")
        }
        val process = startShizukuProcess(cmd)
        process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                parseLogcatLine(line)?.let { emit(it) }
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun forceStopApp(packageName: String) { executeCommand("am force-stop $packageName") }
    override suspend fun clearAppData(packageName: String) { executeCommand("pm clear $packageName") }
    override suspend fun uninstallApp(packageName: String) { executeCommand("pm uninstall --user 0 $packageName") }
    override suspend fun toggleApp(packageName: String, enable: Boolean) {
        val cmd = if (enable) "pm enable" else "pm disable-user"
        executeCommand("$cmd $packageName")
    }

    override suspend fun launchApp(packageName: String, activity: String?): String {
        val target = activity?.let {
            if (it.startsWith(".")) "$packageName$it" else if (it.contains('/')) "$packageName/${it.substringAfter('/')}" else "$packageName/$it"
        }
        val cmd = if (target != null) {
            "am start -n $target"
        } else {
            "monkey -p $packageName -c android.intent.category.LAUNCHER 1"
        }
        return executeCommand(cmd)
    }

    override suspend fun exportApk(packageName: String): String = withContext(Dispatchers.IO) {
        try {
            val out = executeCommand("pm path $packageName")
            val apkPath = out.lines().firstOrNull { it.startsWith("package:") }
                ?.substringAfter("package:")?.trim()
            if (apkPath.isNullOrBlank()) return@withContext "Error: APK not found"
            val dir = "/sdcard/DCIM/ADBCaptain/exports"
            executeCommand("mkdir -p $dir")
            val dest = "$dir/${packageName}.apk"
            val result = executeCommand("cp \"$apkPath\" \"$dest\"")
            if (result.startsWith("Error") || result.startsWith("Exception")) {
                "Error: $result"
            } else {
                dest
            }
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
    }

    override suspend fun listFiles(path: String): List<FileEntry> = withContext(Dispatchers.IO) {
        val output = executeCommand("ls -la \"$path\"")
        if (output.startsWith("Error") || output.startsWith("Exception")) return@withContext emptyList()
        parseLsOutput(output, path)
    }

    override suspend fun deleteFile(path: String) {
        executeCommand("rm -rf \"${path.replace("\"", "")}\"")
    }

    override suspend fun createDirectory(path: String) {
        executeCommand("mkdir -p \"${path.replace("\"", "")}\"")
    }

    override suspend fun getShowTouches(): Boolean =
        executeCommand("settings get system show_touches").trim() == "1"

    override suspend fun setShowTouches(enabled: Boolean) {
        executeCommand("settings put system show_touches ${if (enabled) 1 else 0}")
    }

    override suspend fun getAnimationScale(): Float =
        executeCommand("settings get global window_animation_scale").trim().toFloatOrNull() ?: 1f

    override suspend fun setAnimationScale(scale: Float) {
        executeCommand("settings put global window_animation_scale $scale")
        executeCommand("settings put global transition_animation_scale $scale")
        executeCommand("settings put global animator_duration_scale $scale")
    }

    override suspend fun getUsbDebugging(): Boolean =
        executeCommand("settings get global adb_enabled").trim() == "1"

    override suspend fun setUsbDebugging(enabled: Boolean) {
        executeCommand("settings put global adb_enabled ${if (enabled) 1 else 0}")
    }

    override suspend fun getOemUnlock(): Boolean =
        executeCommand("settings get global oem_unlock_allowed").trim() == "1"

    override suspend fun setOemUnlock(enabled: Boolean) {
        executeCommand("settings put global oem_unlock_allowed ${if (enabled) 1 else 0}")
    }

    override suspend fun setWifiAdbEnabled(enabled: Boolean, port: Int) {
        executeCommand("settings put global adb_wifi_enabled ${if (enabled) 1 else 0}")
        if (enabled) {
            executeCommand("cmd connectivity wireless-tethering set-enabled adb true || svc wifi enable")
        }
    }

    override suspend fun getWifiAdbEnabled(): Boolean =
        executeCommand("settings get global adb_wifi_enabled").trim() == "1"

    override suspend fun takeScreenshot(deviceSerial: String): String = withContext(Dispatchers.IO) {
        val dir = "/sdcard/DCIM/ADBCaptain"
        executeCommand("mkdir -p $dir")
        val path = "$dir/screenshot_${System.currentTimeMillis()}.png"
        executeCommand("screencap -p $path")
        path
    }

    override suspend fun rebootDevice(deviceSerial: String) { executeCommand("reboot") }

    override suspend fun rebootToRecovery(deviceSerial: String) { executeCommand("reboot recovery") }

    override suspend fun rebootToBootloader(deviceSerial: String) { executeCommand("reboot bootloader") }

    override suspend fun sideloadApk(apkFile: File): String = withContext(Dispatchers.IO) {
        try {
            val remotePath = "/data/local/tmp/adbcaptain_${System.currentTimeMillis()}.apk"

            // Push the APK into a shell-writable location via stdin
            val pushProcess = startShizukuProcess("sh -c 'cat > $remotePath'")
            val pushErr = async { pushProcess.errorStream.bufferedReader().readText() }
            pushProcess.outputStream.use { stdin ->
                apkFile.inputStream().use { it.copyTo(stdin) }
            }
            pushProcess.waitFor()
            val pushError = pushErr.await()
            if (pushError.isNotBlank()) return@withContext "Error: $pushError"

            // Install the APK (long timeout - large APKs take time)
            val result = executeCommand("pm install -r -t $remotePath", timeoutSeconds = 120)

            // Cleanup
            try { startShizukuProcess("rm -f $remotePath").waitFor() } catch (e: Exception) {}

            result
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
    }

    override suspend fun installApkAtPath(remotePath: String): String =
        executeCommand("pm install -r -t \"${remotePath.replace("\"", "")}\"", timeoutSeconds = 120)

    override suspend fun startScreenRecording(): String = withContext(Dispatchers.IO) {
        val dir = "/sdcard/Movies/ADBCaptain"
        executeCommand("mkdir -p $dir")
        val path = "$dir/rec_${System.currentTimeMillis()}.mp4"
        executeCommand("nohup screenrecord --time-limit 180 $path >/dev/null 2>&1 &")
        path
    }

    override suspend fun stopScreenRecording() {
        executeCommand("pkill -INT -f screenrecord")
    }

    override suspend fun wakeDevice() { executeCommand("input keyevent KEYCODE_WAKEUP") }

    override suspend fun dismissKeyguard() { executeCommand("wm dismiss-keyguard") }

    override suspend fun setStayAwake(enabled: Boolean) {
        executeCommand("svc power stayon ${if (enabled) "true" else "false"}")
    }

    override suspend fun setWifiEnabled(enabled: Boolean) {
        val state = if (enabled) "enabled" else "disabled"
        executeCommand("cmd wifi set-wifi-enabled $state || svc wifi ${if (enabled) "enable" else "disable"}")
    }

    override suspend fun setBluetoothEnabled(enabled: Boolean) {
        val state = if (enabled) "enable" else "disable"
        executeCommand("cmd bluetooth_manager $state || svc bluetooth $state")
    }

    override suspend fun setAirplaneMode(enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        executeCommand("settings put global airplane_mode_on $value && am broadcast -a android.intent.action.AIRPLANE_MODE --ez state ${if (enabled) "true" else "false"}")
    }

    override suspend fun setMediaVolume(level: Int) {
        executeCommand("media volume --stream 3 --set $level || cmd media_session volume --stream 3 --set $level")
    }

    override suspend fun sendTextInput(text: String) {
        // Escape special characters and spaces for the `input text` command
        val escaped = text
            .replace("'", "")
            .replace("\"", "")
            .replace(";", "")
            .replace("&", "")
            .replace("|", "")
            .replace("`", "")
            .replace("\$", "")
            .replace(" ", "%s")
        executeCommand("input text \"$escaped\"")
    }

    override suspend fun openUrl(url: String) {
        val safe = url.replace("\"", "")
        executeCommand("am start -a android.intent.action.VIEW -d \"$safe\"")
    }

    override suspend fun runInputMacro(script: String): String = withContext(Dispatchers.IO) {
        try {
            val lines = script.lines().map { it.trim() }.filter { it.isNotEmpty() }
            var executed = 0
            for (line in lines) {
                when {
                    line.startsWith("delay ") -> {
                        val ms = line.substringAfter("delay ").toLongOrNull() ?: 500
                        Thread.sleep(ms)
                    }
                    line.startsWith("input ") || line.startsWith("keyevent ") -> {
                        val cmd = if (line.startsWith("keyevent ")) "input $line" else line
                        val res = executeCommand(cmd)
                        if (res.startsWith("Error") || res.startsWith("Exception")) {
                            return@withContext "Error at line $executed: $res"
                        }
                        executed++
                    }
                }
            }
            "Success: executed $executed commands"
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
    }

    override suspend fun getBatteryDetails(): BatteryDetails? = withContext(Dispatchers.IO) {
        try {
            val output = executeCommand("dumpsys battery")
            if (output.startsWith("Error") || output.startsWith("Exception")) return@withContext null
            val lines = output.lines()

            fun findValue(key: String): String =
                lines.find { it.trim().startsWith(key) }
                    ?.substringAfter(":")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() && it != "unknown" }
                    ?: ""

            val statusNum = findValue("status").toIntOrNull()
            val healthNum = findValue("health").toIntOrNull()
            val pluggedNum = when {
                lines.any { it.contains("AC powered") && it.contains("true") } -> "AC"
                lines.any { it.contains("Wireless powered") && it.contains("true") } -> "Wireless"
                lines.any { it.contains("USB powered") && it.contains("true") } -> "USB"
                else -> "Unplugged"
            }

            BatteryDetails(
                level = findValue("level").toIntOrNull() ?: 0,
                status = batteryStatus(statusNum),
                health = batteryHealth(healthNum),
                technology = findValue("technology"),
                temperature = (findValue("temperature").toIntOrNull() ?: 0) / 10,
                voltage = findValue("voltage").toIntOrNull() ?: 0,
                current = findValue("current now").toIntOrNull() ?: 0,
                capacity = findValue("capacity").toIntOrNull() ?: 0,
                plugged = pluggedNum,
                chargeCounter = findValue("charge counter").toIntOrNull()
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getCpuInfo(): CpuInfo? = withContext(Dispatchers.IO) {
        try {
            val stat1 = executeCommand("cat /proc/stat").lines()
            Thread.sleep(200)
            val stat2 = executeCommand("cat /proc/stat").lines()

            data class CpuTimes(val idle: Long, val total: Long)
            fun parseLine(line: String): CpuTimes? {
                val parts = line.split("\\s+".toRegex())
                if (parts.size < 5 || !parts[0].startsWith("cpu")) return null
                val nums = parts.drop(1).mapNotNull { it.toLongOrNull() }
                if (nums.size < 4) return null
                val idle = nums[3] + (nums.getOrNull(4) ?: 0)
                return CpuTimes(idle, nums.sum())
            }

            fun usageOf(l1: CpuTimes?, l2: CpuTimes?): Float {
                if (l1 == null || l2 == null) return 0f
                val totalDelta = (l2.total - l1.total).toFloat()
                if (totalDelta <= 0) return 0f
                val idleDelta = (l2.idle - l1.idle).toFloat()
                return ((totalDelta - idleDelta) / totalDelta * 100).coerceIn(0f, 100f)
            }

            val t1 = parseLine(stat1.firstOrNull { it.startsWith("cpu ") } ?: "")
            val t2 = parseLine(stat2.firstOrNull { it.startsWith("cpu ") } ?: "")

            val perCore = mutableListOf<Float>()
            val freqList = mutableListOf<Long>()
            val govList = mutableListOf<String>()
            val coreLines1 = stat1.filter { it.startsWith("cpu") && !it.startsWith("cpu ") }
            val coreLines2 = stat2.filter { it.startsWith("cpu") && !it.startsWith("cpu ") }
            for (i in coreLines1.indices) {
                if (i >= coreLines2.size) break
                val u = usageOf(parseLine(coreLines1[i]), parseLine(coreLines2[i]))
                perCore.add(u)
                val cpuDir = "/sys/devices/system/cpu/cpu$i/cpufreq"
                val freq = executeCommand("cat $cpuDir/scaling_cur_freq").trim().toLongOrNull() ?: 0
                freqList.add(if (freq > 0) freq / 1000 else 0)
                val gov = executeCommand("cat $cpuDir/scaling_governor").trim()
                govList.add(gov.ifBlank { "n/a" })
            }

            CpuInfo(
                totalUsage = usageOf(t1, t2),
                perCoreUsage = perCore,
                frequency = freqList,
                governor = govList
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getMemoryInfo(): MemoryInfo? = withContext(Dispatchers.IO) {
        try {
            val output = executeCommand("cat /proc/meminfo")
            if (output.startsWith("Error") || output.startsWith("Exception")) return@withContext null
            val lines = output.lines()
            fun kb(key: String): Long =
                lines.find { it.startsWith(key) }?.substringAfter(":")?.substringBefore(" kB")?.trim()?.toLongOrNull() ?: 0

            val total = kb("MemTotal") * 1024
            val avail = kb("MemAvailable") * 1024
            val free = kb("MemFree") * 1024
            val swapTotal = kb("SwapTotal") * 1024
            val swapFree = kb("SwapFree") * 1024

            MemoryInfo(
                totalMem = total,
                availableMem = avail,
                usedMem = (total - avail).coerceAtLeast(0),
                freeMem = free,
                swapTotal = swapTotal,
                swapFree = swapFree
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getTopProcesses(limit: Int): List<ProcessInfo> = withContext(Dispatchers.IO) {
        try {
            val output = executeCommand("ps -A -o PID,CPU,RSS,ARGS")
            if (output.startsWith("Error") || output.startsWith("Exception")) return@withContext emptyList()
            output.lines()
                .drop(1)
                .mapNotNull { line ->
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size < 4) return@mapNotNull null
                    val pid = parts[0].toIntOrNull() ?: return@mapNotNull null
                    val cpu = parts[1].toFloatOrNull() ?: 0f
                    val rss = parts[2].toLongOrNull() ?: 0L
                    val name = parts.drop(3).joinToString(" ").trim().ifBlank { parts[3] }
                    ProcessInfo(
                        pid = pid,
                        name = name,
                        cpuUsage = cpu,
                        memoryUsage = rss * 1024,
                        state = ""
                    )
                }
                .sortedByDescending { it.cpuUsage }
                .take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun setDemoMode(enabled: Boolean) {
        if (enabled) {
            executeCommand("settings put global sysui_demo_allowed 1")
            executeCommand("am broadcast -a com.android.systemui.demo -e command enter")
            executeCommand("am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200")
            executeCommand("am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false")
            executeCommand("am broadcast -a com.android.systemui.demo -e command network -e wifi show -e wifi level 4 -e mobile show -e mobile datatype lte")
        } else {
            executeCommand("am broadcast -a com.android.systemui.demo -e command exit")
            executeCommand("settings put global sysui_demo_allowed 0")
        }
    }

    override suspend fun getDemoMode(): Boolean =
        executeCommand("settings get global sysui_demo_allowed").trim() == "1"

    override suspend fun createDiagnosticReport(): String = withContext(Dispatchers.IO) {
        try {
            val dir = "/sdcard/DCIM/ADBCaptain/reports"
            executeCommand("mkdir -p $dir")
            val path = "$dir/report_${System.currentTimeMillis()}.txt"
            val sb = StringBuilder()
            sb.appendLine("ADB Captain Diagnostic Report")
            sb.appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
            sb.appendLine()
            sb.appendLine("=== Device Info ===")
            sb.appendLine(executeCommand("getprop ro.product.manufacturer").trim())
            sb.appendLine(executeCommand("getprop ro.product.model").trim())
            sb.appendLine(executeCommand("getprop ro.build.version.release").trim())
            sb.appendLine(executeCommand("getprop ro.build.version.sdk").trim())
            sb.appendLine()
            sb.appendLine("=== Battery ===")
            sb.appendLine(executeCommand("dumpsys battery"))
            sb.appendLine()
            sb.appendLine("=== Memory ===")
            sb.appendLine(executeCommand("cat /proc/meminfo"))
            sb.appendLine()
            sb.appendLine("=== Storage ===")
            sb.appendLine(executeCommand("df -h /data /sdcard"))
            sb.appendLine()
            sb.appendLine("=== Top Processes ===")
            sb.appendLine(executeCommand("ps -A -o PID,CPU,RSS,NAME"))
            sb.appendLine()
            sb.appendLine("=== Installed Packages (count) ===")
            sb.appendLine("total: ${executeCommand("pm list packages").lines().count { it.startsWith("package:") }}")
            sb.appendLine()
            sb.appendLine("=== System Properties (selected) ===")
            sb.appendLine(executeCommand("getprop | grep -E 'ro.build|ro.product|persist.sys.locale' | head -50"))

            val content = sb.toString().replace("'", "'\\''")
            val result = executeCommand("echo '$content' > \"$path\"")
            if (result.startsWith("Error") || result.startsWith("Exception")) {
                "Error: $result"
            } else {
                path
            }
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
    }

    override suspend fun getAppDetails(packageName: String): AppDetails? = withContext(Dispatchers.IO) {
        try {
            val output = executeCommand("dumpsys package $packageName")
            if (output.startsWith("Error") || output.startsWith("Exception")) return@withContext null

            val lines = output.lines()

            fun firstMatching(regex: Regex): String? =
                lines.firstNotNullOfOrNull { regex.find(it)?.groupValues?.getOrNull(1) }

            val versionName = firstMatching(Regex("versionName=([^\\s]+)")) ?: "Unknown"
            val versionCode = firstMatching(Regex("versionCode=(\\d+)"))?.toIntOrNull() ?: 0
            val targetSdk = firstMatching(Regex("targetSdk=(\\d+)"))?.toIntOrNull() ?: 0
            val minSdk = firstMatching(Regex("minSdk=(\\d+)"))?.toIntOrNull() ?: 0
            val codePath = firstMatching(Regex("codePath=([^\\s]+)")) ?: ""
            val dataDir = firstMatching(Regex("dataDir=([^\\s]+)")) ?: ""
            val uid = firstMatching(Regex("userId=(\\d+)"))?.toIntOrNull() ?: 0
            val firstInstall = firstMatching(Regex("firstInstallTime=([^\\s]+)"))?.toLongOrNull() ?: 0L
            val lastUpdate = firstMatching(Regex("lastUpdateTime=([^\\s]+)"))?.toLongOrNull() ?: 0L

            val flags = firstMatching(Regex("flags=\\[([^\\]]+)\\]")) ?: ""
            val isDebuggable = flags.contains("DEBUGGABLE")
            val isSystem = flags.contains("SYSTEM")
            val isExternal = flags.contains("EXTERNAL_STORAGE")

            val requestedPermissions = mutableListOf<String>()
            val grantedPermissions = mutableListOf<String>()
            var inRequested = false
            var inGranted = false
            for (line in lines) {
                val t = line.trim()
                when {
                    t.startsWith("requested permissions:") -> { inRequested = true; inGranted = false }
                    t.startsWith("install permissions:") || t.startsWith("runtime permissions:") -> { inRequested = false; inGranted = true }
                    t.isEmpty() || t.startsWith("Package [") -> { inRequested = false; inGranted = false }
                    inRequested && t.startsWith("android.permission.") -> requestedPermissions.add(t)
                    inGranted && t.startsWith("android.permission.") -> grantedPermissions.add(t.split(":").first())
                    inGranted && t.contains("granted=true") && t.contains("android.permission.") -> grantedPermissions.add(t.substringBefore(":").trim())
                }
            }

            val launchableActivities = mutableListOf<String>()
            var inActivityResolver = false
            for (line in lines) {
                if (line.startsWith("Activity Resolver Table:")) inActivityResolver = true
                if (inActivityResolver && line.contains("$packageName/")) {
                    val act = line.trim().substringAfter("$packageName/")
                    if (act.isNotBlank() && act !in launchableActivities) {
                        launchableActivities.add(act)
                    }
                }
            }
            if (launchableActivities.isEmpty()) {
                firstMatching(Regex("$packageName/([^\\s]+)"))?.let { launchableActivities.add(it) }
            }

            var label = packageName
            try {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                label = context.packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                // keep packageName
            }

            AppDetails(
                packageName = packageName,
                label = label,
                versionName = versionName,
                versionCode = versionCode,
                targetSdk = targetSdk,
                minSdk = minSdk,
                isSystem = isSystem,
                enabled = true,
                installTime = firstInstall,
                updateTime = lastUpdate,
                permissions = grantedPermissions,
                launchableActivities = launchableActivities,
                apkPath = codePath,
                dataDir = dataDir,
                uid = uid,
                targetSandboxVersion = 0,
                isDebuggable = isDebuggable,
                isExternal = isExternal,
                requestedPermissions = requestedPermissions,
                installPermissions = grantedPermissions
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun batteryStatus(status: Int?): String = when (status) {
        1 -> "Unknown"
        2 -> "Charging"
        3 -> "Discharging"
        4 -> "Not charging"
        5 -> "Full"
        else -> "Unknown"
    }

    private fun batteryHealth(health: Int?): String = when (health) {
        1 -> "Unknown"
        2 -> "Good"
        3 -> "Overheat"
        4 -> "Dead"
        5 -> "Over voltage"
        6 -> "Unspecified failure"
        7 -> "Cold"
        else -> "Unknown"
    }

    private fun startShizukuProcess(command: String): Process {
        val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        newProcessMethod.isAccessible = true
        return newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
    }

    private suspend fun getBatteryLevel(): Int {
        val output = executeCommand("dumpsys battery")
        return output.lines()
            .find { it.contains("level:") }
            ?.substringAfter("level:")
            ?.trim()
            ?.toIntOrNull() ?: -1
    }

    private fun parseLsOutput(output: String, parentPath: String): List<FileEntry> {
        val entries = mutableListOf<FileEntry>()
        for (line in output.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("total")) continue
            val fields = trimmed.split("\\s+".toRegex())
            if (fields.size < 9) continue
            val perms = fields[0]
            val isDir = perms.startsWith("d")
            val isLink = perms.startsWith("l")
            if (perms.startsWith(".")) continue
            val size = fields[4].toLongOrNull() ?: 0L
            val modified = "${fields[5]} ${fields[6]} ${fields[7]}"
            val name = fields.drop(8).joinToString(" ")
            if (name == "." || name.isEmpty()) continue
            if (name == "..") continue
            entries.add(
                FileEntry(
                    name = name,
                    path = if (parentPath == "/") "/$name" else "$parentPath/$name",
                    isDirectory = isDir || isLink,
                    size = size,
                    modified = modified
                )
            )
        }
        return entries.sortedWith(
            compareByDescending<FileEntry> { it.isDirectory }.thenBy { it.name.lowercase() }
        )
    }

    private fun parseLogcatLine(line: String): LogEntry? {
        // 07-31 11:48:34.530 D/Tag(PID): Message
        return try {
            val parts = line.split("\\s+".toRegex())
            if (parts.size < 5) return null
            val timestamp = "${parts[0]} ${parts[1]}"
            val levelTag = parts[2]
            val levelChar = levelTag.firstOrNull() ?: 'I'
            val tag = levelTag.substringAfter('/').substringBefore('(')
            val pid = levelTag.substringAfter('(').substringBefore(')').toIntOrNull() ?: 0
            val message = parts.drop(3).joinToString(" ")
            LogEntry(timestamp, LogLevel.fromChar(levelChar), tag, message, pid)
        } catch (e: Exception) {
            null
        }
    }
}
