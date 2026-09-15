package com.yach.kinetix.core.finance

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class TaxCalculatorTest {

    @Test
    fun calculateInverseTax_matchesSpecificationExample() {
        val finalPrice = BigDecimal("203.00")
        val taxRate = BigDecimal("0.16")

        val breakdown = TaxCalculator.calculateInverseTax(finalPrice, taxRate)

        assertEquals(BigDecimal("203.00"), breakdown.finalTotal)
        assertEquals(BigDecimal("175.00"), breakdown.baseAmount)
        assertEquals(BigDecimal("28.00"), breakdown.taxAmount)
        assertEquals(breakdown.finalTotal, breakdown.baseAmount.add(breakdown.taxAmount))
    }

    @Test
    fun calculateInverseTax_withPercentageDiscount_computesAccurately() {
        // Precio $200.00 con 10% de descuento = $180.00 a pagar.
        // Tasa 16%: Base = 180.00 / 1.16 = 155.17, IVA = 180.00 - 155.17 = 24.83
        val finalPrice = BigDecimal("200.00")
        val taxRate = BigDecimal("0.16")

        val breakdown = TaxCalculator.calculateInverseTax(
            finalPriceWithTax = finalPrice,
            taxRate = taxRate,
            discountType = DiscountType.PERCENTAGE,
            discountValue = BigDecimal("10")
        )

        assertEquals(BigDecimal("20.00"), breakdown.discountAmount)
        assertEquals(BigDecimal("180.00"), breakdown.finalTotal)
        assertEquals(BigDecimal("155.17"), breakdown.baseAmount)
        assertEquals(BigDecimal("24.83"), breakdown.taxAmount)
        assertEquals(BigDecimal("180.00"), breakdown.baseAmount.add(breakdown.taxAmount))
    }

    @Test
    fun calculateInverseTax_withFixedDiscount_computesAccurately() {
        // Precio $150.00 con $50.00 de descuento fijo = $100.00 a pagar.
        // Tasa 16%: Base = 100.00 / 1.16 = 86.21, IVA = 100.00 - 86.21 = 13.79
        val finalPrice = BigDecimal("150.00")
        val taxRate = BigDecimal("0.16")

        val breakdown = TaxCalculator.calculateInverseTax(
            finalPriceWithTax = finalPrice,
            taxRate = taxRate,
            discountType = DiscountType.FIXED,
            discountValue = BigDecimal("50.00")
        )

        assertEquals(BigDecimal("50.00"), breakdown.discountAmount)
        assertEquals(BigDecimal("100.00"), breakdown.finalTotal)
        assertEquals(BigDecimal("86.21"), breakdown.baseAmount)
        assertEquals(BigDecimal("13.79"), breakdown.taxAmount)
    }
}
