package com.yach.kinetix.core.hardware

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface PrinterService {
    fun hasRequiredPermissions(): Boolean
    fun isBluetoothAvailable(): Boolean
    fun isConnected(): Boolean
    suspend fun connect(deviceAddress: String): Result<Unit>
    suspend fun printTicket(ticketBytes: ByteArray): Result<Unit>
    fun disconnect()
}

@Singleton
class BluetoothPrinterService @Inject constructor(
    @ApplicationContext private val context: Context
) : PrinterService {

    companion object {
        // UUID estandar de perfil Serial Port Profile (SPP)
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    override fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connectGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            val scanGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            connectGranted && scanGranted
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    override fun isConnected(): Boolean {
        return socket?.isConnected == true
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(deviceAddress: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasRequiredPermissions()) {
                throw SecurityException("Permisos de Bluetooth denegados. Impresión física en modo degradado seguro.")
            }
            if (!isBluetoothAvailable()) {
                throw IllegalStateException("El adaptador de Bluetooth está apagado o no disponible.")
            }

            val device: BluetoothDevice = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                ?: throw IllegalArgumentException("Dispositivo Bluetooth no encontrado: $deviceAddress")

            disconnect()

            val newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            newSocket.connect()

            socket = newSocket
            outputStream = newSocket.outputStream
        }
    }

    override suspend fun printTicket(ticketBytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasRequiredPermissions()) {
                throw SecurityException("Permiso Bluetooth no otorgado. La venta se completará pero la impresión física se omitirá.")
            }
            val stream = outputStream ?: throw IOException("Impresora térmica no conectada")
            stream.write(ticketBytes)
            stream.flush()
        }
    }

    override fun disconnect() {
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            socket?.close()
        } catch (_: Exception) {}
        outputStream = null
        socket = null
    }
}
