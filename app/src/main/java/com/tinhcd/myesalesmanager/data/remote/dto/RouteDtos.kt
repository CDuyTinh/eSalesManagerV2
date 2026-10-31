package com.tinhcd.myesalesmanager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SalesRouteDto(
    val id: String,
    val code: String,
    val name: String,
    @SerialName("is_active") val isActive: Boolean = true,
    val salesperson: StaffBriefDto? = null,
    @SerialName("customer_count") val customerCount: Int = 0,
    @SerialName("per_weekday") val perWeekday: List<Int> = emptyList(),
)

@Serializable
data class RouteOutletDto(
    @SerialName("customer_id") val customerId: String,
    val code: String,
    val name: String,
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerialName("visit_order") val visitOrder: Int? = null,
    @SerialName("visit_weekdays") val weekdays: List<Int> = emptyList(),
    @SerialName("visit_weeks") val weeks: List<Int> = emptyList(),
    @SerialName("last_visit") val lastVisit: String? = null,
    @SerialName("last_order") val lastOrder: String? = null,
)

@Serializable
data class RouteHistoryDto(val at: String, val text: String, val actor: String? = null)

@Serializable
data class RouteDetailDto(
    val id: String,
    val code: String,
    val name: String,
    val salesperson: StaffBriefDto? = null,
    val customers: List<RouteOutletDto> = emptyList(),
    val history: List<RouteHistoryDto> = emptyList(),
)

@Serializable
data class RouteCandidateDto(
    @SerialName("customer_id") val customerId: String,
    val code: String,
    val name: String,
    val address: String? = null,
    @SerialName("on_routes") val onRoutes: String? = null,
)

@Serializable
data class RouteActionDto(
    val action: String,
    @SerialName("route_id") val routeId: String,
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("salesperson_id") val salespersonId: String? = null,
    val weekdays: List<Int>? = null,
    val weeks: List<Int>? = null,
    val note: String? = null,
    @SerialName("customer_ids") val customerIds: List<String>? = null,
)
