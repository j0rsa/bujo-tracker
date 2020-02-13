package com.j0rsa.bujo.tracker.handler

import arrow.core.Either
import com.j0rsa.bujo.tracker.TransactionManager.tx
import com.j0rsa.bujo.tracker.handler.RequestLens.response
import com.j0rsa.bujo.tracker.handler.RequestLens.telegramUserIdLens
import com.j0rsa.bujo.tracker.handler.RequestLens.telegramUserLens
import com.j0rsa.bujo.tracker.handler.RequestLens.userIdResponseLens
import com.j0rsa.bujo.tracker.model.UserId
import com.j0rsa.bujo.tracker.model.UserService
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

object UserHandler {
	fun createOrUpdateUser() = { req: Request ->
		val user = telegramUserLens(req)
		tx {
			UserService.findOneBy(user.telegramId)
				?.let(updated(user))
				?: create(user)
		}
	}

	fun findUser() = { req: Request ->
		when (val result = tx { UserService.findOne(telegramUserIdLens(req)) }) {
			is Either.Left -> response(result)
			is Either.Right -> telegramUserLens(result.b, Response(Status.OK))
		}
	}

	private fun updated(userInfo: User) = { user: com.j0rsa.bujo.tracker.model.User ->
		val idValue = UserService.updateUser(userInfo)(user).idValue()
		userIdResponseLens(idValue, Response(Status.OK))
	}

	private fun create(userInfo: User): Response {
		val idValue = UserService.createUser(userInfo).idValue()
		return userIdResponseLens(idValue, Response(Status.CREATED))
	}
}

data class User(
	val id: UserId? = null,
	val telegramId: Long,
	val firstName: String = "",
	val lastName: String = "",
	val language: String = ""
)

typealias UserInfo = User