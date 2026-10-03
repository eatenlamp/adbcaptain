/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.model

/**
 * Модель подключенного устройства.
 */
data class Device(
    val serial: String,
    val model: String = "Unknown",
    val androidVersion: String = "",
    val apiLevel: Int = 0,
    val status: DeviceStatus = DeviceStatus.OFFLINE,
    val batteryLevel: Int = -1
)

enum class DeviceStatus {
    ONLINE, OFFLINE, UNAUTHORIZED, RECOVERY
}
