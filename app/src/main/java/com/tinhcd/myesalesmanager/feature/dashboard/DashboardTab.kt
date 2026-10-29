package com.tinhcd.myesalesmanager.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.Kpi
import com.tinhcd.myesalesmanager.core.ui.KpiBar
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.PercentPill
import com.tinhcd.myesalesmanager.core.ui.StatTile
import com.tinhcd.myesalesmanager.core.ui.compactMoney
import com.tinhcd.myesalesmanager.core.ui.formatDay
import com.tinhcd.myesalesmanager.core.ui.formatDong
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.ChartRange
import com.tinhcd.myesalesmanager.domain.model.MemberFigures
import com.tinhcd.myesalesmanager.domain.model.TeamDashboard
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.model.percent
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import com.tinhcd.myesalesmanager.domain.repository.DashboardRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardState(
    val date: LocalDate = todayVn(),
    val salespersonId: String? = null,
    val team: List<TeamMember> = emptyList(),
    val range: ChartRange = ChartRange.THIS_WEEK,
    val data: TeamDashboard? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch { auth.team.collect { t -> _state.update { it.copy(team = t) } } }
    }

    fun refresh() {
        val s = _state.value
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = repository.overview(s.date.toString(), s.salespersonId)) {
                is DataResult.Success -> _state.update { it.copy(loading = false, data = r.data) }
                is DataResult.Failure -> _state.update { it.copy(loading = false, error = r.error.toMessage()) }
            }
        }
    }

    /** Never past today: there is nothing to show for a day that has not happened. */
    fun shiftDay(days: Long) {
        val next = _state.value.date.plusDays(days)
        if (next.isAfter(todayVn())) return
        _state.update { it.copy(date = next) }
        refresh()
    }

    fun today() {
        _state.update { it.copy(date = todayVn()) }
        refresh()
    }

    fun selectSalesperson(id: String?) {
        _state.update { it.copy(salespersonId = id) }
        refresh()
    }

    fun selectRange(range: ChartRange) = _state.update { it.copy(range = range) }
}

/**
 * Tab "Tổng quan". The team's day, the month against target, the sales line,
 * and the reps ranked by the month — the last being what a supervisor opens
 * this screen for. Legacy DashboardScreen (API_DashboardOverview_Manager).
 */
@Composable
fun DashboardTab(viewModel: DashboardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val data = state.data
    when {
        state.loading && data == null -> LoadingBox()
        data == null -> ErrorBox(state.error ?: "Không tải được số liệu", onRetry = viewModel::refresh)
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { DayBar(state, viewModel) }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    item {
                        FilterChip(
                            selected = state.salespersonId == null,
                            onClick = { viewModel.selectSalesperson(null) },
                            label = { Text("Cả nhóm (${state.team.size})") },
                        )
                    }
                    items(state.team, key = { it.id }) { m ->
                        FilterChip(
                            selected = state.salespersonId == m.id,
                            onClick = { viewModel.selectSalesperson(m.id) },
                            label = { Text(m.fullName) },
                        )
                    }
                }
            }
            item { TodayCard(data) }
            item { MonthCard(data) }
            item { ChartCard(data, state.range, viewModel::selectRange) }
            if (state.salespersonId == null && data.members.isNotEmpty()) {
                item {
                    Text(
                        "Xếp hạng nhân viên (tháng)",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                items(data.members.withIndex().toList(), key = { it.value.id }) { (i, m) ->
                    MemberRow(i + 1, m, onClick = { viewModel.selectSalesperson(m.id) })
                }
            }
        }
    }
}

