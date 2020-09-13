package com.j0rsa.bujo.tracker.handler

import arrow.core.Either
import arrow.core.right
import com.j0rsa.bujo.tracker.*
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus

object CommandHandler : Logging {
	val logger = logger()

	inline fun <reified E : Event> CommandSyntax.process(command: Command): Either<TrackerError, E> = execute(command)

	inline fun <reified E : Event> CommandSyntax.execute(command: Command): Either<TrackerError, E> = run {
		logger.debug("Processing $command")
		val result = when (command) {
			is CreateTagAction -> command.toEvent()
			is CreateHabit -> command.toEvent()
		} as E
		result.publish(ACTIONS)
		result.right()
	}

	fun CreateTagAction.toEvent(): TagActionCreated = TagActionCreated(actionId, userId, tags, date, message)
	fun CreateHabit.toEvent(): HabitCreated =
		HabitCreated(habitId, userId, tags, numberOfRepetitions, period, message)
}

interface CommandSyntax {
	val vertx: Vertx
	fun Event.publish(address: String): EventBus = vertx.eventBus().publish(address, this)
}