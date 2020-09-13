package com.j0rsa.bujo.tracker.handler

import arrow.core.Either
import arrow.core.right
import com.j0rsa.bujo.tracker.*
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus

object CommandHandler : Logging {
	private val logger = logger()

	fun <E : Event> CommandSyntax.process(command: Command): Either<TrackerError, E> = execute(command)

	@Suppress("UNCHECKED_CAST")
	private fun <E : Event> CommandSyntax.execute(command: Command): Either<TrackerError, E> = run {
		logger.debug("Processing $command")
		val result = when (command) {
			is CreateTagAction -> command.toEvent()
			is CreateHabit -> command.toEvent()
		} as E
		result.publish(ACTIONS)
		result.right()
	}

	private fun CreateTagAction.toEvent(): TagActionCreated = TagActionCreated(actionId, userId, tags, date, message)
	private fun CreateHabit.toEvent(): HabitCreated =
		HabitCreated(habitId, userId, tags, numberOfRepetitions, period, message)
}

interface CommandSyntax {
	val vertx: Vertx
	fun Event.publish(address: String): EventBus = vertx.eventBus().publish(address, this)
}