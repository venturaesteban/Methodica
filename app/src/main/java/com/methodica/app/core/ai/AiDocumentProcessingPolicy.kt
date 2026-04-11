package com.methodica.app.core.ai

object AiDocumentProcessingPolicy {
    const val MAX_PDF_PAGES_TEXT = 20
    const val MAX_PDF_PAGES_OCR = 5
    const val OCR_RENDER_SCALE = 1.5f

    val SUPPORTED_UPLOAD_MIME_PREFIXES = listOf("text/", "image/")
    val SUPPORTED_UPLOAD_MIME_EXACT = setOf("application/pdf")
    val TEXT_UPLOAD_EXTENSIONS = setOf(
        "txt", "md", "csv", "json", "xml", "yaml", "yml", "html", "htm",
        "kt", "java", "py", "js", "ts", "sql"
    )
    val SUPPORTED_UPLOAD_EXTENSIONS = TEXT_UPLOAD_EXTENSIONS + setOf(
        "png", "jpg", "jpeg", "webp", "bmp", "gif", "pdf"
    )
}
