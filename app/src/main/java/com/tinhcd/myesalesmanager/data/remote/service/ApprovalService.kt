package com.tinhcd.myesalesmanager.data.remote.service

import com.tinhcd.myesalesmanager.data.remote.dto.ApprovalCountsDto
import com.tinhcd.myesalesmanager.data.remote.dto.ApprovalPageDto
import com.tinhcd.myesalesmanager.data.remote.dto.CustomerApprovalDetailDto
import com.tinhcd.myesalesmanager.data.remote.dto.CustomerApprovalDto
import com.tinhcd.myesalesmanager.data.remote.dto.DecisionRequestDto
import com.tinhcd.myesalesmanager.data.remote.dto.DecisionResultDto
import com.tinhcd.myesalesmanager.data.remote.dto.LeaveApprovalDto
import com.tinhcd.myesalesmanager.data.remote.dto.TradeApprovalDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** `mgr-approvals`: the whole approval tab behind one Edge Function. */
interface ApprovalService {

    @GET("mgr-approvals")
    suspend fun counts(): Response<ApprovalCountsDto>

    @GET("mgr-approvals?kind=leave")
    suspend fun leaves(
        @Query("statuses") statuses: String?,
    ): Response<ApprovalPageDto<LeaveApprovalDto>>

    @GET("mgr-approvals?kind=customer")
    suspend fun customers(
        @Query("statuses") statuses: String?,
        @Query("query") query: String?,
    ): Response<ApprovalPageDto<CustomerApprovalDto>>

    @GET("mgr-approvals?kind=customer")
    suspend fun customerDetail(@Query("id") id: String): Response<CustomerApprovalDetailDto>

    @GET("mgr-approvals?kind=trade")
    suspend fun trades(
        @Query("types") types: String,
        @Query("statuses") statuses: String?,
    ): Response<ApprovalPageDto<TradeApprovalDto>>

    @POST("mgr-approvals")
    suspend fun decide(@Body body: DecisionRequestDto): Response<DecisionResultDto>
}
