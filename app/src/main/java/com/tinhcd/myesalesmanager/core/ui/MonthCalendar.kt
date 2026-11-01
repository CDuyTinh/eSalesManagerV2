package com.tinhcd.myesalesmanager.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

private val weekdays = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

/**
 * A month as a Monday-first grid (the legacy calendars' layout), each day
 * carrying a count — plans that day, people off that day. Picking a day is
 * left to the caller, which shows what that day holds beneath.
 *
 * @param marks how many things fall on each day; days not in it show none.
 */
@Composable
fun MonthCalendar(
    month: YearMonth,
    marks: Map<LocalDate, Int>,
    selected: LocalDate?,
    onMonth: (YearMonth) -> Unit,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    markColor: Color = MaterialTheme.colorScheme.primary,
) {
    val today = todayVn()
    Column(modifier.padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonth(month.minusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Tháng trước")
            }
            Text(
                "Tháng ${month.monthValue}/${month.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onMonth(month.plusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Tháng sau")
            }
        }
        Row {
            weekdays.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        // Blank cells before the 1st so it falls under its weekday.
        val lead = month.atDay(1).dayOfWeek.value - 1
        val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(vertical = 1.dp)) {
                (0 until 7).forEach { i ->
                    val day = week.getOrNull(i)
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (day != null) DayCell(day, marks[day] ?: 0, day == selected, day == today, markColor) { onSelect(day) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, count: Int, selected: Boolean, today: Boolean, markColor: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .then(if (today) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${day.dayOfMonth}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (today) FontWeight.Bold else FontWeight.Normal,
        )
        if (count > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(2.dp).background(markColor, CircleShape).padding(horizontal = 4.dp),
            ) {
                Text("$count", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Every day from [from] to [to] inclusive that falls in [month]. */
fun daysIn(month: YearMonth, from: LocalDate, to: LocalDate): Sequence<LocalDate> {
    val start = maxOf(from, month.atDay(1))
    val end = minOf(to, month.atEndOfMonth())
    return generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }
}
