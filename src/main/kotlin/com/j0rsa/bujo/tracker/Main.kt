package com.j0rsa.bujo.tracker

import com.j0rsa.bujo.tracker.verticle.AppVerticle
import com.j0rsa.bujo.tracker.verticle.SwaggerVerticle
import io.vertx.core.Vertx
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle

class Main : CoroutineVerticle() {
    override suspend fun start() {
        with(vertx) {
            deployVerticleAwait(AppVerticle::class.qualifiedName!!)
            if (Config.app.swagger.enabled) {
                deployVerticleAwait(SwaggerVerticle::class.qualifiedName!!)
            }
            logger.info("Application started")
        }
    }

    companion object : Logging {
        val logger = logger()
    }
}

suspend fun main() {
    val vertx = Vertx.vertx()
    try {
        vertx.deployVerticleAwait(Main::class.qualifiedName!!)
        println("Application started")
    } catch (exception: Throwable) {
        println("Could not start application")
        exception.printStackTrace()
    }
}