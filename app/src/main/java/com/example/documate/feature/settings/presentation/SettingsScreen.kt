package com.example.documate.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.ui.AppViewModelProvider
import com.example.documate.core.ui.components.DocuCard
import com.example.documate.core.ui.components.DocuFilterChip
import com.example.documate.core.ui.components.DocuSectionHeader
import com.example.documate.core.ui.components.DocuTopAppBar

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme preference
            item {
                DocuSectionHeader(title = "Appearance")
                DocuCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Application Theme",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppThemeMode.entries.forEach { theme ->
                            DocuFilterChip(
                                selected = uiState.themeMode == theme,
                                onClick = { viewModel.onEvent(SettingsUiEvent.SelectTheme(theme)) },
                                label = theme.title.substringBefore(" ")
                            )
                        }
                    }
                }
            }

            // Document Defaults
            item {
                DocuSectionHeader(title = "Document Defaults")
                DocuCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Default PDF Page Size",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PdfPageSize.entries.forEach { size ->
                            DocuFilterChip(
                                selected = uiState.defaultPageSize == size,
                                onClick = { viewModel.onEvent(SettingsUiEvent.SelectDefaultPageSize(size)) },
                                label = size.displayName.substringBefore(" (")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Default Compression Level",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompressionPreset.entries.forEach { preset ->
                            DocuFilterChip(
                                selected = uiState.defaultCompression == preset,
                                onClick = { viewModel.onEvent(SettingsUiEvent.SelectDefaultCompression(preset)) },
                                label = preset.title.substringBefore(" ")
                            )
                        }
                    }
                }
            }

            // Backend Service Configuration
            item {
                DocuSectionHeader(title = "Office Conversion Backend")
                DocuCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "LibreOffice API Endpoint",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Used exclusively for Office (Excel, Word, PowerPoint) conversions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.backendServiceUrl,
                        onValueChange = { viewModel.onEvent(SettingsUiEvent.UpdateBackendUrl(it)) },
                        label = { Text("Backend URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Security & Privacy Architecture
            item {
                DocuSectionHeader(title = "Privacy & Security Model")
                DocuCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Privacy-First Architecture",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Images to PDF, Add Images to PDF, and PDF Compression operate 100% on-device.\n• Zero analytics, advertising, or telemetry SDKs.\n• Temporary working files are kept in private sandbox storage and wiped immediately.\n• Cloud backups of document data are disabled (allowBackup=false).\n• Office documents require explicit user consent per-file before backend rendering.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // About application
            item {
                DocuSectionHeader(title = "About DocuMate")
                DocuCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "DocuMate v1.0",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Clean Architecture Kotlin Multiplatform document utility application.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
