package com.j0rsa.bujo.tracker.handler

import com.j0rsa.bujo.tracker.TrackerJackson.auto
import com.j0rsa.bujo.tracker.TransactionManager
import com.j0rsa.bujo.tracker.model.HabitRow
import com.j0rsa.bujo.tracker.model.HabitService
import com.j0rsa.bujo.tracker.model.TagRow
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED
import org.http4k.lens.Header
import org.http4k.lens.uuid
import java.util.*

object HabitHandler {
    private val habitLens = Body.auto<HabitView>().toLens()
    private val userLens = Header.uuid().required("X-Auth-Id")

    fun create() = { req: Request ->
        val habitId = TransactionManager.tx { HabitService.create(req.toHabitDto()) }
        Response(CREATED).body(habitId.toString())
    }

    private fun Request.toHabitDto() = HabitRow(habitLens(this), userLens(this))
}

data class HabitView(
    val name: String,
    val quote: String?,
    val bad: Boolean,
    val tagList: List<TagRow>,
    val id: UUID? = null
)