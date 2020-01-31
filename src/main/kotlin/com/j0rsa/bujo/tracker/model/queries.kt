package com.j0rsa.bujo.tracker.model

import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime
import java.math.BigDecimal
import java.sql.Timestamp

enum class DatePartEnum(val value: String) {
    Week("week"),
    Year("year")
}

class DatePart(private val expr: Column<DateTime?>, private val part: DatePartEnum) :
    Expression<Double?>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("EXTRACT(", part.value, " FROM ", expr, ")")
    }
}

class MinDate(private val expr: Expression<DateTime?>) : Expression<Timestamp?>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("min(", expr, ")")
    }
}

class MaxDate(private val expr: Expression<DateTime?>) : Expression<Timestamp?>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("max(", expr, ")")
    }
}

class Sum(private val expr: Expression<Int>) : Expression<BigDecimal?>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("sum(", expr, ")")
    }
}

class RowNumber(private val expr: Expression<DateTime?>) : Expression<Int?>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("ROW_NUMBER() OVER (ORDER BY ", expr, ")")
    }
}

class YearWeekMinus<T>(
    private val expr1: Expression<DateTime?>,
    private val expr2: Expression<T>
) :
    Expression<Double?>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("EXTRACT(year FROM $expr1)* 100+EXTRACT(week FROM $expr1) - $expr2")
    }
}

class Plus<A, B>(
    private val expr1: Expression<A>,
    private val expr2: Expression<B>
) :
    Expression<A>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append(expr1, "+", expr2)
    }
}

class Times<T>(private val expr1: Expression<T>, private val times: Int) :
    Expression<T>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append(expr1, "* $times")
    }
}