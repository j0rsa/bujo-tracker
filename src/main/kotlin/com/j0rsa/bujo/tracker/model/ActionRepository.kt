package com.j0rsa.bujo.tracker.model

import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime
import java.math.BigDecimal
import java.sql.ResultSet

object ActionRepository {
    fun findAllWithOneTagWithoutAnother(oneTag: TagId, anotherTag: TagId): List<Action> {
        val query = Actions
            .slice(Actions.columns)
            .select {
                notExists(ActionTags.select {
                    (ActionTags.tagId eq anotherTag.value) and (ActionTags.actionId eq Actions.id)
                }) and exists(ActionTags.select {
                    (ActionTags.tagId eq oneTag.value) and (ActionTags.actionId eq Actions.id)
                })
            }

        return Action.wrapRows(query).toList()
    }

    fun findAll(userId: UserId) = Action.find { Actions.user eq userId.value }.toList()

    fun findById(actionId: ActionId) = Action.findById(actionId.value)

    fun findOneBy(actionId: ActionId, userId: UserId) =
        Action.find { (Actions.id eq actionId.value) and (Actions.user eq userId.value) }.toList()

    fun findStreakForWeek(habitId: HabitId, numberOfRepetitions: Int): List<StreakRecord> =
        """
            WITH
              groups(minDate, maxDate, weekMinusRow) AS (
                SELECT
                  MIN(created) minDate,
                  MAX(created) maxDate,
                  (DATE_PART('day', created - to_timestamp(0))/7)::int - ROW_NUMBER() OVER (ORDER BY MIN(created)) weekMinusRow
                FROM Actions
                WHERE habit is not null and habit = ?
                GROUP BY (DATE_PART('day', created - to_timestamp(0))/7)::int
                HAVING COUNT(*) >= ?
              )
            SELECT
              COUNT(*) streak,
              MIN(minDate) startDate,
              MAX(maxDate) endDate
            FROM groups
            GROUP BY weekMinusRow
            ORDER BY endDate DESC
        """.trimIndent()
            .exec({
                setObject(1, habitId.value)
                setInt(2, numberOfRepetitions)
            }, streakRecord())

    fun findCurrentStreakForWeek(habitId: HabitId, numberOfRepetitions: Int): ArrayList<BigDecimal> =
        """
            SELECT
              COUNT(*) streak,
              MIN(minDate) startDate,
              MAX(maxDate) endDate
            FROM (
              SELECT 
                COUNT(*) counts,
                MIN(date_trunc('day', created)) minDate,
                MAX(date_trunc('day', created)) maxDate,
                (DATE_PART('day', created - to_timestamp(0))/7)::int weeks,
                (DATE_PART('day', created - to_timestamp(0))/7)::int - ROW_NUMBER() OVER (ORDER BY MIN(created)) weekMinusRow
              FROM Actions
              WHERE habit is not null and habit = ?
              GROUP BY (DATE_PART('day', created - to_timestamp(0))/7)::int
              HAVING COUNT(*) >= ?
            ) groupedRows
            GROUP BY weekMinusRow
            HAVING (DATE_PART('day', now() - MAX(maxDate))/7)::int in (0, 1)
                OR (DATE_PART('day', MIN(minDate) - now())/7)::int in (0, 1)
            ORDER BY endDate DESC
        """.trimIndent()
            .exec({
                setObject(1, habitId.value)
                setInt(2, numberOfRepetitions)
            }, {it.getBigDecimal("streak")})

    fun findStreakForDay(habitId: HabitId, numberOfRepetitions: Int): List<StreakRecord> =
        ("""
            WITH
              groups(minDate, maxDate, dateMinusRow) AS (
                SELECT 
                    MIN(created) minDate,
                    MAX(created) maxDate,
                    date_trunc('day', created) - INTERVAL '1' DAY * ROW_NUMBER() OVER (ORDER BY MIN(created)) dateMinusRow
                FROM Actions
                WHERE habit is not null and habit = ?
                GROUP BY date_trunc('day', created)
                HAVING COUNT(*) >= ?
              )
            SELECT
              COUNT(*) AS streak,
              MIN(minDate) AS startDate,
              MAX(maxDate) AS endDate
            FROM groups
            GROUP BY dateMinusRow
            ORDER BY endDate DESC
            """
            .trimIndent())
            .exec({
                setObject(1, habitId.value)
                setInt(2, numberOfRepetitions)
            }, streakRecord())

    fun findCurrentStreakForDay(habitId: HabitId, numberOfRepetitions: Int): ArrayList<BigDecimal> =
        ("""
            SELECT
              COUNT(*) streak,
              MIN(minDate) startDate,
              MAX(maxDate) endDate
            FROM (
              SELECT 
                COUNT(*) amount,
                MIN(date_trunc('day', created)) minDate,
                MAX(date_trunc('day', created)) maxDate,
                date_trunc('day', created) - INTERVAL '1' DAY * ROW_NUMBER() OVER (ORDER BY MIN(created)) dateMinusRow
              FROM Actions
              WHERE habit is not null and habit = ?
              GROUP BY date_trunc('day', created)
              HAVING COUNT(*) >= ?
            ) groupedDays
            GROUP BY dateMinusRow
            HAVING date_part('day', date_trunc('day', now()) - MAX(maxDate)) in (0, 1) 
                OR date_part('day', date_trunc('day', MIN(minDate) - now())) in (0, 1) 
            ORDER BY endDate DESC
            LIMIT 1
            """
            .trimIndent())
            .exec({
                setObject(1, habitId.value)
                setInt(2, numberOfRepetitions)
            }, {it.getBigDecimal("streak")})

    private fun streakRecord() = { res: ResultSet ->
        StreakRecord(
            streak = res.getBigDecimal("streak"),
            startDate = DateTime(res.getTimestamp("startDate")),
            endDate = DateTime(res.getTimestamp("endDate"))
        )
    }
}