package com.tinhcd.myesalesmanager.domain.repository

import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.AssignDraft
import com.tinhcd.myesalesmanager.domain.model.PlannableCustomer
import com.tinhcd.myesalesmanager.domain.model.WorkCategory
import com.tinhcd.myesalesmanager.domain.model.WorkPlanDetail
import com.tinhcd.myesalesmanager.domain.model.WorkPlanPage
import com.tinhcd.myesalesmanager.domain.model.WorkPlanStatus

/** The team's work plans. The server scopes every call to the caller's team. */
interface WorkPlanRepository {
    suspend fun plans(
        status: WorkPlanStatus?,
        salespersonId: String?,
        fromDate: String,
        toDate: String,
    ): DataResult<WorkPlanPage>

    suspend fun detail(id: String): DataResult<WorkPlanDetail>

    suspend fun categories(): DataResult<List<WorkCategory>>

    suspend fun customersOf(salespersonId: String, query: String?): DataResult<List<PlannableCustomer>>

    suspend fun assign(draft: AssignDraft): DataResult<String>

    /** Rule on a plan the rep submitted. Rejecting needs a reason. */
    suspend fun decide(id: String, approve: Boolean, reason: String?): DataResult<Unit>

    /** Call off a task the supervisor assigned. Needs a reason. */
    suspend fun cancel(id: String, reason: String): DataResult<Unit>
}
