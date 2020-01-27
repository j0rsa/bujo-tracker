package com.j0rsa.bujo.tracker.model

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.*
import com.j0rsa.bujo.tracker.NotFound
import com.j0rsa.bujo.tracker.TransactionManager
import com.j0rsa.bujo.tracker.TransactionManager.currentTransaction
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.user
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.util.*

internal class HabitServiceTest : TransactionalTest {

    @Test
    fun testCreateWithTagCreation() {
        TransactionManager.tx {
            val habitId = HabitService.create(defaultHabitRow(user.id.value))

            val habit = Habit.findById(habitId)
            assertThat(habit).isNotNull()
            assertThat(habit!!.name).isEqualTo("testHabit")
            assertThat(habit.userId.value).isEqualTo(user.id.value)
            assertThat(habit.tags.toList()).extracting { it.name }.containsOnly("testTag")
            assertThat(habit.tags.toList().flatMap { it.users.toList() }).extracting { it.id.value }
                .containsOnly(user.id.value)
            currentTransaction().rollback()
        }
    }

    @Test
    fun testCreateWithSeveralTagCreation() {
        TransactionManager.tx {
            val habitId = HabitService.create(
                defaultHabitRow(
                    user.id.value, tags = listOf(
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
                .containsOnly(user.id.value)
            currentTransaction().rollback()
        }
    }

    @Test
    fun whenAnotherUserHasTagWithSameNameThenNoNewTagCreation() {
        TransactionManager.tx {
            val anotherUser = defaultUser("anotherUser")
            val tagWithSameName = defaultTag(listOf(anotherUser))
            val habitId = HabitService.create(defaultHabitRow(user.id.value))

            val habit = Habit.findById(habitId)!!
            assertThat(habit.tags.toList()).hasSize(1)
            assertThat(habit.tags.toList()).extracting { it.id.value }.containsOnly(tagWithSameName.id.value)
            assertThat(habit.tags.toList()).extracting { it.name }.containsOnly("testTag")
            assertThat(habit.tags.toList().flatMap { it.users.toList() }).extracting { it.id.value }
                .containsOnly(user.id.value, anotherUser.id.value)
            currentTransaction().rollback()
        }
    }

    @Test
    fun whenUserHasTagThenNoNewTagCreation() {
        TransactionManager.tx {
            val tagWithSameName = defaultTag(listOf(user))
            val habitId = HabitService.create(defaultHabitRow(user.id.value))

            val habit = Habit.findById(habitId)!!
            assertThat(habit.tags.toList()).hasSize(1)
            assertThat(habit.tags.toList()).extracting { it.id.value }.containsOnly(tagWithSameName.id.value)
            assertThat(habit.tags.toList()).extracting { it.name }.containsOnly("testTag")
            assertThat(habit.tags.toList().flatMap { it.users.toList() }).extracting { it.id.value }
                .containsOnly(user.id.value)
            currentTransaction().rollback()
        }
    }

    @Test
    fun findOneWhenHabitExist() {
        TransactionManager.tx {
            val tag = defaultTag(listOf(user))
            val habit = defaultHabit(user, listOf(tag))

            val result = HabitService.findOneBy(habit.id.value, user.id.value)
            assertThat(result.isRight())
            currentTransaction().rollback()
        }
    }

    @Test
    fun findOneWhenHabitNotExist() {
        TransactionManager.tx {
            val result = HabitService.findOneBy(UUID.randomUUID(), user.id.value)
            assertThat(result.isLeft())
            assertThat((result as Either.Left).a == NotFound)
            currentTransaction().rollback()
        }
    }

    @Test
    fun deleteWhenHabitExist() {
        TransactionManager.tx {
            val tag = defaultTag(listOf(user))
            val habit = defaultHabit(user, listOf(tag))

            val result = HabitService.deleteOne(habit.id.value, user.id.value)
            assertThat(result.isRight())

            val habitAfterDeletion = Habit.findById(habit.id.value)
            assertThat(habitAfterDeletion).isNull()
            currentTransaction().rollback()
        }
    }

    @Test
    fun deleteWhenHabitNotExist() {
        TransactionManager.tx {
            val result = HabitService.deleteOne(UUID.randomUUID(), user.id.value)
            assertThat(result.isLeft())
            assertThat((result as Either.Left).a == NotFound)
            currentTransaction().rollback()
        }
    }
}