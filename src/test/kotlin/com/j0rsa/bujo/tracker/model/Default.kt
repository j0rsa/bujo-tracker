package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.handler.TagRow
import com.j0rsa.bujo.tracker.handler.UserInfo
import com.j0rsa.bujo.tracker.handler.ValueRow
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.insert
import org.joda.time.DateTime

fun defaultUser(userEmail: String = "testEmail") = User.new {
    name = "testUser"
    email = userEmail
}

fun defaultTelegramUser() = User.new {
    telegramId = 1L
    firstName = "testTelegramUser"
}

fun defaultUserInfo(id: Long = 1L, firstName: String = "testTelegramUser") = UserInfo(
    telegramId = id,
    firstName = firstName
)

fun defaultTagRow(name: String = "testTag", id: TagId? = null) =
    TagRow(name, id)

fun defaultTag(tagUsers: List<User>, tagName: String = "testTag") = Tag.new {
    name = tagName
    users = SizedCollection(tagUsers)
}

fun defaultHabitRow(
    userId: UserId,
    name: String = "testHabit",
    tags: List<TagRow> = listOf(defaultTagRow()),
    id: HabitId? = null,
    values: List<ValueType> = emptyList()
) = HabitRow(
    name,
    tags,
    userId,
    1,
    Period.Day,
    id = id,
    values = values
)

fun defaultActionRow(
    userId: UserId,
    habitId: HabitId,
    name: String = "testHabitAction",
    tags: List<TagRow> = listOf(defaultTagRow()),
    id: ActionId? = null
) = ActionRow(
    BaseActionRow(name, userId, tags, id),
    habitId
)

fun defaultValue(type: ValueType = ValueType.Mood, value: String = "testValue") = ValueRow(type, value)
fun defaultBaseActionRow(
    userId: UserId,
    name: String = "testTagAction",
    tags: List<TagRow> = listOf(defaultTagRow()),
    id: ActionId? = null,
    values: List<ValueRow> = emptyList()
) = BaseActionRow(name, userId, tags, id, values)

fun defaultHabit(
    habitUser: User,
    tagList: List<Tag> = listOf(),
    habitName: String = "testHabit",
    values: List<ValueType> = emptyList()
) = Habit.new(HabitId.randomValue().value) {
    name = habitName
    user = habitUser
    numberOfRepetitions = 1
    period = Period.Day
    this.values = values.map { it.toString() }
    tags = SizedCollection(tagList)
}

fun defaultAction(
    user: User,
    tags: List<Tag> = listOf(),
    actionDescription: String = "testAction",
    habit: Habit? = null,
    created: DateTime? = DateTime.now()
) = Action.new {
    this.description = actionDescription
    this.user = user
    this.habit = habit
    this.tags = SizedCollection(tags)
    this.created = created
}

fun insertDefaultAction(
    user: User,
    actionDescription: String = "testAction",
    habit: Habit? = null,
    created: DateTime? = DateTime.now()
) = Actions.insert {
    it[this.description] = actionDescription
    it[this.user] = user.id
    it[this.habit] = habit?.id
    it[this.created] = created
}