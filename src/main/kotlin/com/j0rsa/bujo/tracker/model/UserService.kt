package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.handler.UserInfo

object UserService {

    fun findOneBy(telegramId: Long) = UserRepository.findOneByTelegramId(telegramId)

    fun createUser(user: UserInfo) =
        User.new {
            this.telegramId = user.id
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
}