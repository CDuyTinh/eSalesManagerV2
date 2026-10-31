package com.tinhcd.myesalesmanager.feature.workwith

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.tinhcd.myesalesmanager.core.photo.PhotoStore
import com.tinhcd.myesalesmanager.core.photo.PhotoTarget
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.Kpi
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.PrimaryButton
import com.tinhcd.myesalesmanager.core.ui.StatusPill
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.PosmCheck
import com.tinhcd.myesalesmanager.domain.model.PosmCondition
import com.tinhcd.myesalesmanager.domain.model.PosmPlaced
import com.tinhcd.myesalesmanager.domain.repository.InStoreRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** The supervisor's check of one asset, while it is being filled in. */
data class PosmDraft(
    val item: PosmPlaced,
    val counted: String = item.placedQty.toString(),
    val condition: PosmCondition = PosmCondition.USABLE,
    val remark: String = "",
    val photos: List<String> = emptyList(),
)

data class PosmCheckState(
    val items: List<PosmPlaced>? = null,
    val draft: PosmDraft? = null,
    val error: String? = null,
    val saving: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class PosmCheckViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: InStoreRepository,
    private val photos: PhotoStore,
) : ViewModel() {

    private val visitId: String = checkNotNull(savedState[ARG_VISIT_ID])

    private val _state = MutableStateFlow(PosmCheckState())
    val state: StateFlow<PosmCheckState> = _state.asStateFlow()

    private var pending: PhotoTarget? = null

    init { load() }

    fun load() {
        viewModelScope.launch {
            when (val r = repository.posm(visitId)) {
                is DataResult.Success -> _state.update { it.copy(items = r.data, error = null) }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }

    fun open(item: PosmPlaced) = _state.update { it.copy(draft = PosmDraft(item)) }
    fun close() = _state.update { s -> s.draft?.photos?.forEach(photos::delete); s.copy(draft = null) }
    fun edit(change: (PosmDraft) -> PosmDraft) = _state.update { s -> s.copy(draft = s.draft?.let(change)) }

    fun newShot(): PhotoTarget = photos.newTarget().also { pending = it }

    fun onShot(saved: Boolean) {
        val shot = pending ?: return
        pending = null
        if (!saved) {
            photos.delete(shot.path)
            return
        }
        edit { it.copy(photos = it.photos + shot.path) }
    }

    fun save() {
        val d = _state.value.draft ?: return
        val counted = d.counted.toIntOrNull()
        val problem = when {
            counted == null || counted < 0 -> "Nhập số lượng kiểm được"
            d.photos.isEmpty() -> "Chụp ít nhất một ảnh"
            else -> null
        }
        if (problem != null) {
            _state.update { it.copy(message = problem) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val r = repository.checkPosm(PosmCheck(visitId, d.item, counted!!, d.condition, d.remark, d.photos))) {
                is DataResult.Success -> {
                    _state.update { it.copy(saving = false, draft = null, message = "Đã lưu kiểm POSM") }
                    load()
                }
                is DataResult.Failure -> _state.update { it.copy(saving = false, message = r.error.toMessage()) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

/** Kiểm POSM: each asset the outlet holds, counted, judged and photographed. */
@Composable
fun PosmCheckScreen(onBack: () -> Unit, viewModel: PosmCheckViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { viewModel.onShot(it) }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { cameraPermission.launch(Manifest.permission.CAMERA) }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() } }

    val draft = state.draft
    SubScreen(
        title = draft?.let { "Kiểm ${it.item.itemName}" } ?: "Kiểm POSM",
        onBack = if (draft != null) viewModel::close else onBack,
        snackbar = snackbar,
    ) { padding ->
        val items = state.items
        when {
            items == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            items == null -> LoadingBox(Modifier.padding(padding))
            draft != null -> Column(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${draft.item.programCode} · đang đặt ${draft.item.placedQty} ${draft.item.unitName}", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    draft.counted, { v -> viewModel.edit { it.copy(counted = v.filter(Char::isDigit).take(4)) } },
                    label = { Text("Số lượng kiểm được") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PosmCondition.entries.forEach { c ->
                        FilterChip(selected = draft.condition == c, onClick = { viewModel.edit { it.copy(condition = c) } }, label = { Text(c.label) })
                    }
                }
                OutlinedTextField(draft.remark, { v -> viewModel.edit { it.copy(remark = v) } }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(draft.photos) { path ->
                        AsyncImage(
                            model = File(path), contentDescription = "Ảnh POSM", contentScale = ContentScale.Crop,
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
                OutlinedButton(onClick = { takePicture.launch(viewModel.newShot().uri) }, enabled = !state.saving) {
                    Text("Chụp ảnh (${draft.photos.size})")
                }
                PrimaryButton("Lưu", viewModel::save, Modifier.fillMaxWidth(), enabled = !state.saving, loading = state.saving)
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (items.isEmpty()) item { Text("Điểm bán không có POSM", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(items, key = { it.programId + it.itemId }) { i ->
                    Card(onClick = { viewModel.open(i) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(i.itemName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("${i.programCode} · đặt ${i.placedQty} ${i.unitName}", style = MaterialTheme.typography.labelSmall)
                            }
                            if (i.countedQty != null) {
                                StatusPill(
                                    "${i.countedQty} · ${PosmCondition.entries.firstOrNull { it.code == i.condition }?.label ?: i.condition}",
                                    if (i.condition == "usable") Kpi.Green.first() else Kpi.Orange.first(),
                                )
                            } else {
                                TextButton(onClick = { viewModel.open(i) }) { Text("Kiểm") }
                            }
                        }
                    }
                }
            }
        }
    }
}
