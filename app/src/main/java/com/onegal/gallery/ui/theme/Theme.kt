package com.onegal.gallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onegal.gallery.data.model.ThemePreference

object OneUiColors {
    val AmoledBlack = Color(0xFF000000)
    val DarkSurface = Color(0xFF141416)
    val DarkCard = Color(0xFF1C1C1E)
    val DarkPillBorder = Color(0xFF2C2C2E)
    val DarkSecondaryText = Color(0xFF9A9AA1)

    val LightCanvas = Color(0xFFF6F6F8)
    val LightCard = Color(0xFFFFFFFF)
    val LightBorder = Color(0xFFE5E5EA)
    val LightSecondaryText = Color(0xFF63636A)

    val OneUiBlue = Color(0xFF387AFF)
    val OneUiBluePressed = Color(0xFF2563EB)
    val OneUiDanger = Color(0xFFFF453A)
    val OneUiFavorite = Color(0xFFFF375F)
}

private val OneUiDarkScheme = darkColorScheme(
    primary = OneUiColors.OneUiBlue,
    onPrimary = Color.White,
    secondary = Color(0xFF5E92FF),
    onSecondary = Color.White,
    background = OneUiColors.AmoledBlack,
    onBackground = Color(0xFFF5F5F7),
    surface = OneUiColors.AmoledBlack,
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = OneUiColors.DarkCard,
    onSurfaceVariant = OneUiColors.DarkSecondaryText,
    surfaceContainer = OneUiColors.DarkSurface,
    surfaceContainerHigh = OneUiColors.DarkCard,
    outline = OneUiColors.DarkPillBorder,
    error = OneUiColors.OneUiDanger
)

private val OneUiLightScheme = lightColorScheme(
    primary = OneUiColors.OneUiBlue,
    onPrimary = Color.White,
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    background = OneUiColors.LightCanvas,
    onBackground = Color(0xFF0A0A0C),
    surface = OneUiColors.LightCanvas,
    onSurface = Color(0xFF0A0A0C),
    surfaceVariant = OneUiColors.LightCard,
    onSurfaceVariant = OneUiColors.LightSecondaryText,
    surfaceContainer = Color(0xFFEEEEF2),
    surfaceContainerHigh = OneUiColors.LightCard,
    outline = OneUiColors.LightBorder,
    error = OneUiColors.OneUiDanger
)

private val OneUiTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

private val OneUiShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun OnegalTheme(
    preference: ThemePreference = ThemePreference.DARK_AMOLED,
    content: @Composable () -> Unit
) {
    val useDark = when (preference) {
        ThemePreference.DARK_AMOLED -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (useDark) OneUiDarkScheme else OneUiLightScheme,
        typography = OneUiTypography,
        shapes = OneUiShapes,
        content = content
    )
}
