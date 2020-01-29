package com.j0rsa.bujo.tracker.model

import arrow.core.Either
import com.j0rsa.bujo.tracker.NotFound
import com.j0rsa.bujo.tracker.TrackerError
import com.j0rsa.bujo.tracker.TransactionManager
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import kotlin.properties.Delegates

internal interface TransactionalTest {
    fun <T> isNotFound(result: Either<TrackerError, T>) =
        (result as Either.Left).a == NotFound

    companion object {
        lateinit var user: User
        var userId: UserId by Delegates.notNull()
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            TransactionManager.tx {
                createSchema()
                user = defaultUser()
                userId = user.idValue()
            }
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            TransactionManager.tx {
                dropSchema()
            }
        }
    }
}