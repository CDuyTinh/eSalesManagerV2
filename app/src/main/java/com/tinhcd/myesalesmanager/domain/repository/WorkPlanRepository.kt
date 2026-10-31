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
        /** The caller's own plans (drafts too) rather than the team's. */
        mine: Boolean = false,
    ): DataResult<WorkPlanPage>

    suspend fun detail(id: String): DataResult<WorkPlanDetail>

    suspend fun categories(): DataResult<List<WorkCategory>>

    suspend fun customersOf(salespersonId: String, query: String?): DataResult<List<PlannableCustomer>>

    suspend fun assign(draft: AssignDraft): DataResult<String>

    /** Rule on a plan the rep submitted. Rejecting needs a reason. */
    suspend fun decide(id: String, approve: Boolean, reason: String?): DataResult<Unit>

    /** Call off a task the supervisor assigned. Needs a reason. */
    suspend fun cancel(id: String, reason: String): DataResult<Unit>

    /** The manager's own plan, sent straight to their boss for approval. @return its id. */
    suspend fun planOwn(draft: AssignDraft): DataResult<String>

    /** Moves the manager's own plan: submit a draft, report it done, or call it off. */
    suspend fun moveOwn(id: String, status: WorkPlanStatus, reason: String?): DataResult<Unit>
}
