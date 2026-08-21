package adb.captain.ui.theme

import android.app.Activity
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

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = OnTeal,
    primaryContainer = Teal40,
    onPrimaryContainer = Color(0xFFDBFFF5),
    secondary = TealGrey80,
    onSecondary = Color(0xFF003328),
    secondaryContainer = TealGrey40,
    onSecondaryContainer = Color(0xFFCBFFE9),
    tertiary = Copper80,
    onTertiary = Color(0xFF372800),
    tertiaryContainer = Copper40,
    onTertiaryContainer = Color(0xFFFFE1A5),
    background = NavyBase,
    onBackground = Color(0xFFE6EDF5),
    surface = NavySurface,
    onSurface = Color(0xFFE6EDF5),
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = Color(0xFFB6C7D6)
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal80,
    onPrimaryContainer = Color(0xFF00382F),
    secondary = TealGrey40,
    onSecondary = Color.White,
    secondaryContainer = TealGrey80,
    onSecondaryContainer = Color(0xFF14332B),
    tertiary = Copper40,
    onTertiary = Color.White,
    tertiaryContainer = Copper80,
    onTertiaryContainer = Color(0xFF3B2D00),
    background = Color(0xFFF6FAF8),
    onBackground = Color(0xFF171D1C),
    surface = Color(0xFFF7FBFA),
    onSurface = Color(0xFF171D1C),
    surfaceVariant = Color(0xFFDAE5E0),
    onSurfaceVariant = Color(0xFF3F4945)
)

@Composable
fun ADBCaptainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Brand colors are used by default; dynamic wallpaper color can be opted in.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}