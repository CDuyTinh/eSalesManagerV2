package com.tinhcd.myesalesmanager.domain.repository

import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.DisplayScore
import com.tinhcd.myesalesmanager.domain.model.GeoPoint
import com.tinhcd.myesalesmanager.domain.model.WorkWithRoute
import com.tinhcd.myesalesmanager.domain.model.WorkWithVisit

/** Riding a rep's route: the supervisor's own visits, tagged with the rep. */
interface WorkWithRepository {
    suspend fun route(repId: String, date: String): DataResult<WorkWithRoute>

    /** @return the new visit's id. */
    suspend fun checkIn(repId: String, customerId: String, at: GeoPoint?): DataResult<String>

    suspend fun visit(visitId: String): DataResult<WorkWithVisit>

    suspend fun checkOut(visitId: String, note: String?, at: GeoPoint?): DataResult<Unit>

    /** Uploads the photos, then records the score through SFA's display audit. */
    suspend fun scoreDisplay(score: DisplayScore): DataResult<Unit>
}
