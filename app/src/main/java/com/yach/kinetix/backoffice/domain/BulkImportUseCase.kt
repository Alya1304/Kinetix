package com.yach.kinetix.backoffice.domain

import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.dao.CategoryDao
import com.yach.kinetix.backoffice.data.dao.IngredientDao
import com.yach.kinetix.backoffice.data.dao.ProductDao
import com.yach.kinetix.backoffice.data.entity.AuditLogEntity
import com.yach.kinetix.backoffice.data.entity.CategoryEntity
import com.yach.kinetix.backoffice.data.entity.IngredientEntity
import com.yach.kinetix.backoffice.data.entity.ProductEntity
import com.yach.kinetix.core.database.TransactionRunner
import com.yach.kinetix.frontofhouse.data.dao.CustomerDao
import com.yach.kinetix.frontofhouse.data.entity.CustomerEntity
import java.util.UUID
import javax.inject.Inject

data class BulkImportResult(
    val importedCategories: Int,
    val importedProducts: Int,
    val importedIngredients: Int,
    val importedCustomers: Int
)

class BulkImportUseCase @Inject constructor(
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val ingredientDao: IngredientDao,
    private val customerDao: CustomerDao,
    private val auditLogDao: AuditLogDao,
    private val transactionRunner: TransactionRunner
) {

    /**
     * Realiza la importación masiva de categorías, productos, ingredientes o clientes
     * dentro de una transacción atómica segura con bitácora forense.
     */
    suspend operator fun invoke(
        categories: List<CategoryEntity> = emptyList(),
        products: List<ProductEntity> = emptyList(),
        ingredients: List<IngredientEntity> = emptyList(),
        customers: List<CustomerEntity> = emptyList(),
        authorizedByUserId: UUID
    ): Result<BulkImportResult> = runCatching {
        transactionRunner.runInTransaction {
            if (categories.isNotEmpty()) categoryDao.insertCategories(categories)
            if (products.isNotEmpty()) productDao.insertProducts(products)
            if (ingredients.isNotEmpty()) ingredientDao.insertIngredients(ingredients)
            if (customers.isNotEmpty()) customerDao.insertCustomers(customers)

            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = authorizedByUserId,
                    action = "BULK_IMPORT",
                    entityName = "System",
                    entityId = null,
                    details = "Importación masiva completada: ${categories.size} categorías, ${products.size} productos, ${ingredients.size} ingredientes, ${customers.size} clientes."
                )
            )

            BulkImportResult(
                importedCategories = categories.size,
                importedProducts = products.size,
                importedIngredients = ingredients.size,
                importedCustomers = customers.size
            )
        }
    }
}
