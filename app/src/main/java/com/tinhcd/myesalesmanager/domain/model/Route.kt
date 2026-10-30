package com.tinhcd.myesalesmanager.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Weekday numbers as the database keeps them: ISO, 1 = Monday … 7 = Sunday. */
val WEEKDAY_LABELS = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

/**
 * Week 1..4 of the MCL cycle (legacy PPC_WeekofVisitMCL), counted in whole weeks
 * from Monday 2024-01-01. Mirrors the database's mcl_week(), which decides what
 * the rep is shown; here it only labels the picker.
 */
fun mclWeek(date: LocalDate): Int {
    val monday = date.minusDays(date.dayOfWeek.value - 1L)
    val weeks = Math.floorDiv(ChronoUnit.DAYS.between(LocalDate.of(2024, 1, 1), monday), 7L)
    return Math.floorMod(weeks, 4L).toInt() + 1
}

/** "T2, T4, T6" for {1,3,5}; "· Tuần 1, 3" appended when only some weeks. */
fun formatSchedule(weekdays: List<Int>, weeks: List<Int>): String {
    val days = weekdays.sorted().mapNotNull { WEEKDAY_LABELS.getOrNull(it - 1) }.joinToString(", ")
    return if (weeks.isEmpty()) days else "$days · Tuần ${weeks.sorted().joinToString(", ")}"
}

/** A route in the team's list, with how many outlets it calls on each weekday. */
data class SalesRoute(
    val id: String,
    val code: String,
    val name: String,
    val isActive: Boolean,
    val rep: StaffBrief?,
    val customerCount: Int,
    /** Seven counts, Monday first. */
    val perWeekday: List<Int>,
)

/** An outlet on the route and when it is called on (legacy CustomerMcp). */
data class RouteOutlet(
    val customerId: String,
    val code: String,
    val name: String,
    val address: String?,
    val visitOrder: Int?,
    val weekdays: List<Int>,
    /** Empty means every week. */
    val weeks: List<Int>,
    val lastVisit: String?,
    val lastOrder: String?,
)

data class RouteHistoryEntry(val at: String, val text: String, val actor: String?)

data class RouteDetail(
    val id: String,
    val code: String,
    val name: String,
    val rep: StaffBrief?,
    val outlets: List<RouteOutlet>,
    val history: List<RouteHistoryEntry>,
)

/** An approved outlet of the route's distributor not yet on it. */
data class RouteCandidate(
    val customerId: String,
    val code: String,
    val name: String,
    val address: String?,
    /** Codes of the other routes it is already on, comma separated. */
    val onRoutes: String?,
)
