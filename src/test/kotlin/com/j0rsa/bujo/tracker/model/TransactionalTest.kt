package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.TransactionManager
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.util.*

internal interface TransactionalTest {
    companion object {
        lateinit var user: User
        lateinit var userId: UUID
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            TransactionManager.tx {
                createSchema()
                user = defaultUser()
                userId = user.id.value
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