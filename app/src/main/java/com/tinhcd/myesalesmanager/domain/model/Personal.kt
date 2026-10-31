package com.tinhcd.myesalesmanager.domain.model

/**
 * The manager's own day, leave, notes and bell. The manager is a salesperson
 * row like the reps, so all four run on the SFA app's own endpoints.
 */

data class WorkDay(
    val workDate: String,
    val branchName: String,
    val branchAddress: String?,
    val branchLat: Double?,
    val branchLng: Double?,
    val checkInAt: String?,
    val checkOutAt: String?,
    /** The supervisor's own calls still open, which block clocking out. */
    val openVisits: Int,
)

data class LeaveType(val id: String, val code: String, val name: String, val isPaid: Boolean)

data class MyLeave(
    val id: String,
    val typeName: String,
    val isPaid: Boolean,
    val fromDate: String,
    val toDate: String,
    val reason: String,
    /** pending, approved, rejected or cancelled (withdrawn by the requester). */
    val status: String,
    val decisionNote: String?,
    val decidedAt: String?,
)

data class LeaveBoard(val types: List<LeaveType>, val requests: List<MyLeave>)

data class WorkNote(
    val id: String,
    val title: String,
    val body: String?,
    val dueOn: String?,
    val done: Boolean,
    val result: String?,
    val doneAt: String?,
    val createdAt: String?,
)

/** What a bell entry is about; decides where tapping it leads. */
enum class NotificationKind(val code: String) {
    TEAM_LEAVE("team_leave"),
    TEAM_CUSTOMER("team_customer"),
    TEAM_TRADE("team_trade"),
    TEAM_WORK_PLAN("team_work_plan"),
    LEAVE_RESULT("leave_result"),
    WORK_PLAN("work_plan"),
    OTHER(""),
    ;

    companion object {
        fun of(code: String): NotificationKind = entries.firstOrNull { it.code == code } ?: OTHER
    }
}

data class AppNotification(
    val kind: NotificationKind,
    /** As the server sent it, needed to mark it read. */
    val kindCode: String,
    val sourceId: String,
    val title: String,
    val body: String,
    val fromDate: String?,
    val isRead: Boolean,
)

data class NotificationFeed(val items: List<AppNotification>, val unread: Int)
