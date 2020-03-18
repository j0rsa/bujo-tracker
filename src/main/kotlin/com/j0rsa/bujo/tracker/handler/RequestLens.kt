package com.j0rsa.bujo.tracker.handler

import com.j0rsa.bujo.tracker.Serializer.fromJson
import com.j0rsa.bujo.tracker.model.ActionId
import com.j0rsa.bujo.tracker.model.HabitId
import com.j0rsa.bujo.tracker.model.UserId
import io.vertx.ext.web.RoutingContext


object RequestLens {
	val habitLens = Body.auto<Habit>()
	val habitIdLens = Path.of("id").map(::HabitId)

	val userIdLens = Header.required("X-Auth-Id").map(::UserId)
	val telegramUserIdLens = Path.of("telegram_id").map(String::toLong)
	val telegramUserLens = Body.auto<User>()

	val tagLens = Body.auto<Tag>()
	val actionLens = Body.auto<ActionView>()
	val actionIdPathLens = Path.of("id").map(::ActionId)
	val valueLens = Body.auto<ValueRow>()
}

object Path {
	fun of(paramName: String) = Mapping { ctx: RoutingContext ->
		ctx.request().getParam(paramName)
	}
}

object Header {
	fun required(name: String) = Mapping { ctx: RoutingContext ->
		val header = ctx.request().getHeader(name)
		checkNotNull(header)
		header
	}
}

object Body {
	inline fun <reified T> auto() = Mapping { ctx: RoutingContext ->
		fromJson<T>(ctx.bodyAsString)
	}
}

class Mapping<IN, OUT>(val fn: (IN) -> OUT) {
	inline fun <reified NEXT> map(crossinline nextOut: (OUT) -> NEXT): Mapping<IN, NEXT> = Mapping { nextOut(fn(it)) }

	operator fun invoke(value: IN): OUT = fn(value)
}