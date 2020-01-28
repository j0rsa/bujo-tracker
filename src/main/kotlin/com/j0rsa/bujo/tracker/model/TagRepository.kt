package com.j0rsa.bujo.tracker.model

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import java.util.*

object TagRepository {
    fun findAll(userId: UUID): List<Tag> {
        return Tag.wrapRows((UserTags leftJoin Tags)
            .slice(Tags.columns)
            .select { UserTags.userId eq userId }
            .orderBy(Tags.name to SortOrder.ASC))
            .toList()
    }


    fun findOne(tagName: String) = Tag.find { Tags.name eq tagName }.singleOrNull()

    fun findOneForUser(tagName: String, userId: UUID) = Tag.wrapRows((UserTags leftJoin Tags)
        .slice(Tags.columns)
        .select { (UserTags.userId eq userId) and (Tags.name eq tagName) })
        .singleOrNull()

    fun findOneByIdForUser(id: UUID, userId: UUID) = Tag.wrapRows((UserTags leftJoin Tags)
        .slice(Tags.columns)
        .select { (UserTags.userId eq userId) and (Tags.id eq id) })
        .singleOrNull()
}