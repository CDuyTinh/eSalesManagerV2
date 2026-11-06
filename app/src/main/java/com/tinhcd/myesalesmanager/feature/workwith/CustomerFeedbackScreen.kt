package com.tinhcd.myesalesmanager.feature.workwith

import androidx.compose.foundation.layout.height
import androidx.compose.ui.res.stringResource
import com.tinhcd.myesalesmanager.R
import com.tinhcd.myesalesmanager.core.permission.rememberPermissionRequest
import androidx.compose.foundation.layout.Row
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.PrimaryButton
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.FeedbackForm
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

data class CustomerFeedbackState(
    val form: FeedbackForm? = null,
    val topicId: String? = null,
    val note: String = "",
    val photos: List<String> = emptyList(),
    val error: String? = null,
    val saving: Boolean = false,
    val message: String? = null,
    val done: Boolean = false,
)

@HiltViewModel
class CustomerFeedbackViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: InStoreRepository,
    private val photoStore: PhotoStore,
) : ViewModel() {

    private val visitId: String = checkNotNull(savedState[ARG_VISIT_ID])

    private val _state = MutableStateFlow(CustomerFeedbackState())
    val state: StateFlow<CustomerFeedbackState> = _state.asStateFlow()

    private var pending: PhotoTarget? = null

    init { load() }

    fun load() {
        viewModelScope.launch {
            when (val r = repository.feedbackForm(visitId)) {
                // What was filed earlier is shown to be replaced, not added to:
                // a call has one feedback, as on the rep's app.
                is DataResult.Success -> _state.update {
                    it.copy(form = r.data, error = null, topicId = r.data.filed?.topicId, note = r.data.filed?.note.orEmpty())
                }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }

    fun topic(id: String?) = _state.update { it.copy(topicId = if (it.topicId == id) null else id) }
    fun note(v: String) = _state.update { it.copy(note = v) }

    fun newShot(): PhotoTarget = photoStore.newTarget().also { pending = it }

    fun onShot(saved: Boolean) {
        val shot = pending ?: return
        pending = null
        if (saved) _state.update { it.copy(photos = it.photos + shot.path) } else photoStore.delete(shot.path)
    }

    fun save() {
        val s = _state.value
        val form = s.form ?: return
        // The rules submit_feedback applies, said before the round trip.
        val problem = when {
            s.note.trim().length < form.noteMinLength -> "Ghi nội dung phản hồi (ít nhất ${form.noteMinLength} ký tự)"
            s.photos.size < form.photoMin -> "Chụp ít nhất ${form.photoMin} ảnh"
            s.photos.size > form.photoMax -> "Tối đa ${form.photoMax} ảnh"
            else -> null
        }
        if (problem != null) {
            _state.update { it.copy(message = problem) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val r = repository.submitFeedback(visitId, s.topicId, s.note, s.photos)) {
                is DataResult.Success -> _state.update { it.copy(saving = false, done = true) }
                is DataResult.Failure -> _state.update { it.copy(saving = false, message = r.error.toMessage()) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    override fun onCleared() {
        // Left behind only when the screen is abandoned; a save deletes its own.
        if (!_state.value.done) _state.value.photos.forEach(photoStore::delete)
    }
}

/** Phản hồi khách hàng: what the outlet said, filed on the supervisor's call (legacy FEED_BACK_CUSTOMER). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomerFeedbackScreen(onBack: () -> Unit, viewModel: CustomerFeedbackViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { viewModel.onShot(it) }
    // Asked on entry, as before, and again from the shutter: refused, it explains
    // why rather than opening a camera that cannot start.
    val cameraPermission = rememberPermissionRequest(
        listOf(Manifest.permission.CAMERA),
        stringResource(R.string.permission_rationale_camera),
    )
    LaunchedEffect(Unit) { cameraPermission.launch() }
    LaunchedEffect(state.done) { if (state.done) onBack() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() } }

    SubScreen(title = "Phản hồi khách hàng", onBack = onBack, snackbar = snackbar) { padding ->
        val form = state.form
        when {
            form == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::load, modifier = Modifier.padding(padding))
            form == null -> LoadingBox(Modifier.padding(padding))
            else -> Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                form.filed?.let {
                    Text(
                        "Đã ghi phản hồi cho lượt này${if (it.photos > 0) " (${it.photos} ảnh)" else ""} — lưu lại sẽ thay thế.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (form.topics.isNotEmpty()) {
                    Text("Chủ đề", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        form.topics.forEach { (id, name) ->
                            FilterChip(selected = state.topicId == id, onClick = { viewModel.topic(id) }, label = { Text(name) })
                        }
                    }
                }
                OutlinedTextField(
                    state.note, viewModel::note,
                    label = { Text("Nội dung phản hồi") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.photos) { path ->
                        AsyncImage(
                            model = File(path), contentDescription = "Ảnh phản hồi", contentScale = ContentScale.Crop,
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
                // Side by side, half each, as every form's foot is in both apps.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { cameraPermission.launch { takePicture.launch(viewModel.newShot().uri) } },
                        enabled = !state.saving && state.photos.size < form.photoMax,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Text("Chụp ảnh (${state.photos.size}/${form.photoMax})", maxLines = 1)
                    }
                    PrimaryButton("Lưu phản hồi", viewModel::save, Modifier.weight(1f), enabled = !state.saving, loading = state.saving, height = 48.dp)
                }
            }
        }
    }
}
