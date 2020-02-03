package com.j0rsa.bujo.tracker.handler

import arrow.core.Either
import com.j0rsa.bujo.tracker.TrackerError
import com.j0rsa.bujo.tracker.TransactionManager
import com.j0rsa.bujo.tracker.handler.RequestLens.habitIdLens
import com.j0rsa.bujo.tracker.handler.RequestLens.habitInfoLens
import com.j0rsa.bujo.tracker.handler.RequestLens.habitLens
import com.j0rsa.bujo.tracker.handler.RequestLens.multipleHabitsLens
import com.j0rsa.bujo.tracker.handler.RequestLens.response
import com.j0rsa.bujo.tracker.handler.RequestLens.userLens
import com.j0rsa.bujo.tracker.model.*
import com.j0rsa.bujo.tracker.model.Period.Day
import com.j0rsa.bujo.tracker.model.Period.Week
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.joda.time.DateTime
import java.math.BigDecimal

object HabitHandler {

    fun create() = { req: Request ->
        val habitId = TransactionManager.tx { HabitService.create(req.toHabitDto()) }
        Response(CREATED).body(habitId.toString())
    }

    fun delete(): (Request) -> Response = { req: Request ->
        val result = TransactionManager.tx {
            HabitService.deleteOne(habitIdLens(req), userLens(req))
        }
        when (result) {
            is Either.Left -> response(result)
            is Either.Right -> Response(NO_CONTENT)
        }
    }

    fun findOne() = { req: Request ->
        val result = TransactionManager.tx {
            HabitService.findOneBy(habitIdLens(req), userLens(req))
        }.map {
            val streak = findStreaks(it)
            val habitInfo = HabitInfoView(it.toView(), streak)
            habitInfoLens(habitInfo, Response(OK))
        }
        responseFrom(result)
    }

    private fun findStreaks(it: HabitRow): StreakRow = when (it.period) {
        Day -> ActionService.findStreakForDay(it.id!!, it.numberOfRepetitions)
        Week -> ActionService.findStreakForWeek(it.id!!, it.numberOfRepetitions)
    }

    private fun findCurrentStreaks(it: HabitRow) = when (it.period) {
        Day -> ActionService.findCurrentStreakForDay(it.id!!, it.numberOfRepetitions)
        Week -> ActionService.findCurrentStreakForWeek(it.id!!, it.numberOfRepetitions)
    }

    fun findAll() = { req: Request ->
        val habits = TransactionManager.tx {
            HabitService.findAll(userLens(req))
        }.map {
            HabitsInfoView(it.toView(), findCurrentStreaks(it))
        }
        multipleHabitsLens(habits, Response(OK))
    }

    fun update() = { req: Request ->
        val result = TransactionManager.tx {
            HabitService.update(req.toHabitDto())
        }.map { habitLens(it.toView(), Response(OK)) }
        responseFrom(result)
    }

    private fun responseFrom(result: Either<TrackerError, Response>): Response = when (result) {
        is Either.Left -> response(result)
        is Either.Right -> result.b
    }

    private fun Request.toHabitDto() = HabitRow(habitLens(this), userLens(this))
}

data class HabitView(
    val name: String,
    val tagList: List<TagRow>,
    val numberOfRepetitions: Int,
    val period: Period,
    val quote: String?,
    val bad: Boolean?,
    val startFrom: DateTime?,
    val id: HabitId? = null
)

data class HabitsInfoView(
    val habitView: HabitView,
    val currentStreak: BigDecimal = BigDecimal.ZERO
)

data class HabitInfoView(
    val habitView: HabitView,
    val streakRow: StreakRow
)

data class StreakRow(
    val currentStreak: BigDecimal = BigDecimal.ZERO,
    val maxStreak: BigDecimal = BigDecimal.ZERO
)