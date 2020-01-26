package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.handler.HabitDto
import org.jetbrains.exposed.sql.SizedCollection
import java.util.*

object HabitService {
    fun create(habitDto: HabitDto): () -> UUID = {
        val foundUser = User.findById(habitDto.userId)!!
        val allTags = TagsRepository.createTagsIfNotExist(foundUser, habitDto.tags)
        Habit.new(UUID.randomUUID()) {
            name = habitDto.name
            quote = habitDto.quote
            bad = habitDto.bad
            tags = SizedCollection(allTags)
            user = foundUser
        }.id.value
    }


}