package com.matth.scaleconnect.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Extended semantic colors the design uses that Material3's ColorScheme has no slot for. */
data class ExtendedColors(
    val surface2: Color,
    val muted: Color,
    val faint: Color,
    val good: Color,
    val goodSoft: Color,
    val metricFat: Color,
    val metricHydration: Color,
    val metricMuscle: Color,
    val metricBone: Color,
    val metricBmr: Color,
    val bgGradientStart: Color,
    val bgGradientEnd: Color,
)

private val LightExtendedColors = ExtendedColors(
    surface2 = LightSurface2,
    muted = LightMuted,
    faint = LightFaint,
    good = LightGood,
    goodSoft = LightGoodSoft,
    metricFat = LightMetricFat,
    metricHydration = LightMetricHydration,
    metricMuscle = LightMetricMuscle,
    metricBone = LightMetricBone,
    metricBmr = LightMetricBmr,
    bgGradientStart = LightBgGradientStart,
    bgGradientEnd = LightBgGradientEnd,
)

private val DarkExtendedColors = ExtendedColors(
    surface2 = DarkSurface2,
    muted = DarkMuted,
    faint = DarkFaint,
    good = DarkGood,
    goodSoft = DarkGoodSoft,
    metricFat = DarkMetricFat,
    metricHydration = DarkMetricHydration,
    metricMuscle = DarkMetricMuscle,
    metricBone = DarkMetricBone,
    metricBmr = DarkMetricBmr,
    bgGradientStart = DarkBg,
    bgGradientEnd = DarkBg,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

object ScaleTheme {
    val extendedColors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}

private val LightScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurface2,
    onSurfaceVariant = LightMuted,
    outline = LightLine,
)

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = DarkMuted,
    outline = DarkLine,
)

@Composable
fun ScaleConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ScaleConnectTypography,
            content = content
        )
    }
}
