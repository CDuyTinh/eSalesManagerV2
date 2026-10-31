package com.tinhcd.myesalesmanager.feature.prepare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.Kpi
import com.tinhcd.myesalesmanager.core.ui.KpiBar
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.PercentPill
import com.tinhcd.myesalesmanager.core.ui.StatusPill
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.formatDay
import com.tinhcd.myesalesmanager.core.ui.formatIsoDate
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.FocusProduct
import com.tinhcd.myesalesmanager.domain.model.SalesFocus
import com.tinhcd.myesalesmanager.domain.model.percent
import com.tinhcd.myesalesmanager.domain.repository.PrepareRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

data class SalesFocusState(val focus: SalesFocus? = null, val error: String? = null)

@HiltViewModel
class SalesFocusViewModel @Inject constructor(
    private val repository: PrepareRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SalesFocusState())
    val state: StateFlow<SalesFocusState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            when (val r = repository.salesFocus(todayVn().toString())) {
                is DataResult.Success -> _state.update { it.copy(focus = r.data, error = null) }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }
}

private val number = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))

/** Trọng tâm bán hàng: what head office is pushing today and how the team is doing on it. */
@Composable
fun SalesFocusScreen(onBack: () -> Unit, viewModel: SalesFocusViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SubScreen(title = "Trọng tâm bán hàng", subtitle = formatDay(todayVn()), onBack = onBack) { padding ->
        val f = state.focus
        when {
            f == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            f == null -> LoadingBox(Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { Text("Sản phẩm trọng tâm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
                if (f.products.isEmpty()) item { Text("Hôm nay không có sản phẩm trọng tâm", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(f.products, key = { it.productCode + it.fromDate }) { FocusCard(it) }

                item {
                    Text(
                        "Chương trình đang chạy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (f.programs.isEmpty()) item { Text("Không có chương trình", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(f.programs, key = { it.kind + it.code }) { p ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${p.code} – ${p.name}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                StatusPill(if (p.kind == "display") "Trưng bày" else "Tích lũy", if (p.kind == "display") Kpi.Blue.first() else Kpi.Violet.first())
                            }
                            Text(
                                "${formatIsoDate(p.fromDate)} – ${formatIsoDate(p.toDate)} · nhóm đã đăng ký ${p.registered} điểm",
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

@Composable
private fun FocusCard(p: FocusProduct) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${p.productCode} – ${p.productName}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                percent(p.soldBaseQty, p.teamTarget)?.let { PercentPill(it, Kpi.Green.first()) }
            }
            Text(
                "${formatIsoDate(p.fromDate)} – ${formatIsoDate(p.toDate)}" + (p.note?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Nhóm: ${number.format(p.soldBaseQty)}" + (p.teamTarget?.let { " / ${number.format(it)}" } ?: "") +
                    " ${p.baseUom} · ${p.outlets} điểm bán",
                style = MaterialTheme.typography.bodyLarge,
            )
            p.teamTarget?.let { KpiBar((p.soldBaseQty.toFloat() / it).coerceIn(0f, 1f), Kpi.Green) }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            p.reps.forEach { r ->
                Row {
                    Text(r.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(
                        "${number.format(r.soldBaseQty)}" + (p.targetBaseQty?.let { " / ${number.format(it)}" } ?: "") +
                            " · ${r.outlets} điểm",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (p.targetBaseQty != null && r.soldBaseQty < p.targetBaseQty) Kpi.Orange.first() else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
