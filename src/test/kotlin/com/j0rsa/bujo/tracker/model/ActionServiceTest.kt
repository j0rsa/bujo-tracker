package com.j0rsa.bujo.tracker.model

import arrow.core.Either.Right
import assertk.assertThat
import assertk.assertions.*
import com.j0rsa.bujo.tracker.TransactionManager
import com.j0rsa.bujo.tracker.TransactionManager.currentTransaction
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.user
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.userId
import org.junit.jupiter.api.Test

internal class ActionServiceTest : TransactionalTest {
    @Test
    fun testCreateWithHabitWhenHabitExistThenActionWithHabitAndTags() {
        TransactionManager.tx {
            val tag = defaultTag(listOf(user))
            val habit = defaultHabit(user, listOf(tag))

            val newAction = defaultHabitActionRow(userId, habit.idValue())
            val result = ActionService.create(newAction)
            assertThat(result.isRight())

            val foundAction = ActionRepository.findById((result as Right<ActionId>).b)
            assertThat(foundAction).isNotNull()
            assertThat(foundAction!!.habit).isEqualTo(habit)
            assertThat(foundAction.tags.toList()).containsOnly(tag)

            currentTransaction().rollback()
        }
    }

    @Test
    fun testCreateWithHabitWhenHabitNotExistThenNotFound() {
        TransactionManager.tx {
            val newAction = defaultHabitActionRow(userId, HabitId.randomValue())
            val result = ActionService.create(newAction)
            assertThat(result.isLeft())
            assertThat(isNotFound(result))

            currentTransaction().rollback()
        }
    }

    @Test
    fun testCreateWithTagsWhenTagExistWithAnotherUser() {
        TransactionManager.tx {
            val anotherUser = defaultUser("anotherUserEmail")
            val tagWithSameName = defaultTag(listOf(anotherUser))
            val actionWithTagWithExistingName = defaultTagActionRow(userId)

            val actionId = ActionService.create(actionWithTagWithExistingName)

            val foundAction = ActionRepository.findById(actionId)
            assertThat(foundAction).isNotNull()
            assertThat(foundAction!!.habit).isNull()
            assertThat(foundAction.tags.toList()).hasSize(1)
            assertThat(foundAction.tags.first().name).isEqualTo(tagWithSameName.name)
            assertThat(foundAction.tags.first().users.toList())
                .extracting { it.idValue() }
                .containsOnly(userId, anotherUser.idValue())

            currentTransaction().rollback()
        }
    }

    @Test
    fun testCreateWithTagsWhenTagNotExist() {
        TransactionManager.tx {
            val newAction = defaultTagActionRow(userId)
            val actionId = ActionService.create(newAction)

            val foundAction = ActionRepository.findById(actionId)!!
            assertThat(foundAction.tags.toList()).hasSize(1)
            assertThat(foundAction.tags.first().users.toList()).extracting { it.idValue() }.containsOnly(userId)

            currentTransaction().rollback()
        }
    }

    @Test
    fun testCreateWithTagsUserHasTagThenNoNewTagCreation() {
        TransactionManager.tx {
            val tagWithSameName = defaultTag(listOf(user))
            val actionId = ActionService.create(defaultTagActionRow(userId))

            val action = ActionRepository.findById(actionId)!!
            assertThat(action.tags.toList()).hasSize(1)
            assertThat(action.tags.toList()).extracting { it.idValue() }.containsOnly(tagWithSameName.idValue())
            assertThat(action.tags.toList()).extracting { it.name }.containsOnly("testTag")
            assertThat(action.tags.toList().flatMap { it.users.toList() }).extracting { it.idValue() }
                .containsOnly(userId)
            currentTransaction().rollback()
        }
    }
}