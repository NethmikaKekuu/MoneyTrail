package lk.moneytrail.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import lk.moneytrail.app.data.preferences.ThemeMode

// Roles as used by the Figma screens: text fields sit on surfaceContainerLow with an
// outlineVariant border, secondary buttons use secondaryContainer, and the focus ring
// and accents use tertiary. Dynamic colour is off so the brand orange always shows.
private val LightColorScheme = lightColorScheme(
    primary = Ember,
    onPrimary = Color.White,
    primaryContainer = Peach,
    onPrimaryContainer = Rust,
    secondary = Rust,
    onSecondary = Color.White,
    secondaryContainer = Cream,
    onSecondaryContainer = Rust,
    tertiary = TrailOrange,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Cream,
    onSurfaceVariant = Bark,
    surfaceContainerLow = Cream,
    outline = Stone,
    outlineVariant = Sand
)

private val DarkColorScheme = darkColorScheme(
    primary = Glow,
    onPrimary = Char,
    primaryContainer = Rust,
    onPrimaryContainer = Peach,
    secondary = Glow,
    onSecondary = Char,
    secondaryContainer = NightRaised,
    onSecondaryContainer = Peach,
    tertiary = TrailOrange,
    onTertiary = Char,
    background = Night,
    onBackground = Linen,
    surface = Night,
    onSurface = Linen,
    surfaceVariant = NightRaised,
    onSurfaceVariant = Dusk,
    surfaceContainerLow = NightRaised,
    outline = Ash,
    outlineVariant = NightBorder
)

@Composable
fun MoneyTrailTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    },
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
