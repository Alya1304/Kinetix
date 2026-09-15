package com.yach.kinetix.core.database

import androidx.room.withTransaction
import javax.inject.Inject

class RoomTransactionRunner @Inject constructor(
    private val database: AppDatabase
) : TransactionRunner {

    override suspend fun <R> runInTransaction(block: suspend () -> R): R {
        return database.withTransaction(block)
    }
}
