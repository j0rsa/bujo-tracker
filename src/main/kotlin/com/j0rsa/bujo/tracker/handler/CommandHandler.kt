package com.j0rsa.bujo.tracker.handler

import arrow.core.Validated
import com.j0rsa.bujo.tracker.*
import io.vertx.core.eventbus.EventBus
import io.vertx.kotlin.coroutines.CoroutineVerticle

object CommandHandler : Logging {
	private val logger = logger()

	fun SendEventSyntax.process(command: Command): Validated<TrackerError, Event> {

		val res = execute(command)
		TODO()
	}

	private fun execute(command: Command): Validated<TrackerError, Event> {
		logger.debug("Processing $command")
		TODO()
	}
}

interface SendEventSyntax {
	val verticle: CoroutineVerticle
	fun Event.send(address: String): EventBus = verticle.vertx.eventBus().send(address, this)
}