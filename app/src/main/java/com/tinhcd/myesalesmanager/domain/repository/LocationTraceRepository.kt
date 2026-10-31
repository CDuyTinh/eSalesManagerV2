package com.tinhcd.myesalesmanager.domain.repository

import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.TraceFix

/**
 * The manager's own working-day trace (legacy manager ServiceBackground), the
 * same as the reps': queued on the phone, sent in batches, so a dead zone costs
 * a delay rather than the points.
 */
interface LocationTraceRepository {
    /** Seconds between fixes, from the server's settings. */
    fun intervalSeconds(): Int

    suspend fun record(point: TraceFix)

    /**
     * Sends what is queued. A batch the server answered is removed whether it
     * kept every point or not: one it dropped will never be accepted later.
     * @return how many points are still waiting.
     */
    suspend fun flush(): DataResult<Int>
}
