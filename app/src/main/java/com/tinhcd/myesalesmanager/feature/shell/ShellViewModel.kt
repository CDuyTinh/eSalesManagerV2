package com.tinhcd.myesalesmanager.feature.shell

import com.tinhcd.myesalesmanager.domain.model.AppUpdatePolicy
import com.tinhcd.myesalesmanager.domain.model.AppUpdateNeed
import com.tinhcd.myesalesmanager.BuildConfig
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

    /** The latest build the manager has said "later" to, this launch. */
    private val postponedUpdate = MutableStateFlow<Int?>(null)

    /**
     * A newer build head office wants in use, from the settings that came with
     * the profile; null when this build is current or the newer one was put off.
     */
    val update: StateFlow<UpdateNotice?> =
        combine(auth.settings, postponedUpdate) { settings, postponed ->
            val policy = AppUpdatePolicy.from(settings)
            when (policy.check(BuildConfig.VERSION_CODE)) {
                AppUpdateNeed.REQUIRED -> UpdateNotice(true, policy.latestCode, policy.note, policy.url)
                AppUpdateNeed.OPTIONAL ->
                    if (policy.latestCode == postponed) null
                    else UpdateNotice(false, policy.latestCode, policy.note, policy.url)
                AppUpdateNeed.NONE -> null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** "Để sau" on an optional update: not again this launch, unless a newer one appears. */
    fun postponeUpdate() {
        postponedUpdate.value = update.value?.latestCode
    }

    fun retryProfile() {
        viewModelScope.launch { auth.refreshProfile() }
    }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}

/** What the update dialog says: whether it can be put off, and where to get the build. */
data class UpdateNotice(
    val required: Boolean,
    val latestCode: Int?,
    val note: String?,
    val url: String?,
)
