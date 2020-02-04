package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.TransactionManager.currentTransaction
import org.joda.time.DateTime
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

fun String.exec(vararg params: Param): ResultSet? =
    with(currentTransaction().connection.prepareStatement(this)) {
        params.forEachIndexed { index, param ->
            this.setParam(param, index + 1)
        }
        this.executeQuery()
    }

inline fun <reified T : Any> ResultSet?.toEntities(): ArrayList<T> =
    this.map { it.toDataClass(T::class) }

inline fun <reified T : Any> ResultSet?.getValue(name: String): ArrayList<T> =
    this.map { it.toValue(T::class, name) }

fun <T> ResultSet?.map(transformer: (ResultSet) -> T): ArrayList<T> {
    val result = arrayListOf<T>()
    this?.use {
        while (it.next()) {
            result += transformer(this)
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

fun <T : Any> ResultSet.toDataClass(kClass: KClass<T>): T {
    val ctor = kClass.primaryConstructor!!
    val properties = ctor.parameters
    val values = properties.map { this.extractValue(it.type.jvmErasure, it.name!!) }
    return ctor.call(*values.toTypedArray())
}

fun <T : Any> ResultSet.toValue(kClass: KClass<T>, name: String): T = this.extractValue(kClass, name) as T

private fun<T: Any> ResultSet.extractValue(property: KClass<T>, name: String) = when (property) {
    DateTime::class -> DateTime(getTimestamp(name))
    BigDecimal::class -> getBigDecimal(name)
    Boolean::class -> getBoolean(name)
    Double::class -> getDouble(name)
    Float::class -> getFloat(name)
    Int::class -> getInt(name)
    Short::class -> getShort(name)
    Long::class -> getLong(name)
    String::class -> getString(name)
    else -> getObject(name, property.java)
}