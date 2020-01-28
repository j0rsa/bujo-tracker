package com.j0rsa.bujo.tracker.model

import arrow.core.Either.Right
import com.j0rsa.bujo.tracker.TransactionManager
import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.*
import com.j0rsa.bujo.tracker.TransactionManager.currentTransaction
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.user
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.userId

internal class TagServiceTest : TransactionalTest {

    @Test
    fun createTagIfNotExistWhenTagNotExist() {
        TransactionManager.tx {
            val newTag = TagService.createTagIfNotExist(user)(defaultTagRow())
            assertThat(newTag.id.value).isNotNull()
            assertThat(newTag.name).isEqualTo("testTag")
            assertThat(newTag.users.toList()).hasSize(1)
            assertThat(newTag.users).extracting { it.id.value }.containsOnly(userId)
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
                .containsOnly(userId, anotherUser.id.value)
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
            assertThat(tagWithSameName.users).extracting { it.id.value }.containsOnly(userId)
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

            val foundTags = TagService.findAll(userId)
            assertThat(foundTags).extracting { it.name }.containsOnly(*tags.map { it.name }.toTypedArray())
            assertThat(foundTags).extracting { it.id }.containsOnly(*tags.map { it.id.value }.toTypedArray())
            currentTransaction().rollback()
        }
    }

    @Test
    fun testUpdateTagWhenExistTagWithSameName() {
        TransactionManager.tx {
            val oldTag = defaultTag(listOf(user))
            val habit = defaultHabit(user, listOf(oldTag))
            val action = defaultAction(user, listOf(oldTag))

            val existingTag = defaultTag(listOf(user), "existingTagName")
            val tagToUpdate = defaultTagRow("existingTagName", oldTag.id.value)

            val result = TagService.update(userId, tagToUpdate)
            assertThat(result.isRight())
            assertThat((result as Right<TagRow>).b.id!!).isEqualTo(existingTag.id.value)

            val tagAfterDeletion = Tag.findById(oldTag.id.value)
            assertThat(tagAfterDeletion).isNull()

            assertThat(habit.tags.toList()).containsOnly(existingTag)
            assertThat(action.tags.toList()).containsOnly(existingTag)

            currentTransaction().rollback()
        }
    }

    @Test
    fun testUpdateTagWhenActionsAndHabitsAlsoHasExistTagWithSameName() {
        TransactionManager.tx {
            val oldTag = defaultTag(listOf(user))
            val existingTag = defaultTag(listOf(user), "existingTagName")
            val habit = defaultHabit(user, listOf(oldTag, existingTag))
            val action = defaultAction(user, listOf(oldTag, existingTag))

            val tagToUpdate = defaultTagRow("existingTagName", oldTag.id.value)

            val result = TagService.update(userId, tagToUpdate)
            assertThat(result.isRight())

            assertThat(habit.tags.toList()).containsOnly(existingTag)
            assertThat(action.tags.toList()).containsOnly(existingTag)

            currentTransaction().rollback()
        }
    }

    @Test
    fun testUpdateTag() {
        TransactionManager.tx {
            val oldTag = defaultTag(listOf(user))
            val habit = defaultHabit(user, listOf(oldTag))
            val action = defaultAction(user, listOf(oldTag))

            val tagToUpdate = defaultTagRow("existingTagName", oldTag.id.value)

            val result = TagService.update(userId, tagToUpdate)
            assertThat(result.isRight())
            val tagRowResult = (result as Right<TagRow>).b
            assertThat(tagRowResult.name).isEqualTo("existingTagName")

            val tagAfterDeletion = Tag.findById(oldTag.id.value)
            assertThat(tagAfterDeletion).isNull()

            val newTag = Tag.findById(tagRowResult.id!!)
            assertThat(newTag).isNotNull()

            assertThat(habit.tags.toList()).containsOnly(newTag)
            assertThat(action.tags.toList()).containsOnly(newTag)

            currentTransaction().rollback()
        }
    }

    @Test
    fun testUpdateTagWhenUserHasNoTag() {
        TransactionManager.tx {
            val anotherUser = defaultUser("anotherUser")
            val oldTag = defaultTag(listOf(anotherUser))
            val tagToUpdate = defaultTagRow("existingTagName", oldTag.id.value)
            val result = TagService.update(userId, tagToUpdate)
            assertThat(result.isLeft())
            assertThat(isNotFound(result))
            currentTransaction().rollback()
        }
    }
}