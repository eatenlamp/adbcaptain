/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.model

/**
 * Whether the build runs with the paid features unlocked.
 */
enum class ProStatus {
    FREE,
    PRO;

    val isPro: Boolean get() = this == PRO
}

/**
 * Features the paid build unlocks. Every check in the app goes through
 * [ProRepository.isUnlocked], so adding a new one means adding a case here
 * and a check where it matters.
 */
enum class ProFeature {
    /** Macros beyond the free limit. */
    UNLIMITED_MACROS,

    /** Exporting a terminal session as a Markdown file. */
    TERMINAL_MARKDOWN_EXPORT
}

object ProLimits {
    /** How many macros a free user may keep. */
    const val FREE_MACRO_COUNT = 3
}

/**
 * A product offered by the store, ready to be shown to the user.
 */
data class ProProduct(
    val id: String,
    val title: String,
    val description: String,
    val formattedPrice: String
)
