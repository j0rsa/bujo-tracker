package com.j0rsa.bujo.tracker.handler

import arrow.core.Either
import com.j0rsa.bujo.tracker.TransactionManager
import com.j0rsa.bujo.tracker.handler.RequestLens.response
import com.j0rsa.bujo.tracker.handler.RequestLens.tagLens
import com.j0rsa.bujo.tracker.handler.RequestLens.tagsLens
import com.j0rsa.bujo.tracker.handler.RequestLens.userIdLens
import com.j0rsa.bujo.tracker.model.TagId
import com.j0rsa.bujo.tracker.model.TagService
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

object TagHandler {
    fun findAll() = { req: Request ->
        val tags = TransactionManager.tx {
            TagService.findAll(userIdLens(req))
        }
        tagsLens(tags, Response(Status.OK))
    }

    fun update() = { req: Request ->
        val newTag = TransactionManager.tx {
            TagService.update(userIdLens(req), tagLens(req))
        }
        when (newTag) {
            is Either.Left -> response(newTag)
            is Either.Right -> tagLens(newTag.b, Response(Status.OK))
        }
    }
}

data class Tag(
    val name: String,
    val id: TagId? = null
)
typealias TagRow = Tag