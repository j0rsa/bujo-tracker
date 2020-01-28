package com.j0rsa.bujo.tracker.model

import org.jetbrains.exposed.sql.*
import java.util.*

object TagService {

    fun findAll(userId: UUID) = TagRepository.findAll(userId)
        .map { it.toRow() }

    fun createTagIfNotExist(user: User) = { tag: TagRow ->
        TagRepository.findOneForUser(tag.name, user.id.value) ?: (TagRepository.findOne(tag.name)
            ?.also(addUserToTag(user))
            ?: Tag.new {
                name = tag.name
                users = SizedCollection(listOf(user))
            })
    }

    fun createTagsIfNotExist(user: User, tags: List<TagRow>) = tags.map(createTagIfNotExist(user))
    fun createTagsIfNotExist(userId: UUID, tags: List<TagRow>) = createTagsIfNotExist(User.findById(userId)!!, tags)

    private fun addUserToTag(user: User) = { foundTag: Tag ->
        foundTag.users = SizedCollection(foundTag.users + user)
    }
}