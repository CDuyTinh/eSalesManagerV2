package com.tinhcd.myesalesmanager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of `GET /mgr-bootstrap`. */
@Serializable
data class ManagerBootstrapDto(
    /** Null when the auth user has no salesperson row — an unprovisioned account. */
    val manager: StaffDto? = null,
    /** Direct reports of [manager]; empty for a rep, who is refused anyway. */
    val team: List<StaffDto> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
    /** Offered as chips when refusing an approval. */
    @SerialName("reject_reasons") val rejectReasons: List<String> = emptyList(),
)

@Serializable
data class StaffDto(
    val id: String,
    val code: String,
    @SerialName("full_name") val fullName: String,
    val position: String? = null,
    @SerialName("branch_id") val branchId: String? = null,
    val phone: String? = null,
    @SerialName("supervisor_id") val supervisorId: String? = null,
    val branch: BranchDto? = null,
)

@Serializable
data class BranchDto(
    val id: String,
    val code: String,
    val name: String,
    val area: PlaceDto? = null,
)

/** An area, with its region nested; or the region itself. */
@Serializable
data class PlaceDto(
    val id: String,
    val code: String = "",
    val name: String,
    val region: PlaceDto? = null,
)
