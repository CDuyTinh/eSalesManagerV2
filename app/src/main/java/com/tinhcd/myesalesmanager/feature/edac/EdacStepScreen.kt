package com.tinhcd.myesalesmanager.feature.edac

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.Kpi
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.PrimaryButton
import com.tinhcd.myesalesmanager.core.ui.StatusPill
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.EdacAnswer
import com.tinhcd.myesalesmanager.domain.model.EdacCriterion
import com.tinhcd.myesalesmanager.domain.model.EdacKind
import com.tinhcd.myesalesmanager.domain.model.EdacOutlet
import com.tinhcd.myesalesmanager.domain.model.EdacStep
import com.tinhcd.myesalesmanager.domain.model.EdacStepForm
import com.tinhcd.myesalesmanager.domain.model.EdacTraining
import com.tinhcd.myesalesmanager.domain.repository.EdacRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ARG_EDAC_STEP = "step"
const val ARG_EDAC_CUSTOMER = "customer"

data class EdacStepState(
    /** Set on a per-outlet step before an outlet is picked. */
    val outlets: List<EdacOutlet>? = null,
    val form: EdacStepForm? = null,
    /** Steps 0 and 6 are judged against the rep's figures. */
    val training: EdacTraining? = null,
    val answers: Map<String, EdacAnswer> = emptyMap(),
    val note: String = "",
    val error: String? = null,
    val saving: Boolean = false,
    val message: String? = null,
    /** Set when the screen's work is done and it should close. */
    val done: Boolean = false,
)

