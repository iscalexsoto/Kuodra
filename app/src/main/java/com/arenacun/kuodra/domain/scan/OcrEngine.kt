package com.arenacun.kuodra.domain.scan

/**
 * Puerto de OCR: imagen → texto crudo. La implementación (MLKit) vive en `data`; el Uri viaja
 * como `String` para mantener `domain` sin `android.*`. Fakeable en tests.
 */
interface OcrEngine {
    suspend fun recognize(imageUri: String): Result<String>
}
