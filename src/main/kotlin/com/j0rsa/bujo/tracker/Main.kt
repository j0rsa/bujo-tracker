package com.j0rsa.bujo.tracker

import com.j0rsa.bujo.tracker.handler.ActionHandler
import com.j0rsa.bujo.tracker.handler.HabitHandler
import com.j0rsa.bujo.tracker.handler.TagHandler
import com.j0rsa.bujo.tracker.model.*
import org.http4k.server.Http4kServer
import org.apache.logging.log4j.core.config.Configurator
import org.http4k.core.*
import org.http4k.server.Jetty
import org.slf4j.LoggerFactory
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.asServer

fun main() {
    val server = startApp()
    server.block()
}

fun startApp(): Http4kServer {
    Configurator.initialize(null, Config.app.logConfig)

    val logger = LoggerFactory.getLogger("main")

    TransactionManager.tx {
        createSchema()
    }

    val app = CatchLensFailure.then(
        routes(
            "/health" bind Method.GET to { Response(Status.OK) },
            "/habits" bind routes(
                "/" bind Method.POST to HabitHandler.create(),
                "/" bind Method.GET to HabitHandler.findAll(),
                "/{id}" bind routes(
                    Method.GET to HabitHandler.findOne(),
                    Method.POST to HabitHandler.update(),
                    Method.DELETE to HabitHandler.delete()
                )
            ),
            "/tags" bind routes(
                "/" bind Method.GET to TagHandler.findAll(),
                "/{id}" bind routes(
                    Method.POST to TagHandler.update()
                )
            ),
            "/actions" bind routes(
                "/" bind Method.POST to ActionHandler.createWithTags(),
                "/" bind Method.GET to ActionHandler.findAll(),
                "/{id}" bind routes(
                    Method.GET to ActionHandler.findOne(),
                    Method.POST to ActionHandler.update(),
                    Method.DELETE to ActionHandler.delete()
                ),
                "/habit/{id}" bind Method.POST to ActionHandler.createWithHabit()
            )
        )
    )

    logger.info("Starting server...")
    val server = app.asServer(Jetty(Config.app.port)).start()
    logger.info("Server started on port ${Config.app.port}")
    return server
}