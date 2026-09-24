package com.yach.kinetix.core.hardware

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class PrinterConnectionType {
    BLUETOOTH, TCP_IP, USB, NONE
}

@Singleton
class HardwareSettingsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("hardware_settings", Context.MODE_PRIVATE)

    var printerConnectionType: PrinterConnectionType
        get() {
            val name = prefs.getString("printer_connection_type", PrinterConnectionType.NONE.name)
            return runCatching { PrinterConnectionType.valueOf(name!!) }.getOrDefault(PrinterConnectionType.NONE)
        }
        set(value) {
            prefs.edit().putString("printer_connection_type", value.name).apply()
        }

    var printerAddress: String
        get() = prefs.getString("printer_address", "") ?: ""
        set(value) {
            prefs.edit().putString("printer_address", value).apply()
        }

    var restaurantName: String
        get() = prefs.getString("restaurant_name", "Kinetix POS") ?: "Kinetix POS"
        set(value) {
            prefs.edit().putString("restaurant_name", value).apply()
        }

    var ticketHeader: String
        get() = prefs.getString("ticket_header", "¡Gracias por su visita!") ?: "¡Gracias por su visita!"
        set(value) {
            prefs.edit().putString("ticket_header", value).apply()
        }
}
