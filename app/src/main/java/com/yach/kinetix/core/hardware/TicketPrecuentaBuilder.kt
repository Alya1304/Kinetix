package com.yach.kinetix.core.hardware

import com.yach.kinetix.frontofhouse.data.model.OrderWithDetails
import java.math.BigDecimal
import java.math.RoundingMode

class TicketPrecuentaBuilder {

    /**
     * Construye los bytecodes ESC/POS para un ticket de Precuenta / Control de Mesa
     * con desglose de impuestos y sugerencia de propinas.
     */
    fun buildPrecuentaTicket(
        businessName: String,
        orderWithDetails: OrderWithDetails,
        tableName: String?
    ): ByteArray {
        val order = orderWithDetails.order
        val builder = EscPosBuilder()

        builder.alignCenter()
            .bold(true)
            .line(businessName)
            .line("*** PRECUENTA / CONTROL DE MESA ***")
            .bold(false)
            .line("NO FISCAL")
            .feed(1)
            .alignLeft()
            .line("Folio: #${order.orderNumber}")
            .line("Mesa: ${tableName ?: "Para Llevar / Mostrador"}")
            .line("Fecha: ${order.createdAt}")
            .line("--------------------------------")

        for (itemWithMods in orderWithDetails.items) {
            val item = itemWithMods.item
            builder.line("${item.quantity} x ${item.productName}")
            builder.alignRight().line("$$${item.subtotalWithTax}").alignLeft()
        }

        builder.line("--------------------------------")
            .alignRight()
            .line("Subtotal Base: $$${order.baseAmount}")
            .line("Impuestos (IVA): $$${order.taxAmount}")
            .bold(true)
            .line("TOTAL CUENTA: $$${order.totalAmount}")
            .bold(false)
            .line("--------------------------------")
            .alignCenter()
            .line("Propinas Sugeridas:")

        val total = order.totalAmount
        val tip10 = total.multiply(BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP)
        val tip15 = total.multiply(BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP)
        val tip20 = total.multiply(BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP)

        builder.line("10% ($$tip10) | 15% ($$tip15) | 20% ($$tip20)")
            .feed(1)
            .line("¡Gracias por su visita!")
            .cutPaper()

        return builder.build()
    }
}
