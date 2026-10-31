package com.tinhcd.myesalesmanager.data.repository

import com.tinhcd.myesalesmanager.core.photo.PhotoStore
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.data.remote.dto.AuditPhotoDto
import com.tinhcd.myesalesmanager.data.remote.dto.InStoreActionDto
import com.tinhcd.myesalesmanager.data.remote.dto.OrderDto
import com.tinhcd.myesalesmanager.data.remote.dto.OrderLineDto
import com.tinhcd.myesalesmanager.data.remote.dto.PosmCheckDto
import com.tinhcd.myesalesmanager.data.remote.dto.QtyLineDto
import com.tinhcd.myesalesmanager.data.remote.dto.StockCountDto
import com.tinhcd.myesalesmanager.data.remote.dto.SurveyAnswerInDto
import com.tinhcd.myesalesmanager.data.remote.dto.SurveySubmitDto
import com.tinhcd.myesalesmanager.data.remote.http.PostgrestException
import com.tinhcd.myesalesmanager.data.remote.http.orThrow
import com.tinhcd.myesalesmanager.data.remote.service.InStoreService
import com.tinhcd.myesalesmanager.data.remote.storage.PhotoUploader
import com.tinhcd.myesalesmanager.data.session.SessionStore
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.CompetitorSurvey
import com.tinhcd.myesalesmanager.domain.model.MarketSurvey
import com.tinhcd.myesalesmanager.domain.model.PosmCheck
import com.tinhcd.myesalesmanager.domain.model.PosmPlaced
import com.tinhcd.myesalesmanager.domain.model.ProductUnit
import com.tinhcd.myesalesmanager.domain.model.PromotionPreview
import com.tinhcd.myesalesmanager.domain.model.QtyLine
import com.tinhcd.myesalesmanager.domain.model.SurveyCriterion
import com.tinhcd.myesalesmanager.domain.model.SurveyPairing
import com.tinhcd.myesalesmanager.domain.model.VisitProduct
import com.tinhcd.myesalesmanager.domain.model.VisitProducts
import com.tinhcd.myesalesmanager.domain.repository.InStoreRepository
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InStoreRepositoryImpl @Inject constructor(
    private val service: InStoreService,
    private val uploader: PhotoUploader,
    private val photos: PhotoStore,
    private val session: SessionStore,
) : InStoreRepository {

    private fun now() = OffsetDateTime.now().toString()

    override suspend fun products(visitId: String) = call {
        val d = service.products(visitId).orThrow()
        VisitProducts(
            customerId = d.customerId,
            previousCountDate = d.previousCountDate,
            products = d.products.map { p ->
                VisitProduct(
                    p.id, p.code, p.name, p.baseUom, p.category, p.previousBaseQty,
                    p.units.map { ProductUnit(it.uomCode, it.uomName, it.conversionRate, it.price, it.countedQty) },
                )
            },
        )
    }

    override suspend fun countStock(visitId: String, lines: List<QtyLine>) = call {
        service.act(
            InStoreActionDto(
                action = "stock_count",
                count = StockCountDto(
                    id = UUID.randomUUID().toString(),
                    visitId = visitId,
                    countDate = todayVn().toString(),
                    clientCreatedAt = now(),
                    lines = lines.map { QtyLineDto(it.productId, it.uomCode, it.qty) },
                ),
            ),
        ).orThrow()
        Unit
    }

    override suspend fun preview(visitId: String, lines: List<QtyLine>) = call {
        val d = service.preview(
            InStoreActionDto(action = "preview", visitId = visitId, lines = lines.map { QtyLineDto(it.productId, it.uomCode, it.qty) }),
        ).orThrow()
        PromotionPreview(
            totalDiscount = d.totalDiscount,
            earned = d.earned.map { e ->
                listOfNotNull(e.sequenceName?.takeIf { it.isNotBlank() }, e.breakName?.takeIf { it.isNotBlank() })
                    .joinToString(" – ")
            },
        )
    }

    override suspend fun order(visitId: String, lines: List<QtyLine>, clientTotal: Long, note: String?) = call {
        val id = UUID.randomUUID().toString()
        service.act(
            InStoreActionDto(
                action = "order",
                order = OrderDto(
                    id = id,
                    visitId = visitId,
                    orderDate = todayVn().toString(),
                    note = note?.trim()?.takeIf { it.isNotEmpty() },
                    clientTotalAmount = clientTotal,
                    clientCreatedAt = now(),
                    lines = lines.mapIndexed { i, l -> OrderLineDto(i + 1, l.productId, l.uomCode, l.qty) },
                ),
            ),
        ).orThrow()
        id
    }

    override suspend fun posm(visitId: String) = call {
        service.posm(visitId).orThrow().placed.map {
            PosmPlaced(
                it.programId, it.programCode, it.itemId, it.itemCode, it.itemName, it.unitName,
                it.placedQty, it.countedQty, it.condition, it.remark, it.photoCount,
            )
        }
    }

    /**
     * Photos first: submit_posm_check refuses a path that is not in storage yet.
     * They go under the supervisor's own folder, which is the one the bucket
     * policy lets them write and the check lets them cite.
     */
    override suspend fun checkPosm(check: PosmCheck) = call {
        val me = session.current.value?.id ?: error("Chưa tải được thông tin người dùng")
        val uploaded = check.photoPaths.map { local ->
            val size = photos.compress(local)
            AuditPhotoDto(uploader.upload(me, check.visitId, local), size)
        }
        service.act(
            InStoreActionDto(
                action = "posm_check",
                check = PosmCheckDto(
                    id = UUID.randomUUID().toString(),
                    visitId = check.visitId,
                    programId = check.item.programId,
                    itemId = check.item.itemId,
                    countedQty = check.countedQty,
                    condition = check.condition.code,
                    remark = check.remark?.trim()?.takeIf { it.isNotEmpty() },
                    clientCreatedAt = now(),
                    photos = uploaded,
                ),
            ),
        ).orThrow()
        check.photoPaths.forEach(uploader::deleteLocal)
        Unit
    }

    override suspend fun surveys(visitId: String) = call {
        service.surveys(visitId).orThrow().surveys.map {
            MarketSurvey(it.kind, it.id, it.code, it.name, it.toDate, it.isCompleted, it.itemCount)
        }
    }

    override suspend fun survey(visitId: String, surveyId: String) = call {
        val d = service.survey(visitId, surveyId).orThrow()
        CompetitorSurvey(
            id = d.id,
            code = d.code,
            name = d.name,
            criteria = d.criteria.map { SurveyCriterion(it.id, it.name, it.hint, it.isRequired) },
            pairings = d.items.map { p ->
                SurveyPairing(
                    p.productId, p.productName, p.competitorProductId, p.competitorProduct, p.competitorName,
                    p.answers.mapValues { it.value.content },
                )
            },
        )
    }

    override suspend fun submitSurvey(
        visitId: String,
        surveyId: String,
        answers: Map<Triple<String, String, String>, String>,
    ) = call {
        service.act(
            InStoreActionDto(
                action = "survey",
                survey = SurveySubmitDto(
                    visitId = visitId,
                    surveyId = surveyId,
                    clientCreatedAt = now(),
                    answers = answers.filterValues { it.isNotBlank() }.map { (k, v) ->
                        SurveyAnswerInDto(k.first, k.second, k.third, v.trim())
                    },
                ),
            ),
        ).orThrow()
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
