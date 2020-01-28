package com.j0rsa.bujo.tracker.model

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.fix
import com.j0rsa.bujo.tracker.NotFound
import com.j0rsa.bujo.tracker.SyStemError
import com.j0rsa.bujo.tracker.TrackerError
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.and
import java.util.*

object HabitService {
    fun create(habitRow: HabitRow): UUID {
        val foundUser = User.findById(habitRow.userId)!!
        val allTags = TagService.createTagsIfNotExist(foundUser, habitRow.tags)
        val habit = Habit.new(UUID.randomUUID()) {
            name = habitRow.name
            user = foundUser
            tags = SizedCollection(allTags)
            quote = habitRow.quote
            bad = habitRow.bad
        }
        return habit.id.value
    }

    fun update(habitRow: HabitRow): Either<TrackerError, HabitRow> =
        findOne(habitRow.id!!, habitRow.userId).map { update(habitRow, it).toRow() }

    private fun update(habitRow: HabitRow, habit: Habit): Habit {
        val allTags = TagService.createTagsIfNotExist(habitRow.userId, habitRow.tags)
        habit.apply {
            name = habitRow.name
            quote = habitRow.quote
            bad = habitRow.bad
            tags = SizedCollection(allTags)
        }
        return habit
    }

    fun findOneBy(id: UUID, userId: UUID): Either<TrackerError, HabitRow> =
        findOne(id, userId).map { it.toRow() }

    private fun findOne(id: UUID, userId: UUID): Either<TrackerError, Habit> {
        val habits = Habit.find { (Habits.user eq userId) and (Habits.id eq id) }.toList()
        return when (habits.size) {
            0 -> Either.Left(NotFound)
            1 -> Either.Right(habits.first())
            else -> Either.Left(SyStemError("Found too many records"))
        }
    }

    fun findAll(userId: UUID): List<HabitRow> = Habit.find { Habits.user eq userId }.toList().map { it.toRow() }

    fun deleteOne(id: UUID, userId: UUID) = Either.fx<TrackerError, Unit> {
        val (habit) = findOne(id, userId)
        habit.delete()
    }.fix()
}