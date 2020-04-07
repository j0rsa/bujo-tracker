package com.j0rsa.bujo.tracker

import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle

class SwaggerVerticle : CoroutineVerticle(), Logging {
    private val logger = logger()
    override suspend fun start() {
        val router = Router.router(vertx)
        router.route("/*").handler(StaticHandler.create().setIndexPage("index.html"))
        val swaggerPort = Config.app.swagger.port
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(swaggerPort)
        logger.info("Swagger has been started on $swaggerPort")
    }
}