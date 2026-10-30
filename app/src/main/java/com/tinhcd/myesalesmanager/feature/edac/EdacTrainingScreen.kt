package com.tinhcd.myesalesmanager.feature.edac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.tinhcd.myesalesmanager.core.ui.StatTile
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.compactMoney
import com.tinhcd.myesalesmanager.core.ui.formatIsoDate
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.EdacStep
import com.tinhcd.myesalesmanager.domain.model.EdacTraining
import com.tinhcd.myesalesmanager.domain.model.isOpen
import com.tinhcd.myesalesmanager.domain.repository.EdacRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ARG_TRAINING_ID = "trainingId"

data class EdacTrainingState(
    val training: EdacTraining? = null,
    val error: String? = null,
)

@HiltViewModel
class EdacTrainingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: EdacRepository,
) : ViewModel() {

    val trainingId: String = checkNotNull(savedState[ARG_TRAINING_ID])

    private val _state = MutableStateFlow(EdacTrainingState())
    val state: StateFlow<EdacTrainingState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            when (val r = repository.training(trainingId)) {
                is DataResult.Success -> _state.update { it.copy(training = r.data, error = null) }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }
}

/** One coaching day: the rep's figures and the eight steps, done / open / locked. */
@Composable
fun EdacTrainingScreen(
    onBack: () -> Unit,
    onOpenStep: (String, EdacStep) -> Unit,
    onOpenResult: (String) -> Unit,
    viewModel: EdacTrainingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val t = state.training
    SubScreen(
        title = t?.let { "EDAC · ${it.rep.name}" } ?: "Huấn luyện EDAC",
        subtitle = t?.let { formatIsoDate(it.trainingDate) },
        onBack = onBack,
    ) { padding ->
        when {
            t == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            t == null -> LoadingBox(Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { RepFigures(t) }
                items(EdacStep.entries) { step ->
                    val open = !t.completed && step.isOpen(t.currentStep)
                    val done = t.completed || t.currentStep > step.index
                    StepCard(step, open, done, t.outletsDone[step.index]) {
                        if (t.completed) onOpenResult(t.id) else onOpenStep(t.id, step)
                    }
                }
            }
        }
    }
}

/**
 * The rep's numbers: Chuẩn bị judges past results and Đánh giá cuối ngày the
 * day's, so both steps need them in sight (legacy API_GetEDACTrainingKPIManager).
 */
@Composable
fun RepFigures(t: EdacTraining, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        t.today?.let { d ->
            Text("Trong ngày", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Doanh số", compactMoney(d.revenue), Kpi.Blue, Modifier.weight(1f))
                StatTile("Đơn hàng", d.orderCount.toString(), Kpi.Green, Modifier.weight(1f))
                StatTile("Viếng thăm", "${d.visitDone}/${d.visitPlanned}", Kpi.Violet, Modifier.weight(1f))
            }
        }
        t.month?.let { m ->
            Text("Tháng này", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    "Doanh số", compactMoney(m.revenue), Kpi.Blue, Modifier.weight(1f),
                    footer = m.revenueTarget?.let { "CT ${compactMoney(it)}" },
                )
                StatTile(
                    "Đơn hàng", m.orderCount.toString(), Kpi.Green, Modifier.weight(1f),
                    footer = m.orderTarget?.let { "CT $it" },
                )
                StatTile("SKU/đơn", "%.1f".format(m.skuPerOrder), Kpi.Orange, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StepCard(step: EdacStep, open: Boolean, done: Boolean, outlets: Int?, onClick: () -> Unit) {
    Card(onClick = onClick, enabled = open || done, modifier = Modifier.fillMaxWidth().alpha(if (open || done) 1f else 0.5f)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = CircleShape,
                color = if (done) Kpi.Green.first() else if (open) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(32.dp),
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("${step.index + 1}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(step.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    step.hint + (if (step.perOutlet && outlets != null && outlets > 0) " · $outlets điểm" else ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                done -> Icon(Icons.Default.CheckCircle, contentDescription = "Đã xong", tint = Kpi.Green.first())
                open -> Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                else -> Icon(Icons.Default.Lock, contentDescription = "Chưa mở", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
