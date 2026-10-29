package com.tinhcd.myesalesmanager.feature.workplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import com.tinhcd.myesalesmanager.core.ui.PrimaryButton
import com.tinhcd.myesalesmanager.core.ui.SearchBox
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.VN_ZONE
import com.tinhcd.myesalesmanager.core.ui.formatDay
import com.tinhcd.myesalesmanager.core.ui.formatTime
import com.tinhcd.myesalesmanager.core.ui.toIsoInstant
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.AssignDraft
import com.tinhcd.myesalesmanager.domain.model.Manager
import com.tinhcd.myesalesmanager.domain.model.PlannableCustomer
import com.tinhcd.myesalesmanager.domain.model.SessionState
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.model.WorkCategory
import com.tinhcd.myesalesmanager.domain.model.WorkObjectKind
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import com.tinhcd.myesalesmanager.domain.repository.WorkPlanRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** Something a task can name, whatever its kind. */
data class ObjectOption(val id: String, val title: String, val subtitle: String?)

data class AssignState(
    val me: Manager? = null,
    val team: List<TeamMember> = emptyList(),
    val categories: List<WorkCategory> = emptyList(),
    val salespersonId: String? = null,
    val categoryId: String? = null,
    val date: LocalDate = todayVn().plusDays(1),
    val from: LocalTime = LocalTime.of(8, 0),
    val to: LocalTime = LocalTime.of(12, 0),
    val content: String = "",
    val place: String = "",
    val note: String = "",
    val query: String = "",
    val customers: List<PlannableCustomer> = emptyList(),
    val loadingCustomers: Boolean = false,
    val selected: Set<String> = emptySet(),
    val saving: Boolean = false,
    val message: String? = null,
    val done: Boolean = false,
) {
    val category: WorkCategory? get() = categories.firstOrNull { it.id == categoryId }
    val rep: TeamMember? get() = team.firstOrNull { it.id == salespersonId }
    val objectKind: WorkObjectKind? get() = category?.objectKind

    /** The choices for the chosen work type, which depend on who the task is for. */
    val options: List<ObjectOption>
        get() = when (objectKind) {
            WorkObjectKind.CUSTOMER -> customers.map { ObjectOption(it.id, "${it.code} – ${it.name}", it.address) }
            WorkObjectKind.SALESPERSON -> buildList {
                me?.let { add(ObjectOption(it.id, "${it.fullName} (tôi)", it.code)) }
                team.filter { it.id != salespersonId }
                    .forEach { add(ObjectOption(it.id, it.fullName, it.code)) }
            }
            WorkObjectKind.BRANCH -> rep?.branchId?.let {
                listOf(ObjectOption(it, "Nhà phân phối của nhân viên", me?.branchName))
            }.orEmpty()
            null -> emptyList()
        }

    val canSave: Boolean
        get() = !saving && salespersonId != null && categoryId != null && content.isNotBlank() &&
            to.isAfter(from) && (objectKind == null || selected.isNotEmpty())
}

