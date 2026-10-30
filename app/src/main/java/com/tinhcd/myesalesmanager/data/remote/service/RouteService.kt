package com.tinhcd.myesalesmanager.data.remote.service

import com.tinhcd.myesalesmanager.data.remote.dto.RouteActionDto
import com.tinhcd.myesalesmanager.data.remote.dto.RouteCandidateDto
import com.tinhcd.myesalesmanager.data.remote.dto.RouteDetailDto
import com.tinhcd.myesalesmanager.data.remote.dto.SalesRouteDto
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** `mgr-routes`: the team's routes, their outlets and schedule, reassignment. */
interface RouteService {

    @GET("mgr-routes")
    suspend fun routes(): Response<List<SalesRouteDto>>

    @GET("mgr-routes")
    suspend fun detail(@Query("id") routeId: String): Response<RouteDetailDto>

    @GET("mgr-routes")
    suspend fun candidates(
        @Query("candidates") routeId: String,
        @Query("query") query: String?,
    ): Response<List<RouteCandidateDto>>

    @POST("mgr-routes")
    suspend fun act(@Body body: RouteActionDto): Response<JsonElement>
}
