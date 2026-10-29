package com.tinhcd.myesalesmanager.feature.workplan

import androidx.compose.foundation.layout.Arrangement
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
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.formatWindow
import com.tinhcd.myesalesmanager.core.ui.todayVn
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Optional nav argument: open the list already filtered to one status. */
const val ARG_PLAN_STATUS = "status"

data class TeamPlansState(
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
)

@HiltViewModel
class TeamPlansViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: WorkPlanRepository,
    auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        TeamPlansState(status = savedState.get<String>(ARG_PLAN_STATUS)?.let { WorkPlanStatus.of(it) }),
    )
    val state: StateFlow<TeamPlansState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch { auth.team.collect { team -> _state.update { it.copy(team = team) } } }
    }

    /**
     * A month either side of today. Wide enough to hold this week's work and
     * last month's results; the legacy list defaulted to the current month.
     */
    fun refresh() {
        val s = _state.value
        val today = todayVn()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = repository.plans(s.status, s.salespersonId, today.minusDays(30).toString(), today.plusDays(30).toString())) {
                is DataResult.Success -> _state.update {
                    it.copy(loading = false, counts = r.data.counts, items = r.data.items)
                }
                is DataResult.Failure -> _state.update { it.copy(loading = false, error = r.error.toMessage()) }
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

    fun ask(pending: PendingDecision) = _state.update { it.copy(pending = pending) }

    fun dismiss() = _state.update { it.copy(pending = null) }

    fun confirm(note: String, @Suppress("UNUSED_PARAMETER") qty: Int?) {
        val pending = _state.value.pending ?: return
        viewModelScope.launch {
            _state.update { it.copy(deciding = true) }
            when (val r = repository.decide(pending.id, pending.approve, note)) {
                is DataResult.Success -> {
                    _state.update {
                        it.copy(deciding = false, pending = null, message = if (pending.approve) "Đã duyệt" else "Đã từ chối")
                    }
                    refresh()
                }
                is DataResult.Failure -> _state.update { it.copy(deciding = false, message = r.error.toMessage()) }
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

    SubScreen(title = "Công việc của nhóm", onBack = onBack, snackbar = snackbar) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.status == null,
                        onClick = { viewModel.selectStatus(null) },
                        label = { Text("Tất cả (${state.counts.values.sum()})") },
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
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.salespersonId == null,
                        onClick = { viewModel.selectSalesperson(null) },
                        label = { Text("Cả nhóm") },
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
                state.loading && state.items.isEmpty() -> LoadingBox()
                state.error != null && state.items.isEmpty() -> ErrorBox(state.error!!, onRetry = viewModel::refresh)
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.items.isEmpty()) {
                        item {
                            Text(
                                "Không có công việc nào",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 32.dp),
                            )
                        }
                    }
                    items(state.items, key = { it.id }) { plan ->
                        PlanCard(
                            plan = plan,
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
                text = { Text("Giao việc") },
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
    onOpen: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    // Reuses the approval card: only a submitted plan carries the two buttons.
    ApprovalCard(
        status = if (plan.status == WorkPlanStatus.SUBMITTED) ApprovalStatus.PENDING else ApprovalStatus.APPROVED,
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
        LabeledLine("Nhân viên", plan.salesperson?.let { "${it.name} (${it.code})" })
        LabeledLine("Loại", plan.categoryName)
        LabeledLine("Thời gian", formatWindow(plan.fromAt, plan.toAt))
        LabeledLine("Địa điểm", plan.place)
        if (plan.objectCount > 0) {
            LabeledLine(plan.objectKind?.label ?: "Đối tượng", "${plan.objectCount}")
        }
        LabeledLine(
            "Nguồn",
            if (plan.isAssigned) "Giao bởi ${plan.author?.name ?: "cấp trên"}" else "Nhân viên tự lập",
        )
        LabeledLine("Kết quả", plan.result)
        LabeledLine("Lý do", plan.reason)
    }
}

@Composable
fun PlanStatusBadge(status: WorkPlanStatus) {
    val (bg, fg) = when (status) {
        WorkPlanStatus.SUBMITTED -> Color(0xFFFFF3E0) to Color(0xFFB45309)
        WorkPlanStatus.APPROVED -> Color(0xFFE3F2FD) to Color(0xFF0B57A4)
        WorkPlanStatus.COMPLETED -> Color(0xFFE6F4EA) to Color(0xFF1E7B34)
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
