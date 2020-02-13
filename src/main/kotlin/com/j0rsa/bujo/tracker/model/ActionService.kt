package com.j0rsa.bujo.tracker.model

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.extensions.fx
import arrow.core.fix
import com.j0rsa.bujo.tracker.*
import com.j0rsa.bujo.tracker.handler.StreakRow
import org.jetbrains.exposed.sql.SizedCollection
import java.math.BigDecimal

object ActionService {
	fun create(row: ActionRow): Either<TrackerError, ActionId> {
		val foundUser = UserRepository.findOne(row.userId)!!
		val foundHabits = HabitRepository.findOne(row.userId, row.habitId!!)

		return when (foundHabits.size) {
			0 -> Left(NotFound)
			1 -> {
				val tags = TagService.createTagsIfNotExist(foundUser, row.tags)
				val action = Action.new(ActionId.randomValue().value) {
					description = row.description
					user = foundUser
					this.tags = SizedCollection(tags)
					habit = foundHabits.first()
				}
				ValueService.create(row.values, action)
				Right(action.idValue())
			}
			else -> Left(SyStemError("Found too many records"))
		}
	}

	fun create(row: BaseActionRow): ActionId {
		val foundUser = UserRepository.findOne(row.userId)!!
		val tags = TagService.createTagsIfNotExist(foundUser, row.tags)
		val action = Action.new(ActionId.randomValue().value) {
			description = row.description
			user = foundUser
			this.tags = SizedCollection(tags)
		}
		ValueService.create(row.values, action)
		return action.idValue()
	}

	fun findAll(userId: UserId) = ActionRepository.findAll(userId).toList().map { it.toRow() }

	fun findOneBy(actionId: ActionId, userId: UserId): Either<TrackerError, Action> =
		findOne(actionId, userId)

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
		val tags = TagService.createTagsIfNotExist(row.userId, row.tags)
		action.apply {
			description = row.description
			this.tags = SizedCollection(tags)
		}
		ValueService.reCreate(row.values, action)
		action.toRow()
	}

	fun deleteOne(actionId: ActionId, userId: UserId): Either<TrackerError, Unit> =
		Either.fx<TrackerError, Unit> {
			val (action) = findOne(actionId, userId)
			action.delete()
		}.fix()

	fun findCurrentStreakForDay(id: HabitId, numberOfRepetitions: Int): BigDecimal =
		ActionRepository.findCurrentStreakForDay(id, numberOfRepetitions) ?: BigDecimal.ZERO

	fun findStreakForDay(id: HabitId, numberOfRepetitions: Int): StreakRow {
		val result = ActionRepository.findStreakForDay(id, numberOfRepetitions)
		val currentStreak = result.firstOrNull()?.checkStreakOrNull(isEndDateCurrentDay) ?: BigDecimal.ZERO
		return StreakRow(currentStreak, maxStreakOrZero(result))
	}

	fun findCurrentStreakForWeek(id: HabitId, numberOfRepetitions: Int): BigDecimal =
		ActionRepository.findCurrentStreakForWeek(id, numberOfRepetitions) ?: BigDecimal.ZERO

	fun findStreakForWeek(id: HabitId, numberOfRepetitions: Int): StreakRow {
		val result = ActionRepository.findStreakForWeek(id, numberOfRepetitions)
		val currentStreak = result.firstOrNull()?.checkStreakOrNull(isEndDateCurrentWeek) ?: BigDecimal.ZERO
		return StreakRow(currentStreak, maxStreakOrZero(result))
	}

	private val isEndDateCurrentWeek = { record: StreakRecord ->
		record.endDate.isCurrentWeek()
	}

	private val isEndDateCurrentDay = { record: StreakRecord ->
		record.endDate.isCurrentDay()
	}

	private fun maxStreakOrZero(result: List<StreakRecord>) =
		result.map { it.streak }.max() ?: BigDecimal.ZERO

	private fun StreakRecord.checkStreakOrNull(checker: (StreakRecord) -> Boolean) =
		if (checker(this)) streak else null
}