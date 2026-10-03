/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette - deep navy base + teal primary + copper secondary accent.

val NavyBase = Color(0xFF0B1B2B)
val NavySurface = Color(0xFF122A40)
val NavySurfaceVariant = Color(0xFF1B3A55)

val Teal80 = Color(0xFF8DEBD9)
val TealGrey80 = Color(0xFF9AD7CC)
val Copper80 = Color(0xFFE9C078)

val Teal40 = Color(0xFF0E7C6F)
val TealGrey40 = Color(0xFF2E7268)
val Copper40 = Color(0xFF966C1E)

val OnTeal = Color(0xFF00201A)

object AppPalettes {
    // Teal/Navy (default)
    object TealNavy {
        val navyBase = Color(0xFF0B1B2B)
        val navySurface = Color(0xFF122A40)
        val navySurfaceVariant = Color(0xFF1B3A55)
        val primaryDark = Color(0xFF8DEBD9)
        val primaryLight = Color(0xFF0E7C6F)
        val secondaryDark = Color(0xFF9AD7CC)
        val secondaryLight = Color(0xFF2E7268)
        val tertiaryDark = Color(0xFFE9C078)
        val tertiaryLight = Color(0xFF966C1E)
    }

    object Sapphire {
        val bgDark = Color(0xFF0B1424)
        val surfaceDark = Color(0xFF111F33)
        val surfaceVariantDark = Color(0xFF182B45)
        val primaryDark = Color(0xFF9DC6FF)
        val primaryLight = Color(0xFF2A5FA3)
        val secondaryDark = Color(0xFFB9CCEC)
        val secondaryLight = Color(0xFF415F8F)
        val tertiaryDark = Color(0xFFD8C4FF)
        val tertiaryLight = Color(0xFF6753A5)
    }

    object Emerald {
        val bgDark = Color(0xFF0F1711)
        val surfaceDark = Color(0xFF15221A)
        val surfaceVariantDark = Color(0xFF1E3022)
        val primaryDark = Color(0xFFA6D6B8)
        val primaryLight = Color(0xFF3C7C52)
        val secondaryDark = Color(0xFFBFDCC9)
        val secondaryLight = Color(0xFF486B53)
        val tertiaryDark = Color(0xFFBDE6FF)
        val tertiaryLight = Color(0xFF2C5A75)
    }

    object Copper {
        val bgDark = Color(0xFF1A1410)
        val surfaceDark = Color(0xFF231A14)
        val surfaceVariantDark = Color(0xFF2C211A)
        val primaryDark = Color(0xFFF2D08C)
        val primaryLight = Color(0xFF8F6A20)
        val secondaryDark = Color(0xFFE9D5AE)
        val secondaryLight = Color(0xFF70592F)
        val tertiaryDark = Color(0xFFBFD9FF)
        val tertiaryLight = Color(0xFF3C5C8F)
    }

    object Slate {
        val bgDark = Color(0xFF121417)
        val surfaceDark = Color(0xFF181C20)
        val surfaceVariantDark = Color(0xFF20252B)
        val primaryDark = Color(0xFFC5D6E6)
        val primaryLight = Color(0xFF556A7D)
        val secondaryDark = Color(0xFFD5E0EC)
        val secondaryLight = Color(0xFF657587)
        val tertiaryDark = Color(0xFFE0C9FF)
        val tertiaryLight = Color(0xFF6E5A91)
    }
}
