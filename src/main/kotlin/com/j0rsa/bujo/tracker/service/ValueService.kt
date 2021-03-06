package com.j0rsa.bujo.tracker.service

import com.j0rsa.bujo.tracker.handler.ValueRow
import com.j0rsa.bujo.tracker.model.Action
import com.j0rsa.bujo.tracker.model.Value
import com.j0rsa.bujo.tracker.model.Values
import org.jetbrains.exposed.sql.deleteWhere

object ValueService {

	fun create(row: ValueRow, action: Action) = Value.new {
		this.action = action
		type = row.type
		this.value = row.value
		this.name = row.name
	}

	fun create(values: List<ValueRow>, action: Action) = values.map {
        create(
            it,
            action
        )
    }
	fun reCreate(values: List<ValueRow>, action: Action) {
		Values.deleteWhere { Values.actionId eq action.id }
		values.map { create(it, action) }
	}

	fun findAll() = Value.all().toList()
}