package com.j0rsa.bujo.tracker

import com.j0rsa.bujo.tracker.verticle.AppVerticle
import com.j0rsa.bujo.tracker.verticle.SwaggerVerticle
import io.vertx.core.Vertx
import io.vertx.kotlin.core.deployVerticleAwait
import org.slf4j.LoggerFactory

//            post("/actions").coroutineHandler { ActionHandler.createWithTags(vertx)(it) }
//            get("/actions").coroutineHandler { ActionHandler.findAll(vertx)(it) }
//            get("/actions/:id").coroutineHandler { ActionHandler.findOne(vertx)(it) }
//            post("/actions/:id").coroutineHandler { ActionHandler.update(vertx)(it) }
//            delete("/actions/:id").coroutineHandler { ActionHandler.delete(vertx)(it) }
//            post("/actions/:id/value").coroutineHandler { ActionHandler.addValue(vertx)(it) }
//            post("/actions/habit/:id").coroutineHandler { ActionHandler.createWithHabit(vertx)(it) }

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