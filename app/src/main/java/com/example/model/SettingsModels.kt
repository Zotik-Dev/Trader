package com.example.model

import androidx.compose.ui.graphics.Color

enum class AppColorTheme(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val containerColor: Color
) {
    EMERALD(
        displayName = "Emerald Pro",
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF38BDF8),
        containerColor = Color(0x2210B981)
    ),
    CYAN(
        displayName = "Cyber Cyan",
        primaryColor = Color(0xFF06B6D4),
        secondaryColor = Color(0xFF3B82F6),
        containerColor = Color(0x2206B6D4)
    ),
    BLUE(
        displayName = "Royal Ocean",
        primaryColor = Color(0xFF3B82F6),
        secondaryColor = Color(0xFF6366F1),
        containerColor = Color(0x223B82F6)
    ),
    PURPLE(
        displayName = "Electric Violet",
        primaryColor = Color(0xFF8B5CF6),
        secondaryColor = Color(0xFFEC4899),
        containerColor = Color(0x228B5CF6)
    ),
    AMBER(
        displayName = "Gold Bullion",
        primaryColor = Color(0xFFF59E0B),
        secondaryColor = Color(0xFF10B981),
        containerColor = Color(0x22F59E0B)
    ),
    CRIMSON(
        displayName = "Ruby Momentum",
        primaryColor = Color(0xFFF43F5E),
        secondaryColor = Color(0xFFFB923C),
        containerColor = Color(0x22F43F5E)
    ),
    SLATE(
        displayName = "Titanium Slate",
        primaryColor = Color(0xFF94A3B8),
        secondaryColor = Color(0xFFCBD5E1),
        containerColor = Color(0x2294A3B8)
    ),
    NEON(
        displayName = "Matrix Neon",
        primaryColor = Color(0xFF84CC16),
        secondaryColor = Color(0xFF10B981),
        containerColor = Color(0x2284CC16)
    )
}

enum class ThemeMode(val displayName: String) {
    DARK("Dark Theme (Trading Mode)"),
    LIGHT("Light Theme"),
    SYSTEM("System Default")
}

data class CloudAccount(
    val provider: String, // "google" or "facebook"
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val lastBackupTimestamp: Long = 0L,
    val lastBackupTradeCount: Int = 0
)
