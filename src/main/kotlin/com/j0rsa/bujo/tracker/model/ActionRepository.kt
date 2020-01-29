package com.j0rsa.bujo.tracker.model

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.notExists
import org.jetbrains.exposed.sql.select

object ActionRepository {
    fun findAllWithOneTagWithoutAnother(oneTag: TagId, anotherTag: TagId): List<Action> {
        val query = Actions
            .slice(Actions.columns)
            .select {
                notExists(ActionTags.select {
                    (ActionTags.tagId eq anotherTag.value) and (ActionTags.actionId eq Actions.id)
                }) and exists(ActionTags.select {
                    (ActionTags.tagId eq oneTag.value) and (ActionTags.actionId eq Actions.id)
                })
            }

        return Action.wrapRows(query).toList()
    }
}