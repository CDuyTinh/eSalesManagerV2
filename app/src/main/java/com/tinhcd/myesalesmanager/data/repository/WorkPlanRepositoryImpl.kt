package com.tinhcd.myesalesmanager.data.repository

import com.tinhcd.myesalesmanager.data.remote.dto.AssignObjectDto
import com.tinhcd.myesalesmanager.data.remote.dto.AssignPlanDto
import com.tinhcd.myesalesmanager.data.remote.dto.StaffBriefDto
import com.tinhcd.myesalesmanager.data.remote.dto.TeamWorkPlanDto
import com.tinhcd.myesalesmanager.data.remote.dto.WorkPlanActionDto
import com.tinhcd.myesalesmanager.data.remote.http.PostgrestException
import com.tinhcd.myesalesmanager.data.remote.http.orThrow
import com.tinhcd.myesalesmanager.data.remote.service.WorkPlanService
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.ApprovalPhoto
import com.tinhcd.myesalesmanager.domain.model.AssignDraft
import com.tinhcd.myesalesmanager.domain.model.PlannableCustomer
import com.tinhcd.myesalesmanager.domain.model.StaffBrief
import com.tinhcd.myesalesmanager.domain.model.TeamWorkPlan
import com.tinhcd.myesalesmanager.domain.model.WorkCategory
import com.tinhcd.myesalesmanager.domain.model.WorkObjectKind
import com.tinhcd.myesalesmanager.domain.model.WorkPlanDetail
import com.tinhcd.myesalesmanager.domain.model.WorkPlanHistoryEntry
import com.tinhcd.myesalesmanager.domain.model.WorkPlanObject
import com.tinhcd.myesalesmanager.domain.model.WorkPlanPage
import com.tinhcd.myesalesmanager.domain.model.WorkPlanStatus
import com.tinhcd.myesalesmanager.domain.repository.WorkPlanRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkPlanRepositoryImpl @Inject constructor(
    private val service: WorkPlanService,
) : WorkPlanRepository {

    override suspend fun plans(
        status: WorkPlanStatus?,
        salespersonId: String?,
        fromDate: String,
        toDate: String,
    ) = call {
        val page = service.plans(fromDate, toDate, status?.code, salespersonId).orThrow()
        WorkPlanPage(
            counts = page.counts.mapKeys { WorkPlanStatus.of(it.key) },
            items = page.items.map { it.toDomain() },
        )
    }

    override suspend fun detail(id: String) = call {
        val d = service.detail(id).orThrow()
        WorkPlanDetail(
            plan = TeamWorkPlan(
                id = d.id,
                salesperson = d.salesperson?.toDomain(),
                author = d.author?.toDomain(),
                assignedByMe = d.assignedByMe,
                categoryName = d.categoryName,
                objectKind = WorkObjectKind.of(d.objectKind),
                content = d.content,
                place = d.place,
                fromAt = d.fromAt,
                toAt = d.toAt,
                status = WorkPlanStatus.of(d.status),
                result = d.result,
                reason = d.reason,
                objectCount = d.objects.size,
            ),
            note = d.note,
            objects = d.objects.map {
                WorkPlanObject(WorkObjectKind.of(it.kind), it.targetId, it.code, it.name, it.address)
            },
            history = d.history.map {
                WorkPlanHistoryEntry(WorkPlanStatus.of(it.status), it.reason, it.actor, it.at)
            },
            photos = d.photos.map { ApprovalPhoto("", it.url) },
        )
    }

    override suspend fun categories() = call {
        service.categories().orThrow().map {
            WorkCategory(it.id, it.code, it.name, WorkObjectKind.of(it.objectKind), it.fieldTraining)
        }
    }

    override suspend fun customersOf(salespersonId: String, query: String?) = call {
        service.customersOf(salespersonId, query?.takeIf { it.isNotBlank() }).orThrow().map {
            PlannableCustomer(it.id, it.code, it.name, it.address)
        }
    }

    override suspend fun assign(draft: AssignDraft) = call {
        service.act(
            WorkPlanActionDto(
                action = "assign",
                plan = AssignPlanDto(
                    salespersonId = draft.salespersonId,
                    categoryId = draft.categoryId,
                    content = draft.content.trim(),
                    place = draft.place?.trim()?.takeIf { it.isNotEmpty() },
                    note = draft.note?.trim()?.takeIf { it.isNotEmpty() },
                    fromAt = draft.fromAt,
                    toAt = draft.toAt,
                    objects = draft.objectIds.map { AssignObjectDto(it) },
                ),
            ),
        ).orThrow().id
    }

    override suspend fun decide(id: String, approve: Boolean, reason: String?) = call {
        service.act(
            WorkPlanActionDto(
                action = "decide",
                id = id,
                approve = approve,
                reason = reason?.trim()?.takeIf { it.isNotEmpty() },
            ),
        ).orThrow()
        Unit
    }

    override suspend fun cancel(id: String, reason: String) = call {
        service.act(WorkPlanActionDto(action = "cancel", id = id, reason = reason.trim())).orThrow()
        Unit
    }

    /** 409 carries a sentence the database wrote for a person; surface it as-is. */
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

private fun StaffBriefDto.toDomain() = StaffBrief(id, code, name)

private fun TeamWorkPlanDto.toDomain() = TeamWorkPlan(
    id = id,
    salesperson = salesperson?.toDomain(),
    author = author?.toDomain(),
    assignedByMe = assignedByMe,
    categoryName = categoryName,
    objectKind = WorkObjectKind.of(objectKind),
    content = content,
    place = place,
    fromAt = fromAt,
    toAt = toAt,
    status = WorkPlanStatus.of(status),
    result = result,
    reason = reason,
    objectCount = objectCount,
)
