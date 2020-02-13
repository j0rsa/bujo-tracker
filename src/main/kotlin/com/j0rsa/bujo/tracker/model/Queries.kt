package com.j0rsa.bujo.tracker.model

import com.j0rsa.bujo.tracker.TransactionManager.currentTransaction
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.joda.time.DateTime
import org.postgresql.jdbc.PgArray
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
	this.map { it.toEntity<T>() }

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

sealed class ResultColumn<T> {
	data class BooleanColumn(val name: String) : ResultColumn<Boolean>()
	data class IntColumn(val name: String) : ResultColumn<Int>()
	data class StringColumn(val name: String) : ResultColumn<String>()
	data class UUIDColumn(val name: String) : ResultColumn<UUID>()
	data class DoubleColumn(val name: String) : ResultColumn<Double>()
	data class FloatColumn(val name: String) : ResultColumn<Float>()
	data class LongColumn(val name: String) : ResultColumn<Long>()
	data class ShortColumn(val name: String) : ResultColumn<Short>()
	data class BigDecimalColumn(val name: String) : ResultColumn<BigDecimal>()
	data class DateTimeColumn(val name: String) : ResultColumn<DateTime>()
}

fun <T : Any> ResultSet.toDataClass(kClass: KClass<T>): T {
	val ctor = kClass.primaryConstructor!!
	val properties = ctor.parameters
	val values = properties.map { this.extractValue(it.type.jvmErasure, it.name!!) }
	return ctor.call(*values.toTypedArray())
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> ResultSet.toValue(kClass: KClass<T>, name: String): T = this.extractValue(kClass, name) as T

inline fun <reified T> ResultSet?.get(column: ResultColumn<T>) = this.map { column from it }

@Suppress("IMPLICIT_CAST_TO_ANY")
inline infix fun <reified T> ResultColumn<T>.from(res: ResultSet) =
	when (this) {
		is ResultColumn.BooleanColumn -> res.getBoolean(name)
		is ResultColumn.IntColumn -> res.getInt(name)
		is ResultColumn.StringColumn -> res.getString(name)
		is ResultColumn.UUIDColumn -> res.getObject(name, UUID::class.java)
		is ResultColumn.DoubleColumn -> res.getDouble(name)
		is ResultColumn.FloatColumn -> res.getFloat(name)
		is ResultColumn.LongColumn -> res.getLong(name)
		is ResultColumn.ShortColumn -> res.getShort(name)
		is ResultColumn.BigDecimalColumn -> res.getBigDecimal(name)
		is ResultColumn.DateTimeColumn -> DateTime(res.getTimestamp(name))
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

fun Table.arrayOfString(name: String): Column<List<String>> =
	registerColumn(name, StringArrayColumnType())

internal class StringArrayColumnType : ColumnType() {
	override fun sqlType() = "VARCHAR[]"
	override fun valueFromDB(value: Any): List<String> = when (value) {
		is Iterable<*> -> value.map { it.toString() }
		is PgArray -> {
			val array = value.array
			if (array is Array<*>) {
				array.map {
					when (it) {
						is String -> it
						null -> error("Unexpected value of type String but value is $it")
						else -> error("Unexpected value of type String: $it of ${it::class.qualifiedName}")
					}
				}
			} else {
				throw Exception("Values returned from database if not of type kotlin Array<*>")
			}
		}
		else -> throw Exception("Values returned from database if not of type PgArray")
	}

	override fun valueToString(value: Any?): String = when (value) {
		null -> {
			if (!nullable) error("NULL in non-nullable column")
			"NULL"
		}

		is Iterable<*> -> {
			"'{${value.joinToString()}}'"
		}

		else -> {
			nonNullValueToString(value)
		}
	}

	override fun setParameter(stmt: PreparedStatement, index: Int, value: Any?) {
		if (value is List<*>) {
			stmt.setArray(index, stmt.connection.createArrayOf("varchar", value.toTypedArray()))
		} else {
			super.setParameter(stmt, index, value)
		}
	}
}