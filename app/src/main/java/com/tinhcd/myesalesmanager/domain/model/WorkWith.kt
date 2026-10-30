package com.tinhcd.myesalesmanager.domain.model

/** A GPS fix. */
data class GeoPoint(
    val lat: Double,
    val lng: Double,
    val accuracyM: Float?,
)

/** SFA's visit states, as the supervisor reads them on a rep's route. */
enum class VisitStatus(val code: String, val label: String) {
    PLANNED("planned", "Chưa ghé"),
    IN_PROGRESS("in_progress", "Đang ghé"),
    COMPLETED("completed", "Đã ghé"),
    NO_ORDER("no_order", "Không đơn"),
    CLOSED("closed", "Đóng cửa"),
    ;

    companion object {
        fun of(code: String?): VisitStatus? = entries.firstOrNull { it.code == code }
    }
}

/** One shop on the rep's route for the day, with both people's progress there. */
data class WorkWithStop(
    val customerId: String,
    val code: String,
    val name: String,
    val address: String?,
    val lat: Double?,
    val lng: Double?,
    /** On the rep's route for this weekday; false for a shop they called on off-route. */
    val planned: Boolean,
    val repStatus: VisitStatus?,
    val repCheckIn: String?,
    val repOrderAmount: Long,
    val myVisitId: String?,
    val myStatus: VisitStatus?,
)

data class WorkWithRoute(
    val date: String,
    val rep: StaffBrief?,
    /** The supervisor's call still open somewhere, if any — one at a time. */
    val openVisitId: String?,
    val stops: List<WorkWithStop>,
)

/** A display programme to score on this call, with both scores. */
data class DisplayToScore(
    val programId: String,
    val programCode: String,
    val programName: String,
    val levelId: String,
    val levelName: String,
    val requiredFaces: Int,
    val registered: Boolean,
    val myAuditId: String?,
    val myFaces: Int?,
    val myAchieved: Boolean?,
    val myPhotos: Int,
    val repAuditDate: String?,
    val repFaces: Int?,
    val repAchieved: Boolean?,
)

data class WorkWithVisit(
    val visitId: String,
    val status: VisitStatus?,
    val checkInAt: String?,
    val checkOutAt: String?,
    val distanceM: Int?,
    val note: String?,
    val rep: StaffBrief?,
    val customerId: String,
    val customerCode: String,
    val customerName: String,
    val customerAddress: String?,
    val customerPhone: String?,
    val repStatus: VisitStatus?,
    val repCheckIn: String?,
    val repOrderAmount: Long,
    val programs: List<DisplayToScore>,
) {
    val isOpen: Boolean get() = status == VisitStatus.IN_PROGRESS
}

/** The supervisor's score for one programme, photos still on the device. */
data class DisplayScore(
    val visitId: String,
    val programId: String,
    val levelId: String,
    val countedFaces: Int,
    val achieved: Boolean,
    val note: String?,
    val photoPaths: List<String>,
)
