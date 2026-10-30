package com.tinhcd.myesalesmanager.domain.repository

import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.EdacAnswer
import com.tinhcd.myesalesmanager.domain.model.EdacHistoryItem
import com.tinhcd.myesalesmanager.domain.model.EdacOutlet
import com.tinhcd.myesalesmanager.domain.model.EdacResult
import com.tinhcd.myesalesmanager.domain.model.EdacSession
import com.tinhcd.myesalesmanager.domain.model.EdacStep
import com.tinhcd.myesalesmanager.domain.model.EdacStepForm
import com.tinhcd.myesalesmanager.domain.model.EdacTraining

/** Huấn luyện EDAC: the supervisor's coaching days. */
interface EdacRepository {
    suspend fun sessions(date: String): DataResult<List<EdacSession>>

    /** Begins (or reopens) a day, from a plan or straight from a rep. @return the training id. */
    suspend fun start(repId: String?, planId: String?): DataResult<String>

    suspend fun training(id: String): DataResult<EdacTraining>

    suspend fun outlets(id: String, step: EdacStep): DataResult<List<EdacOutlet>>

    suspend fun form(id: String, step: EdacStep, customerId: String?): DataResult<EdacStepForm>

    /**
     * Saves what was answered; [finish] also moves the day past the step.
     * @return the day's current step afterwards.
     */
    suspend fun save(
        id: String,
        step: EdacStep,
        customerId: String?,
        answers: Map<String, EdacAnswer>,
        note: String?,
        finish: Boolean,
    ): DataResult<Int>

    /** Uploads both photos, then closes the day and its plan. */
    suspend fun finish(id: String, result: String, supervisorPhoto: String, salespersonPhoto: String): DataResult<Unit>

    suspend fun history(from: String, to: String): DataResult<List<EdacHistoryItem>>

    suspend fun result(id: String): DataResult<EdacResult>
}
