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

private val CosmicSlateColorScheme = darkColorScheme(
    primary = SophisticatedDarkPrimary,
    onPrimary = SophisticatedDarkOnPrimary,
    primaryContainer = SophisticatedDarkSurface,
    onPrimaryContainer = SophisticatedDarkText,
    secondary = SophisticatedDarkTertiary,
    onSecondary = SophisticatedDarkOnTertiary,
    secondaryContainer = SophisticatedDarkOutline,
    onSecondaryContainer = SophisticatedDarkText,
    tertiary = SophisticatedDarkTertiary,
    background = SophisticatedDarkBg,
    surface = SophisticatedDarkBg,
    surfaceVariant = SophisticatedDarkSurface,
    onBackground = SophisticatedDarkText,
    onSurface = SophisticatedDarkText,
    onSurfaceVariant = SophisticatedDarkSubText,
    outline = SophisticatedDarkOutline,
    outlineVariant = SophisticatedDarkOutline
)

private val NeonAmethystColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFD946EF), // Neon Fuchsia / Pink-Purple
    onPrimary = androidx.compose.ui.graphics.Color(0xFF4C0519), // Dark rose
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF161233), // Midnight Amethyst Surface
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    secondary = androidx.compose.ui.graphics.Color(0xFF06B6D4), // Cyan Accent
    onSecondary = androidx.compose.ui.graphics.Color(0xFF083344), // Deep Cyan
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF251F4F), // Amethyst Border / Dark slate-purple
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    tertiary = androidx.compose.ui.graphics.Color(0xFF8B5CF6), // Violet Accent
    background = androidx.compose.ui.graphics.Color(0xFF0D0B1E), // Deep Indigo / Obsidian Purple background
    surface = androidx.compose.ui.graphics.Color(0xFF0D0B1E),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF161233),
    onBackground = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF94A3B8),
    outline = androidx.compose.ui.graphics.Color(0xFF251F4F),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF251F4F)
)

private val ForestEmeraldColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF10B981), // Emerald Green
    onPrimary = androidx.compose.ui.graphics.Color(0xFF022C22), // Deep Teal
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF0B211D), // Deep Emerald Moss
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    secondary = androidx.compose.ui.graphics.Color(0xFFF59E0B), // Warm Amber
    onSecondary = androidx.compose.ui.graphics.Color(0xFF451A03), // Deep Gold Orange
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF11322C), // Dark Moss Border
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    tertiary = androidx.compose.ui.graphics.Color(0xFF34D399), // Mint Green
    background = androidx.compose.ui.graphics.Color(0xFF061411), // Dark Forest Black-Teal Background
    surface = androidx.compose.ui.graphics.Color(0xFF061411),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF0B211D),
    onBackground = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF94A3B8),
    outline = androidx.compose.ui.graphics.Color(0xFF11322C),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF11322C)
)

private val LightColorScheme =
  lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFD97706),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFFEF3C7),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF78350F),
    secondary = androidx.compose.ui.graphics.Color(0xFF0284C7),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFE0F2FE),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF0369A1),
    background = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF1F5F9),
    onBackground = androidx.compose.ui.graphics.Color(0xFF0F172A),
    onSurface = androidx.compose.ui.graphics.Color(0xFF0F172A),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF475569),
    outline = androidx.compose.ui.graphics.Color(0xFFCBD5E1),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFE2E8F0)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  themeStyle: String = "COSMIC_SLATE",
  // Force false dynamicColor to preserve branding colors
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) {
    when (themeStyle) {
      "NEON_AMETHYST" -> NeonAmethystColorScheme
      "FOREST_EMERALD" -> ForestEmeraldColorScheme
      else -> CosmicSlateColorScheme
    }
  } else {
    LightColorScheme
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
