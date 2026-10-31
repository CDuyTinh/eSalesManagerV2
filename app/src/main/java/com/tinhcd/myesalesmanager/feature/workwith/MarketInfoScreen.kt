package com.tinhcd.myesalesmanager.feature.workwith

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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.tinhcd.myesalesmanager.core.ui.formatIsoDate
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.CompetitorSurvey
import com.tinhcd.myesalesmanager.domain.model.MarketSurvey
import com.tinhcd.myesalesmanager.domain.repository.InStoreRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ARG_SURVEY_ID = "surveyId"

// --- the list --------------------------------------------------------------------

data class MarketInfoState(val surveys: List<MarketSurvey>? = null, val error: String? = null)

@HiltViewModel
class MarketInfoViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: InStoreRepository,
) : ViewModel() {

    val visitId: String = checkNotNull(savedState[ARG_VISIT_ID])

    private val _state = MutableStateFlow(MarketInfoState())
    val state: StateFlow<MarketInfoState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            when (val r = repository.surveys(visitId)) {
                is DataResult.Success -> _state.update { it.copy(surveys = r.data, error = null) }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }
}

/** Thông tin thị trường: the surveys running at this outlet (legacy MarketInfo). */
@Composable
fun MarketInfoScreen(
    onBack: () -> Unit,
    onOpen: (visitId: String, surveyId: String) -> Unit,
    viewModel: MarketInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }
    SubScreen(title = "Thông tin thị trường", onBack = onBack) { padding ->
        val list = state.surveys
        when {
            list == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            list == null -> LoadingBox(Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (list.isEmpty()) item { Text("Không có khảo sát đang chạy", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(list, key = { it.kind + it.id }) { s ->
                    // Market questionnaires are scored step forms on the rep's app;
                    // competitor surveys are the grid filled in here.
                    val competitor = s.kind == "competitor"
                    Card(
                        onClick = { if (competitor) onOpen(viewModel.visitId, s.id) },
                        enabled = competitor,
                        modifier = Modifier.fillMaxWidth().alpha(if (competitor) 1f else 0.6f),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${s.code} – ${s.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    listOfNotNull(
                                        if (competitor) "Khảo sát đối thủ · ${s.itemCount} cặp sản phẩm" else "Khảo sát thị trường (làm trên app nhân viên)",
                                        s.toDate?.let { "đến ${formatIsoDate(it)}" },
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            if (s.completed) StatusPill("Đã làm", Kpi.Green.first())
                        }
                    }
                }
            }
        }
    }
}

// --- one competitor survey ------------------------------------------------------------

data class CompetitorSurveyState(
    val survey: CompetitorSurvey? = null,
    /** (product, competitor product, criterion) → answer. */
    val answers: Map<Triple<String, String, String>, String> = emptyMap(),
    val error: String? = null,
    val saving: Boolean = false,
    val message: String? = null,
    val done: Boolean = false,
)

@HiltViewModel
class CompetitorSurveyViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: InStoreRepository,
) : ViewModel() {

    private val visitId: String = checkNotNull(savedState[ARG_VISIT_ID])
    private val surveyId: String = checkNotNull(savedState[ARG_SURVEY_ID])

    private val _state = MutableStateFlow(CompetitorSurveyState())
    val state: StateFlow<CompetitorSurveyState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            when (val r = repository.survey(visitId, surveyId)) {
                is DataResult.Success -> _state.update { s ->
                    val earlier = r.data.pairings.flatMap { p ->
                        p.answers.map { (c, v) -> Triple(p.productId, p.competitorProductId, c) to v }
                    }.toMap()
                    s.copy(survey = r.data, answers = earlier, error = null)
                }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }

    fun answer(key: Triple<String, String, String>, v: String) = _state.update { it.copy(answers = it.answers + (key to v)) }

    fun save() {
        val s = _state.value
        val survey = s.survey ?: return
        val missing = survey.pairings.any { p ->
            survey.criteria.any { c -> c.required && s.answers[Triple(p.productId, p.competitorProductId, c.id)].isNullOrBlank() }
        }
        if (missing) {
            _state.update { it.copy(message = "Vui lòng trả lời đủ các tiêu chí bắt buộc") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val r = repository.submitSurvey(visitId, surveyId, s.answers)) {
                is DataResult.Success -> _state.update { it.copy(saving = false, done = true) }
                is DataResult.Failure -> _state.update { it.copy(saving = false, message = r.error.toMessage()) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

/** Khảo sát đối thủ: each of our products against its rival, criterion by criterion. */
@Composable
fun CompetitorSurveyScreen(onBack: () -> Unit, viewModel: CompetitorSurveyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.done) { if (state.done) onBack() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() } }

    val survey = state.survey
    SubScreen(
        title = survey?.name ?: "Khảo sát đối thủ",
        onBack = onBack,
        snackbar = snackbar,
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                PrimaryButton(
                    "Lưu khảo sát", viewModel::save, Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp, 10.dp),
                    enabled = !state.saving && survey != null, loading = state.saving,
                )
            }
        },
    ) { padding ->
        when {
            survey == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            survey == null -> LoadingBox(Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(survey.pairings, key = { it.productId + it.competitorProductId }) { p ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(p.productName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text("so với ${p.competitorProduct} (${p.competitorName})", style = MaterialTheme.typography.labelSmall)
                            survey.criteria.forEach { c ->
                                val key = Triple(p.productId, p.competitorProductId, c.id)
                                OutlinedTextField(
                                    value = state.answers[key].orEmpty(),
                                    onValueChange = { viewModel.answer(key, it) },
                                    label = { Text(c.name + if (c.required) " *" else "") },
                                    placeholder = c.hint?.let { { Text(it) } },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
