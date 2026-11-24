package com.example.documate.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.documate.core.ui.components.DocuCard
import com.example.documate.core.ui.components.DocuSectionHeader
import com.example.documate.core.ui.components.DocuTopAppBar

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DocuTopAppBar(
                title = "Settings",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DocuSectionHeader(title = "General Preferences")
            DocuCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Default PDF Page Size",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "A4 (210 × 297 mm)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DocuSectionHeader(title = "Privacy & Security")
            DocuCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "On-Device Processing",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Image-to-PDF and compression run 100% locally on your device without transmitting data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DocuSectionHeader(title = "About DocuMate")
            DocuCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "DocuMate v1.0",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Privacy-first Kotlin Multiplatform document utility application.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
