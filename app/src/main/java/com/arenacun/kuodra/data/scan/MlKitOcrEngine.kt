package com.arenacun.kuodra.data.scan

import android.content.Context
import androidx.core.net.toUri
import com.arenacun.kuodra.domain.scan.OcrEngine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * OCR on-device con MLKit Text Recognition (modelo latino **bundled**: funciona sin red y sin
 * Play Services, que es lo que hace viable el fallback regex offline).
 * `InputImage.fromFilePath` acepta tanto el `file://` de CameraX como el `content://` del Photo
 * Picker y respeta la rotación EXIF: un solo camino de entrada al pipeline.
 *
 * El texto NO se toma de `result.text` (bloques en orden arbitrario que rompen la asociación
 * concepto↔monto en tickets de dos columnas) sino de [reconstructRows], que reordena las líneas
 * por geometría al orden de lectura real.
 */
class MlKitOcrEngine(private val context: Context) : OcrEngine {

    override suspend fun recognize(imageUri: String): Result<String> = runCatching {
        val image = InputImage.fromFilePath(context, imageUri.toUri())
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result = try {
            recognizer.process(image).await()
        } finally {
            recognizer.close()
        }
        val lines = result.textBlocks.flatMap { block -> block.lines }.mapNotNull { line ->
            line.boundingBox?.let { box -> OcrLine(line.text, box.top, box.bottom, box.left) }
        }
        if (lines.isEmpty()) result.text else reconstructRows(lines)
    }
}
