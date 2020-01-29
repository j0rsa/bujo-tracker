package com.j0rsa.bujo.tracker.handler

import arrow.core.Either
import com.j0rsa.bujo.tracker.TransactionManager
import com.j0rsa.bujo.tracker.handler.RequestLens.actionLens
import com.j0rsa.bujo.tracker.handler.RequestLens.habitIdLens
import com.j0rsa.bujo.tracker.handler.RequestLens.multipleActionLens
import com.j0rsa.bujo.tracker.handler.RequestLens.response
import com.j0rsa.bujo.tracker.handler.RequestLens.userLens
import com.j0rsa.bujo.tracker.model.*
import org.http4k.core.Request
import org.http4k.core.Response
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

    private fun Request.toDtoWithHabit() =
        HabitActionRow(actionLens(this), userLens(this), habitIdLens(this))

    private fun Request.toDtoWithTags() = TagActionRow(actionLens(this), userLens(this))
}

data class ActionView(
    val name: String,
    val tagList: List<TagRow>,
    val habitId: HabitId? = null,
    val id: ActionId? = null
)