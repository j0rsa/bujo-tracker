package com.j0rsa.bujo.tracker

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Database.Companion.connect
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager as ExposedTransactionManager


object TransactionManager {
	private val hikariConfig = HikariConfig()
		.apply {
			jdbcUrl = Config.app.db.url
			username = Config.app.db.user
			password = Config.app.db.password
			driverClassName = Config.app.db.driver
			connectionTimeout = 1500
			validationTimeout = 1000
			maxLifetime = 1000
			minimumIdle = 1
			maximumPoolSize = Config.app.db.maxPool
			transactionIsolation = "TRANSACTION_READ_COMMITTED"
			connectionTestQuery = "SELECT 1"
			addDataSourceProperty("statement_timeout", 60000)
		}
	private val dataSource = HikariDataSource(hikariConfig)
	private val db: Database = connect(dataSource)

	fun currentTransaction() = ExposedTransactionManager.current()
	fun <T> tx(block: () -> T) =
		transaction(db) {
			block()
		}

	fun migrate() {
		with(Flyway.configure()) {
			locations("classpath:db/migration")
			baselineOnMigrate(false)
			dataSource(TransactionManager.dataSource).load().migrate()
		}
	}
}