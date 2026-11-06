package com.tinhcd.myesalesmanager.feature.report

import androidx.activity.compose.BackHandler
import com.tinhcd.myesalesmanager.feature.common.label
import com.tinhcd.myesalesmanager.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tinhcd.myesalesmanager.core.ui.theme.MyeSalesTheme
import com.tinhcd.myesalesmanager.core.ui.theme.brand
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.domain.model.PeriodKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * The report's filter, opened from the funnel on its toolbar.
 *
 * The legacy app keeps the period off the report itself and asks for it on a
 * page of its own (`daily_activities_filter_form.dart`, `filter_by_month.dart`):
 * a header band with rounded lower corners, the fields in a card over its edge,
 * and one button at the foot. Nothing reloads until that button — a manager
 * picking "from" then "to" should not fetch the half-chosen range in between.
 *
 * What is asked depends on the report's [PeriodKind]: one day, one month, or a
 * from–to range. The team pickers stay on the report, where they always were.
 *
 * @param onApply the day or any day of the month (as anchor), and the range.
 */
@Composable
internal fun ReportFilterScreen(
    kind: PeriodKind,
    anchor: LocalDate,
    from: LocalDate,
    to: LocalDate,
    onDismiss: () -> Unit,
    onApply: (anchor: LocalDate, from: LocalDate, to: LocalDate) -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val today = todayVn()
    var draftDate by rememberSaveable { mutableStateOf(anchor) }
    var draftMonth by rememberSaveable { mutableStateOf(anchor.withDayOfMonth(1)) }
    var draftFrom by rememberSaveable { mutableStateOf(from) }
    var draftTo by rememberSaveable { mutableStateOf(to) }

    val windowError = when {
        draftTo.isAfter(today) -> stringResource(R.string.mg_201)
        draftFrom.isAfter(draftTo) -> stringResource(R.string.mg_202)
        else -> null
    }
    val canApply = when (kind) {
        PeriodKind.DAY -> !draftDate.isAfter(today)
        PeriodKind.MONTH -> !draftMonth.isAfter(today.withDayOfMonth(1))
        PeriodKind.RANGE -> windowError == null
    }

    fun reset() {
        draftDate = today
        draftMonth = today.withDayOfMonth(1)
        draftFrom = today.withDayOfMonth(1)
        draftTo = today
    }

    fun apply() {
        when (kind) {
            PeriodKind.DAY -> onApply(draftDate, from, to)
            PeriodKind.MONTH -> onApply(draftMonth, from, to)
            PeriodKind.RANGE -> onApply(anchor, draftFrom, draftTo)
        }
        onDismiss()
    }

    // A Surface rather than a bare Box: it takes the touches, so nothing on the
    // report underneath can be tapped through the filter.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(FilterHeaderHeight)
                    .background(
                        Brush.linearGradient(listOf(MaterialTheme.brand.header, MaterialTheme.brand.header.darken())),
                        RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    ),
            )
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.mg_203), tint = MaterialTheme.brand.onHeader)
                    }
                    Text(
                        stringResource(R.string.mg_204),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.brand.onHeader,
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                    )
                    TextButton(onClick = ::reset) {
                        Text(stringResource(R.string.mg_205), color = MaterialTheme.brand.onHeader)
                    }
                }

                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    FilterCard(title = titleFor(kind)) {
                        when (kind) {
                            PeriodKind.DAY -> DayFilter(draftDate, today) { draftDate = it }
                            PeriodKind.MONTH -> MonthFilter(draftMonth, today) { draftMonth = it }
                            PeriodKind.RANGE -> WindowFilter(
                                from = draftFrom,
                                to = draftTo,
                                today = today,
                                error = windowError,
                                onFrom = { draftFrom = it },
                                onTo = { draftTo = it },
                                onPreset = {
                                    draftFrom = it.first
                                    draftTo = it.second
                                },
                            )
                        }
                    }
                }

                Button(
                    onClick = ::apply,
                    enabled = canApply,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.brand.header,
                        contentColor = MaterialTheme.brand.onHeader,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .height(50.dp),
                ) { Text(stringResource(R.string.mg_206), style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

/** The band's far corner, a shade deeper, as the legacy header's gradient runs. */
private fun Color.darken(): Color = Color(red * 0.8f, green * 0.8f, blue * 0.8f, alpha)

/** The status bar, the title row, and the top of the card that sits over it. */
private val FilterHeaderHeight = 150.dp

private val FieldDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
private fun titleFor(kind: PeriodKind): String = when (kind) {
    PeriodKind.DAY -> stringResource(R.string.mgc_13)
    PeriodKind.MONTH -> stringResource(R.string.mgc_14)
    PeriodKind.RANGE -> stringResource(R.string.mgc_15)
}

@Composable
private fun FilterCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

// -----------------------------------------------------------------------------
// One day
// -----------------------------------------------------------------------------

@Composable
private fun DayFilter(date: LocalDate, today: LocalDate, onDate: (LocalDate) -> Unit) {
    PresetRow(
        presets = listOf(stringResource(R.string.mg_26) to today, stringResource(R.string.mg_210) to today.minusDays(1)),
        selected = date,
        onSelect = onDate,
    )
    DateField(label = stringResource(R.string.mg_183), date = date, today = today, onDate = onDate)
}

// -----------------------------------------------------------------------------
// One month
// -----------------------------------------------------------------------------

/**
 * A year and its twelve months. Material has no month picker, and a day picker
 * would ask for a day the report then ignores.
 */
@Composable
private fun MonthFilter(month: LocalDate, today: LocalDate, onMonth: (LocalDate) -> Unit) {
    val thisMonth = today.withDayOfMonth(1)
    var year by rememberSaveable { mutableStateOf(month.year) }

    PresetRow(
        presets = listOf(stringResource(R.string.mg_32) to thisMonth, stringResource(R.string.mg_5) to thisMonth.minusMonths(1)),
        selected = month,
        onSelect = {
            year = it.year
            onMonth(it)
        },
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = { year-- }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.mg_211))
        }
        Text(
            stringResource(R.string.mgt_48, year),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { year++ }, enabled = year < today.year) {
            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.mg_212))
        }
    }

    (1..12).chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            row.forEach { m ->
                val candidate = LocalDate.of(year, m, 1)
                val selected = candidate == month
                val enabled = !candidate.isAfter(thisMonth)
                Surface(
                    onClick = { onMonth(candidate) },
                    enabled = enabled,
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) MaterialTheme.brand.header else MaterialTheme.colorScheme.surface,
                    contentColor = when {
                        selected -> MaterialTheme.brand.onHeader
                        enabled -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    },
                    border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.weight(1f).height(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Th $m", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// A range
// -----------------------------------------------------------------------------

@Composable
private fun WindowFilter(
    from: LocalDate,
    to: LocalDate,
    today: LocalDate,
    error: String?,
    onFrom: (LocalDate) -> Unit,
    onTo: (LocalDate) -> Unit,
    onPreset: (Pair<LocalDate, LocalDate>) -> Unit,
) {
    // The same presets the report offered before the filter page existed.
    val presets = RangePreset.entries.map { it.label() to it.resolve(today) }
    PresetRow(presets = presets, selected = from to to, onSelect = onPreset)

    DateField(label = stringResource(R.string.mg_213), date = from, today = today, onDate = onFrom)
    DateField(label = stringResource(R.string.mg_214), date = to, today = today, onDate = onTo)

    if (error != null) {
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}

// -----------------------------------------------------------------------------
// Pieces
// -----------------------------------------------------------------------------

@Composable
private fun <T> PresetRow(presets: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        presets.forEach { (label, value) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label) },
            )
        }
    }
}

