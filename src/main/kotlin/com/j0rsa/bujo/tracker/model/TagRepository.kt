package com.j0rsa.bujo.tracker.model

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

object TagRepository {
	fun findAll(userId: UserId): List<Tag> {
		return Tag.wrapRows((UserTags leftJoin Tags)
			.slice(Tags.columns)
			.select { UserTags.userId eq userId.value }
			.orderBy(Tags.name to SortOrder.ASC))
			.toList()
	}

	fun findOne(tagName: String) = Tag.find { Tags.name eq tagName }.singleOrNull()

	fun findById(id: TagId) = Tag.findById(id.value)

	fun findOneForUser(tagName: String, userId: UserId) = Tag.wrapRows((UserTags leftJoin Tags)
		.slice(Tags.columns)
		.select { (UserTags.userId eq userId.value) and (Tags.name eq tagName) })
		.singleOrNull()

	fun findOneByIdForUser(id: TagId, userId: UserId) = Tag.wrapRows((UserTags leftJoin Tags)
		.slice(Tags.columns)
		.select { (UserTags.userId eq userId.value) and (Tags.id eq id.value) })
		.singleOrNull()
}