package com.j0rsa.bujo.tracker.model

import arrow.core.Either
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.j0rsa.bujo.tracker.model.TransactionalTest.Companion.telegramUser
import org.junit.jupiter.api.Test

internal class UserServiceTest : TransactionalTest {

	@Test
	fun findOneBy() {
		tempTx {
			val user = UserService.findOneBy(1L)
			assertThat(user).isNotNull()
			assertThat(user!!.name).isEqualTo(telegramUser.name)
		}
	}

	@Test
	fun createUser() {
		tempTx {
			val newUser = defaultUserInfo(2L, "newUser")
			val createdUser = UserService.createUser(newUser)

			val result = UserRepository.findOne(createdUser.idValue())

			assertThat(result).isNotNull()
			assertThat(result!!.telegramId).isEqualTo(2L)
			assertThat(result.firstName).isEqualTo("newUser")
		}
	}

	@Test
	fun updateUser() {
		tempTx {
			val user = defaultUserInfo(firstName = "updatedDefaultUserInfo")
			val updatedUser = UserService.updateUser(user)(telegramUser)

			val result = UserRepository.findOne(updatedUser.idValue())

			assertThat(result).isNotNull()
			assertThat(result!!.telegramId).isEqualTo(1L)
			assertThat(result.firstName).isEqualTo("updatedDefaultUserInfo")
		}
	}

	@Test
	fun testFindOneWhenNotExist() {
		tempTx {
			val result = UserService.findOne(2L)

			assertThat(result.isLeft())
			assertThat(isNotFound(result))
		}
	}

	@Test
	fun testFindOneWhenExist() {
		tempTx {
			val result = UserService.findOne(1L)

			assertThat(result.isRight())
			assertThat((result as Either.Right).b.id).isEqualTo(telegramUser.idValue())
		}
	}
}