package com.example.documate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.documate.core.navigation.DocuNavGraph
import com.example.documate.core.theme.DocuMateTheme
import com.example.documate.feature.settings.presentation.AppThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DocuMateApplication
        val settingsRepository = app.container.settingsRepository

        setContent {
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
    }
}
