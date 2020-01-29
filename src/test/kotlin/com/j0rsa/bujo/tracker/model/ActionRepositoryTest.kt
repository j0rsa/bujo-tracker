package com.j0rsa.bujo.tracker.model

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.j0rsa.bujo.tracker.TransactionManager
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.user
import org.junit.jupiter.api.Test

internal class ActionRepositoryTest : TransactionalTest {
    @Test
    fun foundNothingWhenHasBothTags() {
        TransactionManager.tx {
            val oneTag = defaultTag(listOf(user), "tag1")
            val anotherTag = defaultTag(listOf(user), "tag2")
            defaultAction(user, listOf(oneTag, anotherTag))

            val result = ActionRepository.findAllWithOneTagWithoutAnother(oneTag.idValue(), anotherTag.idValue())
            assertThat(result).isEmpty()
            TransactionManager.currentTransaction().rollback()
        }
    }

    @Test
    fun foundOneActionWhenHasOnlyOneTag() {
        TransactionManager.tx {
            val oneTag = defaultTag(listOf(user), "tag1")
            val anotherTag = defaultTag(listOf(user), "tag2")
            defaultAction(user, listOf(oneTag, anotherTag))
            defaultAction(user, listOf(anotherTag))
            val actionWithOnlyOneTag = defaultAction(user, listOf(oneTag))

            val result = ActionRepository.findAllWithOneTagWithoutAnother(oneTag.idValue(), anotherTag.idValue())
            assertThat(result).hasSize(1)
            assertThat(result.first().idValue()).isEqualTo(actionWithOnlyOneTag.idValue())
            TransactionManager.currentTransaction().rollback()
        }
    }
}