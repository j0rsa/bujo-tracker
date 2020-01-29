package com.j0rsa.bujo.tracker.handler

import arrow.core.Either
import com.j0rsa.bujo.tracker.TrackerError
import com.j0rsa.bujo.tracker.TransactionManager
import com.j0rsa.bujo.tracker.handler.RequestLens.actionIdLens
import com.j0rsa.bujo.tracker.handler.RequestLens.actionLens
import com.j0rsa.bujo.tracker.handler.RequestLens.habitIdLens
import com.j0rsa.bujo.tracker.handler.RequestLens.multipleActionLens
import com.j0rsa.bujo.tracker.handler.RequestLens.response
import com.j0rsa.bujo.tracker.handler.RequestLens.userLens
import com.j0rsa.bujo.tracker.model.*
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.OK

object ActionHandler {
    fun createWithHabit() = { req: Request ->
        when (val actionResult = TransactionManager.tx { ActionService.create(req.toDtoWithHabit()) }) {
            is Either.Left -> response(actionResult)
            is Either.Right -> Response(CREATED).body(actionResult.b.toString())
        }
    }

    fun createWithTags() = { req: Request ->
        val actionId = TransactionManager.tx { ActionService.create(req.toDtoWithTags()) }
        Response(CREATED).body(actionId.toString())
    }

    fun findAll() = { req: Request ->
        val actions = TransactionManager.tx {
            ActionService.findAll(userLens(req))
        }.map { it.toView() }
        multipleActionLens(actions, Response(OK))
    }

    fun findOne() = { req: Request ->
        val result = TransactionManager.tx {
            ActionService.findOneBy(actionIdLens(req), userLens(req))
        }
        responseFrom(result)
    }

    fun update() = { req: Request ->
        val result = TransactionManager.tx {
            ActionService.update(req.toDtoWithTags())
        }
        responseFrom(result)
    }

    fun delete() = { req: Request ->
        val result = TransactionManager.tx {
            ActionService.deleteOne(actionIdLens(req), userLens(req))
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
        ActionRow(actionLens(this), userLens(this), habitIdLens(this))

    private fun Request.toDtoWithTags() = BaseActionRow(actionLens(this), userLens(this))
}

data class ActionView(
    val description: String,
    val tagList: List<TagRow>,
    val habitId: HabitId? = null,
    val id: ActionId? = null
)