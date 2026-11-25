package com.example.documate.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.PresentToAll
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.theme.DocuExcelGreen
import com.example.documate.core.theme.DocuPdfRed
import com.example.documate.core.theme.DocuPowerPointOrange
import com.example.documate.core.theme.DocuWordBlue

@Composable
fun DocuToolCard(
    tool: DocumentTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (tool) {
        DocumentTool.IMAGES_TO_PDF -> Icons.Default.Image to DocuPdfRed
        DocumentTool.ADD_IMAGES_TO_PDF -> Icons.Default.PostAdd to DocuPdfRed
        DocumentTool.PDF_COMPRESSION -> Icons.Default.Summarize to DocuPdfRed
        DocumentTool.EXCEL_TO_PDF -> Icons.Default.TableChart to DocuExcelGreen
        DocumentTool.WORD_TO_PDF -> Icons.Default.Description to DocuWordBlue
        DocumentTool.POWERPOINT_TO_PDF -> Icons.Default.PresentToAll to DocuPowerPointOrange
    }

    DocuCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        padding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = tint.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tool.title,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tool.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
