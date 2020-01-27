package com.j0rsa.bujo.tracker.handler

import com.j0rsa.bujo.tracker.TransactionManager
import com.j0rsa.bujo.tracker.handler.RequestLens.tagsLens
import com.j0rsa.bujo.tracker.handler.RequestLens.userLens
import com.j0rsa.bujo.tracker.model.TagService
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

object TagHandler {
    fun findAll() = { req: Request ->
        val tags = TransactionManager.tx {
            TagService.findAll(userLens(req))
        }
        tagsLens(tags, Response(Status.OK))
    }
}