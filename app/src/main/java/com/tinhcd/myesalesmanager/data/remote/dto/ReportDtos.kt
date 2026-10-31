package com.tinhcd.myesalesmanager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivitySummaryDto(
    val planned: Int = 0,
    val visited: Int = 0,
    val unplanned: Int = 0,
    val strike: Int = 0,
    @SerialName("non_strike") val nonStrike: Int = 0,
    val closed: Int = 0,
    @SerialName("order_amount") val orderAmount: Long = 0,
)

@Serializable
data class ActivityMemberDto(
    val code: String,
    val name: String,
    val planned: Int = 0,
    val visited: Int = 0,
    val strike: Int = 0,
    @SerialName("order_amount") val orderAmount: Long = 0,
    @SerialName("first_check_in") val firstCheckIn: String? = null,
    @SerialName("last_check_out") val lastCheckOut: String? = null,
)

@Serializable
data class ActivityRowDto(
    val salesperson: StaffBriefDto? = null,
    @SerialName("customer_code") val customerCode: String = "",
    @SerialName("customer_name") val customerName: String = "",
    val planned: Boolean = false,
    val status: String = "",
    @SerialName("check_in_at") val checkInAt: String? = null,
    @SerialName("check_out_at") val checkOutAt: String? = null,
    val minutes: Int? = null,
    @SerialName("order_amount") val orderAmount: Long = 0,
)

@Serializable
data class ActivitiesReportDto(
    val summary: ActivitySummaryDto = ActivitySummaryDto(),
    val members: List<ActivityMemberDto> = emptyList(),
    val rows: List<ActivityRowDto> = emptyList(),
)

@Serializable
data class SalesMemberDto(val code: String, val name: String, val orders: Int = 0, val revenue: Long = 0, val target: Long? = null)

@Serializable
data class SalesCustomerDto(
    @SerialName("customer_code") val code: String,
    @SerialName("customer_name") val name: String,
    val orders: Int = 0,
    val revenue: Long = 0,
)

@Serializable
data class SalesProductDto(
    @SerialName("product_code") val code: String,
    @SerialName("product_name") val name: String,
    @SerialName("base_uom") val baseUom: String = "",
    @SerialName("base_qty") val baseQty: Long = 0,
    val revenue: Long = 0,
)

@Serializable
data class SalesReportDto(
    val revenue: Long = 0,
    @SerialName("order_count") val orderCount: Int = 0,
    val target: Long? = null,
    val members: List<SalesMemberDto> = emptyList(),
    val customers: List<SalesCustomerDto> = emptyList(),
    val products: List<SalesProductDto> = emptyList(),
)

@Serializable
data class OrderRowDto(
    @SerialName("order_no") val orderNo: String,
    @SerialName("order_date") val orderDate: String,
    val salesperson: StaffBriefDto? = null,
    @SerialName("customer_code") val customerCode: String = "",
    @SerialName("customer_name") val customerName: String = "",
    val status: String = "",
    @SerialName("line_count") val lineCount: Int = 0,
    @SerialName("total_amount") val totalAmount: Long = 0,
)

@Serializable
data class OrdersReportDto(
    val total: Int = 0,
    @SerialName("total_amount") val totalAmount: Long = 0,
    val counts: Map<String, Int> = emptyMap(),
    val items: List<OrderRowDto> = emptyList(),
)

@Serializable
data class SkuRowDto(
    @SerialName("product_code") val code: String,
    @SerialName("product_name") val name: String,
    @SerialName("stock_uom") val uom: String = "",
    @SerialName("qty_confirmed") val qtyConfirmed: Int = 0,
    @SerialName("qty_pending") val qtyPending: Int = 0,
    @SerialName("qty_total") val qtyTotal: Int = 0,
    @SerialName("amount_total") val amountTotal: Long = 0,
)

@Serializable
data class SkuReportDto(val items: List<SkuRowDto> = emptyList())

@Serializable
data class TradeRowDto(
    val kind: String,
    val salesperson: StaffBriefDto? = null,
    @SerialName("registered_at") val registeredAt: String = "",
    val status: String = "",
    val portion: Int = 1,
    @SerialName("customer_code") val customerCode: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("program_code") val programCode: String = "",
    @SerialName("program_name") val programName: String = "",
    @SerialName("level_name") val levelName: String? = null,
)

@Serializable
data class TradeReportDto(
    @SerialName("display_count") val displayCount: Int = 0,
    @SerialName("loyalty_count") val loyaltyCount: Int = 0,
    @SerialName("customer_count") val customerCount: Int = 0,
    val items: List<TradeRowDto> = emptyList(),
)

@Serializable
data class WorkRowDto(
    val salesperson: StaffBriefDto? = null,
    val content: String = "",
    @SerialName("category_name") val categoryName: String = "",
    val status: String = "",
    @SerialName("from_at") val fromAt: String,
    @SerialName("to_at") val toAt: String,
    @SerialName("object_count") val objectCount: Int = 0,
    val outcome: String? = null,
)

@Serializable
data class WorkReportDto(
    val completed: Int = 0,
    val cancelled: Int = 0,
    val total: Int = 0,
    val items: List<WorkRowDto> = emptyList(),
)

@Serializable
data class TrainingSummaryDto(
    val plans: Int = 0,
    val trained: Int = 0,
    val untrained: Int = 0,
    val outlets: Int = 0,
    val score: Int? = null,
)

@Serializable
data class TrainingMemberDto(
    val code: String,
    val name: String,
    val plans: Int = 0,
    val trained: Int = 0,
    val untrained: Int = 0,
    val score: Int? = null,
)

@Serializable
data class TrainingRowDto(
    @SerialName("plan_id") val planId: String,
    @SerialName("training_id") val trainingId: String? = null,
    val content: String = "",
    @SerialName("from_at") val fromAt: String,
    @SerialName("to_at") val toAt: String,
    val rep: StaffBriefDto,
    val status: String = "not_started",
    @SerialName("current_step") val currentStep: Int? = null,
    val result: String? = null,
    val outlets: Int = 0,
    val score: Int? = null,
)

@Serializable
data class TrainingReportDto(
    val summary: TrainingSummaryDto = TrainingSummaryDto(),
    val members: List<TrainingMemberDto> = emptyList(),
    val items: List<TrainingRowDto> = emptyList(),
)

@Serializable
data class LastActivityDto(val kind: String, val at: String, val detail: String? = null)

@Serializable
data class InVisitDto(val code: String, val name: String, val since: String? = null)

@Serializable
data class TrackingMemberDto(
    val code: String,
    val name: String,
    @SerialName("clock_in") val clockIn: String? = null,
    @SerialName("clock_out") val clockOut: String? = null,
    val planned: Int = 0,
    val visited: Int = 0,
    @SerialName("in_visit") val inVisit: InVisitDto? = null,
    val orders: Int = 0,
    val revenue: Long = 0,
    val last: LastActivityDto? = null,
    @SerialName("battery_pct") val batteryPct: Int? = null,
)

@Serializable
data class TrackingOrderDto(
    @SerialName("order_no") val orderNo: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("total_amount") val totalAmount: Long = 0,
    val salesperson: StaffBriefDto,
    val customer: String = "",
    val status: String = "new",
)

@Serializable
data class TrackingReportDto(
    val members: List<TrackingMemberDto> = emptyList(),
    val orders: List<TrackingOrderDto> = emptyList(),
)
