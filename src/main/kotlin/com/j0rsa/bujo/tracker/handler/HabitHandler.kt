package com.j0rsa.bujo.tracker.handler

import arrow.core.Either
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.j0rsa.bujo.tracker.TrackerError
import com.j0rsa.bujo.tracker.TrackerJackson.auto
import com.j0rsa.bujo.tracker.TransactionManager
import com.j0rsa.bujo.tracker.model.HabitRow
import com.j0rsa.bujo.tracker.model.HabitService
import com.j0rsa.bujo.tracker.model.TagRow
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.lens.uuid
import java.util.*

object HabitHandler {
    private val habitLens = Body.auto<HabitView>().toLens()
    private val multipleHabitsLens = Body.auto<List<HabitView>>().toLens()
    private val habitIdLens = Path.uuid().map(::HabitId).of("id")
    private val userLens = Header.uuid().required("X-Auth-Id")

    fun create() = { req: Request ->
        val habitId = TransactionManager.tx { HabitService.create(req.toHabitDto()) }
        Response(CREATED).body(habitId.toString())
    }

    fun delete(): (Request) -> Response = { req: Request ->
        val result = TransactionManager.tx {
            HabitService.deleteOne(habitIdLens(req).value, userLens(req))
        }
        when (result) {
            is Either.Left -> response(result)
            is Either.Right -> Response(NO_CONTENT)
        }
    }

    fun findOne() = { req: Request ->
        val habitResult = TransactionManager.tx {
            HabitService.findOneBy(habitIdLens(req).value, userLens(req))
        }
        when (habitResult) {
            is Either.Left -> response(habitResult)
            is Either.Right -> habitLens(habitResult.b.toView(), Response(OK))
        }
    }

    private fun response(result: Either.Left<TrackerError>): Response = when (result.a) {
        TrackerError.NotFound -> Response(NOT_FOUND)
        is TrackerError.SyStemError -> Response(INTERNAL_SERVER_ERROR)
    }

    fun findAll() = { req: Request ->
        val userId = userLens(req)
        val habits = TransactionManager.tx {
            HabitService.findAll(userId)
        }.map { it.toView() }
        multipleHabitsLens(habits, Response(OK))
    }

    private fun Request.toHabitDto() = HabitRow(habitLens(this), userLens(this))
}

data class HabitView(
    val name: String,
    val quote: String?,
    val bad: Boolean?,
    val tagList: List<TagRow>,
    val id: UUID? = null
)

data class HabitId @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@JsonValue val value: UUID) {
    override fun toString(): String = this.value.toString()
}