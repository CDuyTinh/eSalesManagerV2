package com.tinhcd.myesalesmanager.domain.model

/** One recorded position on a rep's path. */
data class TracePoint(val lat: Double, val lng: Double, val at: String)

/** An outlet called on, where the rep checked in. */
data class MapVisit(
    val customerCode: String,
    val customerName: String,
    val lat: Double?,
    val lng: Double?,
    val checkInAt: String?,
    val checkOutAt: String?,
    val status: String?,
    val distanceM: Double?,
)

/** A rep's day on the map (legacy OM30400). */
data class RepDay(
    val rep: StaffBrief,
    val clockIn: String?,
    val clockOut: String?,
    val path: List<TracePoint>,
    val visits: List<MapVisit>,
) {
    /** Where the rep was last seen: the end of the path, or the last call. */
    val lastSeen: TracePoint?
        get() = path.lastOrNull()
            ?: visits.lastOrNull { it.lat != null && it.lng != null }?.let { TracePoint(it.lat!!, it.lng!!, it.checkInAt.orEmpty()) }
}
