package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.handler.ActionView
import com.j0rsa.bujo.tracker.handler.HabitView
import com.j0rsa.bujo.tracker.handler.TagRow
import com.j0rsa.bujo.tracker.handler.ValueRow
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime
import org.postgresql.util.PGobject
import java.math.BigDecimal
import java.util.*
import kotlin.reflect.KProperty

object Users : UUIDTable("users", "id") {
    val name = varchar("name", 50).nullable()
    val telegramId = long("telegram_id").uniqueIndex().nullable()
    val email = varchar("email", 50).uniqueIndex().nullable()
    val firstName = varchar("first_name", 50).nullable()
    val lastName = varchar("last_name", 50).nullable()
    val language = varchar("language", 5).nullable()
    val otp = varchar("otp", 50).nullable()
    val created = datetime("created").clientDefault { DateTime.now() }.nullable()
}

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users)

    var name by Users.name
    var telegramId by Users.telegramId
    var email by Users.email
    var firstName by Users.firstName
    var lastName by Users.lastName
    var language by Users.language
    var otp by Users.otp
    fun idValue() = UserId(id.value)
}

enum class Period {
    Day,
    Week
}

object Habits : UUIDTable("habits", "id") {
    val name = varchar("name", 50)
    val user = reference("user", Users)
    val numberOfRepetitions = integer("number_of_repetitions")
    val period = enumeration("period", Period::class)
    val quote = varchar("quote", 500).nullable()
    val bad = bool("bad").default(false).nullable()
    val startFrom = datetime("startFrom").clientDefault { DateTime.now() }.nullable()
    val created = datetime("created").clientDefault { DateTime.now() }.nullable()
    val values = arrayOfString("values")
}

object HabitTags : Table("habit_tags") {
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
    var numberOfRepetitions by Habits.numberOfRepetitions
    var period by Habits.period
    var startFrom by Habits.startFrom
    var values by Habits.values

    fun toRow(): HabitRow = HabitRow(
        name,
        tags.map { it.toRow() },
        userIdValue(),
        numberOfRepetitions,
        period,
        quote,
        bad,
        startFrom,
        idValue(),
        values.map { ValueType.valueOf(it) }
    )

    fun idValue() = HabitId(id.value)
    fun userIdValue() = UserId(userId.value)
}

data class HabitRow(
    val name: String,
    val tags: List<TagRow>,
    val userId: UserId,
    val numberOfRepetitions: Int,
    val period: Period,
    val quote: String? = null,
    val bad: Boolean? = null,
    val startFrom: DateTime? = null,
    val id: HabitId? = null,
    val values: List<ValueType> = emptyList()
) {
    constructor(habit: HabitView, userId: UserId) : this(
        habit.name,
        habit.tags,
        userId,
        habit.numberOfRepetitions,
        habit.period,
        habit.quote,
        habit.bad,
        habit.startFrom,
        habit.id,
        habit.values
    )

    fun toView(): HabitView = HabitView(
        name,
        tags,
        numberOfRepetitions,
        period,
        quote,
        bad,
        startFrom,
        id,
        values
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
        idValue()
    )

    fun idValue() = TagId(id.value)
}

object ActionTags : Table("action_tags") {
    val actionId = reference("actionId", Actions, onDelete = ReferenceOption.CASCADE).primaryKey(0)
    val tagId = reference("tagId", Tags, onDelete = ReferenceOption.CASCADE).primaryKey(1)
}

object Actions : UUIDTable("actions", "id") {
    val description = varchar("description", 50)
    val user = reference("user", Users)
    val habit = optReference("habit", Habits, onDelete = ReferenceOption.CASCADE)
    val created = datetime("created").clientDefault { DateTime.now() }.nullable()
}

