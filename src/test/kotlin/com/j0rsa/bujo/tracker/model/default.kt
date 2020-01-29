package com.j0rsa.bujo.tracker.model

import org.jetbrains.exposed.sql.SizedCollection

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
    Duration.Day,
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

fun defaultHabit(habitUser: User, tagList: List<Tag> = listOf(), habitName: String = "testHabit") =
    Habit.new {
        name = habitName
        user = habitUser
        numberOfRepetitions = 1
        duration = Duration.Day
        tags = SizedCollection(tagList)
    }

fun defaultAction(
    actionUser: User,
    tagList: List<Tag> = listOf(),
    actionName: String = "testAction",
    actionHabit: Habit? = null
) = Action.new {
    description = actionName
    user = actionUser
    habit = actionHabit
    tags = SizedCollection(tagList)
}