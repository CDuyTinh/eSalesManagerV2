package com.tinhcd.myesalesmanager.feature.workwith

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.PrimaryButton
import com.tinhcd.myesalesmanager.core.ui.SearchBox
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.formatDong
import com.tinhcd.myesalesmanager.core.ui.formatIsoDate
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.foldForSearch
import com.tinhcd.myesalesmanager.domain.model.PromotionPreview
import com.tinhcd.myesalesmanager.domain.model.QtyLine
import com.tinhcd.myesalesmanager.domain.model.VisitProduct
import com.tinhcd.myesalesmanager.domain.model.VisitProducts
import com.tinhcd.myesalesmanager.domain.repository.InStoreRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ARG_QTY_MODE = "mode"

/** One screen for both product-by-product steps: they differ in what a quantity means. */
enum class QtyMode(val code: String, val title: String) {
    COUNT("count", "Kiểm tồn"),
    ORDER("order", "Đặt hàng hộ"),
    ;

    companion object {
        fun of(code: String?) = entries.firstOrNull { it.code == code } ?: COUNT
    }
}

data class ProductQtyState(
    val data: VisitProducts? = null,
    /** "productId|uomCode" → quantity as typed. */
    val qty: Map<String, String> = emptyMap(),
    val query: String = "",
    val note: String = "",
    val preview: PromotionPreview? = null,
    val previewing: Boolean = false,
    val error: String? = null,
    val saving: Boolean = false,
    val message: String? = null,
    val done: Boolean = false,
)

private fun key(productId: String, uom: String) = "$productId|$uom"

@HiltViewModel
class ProductQtyViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: InStoreRepository,
) : ViewModel() {

    private val visitId: String = checkNotNull(savedState[ARG_VISIT_ID])
    val mode: QtyMode = QtyMode.of(savedState[ARG_QTY_MODE])

    private val _state = MutableStateFlow(ProductQtyState())
    val state: StateFlow<ProductQtyState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            when (val r = repository.products(visitId)) {
                is DataResult.Success -> _state.update { s ->
                    // A count already taken on this call is shown back for correction.
                    val counted = if (mode == QtyMode.COUNT) {
                        r.data.products.flatMap { p ->
                            p.units.mapNotNull { u -> u.countedQty?.let { key(p.id, u.uomCode) to it.toString() } }
                        }.toMap()
                    } else {
                        emptyMap()
                    }
                    s.copy(data = r.data, qty = counted, error = null)
                }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }

    fun setQty(productId: String, uom: String, v: String) = _state.update {
        it.copy(qty = it.qty + (key(productId, uom) to v.filter(Char::isDigit).take(6)), preview = null)
    }

    fun query(q: String) = _state.update { it.copy(query = q) }
    fun setNote(v: String) = _state.update { it.copy(note = v) }

    private fun lines(): List<QtyLine> = _state.value.qty.mapNotNull { (k, v) ->
        val n = v.toIntOrNull() ?: return@mapNotNull null
        if (n <= 0) return@mapNotNull null
        val (p, u) = k.split('|')
        QtyLine(p, u, n)
    }

    /** Gross before promotions, priced as the server will price it. */
    fun gross(): Long {
        val products = _state.value.data?.products.orEmpty().associateBy { it.id }
        return lines().sumOf { l ->
            val price = products[l.productId]?.units?.firstOrNull { it.uomCode == l.uomCode }?.price ?: 0
            price * l.qty
        }
    }

    fun preview() {
        val l = lines()
        if (l.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(previewing = true) }
            when (val r = repository.preview(visitId, l)) {
                is DataResult.Success -> _state.update { it.copy(previewing = false, preview = r.data) }
                is DataResult.Failure -> _state.update { it.copy(previewing = false, message = r.error.toMessage()) }
            }
        }
    }

    fun submit() {
        val l = lines()
        if (l.isEmpty()) {
            _state.update { it.copy(message = "Chưa nhập số lượng") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val r = when (mode) {
                QtyMode.COUNT -> repository.countStock(visitId, l)
                QtyMode.ORDER -> when (val o = repository.order(visitId, l, gross(), _state.value.note)) {
                    is DataResult.Success -> DataResult.Success(Unit)
                    is DataResult.Failure -> o
                }
            }
            when (r) {
                is DataResult.Success -> _state.update { it.copy(saving = false, done = true) }
                is DataResult.Failure -> _state.update { it.copy(saving = false, message = r.error.toMessage()) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

/** Kiểm tồn / Đặt hàng hộ: a quantity per product and unit. */
@Composable
fun ProductQtyScreen(onBack: () -> Unit, viewModel: ProductQtyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.done) { if (state.done) onBack() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() } }
    val order = viewModel.mode == QtyMode.ORDER

    SubScreen(
        title = viewModel.mode.title,
        subtitle = if (order) "Đơn ghi cho nhân viên" else state.data?.previousCountDate?.let { "Lần kiểm trước ${formatIsoDate(it)}" },
        onBack = onBack,
        snackbar = snackbar,
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp, 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (order) {
                        val gross = viewModel.gross()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Tạm tính ${formatDong(gross)}", fontWeight = FontWeight.SemiBold)
                                state.preview?.let { p ->
                                    Text(
                                        if (p.earned.isEmpty()) "Không có khuyến mãi" else "KM: ${p.earned.joinToString("; ")}" +
                                            (if (p.totalDiscount > 0) " · giảm ${formatDong(p.totalDiscount)}" else ""),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                            OutlinedButton(onClick = viewModel::preview, enabled = !state.previewing && gross > 0) {
                                Text(if (state.previewing) "Đang tính…" else "Tính KM")
                            }
                        }
                    }
                    PrimaryButton(
                        if (order) "Gửi đơn hàng" else "Lưu kiểm tồn", viewModel::submit, Modifier.fillMaxWidth(),
                        enabled = !state.saving && state.data != null, loading = state.saving,
                    )
                }
            }
        },
    ) { padding ->
        val data = state.data
        when {
            data == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            data == null -> LoadingBox(Modifier.padding(padding))
            else -> {
                val q = state.query.foldForSearch()
                val shown = data.products.filter { q.isEmpty() || "${it.code} ${it.name}".foldForSearch().contains(q) }
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { SearchBox(state.query, viewModel::query, placeholder = "Mã, tên sản phẩm") }
                    items(shown, key = { it.id }) { p -> ProductRow(p, order, state.qty, viewModel::setQty) }
                    if (order) {
                        item {
                            OutlinedTextField(
                                state.note, viewModel::setNote, label = { Text("Ghi chú đơn hàng") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRow(p: VisitProduct, order: Boolean, qty: Map<String, String>, onQty: (String, String, String) -> Unit) {
    // A unit with no price cannot be ordered in; it can still be counted in.
    val units = if (order) p.units.filter { it.price != null } else p.units
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${p.code} – ${p.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (!order && p.previousBaseQty != null) {
                Text("Lần trước: ${p.previousBaseQty} ${p.baseUom}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (units.isEmpty()) {
                Text("Không có giá bán", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                units.forEach { u ->
                    OutlinedTextField(
                        value = qty["${p.id}|${u.uomCode}"].orEmpty(),
                        onValueChange = { onQty(p.id, u.uomCode, it) },
                        label = { Text(u.uomName) },
                        supportingText = if (order && u.price != null) ({ Text(formatDong(u.price)) }) else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(140.dp),
                    )
                }
            }
        }
    }
}
