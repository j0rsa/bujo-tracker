package com.j0rsa.bujo.tracker

import com.j0rsa.bujo.tracker.handler.ACTIONS
import com.j0rsa.bujo.tracker.handler.EventHandler
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Weeks
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.full.companionObject

fun DateTime.isCurrentDay() = Days.daysBetween(this, DateTime.now()).days in (0..1)
fun DateTime.isCurrentWeek() = Weeks.weeksBetween(this, DateTime.now()).weeks in (0..1)

fun <T : Any> getClassForLogging(javaClass: Class<T>): Class<*> {
	return javaClass.enclosingClass?.takeIf {
		it.kotlin.companionObject?.java == javaClass
	} ?: javaClass
}

interface Logging

inline fun <reified T : Logging> T.logger(): Logger =
	LoggerFactory.getLogger(getClassForLogging(T::class.java).name + " w/interface")


suspend fun <T> execBlocking(vx: Vertx, fn: () -> T): T = withContext(Dispatchers.Default) {
	val handler = Handler { promise: Promise<T> ->
		try {
			promise.complete(fn())
		} catch (t: Throwable) {
			promise.fail(t)
		}
	}
	awaitResult<T> { vx.executeBlocking(handler, it) }
}

suspend fun <T> blockingTx(vx: Vertx, fn: () -> T) = execBlocking(vx) { TransactionManager.tx { fn() } }

fun <T> CoroutineVerticle.consume(address: String, block: (T) -> Unit): MessageConsumer<T> =
	vertx.eventBus().consumer(address) {
		block(it.body())
	}