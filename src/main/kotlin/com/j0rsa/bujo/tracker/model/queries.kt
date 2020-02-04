package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.TransactionManager.currentTransaction
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*
import kotlin.collections.ArrayList

fun <T> String.exec(vararg params: Param, transform: (ResultSet) -> T): ArrayList<T> {
    val statement = currentTransaction().connection.prepareStatement(this)
    params.forEachIndexed { index, param ->
        statement.setParam(param, index + 1)
    }
    val result = arrayListOf<T>()
    val resultSet = statement.executeQuery()
    resultSet?.use {
        while (it.next()) {
            result += transform(it)
        }
    }
    return result
}

private fun PreparedStatement.setParam(param: Param, index: Int) {
    when (param) {
        is Param.BooleanParam -> setBoolean(index, param.value)
        is Param.IntParam -> setInt(index, param.value)
        is Param.StringParam -> setString(index, param.value)
        is Param.UUIDParam -> setObject(index, param.value)
        is Param.DoubleParam -> setDouble(index, param.value)
        is Param.FloatParam -> setFloat(index, param.value)
        is Param.LongParam -> setLong(index, param.value)
        is Param.ShortParam -> setShort(index, param.value)
    }
}

sealed class Param {
    data class BooleanParam(val value: Boolean) : Param()
    data class IntParam(val value: Int) : Param()
    data class StringParam(val value: String) : Param()
    data class UUIDParam(val value: UUID) : Param()
    data class DoubleParam(val value: Double) : Param()
    data class FloatParam(val value: Float) : Param()
    data class LongParam(val value: Long) : Param()
    data class ShortParam(val value: Short) : Param()
}

fun param(value: Int): Param.IntParam = Param.IntParam(value)
fun param(id: HabitId): Param.UUIDParam = Param.UUIDParam(id.value)