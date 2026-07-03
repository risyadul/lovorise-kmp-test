package com.lovorise.discover.core.util

import kotlin.math.roundToInt

/** 950 -> "950", 1_200 -> "1.2K", 4_050_000 -> "4.1M". */
fun formatCount(count: Int): String = when {
    count < 1_000 -> count.toString()
    count < 1_000_000 -> trimTrailingZero(count / 1_000.0) + "K"
    else -> trimTrailingZero(count / 1_000_000.0) + "M"
}

/** One-decimal rounding without JVM-only formatting so it runs on every target. */
private fun trimTrailingZero(value: Double): String {
    val tenths = (value * 10).roundToInt()
    val whole = tenths / 10
    val fraction = tenths % 10
    return if (fraction == 0) whole.toString() else "$whole.$fraction"
}

/** Minutes-ago -> compact relative label matching the app's casual tone. */
fun timeAgo(minutes: Int): String = when {
    minutes < 1 -> "Just now"
    minutes < 60 -> "${minutes}m ago"
    minutes < 60 * 24 -> "${minutes / 60}h ago"
    else -> "${minutes / (60 * 24)}d ago"
}

/** 3.2 -> "3.2 km away", 0.4 -> "400 m away". */
fun formatDistance(km: Double): String = when {
    km < 1.0 -> "${(km * 1000).toInt()} m away"
    else -> "${trimTrailingZero(km)} km away"
}

/** "Salsabila Rahma" -> "SR"; tolerant of extra/leading whitespace. */
fun initialsOf(name: String): String =
    name.split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