class Action(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Action>(Actions)

    var description by Actions.description
    var user by User referencedOn Actions.user
    var userId by Actions.user
    var tags by Tag via ActionTags
    var habit by Habit optionalReferencedOn Actions.habit
    var habitId by Actions.habit
    var created by Actions.created
    var values by Value via ActionValues

    fun toRow(): ActionRow = ActionRow(
        toBaseActionRow(),
        habitIdValue()
    )

    private fun toBaseActionRow(): BaseActionRow = BaseActionRow(
        description,
        userIdValue(),
        tags.map { it.toRow() },
        idValue(),
        values.map { it.toRow() }
    )

    fun idValue() = ActionId(id.value)
    fun userIdValue() = UserId(userId.value)
    fun habitIdValue() = habitId?.let { HabitId(it.value) }
}

abstract class WithBaseActionRow(baseRow: BaseActionRow) {
    val userId: UserId by baseRow
    val description: String by baseRow
    val tags: List<TagRow> by baseRow
    val id: ActionId? by baseRow
    val values: List<ValueRow> by baseRow
}

data class BaseActionRow(
    val description: String,
    val userId: UserId,
    val tags: List<TagRow>,
    val id: ActionId? = null,
    val values: List<ValueRow> = emptyList()
) {
    constructor(view: ActionView, userId: UserId) : this(
        view.description,
        userId,
        view.tags,
        view.id,
        view.values
    )

    @Suppress("IMPLICIT_CAST_TO_ANY")
    inline operator fun <reified T> getValue(withBaseActionRow: WithBaseActionRow, property: KProperty<*>): T {
        return when (property.name) {
            WithBaseActionRow::description::name.get() -> this.description
            WithBaseActionRow::userId::name.get() -> this.userId
            WithBaseActionRow::tags::name.get() -> this.tags
            WithBaseActionRow::values::name.get() -> this.values
            WithBaseActionRow::id::name.get() -> this.id
            else -> throw RuntimeException()
        } as T
    }
}

data class ActionRow(
    val baseRow: BaseActionRow,
    val habitId: HabitId? = null
) : WithBaseActionRow(baseRow) {
    constructor(view: ActionView, userId: UserId, habitId: HabitId) : this(
        BaseActionRow(view, userId),
        habitId
    )

    fun toView(): ActionView = ActionView(
        description,
        tags,
        habitId,
        id,
        values
    )
}

data class StreakRecord(
    val startDate: DateTime,
    val endDate: DateTime,
    val streak: BigDecimal
)

object UserTags : Table("user_tags") {
    val userId = reference("userId", Users, onDelete = ReferenceOption.CASCADE).primaryKey(0)
    val tagId = reference("tagId", Tags, onDelete = ReferenceOption.CASCADE).primaryKey(1)
}

enum class ValueType {
    Mood,
    EndDate
}

object Values : UUIDTable("values", "id") {
    val type = customEnumeration(
        "value_type",
        "ValueTypeEnum",
        { value -> ValueType.valueOf(value as String) },
        { PGEnum("ValueTypeEnum", it) })
    val value = varchar("description", 200).nullable()
}

object ActionValues : Table("action_values") {
    val actionId = reference("actionId", Actions, onDelete = ReferenceOption.CASCADE).primaryKey(0)
    val valueId = reference("valueId", Values, onDelete = ReferenceOption.CASCADE).primaryKey(1)
}

class Value(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Value>(Values)

    var value by Values.value
    var type by Values.type

    fun idValue() = ValueId(id.value)

    fun toRow(): ValueRow = ValueRow(type, value)
}

fun createSchema() {
    SchemaUtils.createMissingTablesAndColumns(
        Users,
        Tags,
        Habits,
        Actions,
        HabitTags,
        ActionTags,
        UserTags,
        Values,
        ActionValues
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
        UserTags,
        Values
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

inline class TagId(val value: UUID) {
    companion object {
        fun randomValue() = TagId(UUID.randomUUID())
    }
}

inline class ActionId(val value: UUID) {
    companion object {
        fun randomValue() = ActionId(UUID.randomUUID())
    }
}

inline class ValueId(val value: UUID) {
    companion object {
        fun randomValue() = ValueId(UUID.randomUUID())
    }
}

class PGEnum<T : Enum<T>>(enumTypeName: String, enumValue: T?) : PGobject() {
    init {
        value = enumValue?.name
        type = enumTypeName
    }
}