package com.j0rsa.bujo.tracker.model

import org.jetbrains.exposed.sql.*

object HabitRepository {

	fun findAllWithOneTagWithoutAnother(oneTag: TagId, anotherTag: TagId): List<Habit> {
		val query = Habits
			.slice(Habits.columns)
			.select {
				notExists(HabitTags.select {
					(HabitTags.tagId eq anotherTag.value) and (HabitTags.habitId eq Habits.id)
				}) and exists(HabitTags.select {
					(HabitTags.tagId eq oneTag.value) and (HabitTags.habitId eq Habits.id)
				})
			}

		return Habit.wrapRows(query).toList()
	}

	fun findOne(userId: UserId, id: HabitId) =
		Habit.find { (Habits.user eq userId.value) and (Habits.id eq id.value) }.toList()

	fun findById(id: HabitId) = Habit.findById(id.value)

	fun findAll(userId: UserId) = Habit.find { Habits.user eq userId.value }.toList()
}