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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.tinhcd.myesalesmanager.core.location.LocationProvider
import com.tinhcd.myesalesmanager.core.photo.PhotoStore
import com.tinhcd.myesalesmanager.core.photo.PhotoTarget
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.Kpi
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.StatusPill
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.formatDateTime
import com.tinhcd.myesalesmanager.core.ui.formatDong
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.DisplayScore
import com.tinhcd.myesalesmanager.domain.model.DisplayToScore
import com.tinhcd.myesalesmanager.domain.model.WorkWithVisit
import com.tinhcd.myesalesmanager.domain.model.VisitTasks
import com.tinhcd.myesalesmanager.domain.repository.WorkWithRepository
import com.tinhcd.myesalesmanager.feature.approval.LabeledLine
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

const val ARG_VISIT_ID = "visitId"

/** The score being filled in for one programme. */
data class ScoreDraft(
    val program: DisplayToScore,
    val faces: String = "",
    val achieved: Boolean = false,
    val note: String = "",
    val photos: List<String> = emptyList(),
)

data class WorkWithVisitState(
    val loading: Boolean = true,
    val error: String? = null,
    val visit: WorkWithVisit? = null,
    val draft: ScoreDraft? = null,
    val saving: Boolean = false,
    val checkingOut: Boolean = false,
    val askCheckOut: Boolean = false,
    val message: String? = null,
    val closed: Boolean = false,
)

