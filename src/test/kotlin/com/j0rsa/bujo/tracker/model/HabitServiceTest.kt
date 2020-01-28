package com.j0rsa.bujo.tracker.model

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.*
import com.j0rsa.bujo.tracker.NotFound
import com.j0rsa.bujo.tracker.TrackerError
import com.j0rsa.bujo.tracker.TransactionManager
import com.j0rsa.bujo.tracker.TransactionManager.currentTransaction
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.user
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.userId
import org.junit.jupiter.api.Test

import java.util.*

internal class HabitServiceTest : TransactionalTest {

    @Test
    fun testCreateWithTagCreation() {
        TransactionManager.tx {
            val habitId = HabitService.create(defaultHabitRow(userId))

            val habit = Habit.findById(habitId)
            assertThat(habit).isNotNull()
            assertThat(habit!!.name).isEqualTo("testHabit")
            assertThat(habit.userId.value).isEqualTo(userId)
            assertThat(habit.tags.toList()).extracting { it.name }.containsOnly("testTag")
            assertThat(habit.tags.toList().flatMap { it.users.toList() }).extracting { it.id.value }
                .containsOnly(userId)
            currentTransaction().rollback()
        }
    }

    @Test
    fun testCreateWithSeveralTagCreation() {
        TransactionManager.tx {
            val habitId = HabitService.create(
                defaultHabitRow(
                    userId, tags = listOf(
                        defaultTagRow("tag1"),
                        defaultTagRow("tag2"),
                        defaultTagRow("tag3")
                    )
                )
            )

            val habit = Habit.findById(habitId)!!
            assertThat(habit.tags.toList()).hasSize(3)
            assertThat(habit.tags.toList()).extracting { it.name }.containsOnly("tag1", "tag2", "tag3")
            assertThat(habit.tags.toList().flatMap { it.users.toList() }).extracting { it.id.value }
                .containsOnly(userId)
            currentTransaction().rollback()
        }
    }

    @Test
    fun whenAnotherUserHasTagWithSameNameThenNoNewTagCreation() {
        TransactionManager.tx {
            val anotherUser = defaultUser("anotherUser")
            val tagWithSameName = defaultTag(listOf(anotherUser))
            val habitId = HabitService.create(defaultHabitRow(userId))

            val habit = Habit.findById(habitId)!!
            assertThat(habit.tags.toList()).hasSize(1)
            assertThat(habit.tags.toList()).extracting { it.id.value }.containsOnly(tagWithSameName.id.value)
            assertThat(habit.tags.toList()).extracting { it.name }.containsOnly("testTag")
            assertThat(habit.tags.toList().flatMap { it.users.toList() }).extracting { it.id.value }
                .containsOnly(userId, anotherUser.id.value)
            currentTransaction().rollback()
        }
    }

    @Test
    fun whenUserHasTagThenNoNewTagCreation() {
        TransactionManager.tx {
            val tagWithSameName = defaultTag(listOf(user))
            val habitId = HabitService.create(defaultHabitRow(userId))

            val habit = Habit.findById(habitId)!!
            assertThat(habit.tags.toList()).hasSize(1)
            assertThat(habit.tags.toList()).extracting { it.id.value }.containsOnly(tagWithSameName.id.value)
            assertThat(habit.tags.toList()).extracting { it.name }.containsOnly("testTag")
            assertThat(habit.tags.toList().flatMap { it.users.toList() }).extracting { it.id.value }
                .containsOnly(userId)
            currentTransaction().rollback()
        }
    }

    @Test
    fun findOneWhenHabitExist() {
        TransactionManager.tx {
            val tag = defaultTag(listOf(user))
            val habit = defaultHabit(user, listOf(tag))

            val result = HabitService.findOneBy(habit.id.value, userId)
            assertThat(result.isRight())
            currentTransaction().rollback()
        }
    }

    @Test
    fun findOneWhenHabitNotExist() {
        TransactionManager.tx {
            val result = HabitService.findOneBy(UUID.randomUUID(), userId)
            assertThat(result.isLeft())
            assertThat(isNotFound(result))
            currentTransaction().rollback()
        }
    }

    @Test
    fun deleteWhenHabitExist() {
        TransactionManager.tx {
            val tag = defaultTag(listOf(user))
            val habit = defaultHabit(user, listOf(tag))

            val result = HabitService.deleteOne(habit.id.value, userId)
            assertThat(result.isRight())

            val habitAfterDeletion = Habit.findById(habit.id.value)
            assertThat(habitAfterDeletion).isNull()
            currentTransaction().rollback()
        }
    }

    @Test
    fun deleteWhenHabitNotExist() {
        TransactionManager.tx {
            val result = HabitService.deleteOne(UUID.randomUUID(), userId)
            assertThat(result.isLeft())
            assertThat(isNotFound(result))
            currentTransaction().rollback()
        }
    }

    @Test
    fun testUpdateHabitWhenHabitExist() {
        TransactionManager.tx {
            val tag = defaultTag(listOf(user))
            val habit = defaultHabit(user, listOf(tag))

            val tags = listOf(defaultTagRow(), defaultTagRow("anotherTag"))
            val habitToUpdate = defaultHabitRow(userId, "newName", tags, habit.id.value)

            val result = HabitService.update(habitToUpdate)
            assertThat(result.isRight())

            val foundHabit = Habit.findById(habit.id.value)!!
            assertThat(foundHabit.name).isEqualTo("newName")
            assertThat(foundHabit.tags.toList()).hasSize(2)
            assertThat(foundHabit.tags.toList().map { it.name }).containsOnly(*tags.map { it.name }.toTypedArray())
            currentTransaction().rollback()
        }
    }

    @Test
    fun testUpdateHabitWhenHabitNotExist() {
        TransactionManager.tx {
            val notExistingHabit = defaultHabitRow(userId, id = UUID.randomUUID())
            val result = HabitService.update(notExistingHabit)
            assertThat(result.isLeft())
            assertThat(isNotFound(result))
            currentTransaction().rollback()
        }
    }

}