package com.j0rsa.bujo.tracker.handler

import com.j0rsa.bujo.tracker.TransactionManager.tx
import com.j0rsa.bujo.tracker.handler.RequestLens.telegramUserLens
import com.j0rsa.bujo.tracker.handler.RequestLens.userIdResponseLens
import com.j0rsa.bujo.tracker.model.UserService
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

object UserHandler {
    fun createOrUpdateUser() = { req: Request ->
        val user = telegramUserLens(req)
        tx {
            UserService.findOneBy(user.id)
                ?.let(updated(user))
                ?: create(user)
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
    val id: Long,
    val firstName: String = "",
    val lastName: String = "",
    val language: String = ""
)

typealias UserInfo = User