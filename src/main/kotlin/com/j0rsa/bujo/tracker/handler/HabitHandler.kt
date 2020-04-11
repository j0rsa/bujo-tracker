package com.j0rsa.bujo.tracker.handler

import arrow.core.Either
import arrow.core.Either.Right
import com.j0rsa.bujo.tracker.TrackerError
import com.j0rsa.bujo.tracker.blockingTx
import com.j0rsa.bujo.tracker.handler.RequestLens.habitIdLens
import com.j0rsa.bujo.tracker.handler.RequestLens.habitLens
import com.j0rsa.bujo.tracker.handler.RequestLens.userIdLens
import com.j0rsa.bujo.tracker.handler.ResponseState.*
import com.j0rsa.bujo.tracker.model.*
import com.j0rsa.bujo.tracker.model.Period.Day
import com.j0rsa.bujo.tracker.model.Period.Week
import com.j0rsa.bujo.tracker.service.ActionService
import com.j0rsa.bujo.tracker.service.HabitService
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext
import org.joda.time.DateTime
import java.math.BigDecimal

object HabitHandler {

	fun create(vertx: Vertx): suspend (RoutingContext) -> Either<TrackerError, Response<HabitId>> = { request ->
		val habitId = blockingTx(vertx) { HabitService.create(request.toHabitDto()) }
		Right(Response(CREATED, habitId))
	}

	fun delete(vertx: Vertx): suspend (RoutingContext) -> Either<TrackerError, Response<Unit>> = { request ->
		blockingTx(vertx) {
			HabitService.deleteOne(habitIdLens(request), userIdLens(request))
		}.map { Response<Unit>(NO_CONTENT) }
	}

	fun findOne(vertx: Vertx): suspend (RoutingContext) -> Either<TrackerError, Response<HabitInfoView>> = { request ->
		blockingTx(vertx) {
			HabitService
				.findOneBy(habitIdLens(request), userIdLens(request))
				.map {
					val streak = findStreaks(it)
					val done = ActionService.hasActionToday(it.id!!)
					HabitInfoView(it.toView(), streak, done)
				}
		}.map { Response(OK, it) }
	}

	private fun findStreaks(it: HabitRow): StreakRow = when (it.period) {
		Day -> ActionService.findStreakForDay(it.id!!, it.numberOfRepetitions)
		Week -> ActionService.findStreakForWeek(it.id!!, it.numberOfRepetitions)
	}

	private fun findCurrentStreaks(it: HabitRow) = when (it.period) {
		Day -> ActionService.findCurrentStreakForDay(it.id!!, it.numberOfRepetitions)
		Week -> ActionService.findCurrentStreakForWeek(it.id!!, it.numberOfRepetitions)
	}

	fun findAll(vertx: Vertx): suspend (RoutingContext) -> Either<TrackerError, Response<List<HabitsInfoView>>> =
		{ request ->
			val habits = blockingTx(vertx) {
				HabitService.findAll(userIdLens(request))
					.map {
						val streak = findCurrentStreaks(it)
						val done = ActionService.hasActionToday(it.id!!)
						HabitsInfoView(it.toView(), streak, done)
					}
			}
			Right(Response(OK, habits))
		}

	fun update(vertx: Vertx): suspend (RoutingContext) -> Either<TrackerError, Response<HabitView>> = { request ->
		blockingTx(vertx) {
			HabitService.update(request.toHabitDto())
		}.map { Response(OK, it.toView()) }
	}

	private fun RoutingContext.toHabitDto() = HabitRow(habitLens(this), userIdLens(this))
}

data class Habit(
	val name: String,
	val tags: List<Tag>,
	val numberOfRepetitions: Int,
	val period: Period,
	val quote: String?,
	val bad: Boolean?,
	val startFrom: DateTime?,
	val id: HabitId? = null,
	val values: List<ValueTemplateRow> = emptyList()
)
typealias HabitView = Habit

data class HabitsInfoView(
	val habit: Habit,
	val currentStreak: BigDecimal = BigDecimal.ZERO,
	val done: Boolean
)

data class HabitInfoView(
	val habit: Habit,
	val streakRow: StreakRow,
	val done: Boolean
)

data class StreakRow(
	val currentStreak: BigDecimal = BigDecimal.ZERO,
	val maxStreak: BigDecimal = BigDecimal.ZERO
)


data class ValueTemplate(
	val type: ValueType,
	val values: List<String> = emptyList(),
	val name: String? = null
)

typealias ValueTemplateRow = ValueTemplate
