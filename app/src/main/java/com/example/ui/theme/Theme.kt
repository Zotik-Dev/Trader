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
import com.example.model.AppColorTheme

fun getDarkColorScheme(colorTheme: AppColorTheme = AppColorTheme.EMERALD) = darkColorScheme(
    primary = colorTheme.primaryColor,
    onPrimary = TradeBgDark,
    primaryContainer = colorTheme.containerColor,
    onPrimaryContainer = colorTheme.primaryColor,
    secondary = colorTheme.secondaryColor,
    onSecondary = TradeBgDark,
    secondaryContainer = colorTheme.secondaryColor.copy(alpha = 0.15f),
    onSecondaryContainer = colorTheme.secondaryColor,
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

fun getLightColorScheme(colorTheme: AppColorTheme = AppColorTheme.EMERALD) = lightColorScheme(
    primary = colorTheme.primaryColor,
    onPrimary = TradeSurfaceLight,
    primaryContainer = colorTheme.containerColor,
    onPrimaryContainer = colorTheme.primaryColor,
    secondary = colorTheme.secondaryColor,
    onSecondary = TradeSurfaceLight,
    secondaryContainer = colorTheme.secondaryColor.copy(alpha = 0.15f),
    onSecondaryContainer = colorTheme.secondaryColor,
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
    appColorTheme: AppColorTheme = AppColorTheme.EMERALD,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> getDarkColorScheme(appColorTheme)
        else -> getLightColorScheme(appColorTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

