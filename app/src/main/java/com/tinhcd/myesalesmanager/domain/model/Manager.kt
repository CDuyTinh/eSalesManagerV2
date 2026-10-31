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
    /** The rep's distributor, which is what a "work with the distributor" task names. */
    val branchId: String?,
    /** Their direct boss: for a rep, the SS whose filter they fall under. */
    val supervisorId: String? = null,
    /** Where their distributor sits, for the region / area / NPP filter. */
    val branch: BranchPlace? = null,
)

/** A distributor and the area and region it belongs to (either may be unset). */
data class BranchPlace(
    val id: String,
    val name: String,
    val areaId: String?,
    val areaName: String?,
    val regionId: String?,
    val regionName: String?,
)

/** What the place rows of the team filter have picked. */
sealed interface PlacePick {
    data class Region(val id: String) : PlacePick
    data class Area(val id: String) : PlacePick
    data class Branch(val id: String) : PlacePick
}

/** The distributors a pick covers, among those [members] belong to; null for no pick. */
fun PlacePick?.branchIds(members: List<TeamMember>): List<String>? = this?.let { pick ->
    members.mapNotNull { it.branch }.filter { b ->
        when (pick) {
            is PlacePick.Region -> b.regionId == pick.id
            is PlacePick.Area -> b.areaId == pick.id
            is PlacePick.Branch -> b.id == pick.id
        }
    }.map { it.id }.distinct()
}

/** How the header names the signed-in manager's level. */
val Position.title: String
    get() = when (this) {
        Position.SR -> "Nhân viên"
        Position.SS -> "Giám sát"
        Position.AM -> "Quản lý khu vực"
        Position.RM -> "Quản lý vùng"
    }
