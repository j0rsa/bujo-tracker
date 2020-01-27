package com.j0rsa.bujo.tracker.model

import org.jetbrains.exposed.sql.*
import java.util.*

object TagService {

    fun findAll(userId: UUID) = (UserTags leftJoin Tags)
        .slice(Tags.columns)
        .select { UserTags.userId eq userId }
        .orderBy(Tags.name to SortOrder.ASC)
        .toList()
        .map { it.toTagRow() }

    fun createTagIfNotExist(user: User) = { tag: TagRow ->
        findOne(tag.name)
            ?.also(addUserToTagIfNotExist(user))
            ?: Tag.new {
                name = tag.name
                users = SizedCollection(listOf(user))
            }
    }

    fun createTagsIfNotExist(user: User, tags: List<TagRow>) = tags.map(createTagIfNotExist(user))


    private fun addUserToTagIfNotExist(user: User) = { foundTag: Tag ->
        if (!foundTag.users.contains(user)) foundTag.users = SizedCollection(foundTag.users + user)
    }

    private fun findOne(tagName: String) = Tag.find { Tags.name eq tagName }.singleOrNull()
}