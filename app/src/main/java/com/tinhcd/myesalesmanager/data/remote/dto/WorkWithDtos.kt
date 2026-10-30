package com.tinhcd.myesalesmanager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkWithStopDto(
    @SerialName("customer_id") val customerId: String,
    val code: String,
    val name: String,
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val planned: Boolean = true,
    @SerialName("rep_status") val repStatus: String? = null,
    @SerialName("rep_check_in") val repCheckIn: String? = null,
    @SerialName("rep_order_amount") val repOrderAmount: Long = 0,
    @SerialName("my_visit_id") val myVisitId: String? = null,
    @SerialName("my_status") val myStatus: String? = null,
)

@Serializable
data class WorkWithRouteDto(
    val date: String,
    val rep: StaffBriefDto? = null,
    @SerialName("open_visit_id") val openVisitId: String? = null,
    val stops: List<WorkWithStopDto> = emptyList(),
)

@Serializable
data class WorkWithCustomerDto(
    val id: String,
    val code: String,
    val name: String,
    val address: String? = null,
    val phone: String? = null,
)

@Serializable
data class RepTodayDto(
    val status: String? = null,
    @SerialName("check_in_at") val checkInAt: String? = null,
    @SerialName("order_amount") val orderAmount: Long = 0,
)

@Serializable
data class RepAuditDto(
    @SerialName("audit_date") val auditDate: String? = null,
    @SerialName("counted_faces") val countedFaces: Int? = null,
    val achieved: Boolean? = null,
)

@Serializable
data class DisplayToScoreDto(
    @SerialName("program_id") val programId: String,
    @SerialName("program_code") val programCode: String = "",
    @SerialName("program_name") val programName: String = "",
    @SerialName("level_id") val levelId: String,
    @SerialName("level_name") val levelName: String = "",
    @SerialName("required_faces") val requiredFaces: Int = 0,
    val registered: Boolean = false,
    @SerialName("my_audit_id") val myAuditId: String? = null,
    @SerialName("my_faces") val myFaces: Int? = null,
    @SerialName("my_achieved") val myAchieved: Boolean? = null,
    @SerialName("my_photos") val myPhotos: Int = 0,
    @SerialName("rep_audit") val repAudit: RepAuditDto? = null,
)

@Serializable
data class WorkWithVisitDto(
    @SerialName("visit_id") val visitId: String,
    val status: String? = null,
    @SerialName("check_in_at") val checkInAt: String? = null,
    @SerialName("check_out_at") val checkOutAt: String? = null,
    @SerialName("distance_m") val distanceM: Int? = null,
    val note: String? = null,
    val rep: StaffBriefDto? = null,
    val customer: WorkWithCustomerDto,
    @SerialName("rep_today") val repToday: RepTodayDto = RepTodayDto(),
    val programs: List<DisplayToScoreDto> = emptyList(),
)

@Serializable
data class AuditPhotoDto(
    @SerialName("storage_path") val storagePath: String,
    @SerialName("file_size") val fileSize: Long? = null,
)

@Serializable
data class DisplayAuditDto(
    val id: String,
    @SerialName("visit_id") val visitId: String,
    @SerialName("program_id") val programId: String,
    @SerialName("level_id") val levelId: String,
    @SerialName("counted_faces") val countedFaces: Int,
    val achieved: Boolean,
    val note: String? = null,
    val photos: List<AuditPhotoDto>,
)

@Serializable
data class WorkWithActionDto(
    val action: String,
    @SerialName("rep_id") val repId: String? = null,
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("visit_id") val visitId: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerialName("accuracy_m") val accuracyM: Float? = null,
    val note: String? = null,
    val audit: DisplayAuditDto? = null,
)

@Serializable
data class WorkWithActionResultDto(
    @SerialName("visit_id") val visitId: String? = null,
    val id: String? = null,
    val ok: Boolean? = null,
)
