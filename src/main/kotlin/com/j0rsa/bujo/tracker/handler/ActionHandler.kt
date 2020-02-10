package com.j0rsa.bujo.tracker.handler

import arrow.core.Either
import com.j0rsa.bujo.tracker.TrackerError
import com.j0rsa.bujo.tracker.TransactionManager.tx
import com.j0rsa.bujo.tracker.handler.RequestLens.actionIdLens
import com.j0rsa.bujo.tracker.handler.RequestLens.actionLens
import com.j0rsa.bujo.tracker.handler.RequestLens.habitIdLens
import com.j0rsa.bujo.tracker.handler.RequestLens.multipleActionLens
import com.j0rsa.bujo.tracker.handler.RequestLens.response
import com.j0rsa.bujo.tracker.handler.RequestLens.userIdLens
import com.j0rsa.bujo.tracker.model.*
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.OK

object ActionHandler {
    fun createWithHabit() = { req: Request ->
        when (val actionResult = tx { ActionService.create(req.toDtoWithHabit()) }) {
            is Either.Left -> response(actionResult)
            is Either.Right -> Response(CREATED).body(actionResult.b.toString())
        }
    }

    fun createWithTags() = { req: Request ->
        val actionId = tx { ActionService.create(req.toDtoWithTags()) }
        Response(CREATED).body(actionId.toString())
    }

    fun findAll() = { req: Request ->
        val actions = tx {
            ActionService.findAll(userIdLens(req))
        }.map { it.toView() }
        multipleActionLens(actions, Response(OK))
    }

    fun findOne() = { req: Request ->
        val result = tx {
            ActionService.findOneBy(actionIdLens(req), userIdLens(req))
        }
        responseFrom(result)
    }

    fun update() = { req: Request ->
        val result = tx {
            ActionService.update(req.toDtoWithTags())
        }
        responseFrom(result)
    }

    fun delete() = { req: Request ->
        val result = tx {
            ActionService.deleteOne(actionIdLens(req), userIdLens(req))
        }
        when (result) {
            is Either.Left -> response(result)
            is Either.Right -> Response(Status.NO_CONTENT)
        }
    }

    private fun responseFrom(result: Either<TrackerError, ActionRow>): Response = when (result) {
        is Either.Left -> response(result)
        is Either.Right -> actionLens(result.b.toView(), Response(OK))
    }

    private fun Request.toDtoWithHabit() =
        ActionRow(actionLens(this), userIdLens(this), habitIdLens(this))

    private fun Request.toDtoWithTags() = BaseActionRow(actionLens(this), userIdLens(this))
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
    val value: String?
)

typealias ValueRow = Value