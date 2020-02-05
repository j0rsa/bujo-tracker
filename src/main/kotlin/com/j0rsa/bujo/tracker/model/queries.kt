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

inline fun <reified T : Any> ResultSet.toEntity(): T = this.toDataClass(T::class)

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

sealed class Result<T> {
    data class BooleanParam(val name: String) : Result<Boolean>()
    data class IntParam(val name: String) : Result<Int>()
    data class StringParam(val name: String) : Result<String>()
    data class UUIDParam(val name: String) : Result<UUID>()
    data class DoubleParam(val name: String) : Result<Double>()
    data class FloatParam(val name: String) : Result<Float>()
    data class LongParam(val name: String) : Result<Long>()
    data class ShortParam(val name: String) : Result<Short>()
    data class BigDecimalParam(val name: String) : Result<BigDecimal>()
    data class DateTimeParam(val name: String) : Result<DateTime>()
}

fun <T : Any> ResultSet.toDataClass(kClass: KClass<T>): T {
    val ctor = kClass.primaryConstructor!!
    val properties = ctor.parameters
    val values = properties.map { this.extractValue(it.type.jvmErasure, it.name!!) }
    return ctor.call(*values.toTypedArray())
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> ResultSet.toValue(kClass: KClass<T>, name: String): T = this.extractValue(kClass, name) as T

@Suppress("IMPLICIT_CAST_TO_ANY")
inline fun <reified T> Result<T>.get() = { res: ResultSet ->
    this.get(res)
}

@Suppress("IMPLICIT_CAST_TO_ANY")
inline fun <reified T> Result<T>.get(res: ResultSet) =
    when (this) {
        is Result.BooleanParam -> res.getBoolean(name)
        is Result.IntParam -> res.getInt(name)
        is Result.StringParam -> res.getString(name)
        is Result.UUIDParam -> res.getObject(name, UUID::class.java)
        is Result.DoubleParam -> res.getDouble(name)
        is Result.FloatParam -> res.getFloat(name)
        is Result.LongParam -> res.getLong(name)
        is Result.ShortParam -> res.getShort(name)
        is Result.BigDecimalParam -> res.getBigDecimal(name)
        is Result.DateTimeParam -> DateTime(res.getTimestamp(name))
    } as T

@Suppress("IMPLICIT_CAST_TO_ANY")
private fun <T : Any> ResultSet.extractValue(property: KClass<T>, name: String) = when (property) {
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