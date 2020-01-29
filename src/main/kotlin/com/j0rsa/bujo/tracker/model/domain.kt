package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.handler.HabitView
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import java.util.*

object Users : UUIDTable("users", "id") {
    val name = varchar("name", 50)
    val email = varchar("email", 50).index()
    val otp = varchar("otp", 50).nullable()
}

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users)

    var name by Users.name
    var email by Users.email
    var otp by Users.otp
    fun idValue() = UserId(id.value)
}

object Habits : UUIDTable("habits", "id") {
    val name = varchar("name", 50)
    val user = reference("user", Users)
    val quote = varchar("quote", 500).nullable()
    val bad = bool("bad").default(false).nullable()
}

object HabitTags : Table("habit-tags") {
    val habitId = reference("habitId", Habits, onDelete = ReferenceOption.CASCADE).primaryKey(0)
    val tagId = reference("tagId", Tags, onDelete = ReferenceOption.CASCADE).primaryKey(1)
}

class Habit(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Habit>(Habits)

    var name by Habits.name
    var user by User referencedOn Habits.user
    var userId by Habits.user
    var tags by Tag via HabitTags
    var quote by Habits.quote
    var bad by Habits.bad

    fun toRow(): HabitRow = HabitRow(
        name,
        tags.map { it.toRow() },
        userIdValue(),
        quote,
        bad,
        HabitId(id.value)
    )

    fun idValue() = HabitId(id.value)
    fun userIdValue() = UserId(userId.value)
}

data class HabitRow(
    val name: String,
    val tags: List<TagRow>,
    val userId: UserId,
    val quote: String? = null,
    val bad: Boolean? = null,
    val id: HabitId? = null
) {
    constructor(habitView: HabitView, userId: UserId) : this(
        habitView.name,
        habitView.tagList,
        userId,
        habitView.quote,
        habitView.bad
    )

    fun toView(): HabitView = HabitView(
        name,
        quote,
        bad,
        tags,
        id
    )
}

object Tags : UUIDTable("tags", "id") {
    val name = varchar("name", 50).uniqueIndex()
}

class Tag(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Tag>(Tags)

    var name by Tags.name
    var users by User via UserTags

    fun toRow(): TagRow = TagRow(
        name,
        id.value
    )
}

data class TagRow(
    val name: String,
    val id: UUID? = null
)

object ActionTags : Table("action-tags") {
    val actionId = reference("actionId", Actions, onDelete = ReferenceOption.CASCADE).primaryKey(0)
    val tagId = reference("tagId", Tags, onDelete = ReferenceOption.CASCADE).primaryKey(1)
}

object Actions : UUIDTable("actions", "id") {
    val name = varchar("name", 50)
    val user = reference("user", Users)
    val habit = reference("habit", Habits).nullable()
}

class Action(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Action>(Actions)

    var name by Actions.name
    var user by User referencedOn Actions.user
    var tags by Tag via ActionTags
    var habit by Habit optionalReferencedOn Actions.habit
}

object UserTags : Table("user-tags") {
    val userId = reference("userId", Users).primaryKey(0)
    val tagId = reference("tagId", Tags, onDelete = ReferenceOption.CASCADE).primaryKey(1)
}

fun createSchema() {
    SchemaUtils.create(
        Users,
        Tags,
        Habits,
        Actions,
        HabitTags,
        ActionTags,
        UserTags
    )
}

fun dropSchema() {
    SchemaUtils.drop(
        Users,
        Tags,
        Habits,
        Actions,
        HabitTags,
        ActionTags,
        UserTags
    )
}

inline class HabitId(val value: UUID) {
    companion object {
        fun randomValue() = HabitId(UUID.randomUUID())
    }
}

inline class UserId(val value: UUID) {
    companion object {
        fun randomValue() = UserId(UUID.randomUUID())
    }
}