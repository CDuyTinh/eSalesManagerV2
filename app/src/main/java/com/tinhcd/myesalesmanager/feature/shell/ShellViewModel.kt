package com.tinhcd.myesalesmanager.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.domain.model.Manager
import com.tinhcd.myesalesmanager.domain.model.SessionState
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
) : ViewModel() {

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
