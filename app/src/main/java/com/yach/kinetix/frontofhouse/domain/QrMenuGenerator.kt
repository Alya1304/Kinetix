package com.yach.kinetix.frontofhouse.domain

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrMenuGenerator @Inject constructor() {

    /**
     * Genera el payload de datos o URL formateada para el código QR de la carta digital del restaurante.
     * Permite a los comensales visualizar el catálogo desde su navegador móvil sin instalar aplicaciones.
     */
    fun generateMenuQrPayload(baseUrl: String, restaurantCode: String = "default"): String {
        return "$baseUrl/menu?restaurant=$restaurantCode"
    }

    fun generateTableQrPayload(baseUrl: String, tableNumber: String): String {
        return "$baseUrl/menu?mesa=$tableNumber"
    }
}
