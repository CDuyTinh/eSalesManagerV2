package com.tinhcd.myesalesmanager.data.remote.service

import com.tinhcd.myesalesmanager.data.remote.dto.EdacActionDto
import com.tinhcd.myesalesmanager.data.remote.dto.EdacHistoryDto
import com.tinhcd.myesalesmanager.data.remote.dto.EdacSessionDto
import com.tinhcd.myesalesmanager.data.remote.dto.EdacStepDto
import com.tinhcd.myesalesmanager.data.remote.dto.EdacTrainingDto
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** `mgr-edac`: coaching plans, the eight steps, closing, history. */
interface EdacService {

    @GET("mgr-edac")
    suspend fun sessions(@Query("date") date: String): Response<List<EdacSessionDto>>

    @GET("mgr-edac")
    suspend fun training(@Query("id") id: String): Response<EdacTrainingDto>

    @GET("mgr-edac")
    suspend fun step(
        @Query("id") id: String,
        @Query("step") step: Int,
        @Query("customer") customerId: String?,
    ): Response<EdacStepDto>

    @GET("mgr-edac?history=1")
    suspend fun history(@Query("from") from: String, @Query("to") to: String): Response<List<EdacHistoryDto>>

    @GET("mgr-edac")
    suspend fun result(@Query("result") id: String): Response<EdacTrainingDto>

    @POST("mgr-edac")
    suspend fun act(@Body body: EdacActionDto): Response<JsonElement>
}
