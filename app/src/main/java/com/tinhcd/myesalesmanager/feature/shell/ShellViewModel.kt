package com.tinhcd.myesalesmanager.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.todayVn
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.Manager
import com.tinhcd.myesalesmanager.domain.model.SessionState
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import com.tinhcd.myesalesmanager.domain.repository.PersonalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShellUiState(
    val loading: Boolean = true,
    val signedOut: Boolean = false,
    /** Null with [loading] false: signed in, but the profile fetch failed. */
    val manager: Manager? = null,
    val team: List<TeamMember> = emptyList(),
)

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val personal: PersonalRepository,
) : ViewModel() {

    /** The bell's badge; refreshed whenever the shell comes back into view. */
    val unread: StateFlow<Int> = personal.unread

    fun refreshUnread() {
        viewModelScope.launch { personal.notifications() }
    }

    private val _dayOpen = MutableStateFlow<Boolean?>(null)

    /**
     * Whether the manager's own working day is open — clocked in, not yet out —
     * which is when their position is recorded. Null until known, and left as
     * it was when the check fails, so a dropped request neither starts nor
     * stops tracking.
     */
    val dayOpen: StateFlow<Boolean?> = _dayOpen.asStateFlow()

    fun refreshDay() {
        viewModelScope.launch {
            val r = personal.workDay(todayVn().toString())
            if (r is DataResult.Success) _dayOpen.value = r.data.checkInAt != null && r.data.checkOutAt == null
        }
    }

    val state: StateFlow<ShellUiState> =
        combine(auth.sessionState, auth.team) { session, team ->
            when (session) {
                SessionState.SignedOut -> ShellUiState(loading = false, signedOut = true)
                is SessionState.SignedIn ->
                    ShellUiState(loading = false, manager = session.manager, team = team)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShellUiState())

    fun retryProfile() {
        viewModelScope.launch { auth.refreshProfile() }
    }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}
