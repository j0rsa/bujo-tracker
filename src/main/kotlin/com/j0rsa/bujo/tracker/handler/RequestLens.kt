package com.j0rsa.bujo.tracker.handler

import arrow.core.Either
import com.j0rsa.bujo.tracker.TrackerError
import com.j0rsa.bujo.tracker.TrackerJackson.auto
import com.j0rsa.bujo.tracker.model.HabitId
import com.j0rsa.bujo.tracker.model.TagRow
import com.j0rsa.bujo.tracker.model.UserId
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.lens.uuid

object RequestLens {
    val habitLens = Body.auto<HabitView>().toLens()
    val multipleHabitsLens = Body.auto<List<HabitView>>().toLens()
    val habitIdLens = Path.uuid().map(::HabitId).of("id")
    val userLens = Header.uuid().map(::UserId).required("X-Auth-Id")
    val tagLens = Body.auto<TagRow>().toLens()
    val tagsLens = Body.auto<List<TagRow>>().toLens()

    fun response(result: Either.Left<TrackerError>): Response = when (result.a) {
        TrackerError.NotFound -> Response(Status.NOT_FOUND)
        is TrackerError.SyStemError -> Response(Status.INTERNAL_SERVER_ERROR)
    }
}