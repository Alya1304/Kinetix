package com.yach.kinetix.core.hardware

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbPrinterService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager

    /**
     * Envía bytes ESC/POS a una impresora térmica conectada por USB OTG.
     */
    suspend fun printUsb(device: UsbDevice, ticketBytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val manager = usbManager ?: throw IllegalStateException("UsbManager no disponible")
            if (!manager.hasPermission(device)) {
                throw SecurityException("Permiso de USB no concedido para el dispositivo")
            }

            var printerInterface: UsbInterface? = null
            var outEndpoint: UsbEndpoint? = null

            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (iface.interfaceClass == UsbConstants.USB_CLASS_PRINTER || iface.interfaceClass == UsbConstants.USB_CLASS_COMM) {
                    for (j in 0 until iface.endpointCount) {
                        val ep = iface.getEndpoint(j)
                        if (ep.direction == UsbConstants.USB_DIR_OUT) {
                            printerInterface = iface
                            outEndpoint = ep
                            break
                        }
                    }
                }
            }

            val targetInterface = printerInterface ?: throw IOException("Interfaz de impresora USB no encontrada")
            val targetEndpoint = outEndpoint ?: throw IOException("Endpoint OUT de impresora USB no encontrado")

            val connection = manager.openDevice(device) ?: throw IOException("No fue posible abrir conexión USB")
            try {
                connection.claimInterface(targetInterface, true)
                val bytesSent = connection.bulkTransfer(targetEndpoint, ticketBytes, ticketBytes.size, 5000)
                if (bytesSent < 0) {
                    throw IOException("Error en transferencia de bytes USB a impresora")
                }
            } finally {
                connection.releaseInterface(targetInterface)
                connection.close()
            }
        }
    }
}
