package com.tinhcd.myesalesmanager.feature.personal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.tinhcd.myesalesmanager.core.ui.formatDateTime
import com.tinhcd.myesalesmanager.core.ui.formatIsoDate
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.WorkNote
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

data class NoteDraft(val title: String = "", val body: String = "", val dueOn: LocalDate? = null)

data class WorkNotesState(
    val notes: List<WorkNote>? = null,
    val showDone: Boolean = false,
    val error: String? = null,
    val draft: NoteDraft? = null,
    val completing: WorkNote? = null,
    val deleting: WorkNote? = null,
    val saving: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class WorkNotesViewModel @Inject constructor(
    private val repository: PersonalRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkNotesState())
    val state: StateFlow<WorkNotesState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            when (val r = repository.notes()) {
                is DataResult.Success -> _state.update { it.copy(notes = r.data, error = null) }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }

    fun showDone(v: Boolean) = _state.update { it.copy(showDone = v) }
    fun newNote() = _state.update { it.copy(draft = NoteDraft()) }
    fun edit(change: (NoteDraft) -> NoteDraft) = _state.update { s -> s.copy(draft = s.draft?.let(change)) }
    fun askComplete(n: WorkNote) = _state.update { it.copy(completing = n) }
    fun askDelete(n: WorkNote) = _state.update { it.copy(deleting = n) }
    fun dismiss() = _state.update { if (it.saving) it else it.copy(draft = null, completing = null, deleting = null) }

    fun add() {
        val d = _state.value.draft ?: return
        if (d.title.isBlank()) {
            _state.update { it.copy(message = "Nhập nội dung công việc") }
            return
        }
        run("Đã thêm ghi chú") { repository.addNote(d.title, d.body, d.dueOn?.toString()) }
    }

    fun complete(result: String) {
        val n = _state.value.completing ?: return
        if (result.isBlank()) {
            _state.update { it.copy(message = "Nhập kết quả") }
            return
        }
        run("Đã hoàn thành") { repository.completeNote(n.id, result) }
    }

    fun delete() {
        val n = _state.value.deleting ?: return
        run("Đã xoá ghi chú") { repository.deleteNote(n.id) }
    }

    private fun run(done: String, block: suspend () -> DataResult<Unit>) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val r = block()) {
                is DataResult.Success -> {
                    _state.update { it.copy(saving = false, draft = null, completing = null, deleting = null, message = done) }
                    refresh()
                }
                is DataResult.Failure -> _state.update { it.copy(saving = false, message = r.error.toMessage()) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

/** Ghi chú công việc: the manager's own to-do list (SFA work_note). */
@Composable
fun WorkNotesScreen(onBack: () -> Unit, viewModel: WorkNotesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() } }

    SubScreen(title = "Ghi chú công việc", onBack = onBack, snackbar = snackbar) { padding ->
        val notes = state.notes
        when {
            notes == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            notes == null -> LoadingBox(Modifier.padding(padding))
            else -> Box(Modifier.padding(padding).fillMaxSize()) {
                val shown = notes.filter { it.done == state.showDone }
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { FilterChip(!state.showDone, { viewModel.showDone(false) }, { Text("Đang làm (${notes.count { !it.done }})") }) }
                            item { FilterChip(state.showDone, { viewModel.showDone(true) }, { Text("Đã xong (${notes.count { it.done }})") }) }
                        }
                    }
                    if (shown.isEmpty()) {
                        item { Text("Không có ghi chú", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    items(shown, key = { it.id }) { n ->
                        NoteCard(n, onComplete = { viewModel.askComplete(n) }, onDelete = { viewModel.askDelete(n) })
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = viewModel::newNote,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Thêm ghi chú") },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }
        }
    }

    state.draft?.let { NoteDialog(it, state.saving, viewModel) }
    state.completing?.let { n ->
        var result by remember(n.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Hoàn thành") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(n.title, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(result, { result = it }, label = { Text("Kết quả") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.complete(result) }, enabled = !state.saving) { Text("Lưu") } },
            dismissButton = { TextButton(onClick = viewModel::dismiss, enabled = !state.saving) { Text("Huỷ") } },
        )
    }
    state.deleting?.let { n ->
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Xoá ghi chú") },
            text = { Text("Xoá \"${n.title}\"?") },
            confirmButton = { TextButton(onClick = viewModel::delete, enabled = !state.saving) { Text("Xoá") } },
            dismissButton = { TextButton(onClick = viewModel::dismiss, enabled = !state.saving) { Text("Huỷ") } },
        )
    }
}

@Composable
private fun NoteCard(n: WorkNote, onComplete: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(n.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                n.body?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                n.dueOn?.let { due ->
                    val late = !n.done && LocalDate.parse(due).isBefore(todayVn())
                    StatusPill("Hạn ${formatIsoDate(due)}", if (late) Kpi.Orange.first() else Kpi.Slate.first())
                }
                if (n.done) {
                    Text("Kết quả: ${n.result.orEmpty()}", style = MaterialTheme.typography.labelSmall)
                    n.doneAt?.let { Text("Xong lúc ${formatDateTime(it)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if (!n.done) IconButton(onClick = onComplete) { Icon(Icons.Default.CheckCircle, contentDescription = "Hoàn thành", tint = Kpi.Green.first()) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Xoá") }
        }
    }
}

@Composable
private fun NoteDialog(d: NoteDraft, saving: Boolean, viewModel: WorkNotesViewModel) {
    var picking by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = viewModel::dismiss,
        title = { Text("Thêm ghi chú") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(d.title, { v -> viewModel.edit { it.copy(title = v) } }, label = { Text("Công việc") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(d.body, { v -> viewModel.edit { it.copy(body = v) } }, label = { Text("Chi tiết") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { picking = true }) {
                    Text(d.dueOn?.let { "Hạn ${formatIsoDate(it.toString())}" } ?: "Chọn hạn (không bắt buộc)")
                }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::add, enabled = !saving) { Text(if (saving) "Đang lưu…" else "Lưu") } },
        dismissButton = { TextButton(onClick = viewModel::dismiss, enabled = !saving) { Text("Huỷ") } },
    )
    if (picking) DatePickDialog(d.dueOn ?: todayVn(), { v -> viewModel.edit { it.copy(dueOn = v) } }) { picking = false }
}
