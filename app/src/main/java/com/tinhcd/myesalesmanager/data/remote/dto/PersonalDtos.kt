package com.tinhcd.myesalesmanager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The SFA app's own endpoints, so these mirror its DTOs field for field.

@Serializable
data class WorkDayBranchDto(
    val id: String,
    val code: String,
    val name: String,
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)

@Serializable
data class WorkDayDto(
    @SerialName("work_date") val workDate: String,
    val branch: WorkDayBranchDto,
    @SerialName("check_in_at") val checkInAt: String? = null,
    @SerialName("check_out_at") val checkOutAt: String? = null,
    @SerialName("open_visits") val openVisits: Int = 0,
)

@Serializable
data class WorkDayPunchDto(
    val type: String,
    @SerialName("work_date") val workDate: String,
    @SerialName("happened_at") val happenedAt: String,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerialName("accuracy_m") val accuracyM: Double? = null,
    @SerialName("distance_m") val distanceM: Double? = null,
)

@Serializable
data class LeaveTypeDto(
    @SerialName("leave_type_id") val leaveTypeId: String,
    val code: String,
    val name: String,
    @SerialName("is_paid") val isPaid: Boolean = true,
)

@Serializable
data class LeaveRequestDto(
    @SerialName("request_id") val requestId: String,
    @SerialName("type_name") val typeName: String,
    @SerialName("is_paid") val isPaid: Boolean = true,
    @SerialName("from_date") val fromDate: String,
    @SerialName("to_date") val toDate: String,
    val reason: String = "",
    val status: String = "pending",
    @SerialName("decision_note") val decisionNote: String? = null,
    @SerialName("decided_at") val decidedAt: String? = null,
)

@Serializable
data class LeaveBoardDto(
    val types: List<LeaveTypeDto> = emptyList(),
    val requests: List<LeaveRequestDto> = emptyList(),
)

@Serializable
data class NewLeaveRequestDto(
    @SerialName("leave_type_id") val leaveTypeId: String,
    @SerialName("from_date") val fromDate: String,
    @SerialName("to_date") val toDate: String,
    val reason: String,
)

@Serializable
data class WithdrawLeaveDto(@SerialName("request_id") val requestId: String)

@Serializable
data class WorkNoteDto(
    @SerialName("note_id") val noteId: String,
    val title: String,
    val body: String? = null,
    @SerialName("due_on") val dueOn: String? = null,
    val status: String = "open",
    val result: String? = null,
    @SerialName("done_at") val doneAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class WorkNotesDto(val notes: List<WorkNoteDto> = emptyList())

@Serializable
data class NewWorkNoteDto(
    val title: String,
    val body: String? = null,
    @SerialName("due_on") val dueOn: String? = null,
)

@Serializable
data class CompleteWorkNoteDto(
    @SerialName("note_id") val noteId: String,
    val result: String,
)

@Serializable
data class NotificationDto(
    val kind: String,
    @SerialName("source_id") val sourceId: String,
    val title: String,
    val body: String = "",
    @SerialName("from_date") val fromDate: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
)

@Serializable
data class NotificationFeedDto(
    val items: List<NotificationDto> = emptyList(),
    val unread: Int = 0,
)

@Serializable
data class ReadNotificationDto(
    val kind: String? = null,
    val sourceId: String? = null,
    val all: Boolean? = null,
)

@Serializable
data class NotificationReadAckDto(val marked: Int = 0, val unread: Int = 0)
