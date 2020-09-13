package com.j0rsa.bujo.tracker.handler

import arrow.core.Either
import com.j0rsa.bujo.tracker.*
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus

object CommandHandler : Logging {
	private val logger = logger()

	fun <E : Event> CommandSyntax.process(command: Command): Either<TrackerError, E> = execute(command)

	private fun <E : Event> execute(command: Command): Either<TrackerError, E> = run {
		logger.debug("Processing $command")
		when (command) {
			is CreateTagAction -> TODO()
			is CreateHabit -> TODO()
		}
	}
}

interface CommandSyntax {
	val vertx: Vertx
	fun Event.send(address: String): EventBus = vertx.eventBus().send(address, this)
}