package com.yach.kinetix.frontofhouse.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class RecipeAndCreditTest {

    @Test
    fun calculateRecipeIngredientConsumption_multipliesCorrectly() {
        // Platillo: Hamburguesa doble requiere:
        // 0.200 kg de carne molida por porción
        // Si se venden 3 hamburguesas:
        val requiredPerPortion = BigDecimal("0.200")
        val orderQuantity = BigDecimal("3")

        val totalDeducted = requiredPerPortion.multiply(orderQuantity)

        assertEquals(BigDecimal("0.600"), totalDeducted)
    }

    @Test
    fun customerCreditLimit_preventsExcessiveDebt() {
        val creditLimit = BigDecimal("1000.00")
        val currentDebt = BigDecimal("850.00")
        val newPurchase = BigDecimal("200.00")

        val projectedDebt = currentDebt.add(newPurchase)
        val isExceeded = creditLimit > BigDecimal.ZERO && projectedDebt > creditLimit

        assertTrue("La deuda proyectada debe exceder el límite de crédito", isExceeded)
    }
}
