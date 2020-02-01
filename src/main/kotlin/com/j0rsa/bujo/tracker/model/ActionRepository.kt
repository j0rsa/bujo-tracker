package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.model.DatePartEnum.Days
import com.j0rsa.bujo.tracker.model.DatePartEnum.Week
import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime

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

    fun findStreakForWeek(habitId: HabitId): List<StreakRecord> {
        val firstDate = Actions.created.min().alias("firstDate")
        val firstActionDateQuery = (Actions innerJoin Habits)
            .slice(firstDate, Habits.id)
            .select { Habits.id eq habitId.value }
            .groupBy(Habits.id)
            .alias("firstActionDateQuery")
        val weeksBetweenFirstAction = weeksBetweenCreatedAnd(firstActionDateQuery[firstDate]).alias("weekInGroup")
        val minDate = Actions.created.min().alias("minDate")
        val weekMinusRowNumber =
            weeksBetweenCreatedMinusRowNumber(firstActionDateQuery[firstDate]).alias("weekMinusRow")
        val maxDate = Actions.created.max().alias("maxDate")

        val idCounts = Actions.id.count().alias("counts")
        val groupedAlias = (Actions innerJoin Habits)
            .innerJoin(firstActionDateQuery, { Habits.id }, { firstActionDateQuery[Habits.id] })
            .slice(idCounts, weeksBetweenFirstAction, minDate, maxDate, weekMinusRowNumber)
            .select { Habits.id eq habitId.value }
            .groupBy(weeksBetweenFirstAction)
            .alias("groupedAlias")

        val streak = Sum(groupedAlias[idCounts]).alias("streak")
        val startDate = MinDate(groupedAlias[minDate]).alias("startDate")
        val endDate = MaxDate(groupedAlias[maxDate]).alias("endDate")
        val weekMinusRow = groupedAlias[weekMinusRowNumber].alias("weekMinusRow")

        fun ResultRow.toStreakRecord() = StreakRecord(
            startDate = this[startDate]?.let { DateTime(it.time) },
            endDate = this[endDate]?.let { DateTime(it.time) },
            streak = this[streak]
        )

        val query = groupedAlias
            .slice(startDate, endDate, streak, weekMinusRow)
            .selectAll()
            .groupBy(weekMinusRow)
            .orderBy(endDate to SortOrder.DESC)
        return query
            .map { it.toStreakRecord() }
            .toList()
    }

    fun findStreakForDay(habitId: HabitId): List<StreakRecord> {
        val dateInGroup = AsDate(Actions.created).alias("dateInGroup")
        val rows = RowNumber(Actions.created.min())
        val minDate = Actions.created.min().alias("minDate")
        val dateMinusRowNumber = DateMinus(Actions.created, rows).alias("dateMinusRow")
        val maxDate = Actions.created.max().alias("maxDate")
        val idCounts = Actions.id.count().alias("counts")

        val groupedAlias = (Actions innerJoin Habits)
            .slice(idCounts, dateInGroup, minDate, maxDate, dateMinusRowNumber)
            .select { Habits.id eq habitId.value }
            .groupBy(dateInGroup)
            .alias("groupedAlias")

        val streak = Sum(groupedAlias[idCounts]).alias("streak")
        val startDate = MinDate(groupedAlias[minDate]).alias("startDate")
        val endDate = MaxDate(groupedAlias[maxDate]).alias("endDate")
        val dateMinusRow = groupedAlias[dateMinusRowNumber].alias("dateMinusRow")

        fun ResultRow.toStreakRecord() = StreakRecord(
            startDate = this[startDate]?.let { DateTime(it.time) },
            endDate = this[endDate]?.let { DateTime(it.time) },
            streak = this[streak]
        )

        val query = groupedAlias
            .slice(startDate, endDate, streak, dateMinusRow)
            .selectAll()
            .groupBy(dateMinusRow)
            .orderBy(endDate to SortOrder.DESC)
        return query
            .map { it.toStreakRecord() }
            .toList()
    }

    private fun weeksBetweenCreatedAnd(firstActionDate: Expression<DateTime?>) =
        Divide(DatePart(Minus(DateTrunc(Actions.created, Week), DateTrunc(firstActionDate, Week)), Days), 7)


    private fun weeksBetweenCreatedMinusRowNumber(firstActionDate: Expression<DateTime?>): Minus<Double?> {
        val rows = RowNumber(Actions.created.min())
        return Minus(weeksBetweenCreatedAnd(firstActionDate), rows)
    }
}