package com.j0rsa.bujo.tracker

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

object Serializer {
	val gson: Gson = GsonBuilder()
		.registerTypeAdapter(DateTime::class.java, DateTimeTypeAdapter().nullSafe())
		.create()

	inline fun <reified T> toJson(o: T): String = gson.toJson(o, T::class.java)
	inline fun <reified T> fromJson(s: String): T = gson.fromJson(s, T::class.java)
}

class DateTimeTypeAdapter : TypeAdapter<DateTime>() {

	override fun write(out: JsonWriter, value: DateTime) {
		out.value(ISODateTimeFormat.dateTime().print(value))
	}

	override fun read(input: JsonReader): DateTime =
		ISODateTimeFormat.dateTimeParser().withOffsetParsed().parseDateTime(input.nextString())
}