package com.example.oop_warehouse_location.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Centralised date/time helpers — eliminates duplicated SimpleDateFormat
 * instances scattered across adapters and fragments.
 */
object DateUtils {

    private val DEFAULT_FORMAT = SimpleDateFormat("MMM d, yyyy — h:mm a", Locale.getDefault())
    private val LOG_FORMAT = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())

    /** Formats a millis timestamp with the default style used by parcels. */
    fun formatTimestamp(timestamp: Long): String =
        if (timestamp > 0) DEFAULT_FORMAT.format(Date(timestamp)) else ""

    /** Formats a millis timestamp with the dot-separated style used by activity logs. */
    fun formatLogTimestamp(timestamp: Long): String =
        if (timestamp > 0) LOG_FORMAT.format(Date(timestamp)) else ""

    /** Returns true when [timestamp] falls on the current calendar day. */
    fun isToday(timestamp: Long): Boolean {
        if (timestamp <= 0L) return false
        val created = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()
        return created.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            created.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    }
}

