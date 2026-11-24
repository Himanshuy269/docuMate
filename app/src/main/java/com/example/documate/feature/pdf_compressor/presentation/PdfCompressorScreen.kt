package com.example.documate.feature.pdf_compressor.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.documate.core.ui.components.DocuCard
import com.example.documate.core.ui.components.DocuEmptyState
import com.example.documate.core.ui.components.DocuPrimaryButton
import com.example.documate.core.ui.components.DocuTopAppBar

@Composable
fun PdfCompressorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DocuTopAppBar(
                title = "PDF Compression",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            DocuCard(modifier = Modifier.weight(1f)) {
                DocuEmptyState(
                    title = "No PDF Selected",
                    description = "Choose a PDF file to reduce its size using Low, Medium, or High compression presets.",
                    icon = Icons.Default.Compress
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DocuPrimaryButton(
                text = "Select PDF to Compress",
                onClick = { /* Implemented in Stage 5 */ },
                icon = Icons.Default.Compress
            )
        }
    }
}
