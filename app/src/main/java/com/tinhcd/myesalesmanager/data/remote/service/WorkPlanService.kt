package com.tinhcd.myesalesmanager.data.remote.service

import com.tinhcd.myesalesmanager.data.remote.dto.PlannableCustomerDto
import com.tinhcd.myesalesmanager.data.remote.dto.WorkCategoryDto
import com.tinhcd.myesalesmanager.data.remote.dto.WorkPlanActionDto
import com.tinhcd.myesalesmanager.data.remote.dto.WorkPlanActionResultDto
import com.tinhcd.myesalesmanager.data.remote.dto.WorkPlanDetailDto
import com.tinhcd.myesalesmanager.data.remote.dto.WorkPlanPageDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** `mgr-work-plans`: the team's plans, the assign form and the rulings. */
interface WorkPlanService {

    @GET("mgr-work-plans")
    suspend fun plans(
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String,
        @Query("statuses") statuses: String?,
        @Query("salespersonId") salespersonId: String?,
    ): Response<WorkPlanPageDto>

    @GET("mgr-work-plans")
    suspend fun detail(@Query("id") id: String): Response<WorkPlanDetailDto>

    @GET("mgr-work-plans?categories=1")
    suspend fun categories(): Response<List<WorkCategoryDto>>

    @GET("mgr-work-plans")
    suspend fun customersOf(
        @Query("customersOf") salespersonId: String,
        @Query("query") query: String?,
    ): Response<List<PlannableCustomerDto>>

    @POST("mgr-work-plans")
    suspend fun act(@Body body: WorkPlanActionDto): Response<WorkPlanActionResultDto>
}
