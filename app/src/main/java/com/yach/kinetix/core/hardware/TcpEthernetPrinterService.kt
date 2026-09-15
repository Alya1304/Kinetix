package com.yach.kinetix.core.hardware

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TcpEthernetPrinterService @Inject constructor() {

    private val defaultPort = 9100 // Puerto estándar RAW ESC/POS de impresoras de red
    private val socketTimeoutMs = 3500
    private val maxRetries = 2
    private val retryDelayMs = 400L

    /**
     * Imprime una secuencia de bytes ESC/POS directamente a una impresora conectada por cable Ethernet o Wi-Fi
     * incorporando política de reintentos automáticos ante congestión momentánea de red.
     */
    suspend fun printOverTcp(ipAddress: String, ticketBytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        var lastException: Throwable? = null

        for (attempt in 0..maxRetries) {
            try {
                Socket().use { socket ->
                    socket.soTimeout = socketTimeoutMs
                    socket.connect(InetSocketAddress(ipAddress, defaultPort), socketTimeoutMs)
                    val out = socket.getOutputStream()
                    out.write(ticketBytes)
                    out.flush()
                }
                return@withContext Result.success(Unit)
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    delay(retryDelayMs)
                }
            }
        }

        Result.failure(lastException ?: IOException("Error desconocido al imprimir vía TCP en $ipAddress"))
    }
}
