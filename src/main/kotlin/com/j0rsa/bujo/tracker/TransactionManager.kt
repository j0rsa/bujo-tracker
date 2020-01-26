package com.j0rsa.bujo.tracker

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

object TransactionManager {
    private val db: Database = Database.connect(Config.app.db.url, Config.app.db.driver)

    fun <T> tx(block: () -> T) =
        transaction(db) {
            block()
        }
}