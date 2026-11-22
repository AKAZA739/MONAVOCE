package com.myexpenses.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import java.util.Locale

// Signature Obsidian + Periwinkle-Violet + Cosmic Neon Palette
val AccentPeriwinkle = Color(0xFF8B8BFF)
val AccentPeriwinkleDeep = Color(0xFF6C6CFF)
val NeonCyan = Color(0xFF00E5FF)
val NeonMagenta = Color(0xFFD946EF)
val NeonBlue = Color(0xFF3B82F6)
val ObsidianBg = Color(0xFF08080C)
val ObsidianCard = Color(0xFF14141B)
val ObsidianCardElevated = Color(0xFF1C1C26)
val ObsidianBorder = Color(0xFF262634)
val ObsidianMutedText = Color(0xFF8C8CA1)

private val DarkColors = darkColorScheme(
    primary = AccentPeriwinkle,
    onPrimary = Color(0xFF0B0B1A),
    primaryContainer = Color(0xFF252542),
    onPrimaryContainer = Color(0xFFDADAFE),
    secondary = Color(0xFFA5A6FF),
    onSecondary = Color(0xFF12122A),
    secondaryContainer = Color(0xFF1E1E2C),
    onSecondaryContainer = Color(0xFFD8D8EC),
    tertiary = NeonCyan,
    onTertiary = Color(0xFF002026),
    background = ObsidianBg,
    onBackground = Color(0xFFF6F6FB),
    surface = ObsidianCard,
    onSurface = Color(0xFFF6F6FB),
    surfaceVariant = ObsidianCardElevated,
    onSurfaceVariant = ObsidianMutedText,
    outline = Color(0xFF343448),
    outlineVariant = ObsidianBorder,
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2B0606)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5D5DF0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E7FF),
    onPrimaryContainer = Color(0xFF1A1A52),
    secondary = Color(0xFF64648C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECECF7),
    onSecondaryContainer = Color(0xFF212138),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = Color(0xFFF5F5FA),
    onBackground = Color(0xFF11111A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF11111A),
    surfaceVariant = Color(0xFFECECF4),
    onSurfaceVariant = Color(0xFF5E5E73),
    outline = Color(0xFFCBCBD9),
    outlineVariant = Color(0xFFE2E2EE),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 19.sp),
    labelLarge = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp),
    labelMedium = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Medium, lineHeight = 15.sp)
)

fun resolveIsArabic(appLanguage: String): Boolean = when (appLanguage) {
    "ar" -> true
    "en" -> false
    else -> Locale.getDefault().language.equals("ar", ignoreCase = true)
}

@Composable
fun MyExpensesTheme(
    themeMode: String = "dark",
    appLanguage: String = "system",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val isArabic = resolveIsArabic(appLanguage)
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content
        )
    }
}
