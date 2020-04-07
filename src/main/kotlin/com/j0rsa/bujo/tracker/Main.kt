package com.j0rsa.bujo.tracker

import io.vertx.core.Vertx
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import org.slf4j.LoggerFactory

class App : CoroutineVerticle() {
//    override suspend fun start() {
//
//        val router = Router.router(vertx)
//        router.route().handler(BodyHandler.create())
//        val hc = HealthCheckHandler.create(vertx)
//        router.apply {
//            get("/health").handler(hc)
//
//            post("/habits").coroutineHandler { HabitHandler.create(vertx)(it) }
//            get("/habits").coroutineHandler { HabitHandler.findAll(vertx)(it) }
//            get("/habits/:id").coroutineHandler { HabitHandler.findOne(vertx)(it) }
//            post("/habits/:id").coroutineHandler { HabitHandler.update(vertx)(it) }
//            delete("/habits/:id").coroutineHandler { HabitHandler.delete(vertx)(it) }
//
//            get("/tags").coroutineHandler { TagHandler.findAll(vertx)(it) }
//            post("/tags/:id").coroutineHandler { TagHandler.update(vertx)(it) }
//
//            post("/actions").coroutineHandler { ActionHandler.createWithTags(vertx)(it) }
//            get("/actions").coroutineHandler { ActionHandler.findAll(vertx)(it) }
//            get("/actions/:id").coroutineHandler { ActionHandler.findOne(vertx)(it) }
//            post("/actions/:id").coroutineHandler { ActionHandler.update(vertx)(it) }
//            delete("/actions/:id").coroutineHandler { ActionHandler.delete(vertx)(it) }
//            post("/actions/:id/value").coroutineHandler { ActionHandler.addValue(vertx)(it) }
//            post("/actions/habit/:id").coroutineHandler { ActionHandler.createWithHabit(vertx)(it) }
//
//            get("/users/:telegram_id").coroutineHandler { UserHandler.findUser(vertx)(it) }
//            post("/users").coroutineHandler { UserHandler.createOrUpdateUser(vertx)(it) }
//
//        }
//        logger.info("Server on port ${Config.app.port}")
//        vertx.createHttpServer()
//            .requestHandler(router)
//            .listenAwait(Config.app.port)
//    }
}

suspend fun main() {
    val logger = LoggerFactory.getLogger("Main")
    val vertx = Vertx.vertx()
    try {
//        vertx.deployVerticleAwait(AppVerticle::class.qualifiedName!!)
        vertx.deployVerticleAwait(SwaggerVerticle::class.qualifiedName!!)
        logger.info("Application started")
    } catch (exception: Throwable) {
        logger.error("Could not start application")
        exception.printStackTrace()
    }
}