package com.j0rsa.bujo.tracker

import com.j0rsa.bujo.tracker.model.ActionId
import com.j0rsa.bujo.tracker.model.HabitId
import com.j0rsa.bujo.tracker.model.Period
import com.j0rsa.bujo.tracker.model.UserId
import org.joda.time.LocalDateTime

sealed class Command

data class CreateAction(
	val actionId: ActionId,
	val userId: UserId,
	val tag: String,
	val date: LocalDateTime,
	val message: String?,
) : Command()

data class CreateHabit(
	val habitId: HabitId,
	val userId: UserId,
	val tag: List<String>,
	val numberOfRepetitions: Int,
	val period: Period,
	val message: String?,
) : Command()