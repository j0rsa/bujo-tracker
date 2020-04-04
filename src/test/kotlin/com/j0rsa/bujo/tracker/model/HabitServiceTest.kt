package com.j0rsa.bujo.tracker.model

import assertk.assertThat
import assertk.assertions.*
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.user
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.userId
import com.j0rsa.bujo.tracker.repository.HabitRepository
import com.j0rsa.bujo.tracker.service.HabitService
import com.j0rsa.bujo.tracker.service.ValueTemplateService
import org.junit.jupiter.api.Test

internal class HabitServiceTest : TransactionalTest {

	@Test
	fun testCreateWithTagCreation() {
		tempTx {
			val habitId = HabitService.create(defaultHabitRow(userId))

			val habit = HabitRepository.findById(habitId)
			assertThat(habit).isNotNull()
			assertThat(habit!!.name).isEqualTo("testHabit")
			assertThat(habit.userIdValue()).isEqualTo(userId)
			assertThat(habit.tags.toList()).extracting { it.name }.containsOnly("testTag")
			assertThat(habit.tags.toList().flatMap { it.users.toList() }).extracting { it.idValue() }
				.containsOnly(userId)
		}
	}

	@Test
	fun testCreateWithValueTemplateCreation() {
		tempTx {
			val expected = defaultHabitRow(userId, values = listOf(defaultValueTemplate()))
			val habitId = HabitService.create(expected)

			val habit = HabitRepository.findById(habitId)
			assertThat(habit).isNotNull()
			assertThat(habit!!.toRow().values).isEqualTo(expected.values)
		}
	}

	@Test
	fun testUpdateWithValueTemplate() {
		tempTx {
			val createdHabit = defaultHabit(user)
			defaultValueTemplate(createdHabit)

			val template = defaultValueTemplate(ValueType.EndDate, name = "newName")
			val habitRowToUpdate = defaultHabitRow(userId, id = createdHabit.idValue(), values = listOf(template))
			HabitService.update(habitRowToUpdate)

			val habit = HabitRepository.findById(createdHabit.idValue())
			assertThat(habit).isNotNull()
			assertThat(habit!!.toRow().values).containsOnly(template)

			val allTemplates = ValueTemplateService.findAll().map { it.toRow() }
			assertThat(allTemplates).hasSize(1)
			assertThat(allTemplates).containsOnly(template)
		}
	}

	@Test
	fun testCreateWithSeveralTagCreation() {
		tempTx {
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
		}
	}

	@Test
	fun whenAnotherUserHasTagWithSameNameThenNoNewTagCreation() {
		tempTx {
			val anotherUser = defaultUser("anotherUser")
			val tagWithSameName = defaultTag(listOf(anotherUser))
			val habitId = HabitService.create(defaultHabitRow(userId))

			val habit = HabitRepository.findById(habitId)!!
			assertThat(habit.tags.toList()).hasSize(1)
			assertThat(habit.tags.toList()).extracting { it.idValue() }.containsOnly(tagWithSameName.idValue())
			assertThat(habit.tags.toList()).extracting { it.name }.containsOnly("testTag")
			assertThat(habit.tags.toList().flatMap { it.users.toList() }).extracting { it.idValue() }
				.containsOnly(userId, anotherUser.idValue())
		}
	}

	@Test
	fun whenUserHasTagThenNoNewTagCreation() {
		tempTx {
			val tagWithSameName = defaultTag(listOf(user))
			val habitId = HabitService.create(defaultHabitRow(userId))

			val habit = HabitRepository.findById(habitId)!!
			assertThat(habit.tags.toList()).hasSize(1)
			assertThat(habit.tags.toList()).extracting { it.idValue() }.containsOnly(tagWithSameName.idValue())
			assertThat(habit.tags.toList()).extracting { it.name }.containsOnly("testTag")
			assertThat(habit.tags.toList().flatMap { it.users.toList() }).extracting { it.idValue() }
				.containsOnly(userId)
		}
	}

	@Test
	fun findOneWhenHabitExist() {
		tempTx {
			val tag = defaultTag(listOf(user))
			val habit = defaultHabit(user, listOf(tag))

			val result = HabitService.findOneBy(habit.idValue(), userId)
			assertThat(result.isRight())
		}
	}

	@Test
	fun findOneWhenHabitNotExist() {
		tempTx {
			val result = HabitService.findOneBy(HabitId.randomValue(), userId)
			assertThat(result.isLeft())
			assertThat(isNotFound(result))
		}
	}

	@Test
	fun deleteWhenHabitExist() {
		tempTx {
			val tag = defaultTag(listOf(user))
			val habit = defaultHabit(user, listOf(tag))

			val result = HabitService.deleteOne(habit.idValue(), userId)
			assertThat(result.isRight())

			val habitAfterDeletion = HabitRepository.findById(habit.idValue())
			assertThat(habitAfterDeletion).isNull()
		}
	}

	@Test
	fun deleteWhenHabitNotExist() {
		tempTx {
			val result = HabitService.deleteOne(HabitId.randomValue(), userId)
			assertThat(result.isLeft())
			assertThat(isNotFound(result))
		}
	}

	@Test
	fun testUpdateHabitWhenHabitExist() {
		tempTx {
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
		}
	}

	@Test
	fun testUpdateHabitWhenHabitNotExist() {
		tempTx {
			val notExistingHabit = defaultHabitRow(userId, id = HabitId.randomValue())
			val result = HabitService.update(notExistingHabit)
			assertThat(result.isLeft())
			assertThat(isNotFound(result))
		}
	}

}