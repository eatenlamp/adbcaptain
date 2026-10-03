/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.model

import java.util.Date

/**
 * Модель команды ADB.
 */
data class Command(
    val id: Long = 0,
    val text: String,
    val timestamp: Date = Date(),
    val isSuccess: Boolean = true,
    val output: String = ""
)
