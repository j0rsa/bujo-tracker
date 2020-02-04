package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.TransactionManager.currentTransaction
import org.joda.time.DateTime
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.full.primaryConstructor

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

fun <T : Any> KClass<T>.fromRow() = { res: ResultSet ->
    val kClass = this
    val ctor = kClass.primaryConstructor!!
    val properties = ctor.parameters
    val values = properties.map { res.extractValue(it.type.classifier!!, it.name!!) }
    ctor.call(*values.toTypedArray())
}

fun <T : Any> KClass<T>.fromValue(name: String): (ResultSet) -> T = { res: ResultSet ->
    res.extractValue(this, name) as T
}

private fun ResultSet.extractValue(property: KClassifier, name: String) = when (property) {
    DateTime::class -> DateTime(getTimestamp(name))
    BigDecimal::class -> getBigDecimal(name)
    Boolean::class -> getBoolean(name)
    Double::class -> getDouble(name)
    Float::class -> getFloat(name)
    Int::class -> getInt(name)
    Short::class -> getShort(name)
    Long::class -> getLong(name)
    String::class -> getString(name)
    UUID::class -> getObject(name, UUID::class.java)
    else -> getObject(name)
}