@HiltViewModel
class WorkWithVisitViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: WorkWithRepository,
    private val photos: PhotoStore,
    private val location: LocationProvider,
) : ViewModel() {

    private val visitId: String = checkNotNull(savedState[ARG_VISIT_ID])

    private val _state = MutableStateFlow(WorkWithVisitState())
    val state: StateFlow<WorkWithVisitState> = _state.asStateFlow()

    /** The file the camera is writing into, until it reports back. */
    private var pendingShot: PhotoTarget? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = repository.visit(visitId)) {
                is DataResult.Success -> _state.update { it.copy(loading = false, visit = r.data) }
                is DataResult.Failure -> _state.update { it.copy(loading = false, error = r.error.toMessage()) }
            }
        }
    }

    fun openScore(p: DisplayToScore) = _state.update {
        it.copy(draft = ScoreDraft(p, faces = p.myFaces?.toString().orEmpty(), achieved = p.myAchieved ?: false))
    }

    fun closeScore() {
        _state.value.draft?.photos?.forEach(photos::delete)
        _state.update { it.copy(draft = null) }
    }

    /**
     * Typing a count suggests the verdict from the level's requirement, but the
     * switch stays the supervisor's — the legacy kept Evaluate separate from
     * FaceRemark for the same reason: a full shelf can still be the wrong shelf.
     */
    fun setFaces(v: String) = _state.update { s ->
        val d = s.draft ?: return@update s
        val digits = v.filter(Char::isDigit).take(4)
        val count = digits.toIntOrNull()
        s.copy(draft = d.copy(faces = digits, achieved = count?.let { it >= d.program.requiredFaces } ?: d.achieved))
    }

    fun setAchieved(v: Boolean) = _state.update { s -> s.copy(draft = s.draft?.copy(achieved = v)) }
    fun setNote(v: String) = _state.update { s -> s.copy(draft = s.draft?.copy(note = v)) }

    fun newShot(): PhotoTarget = photos.newTarget().also { pendingShot = it }

    fun onShot(saved: Boolean) {
        val shot = pendingShot ?: return
        pendingShot = null
        if (!saved) {
            photos.delete(shot.path)
            return
        }
        _state.update { s -> s.copy(draft = s.draft?.let { it.copy(photos = it.photos + shot.path) }) }
    }

    fun removePhoto(path: String) {
        photos.delete(path)
        _state.update { s -> s.copy(draft = s.draft?.let { it.copy(photos = it.photos - path) }) }
    }

    fun saveScore() {
        val d = _state.value.draft ?: return
        val faces = d.faces.toIntOrNull()
        if (faces == null) {
            _state.update { it.copy(message = "Nhập số mặt hàng đếm được") }
            return
        }
        if (d.photos.isEmpty()) {
            _state.update { it.copy(message = "Chụp ít nhất 1 ảnh trưng bày") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val score = DisplayScore(visitId, d.program.programId, d.program.levelId, faces, d.achieved, d.note, d.photos)
            when (val r = repository.scoreDisplay(score)) {
                is DataResult.Success -> {
                    _state.update { it.copy(saving = false, draft = null, message = "Đã lưu điểm ${d.program.programCode}") }
                    load()
                }
                is DataResult.Failure -> _state.update { it.copy(saving = false, message = r.error.toMessage()) }
            }
        }
    }

    fun askCheckOut(v: Boolean) = _state.update { it.copy(askCheckOut = v) }

    fun checkOut(note: String) {
        viewModelScope.launch {
            _state.update { it.copy(checkingOut = true) }
            val here = runCatching { location.currentLocation() }.getOrNull()
            when (val r = repository.checkOut(visitId, note, here)) {
                is DataResult.Success -> _state.update { it.copy(checkingOut = false, askCheckOut = false, closed = true) }
                is DataResult.Failure -> _state.update { it.copy(checkingOut = false, message = r.error.toMessage()) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

@Composable
fun WorkWithVisitScreen(
    onBack: () -> Unit,
    onOpenStep: (InStoreStep, String) -> Unit = { _, _ -> },
    viewModel: WorkWithVisitViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    // Back from a step, the summary of what the call has done must be current.
    var resumedOnce by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        if (resumedOnce) viewModel.load() else resumedOnce = true
        onPauseOrDispose { }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { viewModel.onShot(it) }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { cameraPermission.launch(Manifest.permission.CAMERA) }

    LaunchedEffect(state.closed) { if (state.closed) onBack() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() }
    }

    val visit = state.visit
    SubScreen(
        title = visit?.customerName ?: "Viếng thăm",
        subtitle = visit?.let { "Cùng ${it.rep?.name.orEmpty()}" },
        onBack = onBack,
        snackbar = snackbar,
        bottomBar = {
            if (visit?.isOpen == true) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = { viewModel.askCheckOut(true) },
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp, 8.dp).height(48.dp),
                    ) { Text("Check-out") }
                }
            }
        },
    ) { padding ->
        when {
            state.loading && visit == null -> LoadingBox(Modifier.padding(padding))
            visit == null -> ErrorBox(state.error ?: "Không tải được lượt viếng thăm", onRetry = viewModel::load, modifier = Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { VisitInfo(visit) }
                visit.tasks?.let { tasks ->
                    item { InStoreSteps(tasks, open = visit.isOpen) { step -> onOpenStep(step, visit.visitId) } }
                }
                item {
                    Text("Chấm điểm trưng bày (${visit.programs.size})", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                if (visit.programs.isEmpty()) {
                    item { Text("Cửa hàng không có chương trình trưng bày nào đang chạy", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(visit.programs, key = { it.programId }) { p ->
                    ProgramCard(p, canScore = visit.isOpen) { viewModel.openScore(p) }
                }
            }
        }
    }

    state.draft?.let { draft ->
        ScoreDialog(
            draft = draft,
            saving = state.saving,
            onFaces = viewModel::setFaces,
            onAchieved = viewModel::setAchieved,
            onNote = viewModel::setNote,
            onShoot = { takePicture.launch(viewModel.newShot().uri) },
            onRemove = viewModel::removePhoto,
            onSave = viewModel::saveScore,
            onDismiss = viewModel::closeScore,
        )
    }

    if (state.askCheckOut) {
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!state.checkingOut) viewModel.askCheckOut(false) },
            title = { Text("Check-out") },
            text = {
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Nhận xét về nhân viên tại điểm này") }, minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.checkOut(note) }, enabled = !state.checkingOut) {
                    Text(if (state.checkingOut) "Đang lưu…" else "Check-out")
                }
            },
            dismissButton = { TextButton(onClick = { viewModel.askCheckOut(false) }, enabled = !state.checkingOut) { Text("Huỷ") } },
        )
    }
}

@Composable
private fun VisitInfo(v: WorkWithVisit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${v.customerCode} – ${v.customerName}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                StatusPill(if (v.isOpen) "Đang ghé" else "Đã check-out", if (v.isOpen) Kpi.Orange.first() else Kpi.Green.first())
            }
            LabeledLine("Địa chỉ", v.customerAddress)
            LabeledLine("Điện thoại", v.customerPhone)
            LabeledLine("Check-in", v.checkInAt?.let { formatDateTime(it) + (v.distanceM?.let { d -> " · cách $d m" } ?: "") })
            LabeledLine("Check-out", v.checkOutAt?.let(::formatDateTime))
            LabeledLine(
                "Nhân viên hôm nay",
                listOfNotNull(
                    v.repStatus?.label ?: "Chưa ghé",
                    v.repCheckIn?.let { "vào ${formatDateTime(it).takeLast(5)}" },
                    if (v.repOrderAmount > 0) "đơn ${formatDong(v.repOrderAmount)}" else null,
                ).joinToString(" · "),
            )
            LabeledLine("Nhận xét", v.note)
        }
    }
}

