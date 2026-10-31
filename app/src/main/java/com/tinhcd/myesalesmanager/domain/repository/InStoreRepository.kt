package com.tinhcd.myesalesmanager.domain.repository

import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.CompetitorSurvey
import com.tinhcd.myesalesmanager.domain.model.MarketSurvey
import com.tinhcd.myesalesmanager.domain.model.PosmCheck
import com.tinhcd.myesalesmanager.domain.model.PosmPlaced
import com.tinhcd.myesalesmanager.domain.model.PromotionPreview
import com.tinhcd.myesalesmanager.domain.model.QtyLine
import com.tinhcd.myesalesmanager.domain.model.VisitProducts

/** The in-store steps of the supervisor's own work-with call. */
interface InStoreRepository {
    suspend fun products(visitId: String): DataResult<VisitProducts>

    /** Replaces the call's stock count with these lines. */
    suspend fun countStock(visitId: String, lines: List<QtyLine>): DataResult<Unit>

    suspend fun preview(visitId: String, lines: List<QtyLine>): DataResult<PromotionPreview>

    /** Books the order to the rep. @return the order id. */
    suspend fun order(visitId: String, lines: List<QtyLine>, clientTotal: Long, note: String?): DataResult<String>

    suspend fun posm(visitId: String): DataResult<List<PosmPlaced>>

    /** Uploads the photos, then records the check. */
    suspend fun checkPosm(check: PosmCheck): DataResult<Unit>

    suspend fun surveys(visitId: String): DataResult<List<MarketSurvey>>

    suspend fun survey(visitId: String, surveyId: String): DataResult<CompetitorSurvey>

    /** [answers]: (product id, competitor product id, criterion id) → content. */
    suspend fun submitSurvey(
        visitId: String,
        surveyId: String,
        answers: Map<Triple<String, String, String>, String>,
    ): DataResult<Unit>
}
