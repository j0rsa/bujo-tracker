package com.j0rsa.bujo.tracker

import assertk.assertThat
import org.joda.time.DateTime
import org.junit.jupiter.api.Test

internal class UtilsKtTest {

    @Test
    fun isCurrentDayWhenYesterday() {
        val isYesterdayCurrentDay = DateTime.now().minusDays(1).isCurrentDay()

        assertThat(isYesterdayCurrentDay)
    }

    @Test
    fun isCurrentWeekWhenIsLessThenWeekAgo() {
        val isWeekAgoCurrentWeek = DateTime.now().minusWeeks(1).isCurrentWeek()

        assertThat(isWeekAgoCurrentWeek)
    }

    @Test
    fun isCurrentDayWhenTodayEvening() {
        val isToDayEveningCurrentDay = DateTime.now().withTime(23, 59, 0, 0).isCurrentDay()

        assertThat(isToDayEveningCurrentDay)
    }

    @Test
    fun isCurrentWeekWhenToday() {
        val isTodayCurrentWeek = DateTime.now().isCurrentWeek()

        assertThat(isTodayCurrentWeek)
    }

    @Test
    fun isCurrentDayWhenTomorrow() {
        val isTomorrowCurrentDay = DateTime.now().plusDays(1).isCurrentDay()

        assertThat(isTomorrowCurrentDay)
    }

    @Test
    fun isCurrentWeekWhenNextWeek() {
        val isNextWeekCurrentWeek = DateTime.now().plusWeeks(1).isCurrentWeek()

        assertThat(isNextWeekCurrentWeek)
    }

    @Test
    fun isNotCurrentDayWhenAfterTomorrow() {
        val isAfterTomorrowCurrentDay = DateTime.now().plusDays(2).isCurrentDay()

        assertThat(!isAfterTomorrowCurrentDay)
    }

    @Test
    fun isNotCurrentWeekWhenAfterNextWeek() {
        val isAfterNextWeekCurrentWeek = DateTime.now().plusWeeks(2).isCurrentWeek()

        assertThat(!isAfterNextWeekCurrentWeek)
    }
}