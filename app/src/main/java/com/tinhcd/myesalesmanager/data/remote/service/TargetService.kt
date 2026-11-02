package com.tinhcd.myesalesmanager.data.remote.service

import com.tinhcd.myesalesmanager.data.remote.dto.SaveTargetsDto
import com.tinhcd.myesalesmanager.data.remote.dto.TargetSheetDto
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** `mgr-targets`: a rep's monthly targets, overall and by product group / product. */
interface TargetService {

    @GET("mgr-targets")
    suspend fun sheet(
        @Query("month") month: String,
        @Query("salespersonId") salespersonId: String,
    ): Response<TargetSheetDto>

    @POST("mgr-targets")
    suspend fun save(@Body body: SaveTargetsDto): Response<JsonElement>
}
