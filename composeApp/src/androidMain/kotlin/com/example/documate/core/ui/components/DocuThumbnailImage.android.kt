package com.example.documate.core.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun DocuThumbnailImage(
    uriString: String,
    modifier: Modifier,
    size: Dp
) {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(uriString) { mutableStateOf(true) }

    LaunchedEffect(uriString) {
        withContext(Dispatchers.IO) {
            try {
                val targetPx = (size.value * context.resources.displayMetrics.density).toInt().coerceAtLeast(80)

                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                if (uriString.startsWith("content://")) {
                    val uri = Uri.parse(uriString)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input, null, boundsOptions)
                    }
                } else {
                    BitmapFactory.decodeFile(uriString, boundsOptions)
                }

                val outW = boundsOptions.outWidth
                val outH = boundsOptions.outHeight
                if (outW > 0 && outH > 0) {
                    var sampleSize = 1
                    while ((outW / sampleSize) > targetPx * 2 || (outH / sampleSize) > targetPx * 2) {
                        sampleSize *= 2
                    }
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }

                    var rawBitmap: Bitmap? = if (uriString.startsWith("content://")) {
                        val uri = Uri.parse(uriString)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            BitmapFactory.decodeStream(input, null, decodeOptions)
                        }
                    } else {
                        BitmapFactory.decodeFile(uriString, decodeOptions)
                    }

                    bitmap = rawBitmap
                }
            } catch (_: Exception) {
                bitmap = null
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(size / 2)
            )
        }
    }
}
