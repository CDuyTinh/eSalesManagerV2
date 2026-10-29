package com.tinhcd.myesalesmanager.domain.model

/**
 * The SFA work_plan state machine, as the supervisor sees it. One enum for the
 * legacy's two alphabets (OM_MakingPlan H W A R C V / OM_WorkingPlan O W A R P C).
 */
enum class WorkPlanStatus(val code: String, val label: String) {
    DRAFT("draft", "Nháp"),
    SUBMITTED("submitted", "Chờ duyệt"),
    APPROVED("approved", "Đang thực hiện"),
    REJECTED("rejected", "Từ chối"),
    COMPLETED("completed", "Hoàn thành"),
    CANCELLED("cancelled", "Đã huỷ"),
    ;

    companion object {
        fun of(code: String?): WorkPlanStatus = entries.firstOrNull { it.code == code } ?: DRAFT

        /** What the supervisor filters by; a rep's unsent draft never reaches them. */
        val visible = listOf(SUBMITTED, APPROVED, COMPLETED, REJECTED, CANCELLED)
    }
}

/** What a plan is with. Null on the category means the work has no object. */
enum class WorkObjectKind(val code: String, val label: String) {
    CUSTOMER("customer", "Khách hàng"),
    SALESPERSON("salesperson", "Nhân viên"),
    BRANCH("branch", "Nhà phân phối"),
    ;

    companion object {
        fun of(code: String?): WorkObjectKind? = entries.firstOrNull { it.code == code }
    }
}

data class WorkCategory(
    val id: String,
    val code: String,
    val name: String,
    val objectKind: WorkObjectKind?,
    val fieldTraining: Boolean,
)

data class TeamWorkPlan(
    val id: String,
    val salesperson: StaffBrief?,
    val author: StaffBrief?,
    /** The signed-in supervisor wrote it — and so may call it off. */
    val assignedByMe: Boolean,
    val categoryName: String,
    val objectKind: WorkObjectKind?,
    val content: String,
    val place: String?,
    /** ISO-8601 instants with offset, as the server sends them. */
    val fromAt: String,
    val toAt: String,
    val status: WorkPlanStatus,
    /** What the rep reported on completion. */
    val result: String?,
    /** Why it was rejected or cancelled. */
    val reason: String?,
    val objectCount: Int,
) {
    /** Written by someone other than the rep it is for. */
    val isAssigned: Boolean get() = author?.id != salesperson?.id
}

data class WorkPlanObject(
    val kind: WorkObjectKind?,
    val targetId: String,
    val code: String?,
    val name: String,
    val address: String?,
)

data class WorkPlanHistoryEntry(
    val status: WorkPlanStatus,
    val reason: String?,
    val actor: String?,
    val at: String,
)

data class WorkPlanDetail(
    val plan: TeamWorkPlan,
    val note: String?,
    val objects: List<WorkPlanObject>,
    val history: List<WorkPlanHistoryEntry>,
    val photos: List<ApprovalPhoto>,
)

data class WorkPlanPage(
    val counts: Map<WorkPlanStatus, Int>,
    val items: List<TeamWorkPlan>,
)

data class PlannableCustomer(
    val id: String,
    val code: String,
    val name: String,
    val address: String?,
)

/** A task the supervisor is about to hand a rep. Times are ISO-8601 with offset. */
data class AssignDraft(
    val salespersonId: String,
    val categoryId: String,
    val content: String,
    val place: String?,
    val note: String?,
    val fromAt: String,
    val toAt: String,
    val objectIds: List<String>,
)
