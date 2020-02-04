package com.j0rsa.bujo.tracker.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import org.joda.time.DateTime
import org.junit.jupiter.api.Test

import java.math.BigDecimal
import java.util.*

internal class QueriesKtTest : TransactionalTest {

    @Test
    fun testExecWithFromRow() {
        tempTx {
            val expected = TestData(
                date = DateTime.parse("1970-01-01T01:00:00"),
                bigDecimal = BigDecimal.TEN,
                b = false,
                d = Double.MAX_VALUE,
                f = Float.MAX_VALUE,
                i = Int.MAX_VALUE,
                sh = Short.MAX_VALUE,
                l = Long.MAX_VALUE,
                s = "testData"
            )

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
}