package com.j0rsa.bujo.tracker.model

import assertk.assertThat
import assertk.assertions.*
import com.j0rsa.bujo.tracker.TransactionManager
import com.j0rsa.bujo.tracker.TransactionManager.currentTransaction
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.user
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.userId
import org.junit.jupiter.api.Test

internal class HabitServiceTest : TransactionalTest {

    @Test
    fun testCreateWithTagCreation() {
        TransactionManager.tx {
            val habitId = HabitService.create(defaultHabitRow(userId))

            val habit = HabitRepository.findById(habitId)
            assertThat(habit).isNotNull()
            assertThat(habit!!.name).isEqualTo("testHabit")
            assertThat(habit.userIdValue()).isEqualTo(userId)
            assertThat(habit.tags.toList()).extracting { it.name }.containsOnly("testTag")
            assertThat(habit.tags.toList().flatMap { it.users.toList() }).extracting { it.idValue() }
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

            val habit = HabitRepository.findById(habitId)!!
            assertThat(habit.tags.toList()).hasSize(3)
            assertThat(habit.tags.toList()).extracting { it.name }.containsOnly("tag1", "tag2", "tag3")
            assertThat(habit.tags.toList().flatMap { it.users.toList() }).extracting { it.idValue() }
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

            val habit = HabitRepository.findById(habitId)!!
            assertThat(habit.tags.toList()).hasSize(1)
            assertThat(habit.tags.toList()).extracting { it.idValue() }.containsOnly(tagWithSameName.idValue())
            assertThat(habit.tags.toList()).extracting { it.name }.containsOnly("testTag")
            assertThat(habit.tags.toList().flatMap { it.users.toList() }).extracting { it.idValue() }
                .containsOnly(userId, anotherUser.idValue())
            currentTransaction().rollback()
        }
    }

    @Test
    fun whenUserHasTagThenNoNewTagCreation() {
        TransactionManager.tx {
            val tagWithSameName = defaultTag(listOf(user))
            val habitId = HabitService.create(defaultHabitRow(userId))

            val habit = HabitRepository.findById(habitId)!!
            assertThat(habit.tags.toList()).hasSize(1)
            assertThat(habit.tags.toList()).extracting { it.idValue() }.containsOnly(tagWithSameName.idValue())
            assertThat(habit.tags.toList()).extracting { it.name }.containsOnly("testTag")
            assertThat(habit.tags.toList().flatMap { it.users.toList() }).extracting { it.idValue() }
                .containsOnly(userId)
            currentTransaction().rollback()
        }
    }

    @Test
    fun findOneWhenHabitExist() {
        TransactionManager.tx {
            val tag = defaultTag(listOf(user))
            val habit = defaultHabit(user, listOf(tag))

            val result = HabitService.findOneBy(habit.idValue(), userId)
            assertThat(result.isRight())
            currentTransaction().rollback()
        }
    }

    @Test
    fun findOneWhenHabitNotExist() {
        TransactionManager.tx {
            val result = HabitService.findOneBy(HabitId.randomValue(), userId)
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

            val result = HabitService.deleteOne(habit.idValue(), userId)
            assertThat(result.isRight())

            val habitAfterDeletion = HabitRepository.findById(habit.idValue())
            assertThat(habitAfterDeletion).isNull()
            currentTransaction().rollback()
        }
    }

    @Test
    fun deleteWhenHabitNotExist() {
        TransactionManager.tx {
            val result = HabitService.deleteOne(HabitId.randomValue(), userId)
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
            val habitToUpdate = defaultHabitRow(userId, "newName", tags, habit.idValue())

            val result = HabitService.update(habitToUpdate)
            assertThat(result.isRight())

            val foundHabit = HabitRepository.findById(habit.idValue())!!
            assertThat(foundHabit.name).isEqualTo("newName")
            assertThat(foundHabit.tags.toList()).hasSize(2)
            assertThat(foundHabit.tags.toList().map { it.name }).containsOnly(*tags.map { it.name }.toTypedArray())
            currentTransaction().rollback()
        }
    }

    @Test
    fun testUpdateHabitWhenHabitNotExist() {
        TransactionManager.tx {
            val notExistingHabit = defaultHabitRow(userId, id = HabitId.randomValue())
            val result = HabitService.update(notExistingHabit)
            assertThat(result.isLeft())
            assertThat(isNotFound(result))
            currentTransaction().rollback()
        }
    }

}