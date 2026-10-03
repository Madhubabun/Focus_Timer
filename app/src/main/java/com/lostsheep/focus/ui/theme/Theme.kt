package com.lostsheep.focus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object Palette {
    val Cream = Color(0xFFF7F2E7)
    val CreamDeep = Color(0xFFEFE7D6)
    val SoftGreen = Color(0xFF6FA067)
    val Earth = Color(0xFFA3876A)
    val Gold = Color(0xFFC99A3E)
    val GoldLight = Color(0xFFE6BE6A)
    val White = Color(0xFFFFFFFF)
    val DarkGreen = Color(0xFF2E5A3F)
    val Ink = Color(0xFF26332B)
    val Muted = Color(0xFF7B7869)

    val Night = Color(0xFF121815)
    val NightSurface = Color(0xFF1A221D)
    val NightRaised = Color(0xFF222C26)
    val NightInk = Color(0xFFEFE9DC)
    val NightMuted = Color(0xFFA39E8E)
    val NightGreen = Color(0xFF9FD08F)
}

/** Colors Material doesn't name, kept in one place for both themes. */
@Immutable
data class SheepColors(
    val muted: Color,
    val hairline: Color,
    val gold: Color,
    val track: Color,
    val sceneDim: Color,
)

val LocalSheepColors = staticCompositionLocalOf {
    SheepColors(Palette.Muted, Color(0x1F26332B), Palette.Gold, Palette.CreamDeep, Color.Transparent)
}

private val Light = lightColorScheme(
    primary = Palette.DarkGreen,
    onPrimary = Palette.White,
    secondary = Palette.SoftGreen,
    onSecondary = Palette.White,
    tertiary = Palette.Gold,
    background = Palette.Cream,
    onBackground = Palette.Ink,
    surface = Palette.Cream,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.CreamDeep,
    onSurfaceVariant = Palette.Muted,
    surfaceContainer = Palette.White,
    outline = Color(0x3326332B),
)

private val Dark = darkColorScheme(
    primary = Palette.NightGreen,
    onPrimary = Color(0xFF13201A),
    secondary = Palette.SoftGreen,
    onSecondary = Color(0xFF13201A),
    tertiary = Palette.GoldLight,
    background = Palette.Night,
    onBackground = Palette.NightInk,
    surface = Palette.Night,
    onSurface = Palette.NightInk,
    surfaceVariant = Palette.NightRaised,
    onSurfaceVariant = Palette.NightMuted,
    surfaceContainer = Palette.NightSurface,
    outline = Color(0x33EFE9DC),
)

private val Serif = FontFamily.Serif
private val Sans = FontFamily.SansSerif

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Light, fontSize = 84.sp, letterSpacing = (-0.02).em, fontFeatureSettings = "tnum"),
    displayMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Light, fontSize = 64.sp, letterSpacing = (-0.02).em, fontFeatureSettings = "tnum"),
    headlineLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 15.sp, letterSpacing = 0.02.em),
    labelMedium = TextStyle(fontFamily = Sans, fontSize = 13.sp, letterSpacing = 0.04.em),
    labelSmall = TextStyle(fontFamily = Sans, fontSize = 11.sp, letterSpacing = 0.12.em),
)

@Composable
fun LostSheepTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val extra = if (dark) {
        SheepColors(Palette.NightMuted, Color(0x22EFE9DC), Palette.GoldLight, Palette.NightRaised, Color(0x33000000))
    } else {
        SheepColors(Palette.Muted, Color(0x1A26332B), Palette.Gold, Palette.CreamDeep, Color.Transparent)
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalSheepColors provides extra) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, typography = AppTypography, content = content)
    }
}
