package com.tinhcd.myesalesmanager.feature.prepare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
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
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.SearchBox
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.formatDateTime
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.foldForSearch
import com.tinhcd.myesalesmanager.domain.model.SiteStock
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

data class StockState(
    val stock: SiteStock? = null,
    val query: String = "",
    val error: String? = null,
)

@HiltViewModel
class StockViewModel @Inject constructor(
    private val repository: PrepareRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StockState())
    val state: StateFlow<StockState> = _state.asStateFlow()

    init { load(null) }

    fun load(siteId: String?) {
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            when (val r = repository.stock(siteId)) {
                is DataResult.Success -> _state.update { it.copy(stock = r.data) }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }

    fun query(q: String) = _state.update { it.copy(query = q) }
}

/**
 * Tồn kho: what the distributor's warehouses hold (legacy prepare_inventory and
 * inventory_company). Read-only — stock is the warehouse system's to change.
 */
@Composable
fun StockScreen(onBack: () -> Unit, viewModel: StockViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val number = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))

    SubScreen(title = "Tồn kho", subtitle = "Kho nhà phân phối", onBack = onBack) { padding ->
        val stock = state.stock
        when {
            stock == null && state.error != null -> ErrorBox(state.error!!, onRetry = { viewModel.load(null) }, modifier = Modifier.padding(padding))
            stock == null -> LoadingBox(Modifier.padding(padding))
            else -> Column(Modifier.padding(padding).fillMaxSize()) {
                if (stock.sites.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(16.dp, 8.dp)) {
                        items(stock.sites, key = { it.id }) { s ->
                            FilterChip(selected = stock.siteId == s.id, onClick = { viewModel.load(s.id) }, label = { Text(s.name) })
                        }
                    }
                } else {
                    stock.sites.firstOrNull()?.let {
                        Text(it.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp, 8.dp))
                    }
                }
                SearchBox(state.query, viewModel::query, placeholder = "Mã, tên sản phẩm", modifier = Modifier.padding(horizontal = 16.dp))
                val q = state.query.foldForSearch()
                val shown = stock.items.filter { q.isEmpty() || "${it.productCode} ${it.productName}".foldForSearch().contains(q) }
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    item {
                        Text(
                            "${shown.size} sản phẩm",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (shown.isEmpty()) item { Text("Không có sản phẩm", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(shown, key = { it.productCode }) { i ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(i.productName, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    listOfNotNull(i.productCode, i.updatedAt?.let { "cập nhật ${formatDateTime(it)}" }).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text("${number.format(i.qtyBase)} ${i.baseUom}", fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
