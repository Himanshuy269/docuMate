package com.example.documate.core.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.os.ParcelFileDescriptor
import com.example.documate.core.common.AppResult
import com.example.documate.core.domain.model.CompressionPreset
import com.example.documate.core.domain.model.PdfMargin
import com.example.documate.core.domain.model.PdfOrientation
import com.example.documate.core.domain.model.PdfPageSize
import com.example.documate.core.domain.repository.PdfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Functional repository implementation for PDF operations using native Android graphics and PDF engines.
 * Employs two-pass memory-safe bitmap decoding, EXIF orientation correction, and app-private isolated storage.
 */
class DefaultPdfRepository(
    private val context: Context
) : PdfRepository {

    override suspend fun createPdfFromImages(
        imageFiles: List<File>,
        pageSize: PdfPageSize,
        orientation: PdfOrientation,
        outputFileName: String,
        margin: PdfMargin,
        onProgress: (Float) -> Unit
    ): AppResult<File> = withContext(Dispatchers.IO) {
        if (imageFiles.isEmpty()) {
            return@withContext AppResult.Error("No image files provided for PDF creation")
        }

        val pdfDocument = PdfDocument()
        try {
            val totalImages = imageFiles.size
            val paint = Paint().apply {
                isFilterBitmap = true
                isDither = true
            }

            for ((index, file) in imageFiles.withIndex()) {
                if (!file.exists() || file.length() == 0L) {
                    continue
                }

                // 1. Inspect EXIF orientation
                val rotationDegrees = getExifRotation(file)

                // 2. Decode image bounds without allocating pixel memory
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
                val rawWidth = boundsOptions.outWidth
                val rawHeight = boundsOptions.outHeight

                if (rawWidth <= 0 || rawHeight <= 0) {
                    continue
                }

                // Effective dimensions after EXIF rotation
                val isRotated90or270 = rotationDegrees == 90 || rotationDegrees == 270
                val orientedWidth = if (isRotated90or270) rawHeight else rawWidth
                val orientedHeight = if (isRotated90or270) rawWidth else rawHeight

                // 3. Determine target PDF page dimensions in points (72 pt = 1 inch)
                val (pageWidth, pageHeight) = calculatePageDimensions(
                    pageSize = pageSize,
                    orientation = orientation,
                    imageWidth = orientedWidth,
                    imageHeight = orientedHeight
                )

                val marginPt = margin.marginPt
                val availableWidth = max(1, pageWidth - (2 * marginPt))
                val availableHeight = max(1, pageHeight - (2 * marginPt))

                // 4. Memory-safe subsampling calculation
                // Aim for max 1800-2400 pixels on long edge to avoid OOM while preserving crisp print quality
                val targetResolutionWidth = min(orientedWidth, max(pageWidth * 2, 1200))
                val targetResolutionHeight = min(orientedHeight, max(pageHeight * 2, 1600))

                val sampleSize = calculateInSampleSize(
                    rawWidth = rawWidth,
                    rawHeight = rawHeight,
                    reqWidth = targetResolutionWidth,
                    reqHeight = targetResolutionHeight
                )

                // 5. Decode pixel bitmap with calculated sample size
                val decodeOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val rawBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                    ?: continue

                // 6. Apply EXIF rotation if needed
                val finalBitmap = if (rotationDegrees != 0) {
                    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                    val rotated = Bitmap.createBitmap(
                        rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
                    )
                    if (rotated != rawBitmap) {
                        rawBitmap.recycle()
                    }
                    rotated
                } else {
                    rawBitmap
                }

                // 7. Calculate destination drawing rect preserving aspect ratio
                val scale = min(
                    availableWidth.toFloat() / finalBitmap.width,
                    availableHeight.toFloat() / finalBitmap.height
                )
                val destWidth = finalBitmap.width * scale
                val destHeight = finalBitmap.height * scale
                val destLeft = marginPt + (availableWidth - destWidth) / 2f
                val destTop = marginPt + (availableHeight - destHeight) / 2f
                val destRect = RectF(destLeft, destTop, destLeft + destWidth, destTop + destHeight)

                // 8. Render page into PdfDocument
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(finalBitmap, null, destRect, paint)
                pdfDocument.finishPage(page)

                // 9. Immediately recycle bitmap to free memory
                finalBitmap.recycle()

                // Report progress
                onProgress((index + 1).toFloat() / totalImages)
            }

            // 10. Write completed PDF to app-private storage
            val outputDir = File(context.filesDir, "generated_pdfs").apply { mkdirs() }
            val cleanName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) {
                outputFileName
            } else {
                "$outputFileName.pdf"
            }
            val outputFile = File(outputDir, cleanName)

            FileOutputStream(outputFile).use { outStream ->
                pdfDocument.writeTo(outStream)
            }

            AppResult.Success(outputFile)
        } catch (e: Exception) {
            AppResult.Error("Images to PDF conversion failed: ${e.message}", e)
        } finally {
            try {
                pdfDocument.close()
            } catch (_: Exception) {}
        }
    }

    override suspend fun addImagesToPdf(
        existingPdfFile: File,
        imageFiles: List<File>,
        insertPositionPage: Int,
        outputFileName: String,
        onProgress: (Float) -> Unit
    ): AppResult<File> = withContext(Dispatchers.IO) {
        if (!existingPdfFile.exists() || existingPdfFile.length() == 0L) {
            return@withContext AppResult.Error("Source PDF does not exist or is empty")
        }
        if (imageFiles.isEmpty()) {
            return@withContext AppResult.Error("No image files provided for insertion")
        }

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val pdfDocument = PdfDocument()

        try {
            pfd = ParcelFileDescriptor.open(existingPdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = try {
                PdfRenderer(pfd)
            } catch (e: SecurityException) {
                return@withContext AppResult.Error("Source PDF is password-protected or encrypted", e)
            }

            val existingPageCount = renderer.pageCount
            if (existingPageCount <= 0) {
                return@withContext AppResult.Error("Source PDF contains no readable pages")
            }

            // Determine insert index (0-based)
            // insertPositionPage: 0 -> beginning; -1 -> end; 1..pageCount -> after page N
            val insertIndex = when {
                insertPositionPage <= 0 && insertPositionPage != -1 -> 0
                insertPositionPage == -1 || insertPositionPage >= existingPageCount -> existingPageCount
                else -> insertPositionPage
            }

            val totalOperations = existingPageCount + imageFiles.size
            var currentOp = 0
            var newPageNumber = 1

            val paint = Paint().apply {
                isFilterBitmap = true
                isDither = true
            }

            // Reference dimensions from first page of existing document
            val firstPage = renderer.openPage(0)
            val defaultWidth = firstPage.width
            val defaultHeight = firstPage.height
            firstPage.close()

            // 1. Existing pages before insertIndex
            for (i in 0 until insertIndex) {
                val page = renderer.openPage(i)
                renderPdfPageToDocument(pdfDocument, page, newPageNumber++, paint)
                page.close()
                currentOp++
                onProgress(currentOp.toFloat() / totalOperations)
            }

            // 2. Insert new image pages
            for (imageFile in imageFiles) {
                renderImagePageToDocument(
                    pdfDocument = pdfDocument,
                    imageFile = imageFile,
                    targetPageNumber = newPageNumber++,
                    defaultWidth = defaultWidth,
                    defaultHeight = defaultHeight,
                    paint = paint
                )
                currentOp++
                onProgress(currentOp.toFloat() / totalOperations)
            }

            // 3. Existing pages after insertIndex
            for (i in insertIndex until existingPageCount) {
                val page = renderer.openPage(i)
                renderPdfPageToDocument(pdfDocument, page, newPageNumber++, paint)
                page.close()
                currentOp++
                onProgress(currentOp.toFloat() / totalOperations)
            }

            val outputDir = File(context.filesDir, "generated_pdfs").apply { mkdirs() }
            val cleanName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) {
                outputFileName
            } else {
                "$outputFileName.pdf"
            }
            val outputFile = File(outputDir, cleanName)

            FileOutputStream(outputFile).use { outStream ->
                pdfDocument.writeTo(outStream)
            }

            AppResult.Success(outputFile)
        } catch (e: Exception) {
            AppResult.Error("Failed to insert images into PDF: ${e.message}", e)
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
            try { pdfDocument.close() } catch (_: Exception) {}
        }
    }

    private data class CompressedPdfPage(
        val widthPt: Int,
        val heightPt: Int,
        val renderWidth: Int,
        val renderHeight: Int,
        val jpegBytes: ByteArray
    )

    override suspend fun compressPdf(
        sourcePdfFile: File,
        preset: CompressionPreset,
        outputFileName: String,
        onProgress: (Float) -> Unit
    ): AppResult<File> = withContext(Dispatchers.IO) {
        if (!sourcePdfFile.exists() || sourcePdfFile.length() == 0L) {
            return@withContext AppResult.Error("Source PDF file does not exist or is empty")
        }

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            pfd = ParcelFileDescriptor.open(sourcePdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = try {
                PdfRenderer(pfd)
            } catch (e: SecurityException) {
                return@withContext AppResult.Error("Source PDF is password-protected or encrypted", e)
            }

            val pageCount = renderer.pageCount
            if (pageCount <= 0) {
                return@withContext AppResult.Error("Source PDF contains no readable pages")
            }

            val quality = preset.targetQualityPercent.coerceIn(1, 100)
            val targetDpi = preset.imageDpi.toFloat()
            // Standard PDF base DPI is 72 pt/inch
            val rawScaleFactor = targetDpi / 72f

            val pages = mutableListOf<CompressedPdfPage>()

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val widthPt = page.width
                val heightPt = page.height

                // Cap long edge to 2048 px to avoid memory bloat on large pages
                val maxDim = max(widthPt, heightPt).toFloat()
                val scaleFactor = if (maxDim * rawScaleFactor > 2048f) {
                    (2048f / maxDim).coerceIn(0.5f, 2.0f)
                } else {
                    rawScaleFactor.coerceIn(0.5f, 2.0f)
                }

                val renderWidth = (widthPt * scaleFactor).toInt().coerceAtLeast(1)
                val renderHeight = (heightPt * scaleFactor).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                // Compress bitmap directly to JPEG bytes stream
                val byteStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteStream)
                bitmap.recycle()

                val jpegBytes = byteStream.toByteArray()
                pages.add(
                    CompressedPdfPage(
                        widthPt = widthPt,
                        heightPt = heightPt,
                        renderWidth = renderWidth,
                        renderHeight = renderHeight,
                        jpegBytes = jpegBytes
                    )
                )

                onProgress(((i + 1).toFloat() / pageCount) * 0.9f)
            }

            val outputDir = File(context.filesDir, "compressed_pdfs").apply { mkdirs() }
            val cleanName = if (outputFileName.endsWith(".pdf", ignoreCase = true)) {
                outputFileName
            } else {
                "$outputFileName.pdf"
            }
            val outputFile = File(outputDir, cleanName)

            // Write PDF embedding the JPEG streams directly (/Filter /DCTDecode)
            writePdfWithJpegStreams(outputFile, pages)

            // Compression safeguard:
            // Compression must NEVER increase the size of the PDF!
            // If the compressed output is not smaller than the source file (e.g. for pure vector/text PDFs),
            // retain the original file so the user never gets an inflated document.
            if (outputFile.exists() && outputFile.length() >= sourcePdfFile.length()) {
                sourcePdfFile.copyTo(outputFile, overwrite = true)
            }

            onProgress(1.0f)

            AppResult.Success(outputFile)
        } catch (e: Exception) {
            AppResult.Error("PDF compression failed: ${e.message}", e)
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    private fun writePdfWithJpegStreams(outputFile: File, pages: List<CompressedPdfPage>) {
        FileOutputStream(outputFile).use { out ->
            var offset = 0L
            fun writeBytes(bytes: ByteArray) {
                out.write(bytes)
                offset += bytes.size
            }
            fun writeAscii(str: String) {
                writeBytes(str.toByteArray(Charsets.US_ASCII))
            }

            val objOffsets = mutableMapOf<Int, Long>()

            // 1. PDF Header
            writeAscii("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n")

            // 2. Catalog (Obj 1)
            objOffsets[1] = offset
            writeAscii("1 0 obj\n<<\n  /Type /Catalog\n  /Pages 2 0 R\n>>\nendobj\n")

            // 3. Pages Root (Obj 2)
            objOffsets[2] = offset
            val kidsArray = pages.indices.joinToString(" ") { k ->
                val pageObjNum = 3 + k * 3
                "$pageObjNum 0 R"
            }
            writeAscii("2 0 obj\n<<\n  /Type /Pages\n  /Kids [$kidsArray]\n  /Count ${pages.size}\n>>\nendobj\n")

            // 4. Per-page objects
            for (k in pages.indices) {
                val page = pages[k]
                val pageObjNum = 3 + k * 3
                val contentObjNum = pageObjNum + 1
                val imageObjNum = pageObjNum + 2
                val imgName = "Im${k + 1}"

                // Page Object
                objOffsets[pageObjNum] = offset
                writeAscii(
                    "$pageObjNum 0 obj\n" +
                    "<<\n" +
                    "  /Type /Page\n" +
                    "  /Parent 2 0 R\n" +
                    "  /MediaBox [0 0 ${page.widthPt} ${page.heightPt}]\n" +
                    "  /Contents $contentObjNum 0 R\n" +
                    "  /Resources <<\n" +
                    "    /ProcSet [/PDF /ImageC]\n" +
                    "    /XObject <<\n" +
                    "      /$imgName $imageObjNum 0 R\n" +
                    "    >>\n" +
                    "  >>\n" +
                    ">>\n" +
                    "endobj\n"
                )

                // Content stream object (draws the image scaled to fit the page MediaBox)
                val contentStream = "q\n${page.widthPt} 0 0 ${page.heightPt} 0 0 cm\n/$imgName Do\nQ\n"
                val contentBytes = contentStream.toByteArray(Charsets.US_ASCII)
                objOffsets[contentObjNum] = offset
                writeAscii(
                    "$contentObjNum 0 obj\n" +
                    "<<\n" +
                    "  /Length ${contentBytes.size}\n" +
                    ">>\n" +
                    "stream\n"
                )
                writeBytes(contentBytes)
                writeAscii("\nendstream\nendobj\n")

                // Image XObject with /Filter /DCTDecode
                objOffsets[imageObjNum] = offset
                writeAscii(
                    "$imageObjNum 0 obj\n" +
                    "<<\n" +
                    "  /Type /XObject\n" +
                    "  /Subtype /Image\n" +
                    "  /Width ${page.renderWidth}\n" +
                    "  /Height ${page.renderHeight}\n" +
                    "  /ColorSpace /DeviceRGB\n" +
                    "  /BitsPerComponent 8\n" +
                    "  /Filter /DCTDecode\n" +
                    "  /Length ${page.jpegBytes.size}\n" +
                    ">>\n" +
                    "stream\n"
                )
                writeBytes(page.jpegBytes)
                writeAscii("\nendstream\nendobj\n")
            }

            // 5. Cross-reference Table (xref)
            val xrefOffset = offset
            val totalObjs = 2 + pages.size * 3
            writeAscii("xref\n0 ${totalObjs + 1}\n")
            writeAscii("0000000000 65535 f \n")
            for (objNum in 1..totalObjs) {
                val objOff = objOffsets[objNum] ?: 0L
                val entry = String.format(java.util.Locale.US, "%010d 00000 n \n", objOff)
                writeAscii(entry)
            }

            // 6. Trailer
            writeAscii(
                "trailer\n" +
                "<<\n" +
                "  /Size ${totalObjs + 1}\n" +
                "  /Root 1 0 R\n" +
                ">>\n" +
                "startxref\n" +
                "$xrefOffset\n" +
                "%%EOF\n"
            )
            out.flush()
        }
    }

    override suspend fun getPdfPageCount(pdfFile: File): AppResult<Int> = withContext(Dispatchers.IO) {
        if (!pdfFile.exists() || pdfFile.length() == 0L) {
            return@withContext AppResult.Error("PDF file does not exist or is empty")
        }
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            AppResult.Success(renderer.pageCount)
        } catch (e: Exception) {
            AppResult.Error("Failed to inspect PDF pages: ${e.message}", e)
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    // --- Private Helper Functions for Memory Safety & Geometry ---

    private fun renderPdfPageToDocument(
        pdfDocument: PdfDocument,
        rendererPage: PdfRenderer.Page,
        targetPageNumber: Int,
        paint: Paint
    ) {
        val width = rendererPage.width
        val height = rendererPage.height

        val scale = min(2.0f, max(1.0f, 2000f / max(width, height)))
        val bmpWidth = (width * scale).toInt().coerceAtLeast(1)
        val bmpHeight = (height * scale).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        rendererPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

        val pageInfo = PdfDocument.PageInfo.Builder(width, height, targetPageNumber).create()
        val newPage = pdfDocument.startPage(pageInfo)
        newPage.canvas.drawColor(Color.WHITE)
        val destRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        newPage.canvas.drawBitmap(bitmap, null, destRect, paint)
        pdfDocument.finishPage(newPage)

        bitmap.recycle()
    }

    private fun renderImagePageToDocument(
        pdfDocument: PdfDocument,
        imageFile: File,
        targetPageNumber: Int,
        defaultWidth: Int = 595,
        defaultHeight: Int = 842,
        paint: Paint
    ): Boolean {
        if (!imageFile.exists() || imageFile.length() == 0L) return false

        val rotationDegrees = getExifRotation(imageFile)

        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(imageFile.absolutePath, boundsOptions)
        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) return false

        val isRotated90or270 = rotationDegrees == 90 || rotationDegrees == 270
        val orientedWidth = if (isRotated90or270) rawHeight else rawWidth
        val orientedHeight = if (isRotated90or270) rawWidth else rawHeight

        val pageWidth = defaultWidth
        val pageHeight = defaultHeight

        val sampleSize = calculateInSampleSize(
            rawWidth = rawWidth,
            rawHeight = rawHeight,
            reqWidth = min(orientedWidth, max(pageWidth * 2, 1200)),
            reqHeight = min(orientedHeight, max(pageHeight * 2, 1600))
        )

        val decodeOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val rawBitmap = BitmapFactory.decodeFile(imageFile.absolutePath, decodeOptions) ?: return false

        val finalBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated = Bitmap.createBitmap(
                rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
            )
            if (rotated != rawBitmap) {
                rawBitmap.recycle()
            }
            rotated
        } else {
            rawBitmap
        }

        val scale = min(
            pageWidth.toFloat() / finalBitmap.width,
            pageHeight.toFloat() / finalBitmap.height
        )
        val destWidth = finalBitmap.width * scale
        val destHeight = finalBitmap.height * scale
        val destLeft = (pageWidth - destWidth) / 2f
        val destTop = (pageHeight - destHeight) / 2f
        val destRect = RectF(destLeft, destTop, destLeft + destWidth, destTop + destHeight)

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, targetPageNumber).create()
        val page = pdfDocument.startPage(pageInfo)
        page.canvas.drawColor(Color.WHITE)
        page.canvas.drawBitmap(finalBitmap, null, destRect, paint)
        pdfDocument.finishPage(page)

        finalBitmap.recycle()
        return true
    }

    private fun getExifRotation(file: File): Int {
        return try {
            val exifInterface = ExifInterface(file.absolutePath)
            when (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Exception) {
            0
        }
    }

    private fun calculatePageDimensions(
        pageSize: PdfPageSize,
        orientation: PdfOrientation,
        imageWidth: Int,
        imageHeight: Int
    ): Pair<Int, Int> {
        return when (pageSize) {
            PdfPageSize.ORIGINAL -> {
                // Scale original image pixels to standard point scale (approx 72 DPI, capped to max dimension)
                val maxDim = 842.0 // max A4 height in pt
                val imgMax = max(imageWidth, imageHeight).toDouble()
                val scale = if (imgMax > maxDim) maxDim / imgMax else 1.0
                val w = (imageWidth * scale).toInt().coerceAtLeast(100)
                val h = (imageHeight * scale).toInt().coerceAtLeast(100)
                if (orientation == PdfOrientation.PORTRAIT) {
                    if (w > h) Pair(h, w) else Pair(w, h)
                } else {
                    if (w < h) Pair(h, w) else Pair(w, h)
                }
            }
            PdfPageSize.A4 -> {
                if (orientation == PdfOrientation.PORTRAIT) {
                    Pair(595, 842)
                } else {
                    Pair(842, 595)
                }
            }
            PdfPageSize.LETTER -> {
                if (orientation == PdfOrientation.PORTRAIT) {
                    Pair(612, 792)
                } else {
                    Pair(792, 612)
                }
            }
        }
    }

    private fun calculateInSampleSize(
        rawWidth: Int,
        rawHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (rawHeight > reqHeight || rawWidth > reqWidth) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
