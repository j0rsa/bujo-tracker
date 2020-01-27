package com.j0rsa.bujo.tracker.handler

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.j0rsa.bujo.tracker.TrackerJackson.auto
import com.j0rsa.bujo.tracker.model.TagRow
import org.http4k.core.Body
import org.http4k.lens.Header
import org.http4k.lens.Path
import org.http4k.lens.uuid
import java.util.*

object RequestLens {
    val habitLens = Body.auto<HabitView>().toLens()
    val multipleHabitsLens = Body.auto<List<HabitView>>().toLens()
    val habitIdLens = Path.uuid().map(::HabitId).of("id")
    val userLens = Header.uuid().required("X-Auth-Id")
    val tagsLens = Body.auto<List<TagRow>>().toLens()

    data class HabitId @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(@JsonValue val value: UUID) {
        override fun toString(): String = this.value.toString()
    }
}