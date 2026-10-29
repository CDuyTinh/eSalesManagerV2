package com.tinhcd.myesalesmanager.core.ui

import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The app is Vietnam-only (as SFA is), so times are shown and entered in
 * Vietnamese time whatever zone the phone happens to be set to.
 */
val VN_ZONE: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

private val dayTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
private val time = DateTimeFormatter.ofPattern("HH:mm")
private val day = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun parse(iso: String): OffsetDateTime? =
    runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(VN_ZONE).toOffsetDateTime() }.getOrNull()

/** "2026-10-30T01:00:00+00:00" → "30/10/2026 08:00". */
fun formatDateTime(iso: String?): String = iso?.let { parse(it)?.format(dayTime) ?: it }.orEmpty()

/** A plan's window, collapsing the date when it starts and ends the same day. */
fun formatWindow(fromIso: String, toIso: String): String {
    val from = parse(fromIso) ?: return "$fromIso – $toIso"
    val to = parse(toIso) ?: return from.format(dayTime)
    return if (from.toLocalDate() == to.toLocalDate()) {
        "${from.format(day)} · ${from.format(time)}–${to.format(time)}"
    } else {
        "${from.format(dayTime)} – ${to.format(dayTime)}"
    }
}

fun formatDay(date: LocalDate): String = date.format(day)

fun formatTime(value: LocalTime): String = value.format(time)

/** A Vietnamese wall-clock moment as the ISO-8601 instant the server stores. */
fun toIsoInstant(date: LocalDate, at: LocalTime): String =
    date.atTime(at).atZone(VN_ZONE).toOffsetDateTime().toString()

fun todayVn(): LocalDate = LocalDate.now(VN_ZONE)
