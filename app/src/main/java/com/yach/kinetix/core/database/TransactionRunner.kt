package com.yach.kinetix.core.database

interface TransactionRunner {
    suspend fun <R> runInTransaction(block: suspend () -> R): R
}
