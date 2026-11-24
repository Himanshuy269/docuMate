package com.example.documate.feature.images_to_pdf.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.documate.core.ui.components.DocuCard
import com.example.documate.core.ui.components.DocuEmptyState
import com.example.documate.core.ui.components.DocuPrimaryButton
import com.example.documate.core.ui.components.DocuTopAppBar

@Composable
fun ImagesToPdfScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DocuTopAppBar(
                title = "Images to PDF",
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
                    title = "No Images Selected",
                    description = "Select images from your device to combine them into a single PDF document.",
                    icon = Icons.Default.AddPhotoAlternate
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DocuPrimaryButton(
                text = "Select Images",
                onClick = { /* Implemented in Stage 4 */ },
                icon = Icons.Default.AddPhotoAlternate
            )
        }
    }
}
