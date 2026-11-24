package com.example.documate.feature.add_images_to_pdf.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.documate.core.ui.components.DocuCard
import com.example.documate.core.ui.components.DocuEmptyState
import com.example.documate.core.ui.components.DocuPrimaryButton
import com.example.documate.core.ui.components.DocuTopAppBar

@Composable
fun AddImagesToPdfScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DocuTopAppBar(
                title = "Add Images to PDF",
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
                    title = "Select Source PDF",
                    description = "Choose an existing PDF document, then select images to insert into it.",
                    icon = Icons.Default.PictureAsPdf
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DocuPrimaryButton(
                text = "Select PDF Document",
                onClick = { /* Implemented in Stage 5 */ },
                icon = Icons.Default.PictureAsPdf
            )
        }
    }
}
