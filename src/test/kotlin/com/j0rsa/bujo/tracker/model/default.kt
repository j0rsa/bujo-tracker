package com.j0rsa.bujo.tracker.model

import org.jetbrains.exposed.sql.SizedCollection
import java.util.*


fun defaultUser(userEmail: String = "testEmail") = User.new {
    name = "testUser"
    email = userEmail
}

fun defaultTagRow(name: String = "testTag") = TagRow(name)

fun defaultTag(tagUsers: List<User>, tagName: String = "testTag") = Tag.new {
    name = tagName
    users = SizedCollection(tagUsers)
}

fun defaultHabitRow(userId: UUID, name: String = "testHabit", tags: List<TagRow> = listOf(defaultTagRow())) = HabitRow(
    name,
    tags,
    userId
)

fun defaultHabit(habitUser: User, tagList: List<Tag>, habitName: String = "testHabit") = Habit.new {
    name = habitName
    user = habitUser
    tags = SizedCollection(tagList)
}