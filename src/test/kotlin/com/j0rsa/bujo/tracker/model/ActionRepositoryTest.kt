package com.j0rsa.bujo.tracker.model

import assertk.assertThat
import assertk.assertions.*
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.user
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class ActionRepositoryTest : TransactionalTest {
	@Test
	fun foundNothingWhenHasBothTags() {
		tempTx {
			val oneTag = defaultTag(listOf(user), "tag1")
			val anotherTag = defaultTag(listOf(user), "tag2")
			defaultAction(user, listOf(oneTag, anotherTag))

			val result = ActionRepository.findAllWithOneTagWithoutAnother(oneTag.idValue(), anotherTag.idValue())
			assertThat(result).isEmpty()
		}
	}

	@Test
	fun foundOneActionWhenHasOnlyOneTag() {
		tempTx {
			val oneTag = defaultTag(listOf(user), "tag1")
			val anotherTag = defaultTag(listOf(user), "tag2")
			defaultAction(user, listOf(oneTag, anotherTag))
			defaultAction(user, listOf(anotherTag))
			val actionWithOnlyOneTag = defaultAction(user, listOf(oneTag))

			val result = ActionRepository.findAllWithOneTagWithoutAnother(oneTag.idValue(), anotherTag.idValue())
			assertThat(result).hasSize(1)
			assertThat(result.first().idValue()).isEqualTo(actionWithOnlyOneTag.idValue())
		}
	}

	@Test
	fun findStreakWhen1StreakFor1() {
		tempTx {
			val habit = defaultHabit(user)
			insertDefaultAction(user, habit = habit)

			val record = ActionRepository.findStreakForWeek(habit.idValue(), 1)

			println(record.joinToString("\n"))
			assertThat(record).hasSize(1)
			assertThat(record.first().streak).isEqualTo(BigDecimal.ONE)
		}
	}

	@Test
	fun findStreakWhen1WeekHas2ActionsThen1Streak() {
		tempTx {
			val habit = defaultHabit(user)
			insertDefaultAction(user, habit = habit)
			insertDefaultAction(user, habit = habit)

			val record = ActionRepository.findStreakForWeek(habit.idValue(), 1)

			println(record.joinToString("\n"))
			assertThat(record).hasSize(1)
			assertThat(record.first().streak).isEqualTo(BigDecimal.ONE)
		}
	}

	@Test
	fun findStreakWhen2StreaksFor2() {
		tempTx {
			val habit = defaultHabit(user)
			val endDateOfLastStreak = DateTime(2020, 1, 30, 9, 0)
			insertDefaultAction(user, habit = habit, created = endDateOfLastStreak)
			insertDefaultAction(user, habit = habit, created = DateTime(2020, 1, 26, 9, 0))
			insertDefaultAction(user, habit = habit, created = DateTime(2020, 1, 12, 9, 0))
			insertDefaultAction(user, habit = habit, created = DateTime(2019, 12, 30, 9, 0))

			val record = ActionRepository.findStreakForWeek(habit.idValue(), 1)

			println(record.joinToString("\n"))
			assertThat(record).hasSize(2)
			assertThat(record.map { it.streak }).containsOnly(BigDecimal(2))
			assertThat(record.first().endDate).isEqualTo(endDateOfLastStreak)
		}
	}

	@Test
	fun findStreakWhen3Streaks() {
		tempTx {
			val habit = defaultHabit(user)
			val endDateOfLastStreak = DateTime(2020, 1, 30, 21, 0)
			insertDefaultAction(user, habit = habit, created = endDateOfLastStreak)
			insertDefaultAction(user, habit = habit, created = DateTime(2020, 1, 29, 10, 0))
			//same week 1 part of streak 2
			insertDefaultAction(user, habit = habit, created = DateTime(2020, 1, 2, 8, 0))
			insertDefaultAction(user, habit = habit, created = DateTime(2019, 12, 30, 9, 0))
			//2 part of streak 2
			insertDefaultAction(user, habit = habit, created = DateTime(2019, 12, 29, 23, 0))

			insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 23, 13, 0))
			insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 12, 11, 0))
			//same week 3 part of streak 3
			insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 6, 12, 0))
			insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 6, 10, 0))

			val record = ActionRepository.findStreakForWeek(habit.idValue(), 1)

			println(record.joinToString("\n"))
			assertThat(record).hasSize(3)
			assertThat(record.map { it.streak }).containsExactly(BigDecimal(1), BigDecimal(2), BigDecimal(3))
			assertThat(record.map { it.startDate to it.endDate }).containsExactly(
				DateTime(2020, 1, 29, 10, 0) to endDateOfLastStreak,
				DateTime(2019, 12, 29, 23, 0) to DateTime(2020, 1, 2, 8, 0),
				DateTime(2019, 11, 6, 10, 0) to DateTime(2019, 11, 23, 13, 0)
			)
			assertThat(record.first().endDate).isEqualTo(endDateOfLastStreak)
		}
	}

	@Test
	fun findStreakWhenEndOfTheYear() {
		tempTx {
			val habit = defaultHabit(user)
			val endDateOfLastStreak = DateTime(2020, 1, 8, 21, 0)
			val startDateOfStreak = DateTime(2019, 12, 29, 9, 0)
			insertDefaultAction(user, habit = habit, created = endDateOfLastStreak)
			insertDefaultAction(user, habit = habit, created = DateTime(2019, 12, 31, 10, 0))
			insertDefaultAction(user, habit = habit, created = startDateOfStreak)

			val record = ActionRepository.findStreakForWeek(habit.idValue(), 1)

			println(record.joinToString("\n"))
			assertThat(record).hasSize(1)
			assertThat(record.map { it.streak }).containsExactly(BigDecimal(3))
			assertThat(record.map { it.startDate to it.endDate }).containsExactly(
				startDateOfStreak to endDateOfLastStreak
			)
		}
	}

	@Test
	fun findStreakForDayWhen3Streaks() {
		tempTx {
			val habit = defaultHabit(user)
			println(habit.idValue())
			val endDateOfLastStreak = DateTime(2020, 1, 30, 21, 0)
			insertDefaultAction(user, habit = habit, created = endDateOfLastStreak)
			insertDefaultAction(user, habit = habit, created = DateTime(2020, 1, 29, 14, 0))
			insertDefaultAction(user, habit = habit, created = DateTime(2020, 1, 15, 8, 0))
			insertDefaultAction(user, habit = habit, created = DateTime(2020, 1, 14, 9, 0))
			insertDefaultAction(user, habit = habit, created = DateTime(2020, 1, 13, 23, 0))
			insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 23, 11, 0))
			insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 22, 20, 0))
			insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 21, 10, 0))
			insertDefaultAction(user, habit = habit, created = DateTime(2019, 11, 20, 12, 0))

			val record = ActionRepository.findStreakForDay(habit.idValue(), 1)

			println(record.joinToString("\n"))
			assertThat(record).hasSize(3)
			assertThat(record.map { it.streak }).containsExactly(BigDecimal(2), BigDecimal(3), BigDecimal(4))
			assertThat(record.map { it.startDate to it.endDate }).containsExactly(
				DateTime(2020, 1, 29, 14, 0) to endDateOfLastStreak,
				DateTime(2020, 1, 13, 23, 0) to DateTime(2020, 1, 15, 8, 0),
				DateTime(2019, 11, 20, 12, 0) to DateTime(2019, 11, 23, 11, 0)
			)
			assertThat(record.first().endDate).isEqualTo(endDateOfLastStreak)
		}
	}

	@Test
	fun findStreakForDayWhenHasCurrentStreakYesterday() {
		tempTx {
			val habit = defaultHabit(user)
			val yesterday = DateTime.now().minusDays(1)
			insertDefaultAction(user, habit = habit, created = yesterday)
			insertDefaultAction(user, habit = habit, created = yesterday.minusDays(1))

			val result = ActionRepository.findCurrentStreakForDay(habit.idValue(), 1)
			assertThat(result).isNotNull()
			assertThat(result).isEqualTo(BigDecimal(2))
		}
	}

	@Test
	fun findStreakForDayWhenHasCurrentStreakTomorrow() {
		tempTx {
			val habit = defaultHabit(user)
			val tomorrow = DateTime.now().plusDays(2)
			insertDefaultAction(user, habit = habit, created = tomorrow)
			insertDefaultAction(user, habit = habit, created = tomorrow.plusDays(1))

			val result = ActionRepository.findCurrentStreakForDay(habit.idValue(), 1)
			assertThat(result).isNotNull()
			assertThat(result).isEqualTo(BigDecimal(2))
		}
	}

	@Test
	fun findStreakForDayWhenNoCurrentStreak() {
		tempTx {
			val habit = defaultHabit(user)
			val dayBeforeYesterday = DateTime.now().minusDays(2)
			insertDefaultAction(user, habit = habit, created = dayBeforeYesterday)
			insertDefaultAction(user, habit = habit, created = dayBeforeYesterday.minusDays(1))

			val result = ActionRepository.findCurrentStreakForDay(habit.idValue(), 1)
			assertThat(result).isNull()
		}
	}

	@Test
	fun findStreakForWeekWhenHasCurrentStreakPreviousWeek() {
		tempTx {
			val habit = defaultHabit(user)
			val previousWeek = DateTime.now().minusWeeks(1)
			insertDefaultAction(user, habit = habit, created = previousWeek)
			insertDefaultAction(user, habit = habit, created = previousWeek.minusWeeks(1))

			val result = ActionRepository.findCurrentStreakForWeek(habit.idValue(), 1)
			assertThat(result).isNotNull()
			assertThat(result).isEqualTo(BigDecimal(2))
		}
	}

	@Test
	fun findStreakForWeekWhenHasCurrentStreakNextWeek() {
		tempTx {
			val habit = defaultHabit(user)
			val nextWeek = DateTime.now().plusWeeks(1)
			insertDefaultAction(user, habit = habit, created = nextWeek)
			insertDefaultAction(user, habit = habit, created = nextWeek.plusWeeks(1))

			val result = ActionRepository.findCurrentStreakForWeek(habit.idValue(), 1)
			assertThat(result).isNotNull()
			assertThat(result).isEqualTo(BigDecimal(2))
		}
	}

	@Test
	fun findStreakForWeekWhenNoCurrentStreak() {
		tempTx {
			val habit = defaultHabit(user)
			val notPreviousWeek = DateTime.now().minusWeeks(2)
			insertDefaultAction(user, habit = habit, created = notPreviousWeek)
			insertDefaultAction(user, habit = habit, created = notPreviousWeek.minusWeeks(1))

			val result = ActionRepository.findCurrentStreakForWeek(habit.idValue(), 1)
			assertThat(result).isNull()
		}
	}
}