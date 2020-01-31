package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.model.DatePartEnum.Week
import com.j0rsa.bujo.tracker.model.DatePartEnum.Year
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
        val weekInGroup = yearAndWeekFromDate().alias("weekInGroup")
        val week = yearAndWeekFromDate().alias("week")

        val idCounts = Actions.id.count().alias("counts")
        val groupedAlias = (Actions innerJoin Habits)
            .slice(idCounts, weekInGroup)
            .select { Habits.id eq habitId.value }
            .groupBy(weekInGroup)
            .alias("groupedAlias")

        val rows = RowNumber(Actions.created.min())
        val minDate = Actions.created.min().alias("minDate")
        val weekMinusRowNumber = YearWeekMinus(Actions.created.min(), rows).alias("weekMinusRow")
        val maxDate = Actions.created.max().alias("maxDate")

        val weekDataAlias = (Actions innerJoin Habits)
            .slice(week, minDate, maxDate, weekMinusRowNumber)
            .select { Habits.id eq habitId.value }
            .groupBy(week)
            .alias("weekDataAlias")

        val streak = Sum(groupedAlias[idCounts]).alias("streak")
        val startDate = MinDate(weekDataAlias[minDate]).alias("startDate")
        val endDate = MaxDate(weekDataAlias[maxDate]).alias("endDate")
        val weekMinusRow = weekDataAlias[weekMinusRowNumber].alias("weekMinusRow")

        fun ResultRow.toStreakRecord() = StreakRecord(
            startDate = this[startDate]?.let { DateTime(it.time) },
            endDate = this[endDate]?.let { DateTime(it.time) },
            streak = this[streak],
            m = this[weekMinusRow]
        )

        val query = Join(weekDataAlias).leftJoin(groupedAlias, { groupedAlias[weekInGroup] }, { weekDataAlias[week] })
            .slice(startDate, endDate, streak, weekMinusRow)
            .selectAll()
            .groupBy(weekMinusRow)
            .orderBy(endDate to SortOrder.DESC)
        return query
            .map { it.toStreakRecord() }
            .toList()
    }

    private fun yearAndWeekFromDate() =
        Plus(Times(DatePart(Actions.created, Year), 100), DatePart(Actions.created, Week))
}