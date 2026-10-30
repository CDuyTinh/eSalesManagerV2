package com.tinhcd.myesalesmanager.data.remote.service

import com.tinhcd.myesalesmanager.data.remote.dto.WorkWithActionDto
import com.tinhcd.myesalesmanager.data.remote.dto.WorkWithActionResultDto
import com.tinhcd.myesalesmanager.data.remote.dto.WorkWithRouteDto
import com.tinhcd.myesalesmanager.data.remote.dto.WorkWithVisitDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** `mgr-work-with`: the rep's route, the supervisor's calls, display scores. */
interface WorkWithService {

    @GET("mgr-work-with")
    suspend fun route(@Query("rep") repId: String, @Query("date") date: String): Response<WorkWithRouteDto>

    @GET("mgr-work-with")
    suspend fun visit(@Query("visit") visitId: String): Response<WorkWithVisitDto>

    @POST("mgr-work-with")
    suspend fun act(@Body body: WorkWithActionDto): Response<WorkWithActionResultDto>
}
