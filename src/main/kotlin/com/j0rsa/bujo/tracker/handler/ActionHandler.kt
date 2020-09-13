package com.j0rsa.bujo.tracker.handler

import arrow.core.Either
import arrow.core.Right
import com.j0rsa.bujo.tracker.*
import com.j0rsa.bujo.tracker.handler.CommandHandler.process
import com.j0rsa.bujo.tracker.handler.RequestLens.actionIdPathLens
import com.j0rsa.bujo.tracker.handler.RequestLens.actionLens
import com.j0rsa.bujo.tracker.handler.RequestLens.habitIdLens
import com.j0rsa.bujo.tracker.handler.RequestLens.userIdLens
import com.j0rsa.bujo.tracker.handler.RequestLens.valueLens
import com.j0rsa.bujo.tracker.handler.ResponseState.*
import com.j0rsa.bujo.tracker.model.*
import com.j0rsa.bujo.tracker.service.ActionService
import com.j0rsa.bujo.tracker.service.ValueService
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext
import org.joda.time.LocalDateTime

object ActionHandler {
	fun createWithHabit(vertx: Vertx): suspend (RoutingContext) -> Either<TrackerError, Response<ActionId>> = { req ->
		blockingTx(vertx) {
			ActionService.create(req.toDtoWithHabit())
		}.map { Response(CREATED, it) }
	}

	fun CommandSyntax.createWithTags(req: RoutingContext): Either<TrackerError, Response<ActionId>> =
		process<TagActionCreated>(req.toTagAction())
				.map { Response(CREATED, it.actionId) }

	fun findAll(vertx: Vertx): suspend (RoutingContext) -> Either<TrackerError, Response<List<ActionView>>> = { req ->
		val actions = blockingTx(vertx) {
			ActionService.findAll(userIdLens(req))
		}.map { it.toView() }
		Right(Response(OK, actions))
	}

	fun findOne(vertx: Vertx): suspend (RoutingContext) -> Either<TrackerError, Response<ActionRow>> = { req ->
		blockingTx(vertx) {
			ActionService.findOneBy(actionIdPathLens(req), userIdLens(req)).map { it.toRow() }
		}.map { Response(OK, it) }
	}

	fun addValue(vertx: Vertx): suspend (RoutingContext) -> Either<TrackerError, Response<ActionId>> = { req ->
		blockingTx(vertx) {
			ActionService.findOneBy(actionIdPathLens(req), userIdLens(req))
				.map {
					ValueService.create(valueLens(req), it)
					it.idValue()
				}
		}.map { Response(CREATED, it) }
	}

	fun update(vertx: Vertx): suspend (RoutingContext) -> Either<TrackerError, Response<ActionRow>> = { req ->
		blockingTx(vertx) {
			ActionService.update(req.toDtoWithTags())
		}.map { Response(OK, it) }
	}

	fun delete(vertx: Vertx): suspend (RoutingContext) -> Either<TrackerError, Response<Unit>> = { req ->
		blockingTx(vertx) {
			ActionService.deleteOne(actionIdPathLens(req), userIdLens(req))
		}.map { Response<Unit>(NO_CONTENT) }
	}

	private fun RoutingContext.toDtoWithHabit() =
		ActionRow(actionLens(this), userIdLens(this), habitIdLens(this))

	private fun RoutingContext.toTagAction() = run {
		val view = actionLens(this)
		CreateTagAction(userIdLens(this), view.tags.map { it.name }, LocalDateTime.now(), view.description)
	}

	private fun RoutingContext.toDtoWithTags() = BaseActionRow(actionLens(this), userIdLens(this))
}

data class ActionView(
	val description: String = "",
	val tags: List<TagRow> = emptyList(),
	val habitId: HabitId? = null,
	val id: ActionId? = null,
	val values: List<ValueRow> = emptyList()
)

data class Value(
	val type: ValueType,
	// TODO: Why nullable, keyridan?
	val value: String?,
	val name: String?
)

typealias ValueRow = Value