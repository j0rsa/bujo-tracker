package com.j0rsa.bujo.tracker.model

object UserRepository {
    fun findOne(id: UserId) = User.findById(id.value)
    fun findOneByTelegramId(id: Long) = User.find { Users.telegramId.eq(id)}.toList()
}