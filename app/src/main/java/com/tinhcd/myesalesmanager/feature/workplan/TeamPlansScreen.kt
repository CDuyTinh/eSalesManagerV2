package com.tinhcd.myesalesmanager.feature.workplan

import androidx.compose.foundation.layout.Arrangement
import com.tinhcd.myesalesmanager.core.ui.AppStrings
import com.tinhcd.myesalesmanager.R
import androidx.compose.ui.res.stringResource
import com.tinhcd.myesalesmanager.core.ui.theme.status
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.MonthCalendar
import com.tinhcd.myesalesmanager.core.ui.SkeletonList
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.daysIn
import com.tinhcd.myesalesmanager.core.ui.formatDay
import com.tinhcd.myesalesmanager.core.ui.formatWindow
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.core.ui.vnDate
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.model.TeamWorkPlan
import com.tinhcd.myesalesmanager.domain.model.WorkPlanStatus
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import com.tinhcd.myesalesmanager.domain.repository.WorkPlanRepository
import com.tinhcd.myesalesmanager.feature.approval.ApprovalCard
import com.tinhcd.myesalesmanager.feature.approval.DecisionDialog
import com.tinhcd.myesalesmanager.feature.approval.LabeledLine
import com.tinhcd.myesalesmanager.feature.approval.PendingDecision
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import com.tinhcd.myesalesmanager.domain.model.ApprovalStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Optional nav argument: open the list already filtered to one status. */
const val ARG_PLAN_STATUS = "status"

/** Optional nav argument: "1" lists the manager's own plans instead of the team's. */
const val ARG_PLAN_MINE = "mine"

data class TeamPlansState(
    /** The manager's own plans (Kế hoạch của tôi) rather than the team's. */
    val mine: Boolean = false,
    val status: WorkPlanStatus? = null,
    val salespersonId: String? = null,
    val team: List<TeamMember> = emptyList(),
    val counts: Map<WorkPlanStatus, Int> = emptyMap(),
    val items: List<TeamWorkPlan> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val pending: PendingDecision? = null,
    val deciding: Boolean = false,
    val message: String? = null,
    /** Month view (legacy task calendar) instead of the list. */
    val calendar: Boolean = false,
    val month: YearMonth = YearMonth.from(todayVn()),
    val day: LocalDate = todayVn(),
) {
    /** How many plans run on each day of the shown month; one spanning days counts on each. */
    val marks: Map<LocalDate, Int>
        get() = items.flatMap { p ->
            val from = vnDate(p.fromAt) ?: return@flatMap emptyList()
            daysIn(month, from, vnDate(p.toAt) ?: from).toList()
        }.groupingBy { it }.eachCount()

    /** What the list shows: everything, or in the month view the picked day's plans. */
    val shown: List<TeamWorkPlan>
        get() = if (!calendar) items else items.filter { p ->
            val from = vnDate(p.fromAt) ?: return@filter false
            !day.isBefore(from) && !day.isAfter(vnDate(p.toAt) ?: from)
        }
}

