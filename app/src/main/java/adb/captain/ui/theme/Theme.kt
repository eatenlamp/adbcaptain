/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppTheme(val displayName: String) {
    TealNavy("Teal/Navy"),
    Sapphire("Sapphire"),
    Emerald("Emerald"),
    Copper("Copper"),
    Slate("Slate"),
    Dynamic("Material You")
}

fun AppTheme.accentColor(): Color = when (this) {
    AppTheme.TealNavy -> AppPalettes.TealNavy.primaryDark
    AppTheme.Sapphire -> AppPalettes.Sapphire.primaryDark
    AppTheme.Emerald -> AppPalettes.Emerald.primaryDark
    AppTheme.Copper -> AppPalettes.Copper.primaryDark
    AppTheme.Slate -> AppPalettes.Slate.primaryDark
    AppTheme.Dynamic -> Color(0xFF7C4DFF)
}


private fun tealNavyDark() = darkColorScheme(
    primary = AppPalettes.TealNavy.primaryDark,
    onPrimary = Color(0xFF00201A),
    primaryContainer = AppPalettes.TealNavy.primaryLight,
    onPrimaryContainer = Color(0xFFDBFFF5),
    secondary = AppPalettes.TealNavy.secondaryDark,
    onSecondary = Color(0xFF003328),
    secondaryContainer = AppPalettes.TealNavy.secondaryLight,
    onSecondaryContainer = Color(0xFFCBFFE9),
    tertiary = AppPalettes.TealNavy.tertiaryDark,
    onTertiary = Color(0xFF372800),
    tertiaryContainer = AppPalettes.TealNavy.tertiaryLight,
    onTertiaryContainer = Color(0xFFFFE1A5),
    background = AppPalettes.TealNavy.navyBase,
    onBackground = Color(0xFFE6EDF5),
    surface = AppPalettes.TealNavy.navySurface,
    onSurface = Color(0xFFE6EDF5),
    surfaceVariant = AppPalettes.TealNavy.navySurfaceVariant,
    onSurfaceVariant = Color(0xFFB6C7D6)
)

private fun tealNavyLight() = lightColorScheme(
    primary = AppPalettes.TealNavy.primaryLight,
    onPrimary = Color.White,
    primaryContainer = AppPalettes.TealNavy.primaryDark,
    onPrimaryContainer = Color(0xFF00382F),
    secondary = AppPalettes.TealNavy.secondaryLight,
    onSecondary = Color.White,
    secondaryContainer = AppPalettes.TealNavy.secondaryDark,
    onSecondaryContainer = Color(0xFF14332B),
    tertiary = AppPalettes.TealNavy.tertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = AppPalettes.TealNavy.tertiaryDark,
    onTertiaryContainer = Color(0xFF3B2D00),
    background = Color(0xFFF6FAF8),
    onBackground = Color(0xFF171D1C),
    surface = Color(0xFFF7FBFA),
    onSurface = Color(0xFF171D1C),
    surfaceVariant = Color(0xFFDAE5E0),
    onSurfaceVariant = Color(0xFF3F4945)
)

private fun sapphireDark() = darkColorScheme(
    primary = AppPalettes.Sapphire.primaryDark,
    onPrimary = Color(0xFF001B3C),
    primaryContainer = AppPalettes.Sapphire.primaryLight,
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = AppPalettes.Sapphire.secondaryDark,
    onSecondary = Color(0xFF121D2B),
    secondaryContainer = AppPalettes.Sapphire.secondaryLight,
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = AppPalettes.Sapphire.tertiaryDark,
    onTertiary = Color(0xFF231A36),
    tertiaryContainer = AppPalettes.Sapphire.tertiaryLight,
    onTertiaryContainer = Color(0xFFDBDEFF),
    background = AppPalettes.Sapphire.bgDark,
    onBackground = Color(0xFFE1E2E8),
    surface = AppPalettes.Sapphire.surfaceDark,
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = AppPalettes.Sapphire.surfaceVariantDark,
    onSurfaceVariant = Color(0xFFB7C7DA)
)

private fun sapphireLight() = lightColorScheme(
    primary = AppPalettes.Sapphire.primaryLight,
    onPrimary = Color.White,
    primaryContainer = AppPalettes.Sapphire.primaryDark,
    onPrimaryContainer = Color(0xFF001B3C),
    secondary = AppPalettes.Sapphire.secondaryLight,
    onSecondary = Color.White,
    secondaryContainer = AppPalettes.Sapphire.secondaryDark,
    onSecondaryContainer = Color(0xFF121D2B),
    tertiary = AppPalettes.Sapphire.tertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = AppPalettes.Sapphire.tertiaryDark,
    onTertiaryContainer = Color(0xFF221533),
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF7F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EC),
    onSurfaceVariant = Color(0xFF42474F)
)

private fun emeraldDark() = darkColorScheme(
    primary = AppPalettes.Emerald.primaryDark,
    onPrimary = Color(0xFF002913),
    primaryContainer = AppPalettes.Emerald.primaryLight,
    onPrimaryContainer = Color(0xFFC3F1D1),
    secondary = AppPalettes.Emerald.secondaryDark,
    onSecondary = Color(0xFF122B1A),
    secondaryContainer = AppPalettes.Emerald.secondaryLight,
    onSecondaryContainer = Color(0xFFD2E8D6),
    tertiary = AppPalettes.Emerald.tertiaryDark,
    onTertiary = Color(0xFF001F2B),
    tertiaryContainer = AppPalettes.Emerald.tertiaryLight,
    onTertiaryContainer = Color(0xFFBDE6FF),
    background = AppPalettes.Emerald.bgDark,
    onBackground = Color(0xFFE1E3DF),
    surface = AppPalettes.Emerald.surfaceDark,
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = AppPalettes.Emerald.surfaceVariantDark,
    onSurfaceVariant = Color(0xFFB8CCC0)
)

