package com.tinhcd.myesalesmanager.data.repository

import com.tinhcd.myesalesmanager.data.remote.dto.CompleteWorkNoteDto
import com.tinhcd.myesalesmanager.data.remote.dto.NewLeaveRequestDto
import com.tinhcd.myesalesmanager.data.remote.dto.NewWorkNoteDto
import com.tinhcd.myesalesmanager.data.remote.dto.ReadNotificationDto
import com.tinhcd.myesalesmanager.data.remote.dto.WithdrawLeaveDto
import com.tinhcd.myesalesmanager.data.remote.dto.WorkDayPunchDto
import com.tinhcd.myesalesmanager.data.remote.http.PostgrestException
import com.tinhcd.myesalesmanager.data.remote.http.orThrow
import com.tinhcd.myesalesmanager.data.remote.service.PersonalService
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.AppNotification
import com.tinhcd.myesalesmanager.domain.model.GeoPoint
import com.tinhcd.myesalesmanager.domain.model.LeaveBoard
import com.tinhcd.myesalesmanager.domain.model.LeaveType
import com.tinhcd.myesalesmanager.domain.model.MyLeave
import com.tinhcd.myesalesmanager.domain.model.NotificationFeed
import com.tinhcd.myesalesmanager.domain.model.NotificationKind
import com.tinhcd.myesalesmanager.domain.model.WorkDay
import com.tinhcd.myesalesmanager.domain.model.WorkNote
import com.tinhcd.myesalesmanager.domain.repository.PersonalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class PersonalRepositoryImpl @Inject constructor(
    private val service: PersonalService,
) : PersonalRepository {

    private val _unread = MutableStateFlow(0)
    override val unread: StateFlow<Int> = _unread.asStateFlow()

    override suspend fun workDay(date: String) = call {
        val d = service.workDay(date).orThrow()
        WorkDay(
            d.workDate, d.branch.name, d.branch.address, d.branch.lat, d.branch.lng,
            d.checkInAt, d.checkOutAt, d.openVisits,
        )
    }

    override suspend fun punch(day: WorkDay, checkIn: Boolean, at: GeoPoint?) = call {
        val distance = if (at != null && day.branchLat != null && day.branchLng != null) {
            metres(at.lat, at.lng, day.branchLat, day.branchLng)
        } else {
            null
        }
        service.punch(
            WorkDayPunchDto(
                type = if (checkIn) "check_in" else "check_out",
                workDate = day.workDate,
                happenedAt = OffsetDateTime.now().toString(),
                lat = at?.lat,
                lng = at?.lng,
                accuracyM = at?.accuracyM?.toDouble(),
                distanceM = distance,
            ),
        ).orThrow()
        Unit
    }

    override suspend fun leave() = call {
        val d = service.leave().orThrow()
        LeaveBoard(
            types = d.types.map { LeaveType(it.leaveTypeId, it.code, it.name, it.isPaid) },
            requests = d.requests.map {
                MyLeave(it.requestId, it.typeName, it.isPaid, it.fromDate, it.toDate, it.reason,
                    it.status, it.decisionNote, it.decidedAt)
            },
        )
    }

    override suspend fun requestLeave(typeId: String, from: String, to: String, reason: String) = call {
        service.requestLeave(NewLeaveRequestDto(typeId, from, to, reason.trim())).orThrow()
        Unit
    }

    override suspend fun withdrawLeave(id: String) = call {
        service.withdrawLeave(WithdrawLeaveDto(id)).orThrow()
        Unit
    }

    override suspend fun notes() = call {
        service.notes().orThrow().notes.map {
            WorkNote(it.noteId, it.title, it.body, it.dueOn, it.status == "done", it.result, it.doneAt, it.createdAt)
        }
    }

    override suspend fun addNote(title: String, body: String?, dueOn: String?) = call {
        service.addNote(NewWorkNoteDto(title.trim(), body?.trim()?.takeIf { it.isNotEmpty() }, dueOn)).orThrow()
        Unit
    }

    override suspend fun completeNote(id: String, result: String) = call {
        service.completeNote(CompleteWorkNoteDto(id, result.trim())).orThrow()
        Unit
    }

    override suspend fun deleteNote(id: String) = call {
        service.deleteNote(id).orThrow()
        Unit
    }

    override suspend fun notifications() = call {
        val d = service.notifications().orThrow()
        _unread.value = d.unread
        NotificationFeed(
            items = d.items.map {
                AppNotification(NotificationKind.of(it.kind), it.kind, it.sourceId, it.title, it.body, it.fromDate, it.isRead)
            },
            unread = d.unread,
        )
    }

    override suspend fun markRead(kindCode: String, sourceId: String) = call {
        _unread.value = service.read(ReadNotificationDto(kind = kindCode, sourceId = sourceId)).orThrow().unread
    }

    override suspend fun markAllRead() = call {
        _unread.value = service.read(ReadNotificationDto(all = true)).orThrow().unread
    }

    /** Great-circle distance, as the server's distance_m() computes it. */
    private fun metres(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private inline fun <T> call(block: () -> T): DataResult<T> = try {
        DataResult.Success(block())
    } catch (e: PostgrestException) {
        DataResult.Failure(
            if (e.status == 409) AppError.Rule("server", e.message) else AppError.Server(e.status, e.message),
        )
    } catch (e: Exception) {
        DataResult.Failure(e.toAppError())
    }
}
