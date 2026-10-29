package com.tinhcd.myesalesmanager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeamWorkPlanDto(
    val id: String,
    val salesperson: StaffBriefDto? = null,
    val author: StaffBriefDto? = null,
    @SerialName("assigned_by_me") val assignedByMe: Boolean = false,
    @SerialName("category_name") val categoryName: String = "",
    @SerialName("object_kind") val objectKind: String? = null,
    val content: String = "",
    val place: String? = null,
    @SerialName("from_at") val fromAt: String,
    @SerialName("to_at") val toAt: String,
    val status: String,
    val result: String? = null,
    val reason: String? = null,
    @SerialName("object_count") val objectCount: Int = 0,
)

@Serializable
data class WorkPlanPageDto(
    val counts: Map<String, Int> = emptyMap(),
    val items: List<TeamWorkPlanDto> = emptyList(),
)

@Serializable
data class WorkPlanObjectDto(
    val kind: String? = null,
    @SerialName("target_id") val targetId: String,
    val code: String? = null,
    val name: String = "",
    val address: String? = null,
)

@Serializable
data class WorkPlanHistoryDto(
    val status: String,
    val reason: String? = null,
    val actor: String? = null,
    val at: String = "",
)

@Serializable
data class WorkPlanPhotoDto(val url: String? = null)

@Serializable
data class WorkPlanDetailDto(
    val id: String,
    val salesperson: StaffBriefDto? = null,
    val author: StaffBriefDto? = null,
    @SerialName("assigned_by_me") val assignedByMe: Boolean = false,
    @SerialName("category_name") val categoryName: String = "",
    @SerialName("object_kind") val objectKind: String? = null,
    val content: String = "",
    val note: String? = null,
    val place: String? = null,
    @SerialName("from_at") val fromAt: String,
    @SerialName("to_at") val toAt: String,
    val status: String,
    val result: String? = null,
    val reason: String? = null,
    val objects: List<WorkPlanObjectDto> = emptyList(),
    val history: List<WorkPlanHistoryDto> = emptyList(),
    val photos: List<WorkPlanPhotoDto> = emptyList(),
)

@Serializable
data class WorkCategoryDto(
    val id: String,
    val code: String,
    val name: String,
    @SerialName("object_kind") val objectKind: String? = null,
    @SerialName("field_training") val fieldTraining: Boolean = false,
)

@Serializable
data class PlannableCustomerDto(
    val id: String,
    val code: String,
    val name: String,
    val address: String? = null,
)

@Serializable
data class AssignObjectDto(val id: String)

@Serializable
data class AssignPlanDto(
    @SerialName("salesperson_id") val salespersonId: String,
    @SerialName("category_id") val categoryId: String,
    val content: String,
    val place: String? = null,
    val note: String? = null,
    @SerialName("from_at") val fromAt: String,
    @SerialName("to_at") val toAt: String,
    val objects: List<AssignObjectDto> = emptyList(),
)

@Serializable
data class WorkPlanActionDto(
    val action: String,
    val id: String? = null,
    val approve: Boolean? = null,
    val reason: String? = null,
    val plan: AssignPlanDto? = null,
)

@Serializable
data class WorkPlanActionResultDto(
    val id: String,
    val status: String? = null,
)
