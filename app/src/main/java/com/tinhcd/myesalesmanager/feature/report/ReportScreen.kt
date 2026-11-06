package com.tinhcd.myesalesmanager.feature.report

import com.tinhcd.myesalesmanager.domain.model.OrderRow
import com.tinhcd.myesalesmanager.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.size
import androidx.paging.compose.itemKey
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.LazyPagingItems
import androidx.paging.LoadState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.Kpi
import com.tinhcd.myesalesmanager.core.ui.KpiBar
import com.tinhcd.myesalesmanager.core.ui.PercentPill
import com.tinhcd.myesalesmanager.core.ui.SkeletonList
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
import com.tinhcd.myesalesmanager.core.ui.theme.brand
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
    val orderRows = viewModel.orderRows.collectAsLazyPagingItems()
    val team = state.salespersonId == null
    val context = LocalContext.current

    var filtering by rememberSaveable { mutableStateOf(false) }
    // A rep, and optionally one outlet of theirs, on the report's day.
    val openPhotos: (String?, String?) -> Unit = { id, customer ->
        val name = state.team.firstOrNull { it.id == id }?.fullName
        if (id != null) viewModel.openPhotos(id, name ?: "", customer)
    }

    Box(Modifier.fillMaxSize()) {
        SubScreen(
            title = state.kind.title,
            onBack = onBack,
            // The period is no longer on the page, so the header says it.
            subtitle = periodLabel(state),
            band = true,
            actions = {
                IconButton(onClick = { filtering = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.mg_204), tint = MaterialTheme.brand.onHeader)
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // The team pickers, in a card so they read on the header band.
                item {
                    WhiteCard {
                        TeamFilter(
                            supervisors = state.supervisors,
                            supervisorId = state.supervisorId,
                            onSupervisor = viewModel::selectSupervisor,
                            reps = state.team,
                            salespersonId = state.salespersonId,
                            onRep = viewModel::selectSalesperson,
                            place = state.place,
                            onPlace = viewModel::selectPlace,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    }
                }
                val data = state.data
                if (data != null && !state.loading && state.error == null) {
                    item {
                        TextButton(
                            // Orders are fetched in full first: the file has every page, not just those seen.
                            onClick = { viewModel.forExport { all -> all?.let { shareReport(context, state.kind, state.period, it) } } },
                            enabled = !state.loadingMore,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Text(stringResource(R.string.mg_215))
                        }
                    }
                }
                when {
                    state.loading && data == null -> item { SkeletonList(count = 5) }
                    state.error != null -> item { ErrorBox(state.error!!, onRetry = viewModel::refresh) }
                    data is ActivitiesReport -> activities(data, team) { sp, customer -> openPhotos(sp?.id ?: state.salespersonId, customer) }
                    data is SalesReport -> sales(data, team)
                    data is OrdersReport -> orders(data, team, orderRows)
                    data is SkuReport -> sku(data)
                    data is TradeReport -> trade(data, team)
                    data is WorkReport -> work(data, team)
                    data is TrainingReport -> training(data, team, onOpenTraining)
                    data is TrackingReport -> tracking(data, team) { code -> openPhotos(state.team.firstOrNull { it.code == code }?.id, null) }
                }
            }
        }

        state.photos?.let { StaffPhotosSheet(it, day = formatDay(state.anchor), onClose = viewModel::closePhotos) }

        AnimatedVisibility(
            visible = filtering,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            ReportFilterScreen(
                kind = state.kind.period,
                anchor = state.anchor,
                from = state.rangeFrom,
                to = state.rangeTo,
                onDismiss = { filtering = false },
                onApply = viewModel::applyPeriod,
            )
        }
    }
}

/** The period on screen, as the header states it. */
private fun periodLabel(state: ReportState): String = when (state.kind.period) {
    PeriodKind.DAY -> "Ngày ${formatDay(state.anchor)}"
    PeriodKind.MONTH -> "Tháng %02d/%d".format(state.anchor.monthValue, state.anchor.year)
    PeriodKind.RANGE -> "${formatIsoDate(state.period.from)} – ${formatIsoDate(state.period.to)}"
}

// --- building blocks ---------------------------------------------------------

/** White and lifted, as the dashboard's cards are; the first sits over the header band. */
@Composable
private fun WhiteCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) { content() }
}

