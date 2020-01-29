package com.j0rsa.bujo.tracker.model

import arrow.core.Either
import com.j0rsa.bujo.tracker.NotFound
import com.j0rsa.bujo.tracker.SyStemError
import com.j0rsa.bujo.tracker.TrackerError
import org.jetbrains.exposed.sql.SizedCollection

object ActionService {
    fun create(row: HabitActionRow): Either<TrackerError, ActionId> {
        val foundUser = UserRepository.findOne(row.userId)!!
        val foundHabits = HabitRepository.findOne(row.userId, row.habitId!!)

        return when (foundHabits.size) {
            0 -> Either.Left(NotFound)
            1 -> Either.Right(Action.new(ActionId.randomValue().value) {
                name = row.name
                user = foundUser
                tags = foundHabits.first().tags
                habit = foundHabits.first()
            }.idValue())
            else -> Either.Left(SyStemError("Found too many records"))
        }
    }

    fun create(row: TagActionRow): ActionId {
        val foundUser = UserRepository.findOne(row.userId)!!
        val allTags = TagService.createTagsIfNotExist(foundUser, row.tags)
        return Action.new(ActionId.randomValue().value) {
            name = row.name
            user = foundUser
            tags = SizedCollection(allTags)
        }.idValue()
    }

    fun findAll(userId: UserId) = ActionRepository.findAll(userId).toList().map { it.toActionRow() }
}