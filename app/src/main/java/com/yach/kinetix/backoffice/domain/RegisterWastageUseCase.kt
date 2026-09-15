package com.yach.kinetix.backoffice.domain

import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.dao.ProductDao
import com.yach.kinetix.backoffice.data.dao.WastageDao
import com.yach.kinetix.backoffice.data.entity.AuditLogEntity
import com.yach.kinetix.backoffice.data.entity.WastageEntity
import com.yach.kinetix.core.database.TransactionRunner
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class RegisterWastageUseCase @Inject constructor(
    private val productDao: ProductDao,
    private val wastageDao: WastageDao,
    private val auditLogDao: AuditLogDao,
    private val transactionRunner: TransactionRunner
) {

    /**
     * Da de baja productos mermados o echados a perder con justificación obligatoria,
     * descontando las unidades del stock físico de forma atómica.
     */
    suspend operator fun invoke(
        productId: UUID,
        quantity: BigDecimal,
        reason: String,
        reportedByUserId: UUID
    ): Result<WastageEntity> = runCatching {
        require(quantity > BigDecimal.ZERO) { "La cantidad mermada debe ser mayor a cero" }
        require(reason.isNotBlank()) { "La justificación de la merma es obligatoria" }

        transactionRunner.runInTransaction {
            val product = productDao.getProductById(productId)
                ?: throw IllegalArgumentException("Producto no encontrado")

            val wastage = WastageEntity(
                id = UUID.randomUUID(),
                productId = productId,
                quantity = quantity,
                reason = reason,
                reportedByUserId = reportedByUserId,
                registeredAt = Instant.now()
            )

            // Deducción automática de stock
            productDao.deductStock(productId, quantity)
            wastageDao.insertWastage(wastage)

            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = reportedByUserId,
                    action = "REGISTER_WASTAGE",
                    entityName = "Product",
                    entityId = productId.toString(),
                    details = "Merma registrada para ${product.name}: $quantity unidades. Motivo: $reason"
                )
            )

            wastage
        }
    }
}
