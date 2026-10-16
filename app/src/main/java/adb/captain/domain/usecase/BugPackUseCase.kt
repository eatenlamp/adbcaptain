/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.usecase

import adb.captain.domain.repository.AdbRepository
import javax.inject.Inject

/**
 * Collects a diagnostic snapshot of the device into named text entries,
 * ready to be zipped into a bug report.
 */
class BugPackUseCase @Inject constructor(
    private val repository: AdbRepository
) {
    /** What a [collect] call should gather. */
    data class Options(
        val systemInfo: Boolean = true,
        val packageInfo: Boolean = true,
        val packageName: String = "",
        val activityInfo: Boolean = true,
        val logcatMain: Boolean = true,
        val logcatEvents: Boolean = true,
        val processes: Boolean = true,
        val packagesList: Boolean = true,
        val battery: Boolean = true,
        val memory: Boolean = true,
        val cpu: Boolean = true
    )

    /** filename -> content */
    suspend fun collect(options: Options): Map<String, String> {
        val out = LinkedHashMap<String, String>()

        if (options.systemInfo) {
            out["system_info.txt"] = safe { repository.executeCommand("getprop") }
        }
        if (options.packageInfo && options.packageName.isNotBlank()) {
            out["package_${options.packageName}.txt"] =
                safe { repository.executeCommand("dumpsys package ${options.packageName}") }
        }
        if (options.activityInfo) {
            out["activity.txt"] = safe { repository.executeCommand("dumpsys activity activities") }
        }
        if (options.logcatMain) {
            out["logcat_main.txt"] = safe { repository.executeCommand("logcat -d -t 500") }
        }
        if (options.logcatEvents) {
            out["logcat_events.txt"] = safe { repository.executeCommand("logcat -b events -d -t 200") }
        }
        if (options.processes) {
            out["processes.txt"] = safe { repository.executeCommand("ps -A -o PID,CPU,RSS,NAME") }
        }
        if (options.packagesList) {
            out["packages.txt"] = safe { repository.executeCommand("pm list packages -f") }
        }
        if (options.battery) {
            out["battery.txt"] = safe { repository.executeCommand("dumpsys battery") }
        }
        if (options.memory) {
            out["memory.txt"] = safe { repository.executeCommand("cat /proc/meminfo") }
        }
        if (options.cpu) {
            out["cpu.txt"] = safe { repository.executeCommand("cat /proc/stat") }
        }

        return out
    }

    private inline fun safe(block: () -> String): String =
        runCatching { block() }.getOrElse { "Error: ${it.message}" }
}
