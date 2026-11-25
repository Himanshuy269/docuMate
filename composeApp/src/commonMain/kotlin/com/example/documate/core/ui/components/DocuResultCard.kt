package com.example.documate.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.documate.core.domain.model.DocumentFile

@Composable
fun DocuResultCard(
    resultFile: DocumentFile,
    originalFileSizeBytes: Long? = null,
    onPreview: () -> Unit,
    onShare: () -> Unit,
    onConvertAnother: () -> Unit,
    modifier: Modifier = Modifier,
    onSaveToStorage: (() -> Unit)? = null
) {
    DocuCard(modifier = modifier.fillMaxWidth(), padding = 16.dp) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Conversion Complete!",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = resultFile.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Size: ${resultFile.formattedSize}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                if (originalFileSizeBytes != null && originalFileSizeBytes > resultFile.sizeBytes) {
                    val saved = kotlin.math.round(((originalFileSizeBytes - resultFile.sizeBytes).toDouble() / originalFileSizeBytes) * 100).toLong()
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "-$saved% reduced",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (onSaveToStorage != null) {
                DocuPrimaryButton(
                    text = "Save to Device",
                    onClick = onSaveToStorage,
                    icon = Icons.Default.SaveAlt,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DocuSecondaryButton(
                    text = "Preview",
                    onClick = onPreview,
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    modifier = Modifier.weight(1f)
                )
                DocuSecondaryButton(
                    text = "Share",
                    onClick = onShare,
                    icon = Icons.Default.Share,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            DocuSecondaryButton(
                text = "Convert Another",
                onClick = onConvertAnother,
                icon = Icons.Default.Refresh,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
