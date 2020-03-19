package com.j0rsa.bujo.tracker.model

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Right
import com.j0rsa.bujo.tracker.NotFound
import com.j0rsa.bujo.tracker.SystemError
import com.j0rsa.bujo.tracker.TrackerError
import com.j0rsa.bujo.tracker.handler.UserInfo

object UserService {

	fun findOneBy(telegramId: Long) = UserRepository.findOneByTelegramId(telegramId).firstOrNull()

	fun findOne(telegramId: Long): Either<TrackerError, UserInfo> {
		val foundUsers = UserRepository.findOneByTelegramId(telegramId)
		return when (foundUsers.size) {
			0 -> Left(NotFound)
			1 -> Right(foundUsers.first().toInfo())
			else -> Left(SystemError("found too many actions"))
		}
	}

	fun createUser(user: UserInfo) =
		User.new {
			this.telegramId = user.telegramId
			this.firstName = user.firstName
			this.lastName = user.lastName
			this.language = user.language
		}


	fun updateUser(userInfo: UserInfo) = { user: User ->
		with(user) {
			this.firstName = userInfo.firstName
			this.lastName = userInfo.lastName
			this.language = userInfo.language
			this
		}
	}

	private fun User.toInfo(): UserInfo = UserInfo(
		id = idValue(),
		telegramId = telegramId!!,
		firstName = firstName ?: "",
		lastName = lastName ?: "",
		language = language ?: "en"
	)
}