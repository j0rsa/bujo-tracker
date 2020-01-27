package com.j0rsa.bujo.tracker

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager as ExposedTransactionManager


object TransactionManager {
    private val db: Database = Database.connect(Config.app.db.url, Config.app.db.driver)

    fun currentTransaction() = ExposedTransactionManager.current()
    fun <T> tx(block: () -> T) =
        transaction(db) {
            block()
        }
}