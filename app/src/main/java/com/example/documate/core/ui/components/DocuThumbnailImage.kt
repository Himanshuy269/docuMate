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

/**
 * Memory-safe asynchronous thumbnail image loader.
 * Employs two-pass sub-sampled bitmap decoding and EXIF rotation without third-party dependencies.
 */
@Composable
fun DocuThumbnailImage(
    uriString: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(uriString) { mutableStateOf(true) }

    LaunchedEffect(uriString) {
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                val targetPx = (size.value * context.resources.displayMetrics.density).toInt().coerceAtLeast(80)

                // 1. Decode bounds
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, boundsOptions)
                }

                if (boundsOptions.outWidth > 0 && boundsOptions.outHeight > 0) {
                    // 2. Compute inSampleSize
                    var sample = 1
                    val rawWidth = boundsOptions.outWidth
                    val rawHeight = boundsOptions.outHeight
                    while ((rawWidth / (sample * 2)) >= targetPx && (rawHeight / (sample * 2)) >= targetPx) {
                        sample *= 2
                    }

                    // 3. Decode scaled bitmap
                    val decodeOptions = BitmapFactory.Options().apply {
                        inJustDecodeBounds = false
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }

                    val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input, null, decodeOptions)
                    }

                    // 4. Resolve EXIF rotation if possible
                    val finalBmp = if (decoded != null) {
                        val rotation = try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                    val exif = ExifInterface(stream)
                                    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                                        else -> 0
                                    }
                                } ?: 0
                            } else 0
                        } catch (_: Exception) {
                            0
                        }

                        if (rotation != 0) {
                            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                            val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                            if (rotated != decoded) decoded.recycle()
                            rotated
                        } else {
                            decoded
                        }
                    } else null

                    bitmap = finalBmp
                }
            } catch (_: Exception) {
                bitmap = null
            } finally {
                isLoading = false
            }
        }
    }

    val shape = RoundedCornerShape(8.dp)
    val displayBitmap = bitmap

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center
    ) {
        if (displayBitmap != null) {
            Image(
                bitmap = displayBitmap.asImageBitmap(),
                contentDescription = "Thumbnail",
                modifier = Modifier.size(size),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Image preview",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(size / 2)
            )
        }
    }
}