@HiltViewModel
class AssignWorkPlanViewModel @Inject constructor(
    private val repository: WorkPlanRepository,
    auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AssignState())
    val state: StateFlow<AssignState> = _state.asStateFlow()

    private var customerJob: Job? = null

    init {
        viewModelScope.launch {
            auth.sessionState.collect { s ->
                _state.update { it.copy(me = (s as? SessionState.SignedIn)?.manager) }
            }
        }
        viewModelScope.launch {
            auth.team.collect { team ->
                _state.update { it.copy(team = team, salespersonId = it.salespersonId ?: team.singleOrNull()?.id) }
            }
        }
        viewModelScope.launch {
            when (val r = repository.categories()) {
                is DataResult.Success -> _state.update { it.copy(categories = r.data) }
                is DataResult.Failure -> _state.update { it.copy(message = r.error.toMessage()) }
            }
        }
    }

    fun selectRep(id: String) {
        // Objects belong to the rep (their outlets, their distributor), so a new
        // rep starts the picker over.
        _state.update { it.copy(salespersonId = id, selected = emptySet(), customers = emptyList()) }
        loadCustomers()
    }

    fun selectCategory(id: String) {
        _state.update { it.copy(categoryId = id, selected = emptySet()) }
        val s = _state.value
        if (s.objectKind == WorkObjectKind.BRANCH) {
            s.options.singleOrNull()?.let { only -> _state.update { it.copy(selected = setOf(only.id)) } }
        }
        loadCustomers()
    }

    fun onQuery(q: String) {
        _state.update { it.copy(query = q) }
        loadCustomers(debounce = true)
    }

    private fun loadCustomers(debounce: Boolean = false) {
        val s = _state.value
        val rep = s.salespersonId ?: return
        if (s.objectKind != WorkObjectKind.CUSTOMER) return
        customerJob?.cancel()
        customerJob = viewModelScope.launch {
            if (debounce) delay(300)
            _state.update { it.copy(loadingCustomers = true) }
            val r = repository.customersOf(rep, _state.value.query)
            _state.update {
                it.copy(
                    loadingCustomers = false,
                    customers = (r as? DataResult.Success)?.data ?: it.customers,
                    message = (r as? DataResult.Failure)?.error?.toMessage() ?: it.message,
                )
            }
        }
    }

    fun toggle(id: String) = _state.update {
        it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id)
    }

    fun setDate(d: LocalDate) = _state.update { it.copy(date = d) }
    fun setFrom(t: LocalTime) = _state.update { it.copy(from = t, to = if (it.to.isAfter(t)) it.to else t.plusHours(1)) }
    fun setTo(t: LocalTime) = _state.update { it.copy(to = t) }
    fun setContent(v: String) = _state.update { it.copy(content = v) }
    fun setPlace(v: String) = _state.update { it.copy(place = v) }
    fun setNote(v: String) = _state.update { it.copy(note = v) }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val draft = AssignDraft(
                salespersonId = s.salespersonId!!,
                categoryId = s.categoryId!!,
                content = s.content,
                place = s.place,
                note = s.note,
                fromAt = toIsoInstant(s.date, s.from),
                toAt = toIsoInstant(s.date, s.to),
                objectIds = s.selected.toList(),
            )
            when (val r = repository.assign(draft)) {
                is DataResult.Success -> _state.update { it.copy(saving = false, done = true) }
                is DataResult.Failure -> _state.update { it.copy(saving = false, message = r.error.toMessage()) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

/** Giao việc. The legacy manager's "add task" form (API_GetListTaskAssignManager). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignWorkPlanScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: AssignWorkPlanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var picking by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.done) { if (state.done) onDone() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }

    SubScreen(
        title = "Giao việc",
        onBack = onBack,
        snackbar = snackbar,
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                PrimaryButton(
                    text = "Giao việc",
                    onClick = viewModel::save,
                    enabled = state.canSave,
                    loading = state.saving,
                    modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Picker(
                    label = "Nhân viên *",
                    value = state.rep?.let { "${it.fullName} (${it.code})" },
                    options = state.team.map { it.id to "${it.fullName} (${it.code})" },
                    onSelect = viewModel::selectRep,
                )
            }
            item {
                Picker(
                    label = "Loại công việc *",
                    value = state.category?.name,
                    options = state.categories.map { it.id to it.name },
                    onSelect = viewModel::selectCategory,
                )
            }
            item {
                // Date on its own row and the two times beneath: three labelled
                // buttons side by side wrapped "Từ / 08:00" onto two lines on a
                // 720px-wide phone.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picking = "date" }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ngày: ${formatDay(state.date)}")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { picking = "from" }, modifier = Modifier.weight(1f)) {
                            Text("Từ ${formatTime(state.from)}", maxLines = 1)
                        }
                        OutlinedButton(onClick = { picking = "to" }, modifier = Modifier.weight(1f)) {
                            Text("Đến ${formatTime(state.to)}", maxLines = 1)
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.content, onValueChange = viewModel::setContent,
                    label = { Text("Nội dung công việc *") }, minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = state.place, onValueChange = viewModel::setPlace,
                    label = { Text("Địa điểm") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = state.note, onValueChange = viewModel::setNote,
                    label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth(),
                )
            }

            val kind = state.objectKind
            if (kind != null) {
                item {
                    Text(
                        "${kind.label} (${state.selected.size} đã chọn) *",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (kind == WorkObjectKind.CUSTOMER) {
                    item {
                        if (state.salespersonId == null) {
                            Text("Chọn nhân viên trước", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            SearchBox(query = state.query, onQueryChanged = viewModel::onQuery, placeholder = "Tìm khách hàng trong tuyến")
                        }
                    }
                }
                if (state.options.isEmpty() && !state.loadingCustomers && state.salespersonId != null) {
                    item { Text("Không có lựa chọn nào", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(state.options, key = { it.id }) { o ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(value = o.id in state.selected, onValueChange = { viewModel.toggle(o.id) }),
                    ) {
                        Checkbox(checked = o.id in state.selected, onCheckedChange = null)
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(o.title, style = MaterialTheme.typography.bodyLarge)
                            o.subtitle?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    when (picking) {
        "date" -> {
            val dateState = rememberDatePickerState(
                initialSelectedDateMillis = state.date.atStartOfDay(VN_ZONE).toInstant().toEpochMilli(),
            )
            DatePickerDialog(
                onDismissRequest = { picking = null },
                confirmButton = {
                    TextButton(onClick = {
                        dateState.selectedDateMillis?.let {
                            // The picker hands back UTC midnight of the chosen day.
                            viewModel.setDate(Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate())
                        }
                        picking = null
                    }) { Text("Chọn") }
                },
                dismissButton = { TextButton(onClick = { picking = null }) { Text("Huỷ") } },
            ) { DatePicker(state = dateState) }
        }
        "from", "to" -> {
            val initial = if (picking == "from") state.from else state.to
            val timeState = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
            AlertDialog(
                onDismissRequest = { picking = null },
                confirmButton = {
                    TextButton(onClick = {
                        val t = LocalTime.of(timeState.hour, timeState.minute)
                        if (picking == "from") viewModel.setFrom(t) else viewModel.setTo(t)
                        picking = null
                    }) { Text("Chọn") }
                },
                dismissButton = { TextButton(onClick = { picking = null }) { Text("Huỷ") } },
                text = { TimePicker(state = timeState) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Picker(
    label: String,
    value: String?,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}