@HiltViewModel
class EdacStepViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: EdacRepository,
) : ViewModel() {

    val trainingId: String = checkNotNull(savedState[ARG_TRAINING_ID])
    val step: EdacStep = EdacStep.of(checkNotNull(savedState.get<String>(ARG_EDAC_STEP)).toInt())
    val customerId: String? = savedState[ARG_EDAC_CUSTOMER]

    /** The outlet list of a per-outlet step, as opposed to one outlet's form. */
    val isOutletList: Boolean get() = step.perOutlet && customerId == null

    private val _state = MutableStateFlow(EdacStepState())
    val state: StateFlow<EdacStepState> = _state.asStateFlow()

    private var loadedForm = false

    fun refresh() {
        viewModelScope.launch {
            if (isOutletList) {
                when (val r = repository.outlets(trainingId, step)) {
                    is DataResult.Success -> _state.update { it.copy(outlets = r.data, error = null) }
                    is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
                }
                return@launch
            }
            // The form is loaded once: returning from the keyboard must not wipe a draft.
            if (loadedForm) return@launch
            if (step == EdacStep.PREPARE || step == EdacStep.END_OF_DAY) {
                (repository.training(trainingId) as? DataResult.Success)?.let { r -> _state.update { it.copy(training = r.data) } }
            }
            when (val r = repository.form(trainingId, step, customerId)) {
                is DataResult.Success -> {
                    loadedForm = true
                    _state.update {
                        it.copy(
                            form = r.data,
                            answers = r.data.criteria.mapNotNull { c -> c.answer?.let { a -> c.id to a } }.toMap(),
                            note = r.data.note.orEmpty(),
                            error = null,
                        )
                    }
                }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }

    fun answer(criterionId: String, change: (EdacAnswer) -> EdacAnswer) = _state.update {
        it.copy(answers = it.answers + (criterionId to change(it.answers[criterionId] ?: EdacAnswer())))
    }

    fun setNote(v: String) = _state.update { it.copy(note = v) }

    /** Saves the form; on a plan-level step [finish] also closes the step. */
    fun save(finish: Boolean) {
        val s = _state.value
        val form = s.form
        if (form != null) {
            val missing = form.criteria.firstOrNull { c -> c.required && !s.answers[c.id].isComplete(c.kind) }
            if (missing != null) {
                _state.update { it.copy(message = "Vui lòng trả lời: ${missing.content}") }
                return
            }
        }
        submit(finish, answers = s.answers, note = s.note.takeIf { step == EdacStep.DEMONSTRATE })
    }

    /** Closes a per-outlet step from its outlet list. */
    fun finishStep() = submit(finish = true, answers = emptyMap(), note = null)

    private fun submit(finish: Boolean, answers: Map<String, EdacAnswer>, note: String?) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val r = repository.save(trainingId, step, customerId, answers, note, finish)) {
                is DataResult.Success -> _state.update { it.copy(saving = false, done = true) }
                is DataResult.Failure -> _state.update { it.copy(saving = false, message = r.error.toMessage()) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

private fun EdacAnswer?.isComplete(kind: EdacKind): Boolean = when (kind) {
    EdacKind.SCALE -> this?.value != null
    EdacKind.YES_NO -> this?.yes != null
    EdacKind.TEXT -> !this?.text.isNullOrBlank()
}

/**
 * A step. On a per-outlet step it first lists the rep's outlets for the day,
 * marking those already recorded; picking one opens the same screen as that
 * outlet's form.
 */
@Composable
fun EdacStepScreen(
    onBack: () -> Unit,
    onOpenOutlet: (String, EdacStep, String) -> Unit,
    viewModel: EdacStepViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.done) { if (state.done) onBack() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() } }

    val step = viewModel.step
    val form = state.form
    SubScreen(
        title = "${step.index + 1}. ${step.title}",
        subtitle = form?.customer?.let { "${it.code} – ${it.name}" } ?: step.hint,
        onBack = onBack,
        snackbar = snackbar,
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp, 10.dp),
                ) {
                    when {
                        viewModel.isOutletList -> PrimaryButton(
                            "Hoàn tất bước ${step.title.lowercase()}", viewModel::finishStep,
                            Modifier.weight(1f), enabled = !state.saving && state.outlets != null, loading = state.saving,
                        )
                        step.perOutlet -> PrimaryButton(
                            "Lưu điểm bán", { viewModel.save(false) },
                            Modifier.weight(1f), enabled = !state.saving && form != null, loading = state.saving,
                        )
                        else -> {
                            OutlinedButton(onClick = { viewModel.save(false) }, enabled = !state.saving && form != null, modifier = Modifier.weight(1f)) {
                                Text("Lưu nháp")
                            }
                            PrimaryButton(
                                "Hoàn tất bước", { viewModel.save(true) },
                                Modifier.weight(1f), enabled = !state.saving && form != null, loading = state.saving,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        val outlets = state.outlets
        when {
            viewModel.isOutletList && outlets != null -> OutletList(outlets, step, Modifier.padding(padding)) {
                onOpenOutlet(viewModel.trainingId, step, it.customerId)
            }
            form != null -> FormBody(state, form, Modifier.padding(padding), viewModel)
            state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            else -> LoadingBox(Modifier.padding(padding))
        }
    }
}

@Composable
private fun OutletList(outlets: List<EdacOutlet>, step: EdacStep, modifier: Modifier, onOpen: (EdacOutlet) -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "${outlets.count { step.index in it.doneSteps }}/${outlets.size} điểm đã ghi nhận ở bước này",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (outlets.isEmpty()) {
            item { Text("Hôm nay nhân viên không có điểm bán trên tuyến", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(outlets, key = { it.customerId }) { o ->
            Card(onClick = { onOpen(o) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${o.code} – ${o.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (step.index in o.doneSteps) StatusPill("Đã ghi nhận", Kpi.Green.first())
                    }
                    o.address?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    val others = o.doneSteps.filter { it != step.index }.sorted()
                    if (!o.due || others.isNotEmpty()) {
                        Text(
                            listOfNotNull(
                                if (!o.due) "Ngoài lịch hôm nay" else null,
                                others.takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = "Đã có: ") { EdacStep.of(it).title },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormBody(state: EdacStepState, form: EdacStepForm, modifier: Modifier, viewModel: EdacStepViewModel) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.training?.let { t -> item { RepFigures(t) } }
        if (form.observed.isNotEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Kết quả quan sát", fontWeight = FontWeight.SemiBold)
                        form.observed.forEach { o ->
                            Row {
                                Text(o.content, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                Text(
                                    "Có ${o.yes} · Không ${o.no}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (o.no > 0) Kpi.Orange.first() else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (form.step == EdacStep.DEMONSTRATE) {
            item {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::setNote,
                    label = { Text("Nội dung giám sát làm mẫu") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        items(form.criteria, key = { it.id }) { c ->
            CriterionCard(c, state.answers[c.id]) { change -> viewModel.answer(c.id, change) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CriterionCard(c: EdacCriterion, answer: EdacAnswer?, onChange: ((EdacAnswer) -> EdacAnswer) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(c.content + if (c.required) " *" else "", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            when (c.kind) {
                EdacKind.YES_NO -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = answer?.yes == true, onClick = { onChange { it.copy(yes = true) } }, label = { Text("Có") })
                    FilterChip(selected = answer?.yes == false, onClick = { onChange { it.copy(yes = false) } }, label = { Text("Không") })
                }
                EdacKind.SCALE -> c.options.forEach { o ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onChange { it.copy(value = o.value) } },
                    ) {
                        RadioButton(selected = answer?.value == o.value, onClick = { onChange { it.copy(value = o.value) } })
                        Text("${o.value}. ${o.label}", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                EdacKind.TEXT -> OutlinedTextField(
                    value = answer?.text.orEmpty(),
                    onValueChange = { v -> onChange { it.copy(text = v) } },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (c.kind != EdacKind.TEXT) {
                OutlinedTextField(
                    value = answer?.note.orEmpty(),
                    onValueChange = { v -> onChange { it.copy(note = v) } },
                    label = { Text("Ghi chú") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
