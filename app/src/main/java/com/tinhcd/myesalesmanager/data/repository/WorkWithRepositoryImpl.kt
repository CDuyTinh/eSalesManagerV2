package com.tinhcd.myesalesmanager.data.repository

import com.tinhcd.myesalesmanager.core.photo.PhotoStore
import com.tinhcd.myesalesmanager.data.remote.dto.AuditPhotoDto
import com.tinhcd.myesalesmanager.data.remote.dto.DisplayAuditDto
import com.tinhcd.myesalesmanager.data.remote.dto.WorkWithActionDto
import com.tinhcd.myesalesmanager.data.remote.http.PostgrestException
import com.tinhcd.myesalesmanager.data.remote.http.orThrow
import com.tinhcd.myesalesmanager.data.remote.service.WorkWithService
import com.tinhcd.myesalesmanager.data.remote.storage.PhotoUploader
import com.tinhcd.myesalesmanager.data.session.SessionStore
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.DisplayScore
import com.tinhcd.myesalesmanager.domain.model.DisplayToScore
import com.tinhcd.myesalesmanager.domain.model.GeoPoint
import com.tinhcd.myesalesmanager.domain.model.StaffBrief
import com.tinhcd.myesalesmanager.domain.model.VisitStatus
import com.tinhcd.myesalesmanager.domain.model.WorkWithRoute
import com.tinhcd.myesalesmanager.domain.model.WorkWithStop
import com.tinhcd.myesalesmanager.domain.model.WorkWithVisit
import com.tinhcd.myesalesmanager.domain.repository.WorkWithRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkWithRepositoryImpl @Inject constructor(
    private val service: WorkWithService,
    private val uploader: PhotoUploader,
    private val photos: PhotoStore,
    private val session: SessionStore,
) : WorkWithRepository {

    override suspend fun route(repId: String, date: String) = call {
        val d = service.route(repId, date).orThrow()
        WorkWithRoute(
            date = d.date,
            rep = d.rep?.let { StaffBrief(it.id, it.code, it.name) },
            openVisitId = d.openVisitId,
            stops = d.stops.map {
                WorkWithStop(
                    it.customerId, it.code, it.name, it.address, it.lat, it.lng, it.planned,
                    VisitStatus.of(it.repStatus), it.repCheckIn, it.repOrderAmount,
                    it.myVisitId, VisitStatus.of(it.myStatus),
                )
            },
        )
    }

    override suspend fun checkIn(repId: String, customerId: String, at: GeoPoint?) = call {
        service.act(
            WorkWithActionDto(
                action = "check_in", repId = repId, customerId = customerId,
                lat = at?.lat, lng = at?.lng, accuracyM = at?.accuracyM,
            ),
        ).orThrow().visitId ?: error("server returned no visit id")
    }

    override suspend fun visit(visitId: String) = call {
        val d = service.visit(visitId).orThrow()
        WorkWithVisit(
            visitId = d.visitId,
            status = VisitStatus.of(d.status),
            checkInAt = d.checkInAt,
            checkOutAt = d.checkOutAt,
            distanceM = d.distanceM,
            note = d.note,
            rep = d.rep?.let { StaffBrief(it.id, it.code, it.name) },
            customerId = d.customer.id,
            customerCode = d.customer.code,
            customerName = d.customer.name,
            customerAddress = d.customer.address,
            customerPhone = d.customer.phone,
            repStatus = VisitStatus.of(d.repToday.status),
            repCheckIn = d.repToday.checkInAt,
            repOrderAmount = d.repToday.orderAmount,
            programs = d.programs.map {
                DisplayToScore(
                    it.programId, it.programCode, it.programName, it.levelId, it.levelName,
                    it.requiredFaces, it.registered, it.myAuditId, it.myFaces, it.myAchieved,
                    it.myPhotos, it.repAudit?.auditDate, it.repAudit?.countedFaces, it.repAudit?.achieved,
                )
            },
        )
    }

    override suspend fun checkOut(visitId: String, note: String?, at: GeoPoint?) = call {
        service.act(
            WorkWithActionDto(
                action = "check_out", visitId = visitId,
                note = note?.trim()?.takeIf { it.isNotEmpty() }, lat = at?.lat, lng = at?.lng,
            ),
        ).orThrow()
        Unit
    }

    /**
     * Photos first, then the score. submit_display_audit checks every path is in
     * storage, so the order matters; the paths follow the bucket's convention
     * `<salesperson_id>/<visit_id>/<file>`, the supervisor's own id leading.
     */
    override suspend fun scoreDisplay(score: DisplayScore) = call {
        val me = session.current.value?.id ?: error("Chưa tải được thông tin người dùng")
        val uploaded = score.photoPaths.map { local ->
            val size = photos.compress(local)
            AuditPhotoDto(uploader.upload(me, score.visitId, local), size)
        }
        service.act(
            WorkWithActionDto(
                action = "display",
                audit = DisplayAuditDto(
                    id = UUID.randomUUID().toString(),
                    visitId = score.visitId,
                    programId = score.programId,
                    levelId = score.levelId,
                    countedFaces = score.countedFaces,
                    achieved = score.achieved,
                    note = score.note?.trim()?.takeIf { it.isNotEmpty() },
                    photos = uploaded,
                ),
            ),
        ).orThrow()
        score.photoPaths.forEach(uploader::deleteLocal)
        Unit
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