@Composable
private fun DayBar(state: DashboardState, viewModel: DashboardViewModel) {
    val isToday = state.date == todayVn()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        IconButton(onClick = { viewModel.shiftDay(-1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Ngày trước")
        }
        Text(
            (if (isToday) "Hôm nay · " else "") + formatDay(state.date),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (!isToday) TextButton(onClick = viewModel::today) { Text("Hôm nay") }
        IconButton(onClick = { viewModel.shiftDay(1) }, enabled = !isToday) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Ngày sau")
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun TodayCard(d: TeamDashboard) {
    val t = d.today
    SectionCard("Trong ngày") {
        Text(formatDong(t.revenue), style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("Viếng thăm", "${t.visitDone}/${t.visitPlanned}", Kpi.Blue, Modifier.weight(1f))
            StatTile("Đơn hàng", "${t.orderCount}", Kpi.Green, Modifier.weight(1f))
            StatTile("SKU/đơn", oneDecimal(t.skuPerOrder), Kpi.Orange, Modifier.weight(1f))
        }
        if (d.teamSize > 1) {
            Text(
                "Nhân viên đã đi tuyến: ${t.activeReps}/${d.teamSize}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthCard(d: TeamDashboard) {
    val m = d.month
    SectionCard("Tháng này") {
        KpiLine("Doanh số", compactMoney(m.revenue), m.revenueTarget?.let(::compactMoney), percent(m.revenue, m.revenueTarget), Kpi.Violet)
        KpiLine("Đơn hàng", "${m.orderCount}", m.orderTarget?.toString(), percent(m.orderCount.toLong(), m.orderTarget?.toLong()), Kpi.Green)
        KpiLine("Độ phủ tuyến", "${m.visitDone}", "${m.visitPlanned}", percent(m.visitDone.toLong(), m.visitPlanned.toLong()), Kpi.Blue)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("VPO", compactMoney(m.vpo), Kpi.Slate, Modifier.weight(1f))
            StatTile("SKU/đơn", oneDecimal(m.skuPerOrder), Kpi.Orange, Modifier.weight(1f))
            StatTile("KH mới", "${m.newCustomers}", Kpi.Green, Modifier.weight(1f))
        }
    }
}

/** A month figure against its target, with the bar only when there is a target. */
@Composable
private fun KpiLine(label: String, value: String, target: String?, pct: Int?, ramp: List<Color>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                if (target != null) "$value / $target" else value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (pct != null) {
                Spacer(Modifier.width(8.dp))
                PercentPill(pct, ramp.first())
            }
        }
        if (pct != null) {
            KpiBar(pct / 100f, ramp)
        } else {
            Text(
                "Chưa có chỉ tiêu",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChartCard(d: TeamDashboard, range: ChartRange, onRange: (ChartRange) -> Unit) {
    SectionCard("Doanh số bán hàng") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChartRange.entries.forEach { r ->
                FilterChip(selected = r == range, onClick = { onRange(r) }, label = { Text(r.label) })
            }
        }
        val points = d.charts[range].orEmpty()
        Text(
            "Tổng: ${formatDong(points.sumOf { it.actual })}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SalesTrendChart(points, Modifier.fillMaxWidth().height(180.dp))
    }
}

@Composable
private fun MemberRow(rank: Int, m: MemberFigures, onClick: () -> Unit) {
    val pct = m.achievement
    val ramp = when {
        pct == null -> Kpi.Slate
        pct >= 100 -> Kpi.Green
        pct >= 70 -> Kpi.Blue
        else -> Kpi.Orange
    }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$rank. ${m.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (pct != null) PercentPill(pct, ramp.first())
            }
            Text(
                "Tháng: ${compactMoney(m.monthRevenue)}" +
                    (m.revenueTarget?.let { " / ${compactMoney(it)}" } ?: "") +
                    " · ${m.monthOrders} đơn",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (pct != null) KpiBar(pct / 100f, ramp)
            Text(
                "Hôm nay: ${m.todayVisits}/${m.todayPlanned} viếng thăm · ${m.todayOrders} đơn · ${money(m.todayRevenue)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 2.2 → "2,2", 2.0 → "2": the decimal comma compactMoney already writes. */
private fun oneDecimal(value: Double): String =
    "%.1f".format(java.util.Locale.US, value).replace('.', ',').removeSuffix(",0")

/** compactMoney says "tr"/"ng" for big amounts but nothing for small ones. */
private fun money(value: Long): String = if (value < 1_000) "$value đ" else compactMoney(value)
