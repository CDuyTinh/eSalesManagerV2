package com.tinhcd.myesalesmanager.data.remote.service

import com.tinhcd.myesalesmanager.data.remote.dto.TeamDashboardDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** `mgr-dashboard`: the team's Tổng quan in one call. */
interface DashboardService {

    @GET("mgr-dashboard")
    suspend fun overview(
        @Query("date") date: String,
        @Query("salespersonId") salespersonId: String?,
    ): Response<TeamDashboardDto>
}
