package com.tinhcd.myesalesmanager.feature.prepare

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.location.LocationProvider
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.PrimaryButton
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.formatDateTime
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.CustomerChange
import com.tinhcd.myesalesmanager.domain.model.CustomerEdit
import com.tinhcd.myesalesmanager.domain.model.NamedOption
import com.tinhcd.myesalesmanager.domain.repository.PrepareRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

const val ARG_CUSTOMER_EDIT_ID = "customerId"

data class EditCustomerState(
    /** As loaded; what the draft is compared against when saving. */
    val original: CustomerEdit? = null,
    val draft: CustomerEdit? = null,
    val error: String? = null,
    val saving: Boolean = false,
    val locating: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class EditCustomerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: PrepareRepository,
    private val location: LocationProvider,
) : ViewModel() {

    private val id: String = checkNotNull(savedState[ARG_CUSTOMER_EDIT_ID])

    private val _state = MutableStateFlow(EditCustomerState())
    val state: StateFlow<EditCustomerState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            when (val r = repository.customer(id)) {
                is DataResult.Success -> _state.update { it.copy(original = r.data, draft = r.data, error = null) }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }

    fun edit(change: (CustomerEdit) -> CustomerEdit) = _state.update { s -> s.copy(draft = s.draft?.let(change)) }

    /** Moves the outlet's pin to where the supervisor is standing. */
    fun useCurrentLocation() {
        viewModelScope.launch {
            _state.update { it.copy(locating = true) }
            val here = runCatching { location.currentLocation() }.getOrNull()
            _state.update { s ->
                if (here == null) {
                    s.copy(locating = false, message = "Không lấy được vị trí hiện tại")
                } else {
                    s.copy(locating = false, draft = s.draft?.copy(lat = here.lat, lng = here.lng))
                }
            }
        }
    }

    fun save() {
        val s = _state.value
        val before = s.original ?: return
        val after = s.draft ?: return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val r = repository.updateCustomer(before, after)) {
                is DataResult.Success -> {
                    _state.update { it.copy(saving = false, message = if (r.data == 0) "Không có thay đổi" else "Đã lưu ${r.data} thay đổi") }
                    if (r.data > 0) load()
                }
                is DataResult.Failure -> _state.update { it.copy(saving = false, message = r.error.toMessage()) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

/** One outlet's details, as the supervisor can correct them (legacy edit_customer_detail). */
@Composable
fun EditCustomerScreen(onBack: () -> Unit, viewModel: EditCustomerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    LaunchedEffect(Unit) {
        permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() } }

    val d = state.draft
    SubScreen(
        title = d?.let { "${it.code} – ${it.name}" } ?: "Sửa khách hàng",
        onBack = onBack,
        snackbar = snackbar,
        bottomBar = {
            if (d != null) {
                Surface(tonalElevation = 3.dp) {
                    PrimaryButton(
                        "Lưu thay đổi", viewModel::save,
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp, 10.dp),
                        enabled = !state.saving && state.draft != state.original,
                        loading = state.saving,
                    )
                }
            }
        },
    ) { padding ->
        when {
            d == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            d == null -> LoadingBox(Modifier.padding(padding))
            else -> Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(d.name, { v -> viewModel.edit { it.copy(name = v) } }, label = { Text("Tên điểm bán *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(d.contactName.orEmpty(), { v -> viewModel.edit { it.copy(contactName = v) } }, label = { Text("Người liên hệ") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    d.phone.orEmpty(), { v -> viewModel.edit { it.copy(phone = v) } },
                    label = { Text("Điện thoại") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(d.address.orEmpty(), { v -> viewModel.edit { it.copy(address = v) } }, label = { Text("Địa chỉ *") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OptionGroup("Hạng", d.classes, d.classId) { id -> viewModel.edit { it.copy(classId = id) } }
                OptionGroup("Kênh", d.channels, d.channelId) { id -> viewModel.edit { it.copy(channelId = id) } }
                OptionGroup("Loại cửa hàng", d.shopTypes, d.shopTypeId) { id -> viewModel.edit { it.copy(shopTypeId = id) } }

                Text("Vị trí", style = MaterialTheme.typography.labelSmall)
                Text(
                    if (d.lat != null && d.lng != null) String.format(Locale.US, "%.6f, %.6f", d.lat, d.lng) else "Chưa có toạ độ",
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedButton(onClick = viewModel::useCurrentLocation, enabled = !state.locating) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Text(if (state.locating) " Đang lấy vị trí…" else " Lấy vị trí hiện tại")
                }

                if (d.history.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Lịch sử chỉnh sửa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    d.history.forEach { HistoryEntry(it, d) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionGroup(label: String, options: List<NamedOption>, selected: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { o -> FilterChip(selected = selected == o.id, onClick = { onSelect(o.id) }, label = { Text(o.name) }) }
        }
    }
}

private val fieldLabels = mapOf(
    "name" to "Tên", "contact_name" to "Người liên hệ", "phone" to "Điện thoại", "address" to "Địa chỉ",
    "class_id" to "Hạng", "channel_id" to "Kênh", "shop_type_id" to "Loại cửa hàng", "lat" to "Vĩ độ", "lng" to "Kinh độ",
)

@Composable
private fun HistoryEntry(h: CustomerChange, d: CustomerEdit) {
    // Option ids read better as the names they stand for.
    val names = (d.classes + d.channels + d.shopTypes).associate { it.id to it.name }
    fun show(v: String?) = v?.let { names[it] ?: it } ?: "—"
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("${formatDateTime(h.at)} · ${h.actor}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        h.fields.forEach { (field, change) ->
            Text("${fieldLabels[field] ?: field}: ${show(change.first)} → ${show(change.second)}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
