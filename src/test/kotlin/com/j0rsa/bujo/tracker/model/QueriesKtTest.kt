package com.j0rsa.bujo.tracker.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.j0rsa.bujo.tracker.model.ResultColumn.BigDecimalColumn
import com.j0rsa.bujo.tracker.model.ResultColumn.DateTimeColumn
import org.joda.time.DateTime
import org.junit.jupiter.api.Test

import java.math.BigDecimal
import java.util.*

internal class QueriesKtTest : TransactionalTest {

	@Test
	fun testExecWithFromRow() {
		tempTx {
			val expected = defaultTestData()
			val result = """
            select
              to_timestamp(0) date,
              false b,
              'testData' s,
              1.7976931348623157E308 d,
              3.4028235E38 f,
              2147483647 i,
              32767 sh,
              9223372036854775807 l,
              10 bigDecimal
              """.trimIndent()
				.exec()
				.toEntities<TestData>()
				.first()
			assertThat(result).isEqualTo(expected)
		}
	}

	@Test
	fun testExecMapWithPair() {
		tempTx {
			val result = """
            select
              to_timestamp(0) date,
              false b,
              'testData' s,
              1.7976931348623157E308 d,
              3.4028235E38 f,
              2147483647 i,
              32767 sh,
              9223372036854775807 l,
              10 bigDecimal
              """.trimIndent()
				.exec()
				.map {
					BigDecimalColumn("bigDecimal") from it to (DateTimeColumn("date") from it)
				}
				.first()
			assertThat(result).isNotNull()
			assertThat(result).isEqualTo(BigDecimal.TEN to DateTime.parse("1970-01-01T00:00:00"))
		}
	}

	@Test
	fun testExecMapWithGet() {
		tempTx {
			val result = """
            select
              to_timestamp(0) date,
              false b,
              'testData' s,
              1.7976931348623157E308 d,
              3.4028235E38 f,
              2147483647 i,
              32767 sh,
              9223372036854775807 l,
              10 bigDecimal
              """.trimIndent()
				.exec()
				.get(BigDecimalColumn("bigDecimal"))
				.first()
			assertThat(result).isNotNull()
			assertThat(result).isEqualTo(BigDecimal.TEN)
		}
	}

	@Test
	fun testExecMapWhenComplicated() {
		tempTx {
			val expectedTestData = defaultTestData()
			val result = """
            select
              to_timestamp(0) date,
              false b,
              'testData' s,
              1.7976931348623157E308 d,
              3.4028235E38 f,
              2147483647 i,
              32767 sh,
              9223372036854775807 l,
              10 bigDecimal,
              uuid_generate_v4() id
              """.trimIndent()
				.exec()
				.map {
					TestData2(
						testData = it.toEntity(),
						id = ResultColumn.UUIDColumn("id") from it
					)
				}
				.first()
			assertThat(result.testData).isEqualTo(expectedTestData)
			assertThat(result.id).isNotNull()
		}
	}

	private fun defaultTestData(): TestData =
		TestData(
			date = DateTime.parse("1970-01-01T00:00:00"),
			bigDecimal = BigDecimal.TEN,
			b = false,
			d = 1.7976931348623157E308,
			f = 3.4028235E38f,
			i = 2147483647,
			sh = 32767,
			l = 9223372036854775807,
			s = "testData"
		)

	@Test
	fun testExecWithFromValue() {
		tempTx {
			val result = """
            select
              uuid_generate_v4() id
              """.trimIndent()
				.exec()
				.getValue<UUID>("id")
			println(result)
			assertThat(result).isNotNull()
		}
	}

	data class TestData(
		val date: DateTime,
		val bigDecimal: BigDecimal,
		val b: Boolean,
		val d: Double,
		val f: Float,
		val i: Int,
		val sh: Short,
		val l: Long,
		val s: String
	)

	data class TestData2(
		val testData: TestData,
		val id: UUID
	)
}