package com.j0rsa.bujo.tracker

import arrow.core.Either
import com.j0rsa.bujo.tracker.handler.*
import com.j0rsa.bujo.tracker.handler.ResponseState.INTERNAL_SERVER_ERROR
import com.j0rsa.bujo.tracker.handler.ResponseState.NOT_FOUND
import com.j0rsa.bujo.tracker.model.HabitId
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.reflect.KFunction1


class App : CoroutineVerticle() {
	override suspend fun start() {

		val router = Router.router(vertx)
		router.route().handler(BodyHandler.create())
		val hc = HealthCheckHandler.create(vertx)
		router.apply {
			get("/health").handler(hc)

			post("/habits").coroutineHandler { HabitHandler.create(vertx)(it) }
			get("/habits").coroutineHandler { HabitHandler.findAll(vertx)(it) }
			get("/habits/:id").coroutineHandler { HabitHandler.findOne(vertx)(it) }
			post("/habits/:id").coroutineHandler { HabitHandler.update(vertx)(it) }
			delete("/habits/:id").coroutineHandler { HabitHandler.delete(vertx)(it) }

			get("/tags").coroutineHandler { TagHandler.findAll(vertx)(it) }
			post("/tags/:id").coroutineHandler { TagHandler.update(vertx)(it) }

			post("/actions").coroutineHandler { ActionHandler.createWithTags(vertx)(it) }
			get("/actions").coroutineHandler { ActionHandler.findAll(vertx)(it) }
			get("/actions/:id").coroutineHandler { ActionHandler.findOne(vertx)(it) }
			post("/actions/:id").coroutineHandler { ActionHandler.update(vertx)(it) }
			delete("/actions/:id").coroutineHandler { ActionHandler.delete(vertx)(it) }
			post("/actions/:id/value").coroutineHandler { ActionHandler.addValue(vertx)(it) }
			post("/actions/habit/:id").coroutineHandler { ActionHandler.createWithHabit(vertx)(it) }

			get("/users/:telegram_id").coroutineHandler { UserHandler.findUser(vertx)(it) }
			post("/users").coroutineHandler { UserHandler.createOrUpdateUser(vertx)(it) }

		}
		logger.info("Server on port ${Config.app.port}")
		vertx.createHttpServer()
			.requestHandler(router)
			.listenAwait(Config.app.port)
	}

	companion object : Logging {
		private val logger = logger()

			val myMap: Map<String, KFunction1<Vertx, suspend (RoutingContext) -> Either<TrackerError, Response<out Any>>>> = mapOf(
				"createHabit" to HabitHandler::create,
				"ololo" to ActionHandler::createWithTags

			)

			fun main() {
				val vertx = Vertx.vertx()
				OpenAPI3RouterFactory.create(vertx, "src/main/resources/spec.yaml") { asyncResult ->
					if (asyncResult.succeeded()) {
						asyncResult.result().apply {
							addHandlerByOperationId("getHealthInfo", HealthCheckHandler.create(vertx))
							myMap.forEach { (k, v) ->
								addHandlerByOperationId(k, coroutineHandler { v.call(vertx)(it) })
							}
							addHandlerByOperationId("createHabit", coroutineHandler { HabitHandler.create(vertx)(it) })
							addHandlerByOperationId("getAllHabits", coroutineHandler { HabitHandler.findAll(vertx)(it) })
							addHandlerByOperationId("getHabit", coroutineHandler { HabitHandler.findOne(vertx)(it) })
							addHandlerByOperationId("updateHabit", coroutineHandler { HabitHandler.update(vertx)(it) })
							addHandlerByOperationId("deleteHabit", coroutineHandler { HabitHandler.delete(vertx)(it) })

							addHandlerByOperationId("getAllTags", coroutineHandler { TagHandler.findAll(vertx)(it) })
							addHandlerByOperationId("updateTag", coroutineHandler { TagHandler.update(vertx)(it) })

							addHandlerByOperationId("createAction", coroutineHandler { ActionHandler.createWithTags(vertx)(it) })

							vertx.createHttpServer()
								.requestHandler(router)
								.listen(Config.app.port)
						}
						logger.info("Application started")
					} else {
						logger.error("Could not start application")
						asyncResult.cause().printStackTrace()
					}
				}
			}

			private inline fun <reified T> coroutineHandler(crossinline fn: suspend (RoutingContext) -> Either<TrackerError, Response<T>>): (RoutingContext) -> Unit =
				{ ctx ->
					GlobalScope.launch(ctx.vertx().dispatcher()) {
						try {
							ctx.response()
							when (val result = fn(ctx)) {
								is Either.Left -> errorResponse(result, ctx.response()).end()
								is Either.Right -> result.b.response(ctx.response())
							}
						} catch (e: Exception) {
							ctx.fail(e)
						}
					}
				}

			private fun errorResponse(result: Either.Left<TrackerError>, response: HttpServerResponse): HttpServerResponse =
				when (result.a) {
					TrackerError.NotFound -> response.setStatusCode(NOT_FOUND.value)
					is TrackerError.SyStemError -> response.setStatusCode(INTERNAL_SERVER_ERROR.value)
				}

			private inline fun <reified T> Response<T>.response(response: HttpServerResponse) {
				response.statusCode = this.state.value
				this.value?.also {
					response.putHeader("Content-Type", "application/json")
					response.end(Serializer.toJson(it));
				} ?: response.end()
			}
		}
	}
}

fun main() {
	App.main()
}