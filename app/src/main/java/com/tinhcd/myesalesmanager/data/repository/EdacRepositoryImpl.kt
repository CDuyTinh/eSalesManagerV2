package com.tinhcd.myesalesmanager.data.repository

import com.tinhcd.myesalesmanager.core.photo.PhotoStore
import com.tinhcd.myesalesmanager.data.remote.dto.EdacActionDto
import com.tinhcd.myesalesmanager.data.remote.dto.EdacAnswerInDto
import com.tinhcd.myesalesmanager.data.remote.dto.EdacCriterionInDto
import com.tinhcd.myesalesmanager.data.remote.dto.EdacOptionDto
import com.tinhcd.myesalesmanager.data.remote.dto.EdacSavedDto
import com.tinhcd.myesalesmanager.data.remote.dto.EdacStartedDto
import com.tinhcd.myesalesmanager.data.remote.dto.EdacTrainingDto
import com.tinhcd.myesalesmanager.data.remote.dto.TeamMonthDto
import com.tinhcd.myesalesmanager.data.remote.dto.TeamTodayDto
import com.tinhcd.myesalesmanager.data.remote.http.PostgrestException
import com.tinhcd.myesalesmanager.data.remote.http.orThrow
import com.tinhcd.myesalesmanager.data.remote.service.EdacService
import com.tinhcd.myesalesmanager.data.remote.storage.PhotoUploader
import com.tinhcd.myesalesmanager.data.session.SessionStore
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.EdacAnswer
import com.tinhcd.myesalesmanager.domain.model.EdacCriteriaBook
import com.tinhcd.myesalesmanager.domain.model.EdacCriterion
import com.tinhcd.myesalesmanager.domain.model.EdacCriterionAdmin
import com.tinhcd.myesalesmanager.domain.model.EdacHistoryItem
import com.tinhcd.myesalesmanager.domain.model.EdacKind
import com.tinhcd.myesalesmanager.domain.model.EdacObserved
import com.tinhcd.myesalesmanager.domain.model.EdacOption
import com.tinhcd.myesalesmanager.domain.model.EdacOutlet
import com.tinhcd.myesalesmanager.domain.model.EdacResult
import com.tinhcd.myesalesmanager.domain.model.EdacResultLine
import com.tinhcd.myesalesmanager.domain.model.EdacSession
import com.tinhcd.myesalesmanager.domain.model.EdacStep
import com.tinhcd.myesalesmanager.domain.model.EdacStepForm
import com.tinhcd.myesalesmanager.domain.model.EdacTraining
import com.tinhcd.myesalesmanager.domain.model.StaffBrief
import com.tinhcd.myesalesmanager.domain.model.TeamMonth
import com.tinhcd.myesalesmanager.domain.model.TeamToday
import com.tinhcd.myesalesmanager.domain.repository.EdacRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EdacRepositoryImpl @Inject constructor(
    private val service: EdacService,
    private val uploader: PhotoUploader,
    private val photos: PhotoStore,
    private val session: SessionStore,
    private val json: Json,
) : EdacRepository {

    override suspend fun sessions(date: String) = call {
        service.sessions(date).orThrow().map {
            EdacSession(
                planId = it.planId,
                content = it.content,
                fromAt = it.fromAt,
                toAt = it.toAt,
                rep = StaffBrief(it.rep.id, it.rep.code, it.rep.name),
                trainingId = it.training?.id,
                currentStep = it.training?.currentStep ?: 0,
                completed = it.training?.status == "completed",
            )
        }
    }

    override suspend fun start(repId: String?, planId: String?) = call {
        val body = service.act(EdacActionDto("start", repId = repId, planId = planId)).orThrow()
        json.decodeFromJsonElement(EdacStartedDto.serializer(), body).id
    }

    override suspend fun training(id: String) = call { service.training(id).orThrow().toDomain() }

    override suspend fun outlets(id: String, step: EdacStep) = call {
        service.step(id, step.index, null).orThrow().outlets.orEmpty().map {
            EdacOutlet(it.customerId, it.code, it.name, it.address, it.due, it.doneSteps.toSet())
        }
    }

    override suspend fun form(id: String, step: EdacStep, customerId: String?) = call {
        val d = service.step(id, step.index, customerId).orThrow()
        EdacStepForm(
            step = step,
            customer = d.customer?.let { StaffBrief(it.id, it.code, it.name) },
            customerAddress = d.customer?.address,
            note = d.note,
            criteria = d.criteria.map { c ->
                EdacCriterion(
                    id = c.id,
                    content = c.content,
                    kind = when (c.kind) {
                        "yes_no" -> EdacKind.YES_NO
                        "text" -> EdacKind.TEXT
                        else -> EdacKind.SCALE
                    },
                    kpi = c.kpi,
                    required = c.required,
                    options = c.options.map { EdacOption(it.value, it.label) },
                    answer = c.answer?.let { EdacAnswer(it.value, it.yes, it.text, it.note) },
                )
            },
            observed = d.observed.orEmpty().map { EdacObserved(it.content, it.yes, it.no) },
        )
    }

    override suspend fun save(
        id: String,
        step: EdacStep,
        customerId: String?,
        answers: Map<String, EdacAnswer>,
        note: String?,
        finish: Boolean,
    ) = call {
        val body = service.act(
            EdacActionDto(
                action = "save",
                id = id,
                step = step.index,
                customerId = customerId,
                answers = answers.map { (criterion, a) ->
                    EdacAnswerInDto(criterion, a.value, a.yes, a.text?.trim(), a.note?.trim()?.takeIf { it.isNotEmpty() })
                },
                note = note?.trim(),
                finish = finish,
            ),
        ).orThrow()
        json.decodeFromJsonElement(EdacSavedDto.serializer(), body).currentStep
    }

    /**
     * Photos first: mgr_edac_finish checks both are in storage, under the
     * supervisor's folder as `<id>/edac-<training>/<file>`.
     */
    override suspend fun finish(id: String, result: String, supervisorPhoto: String, salespersonPhoto: String) = call {
        val me = session.current.value?.id ?: error("Chưa tải được thông tin người dùng")
        val folder = "edac-$id"
        val paths = listOf(supervisorPhoto, salespersonPhoto).map { local ->
            photos.compress(local)
            uploader.upload(me, folder, local)
        }
        service.act(
            EdacActionDto(
                action = "finish", id = id, result = result.trim(),
                supervisorPhoto = paths[0], salespersonPhoto = paths[1],
            ),
        ).orThrow()
        listOf(supervisorPhoto, salespersonPhoto).forEach(uploader::deleteLocal)
        Unit
    }

    override suspend fun history(from: String, to: String) = call {
        service.history(from, to).orThrow().map {
            EdacHistoryItem(
                it.id, it.trainingDate, it.currentStep, it.status == "completed", it.result,
                StaffBrief(it.rep.id, it.rep.code, it.rep.name), it.outlets,
            )
        }
    }

    override suspend fun result(id: String) = call {
        val d = service.result(id).orThrow()
        EdacResult(
            training = d.toDomain(),
            lines = d.answers.map { EdacResultLine(it.step, it.customer, it.content.orEmpty(), it.answer, it.note) },
            notes = d.notes.map { EdacResultLine(it.step, it.customer, "Làm mẫu", it.note, null) },
        )
    }

    override suspend fun criteria() = call {
        val d = service.criteria().orThrow()
        EdacCriteriaBook(
            canEdit = d.canEdit,
            criteria = d.criteria.map { c ->
                EdacCriterionAdmin(
                    id = c.id, step = c.step, code = c.code, content = c.content,
                    kind = when (c.kind) {
                        "yes_no" -> EdacKind.YES_NO
                        "text" -> EdacKind.TEXT
                        else -> EdacKind.SCALE
                    },
                    kpi = c.kpi, required = c.required, active = c.active, answered = c.answered,
                    options = c.options.map { EdacOption(it.value, it.label) },
                )
            },
        )
    }

    override suspend fun saveCriterion(c: EdacCriterionAdmin) = call {
        service.act(
            EdacActionDto(
                action = "criterion",
                criterion = EdacCriterionInDto(
                    id = c.id, step = c.step, content = c.content.trim(),
                    kind = when (c.kind) {
                        EdacKind.YES_NO -> "yes_no"
                        EdacKind.TEXT -> "text"
                        EdacKind.SCALE -> "scale"
                    },
                    kpi = c.kpi?.trim()?.takeIf { it.isNotEmpty() }, required = c.required, active = c.active,
                    options = if (c.kind == EdacKind.SCALE) c.options.map { EdacOptionDto(it.value, it.label.trim()) } else emptyList(),
                ),
            ),
        ).orThrow()
        Unit
    }

    private fun EdacTrainingDto.toDomain() = EdacTraining(
        id = id,
        trainingDate = trainingDate,
        currentStep = currentStep,
        completed = status == "completed",
        checkInAt = checkInAt,
        checkOutAt = checkOutAt,
        result = result,
        supervisorPhotoUrl = supervisorPhotoUrl,
        salespersonPhotoUrl = salespersonPhotoUrl,
        rep = StaffBrief(rep.id, rep.code, rep.name),
        today = today?.toDomain(),
        month = month?.toDomain(),
        outletsDone = outletsDone.mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap(),
    )

    private fun TeamTodayDto.toDomain() = TeamToday(revenue, orderCount, visitDone, visitPlanned, skuPerOrder, activeReps)

    private fun TeamMonthDto.toDomain() = TeamMonth(
        revenue, revenueTarget, orderCount, orderTarget, visitDone, visitPlanned, skuPerOrder, vpo, newCustomers,
    )

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
