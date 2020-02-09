package com.j0rsa.bujo.tracker.model

import arrow.core.Either.Right
import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.*
import com.j0rsa.bujo.tracker.handler.TagRow
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.user
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.userId

internal class TagServiceTest : TransactionalTest {

    @Test
    fun createTagIfNotExistWhenTagNotExist() {
        tempTx {
            val newTag = TagService.createTagIfNotExist(user)(defaultTagRow())
            assertThat(newTag.idValue()).isNotNull()
            assertThat(newTag.name).isEqualTo("testTag")
            assertThat(newTag.users.toList()).hasSize(1)
            assertThat(newTag.users).extracting { it.idValue() }.containsOnly(userId)
        }
    }

    @Test
    fun createTagIfNotExistWhenTagExistsWithAnotherUser() {
        tempTx {
            val anotherUser = defaultUser("anotherUser")
            val tag = defaultTag(listOf(anotherUser))
            val tagWithSameName = TagService.createTagIfNotExist(user)(defaultTagRow())
            assertThat(tagWithSameName.idValue()).isEqualTo(tag.idValue())
            assertThat(tagWithSameName.name).isEqualTo("testTag")
            assertThat(tagWithSameName.users.toList()).hasSize(2)
            assertThat(tagWithSameName.users).extracting { it.idValue() }
                .containsOnly(userId, anotherUser.idValue())
        }
    }

    @Test
    fun createTagIfNotExistWhenTagExistsWithSameUser() {
        tempTx {
            val tag = defaultTag(listOf(user))
            val tagWithSameName = TagService.createTagIfNotExist(user)(defaultTagRow())
            assertThat(tagWithSameName.idValue()).isEqualTo(tag.idValue())
            assertThat(tagWithSameName.name).isEqualTo("testTag")
            assertThat(tagWithSameName.users.toList()).hasSize(1)
            assertThat(tagWithSameName.users).extracting { it.idValue() }.containsOnly(userId)
        }
    }

    @Test
    fun testFindAll() {
        tempTx {
            val tags = listOf(
                defaultTag(listOf(user), "tag1"),
                defaultTag(listOf(user), "tag2"),
                defaultTag(listOf(user), "tag3")
            )

            val foundTags = TagService.findAll(userId)
            assertThat(foundTags).extracting { it.name }.containsOnly(*tags.map { it.name }.toTypedArray())
            assertThat(foundTags).extracting { it.id }.containsOnly(*tags.map { it.idValue() }.toTypedArray())
        }
    }

    @Test
    fun testUpdateTagWhenExistTagWithSameName() {
        tempTx {
            val oldTag = defaultTag(listOf(user))
            val habit = defaultHabit(user, listOf(oldTag))
            val action = defaultAction(user, listOf(oldTag))

            val existingTag = defaultTag(listOf(user), "existingTagName")
            val tagToUpdate = defaultTagRow("existingTagName", oldTag.idValue())

            val result = TagService.update(userId, tagToUpdate)
            assertThat(result.isRight())
            assertThat((result as Right<TagRow>).b.id!!).isEqualTo(existingTag.idValue())

            val tagAfterDeletion = TagRepository.findById(oldTag.idValue())
            assertThat(tagAfterDeletion).isNull()

            assertThat(habit.tags.toList()).containsOnly(existingTag)
            assertThat(action.tags.toList()).containsOnly(existingTag)
        }
    }

    @Test
    fun testUpdateTagWhenActionsAndHabitsAlsoHasExistTagWithSameName() {
        tempTx {
            val oldTag = defaultTag(listOf(user))
            val existingTag = defaultTag(listOf(user), "existingTagName")
            val habit = defaultHabit(user, listOf(oldTag, existingTag))
            val action = defaultAction(user, listOf(oldTag, existingTag))

            val tagToUpdate = defaultTagRow("existingTagName", oldTag.idValue())

            val result = TagService.update(userId, tagToUpdate)
            assertThat(result.isRight())

            assertThat(habit.tags.toList()).containsOnly(existingTag)
            assertThat(action.tags.toList()).containsOnly(existingTag)
        }
    }

    @Test
    fun testUpdateTag() {
        tempTx {
            val oldTag = defaultTag(listOf(user))
            val habit = defaultHabit(user, listOf(oldTag))
            val action = defaultAction(user, listOf(oldTag))

            val tagToUpdate = defaultTagRow("existingTagName", oldTag.idValue())

            val result = TagService.update(userId, tagToUpdate)
            assertThat(result.isRight())
            val tagRowResult = (result as Right<TagRow>).b
            assertThat(tagRowResult.name).isEqualTo("existingTagName")

            val tagAfterDeletion = TagRepository.findById(oldTag.idValue())
            assertThat(tagAfterDeletion).isNull()

            val newTag = TagRepository.findById(tagRowResult.id!!)
            assertThat(newTag).isNotNull()

            assertThat(habit.tags.toList()).containsOnly(newTag)
            assertThat(action.tags.toList()).containsOnly(newTag)
        }
    }

    @Test
    fun testUpdateTagWhenUserHasNoTag() {
        tempTx {
            val anotherUser = defaultUser("anotherUser")
            val oldTag = defaultTag(listOf(anotherUser))
            val tagToUpdate = defaultTagRow("existingTagName", oldTag.idValue())
            val result = TagService.update(userId, tagToUpdate)
            assertThat(result.isLeft())
            assertThat(isNotFound(result))
        }
    }
}