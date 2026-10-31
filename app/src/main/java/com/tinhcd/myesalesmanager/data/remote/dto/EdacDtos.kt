package com.tinhcd.myesalesmanager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EdacTrainingBriefDto(
    val id: String,
    @SerialName("current_step") val currentStep: Int = 0,
    val status: String = "in_progress",
)

@Serializable
data class EdacSessionDto(
    @SerialName("plan_id") val planId: String,
    val content: String = "",
    @SerialName("from_at") val fromAt: String,
    @SerialName("to_at") val toAt: String,
    val rep: StaffBriefDto,
    val training: EdacTrainingBriefDto? = null,
)

@Serializable
data class EdacTrainingDto(
    val id: String,
    @SerialName("training_date") val trainingDate: String,
    @SerialName("current_step") val currentStep: Int = 0,
    val status: String = "in_progress",
    @SerialName("check_in_at") val checkInAt: String? = null,
    @SerialName("check_out_at") val checkOutAt: String? = null,
    val result: String? = null,
    @SerialName("supervisor_photo_url") val supervisorPhotoUrl: String? = null,
    @SerialName("salesperson_photo_url") val salespersonPhotoUrl: String? = null,
    val rep: StaffBriefDto,
    val today: TeamTodayDto? = null,
    val month: TeamMonthDto? = null,
    @SerialName("outlets_done") val outletsDone: Map<String, Int> = emptyMap(),
    val answers: List<EdacResultLineDto> = emptyList(),
    val notes: List<EdacResultLineDto> = emptyList(),
)

@Serializable
data class EdacOutletDto(
    @SerialName("customer_id") val customerId: String,
    val code: String,
    val name: String,
    val address: String? = null,
    val due: Boolean = true,
    @SerialName("done_steps") val doneSteps: List<Int> = emptyList(),
)

@Serializable
data class EdacOptionDto(val value: Int, val label: String)

@Serializable
data class EdacAnswerDto(
    val value: Int? = null,
    val yes: Boolean? = null,
    val text: String? = null,
    val note: String? = null,
)

@Serializable
data class EdacCriterionDto(
    val id: String,
    val content: String,
    val kind: String,
    val kpi: String? = null,
    val required: Boolean = true,
    val options: List<EdacOptionDto> = emptyList(),
    val answer: EdacAnswerDto? = null,
)

@Serializable
data class EdacCustomerDto(val id: String, val code: String, val name: String, val address: String? = null)

@Serializable
data class EdacObservedDto(val content: String, val yes: Int = 0, val no: Int = 0)

/** Either an outlet list (per-outlet step, no outlet chosen) or a form. */
@Serializable
data class EdacStepDto(
    val step: Int,
    val outlets: List<EdacOutletDto>? = null,
    val customer: EdacCustomerDto? = null,
    val note: String? = null,
    val criteria: List<EdacCriterionDto> = emptyList(),
    val observed: List<EdacObservedDto>? = null,
)

@Serializable
data class EdacResultLineDto(
    val step: Int,
    val customer: String? = null,
    val content: String? = null,
    val answer: String? = null,
    val note: String? = null,
)

@Serializable
data class EdacHistoryDto(
    val id: String,
    @SerialName("training_date") val trainingDate: String,
    @SerialName("current_step") val currentStep: Int = 0,
    val status: String = "in_progress",
    val result: String? = null,
    val rep: StaffBriefDto,
    val outlets: Int = 0,
)

@Serializable
data class EdacAnswerInDto(
    @SerialName("criterion_id") val criterionId: String,
    val value: Int? = null,
    val yes: Boolean? = null,
    val text: String? = null,
    val note: String? = null,
)

@Serializable
data class EdacActionDto(
    val action: String,
    val id: String? = null,
    @SerialName("rep_id") val repId: String? = null,
    @SerialName("plan_id") val planId: String? = null,
    val step: Int? = null,
    @SerialName("customer_id") val customerId: String? = null,
    val answers: List<EdacAnswerInDto>? = null,
    val note: String? = null,
    val finish: Boolean? = null,
    val result: String? = null,
    @SerialName("supervisor_photo") val supervisorPhoto: String? = null,
    @SerialName("salesperson_photo") val salespersonPhoto: String? = null,
    val criterion: EdacCriterionInDto? = null,
)

@Serializable
data class EdacStartedDto(val id: String)

@Serializable
data class EdacSavedDto(@SerialName("current_step") val currentStep: Int = 0)

@Serializable
data class EdacCriterionAdminDto(
    val id: String,
    val step: Int,
    val code: String = "",
    val content: String,
    val kind: String,
    val kpi: String? = null,
    val required: Boolean = true,
    val active: Boolean = true,
    val answered: Boolean = false,
    val options: List<EdacOptionDto> = emptyList(),
)

@Serializable
data class EdacCriteriaDto(
    @SerialName("can_edit") val canEdit: Boolean = false,
    val criteria: List<EdacCriterionAdminDto> = emptyList(),
)

@Serializable
data class EdacCriterionInDto(
    val id: String? = null,
    val step: Int,
    val content: String,
    val kind: String,
    val kpi: String? = null,
    val required: Boolean,
    val active: Boolean,
    val options: List<EdacOptionDto> = emptyList(),
)
