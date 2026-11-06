package com.tinhcd.myesalesmanager.feature.approval

import androidx.compose.foundation.layout.Arrangement
import com.tinhcd.myesalesmanager.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.MonthCalendar
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.daysIn
import com.tinhcd.myesalesmanager.core.ui.formatDay
import com.tinhcd.myesalesmanager.core.ui.formatIsoDate
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.ApprovalStatus
import com.tinhcd.myesalesmanager.domain.model.LeaveApproval
import com.tinhcd.myesalesmanager.domain.repository.ApprovalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class LeaveCalendarState(
    /** Pending and approved requests; a refused or withdrawn one keeps nobody away. */
    val leaves: List<LeaveApproval>? = null,
    val error: String? = null,
    val month: YearMonth = YearMonth.from(todayVn()),
    val day: LocalDate = todayVn(),
) {
    private fun LeaveApproval.span(): Pair<LocalDate, LocalDate>? = runCatching {
        LocalDate.parse(fromDate) to LocalDate.parse(toDate)
    }.getOrNull()

    /** How many people are off (or asking to be) on each day of the month. */
    val marks: Map<LocalDate, Int>
        get() = leaves.orEmpty().flatMap { l -> l.span()?.let { (f, t) -> daysIn(month, f, t).toList() }.orEmpty() }
            .groupingBy { it }.eachCount()

    val onDay: List<LeaveApproval>
        get() = leaves.orEmpty().filter { l -> l.span()?.let { (f, t) -> !day.isBefore(f) && !day.isAfter(t) } == true }
}

@HiltViewModel
class LeaveCalendarViewModel @Inject constructor(
    private val repository: ApprovalRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LeaveCalendarState())
    val state: StateFlow<LeaveCalendarState> = _state.asStateFlow()

    /** Every request the team has; small enough to place on any month without asking again. */
    fun refresh() {
        viewModelScope.launch {
            when (val r = repository.leaves(null)) {
                is DataResult.Success -> _state.update { s ->
                    s.copy(
                        leaves = r.data.items.filter { it.status == ApprovalStatus.PENDING || it.status == ApprovalStatus.APPROVED },
                        error = null,
                    )
                }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }

    fun showMonth(month: YearMonth) {
        val today = todayVn()
        _state.update { it.copy(month = month, day = if (YearMonth.from(today) == month) today else month.atDay(1)) }
    }

    fun selectDay(day: LocalDate) = _state.update { it.copy(day = day) }
}

/** Lịch nghỉ phép (legacy calendar of leave to approve): who is away, day by day. */
@Composable
fun LeaveCalendarScreen(onBack: () -> Unit, viewModel: LeaveCalendarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    SubScreen(title = stringResource(R.string.cal_title), subtitle = stringResource(R.string.cal_subtitle), onBack = onBack) { padding ->
        val leaves = state.leaves
        when {
            leaves == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            leaves == null -> LoadingBox(Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    MonthCalendar(
                        month = state.month,
                        marks = state.marks,
                        selected = state.day,
                        onMonth = viewModel::showMonth,
                        onSelect = viewModel::selectDay,
                        markColor = MaterialTheme.colorScheme.error,
                    )
                }
                item {
                    Text(formatDay(state.day), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                val onDay = state.onDay
                if (onDay.isEmpty()) {
                    item { Text(stringResource(R.string.cal_nobody), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(onDay, key = { it.id }) { l ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(l.salesperson?.name.orEmpty(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${l.typeName} · ${formatIsoDate(l.fromDate)}" +
                                        (if (l.toDate != l.fromDate) " – ${formatIsoDate(l.toDate)}" else "") + stringResource(R.string.cal_days, l.days),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            StatusBadge(l.status)
                        }
                    }
                }
            }
        }
    }
}
