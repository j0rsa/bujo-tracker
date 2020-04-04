package com.j0rsa.bujo.tracker.repository

import com.j0rsa.bujo.tracker.model.User
import com.j0rsa.bujo.tracker.model.UserId
import com.j0rsa.bujo.tracker.model.Users

object UserRepository {
	fun findOne(id: UserId) = User.findById(id.value)
	fun findOneByTelegramId(id: Long) = User.find { Users.telegramId.eq(id) }.toList()
}