package com.example.documate.core.domain.model

/**
 * Category grouping for tools on the Home Screen.
 */
enum class ToolCategory(val title: String) {
    PDF("PDF Tools"),
    OFFICE("Office to PDF"),
    PDF_TO_OFFICE("PDF to Office")
}

/**
 * The core document utility tools supported by DocuKit/DocuMate.
 */
enum class DocumentTool(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: ToolCategory,
    val supportedExtensions: List<String>,
    val route: String
) {
    IMAGES_TO_PDF(
        id = "images_to_pdf",
        title = "Images to PDF",
        subtitle = "Combine photos into a neat PDF document",
        category = ToolCategory.PDF,
        supportedExtensions = listOf("jpg", "jpeg", "png", "webp"),
        route = "images_to_pdf"
    ),
    ADD_IMAGES_TO_PDF(
        id = "add_images_to_pdf",
        title = "Add Images to PDF",
        subtitle = "Insert pages or pictures into existing PDF",
        category = ToolCategory.PDF,
        supportedExtensions = listOf("pdf", "jpg", "jpeg", "png"),
        route = "add_images_to_pdf"
    ),
    PDF_COMPRESSION(
        id = "pdf_compressor",
        title = "PDF Compression",
        subtitle = "Reduce PDF file size while preserving clarity",
        category = ToolCategory.PDF,
        supportedExtensions = listOf("pdf"),
        route = "pdf_compressor"
    ),
    EXCEL_TO_PDF(
        id = "excel_to_pdf",
        title = "Excel to PDF",
        subtitle = "Convert .xlsx and .xls sheets to PDF",
        category = ToolCategory.OFFICE,
        supportedExtensions = listOf("xlsx", "xls"),
        route = "excel_to_pdf"
    ),
    WORD_TO_PDF(
        id = "word_to_pdf",
        title = "Word to PDF",
        subtitle = "Convert .docx and .doc files to PDF",
        category = ToolCategory.OFFICE,
        supportedExtensions = listOf("docx", "doc"),
        route = "word_to_pdf"
    ),
    POWERPOINT_TO_PDF(
        id = "powerpoint_to_pdf",
        title = "PowerPoint to PDF",
        subtitle = "Convert .pptx and .ppt presentations to PDF",
        category = ToolCategory.OFFICE,
        supportedExtensions = listOf("pptx", "ppt"),
        route = "powerpoint_to_pdf"
    ),
    PDF_TO_WORD(
        id = "pdf_to_word",
        title = "PDF to Word",
        subtitle = "Convert .pdf documents to editable .docx files",
        category = ToolCategory.PDF_TO_OFFICE,
        supportedExtensions = listOf("pdf"),
        route = "pdf_to_word"
    ),
    PDF_TO_EXCEL(
        id = "pdf_to_excel",
        title = "PDF to Excel",
        subtitle = "Extract tables from .pdf into .xlsx spreadsheets",
        category = ToolCategory.PDF_TO_OFFICE,
        supportedExtensions = listOf("pdf"),
        route = "pdf_to_excel"
    ),
    PDF_TO_POWERPOINT(
        id = "pdf_to_powerpoint",
        title = "PDF to PowerPoint",
        subtitle = "Turn .pdf slides into editable .pptx presentations",
        category = ToolCategory.PDF_TO_OFFICE,
        supportedExtensions = listOf("pdf"),
        route = "pdf_to_powerpoint"
    );

    companion object {
        val COMPRESS_PDF: DocumentTool get() = PDF_COMPRESSION
        val PDF_COMPRESSOR: DocumentTool get() = PDF_COMPRESSION
        fun fromId(id: String): DocumentTool? = entries.find { it.id == id }
    }
}
