package com.j0rsa.bujo.tracker.model

import arrow.core.Either.Right
import assertk.assertThat
import assertk.assertions.*
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.user
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.userId
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ActionServiceTest : TransactionalTest {
    @Test
    fun testCreateWithHabitWhenHabitExistThenActionWithHabitAndTagsFromView() {
        tempTx {
            val tag = defaultTag(listOf(user))
            val habit = defaultHabit(user, listOf(tag))

            val newAction = defaultActionRow(userId, habit.idValue())
            val result = ActionService.create(newAction)
            assertThat(result.isRight())

            val foundAction = ActionRepository.findById((result as Right<ActionId>).b)
            assertThat(foundAction).isNotNull()
            assertThat(foundAction!!.habit).isEqualTo(habit)
            assertThat(foundAction.tags.toList().map { it.name }).containsOnly("testTag")
        }
    }

    @Test
    fun testCreateWithHabitWhenHabitNotExistThenNotFound() {
        tempTx {
            val newAction = defaultActionRow(userId, HabitId.randomValue())
            val result = ActionService.create(newAction)
            assertThat(result.isLeft())
            assertThat(isNotFound(result))
        }
    }

    @Test
    fun testCreateWithTagsWhenTagExistWithAnotherUser() {
        tempTx {
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
        }
    }

    @Test
    fun testCreateWithTagsWhenTagNotExist() {
        tempTx {
            val newAction = defaultBaseActionRow(userId)
            val actionId = ActionService.create(newAction)

            val foundAction = ActionRepository.findById(actionId)!!
            assertThat(foundAction.tags.toList()).hasSize(1)
            assertThat(foundAction.tags.first().users.toList()).extracting { it.idValue() }.containsOnly(userId)
        }
    }

    @Test
    fun testCreateWithTagsUserHasTagThenNoNewTagCreation() {
        tempTx {
            val tagWithSameName = defaultTag(listOf(user))
            val actionId = ActionService.create(defaultBaseActionRow(userId))

            val action = ActionRepository.findById(actionId)!!
            assertThat(action.tags.toList()).hasSize(1)
            assertThat(action.tags.toList()).extracting { it.idValue() }.containsOnly(tagWithSameName.idValue())
            assertThat(action.tags.toList()).extracting { it.name }.containsOnly("testTag")
            assertThat(action.tags.toList().flatMap { it.users.toList() }).extracting { it.idValue() }
                .containsOnly(userId)
        }
    }

    @Test
    fun deleteWhenActionExist() {
        tempTx {
            val tag = defaultTag(listOf(user))
            val action = defaultAction(user, listOf(tag))

            val result = ActionService.deleteOne(action.idValue(), userId)
            assertThat(result.isRight())

            val recordAfterDeletion = ActionRepository.findById(action.idValue())
            assertThat(recordAfterDeletion).isNull()
        }
    }

    @Test
    fun deleteWhenActionNotExist() {
        tempTx {
            val result = ActionService.deleteOne(ActionId.randomValue(), userId)
            assertThat(result.isLeft())
            assertThat(isNotFound(result))
        }
    }

    @Test
    fun testUpdateActionWhenActionExist() {
        tempTx {
            val tag = defaultTag(listOf(user))
            val habit = defaultHabit(user, listOf(tag))
            val action = defaultAction(user, listOf(tag), habit = habit)

            val tags = listOf(defaultTagRow(), defaultTagRow("anotherTag"))
            val actionToUpdate = defaultBaseActionRow(userId, "newName", tags, action.idValue())

            val result = ActionService.update(actionToUpdate)
            assertThat(result.isRight())

            val foundHabit = ActionRepository.findById(action.idValue())!!
            assertThat(foundHabit.description).isEqualTo("newName")
            assertThat(foundHabit.tags.toList()).hasSize(2)
            assertThat(foundHabit.tags.toList().map { it.name }).containsOnly(*tags.map { it.name }.toTypedArray())
        }
    }

    @Test
    fun testUpdateActionWhenHabitNotExist() {
        tempTx {
            val notExistingAction = defaultBaseActionRow(userId, id = ActionId.randomValue())
            val result = ActionService.update(notExistingAction)
            assertThat(result.isLeft())
            assertThat(isNotFound(result))
        }
    }


    @Test
    fun findOneWhenActionExist() {
        tempTx {
            val tag = defaultTag(listOf(user))
            val action = defaultAction(user, listOf(tag))

            val result = ActionService.findOneBy(action.idValue(), userId)
            assertThat(result.isRight())
        }
    }

    @Test
    fun findOneWhenActionNotExist() {
        tempTx {
            val result = ActionService.findOneBy(ActionId.randomValue(), userId)
            assertThat(result.isLeft())
            assertThat(isNotFound(result))
        }
    }

    @Test
    fun findStreakForDayWhenHasCurrentStreak() {
        tempTx {
            val habit = defaultHabit(user)
            val yesterday = DateTime.now().minusDays(1)
            insertDefaultAction(user, habit = habit, created = yesterday)
            insertDefaultAction(user, habit = habit, created = yesterday.minusDays(1))
            insertDefaultAction(user, habit = habit, created = DateTime(2020, 1, 15, 8, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2020, 1, 14, 9, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2020, 1, 13, 23, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 23, 11, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 22, 20, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 21, 10, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 20, 12, 0))

            val result = ActionService.findStreakForDay(habit.idValue(), 1)
            assertThat(result.currentStreak).isEqualTo(BigDecimal(2))
            assertThat(result.maxStreak).isEqualTo(BigDecimal(4))
        }
    }

    @Test
    fun findStreakForDayWhenNotCurrentStreak() {
        tempTx {
            val habit = defaultHabit(user)
            val notYesterday = DateTime.now().minusDays(2)
            insertDefaultAction(user, habit = habit, created = notYesterday)
            insertDefaultAction(user, habit = habit, created = notYesterday.minusDays(1))
            insertDefaultAction(user, habit = habit, created = DateTime(2020, 1, 15, 8, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2020, 1, 14, 9, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2020, 1, 13, 23, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 23, 11, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 22, 20, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 21, 10, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 20, 12, 0))

            val result = ActionService.findStreakForDay(habit.idValue(), 1)
            assertThat(result.currentStreak).isEqualTo(BigDecimal.ZERO)
            assertThat(result.maxStreak).isEqualTo(BigDecimal(4))
        }
    }

    @Test
    fun findStreakForDayWhenNoData() {
        tempTx {
            val habit = defaultHabit(user)

            val result = ActionService.findStreakForDay(habit.idValue(), 1)
            assertThat(result.currentStreak).isEqualTo(BigDecimal.ZERO)
            assertThat(result.maxStreak).isEqualTo(BigDecimal.ZERO)
        }
    }

    @Test
    fun findStreakForWeekWhenHasCurrentStreak() {
        tempTx {
            val habit = defaultHabit(user)
            val previousWeek = DateTime.now().minusWeeks(1)
            insertDefaultAction(user, habit = habit, created = previousWeek)
            insertDefaultAction(user, habit = habit, created = previousWeek.minusWeeks(1))

            //1 part of streak 2
            insertDefaultAction(user, habit = habit, created = DateTime(2020, 1, 2, 8, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 12, 30, 9, 0))
            //2 part of streak 2
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 12, 29, 23, 0))

            insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 23, 13, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 12, 11, 0))
            //same week 3 part of streak 3
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 6, 12, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 6, 10, 0))


            val result = ActionService.findStreakForWeek(habit.idValue(), 1)
            assertThat(result.currentStreak).isEqualTo(BigDecimal(2))
            assertThat(result.maxStreak).isEqualTo(BigDecimal(3))
        }
    }

    @Test
    fun findStreakForWeekWhenNoCurrentStreak() {
        tempTx {
            val habit = defaultHabit(user)
            val notPreviousWeek = DateTime.now().minusWeeks(2)
            insertDefaultAction(user, habit = habit, created = notPreviousWeek)
            insertDefaultAction(user, habit = habit, created = notPreviousWeek.minusWeeks(1))

            //1 part of streak 2
            insertDefaultAction(user, habit = habit, created = DateTime(2020, 1, 2, 8, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 12, 30, 9, 0))
            //2 part of streak 2
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 12, 29, 23, 0))

            insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 23, 13, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 12, 11, 0))
            //same week 3 part of streak 3
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 6, 12, 0))
            insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 6, 10, 0))

            val result = ActionService.findStreakForWeek(habit.idValue(), 1)
            assertThat(result.currentStreak).isEqualTo(BigDecimal.ZERO)
            assertThat(result.maxStreak).isEqualTo(BigDecimal(3))
        }
    }

    @Test
    fun findStreakForWeekWhenNoData() {
        tempTx {
            val habit = defaultHabit(user)

            val result = ActionService.findStreakForWeek(habit.idValue(), 1)
            assertThat(result.currentStreak).isEqualTo(BigDecimal.ZERO)
            assertThat(result.maxStreak).isEqualTo(BigDecimal.ZERO)
        }
    }

    @Test
    fun testCreateWithValues() {
        tempTx {
            val defaultValue = defaultValue()
            val actionRow = defaultBaseActionRow(userId, values = listOf(defaultValue))

            val actionId = ActionService.create(actionRow)

            val foundAction = ActionRepository.findById(actionId)
            assertThat(foundAction).isNotNull()
            assertThat(foundAction!!.toRow().values).containsOnly(defaultValue)
        }
    }

    @Test
    fun testUpdateWithValues() {
        tempTx {
            val action = defaultAction(user)
            val valueId = defaultValue(action, ValueType.EndDate).idValue()

            val defaultValue = defaultValue()
            val actionRow = defaultBaseActionRow(userId, id = action.idValue(), values = listOf(defaultValue))
            ActionService.update(actionRow)

            val foundAction = ActionRepository.findById(action.idValue())
            assertThat(foundAction).isNotNull()
            assertThat(foundAction!!.toRow().values).containsOnly(defaultValue)
            val foundValues = ValueService.findAll()
            assertThat(foundValues).hasSize(1)
        }
    }
}