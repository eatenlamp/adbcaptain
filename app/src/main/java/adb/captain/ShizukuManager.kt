package adb.captain

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

object ShizukuManager {
    const val REQUEST_CODE_SHIZUKU = 1001

    private val _isRunning = MutableStateFlow(runningCheck())
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val binderListener = Shizuku.OnBinderReceivedListener { refresh() }
    private val deadListener = Shizuku.OnBinderDeadListener { refresh() }

    /**
     * Подписывается на события binder Shizuku: UI обновляется мгновенно,
     * без затратного polling каждые 2 секунды.
     */
    fun init() {
        try {
            // Sticky-вариант также сразу вызывает listener, если binder уже активен.
            Shizuku.addBinderReceivedListenerSticky(binderListener)
        } catch (_: Throwable) {
        }
        try {
            Shizuku.addBinderDeadListener(deadListener)
        } catch (_: Throwable) {
        }
    }

    fun refresh() {
        val state = runningCheck()
        if (_isRunning.value != state) _isRunning.value = state
    }

    private fun runningCheck(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Exception) {
        false
    }

    fun isShizukuRunning(): Boolean = runningCheck()

    fun checkShizukuPermission(): Boolean {
        return if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            false
        } else {
            when (Shizuku.checkSelfPermission()) {
                PackageManager.PERMISSION_GRANTED -> true
                else -> false
            }
        }
    }

    fun requestShizukuPermission(activity: Activity) {
        Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
    }

    /**
     * Открывает приложение Shizuku или его официальную страницу, если оно не установлено.
     */
    fun openShizukuApp(context: Context) {
        val packageName = "moe.shizuku.privileged.api"
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            context.startActivity(intent)
        } else {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/"))
                browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(browserIntent)
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}