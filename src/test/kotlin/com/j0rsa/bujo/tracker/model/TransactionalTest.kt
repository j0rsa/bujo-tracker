package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.TransactionManager
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

internal interface TransactionalTest {
    companion object {
        lateinit var user: User
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            TransactionManager.tx {
                createSchema()
                user = defaultUser()
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