@Composable
private fun ProgramCard(p: DisplayToScore, canScore: Boolean, onScore: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${p.programCode} – ${p.programName}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text("${p.levelName} · yêu cầu ${p.requiredFaces} mặt", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScorePill("NV", p.repFaces, p.repAchieved)
                ScorePill("Bạn", p.myFaces, p.myAchieved)
            }
            if (canScore) {
                OutlinedButton(onClick = onScore, modifier = Modifier.fillMaxWidth()) {
                    Text(if (p.myAuditId == null) "Chấm điểm" else "Chấm lại")
                }
            }
        }
    }
}

@Composable
private fun ScorePill(who: String, faces: Int?, achieved: Boolean?) {
    val text = if (faces == null) "$who: chưa chấm" else "$who: $faces mặt · ${if (achieved == true) "Đạt" else "Không đạt"}"
    val color = when (achieved) {
        true -> Kpi.Green.first()
        false -> Kpi.Orange.first()
        null -> Kpi.Slate.first()
    }
    StatusPill(text, color)
}

@Composable
private fun ScoreDialog(
    draft: ScoreDraft,
    saving: Boolean,
    onFaces: (String) -> Unit,
    onAchieved: (Boolean) -> Unit,
    onNote: (String) -> Unit,
    onShoot: () -> Unit,
    onRemove: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Chấm ${draft.program.programCode}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${draft.program.levelName} · yêu cầu ${draft.program.requiredFaces} mặt", style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = draft.faces, onValueChange = onFaces,
                    label = { Text("Số mặt đếm được *") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Đạt chuẩn", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = draft.achieved, onCheckedChange = onAchieved)
                }
                OutlinedTextField(
                    value = draft.note, onValueChange = onNote,
                    label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth(),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(draft.photos) { path ->
                        AsyncImage(
                            model = File(path),
                            contentDescription = "Ảnh trưng bày",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onShoot, enabled = !saving) { Text("Chụp ảnh (${draft.photos.size})") }
                    if (draft.photos.isNotEmpty()) {
                        TextButton(onClick = { onRemove(draft.photos.last()) }, enabled = !saving) { Text("Xoá ảnh cuối") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !saving) { Text(if (saving) "Đang lưu…" else "Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Huỷ") } },
    )
}

/** The legacy in-call steps a supervisor can do alongside the rep. */
enum class InStoreStep(val title: String) {
    STOCK("Kiểm tồn"),
    ORDER("Đặt hàng hộ"),
    POSM("Kiểm POSM"),
    MARKET("Thông tin thị trường"),
}

@Composable
private fun InStoreSteps(t: VisitTasks, open: Boolean, onOpen: (InStoreStep) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Công việc tại điểm bán", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            InStoreStep.entries.forEach { step ->
                val done = when (step) {
                    InStoreStep.STOCK -> t.stockCountedAt?.let { "${t.stockCountLines} dòng · ${formatDateTime(it).takeLast(5)}" }
                    InStoreStep.ORDER -> t.orders.takeIf { it.isNotEmpty() }?.let { o -> "${o.size} đơn · ${formatDong(o.sumOf { it.totalAmount })}" }
                    InStoreStep.POSM -> t.posmChecked.takeIf { it > 0 }?.let { "$it vật phẩm" }
                    InStoreStep.MARKET -> t.surveysDone.takeIf { it > 0 }?.let { "$it khảo sát" }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(step.title, style = MaterialTheme.typography.bodyLarge)
                        Text(done ?: "Chưa làm", style = MaterialTheme.typography.labelSmall, color = if (done != null) Kpi.Green.first() else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // A closed call can still be looked at; nothing more is recorded on it.
                    TextButton(onClick = { onOpen(step) }, enabled = open) { Text(if (done != null) "Làm lại" else "Làm") }
                }
            }
            if (open && t.minMinutes > 0) {
                Text("Cần ở điểm bán ít nhất ${t.minMinutes} phút trước khi check-out", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
