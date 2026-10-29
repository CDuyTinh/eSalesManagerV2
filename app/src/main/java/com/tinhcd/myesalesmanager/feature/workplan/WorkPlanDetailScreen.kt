package com.tinhcd.myesalesmanager.feature.workplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.formatDateTime
import com.tinhcd.myesalesmanager.core.ui.formatWindow
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.WorkPlanDetail
import com.tinhcd.myesalesmanager.domain.model.WorkPlanStatus
import com.tinhcd.myesalesmanager.domain.repository.WorkPlanRepository
import com.tinhcd.myesalesmanager.feature.approval.DecisionButtons
import com.tinhcd.myesalesmanager.feature.approval.DecisionDialog
import com.tinhcd.myesalesmanager.feature.approval.LabeledLine
import com.tinhcd.myesalesmanager.feature.approval.PendingDecision
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ARG_PLAN_ID = "planId"

/** Which confirmation is open: a ruling on a submitted plan, or calling one off. */
private const val CANCEL = "cancel"

data class PlanDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val detail: WorkPlanDetail? = null,
    val pending: PendingDecision? = null,
    val cancelling: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class WorkPlanDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: WorkPlanRepository,
) : ViewModel() {

    private val id: String = checkNotNull(savedState[ARG_PLAN_ID])

    private val _state = MutableStateFlow(PlanDetailState())
    val state: StateFlow<PlanDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = repository.detail(id)) {
                is DataResult.Success -> _state.update { it.copy(loading = false, detail = r.data) }
                is DataResult.Failure -> _state.update { it.copy(loading = false, error = r.error.toMessage()) }
            }
        }
    }

    fun ask(approve: Boolean) {
        val d = _state.value.detail ?: return
        _state.update { it.copy(pending = PendingDecision(id, approve, d.plan.content), cancelling = false) }
    }

    /** Calling off reads as a refusal: the dialog demands a reason before it confirms. */
    fun askCancel() {
        val d = _state.value.detail ?: return
        _state.update {
            it.copy(
                pending = PendingDecision(
                    CANCEL, false, d.plan.content,
                    heading = "Huỷ công việc", reasonLabel = "Lý do huỷ *",
                ),
                cancelling = true,
            )
        }
    }

    fun dismiss() = _state.update { it.copy(pending = null, cancelling = false) }

    fun confirm(note: String, @Suppress("UNUSED_PARAMETER") qty: Int?) {
        val s = _state.value
        val pending = s.pending ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val result = if (s.cancelling) {
                repository.cancel(id, note)
            } else {
                repository.decide(id, pending.approve, note)
            }
            when (result) {
                is DataResult.Success -> {
                    _state.update {
                        it.copy(
                            busy = false, pending = null, cancelling = false,
                            message = when {
                                s.cancelling -> "Đã huỷ công việc"
                                pending.approve -> "Đã duyệt"
                                else -> "Đã từ chối"
                            },
                        )
                    }
                    load()
                }
                is DataResult.Failure -> _state.update { it.copy(busy = false, message = result.error.toMessage()) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

@Composable
fun WorkPlanDetailScreen(
    onBack: () -> Unit,
    viewModel: WorkPlanDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }

    val plan = state.detail?.plan
    val canDecide = plan?.status == WorkPlanStatus.SUBMITTED
    val canCancel = plan != null && plan.assignedByMe &&
        plan.status in setOf(WorkPlanStatus.APPROVED, WorkPlanStatus.SUBMITTED) && !canDecide

    SubScreen(
        title = "Chi tiết công việc",
        subtitle = plan?.salesperson?.name,
        onBack = onBack,
        snackbar = snackbar,
        bottomBar = {
            if (canDecide || canCancel) {
                Surface(tonalElevation = 3.dp) {
                    val mod = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)
                    if (canDecide) {
                        DecisionButtons(onApprove = { viewModel.ask(true) }, onReject = { viewModel.ask(false) }, modifier = mod)
                    } else {
                        OutlinedButton(
                            onClick = viewModel::askCancel,
                            modifier = mod.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) { Text("Huỷ công việc đã giao") }
                    }
                }
            }
        },
    ) { padding ->
        val detail = state.detail
        when {
            state.loading && detail == null -> LoadingBox(Modifier.padding(padding))
            detail == null -> ErrorBox(state.error ?: "Không tải được công việc", onRetry = viewModel::load, modifier = Modifier.padding(padding))
            else -> DetailBody(detail, padding)
        }
    }

    state.pending?.let {
        DecisionDialog(pending = it, busy = state.busy, onConfirm = viewModel::confirm, onDismiss = viewModel::dismiss)
    }
}

@Composable
private fun DetailBody(detail: WorkPlanDetail, padding: PaddingValues) {
    val p = detail.plan
    LazyColumn(
        modifier = Modifier.padding(padding).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.content, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        PlanStatusBadge(p.status)
                    }
                    LabeledLine("Nhân viên", p.salesperson?.let { "${it.name} (${it.code})" })
                    LabeledLine("Loại công việc", p.categoryName)
                    LabeledLine("Thời gian", formatWindow(p.fromAt, p.toAt))
                    LabeledLine("Địa điểm", p.place)
                    LabeledLine("Ghi chú", detail.note)
                    LabeledLine(
                        "Nguồn",
                        if (p.isAssigned) "Giao bởi ${p.author?.name ?: "cấp trên"}" else "Nhân viên tự lập",
                    )
                    LabeledLine("Kết quả", p.result)
                    LabeledLine("Lý do", p.reason)
                }
            }
        }

        if (detail.objects.isNotEmpty()) {
            item { Section("${p.objectKind?.label ?: "Đối tượng"} (${detail.objects.size})") }
            items(detail.objects, key = { it.targetId }) { o ->
                Column {
                    Text(listOfNotNull(o.code, o.name).joinToString(" – "), style = MaterialTheme.typography.bodyLarge)
                    o.address?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (detail.photos.isNotEmpty()) {
            item {
                Section("Hình ảnh (${detail.photos.size})")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(detail.photos) { photo ->
                        AsyncImage(
                            model = photo.url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.width(160.dp).height(120.dp).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
            }
        }

        if (detail.history.isNotEmpty()) {
            item { Section("Lịch sử") }
            items(detail.history) { h ->
                Column {
                    Text(
                        "${formatDateTime(h.at)} · ${h.status.label}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        listOfNotNull(h.actor, h.reason).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
}
