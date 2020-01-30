package com.j0rsa.bujo.tracker.model

import arrow.core.Either
import arrow.core.extensions.fx
import arrow.core.fix
import com.j0rsa.bujo.tracker.NotFound
import com.j0rsa.bujo.tracker.SyStemError
import com.j0rsa.bujo.tracker.TrackerError
import org.jetbrains.exposed.sql.SizedCollection

object HabitService {
    fun create(habitRow: HabitRow): HabitId {
        val foundUser = UserRepository.findOne(habitRow.userId)!!
        val allTags = TagService.createTagsIfNotExist(foundUser, habitRow.tags)
        val habit = Habit.new(HabitId.randomValue().value) {
            name = habitRow.name
            user = foundUser
            numberOfRepetitions = habitRow.numberOfRepetitions
            period = habitRow.period
            tags = SizedCollection(allTags)
            quote = habitRow.quote
            bad = habitRow.bad
        }
        return habit.idValue()
    }

    fun update(habitRow: HabitRow): Either<TrackerError, HabitRow> =
        findOne(habitRow.id!!, habitRow.userId).map(updateHabit(habitRow))

    private fun updateHabit(habitRow: HabitRow) = { habit: Habit ->
        val allTags = TagService.createTagsIfNotExist(habitRow.userId, habitRow.tags)
        habit.apply {
            name = habitRow.name
            numberOfRepetitions = habitRow.numberOfRepetitions
            period = habitRow.period
            quote = habitRow.quote
            bad = habitRow.bad
            tags = SizedCollection(allTags)
        }
        habit.toRow()
    }

    fun findOneBy(id: HabitId, userId: UserId): Either<TrackerError, HabitRow> =
        findOne(id, userId).map { it.toRow() }

    private fun findOne(id: HabitId, userId: UserId): Either<TrackerError, Habit> {
        val habits = HabitRepository.findOne(userId, id)
        return when (habits.size) {
            0 -> Either.Left(NotFound)
            1 -> Either.Right(habits.first())
            else -> Either.Left(SyStemError("Found too many records"))
        }
    }

    fun findAll(userId: UserId): List<HabitRow> = HabitRepository.findAll(userId).map { it.toRow() }

    fun deleteOne(id: HabitId, userId: UserId) = Either.fx<TrackerError, Unit> {
        val (habit) = findOne(id, userId)
        habit.delete()
    }.fix()
}