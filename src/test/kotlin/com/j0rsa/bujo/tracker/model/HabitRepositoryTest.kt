package com.j0rsa.bujo.tracker.model

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.user
import org.junit.jupiter.api.Test

internal class HabitRepositoryTest : TransactionalTest {

	@Test
	fun foundNothingWhenHasBothTags() {
		tempTx {
			val oneTag = defaultTag(listOf(user), "tag1")
			val anotherTag = defaultTag(listOf(user), "tag2")
			defaultHabit(user, listOf(oneTag, anotherTag))

			val result = HabitRepository.findAllWithOneTagWithoutAnother(oneTag.idValue(), anotherTag.idValue())
			assertThat(result).isEmpty()
		}
	}

	@Test
	fun foundOneHabitWhenHasOnlyOneTag() {
		tempTx {
			val oneTag = defaultTag(listOf(user), "tag1")
			val anotherTag = defaultTag(listOf(user), "tag2")
			defaultHabit(user, listOf(oneTag, anotherTag))
			defaultHabit(user, listOf(anotherTag))
			val habitWithOnlyOneTag = defaultHabit(user, listOf(oneTag))

			val result = HabitRepository.findAllWithOneTagWithoutAnother(oneTag.idValue(), anotherTag.idValue())
			assertThat(result).hasSize(1)
			assertThat(result.first().idValue()).isEqualTo(habitWithOnlyOneTag.idValue())
		}
	}
}