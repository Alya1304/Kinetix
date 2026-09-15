package com.yach.kinetix.core.finance

import java.math.BigDecimal
import java.math.RoundingMode

enum class DiscountType {
    NONE,
    FIXED,
    PERCENTAGE
}

data class TaxBreakdown(
    val originalPrice: BigDecimal,
    val discountAmount: BigDecimal,
    val finalTotal: BigDecimal,
    val baseAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val taxRate: BigDecimal
)

object TaxCalculator {

    private const val DISPLAY_SCALE = 2
    private val DEFAULT_ROUNDING = RoundingMode.HALF_UP

    /**
     * Calcula el Desglose Inverso (Tax-Inclusive Pricing) con soporte opcional de descuento
     * fijo o porcentual aplicado sobre el precio final.
     *
     * Fórmula:
     *   Precio Con Descuento = Max(0, Precio Original - Descuento)
     *   Base = Precio Con Descuento / (1 + Tasa)
     *   Impuesto = Precio Con Descuento - Base
     */
    fun calculateInverseTax(
        finalPriceWithTax: BigDecimal,
        taxRate: BigDecimal,
        discountType: DiscountType = DiscountType.NONE,
        discountValue: BigDecimal = BigDecimal.ZERO
    ): TaxBreakdown {
        require(finalPriceWithTax >= BigDecimal.ZERO) { "El precio final no puede ser negativo" }
        require(taxRate >= BigDecimal.ZERO) { "La tasa de impuesto no puede ser negativa" }

        val discountAmount = when (discountType) {
            DiscountType.NONE -> BigDecimal.ZERO
            DiscountType.FIXED -> discountValue.min(finalPriceWithTax)
            DiscountType.PERCENTAGE -> {
                val rate = discountValue.divide(BigDecimal("100"), 4, DEFAULT_ROUNDING)
                finalPriceWithTax.multiply(rate).setScale(DISPLAY_SCALE, DEFAULT_ROUNDING).min(finalPriceWithTax)
            }
        }

        val priceAfterDiscount = finalPriceWithTax.subtract(discountAmount).max(BigDecimal.ZERO)
        val divisor = BigDecimal.ONE.add(taxRate)

        // Base calculada hacia atras con redondeo HALF_UP
        val baseAmount = priceAfterDiscount.divide(divisor, DISPLAY_SCALE, DEFAULT_ROUNDING)

        // El impuesto es la diferencia exacta entre el importe cobrado y la base
        val taxAmount = priceAfterDiscount.subtract(baseAmount)

        return TaxBreakdown(
            originalPrice = finalPriceWithTax.setScale(DISPLAY_SCALE, DEFAULT_ROUNDING),
            discountAmount = discountAmount.setScale(DISPLAY_SCALE, DEFAULT_ROUNDING),
            finalTotal = priceAfterDiscount.setScale(DISPLAY_SCALE, DEFAULT_ROUNDING),
            baseAmount = baseAmount,
            taxAmount = taxAmount.setScale(DISPLAY_SCALE, DEFAULT_ROUNDING),
            taxRate = taxRate
        )
    }

    /**
     * Calcula el subtotal, base e impuesto para una linea de orden con modificadores y cantidad.
     */
    fun calculateLineItemBreakdown(
        unitPriceWithTax: BigDecimal,
        modifiersExtraWithTax: BigDecimal,
        quantity: BigDecimal,
        taxRate: BigDecimal,
        discountType: DiscountType = DiscountType.NONE,
        discountValue: BigDecimal = BigDecimal.ZERO
    ): TaxBreakdown {
        val totalUnit = unitPriceWithTax.add(modifiersExtraWithTax)
        val lineTotal = totalUnit.multiply(quantity).setScale(DISPLAY_SCALE, DEFAULT_ROUNDING)
        return calculateInverseTax(lineTotal, taxRate, discountType, discountValue)
    }
}
