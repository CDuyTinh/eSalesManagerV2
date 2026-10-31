package com.tinhcd.myesalesmanager.feature.edac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.Kpi
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.StatusPill
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.EdacCriteriaBook
import com.tinhcd.myesalesmanager.domain.model.EdacCriterionAdmin
import com.tinhcd.myesalesmanager.domain.model.EdacKind
import com.tinhcd.myesalesmanager.domain.model.EdacOption
import com.tinhcd.myesalesmanager.domain.model.EdacStep
import com.tinhcd.myesalesmanager.domain.repository.EdacRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The steps that carry criteria; Làm mẫu takes a note and Hoàn tất a result. */
private val criteriaSteps = EdacStep.entries.filter { it.index in setOf(0, 1, 2, 4, 5, 6) }

private val EdacKind.label: String
    get() = when (this) {
        EdacKind.SCALE -> "Thang điểm"
        EdacKind.YES_NO -> "Có / Không"
        EdacKind.TEXT -> "Văn bản"
    }

data class EdacCriteriaState(
    val book: EdacCriteriaBook? = null,
    val editing: EdacCriterionAdmin? = null,
    val error: String? = null,
    val saving: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class EdacCriteriaViewModel @Inject constructor(
    private val repository: EdacRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EdacCriteriaState())
    val state: StateFlow<EdacCriteriaState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            when (val r = repository.criteria()) {
                is DataResult.Success -> _state.update { it.copy(book = r.data, error = null) }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }

    fun open(c: EdacCriterionAdmin) = _state.update { it.copy(editing = c) }

    fun new(step: Int) = _state.update {
        it.copy(
            editing = EdacCriterionAdmin(
                id = null, step = step, code = "", content = "", kind = EdacKind.SCALE, kpi = null,
                required = true, active = true, answered = false,
                options = listOf(EdacOption(1, ""), EdacOption(2, ""), EdacOption(3, "")),
            ),
        )
    }

    fun edit(change: (EdacCriterionAdmin) -> EdacCriterionAdmin) = _state.update { s -> s.copy(editing = s.editing?.let(change)) }
    fun dismiss() = _state.update { if (it.saving) it else it.copy(editing = null) }

    fun save() {
        val c = _state.value.editing ?: return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val r = repository.saveCriterion(c)) {
                is DataResult.Success -> {
                    _state.update { it.copy(saving = false, editing = null, message = "Đã lưu tiêu chí") }
                    load()
                }
                is DataResult.Failure -> _state.update { it.copy(saving = false, message = r.error.toMessage()) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

/**
 * Bộ tiêu chí EDAC (legacy OM20130): what each step asks. An AM or RM edits it;
 * an SS sees what their coaching days are scored against.
 */
@Composable
fun EdacCriteriaScreen(onBack: () -> Unit, viewModel: EdacCriteriaViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() } }

    val book = state.book
    SubScreen(
        title = "Bộ tiêu chí EDAC",
        subtitle = book?.let { if (it.canEdit) "Bấm để sửa" else "Chỉ xem — quản lý khu vực / vùng sửa" },
        onBack = onBack,
        snackbar = snackbar,
    ) { padding ->
        when {
            book == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            book == null -> LoadingBox(Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                criteriaSteps.forEach { step ->
                    item(key = "h${step.index}") {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            Text("${step.index + 1}. ${step.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (book.canEdit) {
                                TextButton(onClick = { viewModel.new(step.index) }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Text(" Thêm")
                                }
                            }
                        }
                    }
                    items(book.criteria.filter { it.step == step.index }, key = { it.id!! }) { c ->
                        Card(
                            onClick = { viewModel.open(c) },
                            enabled = book.canEdit,
                            modifier = Modifier.fillMaxWidth().alpha(if (c.active) 1f else 0.55f),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(c.content, style = MaterialTheme.typography.bodyLarge)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusPill(c.kind.label, Kpi.Blue.first())
                                    if (c.required) StatusPill("Bắt buộc", Kpi.Orange.first())
                                    if (!c.active) StatusPill("Ngừng dùng", Kpi.Slate.first())
                                    c.kpi?.let { StatusPill("KPI $it", Kpi.Violet.first()) }
                                }
                                if (c.kind == EdacKind.SCALE) {
                                    Text(
                                        c.options.joinToString(" · ") { "${it.value}. ${it.label}" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    state.editing?.let { CriterionDialog(it, state.saving, viewModel) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CriterionDialog(c: EdacCriterionAdmin, saving: Boolean, viewModel: EdacCriteriaViewModel) {
    AlertDialog(
        onDismissRequest = viewModel::dismiss,
        title = { Text(if (c.id == null) "Thêm tiêu chí — ${EdacStep.of(c.step).title}" else "Sửa tiêu chí ${c.code}") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    c.content, { v -> viewModel.edit { it.copy(content = v) } },
                    label = { Text("Nội dung") }, minLines = 2, modifier = Modifier.fillMaxWidth(),
                )
                // A criterion already answered keeps its kind, or old answers would mean nothing.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EdacKind.entries.forEach { k ->
                        FilterChip(
                            selected = c.kind == k,
                            onClick = { viewModel.edit { it.copy(kind = k) } },
                            enabled = !c.answered || c.kind == k,
                            label = { Text(k.label) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bắt buộc", modifier = Modifier.weight(1f))
                    Switch(checked = c.required, onCheckedChange = { v -> viewModel.edit { it.copy(required = v) } })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Đang dùng", modifier = Modifier.weight(1f))
                    Switch(checked = c.active, onCheckedChange = { v -> viewModel.edit { it.copy(active = v) } })
                }
                if (c.kind == EdacKind.SCALE) {
                    Text("Các mức (mức đã được chấm không xoá được)", style = MaterialTheme.typography.labelSmall)
                    c.options.forEachIndexed { i, o ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${o.value}.", modifier = Modifier.width(28.dp))
                            OutlinedTextField(
                                o.label,
                                { v -> viewModel.edit { e -> e.copy(options = e.options.toMutableList().also { it[i] = o.copy(label = v) }) } },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { viewModel.edit { e -> e.copy(options = e.options.filterIndexed { j, _ -> j != i }) } },
                                enabled = c.options.size > 2,
                            ) { Icon(Icons.Default.Close, contentDescription = "Bỏ mức") }
                        }
                    }
                    TextButton(onClick = {
                        viewModel.edit { e -> e.copy(options = e.options + EdacOption((e.options.maxOfOrNull { it.value } ?: 0) + 1, "")) }
                    }) { Text("+ Thêm mức") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::save, enabled = !saving && c.content.isNotBlank()) { Text(if (saving) "Đang lưu…" else "Lưu") }
        },
        dismissButton = { TextButton(onClick = viewModel::dismiss, enabled = !saving) { Text("Huỷ") } },
    )
}
