package com.j0rsa.bujo.tracker

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

data class AppConfig(
    val db: DbConfig,
    val port: Int
)

data class DbConfig(
    val url: String,
    val driver: String,
    val user: String,
    val password: String,
    val maxPool: Int
)

object Config {
    val app: AppConfig = ConfigFactory.load().extract("app")
}

