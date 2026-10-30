package com.tinhcd.myesalesmanager.feature.edac

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.tinhcd.myesalesmanager.core.ui.formatIsoDate
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.EdacResult
import com.tinhcd.myesalesmanager.domain.model.EdacResultLine
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

data class EdacResultState(val result: EdacResult? = null, val error: String? = null)

@HiltViewModel
class EdacResultViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: EdacRepository,
) : ViewModel() {

    private val trainingId: String = checkNotNull(savedState[ARG_TRAINING_ID])

    private val _state = MutableStateFlow(EdacResultState())
    val state: StateFlow<EdacResultState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            when (val r = repository.result(trainingId)) {
                is DataResult.Success -> _state.update { it.copy(result = r.data, error = null) }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }
}

/** A finished day read back: result, photos, every answer by step (legacy edac_training_history). */
@Composable
fun EdacResultScreen(onBack: () -> Unit, viewModel: EdacResultViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val r = state.result
    SubScreen(
        title = r?.let { "EDAC · ${it.training.rep.name}" } ?: "Kết quả huấn luyện",
        subtitle = r?.let { formatIsoDate(it.training.trainingDate) },
        onBack = onBack,
    ) { padding ->
        when {
            r == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            r == null -> LoadingBox(Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Kết quả", fontWeight = FontWeight.SemiBold)
                            Text(r.training.result ?: "—")
                            Text(
                                "Bắt đầu ${formatDateTime(r.training.checkInAt)}" +
                                    (r.training.checkOutAt?.let { " · kết thúc ${formatDateTime(it)}" } ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(r.training.supervisorPhotoUrl to "Giám sát", r.training.salespersonPhotoUrl to "Nhân viên")
                                    .forEach { (url, label) ->
                                        Column(Modifier.weight(1f)) {
                                            Text(label, style = MaterialTheme.typography.labelSmall)
                                            if (url != null) {
                                                AsyncImage(
                                                    model = url, contentDescription = label, contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(8.dp)),
                                                )
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }
                item { RepFigures(r.training) }
                val byStep = (r.lines + r.notes).groupBy { it.step }.toSortedMap()
                byStep.forEach { (step, lines) ->
                    item(key = "step$step") {
                        Text(
                            "${step + 1}. ${EdacStep.of(step).title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    lines.groupBy { it.customer }.forEach { (customer, group) ->
                        item(key = "step$step-$customer") { AnswerGroup(customer, group) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerGroup(customer: String?, lines: List<EdacResultLine>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            customer?.let { Text(it, fontWeight = FontWeight.SemiBold) }
            lines.forEachIndexed { i, line ->
                if (i > 0) HorizontalDivider()
                Text(line.content, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(line.answer ?: "—", style = MaterialTheme.typography.bodyLarge)
                line.note?.let { Text("Ghi chú: $it", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}
