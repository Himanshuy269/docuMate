package com.example.documate.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
expect fun DocuThumbnailImage(
    uriString: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
)
