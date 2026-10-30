package com.tinhcd.myesalesmanager.data.remote.service

import com.tinhcd.myesalesmanager.data.remote.dto.CompleteWorkNoteDto
import com.tinhcd.myesalesmanager.data.remote.dto.LeaveBoardDto
import com.tinhcd.myesalesmanager.data.remote.dto.NewLeaveRequestDto
import com.tinhcd.myesalesmanager.data.remote.dto.NewWorkNoteDto
import com.tinhcd.myesalesmanager.data.remote.dto.NotificationFeedDto
import com.tinhcd.myesalesmanager.data.remote.dto.NotificationReadAckDto
import com.tinhcd.myesalesmanager.data.remote.dto.ReadNotificationDto
import com.tinhcd.myesalesmanager.data.remote.dto.WithdrawLeaveDto
import com.tinhcd.myesalesmanager.data.remote.dto.WorkDayDto
import com.tinhcd.myesalesmanager.data.remote.dto.WorkDayPunchDto
import com.tinhcd.myesalesmanager.data.remote.dto.WorkNotesDto
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * The SFA endpoints for one's own day, leave, notes and bell. They act on the
 * caller, whom they find through current_salesperson_id(), so a supervisor gets
 * their own data and never a team member's.
 */
interface PersonalService {

    @GET("work-day")
    suspend fun workDay(@Query("date") date: String): Response<WorkDayDto>

    @POST("submit-work-day")
    suspend fun punch(@Body body: WorkDayPunchDto): Response<JsonElement>

    @GET("leave-requests")
    suspend fun leave(): Response<LeaveBoardDto>

    @POST("leave-requests")
    suspend fun requestLeave(@Body body: NewLeaveRequestDto): Response<JsonElement>

    @PATCH("leave-requests")
    suspend fun withdrawLeave(@Body body: WithdrawLeaveDto): Response<JsonElement>

    @GET("work-notes")
    suspend fun notes(): Response<WorkNotesDto>

    @POST("work-notes")
    suspend fun addNote(@Body body: NewWorkNoteDto): Response<JsonElement>

    @PATCH("work-notes")
    suspend fun completeNote(@Body body: CompleteWorkNoteDto): Response<JsonElement>

    @DELETE("work-notes")
    suspend fun deleteNote(@Query("id") id: String): Response<JsonElement>

    @GET("notifications")
    suspend fun notifications(): Response<NotificationFeedDto>

    @POST("read-notification")
    suspend fun read(@Body body: ReadNotificationDto): Response<NotificationReadAckDto>
}
