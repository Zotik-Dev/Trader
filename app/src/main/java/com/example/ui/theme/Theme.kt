package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ProfitGreen,
    onPrimary = TradeBgDark,
    primaryContainer = ProfitGreenBg,
    onPrimaryContainer = ProfitGreen,
    secondary = ElectricBlue,
    onSecondary = TradeBgDark,
    secondaryContainer = ElectricBlueBg,
    onSecondaryContainer = ElectricBlue,
    tertiary = AmberGold,
    background = TradeBgDark,
    surface = TradeSurfaceDark,
    surfaceVariant = TradeSurfaceVariantDark,
    outline = TradeOutlineDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    error = LossRed,
    errorContainer = LossRedBg,
    onError = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = ProfitGreen,
    onPrimary = TradeSurfaceLight,
    primaryContainer = ProfitGreenBg,
    onPrimaryContainer = ProfitGreen,
    secondary = ElectricBlue,
    onSecondary = TradeSurfaceLight,
    secondaryContainer = ElectricBlueBg,
    onSecondaryContainer = ElectricBlue,
    tertiary = AmberGold,
    background = TradeBgLight,
    surface = TradeSurfaceLight,
    surfaceVariant = TradeSurfaceVariantLight,
    outline = TradeOutlineLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    error = LossRed,
    errorContainer = LossRedBg,
    onError = TradeSurfaceLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // We prioritize our sleek trading theme
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
