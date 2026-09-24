package com.yach.kinetix.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yach.kinetix.core.security.SecurityManager
import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.dao.CategoryDao
import com.yach.kinetix.backoffice.data.dao.IngredientDao
import com.yach.kinetix.backoffice.data.dao.PriceListDao
import com.yach.kinetix.backoffice.data.dao.ProductDao
import com.yach.kinetix.backoffice.data.dao.RecipeDao
import com.yach.kinetix.backoffice.data.dao.StockAuditDao
import com.yach.kinetix.backoffice.data.dao.UserDao
import com.yach.kinetix.backoffice.data.dao.WastageDao
import com.yach.kinetix.core.security.DatabasePassphraseProvider
import com.yach.kinetix.frontofhouse.data.dao.CustomerDao
import com.yach.kinetix.frontofhouse.data.dao.ModifierDao
import com.yach.kinetix.frontofhouse.data.dao.OrderDao
import com.yach.kinetix.frontofhouse.data.dao.RoomDao
import com.yach.kinetix.frontofhouse.data.dao.TableDao
import com.yach.kinetix.treasury.data.dao.CashMovementDao
import com.yach.kinetix.treasury.data.dao.CashRegisterDao
import com.yach.kinetix.treasury.data.dao.CashShiftDao
import com.yach.kinetix.treasury.data.dao.ExpenseDao
import com.yach.kinetix.treasury.data.dao.SupplierDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "kitchen_pos_secure.db"

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        passphraseProvider: DatabasePassphraseProvider,
        securityManager: SecurityManager
    ): AppDatabase {
        val passphrase = passphraseProvider.getPassphrase()
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    val id = java.util.UUID.randomUUID().toString()
                    val pinHash = securityManager.hashPin("1234")
                    val now = System.currentTimeMillis()
                    db.execSQL(
                        "INSERT INTO users (id, username, fullName, role, pinHash, isActive, createdAt, updatedAt) " +
                        "VALUES ('${id}', 'admin', 'Administrador Sistema', 'ADMIN', '${pinHash}', 1, ${now}, ${now})"
                    )
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideTransactionRunner(
        database: AppDatabase
    ): TransactionRunner = RoomTransactionRunner(database)

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideWastageDao(db: AppDatabase): WastageDao = db.wastageDao()

    @Provides
    fun provideStockAuditDao(db: AppDatabase): StockAuditDao = db.stockAuditDao()

    @Provides
    fun provideAuditLogDao(db: AppDatabase): AuditLogDao = db.auditLogDao()

    @Provides
    fun provideCashShiftDao(db: AppDatabase): CashShiftDao = db.cashShiftDao()

    @Provides
    fun provideCashMovementDao(db: AppDatabase): CashMovementDao = db.cashMovementDao()

    @Provides
    fun provideTableDao(db: AppDatabase): TableDao = db.tableDao()

    @Provides
    fun provideModifierDao(db: AppDatabase): ModifierDao = db.modifierDao()

    @Provides
    fun provideOrderDao(db: AppDatabase): OrderDao = db.orderDao()

    @Provides
    fun provideCustomerDao(db: AppDatabase): CustomerDao = db.customerDao()

    @Provides
    fun provideRoomDao(db: AppDatabase): RoomDao = db.roomDao()

    @Provides
    fun provideCashRegisterDao(db: AppDatabase): CashRegisterDao = db.cashRegisterDao()

    @Provides
    fun provideSupplierDao(db: AppDatabase): SupplierDao = db.supplierDao()

    @Provides
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideIngredientDao(db: AppDatabase): IngredientDao = db.ingredientDao()

    @Provides
    fun provideRecipeDao(db: AppDatabase): RecipeDao = db.recipeDao()

    @Provides
    fun providePriceListDao(db: AppDatabase): PriceListDao = db.priceListDao()
}
