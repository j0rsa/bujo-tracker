package com.j0rsa.bujo.tracker

import arrow.core.Either
import com.google.gson.Gson
import com.j0rsa.bujo.tracker.handler.*
import com.j0rsa.bujo.tracker.model.createSchema
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.deployVerticleAwait
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class App : CoroutineVerticle() {
	override suspend fun start() {
		dbMigrate()

		val router = Router.router(vertx)
		router.route().handler(BodyHandler.create())
		val hc = HealthCheckHandler.create(vertx)
		router.get("/health").handler(hc);

		router.post("/habits").coroutineHandler { HabitHandler.create(vertx)(it) }
		router.get("/habits").coroutineHandler { HabitHandler.findAll(vertx)(it) }
		router.get("/habits/:id").coroutineHandler { HabitHandler.findOne(vertx)(it) }
		router.post("/habits/:id").coroutineHandler { HabitHandler.update(vertx)(it) }
		router.delete("/habits/:id").coroutineHandler { HabitHandler.delete(vertx)(it) }

		router.get("/tags").coroutineHandler { TagHandler.findAll(vertx)(it) }
		router.post("/tags/:id").coroutineHandler { TagHandler.update(vertx)(it) }

		router.post("/actions").coroutineHandler { ActionHandler.createWithTags(vertx)(it) }
		router.get("/actions").coroutineHandler { ActionHandler.findAll(vertx)(it) }
		router.get("/actions/:id").coroutineHandler { ActionHandler.findOne(vertx)(it) }
		router.post("/actions/:id").coroutineHandler { ActionHandler.update(vertx)(it) }
		router.delete("/actions/:id").coroutineHandler { ActionHandler.delete(vertx)(it) }
		router.post("/actions/:id/value").coroutineHandler { ActionHandler.addValue(vertx)(it) }
		router.post("/actions/habit/:id").coroutineHandler { ActionHandler.createWithHabit(vertx)(it) }

		router.get("/users/:telegram_id").coroutineHandler { UserHandler.findUser(vertx)(it) }
		router.post("/users").coroutineHandler { UserHandler.createOrUpdateUser(vertx)(it) }

		logger.info("Server on port ${Config.app.port}")
		vertx.createHttpServer()
			.requestHandler(router)
			.listenAwait(Config.app.port)
	}

	companion object : Logging {
		val logger = logger()
	}

	private suspend fun dbMigrate() {
//    TransactionManager.migrate()
		blockingTx(vertx) {
			createSchema()
		}
	}
}

suspend fun main() {
	val vertx = Vertx.vertx()
	try {
		vertx.deployVerticleAwait("com.j0rsa.bujo.tracker.App")
		println("Application started")
	} catch (exception: Throwable) {
		println("Could not start application")
		exception.printStackTrace()
	}
}

inline fun <reified T> Route.coroutineHandler(crossinline fn: suspend (RoutingContext) -> Either<TrackerError, Response<T>>): Route =
	handler { ctx ->
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

fun errorResponse(result: Either.Left<TrackerError>, response: HttpServerResponse): HttpServerResponse =
	when (result.a) {
		TrackerError.NotFound -> response.setStatusCode(404)
		is TrackerError.SyStemError -> response.setStatusCode(500)
	}

inline fun <reified T> Response<T>.response(response: HttpServerResponse) {
	response.statusCode = this.state.value
	this.value?.also {
		response.putHeader("Content-Type", "application/json")
		response.end(Serializer.toJson(it));
	} ?: response.end()
}

object Serializer {
	val gson = Gson()
	inline fun <reified T> toJson(o: T): String = gson.toJson(o, T::class.java)
	inline fun <reified T> fromJson(s: String): T = gson.fromJson(s, T::class.java)
}