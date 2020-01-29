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
    fun testCreateWithHabitWhenHabitExistThenActionWithHabitAndTagsFromView() {
        TransactionManager.tx {
            val tag = defaultTag(listOf(user))
            val habit = defaultHabit(user, listOf(tag))

            val newAction = defaultActionRow(userId, habit.idValue())
            val result = ActionService.create(newAction)
            assertThat(result.isRight())

            val foundAction = ActionRepository.findById((result as Right<ActionId>).b)
            assertThat(foundAction).isNotNull()
            assertThat(foundAction!!.habit).isEqualTo(habit)
            assertThat(foundAction.tags.toList().map { it.name }).containsOnly("testTag")

            currentTransaction().rollback()
        }
    }

    @Test
    fun testCreateWithHabitWhenHabitNotExistThenNotFound() {
        TransactionManager.tx {
            val newAction = defaultActionRow(userId, HabitId.randomValue())
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
            val actionWithTagWithExistingName = defaultBaseActionRow(userId)

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
            val newAction = defaultBaseActionRow(userId)
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
            val actionId = ActionService.create(defaultBaseActionRow(userId))

            val action = ActionRepository.findById(actionId)!!
            assertThat(action.tags.toList()).hasSize(1)
            assertThat(action.tags.toList()).extracting { it.idValue() }.containsOnly(tagWithSameName.idValue())
            assertThat(action.tags.toList()).extracting { it.name }.containsOnly("testTag")
            assertThat(action.tags.toList().flatMap { it.users.toList() }).extracting { it.idValue() }
                .containsOnly(userId)
            currentTransaction().rollback()
        }
    }

    @Test
    fun deleteWhenActionExist() {
        TransactionManager.tx {
            val tag = defaultTag(listOf(user))
            val action = defaultAction(user, listOf(tag))

            val result = ActionService.deleteOne(action.idValue(), userId)
            assertThat(result.isRight())

            val recordAfterDeletion = ActionRepository.findById(action.idValue())
            assertThat(recordAfterDeletion).isNull()
            currentTransaction().rollback()
        }
    }

    @Test
    fun deleteWhenActionNotExist() {
        TransactionManager.tx {
            val result = ActionService.deleteOne(ActionId.randomValue(), userId)
            assertThat(result.isLeft())
            assertThat(isNotFound(result))
            currentTransaction().rollback()
        }
    }

    @Test
    fun testUpdateActionWhenActionExist() {
        TransactionManager.tx {
            val tag = defaultTag(listOf(user))
            val habit = defaultHabit(user, listOf(tag))
            val action = defaultAction(user, listOf(tag), actionHabit = habit)

            val tags = listOf(defaultTagRow(), defaultTagRow("anotherTag"))
            val actionToUpdate = defaultBaseActionRow(userId, "newName", tags, action.idValue())

            val result = ActionService.update(actionToUpdate)
            assertThat(result.isRight())

            val foundHabit = ActionRepository.findById(action.idValue())!!
            assertThat(foundHabit.name).isEqualTo("newName")
            assertThat(foundHabit.tags.toList()).hasSize(2)
            assertThat(foundHabit.tags.toList().map { it.name }).containsOnly(*tags.map { it.name }.toTypedArray())
            currentTransaction().rollback()
        }
    }

    @Test
    fun testUpdateActionWhenHabitNotExist() {
        TransactionManager.tx {
            val notExistingAction = defaultBaseActionRow(userId, id = ActionId.randomValue())
            val result = ActionService.update(notExistingAction)
            assertThat(result.isLeft())
            assertThat(isNotFound(result))
            currentTransaction().rollback()
        }
    }


    @Test
    fun findOneWhenActionExist() {
        TransactionManager.tx {
            val tag = defaultTag(listOf(user))
            val action = defaultAction(user, listOf(tag))

            val result = ActionService.findOneBy(action.idValue(), userId)
            assertThat(result.isRight())
            currentTransaction().rollback()
        }
    }

    @Test
    fun findOneWhenActionNotExist() {
        TransactionManager.tx {
            val result = ActionService.findOneBy(ActionId.randomValue(), userId)
            assertThat(result.isLeft())
            assertThat(isNotFound(result))
            currentTransaction().rollback()
        }
    }
}