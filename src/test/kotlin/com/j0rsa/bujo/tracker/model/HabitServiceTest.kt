package com.j0rsa.bujo.tracker.model

import assertk.assertThat
import assertk.assertions.*
import com.j0rsa.bujo.tracker.TransactionManager
import com.j0rsa.bujo.tracker.TransactionManager.currentTransaction
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.user
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

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
}