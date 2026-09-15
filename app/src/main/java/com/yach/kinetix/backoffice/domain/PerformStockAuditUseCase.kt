package com.yach.kinetix.backoffice.domain

import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.dao.ProductDao
import com.yach.kinetix.backoffice.data.dao.StockAuditDao
import com.yach.kinetix.backoffice.data.entity.AuditLogEntity
import com.yach.kinetix.backoffice.data.entity.StockAuditEntity
import com.yach.kinetix.core.database.TransactionRunner
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class PerformStockAuditUseCase @Inject constructor(
    private val productDao: ProductDao,
    private val stockAuditDao: StockAuditDao,
    private val auditLogDao: AuditLogDao,
    private val transactionRunner: TransactionRunner
) {

    /**
     * Registra el conteo ciego de inventario en almacén, calculando automáticamente la diferencia
     * con respecto al stock teórico y actualizando las existencias de forma auditable.
     */
    suspend operator fun invoke(
        productId: UUID,
        countedStock: BigDecimal,
        auditedByUserId: UUID,
        notes: String? = null
    ): Result<StockAuditEntity> = runCatching {
        require(countedStock >= BigDecimal.ZERO) { "El conteo físico de stock no puede ser negativo" }

        transactionRunner.runInTransaction {
            val product = productDao.getProductById(productId)
                ?: throw IllegalArgumentException("Producto $productId no encontrado")

            val expectedStock = product.stockQuantity
            val difference = countedStock.subtract(expectedStock)

            val audit = StockAuditEntity(
                id = UUID.randomUUID(),
                productId = productId,
                expectedStock = expectedStock,
                countedStock = countedStock,
                difference = difference,
                auditedByUserId = auditedByUserId,
                notes = notes,
                auditedAt = Instant.now()
            )

            // Ajustar el stock en base de datos al conteo físico real
            productDao.updateStock(productId, countedStock)
            stockAuditDao.insertAudit(audit)

            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = auditedByUserId,
                    action = "STOCK_AUDIT_ADJUSTMENT",
                    entityName = "Product",
                    entityId = productId.toString(),
                    details = "Auditoría de inventario para ${product.name}: Esperado: $expectedStock, Físico: $countedStock, Ajuste/Diferencia: $difference. Notas: ${notes ?: "Sin notas"}"
                )
            )

            audit
        }
    }
}
