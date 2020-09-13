package com.j0rsa.bujo.tracker

import com.j0rsa.bujo.tracker.model.ActionId
import com.j0rsa.bujo.tracker.model.HabitId
import com.j0rsa.bujo.tracker.model.Period
import com.j0rsa.bujo.tracker.model.UserId
import org.joda.time.LocalDateTime

sealed class Event

data class TagActionCreated(
	val actionId: ActionId,
	val userId: UserId,
	val tags: Set<String>,
	val date: LocalDateTime,
	val message: String?,
) : Event()

data class HabitCreated(
	val habitId: HabitId,
	val userId: UserId,
	val tags: Set<String>,
	val numberOfRepetitions: Int,
	val period: Period,
	val message: String?,
) : Event()