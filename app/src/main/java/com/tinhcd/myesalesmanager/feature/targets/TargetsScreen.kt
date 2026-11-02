package com.tinhcd.myesalesmanager.feature.targets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.PrimaryButton
import com.tinhcd.myesalesmanager.core.ui.SkeletonList
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.TargetSheet
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import com.tinhcd.myesalesmanager.domain.repository.TargetRepository
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

data class TargetsState(
    val month: LocalDate = todayVn().withDayOfMonth(1),
    val reps: List<TeamMember> = emptyList(),
    val repId: String? = null,
    val sheet: TargetSheet? = null,
    /** The figures as typed: digits only, empty for none. */
    val revenue: String = "",
    val orders: String = "",
    val categories: Map<String, String> = emptyMap(),
    /** The products on the form, in the order they were added. */
    val products: Map<String, String> = emptyMap(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

@HiltViewModel
class TargetsViewModel @Inject constructor(
    private val repository: TargetRepository,
    auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TargetsState())
    val state: StateFlow<TargetsState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch {
            auth.team.collect { reps ->
                val first = _state.value.repId == null
                _state.update { it.copy(reps = reps, repId = it.repId ?: reps.firstOrNull()?.id) }
                if (first && reps.isNotEmpty()) load()
            }
        }
    }

    fun selectRep(id: String) {
        _state.update { it.copy(repId = id) }
        load()
    }

    fun shiftMonth(months: Long) {
        _state.update { it.copy(month = it.month.plusMonths(months)) }
        load()
    }

    /**  message kept through the reload, for "saved" to outlive the save's own refresh. */
    fun load(message: String? = null) {
        val s = _state.value
        val rep = s.repId ?: return
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, message = message) }
            when (val r = repository.sheet(s.month, rep)) {
                is DataResult.Success -> {
                    val sheet = r.data
                    _state.update {
                        it.copy(
                            loading = false,
                            sheet = sheet,
                            revenue = sheet.revenueTarget?.takeIf { v -> v > 0 }?.toString().orEmpty(),
                            orders = sheet.orderTarget?.takeIf { v -> v > 0 }?.toString().orEmpty(),
                            categories = sheet.categories.associate { c -> c.id to (c.target?.toString().orEmpty()) },
                            products = sheet.products.filter { p -> p.target != null }
                                .associate { p -> p.id to p.target.toString() },
                        )
                    }
                }
                is DataResult.Failure -> _state.update { it.copy(loading = false, sheet = null, error = r.error.toMessage()) }
            }
        }
    }

    fun setRevenue(v: String) = _state.update { it.copy(revenue = v.digits(), message = null) }
    fun setOrders(v: String) = _state.update { it.copy(orders = v.digits().take(6), message = null) }
    fun setCategory(id: String, v: String) =
        _state.update { it.copy(categories = it.categories + (id to v.digits()), message = null) }
    fun setProduct(id: String, v: String) =
        _state.update { it.copy(products = it.products + (id to v.digits()), message = null) }
    fun addProduct(id: String) = _state.update { it.copy(products = it.products + (id to "")) }
    fun removeProduct(id: String) = _state.update { it.copy(products = it.products - id, message = null) }

    fun save() {
        val s = _state.value
        val rep = s.repId ?: return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null, message = null) }
            val r = repository.save(
                salespersonId = rep,
                month = s.month,
                revenueTarget = s.revenue.toLongOrNull(),
                orderTarget = s.orders.toIntOrNull(),
                categories = s.categories.amounts(),
                products = s.products.amounts(),
            )
            when (r) {
                is DataResult.Success -> {
                    _state.update { it.copy(saving = false) }
                    load(message = "Đã lưu chỉ tiêu")
                }
                is DataResult.Failure -> _state.update { it.copy(saving = false, error = r.error.toMessage()) }
            }
        }
    }

    // Up to 15 digits: a trillion đồng is past any rep's month, and the field
    // stays inside a Long.
    private fun String.digits() = filter(Char::isDigit).trimStart('0').take(15)

    private fun Map<String, String>.amounts() =
        mapNotNull { (id, v) -> v.toLongOrNull()?.takeIf { it > 0 }?.let { id to it } }.toMap()
}

/**
 * Chỉ tiêu nhân viên: a month and a rep, then the overall figures, one field
 * per product group, and the products given a target of their own. An AM or
 * RM edits the current month and later ones; anyone else, or a past month,
 * reads.
 */
