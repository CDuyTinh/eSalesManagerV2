package com.tinhcd.myesalesmanager.data.remote.service

import com.tinhcd.myesalesmanager.data.remote.dto.CompetitorSurveyDto
import com.tinhcd.myesalesmanager.data.remote.dto.InStoreActionDto
import com.tinhcd.myesalesmanager.data.remote.dto.MarketSurveysDto
import com.tinhcd.myesalesmanager.data.remote.dto.PosmListDto
import com.tinhcd.myesalesmanager.data.remote.dto.PromotionPreviewDto
import com.tinhcd.myesalesmanager.data.remote.dto.VisitProductsDto
import com.tinhcd.myesalesmanager.data.remote.dto.WorkWithActionResultDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** `mgr-work-with`'s in-store half: what a work-with call can record at the outlet. */
interface InStoreService {

    @GET("mgr-work-with?products=1")
    suspend fun products(@Query("visit") visitId: String): Response<VisitProductsDto>

    @GET("mgr-work-with?posm=1")
    suspend fun posm(@Query("visit") visitId: String): Response<PosmListDto>

    @GET("mgr-work-with?surveys=1")
    suspend fun surveys(@Query("visit") visitId: String): Response<MarketSurveysDto>

    @GET("mgr-work-with")
    suspend fun survey(@Query("visit") visitId: String, @Query("survey") surveyId: String): Response<CompetitorSurveyDto>

    @POST("mgr-work-with")
    suspend fun preview(@Body body: InStoreActionDto): Response<PromotionPreviewDto>

    @POST("mgr-work-with")
    suspend fun act(@Body body: InStoreActionDto): Response<WorkWithActionResultDto>
}
