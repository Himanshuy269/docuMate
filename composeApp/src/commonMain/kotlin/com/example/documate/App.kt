package com.example.documate

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.documate.core.di.AppContainerHolder
import com.example.documate.core.domain.model.AppThemeMode
import com.example.documate.core.navigation.DocuNavGraph
import com.example.documate.core.theme.DocuMateTheme

/**
 * Universal Composable application root shared across Android, iOS, and Web.
 */
@Composable
fun App() {
    val settingsRepository = AppContainerHolder.current.settingsRepository
    val userSettings by settingsRepository.getSettings().collectAsState(
        initial = settingsRepository.getSettingsSync()
    )
    val isDark = when (userSettings.themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    DocuMateTheme(darkTheme = isDark) {
        Surface(modifier = Modifier.fillMaxSize()) {
            DocuNavGraph()
        }
    }
}
