package adb.captain

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import rikka.shizuku.Shizuku

object ShizukuManager {
    const val REQUEST_CODE_SHIZUKU = 1001

    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

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
