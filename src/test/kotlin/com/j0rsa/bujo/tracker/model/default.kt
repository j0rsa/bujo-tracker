package com.j0rsa.bujo.tracker.model

import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.insert
import org.joda.time.DateTime

fun defaultUser(userEmail: String = "testEmail") = User.new {
    name = "testUser"
    email = userEmail
}

fun defaultTagRow(name: String = "testTag", id: TagId? = null) = TagRow(name, id)

fun defaultTag(tagUsers: List<User>, tagName: String = "testTag") = Tag.new {
    name = tagName
    users = SizedCollection(tagUsers)
}

fun defaultHabitRow(
    userId: UserId,
    name: String = "testHabit",
    tags: List<TagRow> = listOf(defaultTagRow()),
    id: HabitId? = null
) = HabitRow(
    name,
    tags,
    userId,
    1,
    Period.Day,
    id = id
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

fun defaultBaseActionRow(
    userId: UserId,
    name: String = "testTagAction",
    tags: List<TagRow> = listOf(defaultTagRow()),
    id: ActionId? = null
) = BaseActionRow(name, userId, tags, id)

fun defaultHabit(
    habitUser: User,
    tagList: List<Tag> = listOf(), habitName: String = "testHabit"
) =
    Habit.new(HabitId.randomValue().value) {
        name = habitName
        user = habitUser
        numberOfRepetitions = 1
        period = Period.Day
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