package com.j0rsa.bujo.tracker

import com.j0rsa.bujo.tracker.verticle.AppVerticle
import com.j0rsa.bujo.tracker.verticle.SwaggerVerticle
import io.vertx.core.Vertx
import io.vertx.kotlin.core.deployVerticleAwait
import org.slf4j.LoggerFactory

suspend fun main() {
    val logger = LoggerFactory.getLogger("Main")
    try {
        with(Vertx.vertx()) {
            deployVerticleAwait(AppVerticle::class.qualifiedName!!)
            if (Config.app.swagger.enabled) {
                deployVerticleAwait(SwaggerVerticle::class.qualifiedName!!)
            }
            logger.info("Application started")
        }
    } catch (exception: Throwable) {
        logger.error("Could not start application")
        exception.printStackTrace()
    }
}