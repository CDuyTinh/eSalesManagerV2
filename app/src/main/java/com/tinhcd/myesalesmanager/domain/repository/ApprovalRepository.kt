package com.tinhcd.myesalesmanager.domain.repository

import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.ApprovalCounts
import com.tinhcd.myesalesmanager.domain.model.ApprovalPage
import com.tinhcd.myesalesmanager.domain.model.ApprovalStatus
import com.tinhcd.myesalesmanager.domain.model.CustomerApproval
import com.tinhcd.myesalesmanager.domain.model.CustomerApprovalDetail
import com.tinhcd.myesalesmanager.domain.model.Decision
import com.tinhcd.myesalesmanager.domain.model.LeaveApproval
import com.tinhcd.myesalesmanager.domain.model.TradeApproval
import com.tinhcd.myesalesmanager.domain.model.TradeKind

/**
 * Everything the supervisor rules on for their team. The server scopes every
 * call to the caller's team; nothing here filters by rep.
 */
interface ApprovalRepository {
    suspend fun counts(): DataResult<ApprovalCounts>

    suspend fun leaves(status: ApprovalStatus?): DataResult<ApprovalPage<LeaveApproval>>

    suspend fun customers(status: ApprovalStatus?, query: String?): DataResult<ApprovalPage<CustomerApproval>>

    suspend fun customerDetail(id: String): DataResult<CustomerApprovalDetail>

    suspend fun trades(kind: TradeKind, status: ApprovalStatus?): DataResult<ApprovalPage<TradeApproval>>

    suspend fun decideLeave(id: String, decision: Decision): DataResult<Unit>

    suspend fun decideCustomer(id: String, decision: Decision): DataResult<Unit>

    suspend fun decideTrade(kind: TradeKind, id: String, decision: Decision): DataResult<Unit>
}
