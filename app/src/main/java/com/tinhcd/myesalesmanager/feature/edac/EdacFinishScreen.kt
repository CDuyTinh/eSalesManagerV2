package com.tinhcd.myesalesmanager.feature.edac

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.tinhcd.myesalesmanager.core.photo.PhotoStore
import com.tinhcd.myesalesmanager.core.photo.PhotoTarget
import com.tinhcd.myesalesmanager.core.ui.PrimaryButton
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.repository.EdacRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class EdacFinishState(
    val result: String = "",
    val supervisorPhoto: String? = null,
    val salespersonPhoto: String? = null,
    val saving: Boolean = false,
    val message: String? = null,
    val done: Boolean = false,
)

@HiltViewModel
class EdacFinishViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: EdacRepository,
    private val photos: PhotoStore,
) : ViewModel() {

    private val trainingId: String = checkNotNull(savedState[ARG_TRAINING_ID])

    private val _state = MutableStateFlow(EdacFinishState())
    val state: StateFlow<EdacFinishState> = _state.asStateFlow()

    private var pending: PhotoTarget? = null
    private var pendingIsSupervisor = true

    fun setResult(v: String) = _state.update { it.copy(result = v) }

    fun newShot(supervisor: Boolean): PhotoTarget =
        photos.newTarget().also { pending = it; pendingIsSupervisor = supervisor }

    /** A new shot replaces the earlier one of the same person. */
    fun onShot(saved: Boolean) {
        val shot = pending ?: return
        pending = null
        if (!saved) {
            photos.delete(shot.path)
            return
        }
        _state.update {
            if (pendingIsSupervisor) {
                it.supervisorPhoto?.let(photos::delete)
                it.copy(supervisorPhoto = shot.path)
            } else {
                it.salespersonPhoto?.let(photos::delete)
                it.copy(salespersonPhoto = shot.path)
            }
        }
    }

    fun finish() {
        val s = _state.value
        val message = when {
            s.result.isBlank() -> "Nhập kết quả huấn luyện"
            s.supervisorPhoto == null || s.salespersonPhoto == null -> "Cần chụp ảnh giám sát và nhân viên"
            else -> null
        }
        if (message != null) {
            _state.update { it.copy(message = message) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val r = repository.finish(trainingId, s.result, s.supervisorPhoto!!, s.salespersonPhoto!!)) {
                is DataResult.Success -> _state.update { it.copy(saving = false, done = true) }
                is DataResult.Failure -> _state.update { it.copy(saving = false, message = r.error.toMessage()) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

/** Hoàn tất: what came of the day, and a photo of each person (legacy SaveFinishStep). */
@Composable
fun EdacFinishScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: EdacFinishViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { viewModel.onShot(it) }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { cameraPermission.launch(Manifest.permission.CAMERA) }
    LaunchedEffect(state.done) { if (state.done) onDone() }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() } }

    SubScreen(
        title = "8. Hoàn tất",
        subtitle = "Kết quả huấn luyện",
        onBack = onBack,
        snackbar = snackbar,
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                PrimaryButton(
                    "Kết thúc huấn luyện", viewModel::finish,
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp, 10.dp),
                    enabled = !state.saving, loading = state.saving,
                )
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.result,
                onValueChange = viewModel::setResult,
                label = { Text("Kết quả huấn luyện *") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PhotoSlot("Ảnh giám sát *", state.supervisorPhoto, Modifier.weight(1f)) {
                    takePicture.launch(viewModel.newShot(supervisor = true).uri)
                }
                PhotoSlot("Ảnh nhân viên *", state.salespersonPhoto, Modifier.weight(1f)) {
                    takePicture.launch(viewModel.newShot(supervisor = false).uri)
                }
            }
        }
    }
}

@Composable
private fun PhotoSlot(label: String, path: String?, modifier: Modifier, onShoot: () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onShoot),
        ) {
            if (path != null) {
                AsyncImage(model = File(path), contentDescription = label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Text("Chụp ảnh", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
