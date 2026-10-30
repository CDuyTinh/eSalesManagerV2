package com.tinhcd.myesalesmanager.domain.model

/** Weekday numbers as the database keeps them: ISO, 1 = Monday … 7 = Sunday. */
val WEEKDAY_LABELS = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

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
