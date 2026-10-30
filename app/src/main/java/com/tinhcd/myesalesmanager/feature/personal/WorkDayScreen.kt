package com.tinhcd.myesalesmanager.feature.personal

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.location.LocationProvider
import com.tinhcd.myesalesmanager.core.ui.ErrorBox
import com.tinhcd.myesalesmanager.core.ui.Kpi
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.PrimaryButton
import com.tinhcd.myesalesmanager.core.ui.StatusPill
import com.tinhcd.myesalesmanager.core.ui.SubScreen
import com.tinhcd.myesalesmanager.core.ui.formatDateTime
import com.tinhcd.myesalesmanager.core.ui.formatDay
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.WorkDay
import com.tinhcd.myesalesmanager.domain.repository.PersonalRepository
import com.tinhcd.myesalesmanager.feature.approval.toMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkDayState(
    val day: WorkDay? = null,
    val error: String? = null,
    val punching: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class WorkDayViewModel @Inject constructor(
    private val repository: PersonalRepository,
    private val location: LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(WorkDayState())
    val state: StateFlow<WorkDayState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            when (val r = repository.workDay(todayVn().toString())) {
                is DataResult.Success -> _state.update { it.copy(day = r.data, error = null) }
                is DataResult.Failure -> _state.update { it.copy(error = r.error.toMessage()) }
            }
        }
    }

    fun punch() {
        val day = _state.value.day ?: return
        val checkIn = day.checkInAt == null
        viewModelScope.launch {
            _state.update { it.copy(punching = true) }
            // The distance is recorded when there is a fix; a supervisor is not
            // held to the depot's radius the way a rep is.
            val here = runCatching { location.currentLocation() }.getOrNull()
            when (val r = repository.punch(day, checkIn, here)) {
                is DataResult.Success -> {
                    _state.update { it.copy(punching = false, message = if (checkIn) "Đã chấm công vào" else "Đã chấm công ra") }
                    refresh()
                }
                is DataResult.Failure -> _state.update { it.copy(punching = false, message = r.error.toMessage()) }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }
}

/** Chấm công: the manager's own day at the depot (legacy CheckIns, SFA timekeeping). */
@Composable
fun WorkDayScreen(onBack: () -> Unit, viewModel: WorkDayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    LaunchedEffect(Unit) {
        permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.message) { state.message?.let { snackbar.showSnackbar(it); viewModel.messageShown() } }

    SubScreen(title = "Chấm công", subtitle = formatDay(todayVn()), onBack = onBack, snackbar = snackbar) { padding ->
        val day = state.day
        when {
            day == null && state.error != null -> ErrorBox(state.error!!, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            day == null -> LoadingBox(Modifier.padding(padding))
            else -> Column(
                Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(day.branchName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        day.branchAddress?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        PunchLine("Vào", day.checkInAt)
                        PunchLine("Ra", day.checkOutAt)
                        if (day.openVisits > 0) {
                            Text(
                                "Còn ${day.openVisits} lượt viếng thăm chưa check-out",
                                color = Kpi.Orange.first(),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                when {
                    day.checkOutAt != null -> Text("Đã kết thúc ngày làm việc", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> PrimaryButton(
                        if (day.checkInAt == null) "Chấm công vào" else "Chấm công ra",
                        viewModel::punch,
                        Modifier.fillMaxWidth(),
                        enabled = !state.punching,
                        loading = state.punching,
                    )
                }
            }
        }
    }
}

@Composable
private fun PunchLine(label: String, at: String?) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (at != null) StatusPill(formatDateTime(at), Kpi.Green.first()) else StatusPill("Chưa chấm", Kpi.Slate.first())
    }
}
