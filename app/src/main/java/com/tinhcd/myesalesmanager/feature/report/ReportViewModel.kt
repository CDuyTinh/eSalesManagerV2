package com.tinhcd.myesalesmanager.feature.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.OrdersReport
import com.tinhcd.myesalesmanager.domain.model.PeriodKind
import com.tinhcd.myesalesmanager.domain.model.ReportData
import com.tinhcd.myesalesmanager.domain.model.ReportKind
import com.tinhcd.myesalesmanager.domain.model.ReportPeriod
import com.tinhcd.myesalesmanager.domain.model.PlacePick
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.model.branchIds
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
    val supervisorId: String? = null,
    val place: PlacePick? = null,
    val team: List<TeamMember> = emptyList(),
    val supervisors: List<TeamMember> = emptyList(),
    /** The day (DAY reports) or any day of the month (MONTH reports). */
    val anchor: LocalDate = todayVn(),
    /** RANGE reports: the window picked on the filter page, this month so far until then. */
    val rangeFrom: LocalDate = todayVn().withDayOfMonth(1),
    val rangeTo: LocalDate = todayVn(),
    val data: ReportData? = null,
    val loading: Boolean = true,
    val error: String? = null,
    /** The next page of orders, or every page before an export. */
    val loadingMore: Boolean = false,
) {
    val period: ReportPeriod
        get() = when (kind.period) {
            PeriodKind.DAY -> ReportPeriod(anchor.toString(), anchor.toString())
            PeriodKind.MONTH -> anchor.withDayOfMonth(1).let { ReportPeriod(it.toString(), it.plusMonths(1).minusDays(1).toString()) }
            PeriodKind.RANGE -> ReportPeriod(rangeFrom.toString(), rangeTo.toString())
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
        viewModelScope.launch { auth.supervisors.collect { s -> _state.update { it.copy(supervisors = s) } } }
        refresh()
    }

    fun refresh() {
        val s = _state.value
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = repository.load(s.kind, s.period, s.salespersonId, s.supervisorId, s.place.branchIds(s.team + s.supervisors))) {
                is DataResult.Success -> _state.update { it.copy(loading = false, data = r.data) }
                is DataResult.Failure -> _state.update { it.copy(loading = false, error = r.error.toMessage()) }
            }
        }
    }

    /** Appends the next page of orders to the ones shown. */
    fun moreOrders() {
        val s = _state.value
        val shown = s.data as? OrdersReport ?: return
        if (s.loadingMore || shown.items.size >= shown.total) return
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            when (val r = nextPage(s, shown.items.size)) {
                is DataResult.Success -> _state.update { cur ->
                    val now = cur.data as? OrdersReport
                    cur.copy(loadingMore = false, data = now?.copy(items = now.items + r.data.items) ?: cur.data)
                }
                is DataResult.Failure -> _state.update { it.copy(loadingMore = false, error = r.error.toMessage()) }
            }
        }
    }

    /**
     * The report as it should go out: for orders, every page, not just the ones
     * scrolled to. Hands null to [onReady] if a page could not be fetched.
     */
    fun forExport(onReady: (ReportData?) -> Unit) {
        val s = _state.value
        val data = s.data ?: return onReady(null)
        if (data !is OrdersReport || data.items.size >= data.total) return onReady(data)
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            var all: OrdersReport = data
            while (all.items.size < all.total) {
                val r = nextPage(s, all.items.size)
                if (r !is DataResult.Success || r.data.items.isEmpty()) {
                    _state.update { it.copy(loadingMore = false) }
                    return@launch onReady(null)
                }
                all = all.copy(items = all.items + r.data.items)
            }
            _state.update { it.copy(loadingMore = false, data = all) }
            onReady(all)
        }
    }

    private suspend fun nextPage(s: ReportState, offset: Int) =
        repository.moreOrders(s.period, s.salespersonId, s.supervisorId, s.place.branchIds(s.team + s.supervisors), offset)

    fun selectSalesperson(id: String?) {
        _state.update { it.copy(salespersonId = id) }
        refresh()
    }

    fun selectSupervisor(id: String?) {
        _state.update { it.copy(supervisorId = id, salespersonId = null) }
        refresh()
    }

    /** A region, area or distributor; the supervisor and rep picked below it are let go. */
    fun selectPlace(place: PlacePick?) {
        _state.update { it.copy(place = place, supervisorId = null, salespersonId = null) }
        refresh()
    }

    /**
     * The period chosen on the filter page: a day, any day of a month, or a
     * from–to window, by the report's kind. Never past today.
     */
    fun applyPeriod(anchor: LocalDate, from: LocalDate, to: LocalDate) {
        val today = todayVn()
        if (anchor.isAfter(today) || to.isAfter(today) || from.isAfter(to)) return
        _state.update { it.copy(anchor = anchor, rangeFrom = from, rangeTo = to) }
        refresh()
    }
}
