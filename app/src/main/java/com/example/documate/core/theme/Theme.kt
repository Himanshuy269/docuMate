package com.example.documate.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = DocuPrimaryLight,
    onPrimary = DocuOnPrimaryLight,
    primaryContainer = DocuPrimaryContainerLight,
    onPrimaryContainer = DocuOnPrimaryContainerLight,
    secondary = DocuSecondaryLight,
    onSecondary = DocuOnSecondaryLight,
    secondaryContainer = DocuSecondaryContainerLight,
    onSecondaryContainer = DocuOnSecondaryContainerLight,
    tertiary = DocuTertiaryLight,
    onTertiary = DocuOnTertiaryLight,
    tertiaryContainer = DocuTertiaryContainerLight,
    onTertiaryContainer = DocuOnTertiaryContainerLight,
    background = DocuBackgroundLight,
    onBackground = DocuOnBackgroundLight,
    surface = DocuSurfaceLight,
    onSurface = DocuOnSurfaceLight,
    surfaceVariant = DocuSurfaceVariantLight,
    onSurfaceVariant = DocuOnSurfaceVariantLight,
    outline = DocuOutlineLight,
    outlineVariant = DocuOutlineVariantLight,
    error = DocuError
)

private val DarkColorScheme = darkColorScheme(
    primary = DocuPrimaryDark,
    onPrimary = DocuOnPrimaryDark,
    primaryContainer = DocuPrimaryContainerDark,
    onPrimaryContainer = DocuOnPrimaryContainerDark,
    secondary = DocuSecondaryDark,
    onSecondary = DocuOnSecondaryDark,
    secondaryContainer = DocuSecondaryContainerDark,
    onSecondaryContainer = DocuOnSecondaryContainerDark,
    tertiary = DocuTertiaryDark,
    onTertiary = DocuOnTertiaryDark,
    tertiaryContainer = DocuTertiaryContainerDark,
    onTertiaryContainer = DocuOnTertiaryContainerDark,
    background = DocuBackgroundDark,
    onBackground = DocuOnBackgroundDark,
    surface = DocuSurfaceDark,
    onSurface = DocuOnSurfaceDark,
    surfaceVariant = DocuSurfaceVariantDark,
    onSurfaceVariant = DocuOnSurfaceVariantDark,
    outline = DocuOutlineDark,
    outlineVariant = DocuOutlineVariantDark,
    error = DocuError
)

@Composable
fun DocuMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to ensure consistent brand colors across devices
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
        typography = DocuTypography,
        shapes = DocuShapes,
        content = content
    )
}
