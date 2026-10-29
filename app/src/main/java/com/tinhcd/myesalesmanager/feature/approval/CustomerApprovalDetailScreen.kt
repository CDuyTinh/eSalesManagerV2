package com.tinhcd.myesalesmanager.feature.approval

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import com.tinhcd.myesalesmanager.core.ui.formatIsoDate
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.ApprovalStatus
import com.tinhcd.myesalesmanager.domain.model.CustomerApprovalDetail
import com.tinhcd.myesalesmanager.domain.model.Decision
import com.tinhcd.myesalesmanager.domain.repository.ApprovalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ARG_CUSTOMER_ID = "customerId"

data class CustomerDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val detail: CustomerApprovalDetail? = null,
    /** Route to put the outlet on when approving; null = do not add to a route. */
    val routeId: String? = null,
    val pending: PendingDecision? = null,
    val deciding: Boolean = false,
    val message: String? = null,
    val done: Boolean = false,
)

@HiltViewModel
class CustomerApprovalDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: ApprovalRepository,
) : ViewModel() {

    private val id: String = checkNotNull(savedState[ARG_CUSTOMER_ID])

    private val _state = MutableStateFlow(CustomerDetailState())
    val state: StateFlow<CustomerDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.customerDetail(id)) {
                is DataResult.Success -> _state.update { s ->
                    // Preselect the rep's only route: the common case is one route,
                    // and an approved outlet on no route is one nobody visits.
                    val routes = result.data.routes
                    s.copy(
                        loading = false,
                        detail = result.data,
                        routeId = s.routeId ?: routes.singleOrNull()?.id,
                    )
                }
                is DataResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.error.toMessage())
                }
            }
        }
    }

    fun selectRoute(routeId: String?) = _state.update { it.copy(routeId = routeId) }

    fun ask(approve: Boolean) {
        val d = _state.value.detail ?: return
        _state.update {
            it.copy(pending = PendingDecision(d.summary.id, approve, "${d.summary.name} (${d.summary.code})"))
        }
    }

    fun dismiss() = _state.update { it.copy(pending = null) }

    fun confirm(note: String, @Suppress("UNUSED_PARAMETER") qty: Int?) {
        val s = _state.value
        val pending = s.pending ?: return
        viewModelScope.launch {
            _state.update { it.copy(deciding = true) }
            val decision = Decision(
                approve = pending.approve,
                note = note,
                routeId = s.routeId.takeIf { pending.approve },
            )
            when (val result = repository.decideCustomer(pending.id, decision)) {
                is DataResult.Success -> _state.update { it.copy(deciding = false, pending = null, done = true) }
                is DataResult.Failure -> _state.update {
                    it.copy(deciding = false, message = result.error.toMessage())
                }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

@Composable
fun CustomerApprovalDetailScreen(
    onBack: () -> Unit,
    viewModel: CustomerApprovalDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.done) { if (state.done) onBack() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val detail = state.detail
    SubScreen(
        title = "Khách hàng mới",
        subtitle = detail?.summary?.code,
        onBack = onBack,
        snackbar = snackbar,
        bottomBar = {
            if (detail?.summary?.status == ApprovalStatus.PENDING) {
                Surface(tonalElevation = 3.dp) {
                    DecisionButtons(
                        onApprove = { viewModel.ask(true) },
                        onReject = { viewModel.ask(false) },
                        modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        },
    ) { padding ->
        when {
            state.loading && detail == null -> LoadingBox(Modifier.padding(padding))
            detail == null -> ErrorBox(
                message = state.error ?: "Không tải được thông tin",
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )
            else -> DetailBody(
                detail = detail,
                routeId = state.routeId,
                onSelectRoute = viewModel::selectRoute,
                padding = padding,
            )
        }
    }

    state.pending?.let {
        DecisionDialog(pending = it, busy = state.deciding, onConfirm = viewModel::confirm, onDismiss = viewModel::dismiss)
    }
}

@Composable
private fun DetailBody(
    detail: CustomerApprovalDetail,
    routeId: String?,
    onSelectRoute: (String?) -> Unit,
    padding: PaddingValues,
) {
    val c = detail.summary
    val pending = c.status == ApprovalStatus.PENDING

    LazyColumn(
        modifier = Modifier.padding(padding).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            c.name,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        StatusBadge(c.status)
                    }
                    LabeledLine("Địa chỉ", c.address)
                    LabeledLine(
                        "Khu vực",
                        listOfNotNull(detail.ward, detail.district, detail.province).joinToString(", "),
                    )
                    LabeledLine("Điện thoại", c.phone)
                    LabeledLine("Người liên hệ", detail.contactName)
                    LabeledLine("Loại khách hàng", detail.className)
                    LabeledLine("Kênh", detail.channelName)
                    LabeledLine("Loại cửa hàng", detail.shopTypeName)
                    if (detail.lat != null && detail.lng != null) {
                        LabeledLine("Toạ độ", "%.5f, %.5f".format(detail.lat, detail.lng))
                    }
                    LabeledLine("Nhân viên", c.salesperson?.let { "${it.name} (${it.code})" })
                    LabeledLine("Ngày đăng ký", formatIsoDate(c.createdAt))
                    LabeledLine("Ghi chú NV", c.note)
                    LabeledLine("Ghi chú duyệt", c.decisionNote)
                }
            }
        }

        item {
            SectionTitle("Hình ảnh (${detail.photos.size})")
            if (detail.photos.isEmpty()) {
                Text(
                    "Nhân viên chưa chụp ảnh nào",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(detail.photos) { photo ->
                        Column(Modifier.width(160.dp)) {
                            AsyncImage(
                                model = photo.url,
                                contentDescription = photo.typeName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                            Text(photo.typeName, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        if (pending) {
            item { SectionTitle("Đưa vào tuyến khi duyệt") }
            if (detail.routes.isEmpty()) {
                item {
                    Text(
                        "Nhân viên chưa có tuyến nào. Khách hàng sẽ được duyệt nhưng chưa nằm trong tuyến.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item { RouteRow("Không đưa vào tuyến", selected = routeId == null) { onSelectRoute(null) } }
                items(detail.routes, key = { it.id }) { r ->
                    RouteRow("${r.code} – ${r.name}", selected = routeId == r.id) { onSelectRoute(r.id) }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun RouteRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
