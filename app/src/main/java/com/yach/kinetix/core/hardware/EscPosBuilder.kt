package com.yach.kinetix.core.hardware

import java.io.ByteArrayOutputStream
import java.math.BigDecimal

/**
 * Generador de secuencias de bytes crudas bajo el estandar ESC/POS para impresoras termicas de tickets.
 */
class EscPosBuilder {

    private val outputStream = ByteArrayOutputStream()

    companion object {
        val ESC_INIT = byteArrayOf(0x1B, 0x40) // Inicializar impresora
        val ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
        val ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
        val ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
        val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
        val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
        val FEED_LINE = byteArrayOf(0x0A)
        val PAPER_CUT = byteArrayOf(0x1D, 0x56, 0x42, 0x00) // Corte parcial o total
    }

    init {
        outputStream.write(ESC_INIT)
    }

    fun alignLeft(): EscPosBuilder = apply { outputStream.write(ALIGN_LEFT) }
    fun alignCenter(): EscPosBuilder = apply { outputStream.write(ALIGN_CENTER) }
    fun alignRight(): EscPosBuilder = apply { outputStream.write(ALIGN_RIGHT) }
    fun bold(enable: Boolean): EscPosBuilder = apply {
        outputStream.write(if (enable) BOLD_ON else BOLD_OFF)
    }

    fun text(text: String): EscPosBuilder = apply {
        outputStream.write(text.toByteArray(Charsets.ISO_8859_1))
    }

    fun line(text: String): EscPosBuilder = apply {
        text(text)
        feed()
    }

    fun feed(lines: Int = 1): EscPosBuilder = apply {
        repeat(lines) {
            outputStream.write(FEED_LINE)
        }
    }

    fun cutPaper(): EscPosBuilder = apply {
        feed(3)
        outputStream.write(PAPER_CUT)
    }

    fun build(): ByteArray = outputStream.toByteArray()
}
