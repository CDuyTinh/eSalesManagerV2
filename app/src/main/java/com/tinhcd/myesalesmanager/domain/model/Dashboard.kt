package com.tinhcd.myesalesmanager.domain.model

import kotlin.math.roundToInt

/** One day on the sell-out line: a short axis label and what was sold. */
data class SalesPoint(val title: String, val actual: Long)

/** The three spans the chart can show, as the legacy screens had them. */
enum class ChartRange(val label: String) {
    THIS_WEEK("Tuần này"),
    LAST_WEEK("Tuần trước"),
    THIS_MONTH("Tháng này"),
}

/** The selected day, summed over the team (or one rep when filtered). */
data class TeamToday(
    val revenue: Long,
    val orderCount: Int,
    val visitDone: Int,
    val visitPlanned: Int,
    val skuPerOrder: Double,
    /** Reps who reached at least one shop that day. */
    val activeReps: Int,
)

/**
 * The month so far. Targets are null when head office set none — not zero,
 * which would read as "given nothing to do" and break every percentage.
 */
data class TeamMonth(
    val revenue: Long,
    val revenueTarget: Long?,
    val orderCount: Int,
    val orderTarget: Int?,
    /** Visits made up to the selected day, against stops planned up to it. */
    val visitDone: Int,
    val visitPlanned: Int,
    val skuPerOrder: Double,
    /** Value per outlet that ordered. Legacy VPO. */
    val vpo: Long,
    val newCustomers: Int,
)

/** One rep's line in the ranking. */
data class MemberFigures(
    val id: String,
    val code: String,
    val name: String,
    val todayRevenue: Long,
    val todayOrders: Int,
    val todayVisits: Int,
    val todayPlanned: Int,
    val monthRevenue: Long,
    val monthOrders: Int,
    val revenueTarget: Long?,
) {
    /** Month revenue against target, or null when there is no target. */
    val achievement: Int? get() = percent(monthRevenue, revenueTarget)
}

data class TeamDashboard(
    val date: String,
    val teamSize: Int,
    val today: TeamToday,
    val month: TeamMonth,
    val members: List<MemberFigures>,
    val charts: Map<ChartRange, List<SalesPoint>>,
)

/** Whole-number percentage, or null when there is nothing to divide by. */
fun percent(value: Long, of: Long?): Int? =
    if (of == null || of <= 0L) null else (value * 100.0 / of).roundToInt()

/** Each point's height as a share of the highest, for the chart. */
fun List<SalesPoint>.heights(): List<Float> {
    val peak = maxOfOrNull { it.actual } ?: 0L
    if (peak <= 0L) return List(size) { 0f }
    return map { (it.actual.toDouble() / peak.toDouble()).toFloat() }
}
