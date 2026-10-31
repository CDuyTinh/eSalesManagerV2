package com.tinhcd.myesalesmanager.feature.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.Kpi
import com.tinhcd.myesalesmanager.core.ui.KpiBar
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.PercentPill
import com.tinhcd.myesalesmanager.core.ui.StatTile
import com.tinhcd.myesalesmanager.core.ui.StatusPill
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.TeamFilter
import com.tinhcd.myesalesmanager.core.ui.compactMoney
import com.tinhcd.myesalesmanager.core.ui.formatDateTime
import com.tinhcd.myesalesmanager.core.ui.formatDay
import com.tinhcd.myesalesmanager.core.ui.formatDong
import com.tinhcd.myesalesmanager.core.ui.formatIsoDate
import com.tinhcd.myesalesmanager.core.ui.formatWindow
import com.tinhcd.myesalesmanager.domain.model.ActivitiesReport
import com.tinhcd.myesalesmanager.domain.model.OrdersReport
import com.tinhcd.myesalesmanager.domain.model.PeriodKind
import com.tinhcd.myesalesmanager.domain.model.SalesReport
import com.tinhcd.myesalesmanager.domain.model.SkuReport
import com.tinhcd.myesalesmanager.domain.model.StaffBrief
import com.tinhcd.myesalesmanager.domain.model.TradeReport
import com.tinhcd.myesalesmanager.domain.model.TrackingReport
import com.tinhcd.myesalesmanager.domain.model.TrainingReport
import com.tinhcd.myesalesmanager.domain.model.WorkReport
import com.tinhcd.myesalesmanager.domain.model.percent

/**
 * @param onOpenTraining a coaching day picked from Kết quả huấn luyện: its id and
 *   whether it is finished (result screen) or still open (the day itself).
 */
@Composable
fun ReportScreen(
    onBack: () -> Unit,
    onOpenTraining: (String, Boolean) -> Unit = { _, _ -> },
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val team = state.salespersonId == null

    SubScreen(title = state.kind.title, onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { PeriodBar(state, viewModel) }
            item {
                TeamFilter(
                    supervisors = state.supervisors,
                    supervisorId = state.supervisorId,
                    onSupervisor = viewModel::selectSupervisor,
                    reps = state.team,
                    salespersonId = state.salespersonId,
                    onRep = viewModel::selectSalesperson,
                )
            }
            val data = state.data
            when {
                state.loading && data == null -> item { LoadingBox(Modifier.padding(top = 48.dp)) }
                state.error != null -> item { ErrorBox(state.error!!, onRetry = viewModel::refresh) }
                data is ActivitiesReport -> activities(data, team)
                data is SalesReport -> sales(data, team)
                data is OrdersReport -> orders(data, team)
                data is SkuReport -> sku(data)
                data is TradeReport -> trade(data, team)
                data is WorkReport -> work(data, team)
                data is TrainingReport -> training(data, team, onOpenTraining)
                data is TrackingReport -> tracking(data, team)
            }
        }
    }
}

