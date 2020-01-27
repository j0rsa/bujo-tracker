package com.j0rsa.bujo.tracker.model

import org.jetbrains.exposed.sql.SizedCollection
import java.util.*

object HabitService {
    fun create(habitRow: HabitRow): UUID {
        val foundUser = User.findById(habitRow.userId)!!
        val allTags = TagsRepository.createTagsIfNotExist(foundUser, habitRow.tags)
        val habit = Habit.new(UUID.randomUUID()) {
            name = habitRow.name
            user = foundUser
            tags = SizedCollection(allTags)
            quote = habitRow.quote
            bad = habitRow.bad
        }
        return habit.id.value
    }
}