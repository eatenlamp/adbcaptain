/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.model

/**
 * Сохранённое устройство для подключения по ADB over Wi-Fi.
 */
data class SavedAdbDevice(
    val name: String,
    val host: String,
    val port: Int,
    val autoReconnect: Boolean = false
) {
    val endpoint: String get() = "$host:$port"
}
