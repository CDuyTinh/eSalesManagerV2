package com.tinhcd.myesalesmanager.feature.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.PeriodKind
import com.tinhcd.myesalesmanager.domain.model.ReportData
import com.tinhcd.myesalesmanager.domain.model.ReportKind
import com.tinhcd.myesalesmanager.domain.model.ReportPeriod
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import com.tinhcd.myesalesmanager.domain.repository.ReportRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

const val ARG_REPORT = "report"

/** Ready-made windows for the range reports; the legacy screens offered the same few. */
enum class RangePreset(val label: String) {
    THIS_MONTH("Tháng này"),
    LAST_MONTH("Tháng trước"),
    LAST_7("7 ngày"),
    LAST_30("30 ngày"),
    ;

    fun resolve(today: LocalDate): Pair<LocalDate, LocalDate> = when (this) {
        THIS_MONTH -> today.withDayOfMonth(1) to today
        LAST_MONTH -> today.withDayOfMonth(1).minusMonths(1).let { it to it.plusMonths(1).minusDays(1) }
        LAST_7 -> today.minusDays(6) to today
        LAST_30 -> today.minusDays(29) to today
    }
}

data class ReportState(
    val kind: ReportKind,
    val salespersonId: String? = null,
    val team: List<TeamMember> = emptyList(),
    /** The day (DAY reports) or any day of the month (MONTH reports). */
    val anchor: LocalDate = todayVn(),
    val preset: RangePreset = RangePreset.THIS_MONTH,
    val data: ReportData? = null,
    val loading: Boolean = true,
    val error: String? = null,
) {
    val period: ReportPeriod
        get() = when (kind.period) {
            PeriodKind.DAY -> ReportPeriod(anchor.toString(), anchor.toString())
            PeriodKind.MONTH -> anchor.withDayOfMonth(1).let { ReportPeriod(it.toString(), it.plusMonths(1).minusDays(1).toString()) }
            PeriodKind.RANGE -> preset.resolve(todayVn()).let { ReportPeriod(it.first.toString(), it.second.toString()) }
        }
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: ReportRepository,
    auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportState(kind = ReportKind.of(savedState.get<String>(ARG_REPORT))))
    val state: StateFlow<ReportState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch { auth.team.collect { t -> _state.update { it.copy(team = t) } } }
        refresh()
    }

    fun refresh() {
        val s = _state.value
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = repository.load(s.kind, s.period, s.salespersonId)) {
                is DataResult.Success -> _state.update { it.copy(loading = false, data = r.data) }
                is DataResult.Failure -> _state.update { it.copy(loading = false, error = r.error.toMessage()) }
            }
        }
    }

    fun selectSalesperson(id: String?) {
        _state.update { it.copy(salespersonId = id) }
        refresh()
    }

    /** One day or one month back or forward, never past the current one. */
    fun step(forward: Boolean) {
        val s = _state.value
        val next = when (s.kind.period) {
            PeriodKind.DAY -> s.anchor.plusDays(if (forward) 1 else -1)
            PeriodKind.MONTH -> s.anchor.plusMonths(if (forward) 1 else -1)
            PeriodKind.RANGE -> return
        }
        val today = todayVn()
        if (s.kind.period == PeriodKind.DAY && next.isAfter(today)) return
        if (s.kind.period == PeriodKind.MONTH && next.withDayOfMonth(1).isAfter(today)) return
        _state.update { it.copy(anchor = next) }
        refresh()
    }

    fun selectPreset(preset: RangePreset) {
        _state.update { it.copy(preset = preset) }
        refresh()
    }
}
