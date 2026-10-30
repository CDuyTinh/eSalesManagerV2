package com.tinhcd.myesalesmanager.domain.repository

import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.GeoPoint
import com.tinhcd.myesalesmanager.domain.model.LeaveBoard
import com.tinhcd.myesalesmanager.domain.model.NotificationFeed
import com.tinhcd.myesalesmanager.domain.model.WorkDay
import com.tinhcd.myesalesmanager.domain.model.WorkNote
import kotlinx.coroutines.flow.StateFlow

/** The manager's own work: clocking in and out, leave, notes, the bell. */
interface PersonalRepository {
    /** The bell's badge, refreshed by every read of the feed. */
    val unread: StateFlow<Int>

    suspend fun workDay(date: String): DataResult<WorkDay>

    /** Clocks in ([checkIn]) or out at [at], recording how far from the depot. */
    suspend fun punch(day: WorkDay, checkIn: Boolean, at: GeoPoint?): DataResult<Unit>

    suspend fun leave(): DataResult<LeaveBoard>

    suspend fun requestLeave(typeId: String, from: String, to: String, reason: String): DataResult<Unit>

    suspend fun withdrawLeave(id: String): DataResult<Unit>

    suspend fun notes(): DataResult<List<WorkNote>>

    suspend fun addNote(title: String, body: String?, dueOn: String?): DataResult<Unit>

    suspend fun completeNote(id: String, result: String): DataResult<Unit>

    suspend fun deleteNote(id: String): DataResult<Unit>

    suspend fun notifications(): DataResult<NotificationFeed>

    suspend fun markRead(kindCode: String, sourceId: String): DataResult<Unit>

    suspend fun markAllRead(): DataResult<Unit>
}