@Composable
private fun Block(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    WhiteCard {
        Column(
            (if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) { content() }
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

private fun LazyListScope.activities(d: ActivitiesReport, team: Boolean, onPhotos: (StaffBrief?, String) -> Unit) {
    val s = d.summary
    item {
        Block {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(stringResource(R.string.mg_195), "${s.planned}", Kpi.Slate, Modifier.weight(1f))
                StatTile(stringResource(R.string.mg_199), "${s.visited}", Kpi.Blue, Modifier.weight(1f))
                StatTile(stringResource(R.string.mg_221), "${s.strike}", Kpi.Green, Modifier.weight(1f))
            }
            Line(stringResource(R.string.mg_222), "${s.nonStrike} / ${s.closed}")
            Line(stringResource(R.string.mg_223), "${s.unplanned}")
            Line(stringResource(R.string.mg_33), formatDong(s.orderAmount))
        }
    }
    if (team && d.members.isNotEmpty()) {
        item { Block { Title(stringResource(R.string.mg_224))
            d.members.forEach { m ->
                Line(m.name, "${m.visited}/${m.planned} · ${m.strike} đơn · ${compactMoney(m.orderAmount)}")
                Faint(
                    if (m.firstCheckIn == null) stringResource(R.string.mg_225)
                    else "Bắt đầu ${formatDateTime(m.firstCheckIn).takeLast(5)}" +
                        (m.lastCheckOut?.let { " · kết thúc ${formatDateTime(it).takeLast(5)}" } ?: ""),
                )
            }
        } }
    }
    if (d.rows.isEmpty()) empty("Không có lượt viếng thăm nào") else items(d.rows) { r ->
        Block(onClick = { onPhotos(r.salesperson, r.customerCode) }) {
            Row {
                Text("${r.customerCode} – ${r.customerName}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (!r.planned) StatusPill(stringResource(R.string.mg_223), Kpi.Orange.first())
            }
            Faint(
                listOfNotNull(
                    if (team) who(r.salesperson) else null,
                    r.checkInAt?.let { formatDateTime(it).takeLast(5) },
                    r.minutes?.let { "$it phút" },
                    if (r.orderAmount > 0) formatDong(r.orderAmount) else when (r.status) { "closed" -> stringResource(R.string.mg_227); else -> stringResource(R.string.mg_228) },
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
            Line(stringResource(R.string.mg_181), "${d.orderCount}")
            val pct = percent(d.revenue, d.target)
            if (pct != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Chỉ tiêu ${compactMoney(d.target!!)}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    PercentPill(pct, Kpi.Violet.first())
                }
                KpiBar(pct / 100f, Kpi.Violet)
            } else Faint(stringResource(R.string.mg_39))
        }
    }
    if (team && d.members.isNotEmpty()) {
        item { Block { Title(stringResource(R.string.mg_224))
            d.members.forEach { m ->
                Line(m.name, compactMoney(m.revenue) + (percent(m.revenue, m.target)?.let { " · $it%" } ?: ""))
                Faint("${m.orders} đơn" + (m.target?.let { " · chỉ tiêu ${compactMoney(it)}" } ?: ""))
            }
        } }
    }
    item { Block { Title("Theo khách hàng (${d.customers.size})")
        if (d.customers.isEmpty()) Faint(stringResource(R.string.mg_229))
        d.customers.take(20).forEach { c -> Line("${c.code} – ${c.name}", compactMoney(c.revenue)) }
        if (d.customers.size > 20) Faint("… và ${d.customers.size - 20} khách hàng khác")
    } }
    item { Block { Title("Theo sản phẩm (${d.products.size})")
        if (d.products.isEmpty()) Faint(stringResource(R.string.mg_229))
        d.products.forEach { p ->
            Line(p.name, compactMoney(p.revenue))
            Faint("${p.code} · ${p.baseQty} ${p.baseUom}")
        }
    } }
}

// --- Đơn hàng -----------------------------------------------------------------------

private fun LazyListScope.orders(d: OrdersReport, team: Boolean, rows: LazyPagingItems<OrderRow>) {
    item {
        Block {
            Line(stringResource(R.string.mg_181), "${d.total}")
            Line(stringResource(R.string.mg_230), formatDong(d.totalAmount))
            Faint(d.counts.entries.joinToString(" · ") { "${orderStatusLabel(it.key)}: ${it.value}" })
        }
    }
    when (rows.loadState.refresh) {
        is LoadState.Loading -> if (rows.itemCount == 0) return item { SkeletonList(count = 3) }
        is LoadState.Error -> return item { PageError(stringResource(R.string.mg_231), rows::retry) }
        is LoadState.NotLoading -> if (rows.itemCount == 0) return empty()
    }
    items(count = rows.itemCount, key = rows.itemKey { it.orderNo }) { index ->
        val o = rows[index] ?: return@items
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
    when (rows.loadState.append) {
        is LoadState.Loading -> item {
            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        }
        is LoadState.Error -> item { PageError(stringResource(R.string.mg_232), rows::retry) }
        is LoadState.NotLoading -> Unit
    }
}

@Composable
private fun PageError(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.permission_ask_again)) }
    }
}

// --- SKU ----------------------------------------------------------------------------

private fun LazyListScope.sku(d: SkuReport) {
    item {
        Block {
            Line(stringResource(R.string.mg_233), "${d.items.size}")
            Line(stringResource(R.string.mg_33), formatDong(d.items.sumOf { it.amountTotal }))
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
                StatTile(stringResource(R.string.mg_160), "${d.displayCount}", Kpi.Blue, Modifier.weight(1f))
                StatTile(stringResource(R.string.mg_161), "${d.loyaltyCount}", Kpi.Violet, Modifier.weight(1f))
                StatTile(stringResource(R.string.mg_176), "${d.customerCount}", Kpi.Green, Modifier.weight(1f))
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
                    if (r.kind == "display") stringResource(R.string.mg_160) else stringResource(R.string.mg_161),
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
                StatTile(stringResource(R.string.mg_137), "${d.completed}", Kpi.Green, Modifier.weight(1f))
                StatTile(stringResource(R.string.mg_220), "${d.cancelled}", Kpi.Orange, Modifier.weight(1f))
                StatTile(stringResource(R.string.mg_234), "${d.total}", Kpi.Slate, Modifier.weight(1f))
            }
        }
    }
    if (d.items.isEmpty()) empty("Chưa có công việc nào kết thúc trong kỳ này") else items(d.items) { r ->
        Block {
            Row {
                Text(r.content, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                StatusPill(if (r.status == "completed") stringResource(R.string.mg_137) else stringResource(R.string.mg_220),
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
                StatTile(stringResource(R.string.mg_90), "${d.trained}/${d.plans}", Kpi.Green, Modifier.weight(1f))
                StatTile(stringResource(R.string.mg_236), "${d.untrained}", Kpi.Orange, Modifier.weight(1f))
                StatTile(stringResource(R.string.mg_198), d.score?.let { "$it%" } ?: "—", Kpi.Blue, Modifier.weight(1f))
            }
            Faint("${d.outlets} điểm bán đã kèm cặp · điểm = trung bình bước Đánh giá theo thang tối đa")
        }
    }
    if (team && d.members.isNotEmpty()) {
        item {
            Block {
                Title(stringResource(R.string.mg_224))
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
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Text(r.rep.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    when (r.status) {
                        "completed" -> StatusPill(stringResource(R.string.mg_196), Kpi.Green.first())
                        "in_progress" -> StatusPill("Đang ở bước ${(r.currentStep ?: 0) + 1}/8", Kpi.Orange.first())
                        else -> StatusPill(stringResource(R.string.mg_238), Kpi.Slate.first())
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

private fun LazyListScope.tracking(d: TrackingReport, team: Boolean, onPhotos: (code: String) -> Unit) {
    item {
        Block {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(stringResource(R.string.mg_199), "${d.members.sumOf { it.visited }}/${d.members.sumOf { it.planned }}", Kpi.Blue, Modifier.weight(1f))
                StatTile(stringResource(R.string.mg_30), "${d.members.sumOf { it.orders }}", Kpi.Green, Modifier.weight(1f))
                StatTile(stringResource(R.string.mg_33), compactMoney(d.members.sumOf { it.revenue }), Kpi.Orange, Modifier.weight(1f))
            }
        }
    }
    if (d.members.isEmpty()) empty() else items(d.members, key = { it.code }) { m ->
        Block {
            Row {
                Text(m.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                when {
                    m.inVisit != null -> StatusPill(stringResource(R.string.mg_239), Kpi.Orange.first())
                    m.clockOut != null -> StatusPill(stringResource(R.string.mg_240), Kpi.Slate.first())
                    m.clockIn != null -> StatusPill(stringResource(R.string.mg_241), Kpi.Green.first())
                    else -> StatusPill(stringResource(R.string.mg_242), Kpi.Slate.first())
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
            if (m.clockIn != null || m.visited > 0) {
                TextButton(onClick = { onPhotos(m.code) }, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Text(stringResource(R.string.mg_243))
                }
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