@Composable
private fun PeriodBar(state: ReportState, viewModel: ReportViewModel) {
    when (state.kind.period) {
        PeriodKind.DAY, PeriodKind.MONTH -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        ) {
            IconButton(onClick = { viewModel.step(false) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Trước")
            }
            Text(
                if (state.kind.period == PeriodKind.DAY) formatDay(state.anchor)
                else "Tháng %02d/%d".format(state.anchor.monthValue, state.anchor.year),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { viewModel.step(true) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sau")
            }
        }
        PeriodKind.RANGE -> Column {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                items(RangePreset.entries) { p ->
                    FilterChip(selected = state.preset == p, onClick = { viewModel.selectPreset(p) }, label = { Text(p.label) })
                }
            }
            Text(
                "${formatIsoDate(state.period.from)} – ${formatIsoDate(state.period.to)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

// --- building blocks ---------------------------------------------------------

@Composable
private fun Block(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun Line(left: String, right: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(left, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(right, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Faint(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun who(s: StaffBrief?) = s?.name.orEmpty()

private fun LazyListScope.empty(text: String = "Không có dữ liệu trong kỳ này") {
    item {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            textAlign = TextAlign.Center,
        )
    }
}

private fun orderStatusLabel(s: String) = when (s) {
    "new" -> "Mới"
    "confirmed" -> "Đã xác nhận"
    "cancelled" -> "Đã huỷ"
    else -> s
}

private fun approvalLabel(s: String) = when (s) {
    "pending" -> "Chờ duyệt"
    "approved" -> "Đã duyệt"
    "rejected" -> "Từ chối"
    else -> s
}

// --- Hoạt động trong ngày ------------------------------------------------------------

private fun LazyListScope.activities(d: ActivitiesReport, team: Boolean) {
    val s = d.summary
    item {
        Block {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Kế hoạch", "${s.planned}", Kpi.Slate, Modifier.weight(1f))
                StatTile("Đã ghé", "${s.visited}", Kpi.Blue, Modifier.weight(1f))
                StatTile("Có đơn", "${s.strike}", Kpi.Green, Modifier.weight(1f))
            }
            Line("Không đơn / Đóng cửa", "${s.nonStrike} / ${s.closed}")
            Line("Ngoài tuyến", "${s.unplanned}")
            Line("Doanh số", formatDong(s.orderAmount))
        }
    }
    if (team && d.members.isNotEmpty()) {
        item { Block { Title("Theo nhân viên")
            d.members.forEach { m ->
                Line(m.name, "${m.visited}/${m.planned} · ${m.strike} đơn · ${compactMoney(m.orderAmount)}")
                Faint(
                    if (m.firstCheckIn == null) "Chưa đi tuyến"
                    else "Bắt đầu ${formatDateTime(m.firstCheckIn).takeLast(5)}" +
                        (m.lastCheckOut?.let { " · kết thúc ${formatDateTime(it).takeLast(5)}" } ?: ""),
                )
            }
        } }
    }
    if (d.rows.isEmpty()) empty("Không có lượt viếng thăm nào") else items(d.rows) { r ->
        Block {
            Row {
                Text("${r.customerCode} – ${r.customerName}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (!r.planned) StatusPill("Ngoài tuyến", Kpi.Orange.first())
            }
            Faint(
                listOfNotNull(
                    if (team) who(r.salesperson) else null,
                    r.checkInAt?.let { formatDateTime(it).takeLast(5) },
                    r.minutes?.let { "$it phút" },
                    if (r.orderAmount > 0) formatDong(r.orderAmount) else when (r.status) { "closed" -> "Đóng cửa"; else -> "Không đơn" },
                ).joinToString(" · "),
            )
        }
    }
}

// --- Doanh số tháng ------------------------------------------------------------------

private fun LazyListScope.sales(d: SalesReport, team: Boolean) {
    item {
        Block {
            Text(formatDong(d.revenue), style = MaterialTheme.typography.headlineSmall)
            Line("Số đơn", "${d.orderCount}")
            val pct = percent(d.revenue, d.target)
            if (pct != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Chỉ tiêu ${compactMoney(d.target!!)}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    PercentPill(pct, Kpi.Violet.first())
                }
                KpiBar(pct / 100f, Kpi.Violet)
            } else Faint("Chưa có chỉ tiêu")
        }
    }
    if (team && d.members.isNotEmpty()) {
        item { Block { Title("Theo nhân viên")
            d.members.forEach { m ->
                Line(m.name, compactMoney(m.revenue) + (percent(m.revenue, m.target)?.let { " · $it%" } ?: ""))
                Faint("${m.orders} đơn" + (m.target?.let { " · chỉ tiêu ${compactMoney(it)}" } ?: ""))
            }
        } }
    }
    item { Block { Title("Theo khách hàng (${d.customers.size})")
        if (d.customers.isEmpty()) Faint("Chưa có")
        d.customers.take(20).forEach { c -> Line("${c.code} – ${c.name}", compactMoney(c.revenue)) }
        if (d.customers.size > 20) Faint("… và ${d.customers.size - 20} khách hàng khác")
    } }
    item { Block { Title("Theo sản phẩm (${d.products.size})")
        if (d.products.isEmpty()) Faint("Chưa có")
        d.products.forEach { p ->
            Line(p.name, compactMoney(p.revenue))
            Faint("${p.code} · ${p.baseQty} ${p.baseUom}")
        }
    } }
}

// --- Đơn hàng -----------------------------------------------------------------------

private fun LazyListScope.orders(d: OrdersReport, team: Boolean) {
    item {
        Block {
            Line("Số đơn", "${d.total}")
            Line("Tổng tiền (trừ đơn huỷ)", formatDong(d.totalAmount))
            Faint(d.counts.entries.joinToString(" · ") { "${orderStatusLabel(it.key)}: ${it.value}" })
        }
    }
    if (d.items.isEmpty()) empty() else items(d.items, key = { it.orderNo }) { o ->
        Block {
            Row {
                Text(o.customerName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(formatDong(o.totalAmount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Faint(
                listOfNotNull(o.orderNo, formatIsoDate(o.orderDate), if (team) who(o.salesperson) else null,
                    "${o.lineCount} dòng", orderStatusLabel(o.status)).joinToString(" · "),
            )
        }
    }
    if (d.items.size < d.total) item { Faint("  Hiển thị ${d.items.size}/${d.total} đơn mới nhất") }
}

// --- SKU ----------------------------------------------------------------------------

private fun LazyListScope.sku(d: SkuReport) {
    item {
        Block {
            Line("Số sản phẩm", "${d.items.size}")
            Line("Doanh số", formatDong(d.items.sumOf { it.amountTotal }))
        }
    }
    if (d.items.isEmpty()) empty() else items(d.items, key = { it.code }) { r ->
        Block {
            Line(r.name, "${r.qtyTotal} ${r.uom}")
            Faint("${r.code} · đã xác nhận ${r.qtyConfirmed} · chờ ${r.qtyPending} · ${compactMoney(r.amountTotal)}")
        }
    }
}

// --- Đăng ký chương trình ---------------------------------------------------------------

private fun LazyListScope.trade(d: TradeReport, team: Boolean) {
    item {
        Block {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Trưng bày", "${d.displayCount}", Kpi.Blue, Modifier.weight(1f))
                StatTile("Tích lũy", "${d.loyaltyCount}", Kpi.Violet, Modifier.weight(1f))
                StatTile("Khách hàng", "${d.customerCount}", Kpi.Green, Modifier.weight(1f))
            }
        }
    }
    if (d.items.isEmpty()) empty() else items(d.items) { r ->
        Block {
            Row {
                Text(r.customerName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                StatusPill(approvalLabel(r.status), if (r.status == "approved") Kpi.Green.first() else if (r.status == "rejected") Kpi.Orange.first() else Kpi.Slate.first())
            }
            Faint(
                listOfNotNull(
                    if (r.kind == "display") "Trưng bày" else "Tích lũy",
                    "${r.programCode} ${r.levelName.orEmpty()}".trim(),
                    "${r.portion} suất",
                    formatIsoDate(r.registeredAt),
                    if (team) who(r.salesperson) else null,
                ).joinToString(" · "),
            )
        }
    }
}

// --- Kết quả công việc ---------------------------------------------------------------

private fun LazyListScope.work(d: WorkReport, team: Boolean) {
    item {
        Block {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Hoàn thành", "${d.completed}", Kpi.Green, Modifier.weight(1f))
                StatTile("Đã huỷ", "${d.cancelled}", Kpi.Orange, Modifier.weight(1f))
                StatTile("Tổng", "${d.total}", Kpi.Slate, Modifier.weight(1f))
            }
        }
    }
    if (d.items.isEmpty()) empty("Chưa có công việc nào kết thúc trong kỳ này") else items(d.items) { r ->
        Block {
            Row {
                Text(r.content, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                StatusPill(if (r.status == "completed") "Hoàn thành" else "Đã huỷ",
                    if (r.status == "completed") Kpi.Green.first() else Kpi.Orange.first())
            }
            Faint(listOfNotNull(r.categoryName, formatWindow(r.fromAt, r.toAt), if (team) who(r.salesperson) else null).joinToString(" · "))
            r.outcome?.let { Text("Kết quả: $it", style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

// --- Kết quả huấn luyện ---------------------------------------------------------------

private fun LazyListScope.training(d: TrainingReport, team: Boolean, onOpen: (String, Boolean) -> Unit) {
    item {
        Block {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Đã xong", "${d.trained}/${d.plans}", Kpi.Green, Modifier.weight(1f))
                StatTile("Chưa xong", "${d.untrained}", Kpi.Orange, Modifier.weight(1f))
                StatTile("Điểm", d.score?.let { "$it%" } ?: "—", Kpi.Blue, Modifier.weight(1f))
            }
            Faint("${d.outlets} điểm bán đã kèm cặp · điểm = trung bình bước Đánh giá theo thang tối đa")
        }
    }
    if (team && d.members.isNotEmpty()) {
        item {
            Block {
                Title("Theo nhân viên")
                d.members.forEach { m ->
                    Line(m.name, "${m.trained}/${m.plans} buổi" + (m.score?.let { " · $it%" } ?: ""))
                }
            }
        }
    }
    if (d.items.isEmpty()) empty("Không có kế hoạch huấn luyện trong kỳ này") else items(d.items, key = { it.planId }) { r ->
        val open = r.trainingId
        Card(
            onClick = { if (open != null) onOpen(open, r.status == "completed") },
            enabled = open != null,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Text(r.rep.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    when (r.status) {
                        "completed" -> StatusPill("Đã huấn luyện", Kpi.Green.first())
                        "in_progress" -> StatusPill("Đang ở bước ${(r.currentStep ?: 0) + 1}/8", Kpi.Orange.first())
                        else -> StatusPill("Chưa huấn luyện", Kpi.Slate.first())
                    }
                }
                Faint(
                    listOfNotNull(
                        formatWindow(r.fromAt, r.toAt),
                        if (r.outlets > 0) "${r.outlets} điểm bán" else null,
                        r.score?.let { "điểm $it%" },
                    ).joinToString(" · "),
                )
                r.result?.let { Text("Kết quả: $it", style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
}

// --- Theo dõi bán hàng trong ngày ---------------------------------------------------

private fun LazyListScope.tracking(d: TrackingReport, team: Boolean) {
    item {
        Block {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("Đã ghé", "${d.members.sumOf { it.visited }}/${d.members.sumOf { it.planned }}", Kpi.Blue, Modifier.weight(1f))
                StatTile("Đơn hàng", "${d.members.sumOf { it.orders }}", Kpi.Green, Modifier.weight(1f))
                StatTile("Doanh số", compactMoney(d.members.sumOf { it.revenue }), Kpi.Orange, Modifier.weight(1f))
            }
        }
    }
    if (d.members.isEmpty()) empty() else items(d.members, key = { it.code }) { m ->
        Block {
            Row {
                Text(m.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                when {
                    m.inVisit != null -> StatusPill("Đang ghé", Kpi.Orange.first())
                    m.clockOut != null -> StatusPill("Đã nghỉ", Kpi.Slate.first())
                    m.clockIn != null -> StatusPill("Đang làm", Kpi.Green.first())
                    else -> StatusPill("Chưa vào ca", Kpi.Slate.first())
                }
            }
            m.inVisit?.let { Text("Tại $it" + (m.inVisitSince?.let { s -> " từ ${formatDateTime(s).takeLast(5)}" } ?: ""), style = MaterialTheme.typography.bodyLarge) }
            Faint(
                listOfNotNull(
                    m.clockIn?.let { "vào ${formatDateTime(it).takeLast(5)}" },
                    m.clockOut?.let { "ra ${formatDateTime(it).takeLast(5)}" },
                    "ghé ${m.visited}/${m.planned}",
                    "${m.orders} đơn · ${compactMoney(m.revenue)}",
                    m.batteryPct?.let { "pin $it%" },
                ).joinToString(" · "),
            )
            m.last?.let { l ->
                Faint("Gần nhất: ${l.label} ${formatDateTime(l.at).takeLast(5)}" + (l.detail?.let { " · $it" } ?: ""))
            }
        }
    }
    if (d.orders.isNotEmpty()) {
        item { Text("Đơn hàng trong ngày (${d.orders.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp)) }
        items(d.orders, key = { it.orderNo }) { o ->
            Block {
                Row {
                    Text(o.customer, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(formatDong(o.amount), fontWeight = FontWeight.SemiBold)
                }
                Faint(listOfNotNull(o.orderNo, formatDateTime(o.at).takeLast(5), if (team) who(o.salesperson) else null).joinToString(" · "))
            }
        }
    }
}
