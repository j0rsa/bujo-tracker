package com.j0rsa.bujo.tracker.model

import org.jetbrains.exposed.sql.*
import java.util.*

object HabitRepository {

    fun findAllWithOneTagWithoutAnother(oneTag: UUID, anotherTag: UUID): List<Habit> {
        val query = Habits
            .slice(Habits.columns)
            .select {
                notExists(HabitTags.select {
                    (HabitTags.tagId eq anotherTag) and (HabitTags.habitId eq Habits.id)
                }) and exists(HabitTags.select {
                    (HabitTags.tagId eq oneTag) and (HabitTags.habitId eq Habits.id)
                })
            }

        return Habit.wrapRows(query).toList()
    }
}