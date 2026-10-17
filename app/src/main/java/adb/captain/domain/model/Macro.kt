/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.model

/** A named command workflow the user can run in one tap. */
data class Macro(
    val id: Long = 0,
    val name: String,
    val commands: List<String>,
    val createdAt: Long = 0L
)