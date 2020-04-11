package com.j0rsa.bujo.tracker.verticle

import arrow.core.Either
import com.j0rsa.bujo.tracker.*
import com.j0rsa.bujo.tracker.handler.*
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory
import io.vertx.ext.web.api.validation.ValidationException
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IllegalStateException
import kotlin.reflect.KFunction1

class AppVerticle : CoroutineVerticle() {
    override suspend fun start() {
        TransactionManager.migrate()
        val specUrl = this::class.java.getResource("/webroot/spec.yaml")
            ?: throw IllegalStateException("Spec file not found!")
        OpenAPI3RouterFactory.create(vertx, specUrl.file) { asyncResult ->
            if (asyncResult.succeeded()) {
                val appPort = Config.app.port
                asyncResult.result().apply {
                    val operationHandlers = mapOf<String, Handler<RoutingContext>>(
                        "getHealthInfo" to HealthCheckHandler.create(vertx),

                        "createHabit" to HabitHandler::create,
                        "getAllHabits" to HabitHandler::findAll,
                        "getHabit" to HabitHandler::findOne,
                        "updateHabit" to HabitHandler::update,
                        "deleteHabit" to HabitHandler::delete,
                        "getAllTags" to TagHandler::findAll,
                        "updateTag" to TagHandler::update,
                        "createAction" to ActionHandler::createWithTags,
                        "getAllActions" to ActionHandler::findAll,
                        "getAction" to ActionHandler::findOne,
                        "updateAction" to ActionHandler::update,
                        "deleteAction" to ActionHandler::delete,
                        "addActionValue" to ActionHandler::addValue,
                        "createHabitAction" to ActionHandler::createWithHabit,

                        "createOrUpdateUser" to UserHandler::createOrUpdateUser,
                        "getTelegramUser" to UserHandler::findUser
                    )
                    operationHandlers.forEach { (k, v) -> addHandlerByOperationId(k, v) }
                    addGlobalHandler(Cors.disable())
                    addGlobalHandler { event ->
                        logger.debug(event.request().toString())
                        event.next()
                    }

                    operationHandlers.forEach { (t, _) ->
                        addFailureHandlerByOperationId(t) { routingContext ->
                            val failure: Throwable = routingContext.failure()
                            if (failure is ValidationException) // Handle Validation Exception
                                Response(
                                    ResponseState.BAD_REQUEST,
                                    BadRequestError(failure.type().name, failure.message)
                                ).response(routingContext.response())
                        }
                    }

                    vertx.createHttpServer()
                        .requestHandler(router)
                        .exceptionHandler { logger.error(it.message) }
                        .listen(appPort)
                }
                logger.info("Server started on $appPort")
            } else {
                logger.error("Could not start server")
                asyncResult.cause().printStackTrace()
            }
        }
    }

    private infix fun String.to(fn: KFunction1<Vertx, suspend (RoutingContext) -> Either<TrackerError, Response<*>>>): Pair<String, Handler<RoutingContext>> {
        return this to coroutineHandler {
            fn(vertx)(it)
        }
    }

    companion object : Logging {
        private val logger = logger()
        private fun coroutineHandler(fn: suspend (RoutingContext) -> Either<TrackerError, Response<*>>): Handler<RoutingContext> =
            Handler<RoutingContext> { ctx ->
                GlobalScope.launch(ctx.vertx().dispatcher()) {
                    try {
                        ctx.response()
                        when (val result = fn(ctx)) {
                            is Either.Left -> errorResponse(
                                result,
                                ctx.response()
                            ).end()
                            is Either.Right -> result.b.response(ctx.response())
                        }
                    } catch (e: Exception) {
                        logger.warn(e.message, e)
                        ctx.fail(e)
                    }
                }
            }

        private fun errorResponse(result: Either.Left<TrackerError>, response: HttpServerResponse): HttpServerResponse =
            when (result.a) {
                TrackerError.NotFound -> response.setStatusCode(ResponseState.NOT_FOUND.value)
                is TrackerError.SyStemError -> response.setStatusCode(ResponseState.INTERNAL_SERVER_ERROR.value)
            }

        private fun Response<*>.response(response: HttpServerResponse) {
            response.statusCode = this.state.value
            this.value?.also {
                response.putHeader("Content-Type", "application/json")
                response.end(Serializer.toJson(it));
            } ?: response.end()
        }
    }
}