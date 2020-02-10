package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.handler.ValueRow

object ValueService {

    fun create(value: ValueRow) = Value.new {
        type = value.type
        this.value = value.value
    }

    fun create(values: List<ValueRow>) = values.map { create(it) }
}