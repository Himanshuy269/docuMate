package com.example.documate.ui.theme

import androidx.compose.runtime.Composable
import com.example.documate.core.theme.DocuMateTheme as CoreDocuMateTheme

@Composable
fun DocuMateTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CoreDocuMateTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
