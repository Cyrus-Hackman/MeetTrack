package com.meettrack.app.data.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val reminderIntervalsType = object : TypeToken<List<Int>>() {}.type

fun parseReminderIntervals(json: String): List<Int> =
    try {
        Gson().fromJson<List<Int>>(json, reminderIntervalsType) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
