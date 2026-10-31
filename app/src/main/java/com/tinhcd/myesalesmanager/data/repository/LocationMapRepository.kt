package com.tinhcd.myesalesmanager.data.repository

import com.tinhcd.myesalesmanager.data.remote.http.PostgrestException
import com.tinhcd.myesalesmanager.data.remote.http.orThrow
import com.tinhcd.myesalesmanager.data.remote.service.LocationMapService
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.MapVisit
import com.tinhcd.myesalesmanager.domain.model.RepDay
import com.tinhcd.myesalesmanager.domain.model.StaffBrief
import com.tinhcd.myesalesmanager.domain.model.TracePoint
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/** The team's day on a map. Read-only, so no interface of its own beyond this. */
@Singleton
class LocationMapRepository @Inject constructor(
    private val service: LocationMapService,
) {
    suspend fun day(date: String, salespersonId: String?): DataResult<List<RepDay>> = try {
        DataResult.Success(
            service.map(date, salespersonId).orThrow().reps.map { r ->
                RepDay(
                    rep = StaffBrief(r.rep.id, r.rep.code, r.rep.name),
                    clockIn = r.clockIn,
                    clockOut = r.clockOut,
                    path = r.path.mapNotNull { p ->
                        val lat = p.getOrNull(0)?.jsonPrimitive?.doubleOrNull
                        val lng = p.getOrNull(1)?.jsonPrimitive?.doubleOrNull
                        if (lat == null || lng == null) null
                        else TracePoint(lat, lng, p.getOrNull(2)?.jsonPrimitive?.contentOrNull.orEmpty())
                    },
                    visits = r.visits.map {
                        MapVisit(it.customerCode, it.customerName, it.lat, it.lng, it.checkInAt, it.checkOutAt, it.status, it.distanceM)
                    },
                )
            },
        )
    } catch (e: PostgrestException) {
        DataResult.Failure(AppError.Server(e.status, e.message))
    } catch (e: Exception) {
        DataResult.Failure(e.toAppError())
    }
}
