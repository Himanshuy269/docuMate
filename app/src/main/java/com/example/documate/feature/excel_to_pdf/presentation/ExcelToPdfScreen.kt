package com.example.documate.feature.excel_to_pdf.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.documate.core.ui.components.DocuCard
import com.example.documate.core.ui.components.DocuEmptyState
import com.example.documate.core.ui.components.DocuPrimaryButton
import com.example.documate.core.ui.components.DocuTopAppBar

@Composable
fun ExcelToPdfScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DocuTopAppBar(
                title = "Excel to PDF",
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
                    title = "Select Spreadsheet",
                    description = "Choose an Excel workbook (.xlsx or .xls) to convert tables and sheets to PDF.",
                    icon = Icons.Default.TableChart
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DocuPrimaryButton(
                text = "Select Excel File",
                onClick = { /* Implemented in Stage 6 */ },
                icon = Icons.Default.TableChart
            )
        }
    }
}
