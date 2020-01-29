package com.j0rsa.bujo.tracker.model

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.extensions.fx
import arrow.core.fix
import com.j0rsa.bujo.tracker.NotFound
import com.j0rsa.bujo.tracker.SyStemError
import com.j0rsa.bujo.tracker.TrackerError
import org.jetbrains.exposed.sql.SizedCollection

object ActionService {
    fun create(row: ActionRow): Either<TrackerError, ActionId> {
        val foundUser = UserRepository.findOne(row.userId)!!
        val foundHabits = HabitRepository.findOne(row.userId, row.habitId!!)
        val allTags = TagService.createTagsIfNotExist(foundUser, row.tags)

        return when (foundHabits.size) {
            0 -> Left(NotFound)
            1 -> Right(Action.new(ActionId.randomValue().value) {
                name = row.name
                user = foundUser
                tags = SizedCollection(allTags)
                habit = foundHabits.first()
            }.idValue())
            else -> Left(SyStemError("Found too many records"))
        }
    }

    fun create(row: BaseActionRow): ActionId {
        val foundUser = UserRepository.findOne(row.userId)!!
        val allTags = TagService.createTagsIfNotExist(foundUser, row.tags)
        return Action.new(ActionId.randomValue().value) {
            name = row.name
            user = foundUser
            tags = SizedCollection(allTags)
        }.idValue()
    }

    fun findAll(userId: UserId) = ActionRepository.findAll(userId).toList().map { it.toRow() }

    fun findOneBy(actionId: ActionId, userId: UserId): Either<TrackerError, ActionRow> =
        findOne(actionId, userId).map { it.toRow() }

    private fun findOne(actionId: ActionId, userId: UserId): Either<TrackerError, Action> {
        val actions = ActionRepository.findOneBy(actionId, userId).toList()
        return when (actions.size) {
            0 -> Left(NotFound)
            1 -> Right(actions.first())
            else -> Left(SyStemError("found too many actions"))
        }
    }

    fun update(row: BaseActionRow): Either<TrackerError, ActionRow> =
        findOne(row.id!!, row.userId).map(updateAction(row))

    private fun updateAction(row: BaseActionRow): (Action) -> ActionRow = { action: Action ->
        val allTags = TagService.createTagsIfNotExist(row.userId, row.tags)
        action.apply {
            name = row.name
            tags = SizedCollection(allTags)
        }
        action.toRow()
    }

    fun deleteOne(actionId: ActionId, userId: UserId): Either<TrackerError, Unit> =
        Either.fx<TrackerError, Unit> {
            val (action) = findOne(actionId, userId)
            action.delete()
        }.fix()
}