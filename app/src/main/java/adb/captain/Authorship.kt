/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain

/**
 * Ownership marker for ADB Captain.
 *
 * The author name is assembled at runtime from code points instead of being a
 * plain literal, so a naive "replace all" of the visible name does not silently
 * strip every reference at once. This is attribution, not a security measure:
 * anything in source can still be edited by a determined person.
 */
object Authorship {
    private val namePoints = intArrayOf(101, 97, 116, 101, 110, 108, 97, 109, 112)

    val author: String get() = namePoints.map { it.toChar() }.joinToString("")

    const val PROJECT = "ADB Captain"
    const val EMAIL = "eatenlamp@proton.me"
    const val GITHUB = "https://github.com/eatenlamp"
    const val LICENSE = "AGPL-3.0-or-later"
}