/** A labelled date that opens the calendar; days after today cannot be chosen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, date: LocalDate, today: LocalDate, onDate: (LocalDate) -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }

    Surface(
        onClick = { picking = true },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(date.format(FieldDateFormat), style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.brand.header)
        }
    }

    if (picking) {
        val todayMillis = today.toUtcMillis()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.toUtcMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
                override fun isSelectableYear(year: Int) = year <= today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        picking = false
                    },
                ) { Text(stringResource(R.string.mg_3)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = pickerState) }
    }
}

/** The picker counts in UTC midnights, whatever the phone's zone. */
private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

@Preview(name = "Bộ lọc khoảng thời gian", showBackground = true, heightDp = 700)
@Composable
private fun RangeFilterPreview() {
    MyeSalesTheme {
        ReportFilterScreen(
            kind = PeriodKind.RANGE,
            anchor = LocalDate.of(2026, 11, 1),
            from = LocalDate.of(2026, 11, 1),
            to = LocalDate.of(2026, 11, 1),
            onDismiss = {},
            onApply = { _, _, _ -> },
        )
    }
}

@Preview(name = "Bộ lọc tháng", showBackground = true, heightDp = 700)
@Composable
private fun MonthFilterPreview() {
    MyeSalesTheme {
        ReportFilterScreen(
            kind = PeriodKind.MONTH,
            anchor = LocalDate.of(2026, 11, 1),
            from = LocalDate.of(2026, 11, 1),
            to = LocalDate.of(2026, 11, 1),
            onDismiss = {},
            onApply = { _, _, _ -> },
        )
    }
}