@Composable
fun TargetsScreen(onBack: () -> Unit, viewModel: TargetsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheet = state.sheet
    val editable = sheet?.canEdit == true
    var adding by rememberSaveable { mutableStateOf(false) }

    SubScreen(
        title = "Chỉ tiêu nhân viên",
        onBack = onBack,
        subtitle = "Tháng %02d/%d".format(state.month.monthValue, state.month.year),
        bottomBar = {
            if (editable) {
                Surface(shadowElevation = 8.dp) {
                    PrimaryButton(
                        "Lưu chỉ tiêu",
                        viewModel::save,
                        Modifier.navigationBarsPadding().padding(16.dp),
                        enabled = !state.loading,
                        loading = state.saving,
                        height = 48.dp,
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MonthStepper(state.month, onShift = viewModel::shiftMonth) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    items(state.reps, key = { it.id }) { rep ->
                        FilterChip(
                            selected = rep.id == state.repId,
                            onClick = { viewModel.selectRep(rep.id) },
                            label = { Text(rep.fullName) },
                        )
                    }
                }
            }
            state.message?.let { item { Notice(it, isError = false) } }
            state.error?.takeIf { sheet != null }?.let { item { Notice(it, isError = true) } }

            when {
                state.reps.isEmpty() -> item { ErrorBox("Chưa có nhân viên trong nhóm") }
                state.loading && sheet == null -> item { SkeletonList(count = 3) }
                sheet == null -> item { ErrorBox(state.error ?: "Không tải được chỉ tiêu", onRetry = viewModel::load) }
                else -> {
                    if (!editable) {
                        item {
                            Notice(
                                "Chỉ xem. Chỉ quản lý khu vực / vùng được đặt chỉ tiêu, và không sửa tháng đã qua.",
                                isError = false,
                            )
                        }
                    }
                    item {
                        FormCard("Tổng tháng") {
                            AmountField("Doanh số (VND)", state.revenue, editable, viewModel::setRevenue)
                            AmountField("Số đơn hàng", state.orders, editable, viewModel::setOrders, money = false)
                        }
                    }
                    item {
                        FormCard("Theo nhóm hàng") {
                            sheet.categories.forEach { c ->
                                AmountField(c.name, state.categories[c.id].orEmpty(), editable, { viewModel.setCategory(c.id, it) })
                            }
                        }
                    }
                    item {
                        val names = sheet.products.associateBy { it.id }
                        FormCard("Theo sản phẩm") {
                            if (state.products.isEmpty()) {
                                Text(
                                    "Chưa có sản phẩm nào có chỉ tiêu riêng",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            state.products.forEach { (id, value) ->
                                val p = names[id]
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AmountField(
                                        p?.let { "${it.code} · ${it.name}" } ?: id,
                                        value, editable, { viewModel.setProduct(id, it) },
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (editable) {
                                        IconButton(onClick = { viewModel.removeProduct(id) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Bỏ sản phẩm")
                                        }
                                    }
                                }
                            }
                            if (editable && sheet.products.any { it.id !in state.products }) {
                                OutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Text("  Thêm sản phẩm")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (adding && sheet != null) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Thêm sản phẩm") },
            text = {
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(sheet.products.filter { it.id !in state.products }, key = { it.id }) { p ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addProduct(p.id)
                                    adding = false
                                }
                                .padding(vertical = 12.dp),
                        ) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge)
                            Text(p.code, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { adding = false }) { Text("Đóng") } },
        )
    }
}

@Composable
private fun MonthStepper(month: LocalDate, onShift: (Long) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
        IconButton(onClick = { onShift(-1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Tháng trước")
        }
        Text(
            "Tháng %02d/%d".format(month.monthValue, month.year),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onShift(1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Tháng sau")
        }
    }
}

@Composable
private fun FormCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun AmountField(
    label: String,
    value: String,
    editable: Boolean,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    money: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text("Chưa đặt") },
        readOnly = !editable,
        singleLine = true,
        suffix = if (money && value.isNotEmpty()) ({ Text("đ") }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = ThousandsTransformation,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun Notice(text: String, isError: Boolean) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

/** Digits shown as "1.250.000" while the field holds "1250000". */
internal object ThousandsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val out = StringBuilder()
        raw.forEachIndexed { i, ch ->
            if (i > 0 && (raw.length - i) % 3 == 0) out.append('.')
            out.append(ch)
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                // Separators before this digit: one per full group of three to its right.
                val digitsAfter = raw.length - offset
                val totalSeps = (raw.length - 1) / 3
                val sepsAfter = if (digitsAfter <= 0) 0 else (digitsAfter - 1) / 3
                return offset + (totalSeps - sepsAfter).coerceAtLeast(0)
            }

            override fun transformedToOriginal(offset: Int): Int {
                var digits = 0
                for (i in 0 until offset.coerceAtMost(out.length)) if (out[i] != '.') digits++
                return digits
            }
        }
        return TransformedText(AnnotatedString(out.toString()), mapping)
    }
}
