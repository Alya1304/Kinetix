package com.yach.kinetix.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.dao.CategoryDao
import com.yach.kinetix.backoffice.data.dao.IngredientDao
import com.yach.kinetix.backoffice.data.dao.PriceListDao
import com.yach.kinetix.backoffice.data.dao.ProductDao
import com.yach.kinetix.backoffice.data.dao.RecipeDao
import com.yach.kinetix.backoffice.data.dao.StockAuditDao
import com.yach.kinetix.backoffice.data.dao.UserDao
import com.yach.kinetix.backoffice.data.dao.WastageDao
import com.yach.kinetix.backoffice.data.entity.AuditLogEntity
import com.yach.kinetix.backoffice.data.entity.CategoryEntity
import com.yach.kinetix.backoffice.data.entity.IngredientEntity
import com.yach.kinetix.backoffice.data.entity.PriceListEntity
import com.yach.kinetix.backoffice.data.entity.ProductComboItemEntity
import com.yach.kinetix.backoffice.data.entity.ProductEntity
import com.yach.kinetix.backoffice.data.entity.ProductPriceEntity
import com.yach.kinetix.backoffice.data.entity.RecipeItemEntity
import com.yach.kinetix.backoffice.data.entity.StockAuditEntity
import com.yach.kinetix.backoffice.data.entity.UserEntity
import com.yach.kinetix.backoffice.data.entity.WastageEntity
import com.yach.kinetix.core.database.converters.RoomConverters
import com.yach.kinetix.frontofhouse.data.dao.CustomerDao
import com.yach.kinetix.frontofhouse.data.dao.ModifierDao
import com.yach.kinetix.frontofhouse.data.dao.OrderDao
import com.yach.kinetix.frontofhouse.data.dao.RoomDao
import com.yach.kinetix.frontofhouse.data.dao.TableDao
import com.yach.kinetix.frontofhouse.data.entity.CustomerAccountMovementEntity
import com.yach.kinetix.frontofhouse.data.entity.CustomerEntity
import com.yach.kinetix.frontofhouse.data.entity.ModifierEntity
import com.yach.kinetix.frontofhouse.data.entity.OrderEntity
import com.yach.kinetix.frontofhouse.data.entity.OrderItemEntity
import com.yach.kinetix.frontofhouse.data.entity.OrderItemModifierEntity
import com.yach.kinetix.frontofhouse.data.entity.RestaurantTableEntity
import com.yach.kinetix.frontofhouse.data.entity.RoomEntity
import com.yach.kinetix.treasury.data.dao.CashMovementDao
import com.yach.kinetix.treasury.data.dao.CashRegisterDao
import com.yach.kinetix.treasury.data.dao.CashShiftDao
import com.yach.kinetix.treasury.data.dao.ExpenseDao
import com.yach.kinetix.treasury.data.dao.SupplierDao
import com.yach.kinetix.treasury.data.entity.CashMovementEntity
import com.yach.kinetix.treasury.data.entity.CashRegisterEntity
import com.yach.kinetix.treasury.data.entity.CashShiftEntity
import com.yach.kinetix.treasury.data.entity.ExpenseCategoryEntity
import com.yach.kinetix.treasury.data.entity.ExpenseEntity
import com.yach.kinetix.treasury.data.entity.SupplierAccountMovementEntity
import com.yach.kinetix.treasury.data.entity.SupplierEntity

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        WastageEntity::class,
        StockAuditEntity::class,
        AuditLogEntity::class,
        CashShiftEntity::class,
        CashMovementEntity::class,
        RestaurantTableEntity::class,
        ModifierEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        OrderItemModifierEntity::class,
        CustomerEntity::class,
        CustomerAccountMovementEntity::class,
        RoomEntity::class,
        CashRegisterEntity::class,
        SupplierEntity::class,
        SupplierAccountMovementEntity::class,
        ExpenseCategoryEntity::class,
        ExpenseEntity::class,
        IngredientEntity::class,
        RecipeItemEntity::class,
        PriceListEntity::class,
        ProductPriceEntity::class,
        ProductComboItemEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun wastageDao(): WastageDao
    abstract fun stockAuditDao(): StockAuditDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun cashShiftDao(): CashShiftDao
    abstract fun cashMovementDao(): CashMovementDao
    abstract fun tableDao(): TableDao
    abstract fun modifierDao(): ModifierDao
    abstract fun orderDao(): OrderDao
    abstract fun customerDao(): CustomerDao
    abstract fun roomDao(): RoomDao
    abstract fun cashRegisterDao(): CashRegisterDao
    abstract fun supplierDao(): SupplierDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun recipeDao(): RecipeDao
    abstract fun priceListDao(): PriceListDao
}
