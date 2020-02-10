package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.handler.ValueRow
import org.jetbrains.exposed.sql.deleteWhere

object ValueService {

    fun create(value: ValueRow, action: Action) = Value.new {
        this.action = action
        type = value.type
        this.value = value.value
    }

    fun create(values: List<ValueRow>, action: Action) = values.map { create(it, action) }
    fun reCreate(values: List<ValueRow>, action: Action) {
        Values.deleteWhere { Values.actionId eq action.id }
        values.map { create(it, action) }
    }

    fun findAll() = Value.all().toList()
}