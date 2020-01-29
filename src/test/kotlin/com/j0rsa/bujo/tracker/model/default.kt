package com.j0rsa.bujo.tracker.model

import org.jetbrains.exposed.sql.SizedCollection
import java.util.*

fun defaultUser(userEmail: String = "testEmail") = User.new {
    name = "testUser"
    email = userEmail
}

fun defaultTagRow(name: String = "testTag", id: UUID? = null) = TagRow(name, id)

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
    id = id
)

fun defaultHabit(habitUser: User, tagList: List<Tag>, habitName: String = "testHabit") = Habit.new {
    name = habitName
    user = habitUser
    tags = SizedCollection(tagList)
}

fun defaultAction(actionUser: User, tagList: List<Tag>, actionName: String = "testHabit") = Action.new {
    name = actionName
    user = actionUser
    tags = SizedCollection(tagList)
}