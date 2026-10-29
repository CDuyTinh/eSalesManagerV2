package com.tinhcd.myesalesmanager.domain.model

/** The three states every approvable thing in SFA moves through. */
enum class ApprovalStatus(val code: String, val label: String) {
    PENDING("pending", "Chờ duyệt"),
    APPROVED("approved", "Đã duyệt"),
    REJECTED("rejected", "Từ chối"),
    ;

    companion object {
        fun of(code: String?): ApprovalStatus = entries.firstOrNull { it.code == code } ?: PENDING
    }
}

/** Programme sign-ups the supervisor rules on. Legacy WaitingForApproveType. */
enum class TradeKind(val code: String, val label: String) {
    DISPLAY("display", "Trưng bày"),
    LOYALTY("loyalty", "Tích lũy"),
    POSM("posm", "POSM"),
    ;

    companion object {
        fun of(code: String?): TradeKind? = entries.firstOrNull { it.code == code }
    }
}

/** What is waiting, per kind. Drives the badges on the approval menu. */
data class ApprovalCounts(
    val leave: Int = 0,
    val customer: Int = 0,
    val display: Int = 0,
    val loyalty: Int = 0,
    val posm: Int = 0,
    /** Plans reps have sent up for approval. */
    val workPlan: Int = 0,
) {
    val total: Int get() = leave + customer + display + loyalty + posm + workPlan

    fun of(kind: TradeKind): Int = when (kind) {
        TradeKind.DISPLAY -> display
        TradeKind.LOYALTY -> loyalty
        TradeKind.POSM -> posm
    }
}

data class StaffBrief(val id: String, val code: String, val name: String)

/** One page of a list, with per-status counts that ignore the status filter. */
data class ApprovalPage<T>(
    val counts: Map<ApprovalStatus, Int>,
    val items: List<T>,
)

data class LeaveApproval(
    val id: String,
    val salesperson: StaffBrief?,
    val typeName: String,
    val isPaid: Boolean,
    /** ISO dates, yyyy-MM-dd. */
    val fromDate: String,
    val toDate: String,
    val days: Int,
    val reason: String,
    val status: ApprovalStatus,
    val decisionNote: String?,
    val decidedAt: String?,
)

data class CustomerApproval(
    val id: String,
    val code: String,
    val name: String,
    val phone: String?,
    val address: String?,
    val status: ApprovalStatus,
    val note: String?,
    val decisionNote: String?,
    val createdAt: String,
    val photoCount: Int,
    val salesperson: StaffBrief?,
)

data class ApprovalPhoto(val typeName: String, val url: String?)

/** A route of the registering rep the outlet can be put on when approved. */
data class RouteOption(val id: String, val code: String, val name: String, val alreadyOn: Boolean)

data class CustomerApprovalDetail(
    val summary: CustomerApproval,
    val contactName: String?,
    val lat: Double?,
    val lng: Double?,
    val ward: String?,
    val district: String?,
    val province: String?,
    val className: String?,
    val channelName: String?,
    val shopTypeName: String?,
    val photos: List<ApprovalPhoto>,
    val routes: List<RouteOption>,
)

data class TradeApproval(
    val kind: TradeKind,
    val id: String,
    val status: ApprovalStatus,
    val registeredAt: String,
    val programCode: String,
    val programName: String,
    /** The level for display/loyalty, the item for POSM. */
    val detailName: String,
    /** Slots spent (display/loyalty) or quantity asked for (POSM). */
    val qty: Int,
    val approvedQty: Int?,
    val reason: String?,
    val decisionNote: String?,
    val customerCode: String,
    val customerName: String,
    val customerAddress: String?,
    val salesperson: StaffBrief?,
)

/**
 * A ruling. [note] is required when refusing — the rep is owed a reason, and the
 * server refuses a rejection without one.
 */
data class Decision(
    val approve: Boolean,
    val note: String? = null,
    /** Customer only: put the approved outlet on this route of the rep's. */
    val routeId: String? = null,
    /** POSM only: how many to grant; null grants what was asked for. */
    val approvedQty: Int? = null,
) {
    val isValid: Boolean get() = approve || !note.isNullOrBlank()
}
