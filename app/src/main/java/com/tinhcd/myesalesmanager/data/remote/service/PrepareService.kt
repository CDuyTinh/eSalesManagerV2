package com.tinhcd.myesalesmanager.data.remote.service

import com.tinhcd.myesalesmanager.data.remote.dto.CustomerEditDto
import com.tinhcd.myesalesmanager.data.remote.dto.CustomerUpdateDto
import com.tinhcd.myesalesmanager.data.remote.dto.CustomerUpdatedDto
import com.tinhcd.myesalesmanager.data.remote.dto.SalesFocusDto
import com.tinhcd.myesalesmanager.data.remote.dto.SiteStockDto
import com.tinhcd.myesalesmanager.data.remote.dto.TeamCustomerDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Chuẩn bị and the outlet editor: the SFA app's `site-stock` (already scoped to
 * the caller's distributor), `mgr-sales-focus` and `mgr-customers`.
 */
interface PrepareService {

    @GET("site-stock")
    suspend fun stock(@Query("site_id") siteId: String?): Response<SiteStockDto>

    @GET("mgr-sales-focus")
    suspend fun salesFocus(@Query("date") date: String): Response<SalesFocusDto>

    @GET("mgr-customers")
    suspend fun customers(@Query("rep") repId: String, @Query("query") query: String?): Response<List<TeamCustomerDto>>

    @GET("mgr-customers")
    suspend fun customer(@Query("id") id: String): Response<CustomerEditDto>

    @POST("mgr-customers")
    suspend fun updateCustomer(@Body body: CustomerUpdateDto): Response<CustomerUpdatedDto>
}