private fun emeraldLight() = lightColorScheme(
    primary = AppPalettes.Emerald.primaryLight,
    onPrimary = Color.White,
    primaryContainer = AppPalettes.Emerald.primaryDark,
    onPrimaryContainer = Color(0xFF002913),
    secondary = AppPalettes.Emerald.secondaryLight,
    onSecondary = Color.White,
    secondaryContainer = AppPalettes.Emerald.secondaryDark,
    onSecondaryContainer = Color(0xFF122B1A),
    tertiary = AppPalettes.Emerald.tertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = AppPalettes.Emerald.tertiaryDark,
    onTertiaryContainer = Color(0xFF001F2B),
    background = Color(0xFFF7FBF4),
    onBackground = Color(0xFF191C19),
    surface = Color(0xFFF7FBF4),
    onSurface = Color(0xFF191C19),
    surfaceVariant = Color(0xFFDFE6E0),
    onSurfaceVariant = Color(0xFF424942)
)

private fun copperDark() = darkColorScheme(
    primary = AppPalettes.Copper.primaryDark,
    onPrimary = Color(0xFF261900),
    primaryContainer = AppPalettes.Copper.primaryLight,
    onPrimaryContainer = Color(0xFFF2D08C),
    secondary = AppPalettes.Copper.secondaryDark,
    onSecondary = Color(0xFF211A0C),
    secondaryContainer = AppPalettes.Copper.secondaryLight,
    onSecondaryContainer = Color(0xFFE9D5AE),
    tertiary = AppPalettes.Copper.tertiaryDark,
    onTertiary = Color(0xFF001B3D),
    tertiaryContainer = AppPalettes.Copper.tertiaryLight,
    onTertiaryContainer = Color(0xFFD6E3FF),
    background = AppPalettes.Copper.bgDark,
    onBackground = Color(0xFFEDE0D5),
    surface = AppPalettes.Copper.surfaceDark,
    onSurface = Color(0xFFEDE0D5),
    surfaceVariant = AppPalettes.Copper.surfaceVariantDark,
    onSurfaceVariant = Color(0xFFD7C9B4)
)

private fun copperLight() = lightColorScheme(
    primary = AppPalettes.Copper.primaryLight,
    onPrimary = Color.White,
    primaryContainer = AppPalettes.Copper.primaryDark,
    onPrimaryContainer = Color(0xFF261900),
    secondary = AppPalettes.Copper.secondaryLight,
    onSecondary = Color.White,
    secondaryContainer = AppPalettes.Copper.secondaryDark,
    onSecondaryContainer = Color(0xFF211A0C),
    tertiary = AppPalettes.Copper.tertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = AppPalettes.Copper.tertiaryDark,
    onTertiaryContainer = Color(0xFF001B3D),
    background = Color(0xFFFFF7EE),
    onBackground = Color(0xFF1E1B13),
    surface = Color(0xFFFFF7EE),
    onSurface = Color(0xFF1E1B13),
    surfaceVariant = Color(0xFFECE1CF),
    onSurfaceVariant = Color(0xFF4D4638)
)

private fun slateDark() = darkColorScheme(
    primary = AppPalettes.Slate.primaryDark,
    onPrimary = Color(0xFF0F1C2B),
    primaryContainer = AppPalettes.Slate.primaryLight,
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = AppPalettes.Slate.secondaryDark,
    onSecondary = Color(0xFF151D26),
    secondaryContainer = AppPalettes.Slate.secondaryLight,
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = AppPalettes.Slate.tertiaryDark,
    onTertiary = Color(0xFF261A36),
    tertiaryContainer = AppPalettes.Slate.tertiaryLight,
    onTertiaryContainer = Color(0xFFE0C9FF),
    background = AppPalettes.Slate.bgDark,
    onBackground = Color(0xFFE2E2E6),
    surface = AppPalettes.Slate.surfaceDark,
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = AppPalettes.Slate.surfaceVariantDark,
    onSurfaceVariant = Color(0xFFC5C6D0)
)

private fun slateLight() = lightColorScheme(
    primary = AppPalettes.Slate.primaryLight,
    onPrimary = Color.White,
    primaryContainer = AppPalettes.Slate.primaryDark,
    onPrimaryContainer = Color(0xFF0F1C2B),
    secondary = AppPalettes.Slate.secondaryLight,
    onSecondary = Color.White,
    secondaryContainer = AppPalettes.Slate.secondaryDark,
    onSecondaryContainer = Color(0xFF151D26),
    tertiary = AppPalettes.Slate.tertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = AppPalettes.Slate.tertiaryDark,
    onTertiaryContainer = Color(0xFF261A36),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE2E2EC),
    onSurfaceVariant = Color(0xFF44474F)
)

@Composable
fun ADBCaptainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = AppTheme.TealNavy,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && appTheme == AppTheme.Dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> when (appTheme) {
            AppTheme.TealNavy -> tealNavyDark()
            AppTheme.Sapphire -> sapphireDark()
            AppTheme.Emerald -> emeraldDark()
            AppTheme.Copper -> copperDark()
            AppTheme.Slate -> slateDark()
            AppTheme.Dynamic -> {
                val context = LocalContext.current
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(context) else tealNavyDark()
            }
        }
        else -> when (appTheme) {
            AppTheme.TealNavy -> tealNavyLight()
            AppTheme.Sapphire -> sapphireLight()
            AppTheme.Emerald -> emeraldLight()
            AppTheme.Copper -> copperLight()
            AppTheme.Slate -> slateLight()
            AppTheme.Dynamic -> {
                val context = LocalContext.current
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(context) else tealNavyLight()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
