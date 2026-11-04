package com.tinhcd.myesalesmanager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApprovalCountsDto(
    val leave: Int = 0,
    val customer: Int = 0,
    val display: Int = 0,
    val loyalty: Int = 0,
    val posm: Int = 0,
    @SerialName("work_plan") val workPlan: Int = 0,
)

@Serializable
data class StaffBriefDto(val id: String, val code: String, val name: String)

@Serializable
data class ApprovalPageDto<T>(
    val counts: Map<String, Int> = emptyMap(),
    val items: List<T> = emptyList(),
)

@Serializable
data class LeaveApprovalDto(
    val id: String,
    val salesperson: StaffBriefDto? = null,
    @SerialName("type_name") val typeName: String = "",
    @SerialName("is_paid") val isPaid: Boolean = true,
    @SerialName("from_date") val fromDate: String,
    @SerialName("to_date") val toDate: String,
    val days: Int = 1,
    val reason: String = "",
    val status: String,
    @SerialName("decision_note") val decisionNote: String? = null,
    @SerialName("decided_at") val decidedAt: String? = null,
)

@Serializable
data class CustomerApprovalDto(
    val id: String,
    val code: String,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val status: String,
    val note: String? = null,
    @SerialName("decision_note") val decisionNote: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("photo_count") val photoCount: Int = 0,
    val salesperson: StaffBriefDto? = null,
)

@Serializable
data class CustomerApprovalDetailDto(
    val id: String,
    val code: String,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val status: String,
    val note: String? = null,
    @SerialName("decision_note") val decisionNote: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    val salesperson: StaffBriefDto? = null,
    @SerialName("contact_name") val contactName: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val ward: String? = null,
    val district: String? = null,
    val province: String? = null,
    @SerialName("class_name") val className: String? = null,
    @SerialName("channel_name") val channelName: String? = null,
    @SerialName("shop_type_name") val shopTypeName: String? = null,
    val photos: List<ApprovalPhotoDto> = emptyList(),
    val routes: List<RouteOptionDto> = emptyList(),
)

@Serializable
data class ApprovalPhotoDto(
    @SerialName("type_name") val typeName: String = "",
    val url: String? = null,
)

@Serializable
data class RouteOptionDto(
    val id: String,
    val code: String,
    val name: String,
    @SerialName("already_on") val alreadyOn: Boolean = false,
)

@Serializable
data class TradeCustomerDto(
    val id: String,
    val code: String,
    val name: String,
    val address: String? = null,
)

@Serializable
data class TradeApprovalDto(
    val kind: String,
    val id: String,
    val status: String,
    @SerialName("registered_at") val registeredAt: String = "",
    @SerialName("program_code") val programCode: String = "",
    @SerialName("program_name") val programName: String = "",
    @SerialName("detail_name") val detailName: String = "",
    val qty: Int = 1,
    @SerialName("approved_qty") val approvedQty: Int? = null,
    val reason: String? = null,
    @SerialName("decision_note") val decisionNote: String? = null,
    val customer: TradeCustomerDto,
    val salesperson: StaffBriefDto? = null,
)

@Serializable
data class DecisionRequestDto(
    val kind: String,
    val id: String,
    val approve: Boolean,
    val note: String? = null,
    val routeId: String? = null,
    val approvedQty: Int? = null,
)

@Serializable
data class DecisionResultDto(
    val id: String,
    val status: String,
)

@Serializable
data class DisplayResultDto(
    @SerialName("registration_id") val registrationId: String,
    @SerialName("program_code") val programCode: String = "",
    @SerialName("program_name") val programName: String = "",
    @SerialName("program_to") val programTo: String? = null,
    @SerialName("level_name") val levelName: String = "",
    @SerialName("reward_type") val rewardType: String = "amount",
    @SerialName("reward_amount") val rewardAmount: Long = 0,
    @SerialName("reward_percent") val rewardPercent: Double = 0.0,
    @SerialName("reward_qty") val rewardQty: Int = 0,
    @SerialName("reward_product") val rewardProduct: String? = null,
    @SerialName("customer_code") val customerCode: String = "",
    @SerialName("customer_name") val customerName: String = "",
    val salesperson: StaffBriefDto? = null,
    val audits: Int = 0,
    val achieved: Int = 0,
    @SerialName("last_audit") val lastAudit: String? = null,
    /** Null while undecided; confirmed or rejected after. */
    val decision: String? = null,
    @SerialName("decision_note") val decisionNote: String? = null,
)
