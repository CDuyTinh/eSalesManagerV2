package com.tinhcd.myesalesmanager.feature.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.DatePickDialog
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.Kpi
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.StatusPill
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.formatIsoDate
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.LeaveBoard
import com.tinhcd.myesalesmanager.domain.model.MyLeave
import com.tinhcd.myesalesmanager.domain.repository.PersonalRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LeaveDraft(
    val typeId: String? = null,
    val from: LocalDate = todayVn().plusDays(1),
    val to: LocalDate = todayVn().plusDays(1),
    val reason: String = "",
)

data class MyLeaveState(
    val board: LeaveBoard? = null,
    val error: String? = null,
    val draft: LeaveDraft? = null,
    val withdrawing: MyLeave? = null,
    val saving: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class MyLeaveViewModel @Inject constructor(
    private val repository: PersonalRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MyLeaveState())
    val state: StateFlow<MyLeaveState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            when (val r = repository.leave()) {
                is DataResult.Success -> _state.update { it.copy(board = r.data, error = null) }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }

    fun newRequest() = _state.update { it.copy(draft = LeaveDraft(typeId = it.board?.types?.firstOrNull()?.id)) }
    fun edit(change: (LeaveDraft) -> LeaveDraft) = _state.update { s -> s.copy(draft = s.draft?.let(change)) }
    fun askWithdraw(l: MyLeave) = _state.update { it.copy(withdrawing = l) }
    fun dismiss() = _state.update { if (it.saving) it else it.copy(draft = null, withdrawing = null) }

    fun submit() {
        val d = _state.value.draft ?: return
        val problem = when {
            d.typeId == null -> "Chọn loại nghỉ"
            d.to.isBefore(d.from) -> "Ngày kết thúc phải từ ngày bắt đầu trở đi"
            d.reason.isBlank() -> "Nhập lý do nghỉ"
            else -> null
        }
        if (problem != null) {
            _state.update { it.copy(message = problem) }
            return
        }
        run("Đã gửi đơn nghỉ phép") { repository.requestLeave(d.typeId!!, d.from.toString(), d.to.toString(), d.reason) }
    }

    fun withdraw() {
        val l = _state.value.withdrawing ?: return
        run("Đã rút đơn") { repository.withdrawLeave(l.id) }
    }

    private fun run(done: String, block: suspend () -> DataResult<Unit>) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val r = block()) {
                is DataResult.Success -> {
                    _state.update { it.copy(saving = false, draft = null, withdrawing = null, message = done) }
                    refresh()
                }
                is DataResult.Failure -> _state.update { it.copy(saving = false, message = r.error.toMessage()) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

/** Nghỉ phép của tôi: the manager's own requests (SFA leave_request). */
@Composable
fun MyLeaveScreen(onBack: () -> Unit, viewModel: MyLeaveViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() } }

    SubScreen(title = "Nghỉ phép của tôi", onBack = onBack, snackbar = snackbar) { padding ->
        val board = state.board
        when {
            board == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            board == null -> LoadingBox(Modifier.padding(padding))
            else -> Box(Modifier.padding(padding).fillMaxSize()) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        // There is no AM level yet, so nobody in the app rules on a
                        // supervisor's leave; head office does, as before.
                        Text(
                            "Đơn của giám sát do cấp quản lý trên (ASM) duyệt.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (board.requests.isEmpty()) {
                        item { Text("Chưa có đơn nghỉ phép nào", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(board.requests, key = { it.id }) { l -> LeaveCard(l) { viewModel.askWithdraw(l) } }
                }
                ExtendedFloatingActionButton(
                    onClick = viewModel::newRequest,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Xin nghỉ") },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }
        }
    }

    state.draft?.let { d -> state.board?.let { LeaveDialog(d, it, state.saving, viewModel) } }
    state.withdrawing?.let { l ->
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Rút đơn") },
            text = { Text("Rút đơn ${l.typeName} ${formatIsoDate(l.fromDate)} – ${formatIsoDate(l.toDate)}?") },
            confirmButton = { TextButton(onClick = viewModel::withdraw, enabled = !state.saving) { Text("Rút đơn") } },
            dismissButton = { TextButton(onClick = viewModel::dismiss, enabled = !state.saving) { Text("Huỷ") } },
        )
    }
}

@Composable
private fun LeaveCard(l: MyLeave, onWithdraw: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l.typeName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                val (label, color) = when (l.status) {
                    "approved" -> "Đã duyệt" to Kpi.Green.first()
                    "rejected" -> "Từ chối" to Kpi.Orange.first()
                    "cancelled" -> "Đã rút" to Kpi.Slate.first()
                    "pending" -> "Chờ duyệt" to Kpi.Blue.first()
                    else -> l.status to Kpi.Slate.first()
                }
                StatusPill(label, color)
            }
            Text("${formatIsoDate(l.fromDate)} – ${formatIsoDate(l.toDate)} · ${if (l.isPaid) "có lương" else "không lương"}",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (l.reason.isNotBlank()) Text(l.reason, style = MaterialTheme.typography.bodyLarge)
            l.decisionNote?.let { Text("Ghi chú duyệt: $it", style = MaterialTheme.typography.labelSmall) }
            if (l.status == "pending") OutlinedButton(onClick = onWithdraw) { Text("Rút đơn") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LeaveDialog(d: LeaveDraft, board: LeaveBoard, saving: Boolean, viewModel: MyLeaveViewModel) {
    var picking by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = viewModel::dismiss,
        title = { Text("Xin nghỉ phép") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    board.types.forEach { t ->
                        FilterChip(selected = d.typeId == t.id, onClick = { viewModel.edit { it.copy(typeId = t.id) } }, label = { Text(t.name) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picking = "from" }, modifier = Modifier.weight(1f)) { Text("Từ ${formatIsoDate(d.from.toString())}") }
                    OutlinedButton(onClick = { picking = "to" }, modifier = Modifier.weight(1f)) { Text("Đến ${formatIsoDate(d.to.toString())}") }
                }
                OutlinedTextField(
                    value = d.reason,
                    onValueChange = { v -> viewModel.edit { it.copy(reason = v) } },
                    label = { Text("Lý do") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = viewModel::submit, enabled = !saving) { Text(if (saving) "Đang gửi…" else "Gửi đơn") } },
        dismissButton = { TextButton(onClick = viewModel::dismiss, enabled = !saving) { Text("Huỷ") } },
    )
    when (picking) {
        "from" -> DatePickDialog(d.from, { v -> viewModel.edit { it.copy(from = v, to = if (it.to.isBefore(v)) v else it.to) } }) { picking = null }
        "to" -> DatePickDialog(d.to, { v -> viewModel.edit { it.copy(to = v) } }) { picking = null }
    }
}
