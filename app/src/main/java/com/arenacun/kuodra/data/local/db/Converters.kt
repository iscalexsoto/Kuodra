package com.arenacun.kuodra.data.local.db

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

/** Convertidores de tipos no primitivos para Room. */
class Converters {

    @TypeConverter
    fun dateToEpochDay(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun epochDayToDate(value: Long): LocalDate = LocalDate.ofEpochDay(value)

    @TypeConverter
    fun stringListToJson(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else Json.decodeFromString(value)
}
