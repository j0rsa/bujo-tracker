package com.j0rsa.bujo.tracker.model

import org.jetbrains.exposed.sql.SizedCollection

object TagsRepository {

    private fun addUserToTagIfNotExist(user: User) = { foundTag: Tag ->
        if (!foundTag.users.contains(user)) foundTag.users = SizedCollection(foundTag.users + user)
    }

    fun createTagsIfNotExist(user: User, tags: List<TagRow>) = tags.map(createTagIfNotExist(user))

    fun createTagIfNotExist(user: User) = { tag: TagRow ->
        findOne(tag.name)
            ?.also(addUserToTagIfNotExist(user))
            ?: Tag.new {
                name = tag.name
                users = SizedCollection(listOf(user))
            }
    }

    private fun findOne(tagName: String) = Tag.find { Tags.name eq tagName }.singleOrNull()
}