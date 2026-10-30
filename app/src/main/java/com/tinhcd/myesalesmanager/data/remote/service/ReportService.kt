package com.tinhcd.myesalesmanager.data.remote.service

import com.tinhcd.myesalesmanager.data.remote.dto.ActivitiesReportDto
import com.tinhcd.myesalesmanager.data.remote.dto.OrdersReportDto
import com.tinhcd.myesalesmanager.data.remote.dto.SalesReportDto
import com.tinhcd.myesalesmanager.data.remote.dto.SkuReportDto
import com.tinhcd.myesalesmanager.data.remote.dto.TradeReportDto
import com.tinhcd.myesalesmanager.data.remote.dto.TrainingReportDto
import com.tinhcd.myesalesmanager.data.remote.dto.WorkReportDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** `mgr-reports`: the six SFA reports asked of the team or one rep. */
interface ReportService {

    @GET("mgr-reports?report=activities")
    suspend fun activities(
        @Query("date") date: String,
        @Query("salespersonId") salespersonId: String?,
    ): Response<ActivitiesReportDto>

    @GET("mgr-reports?report=sales")
    suspend fun sales(
        @Query("month") month: String,
        @Query("salespersonId") salespersonId: String?,
    ): Response<SalesReportDto>

    @GET("mgr-reports?report=orders")
    suspend fun orders(
        @Query("fromDate") from: String,
        @Query("toDate") to: String,
        @Query("salespersonId") salespersonId: String?,
    ): Response<OrdersReportDto>

    @GET("mgr-reports?report=sku")
    suspend fun sku(
        @Query("fromDate") from: String,
        @Query("toDate") to: String,
        @Query("salespersonId") salespersonId: String?,
    ): Response<SkuReportDto>

    @GET("mgr-reports?report=trade")
    suspend fun trade(
        @Query("fromDate") from: String,
        @Query("toDate") to: String,
        @Query("salespersonId") salespersonId: String?,
    ): Response<TradeReportDto>

    @GET("mgr-reports?report=work")
    suspend fun work(
        @Query("fromDate") from: String,
        @Query("toDate") to: String,
        @Query("salespersonId") salespersonId: String?,
    ): Response<WorkReportDto>

    @GET("mgr-reports?report=training")
    suspend fun training(
        @Query("fromDate") from: String,
        @Query("toDate") to: String,
        @Query("salespersonId") salespersonId: String?,
    ): Response<TrainingReportDto>
}