@HiltViewModel
class TeamPlansViewModel @Inject constructor(
    private val strings: AppStrings,
    savedState: SavedStateHandle,
    private val repository: WorkPlanRepository,
    auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        TeamPlansState(
            mine = savedState.get<String>(ARG_PLAN_MINE) == "1",
            status = savedState.get<String>(ARG_PLAN_STATUS)?.let { WorkPlanStatus.of(it) },
        ),
    )
    val state: StateFlow<TeamPlansState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        // An area manager's team plans include their supervisors' own.
        viewModelScope.launch {
            combine(auth.supervisors, auth.team) { s, r -> s + r }.collect { team -> _state.update { it.copy(team = team) } }
        }
    }

    /**
     * The list: a month either side of today, wide enough to hold this week's
     * work and last month's results. The calendar: the month on show.
     */
    fun refresh() {
        val s = _state.value
        val today = todayVn()
        val (from, to) = if (s.calendar) s.month.atDay(1) to s.month.atEndOfMonth() else today.minusDays(30) to today.plusDays(30)
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = repository.plans(s.status, s.salespersonId, from.toString(), to.toString(), s.mine)) {
                is DataResult.Success -> _state.update {
                    it.copy(loading = false, counts = r.data.counts, items = r.data.items)
                }
                is DataResult.Failure -> _state.update { it.copy(loading = false, error = r.error.toMessage(strings)) }
            }
        }
    }

    fun selectStatus(status: WorkPlanStatus?) {
        _state.update { it.copy(status = status, items = emptyList()) }
        refresh()
    }

    fun selectSalesperson(id: String?) {
        _state.update { it.copy(salespersonId = id, items = emptyList()) }
        refresh()
    }

    fun toggleCalendar() {
        _state.update { it.copy(calendar = !it.calendar, items = emptyList()) }
        refresh()
    }

    /** A new month opens on its first day, or today when it is this month. */
    fun showMonth(month: YearMonth) {
        val today = todayVn()
        _state.update { it.copy(month = month, day = if (YearMonth.from(today) == month) today else month.atDay(1), items = emptyList()) }
        refresh()
    }

    fun selectDay(day: LocalDate) = _state.update { it.copy(day = day) }

    fun ask(pending: PendingDecision) = _state.update { it.copy(pending = pending) }

    fun dismiss() = _state.update { it.copy(pending = null) }

    fun confirm(note: String, @Suppress("UNUSED_PARAMETER") qty: Int?) {
        val pending = _state.value.pending ?: return
        viewModelScope.launch {
            _state.update { it.copy(deciding = true) }
            when (val r = repository.decide(pending.id, pending.approve, note)) {
                is DataResult.Success -> {
                    _state.update {
                        it.copy(deciding = false, pending = null, message = if (pending.approve) strings(R.string.status_approved) else strings(R.string.mgv_1))
                    }
                    refresh()
                }
                is DataResult.Failure -> _state.update { it.copy(deciding = false, message = r.error.toMessage(strings)) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

/** Công việc của nhóm. Legacy MyTask (manager) + TaskApprove in one list. */
@Composable
fun TeamPlansScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onAssign: () -> Unit,
    viewModel: TeamPlansViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }

    SubScreen(title = if (state.mine) stringResource(R.string.mg_316) else stringResource(R.string.mg_314), onBack = onBack, snackbar = snackbar) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.calendar,
                        onClick = viewModel::toggleCalendar,
                        label = { Text(stringResource(R.string.mg_330)) },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    )
                }
                item {
                    FilterChip(
                        selected = state.status == null,
                        onClick = { viewModel.selectStatus(null) },
                        label = { Text(stringResource(R.string.mgt_87, state.counts.values.sum())) },
                    )
                }
                items(WorkPlanStatus.visible) { s ->
                    FilterChip(
                        selected = state.status == s,
                        onClick = { viewModel.selectStatus(s) },
                        label = { Text("${s.label} (${state.counts[s] ?: 0})") },
                    )
                }
            }
            if (!state.mine) LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.salespersonId == null,
                        onClick = { viewModel.selectSalesperson(null) },
                        label = { Text(stringResource(R.string.mg_331)) },
                    )
                }
                items(state.team, key = { it.id }) { m ->
                    FilterChip(
                        selected = state.salespersonId == m.id,
                        onClick = { viewModel.selectSalesperson(m.id) },
                        label = { Text(m.fullName) },
                    )
                }
            }

            when {
                state.loading && state.items.isEmpty() && !state.calendar -> SkeletonList(lines = 3)
                state.error != null && state.items.isEmpty() -> ErrorBox(state.error!!, onRetry = viewModel::refresh)
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.calendar) {
                        item {
                            MonthCalendar(
                                month = state.month,
                                marks = state.marks,
                                selected = state.day,
                                onMonth = viewModel::showMonth,
                                onSelect = viewModel::selectDay,
                            )
                        }
                        item {
                            Text(formatDay(state.day), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (state.shown.isEmpty()) {
                        item {
                            Text(
                                if (state.calendar) stringResource(R.string.mg_332) else stringResource(R.string.mg_333),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = if (state.calendar) 4.dp else 32.dp),
                            )
                        }
                    }
                    items(state.shown, key = { it.id }) { plan ->
                        PlanCard(
                            plan = plan,
                            mine = state.mine,
                            onOpen = { onOpen(plan.id) },
                            onApprove = { viewModel.ask(PendingDecision(plan.id, true, plan.content)) },
                            onReject = { viewModel.ask(PendingDecision(plan.id, false, plan.content)) },
                        )
                    }
                }
            }
        }

        Row(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.End) {
            ExtendedFloatingActionButton(
                onClick = onAssign,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (state.mine) stringResource(R.string.mg_303) else stringResource(R.string.mg_304)) },
            )
        }
    }

    state.pending?.let {
        DecisionDialog(pending = it, busy = state.deciding, onConfirm = viewModel::confirm, onDismiss = viewModel::dismiss)
    }
}

@Composable
private fun PlanCard(
    plan: TeamWorkPlan,
    mine: Boolean,
    onOpen: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    // Reuses the approval card: only a submitted plan of someone below carries
    // the two buttons; the manager's own waits on their boss.
    ApprovalCard(
        status = if (!mine && plan.status == WorkPlanStatus.SUBMITTED) ApprovalStatus.PENDING else ApprovalStatus.APPROVED,
        decisionNote = null,
        onApprove = onApprove,
        onReject = onReject,
        onClick = onOpen,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                plan.content,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            PlanStatusBadge(plan.status)
        }
        if (!mine) LabeledLine(stringResource(R.string.lbl_staff), plan.salesperson?.let { "${it.name} (${it.code})" })
        LabeledLine(stringResource(R.string.lbl_type), plan.categoryName)
        LabeledLine(stringResource(R.string.lbl_period), formatWindow(plan.fromAt, plan.toAt))
        LabeledLine(stringResource(R.string.mg_310), plan.place)
        if (plan.objectCount > 0) {
            LabeledLine(plan.objectKind?.label ?: stringResource(R.string.mg_194), "${plan.objectCount}")
        }
        LabeledLine(
            stringResource(R.string.mg_334),
            when {
                plan.isAssigned -> stringResource(R.string.mgt_88, plan.author?.name ?: stringResource(R.string.mg_335))
                mine -> stringResource(R.string.mg_336)
                else -> stringResource(R.string.mg_337)
            },
        )
        if (plan.repeats) LabeledLine(stringResource(R.string.mg_308), stringResource(R.string.mg_338))
        LabeledLine(stringResource(R.string.mg_76), plan.result)
        LabeledLine(stringResource(R.string.lbl_reason), plan.reason)
    }
}

@Composable
fun PlanStatusBadge(status: WorkPlanStatus) {
    val (bg, fg) = when (status) {
        WorkPlanStatus.SUBMITTED -> MaterialTheme.status.warningContainer to MaterialTheme.status.onWarningContainer
        WorkPlanStatus.APPROVED -> MaterialTheme.status.infoContainer to MaterialTheme.status.onInfoContainer
        WorkPlanStatus.COMPLETED -> MaterialTheme.status.successContainer to MaterialTheme.status.onSuccessContainer
        WorkPlanStatus.REJECTED, WorkPlanStatus.CANCELLED ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        WorkPlanStatus.DRAFT -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(
            status.label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
