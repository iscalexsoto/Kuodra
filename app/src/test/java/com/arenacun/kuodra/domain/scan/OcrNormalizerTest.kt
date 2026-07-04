package com.arenacun.kuodra.domain.scan

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrNormalizerTest {

    @Test
    fun `trims lines and collapses inner spaces`() {
        assertEquals(
            "OXXO TIENDA 123\nCOCA 600ML 19.00",
            OcrNormalizer.normalize("  OXXO   TIENDA 123  \nCOCA  600ML   19.00"),
        )
    }

    @Test
    fun `compacts amounts split by the OCR`() {
        assertEquals("$1,234.56", OcrNormalizer.normalize("$ 1 , 234 . 56"))
        assertEquals("TOTAL $187.50", OcrNormalizer.normalize("TOTAL $ 187 . 50"))
    }

    @Test
    fun `collapses repeated blank lines and trims edges`() {
        assertEquals(
            "OXXO\n\nTOTAL 10.00",
            OcrNormalizer.normalize("\n\nOXXO\n\n\n\nTOTAL 10.00\n\n"),
        )
    }

    @Test
    fun `unifies unicode dashes and quotes`() {
        assertEquals("PROMO - 'DOS' \"X1\"", OcrNormalizer.normalize("PROMO – ‘DOS’ “X1”"))
    }

    @Test
    fun `compacts date separators split by the OCR`() {
        assertEquals("30/06/2026 21:43", OcrNormalizer.normalize("30/06/ 2026 21:43"))
        assertEquals("01-02-2026", OcrNormalizer.normalize("01 - 02 - 2026"))
    }

    @Test
    fun `fixes zero-for-O confusion inside words`() {
        assertEquals("CAMBIO 40.50", OcrNormalizer.normalize("CAMBI0 40.50"))
        assertEquals("OXXO", OcrNormalizer.normalize("0XXO"))
        // Los números de verdad no se tocan.
        assertEquals("COCA 600ML 100.00", OcrNormalizer.normalize("COCA 600ML 100.00"))
    }

    @Test
    fun `does not glue separate words`() {
        assertEquals("CAMBIO 0.00 GRACIAS", OcrNormalizer.normalize("CAMBIO 0.00 GRACIAS"))
    }

    @Test
    fun `empty input stays empty`() {
        assertEquals("", OcrNormalizer.normalize(""))
        assertEquals("", OcrNormalizer.normalize("\n \n"))
    }
}
