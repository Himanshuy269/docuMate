package com.example.documate.core.domain.model

/**
 * Supported PDF page dimensions.
 */
enum class PdfPageSize(val displayName: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (210 × 297 mm)", 595, 842),
    LETTER("Letter (8.5 × 11 in)", 612, 792),
    ORIGINAL("Original Dimensions", 0, 0)
}

/**
 * PDF page orientation.
 */
enum class PdfOrientation(val displayName: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape")
}

/**
 * Presets for document compression.
 */
enum class CompressionPreset(
    val title: String,
    val description: String,
    val targetQualityPercent: Int,
    val imageDpi: Int
) {
    LOW(
        title = "Low Compression",
        description = "High quality images, slight file size reduction",
        targetQualityPercent = 85,
        imageDpi = 200
    ),
    MEDIUM(
        title = "Recommended",
        description = "Balanced quality and noticeable size reduction",
        targetQualityPercent = 65,
        imageDpi = 150
    ),
    HIGH(
        title = "High Compression",
        description = "Smallest file size, lower image resolution",
        targetQualityPercent = 40,
        imageDpi = 96
    )
}
