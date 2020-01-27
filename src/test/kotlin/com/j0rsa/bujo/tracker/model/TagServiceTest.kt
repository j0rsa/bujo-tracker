package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.TransactionManager
import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.*
import com.j0rsa.bujo.tracker.TransactionManager.currentTransaction
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.user

internal class TagServiceTest : TransactionalTest {

    @Test
    fun createTagIfNotExistWhenTagNotExist() {
        TransactionManager.tx {
            val newTag = TagService.createTagIfNotExist(user)(defaultTagRow())
            assertThat(newTag.id.value).isNotNull()
            assertThat(newTag.name).isEqualTo("testTag")
            assertThat(newTag.users.toList()).hasSize(1)
            assertThat(newTag.users).extracting { it.id.value }.containsOnly(user.id.value)
            currentTransaction().rollback()
        }
    }

    @Test
    fun createTagIfNotExistWhenTagExistsWithAnotherUser() {
        TransactionManager.tx {
            val anotherUser = defaultUser("anotherUser")
            val tag = defaultTag(listOf(anotherUser))
            val tagWithSameName = TagService.createTagIfNotExist(user)(defaultTagRow())
            assertThat(tagWithSameName.id.value).isEqualTo(tag.id.value)
            assertThat(tagWithSameName.name).isEqualTo("testTag")
            assertThat(tagWithSameName.users.toList()).hasSize(2)
            assertThat(tagWithSameName.users).extracting { it.id.value }
                .containsOnly(user.id.value, anotherUser.id.value)
            currentTransaction().rollback()
        }
    }

    @Test
    fun createTagIfNotExistWhenTagExistsWithSameUser() {
        TransactionManager.tx {
            val tag = defaultTag(listOf(user))
            val tagWithSameName = TagService.createTagIfNotExist(user)(defaultTagRow())
            assertThat(tagWithSameName.id.value).isEqualTo(tag.id.value)
            assertThat(tagWithSameName.name).isEqualTo("testTag")
            assertThat(tagWithSameName.users.toList()).hasSize(1)
            assertThat(tagWithSameName.users).extracting { it.id.value }.containsOnly(user.id.value)
            currentTransaction().rollback()
        }
    }

    @Test
    fun testFindAll() {
        TransactionManager.tx {
            val tags = listOf(
                defaultTag(listOf(user), "tag1"),
                defaultTag(listOf(user), "tag2"),
                defaultTag(listOf(user), "tag3")
            )

            val foundTags = TagService.findAll(user.id.value)
            assertThat(foundTags).extracting { it.name }.containsOnly(*tags.map { it.name }.toTypedArray())
            assertThat(foundTags).extracting { it.id }.containsOnly(*tags.map { it.id.value }.toTypedArray())
            currentTransaction().rollback()
        }
    }
}