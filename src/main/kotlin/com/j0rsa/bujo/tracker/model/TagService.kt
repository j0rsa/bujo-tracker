package com.j0rsa.bujo.tracker.model

import arrow.core.Either
import arrow.core.Right
import arrow.core.Left
import com.j0rsa.bujo.tracker.NotFound
import com.j0rsa.bujo.tracker.TrackerError
import com.j0rsa.bujo.tracker.handler.TagRow
import org.jetbrains.exposed.sql.*

object TagService {

	fun findAll(userId: UserId) = TagRepository.findAll(userId)
		.map { it.toRow() }

	fun createTagIfNotExist(user: User) = { tag: TagRow ->
		TagRepository.findOneForUser(tag.name, user.idValue()) ?: (TagRepository.findOne(tag.name)
			?.also(addUserToTag(user))
			?: Tag.new {
				name = tag.name
				users = SizedCollection(listOf(user))
			})
	}

	fun update(userId: UserId, tag: TagRow): Either<TrackerError, TagRow> =
		TagRepository.findOneByIdForUser(tag.id!!, userId)?.let { oldTag ->
			val user = UserRepository.findOne(userId)!!
			val newTag = createTagIfNotExist(user)(tag)
			updateHabitTags(oldTag, newTag)
			updateActionTags(oldTag, newTag)
			oldTag.delete()
			Right(newTag.toRow())
		} ?: Left(NotFound)

	private fun updateHabitTags(tag: Tag, newTag: Tag) {
		val recordsToAddNewTag = HabitRepository.findAllWithOneTagWithoutAnother(tag.idValue(), newTag.idValue())
		recordsToAddNewTag.map { it.tags = SizedCollection(it.tags + newTag) }
	}

	private fun updateActionTags(tag: Tag, newTag: Tag) {
		val recordsToAddNewTag = ActionRepository.findAllWithOneTagWithoutAnother(tag.idValue(), newTag.idValue())
		recordsToAddNewTag.map { it.tags = SizedCollection(it.tags + newTag) }
	}

	fun createTagsIfNotExist(user: User, tags: List<TagRow>) = tags.map(createTagIfNotExist(user))
	fun createTagsIfNotExist(userId: UserId, tags: List<TagRow>) =
		createTagsIfNotExist(UserRepository.findOne(userId)!!, tags)

	private fun addUserToTag(user: User) = { foundTag: Tag ->
		foundTag.users = SizedCollection(foundTag.users + user)
	}
}