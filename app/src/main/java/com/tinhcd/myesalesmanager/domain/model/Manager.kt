package com.tinhcd.myesalesmanager.domain.model

/**
 * Where a person sits in the sales hierarchy. Mirrors the legacy
 * `AR_Salesperson.Position` values S / SS / AM / RM.
 *
 * Only [SS] can use this app for now; [AM] and [RM] need multi-level teams and
 * multi-branch scope, which are planned but not built.
 */
enum class Position(val code: String) {
    SR("sr"),
    SS("ss"),
    AM("am"),
    RM("rm"),
    ;

    val isManager: Boolean get() = this != SR

    companion object {
        /** Unknown codes read as [SR], so a typo can never grant manager access. */
        fun of(code: String?): Position = entries.firstOrNull { it.code == code } ?: SR
    }
}

/**
 * The signed-in manager. Managers are rows of the same `salesperson` table the
 * reps live in — the legacy system kept them in `AR_Salesperson` too — so
 * timekeeping, leave and work notes can reuse the rep tables unchanged.
 */
data class Manager(
    val id: String,
    val code: String,
    val fullName: String,
    val position: Position,
    val branchId: String,
    val branchCode: String,
    val branchName: String,
)

/** A rep reporting to the signed-in manager. */
data class TeamMember(
    val id: String,
    val code: String,
    val fullName: String,
    val position: Position,
    val phone: String?,
)
