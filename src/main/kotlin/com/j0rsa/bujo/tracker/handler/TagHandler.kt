package com.j0rsa.bujo.tracker.handler

import arrow.core.Either
import arrow.core.Right
import com.j0rsa.bujo.tracker.TrackerError
import com.j0rsa.bujo.tracker.blockingTx
import com.j0rsa.bujo.tracker.handler.RequestLens.tagLens
import com.j0rsa.bujo.tracker.handler.RequestLens.userIdLens
import com.j0rsa.bujo.tracker.handler.ResponseState.OK
import com.j0rsa.bujo.tracker.model.TagId
import com.j0rsa.bujo.tracker.model.TagService
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext

object TagHandler {
	fun findAll(vertx: Vertx): suspend (RoutingContext) -> Either<TrackerError, Response<List<TagRow>>> = { req ->
		val tags = blockingTx(vertx) {
			TagService.findAll(userIdLens(req))
		}
		Right(Response(OK, tags))
	}

	fun update(vertx: Vertx): suspend (RoutingContext) -> Either<TrackerError, Response<TagRow>> = { req ->
		blockingTx(vertx) {
			TagService.update(userIdLens(req), tagLens(req))
		}.map { Response(OK, it) }
	}
}

data class Tag(
	val name: String,
	val id: TagId? = null
)
typealias TagRow = Tag