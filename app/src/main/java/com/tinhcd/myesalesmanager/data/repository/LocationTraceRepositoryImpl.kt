package com.tinhcd.myesalesmanager.data.repository

import com.tinhcd.myesalesmanager.data.remote.http.orThrow
import com.tinhcd.myesalesmanager.data.remote.service.LocationTraceService
import com.tinhcd.myesalesmanager.data.remote.service.TraceBatchDto
import com.tinhcd.myesalesmanager.data.remote.service.TracePointDto
import com.tinhcd.myesalesmanager.data.session.SessionStore
import com.tinhcd.myesalesmanager.data.session.TraceQueue
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.TraceFix
import com.tinhcd.myesalesmanager.domain.repository.LocationTraceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationTraceRepositoryImpl @Inject constructor(
    private val service: LocationTraceService,
    private val queue: TraceQueue,
    private val session: SessionStore,
) : LocationTraceRepository {

    /** The default when the service outlives the session that held the settings. */
    override fun intervalSeconds(): Int =
        session.settings.value["gps_track_interval_s"]?.toIntOrNull()?.coerceIn(30, 3600) ?: DEFAULT_INTERVAL_S

    override suspend fun record(point: TraceFix) = queue.append(
        TracePointDto(point.recordedAt, point.lat, point.lng, point.accuracyM, point.speedMps, point.batteryPct),
    )

    override suspend fun flush(): DataResult<Int> = try {
        while (true) {
            val batch = queue.peek(BATCH)
            if (batch.isEmpty()) break
            service.submit(TraceBatchDto(batch)).orThrow()
            queue.drop(batch.size)
            if (batch.size < BATCH) break
        }
        DataResult.Success(queue.size())
    } catch (e: Exception) {
        // Left queued for the next tick: the network is the usual cause.
        DataResult.Failure(e.toAppError())
    }

    private companion object {
        const val DEFAULT_INTERVAL_S = 300
        const val BATCH = 200
    }
}
