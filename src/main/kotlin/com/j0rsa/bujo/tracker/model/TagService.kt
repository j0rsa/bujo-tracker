package com.j0rsa.bujo.tracker.model

import arrow.core.Either
import arrow.core.Right
import arrow.core.Left
import com.j0rsa.bujo.tracker.NotFound
import com.j0rsa.bujo.tracker.TrackerError
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

    fun update(userId: UUID, tag: TagRow): Either<TrackerError, TagRow> =
        TagRepository.findOneByIdForUser(tag.id!!, userId)?.let { oldTag ->
            val user = User.findById(userId)!!
            val newTag = createTagIfNotExist(user)(tag)
            updateHabitTags(oldTag, newTag)
            updateActionTags(oldTag, newTag)
            oldTag.delete()
            Right(newTag.toRow())
        } ?: Left(NotFound)

    private fun updateHabitTags(tag: Tag, newTag: Tag) {
        val recordsToAddNewTag = HabitRepository.findAllWithOneTagWithoutAnother(tag.id.value, newTag.id.value)
        recordsToAddNewTag.map { it.tags = SizedCollection(it.tags + newTag) }
    }

    private fun updateActionTags(tag: Tag, newTag: Tag) {
        val recordsToAddNewTag = ActionRepository.findAllWithOneTagWithoutAnother(tag.id.value, newTag.id.value)
        recordsToAddNewTag.map { it.tags = SizedCollection(it.tags + newTag) }
    }

    fun createTagsIfNotExist(user: User, tags: List<TagRow>) = tags.map(createTagIfNotExist(user))
    fun createTagsIfNotExist(userId: UUID, tags: List<TagRow>) = createTagsIfNotExist(User.findById(userId)!!, tags)

    private fun addUserToTag(user: User) = { foundTag: Tag ->
        foundTag.users = SizedCollection(foundTag.users + user)
    }
}