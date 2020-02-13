package com.j0rsa.bujo.tracker.handler

import assertk.assertThat
import assertk.assertions.isEmpty
import com.google.gson.Gson
import com.j0rsa.bujo.tracker.handler.RequestLens.actionLens
import com.j0rsa.bujo.tracker.model.ActionId
import com.j0rsa.bujo.tracker.model.HabitId
import com.j0rsa.bujo.tracker.model.UserId
import com.j0rsa.bujo.tracker.model.defaultActionRow
import org.eclipse.jetty.server.Response
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class RequestLensTest {

	@Test
	fun testActionLens() {
		val actionView = ActionView()
		val json = Gson().toJson(actionView)
		val request = Request(Method.GET, "/").body(json)
		val result = actionLens(request)
		assertThat(result.values).isEmpty()
	}

	data class ActionView(
		val description: String = "testDescription"
	)
}