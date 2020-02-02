package com.j0rsa.bujo.tracker

import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.Weeks

fun DateTime.isCurrentDay() = Days.daysBetween(this, DateTime.now()).days in (0..1)
fun DateTime.isCurrentWeek() = Weeks.weeksBetween(this, DateTime.now()).weeks in (0..1)