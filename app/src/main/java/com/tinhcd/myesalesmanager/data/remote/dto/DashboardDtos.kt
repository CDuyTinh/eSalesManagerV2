package com.tinhcd.myesalesmanager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeamDashboardDto(
    val date: String,
    @SerialName("team_size") val teamSize: Int = 0,
    val today: TeamTodayDto,
    val month: TeamMonthDto,
    val members: List<MemberFiguresDto> = emptyList(),
    val charts: ChartsDto = ChartsDto(),
)

@Serializable
data class TeamTodayDto(
    val revenue: Long = 0,
    @SerialName("order_count") val orderCount: Int = 0,
    @SerialName("visit_done") val visitDone: Int = 0,
    @SerialName("visit_planned") val visitPlanned: Int = 0,
    @SerialName("sku_per_order") val skuPerOrder: Double = 0.0,
    @SerialName("active_reps") val activeReps: Int = 0,
)

@Serializable
data class TeamMonthDto(
    val revenue: Long = 0,
    @SerialName("revenue_target") val revenueTarget: Long? = null,
    @SerialName("order_count") val orderCount: Int = 0,
    @SerialName("order_target") val orderTarget: Int? = null,
    @SerialName("visit_done") val visitDone: Int = 0,
    @SerialName("visit_planned") val visitPlanned: Int = 0,
    @SerialName("sku_per_order") val skuPerOrder: Double = 0.0,
    val vpo: Long = 0,
    @SerialName("new_customers") val newCustomers: Int = 0,
)

@Serializable
data class MemberFiguresDto(
    val id: String,
    val code: String,
    val name: String,
    @SerialName("today_revenue") val todayRevenue: Long = 0,
    @SerialName("today_orders") val todayOrders: Int = 0,
    @SerialName("today_visits") val todayVisits: Int = 0,
    @SerialName("today_planned") val todayPlanned: Int = 0,
    @SerialName("month_revenue") val monthRevenue: Long = 0,
    @SerialName("month_orders") val monthOrders: Int = 0,
    @SerialName("revenue_target") val revenueTarget: Long? = null,
)

@Serializable
data class SalesPointDto(val title: String, val actual: Long = 0)

@Serializable
data class ChartsDto(
    @SerialName("this_week") val thisWeek: List<SalesPointDto>? = null,
    @SerialName("last_week") val lastWeek: List<SalesPointDto>? = null,
    @SerialName("this_month") val thisMonth: List<SalesPointDto>? = null,
)
