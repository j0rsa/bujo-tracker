package com.j0rsa.bujo.tracker.model

import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime
import java.math.BigDecimal
import java.sql.Timestamp

enum class DatePartEnum(val value: String) {
    Days("days"),
    Week("week"),
}

class DatePart(private val expr: Expression<DateTime?>, private val part: DatePartEnum) :
    Expression<Double?>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("EXTRACT(", part.value, " FROM ", expr, ")")
    }
}

class MinDate(private val expr: Expression<DateTime?>) : Expression<Timestamp>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("min(", expr, ")")
    }
}

class MaxDate(private val expr: Expression<DateTime?>) : Expression<Timestamp>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("max(", expr, ")")
    }
}

class Sum(private val expr: Expression<Int>) : Expression<BigDecimal>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("sum(", expr, ")")
    }
}

class RowNumber(private val expr: Expression<DateTime?>) : Expression<Double?>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("ROW_NUMBER() OVER (ORDER BY ", expr, ")")
    }
}

class DateMinus<T>(
    private val expr1: Expression<DateTime?>,
    private val expr2: Expression<T>
) :
    Expression<Double?>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("DATE(", expr1, ") - INTERVAL '1' DAY * ", expr2)
    }
}

class AsDate(private val expr1: Expression<DateTime?>) : Expression<DateTime?>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("DATE(", expr1, ")")
    }
}

class Divide<A>(
    private val expr1: Expression<A>,
    private val expr2: Int
) :
    Expression<A>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("(", expr1, "/$expr2)")
    }

}

class Minus<T>(private val expr1: Expression<T>, private val expr2: Expression<T>) :
    Expression<T>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("(", expr1, ") - (", expr2, ")")
    }
}

class DateTrunc(private val expr: Expression<DateTime?>, private val part: DatePartEnum) :
    Expression<DateTime?>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("date_trunc('", part.value, "', ", expr, ")")
    }
}