package com.tinhcd.myesalesmanager.data.repository

import android.util.Log
import com.tinhcd.myesalesmanager.data.di.ApplicationScope
import com.tinhcd.myesalesmanager.data.remote.dto.StaffDto
import com.tinhcd.myesalesmanager.data.remote.http.orThrow
import com.tinhcd.myesalesmanager.data.remote.service.BootstrapService
import com.tinhcd.myesalesmanager.data.session.SessionStore
import com.tinhcd.myesalesmanager.data.util.SingleFlight
import com.tinhcd.myesalesmanager.domain.AppError
import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.Manager
import com.tinhcd.myesalesmanager.domain.model.Position
import com.tinhcd.myesalesmanager.domain.model.SessionState
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Same synthetic-address mapping as MyeSalesSFA: the shared auth project knows
 * "gsbh01@esales.local", the manager types "gsbh01".
 */
private const val EMAIL_DOMAIN = "@esales.local"

/** No salesperson row points at this auth user. Permanent; head office must fix it. */
const val ERROR_NOT_PROVISIONED = "account_not_provisioned"

/** A rep's account. They have the SFA app; this one is for supervisors. */
const val ERROR_NOT_A_MANAGER = "not_a_manager"

/** Only reached if the session vanished between opening a screen and saving. */
const val ERROR_NO_SESSION = "no_session"

/** Signed in, but the profile could not be fetched. Transient; retrying may work. */
const val ERROR_PROFILE_UNAVAILABLE = "profile_unavailable"

/**
 * Ported from MyeSalesSFA's AuthRepositoryImpl, keeping its two hard-won rules:
 * the profile fetch is single-flight (sign-in and the session collector race for
 * it), and a failed fetch is "signed in without a profile", never "signed out".
 *
 * What is new is the role gate. Both apps sign into one auth project, so a rep's
 * password works here too; the profile's position is what keeps them out.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    private val service: BootstrapService,
    private val session: SessionStore,
    @param:ApplicationScope private val scope: CoroutineScope,
) : AuthRepository {

    private val resolved = MutableStateFlow<SessionState?>(null)

    private val profileFlight = SingleFlight<DataResult<Manager>>(scope)

    init {
        scope.launch {
            client.auth.sessionStatus
                .filter { it !is SessionStatus.Initializing }
                .collect { status ->
                    if (status is SessionStatus.Authenticated) {
                        refreshProfile()
                    } else {
                        publish(SessionState.SignedOut, emptyList())
                    }
                }
        }
    }

    override val sessionState: Flow<SessionState> = resolved.filterNotNull()

    override val team: Flow<List<TeamMember>> = session.team

    override suspend fun signIn(username: String, password: String): DataResult<Manager> {
        try {
            client.auth.signInWith(Email) {
                this.email = username.lowercase() + EMAIL_DOMAIN
                this.password = password
            }
        } catch (e: Exception) {
            return DataResult.Failure(e.toAppError())
        }
        return refreshProfile()
    }

    override suspend fun refreshProfile(): DataResult<Manager> =
        profileFlight.run { fetchProfile() }

    private suspend fun fetchProfile(): DataResult<Manager> {
        val body = try {
            service.bootstrap().orThrow()
        } catch (e: Exception) {
            Log.w("AuthRepository", "mgr-bootstrap failed: ${e.message}", e)
            publish(SessionState.SignedIn(null), emptyList())
            return DataResult.Failure(AppError.Auth(ERROR_PROFILE_UNAVAILABLE))
        }

        val manager = body.manager?.toManager()
        val refusal = when {
            manager == null -> ERROR_NOT_PROVISIONED
            !manager.position.isManager -> ERROR_NOT_A_MANAGER
            else -> null
        }
        if (refusal != null) {
            // Permanent answers, so the session is dropped rather than left
            // dangling in an app the account cannot use.
            client.auth.signOut()
            publish(SessionState.SignedOut, emptyList())
            return DataResult.Failure(AppError.Auth(refusal))
        }

        publish(SessionState.SignedIn(manager), body.team.map { it.toTeamMember() })
        return DataResult.Success(manager!!)
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): DataResult<Unit> = try {
        val email = client.auth.currentUserOrNull()?.email
        if (email.isNullOrBlank()) {
            DataResult.Failure(AppError.Auth(ERROR_NO_SESSION))
        } else {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = currentPassword
            }
            client.auth.updateUser { password = newPassword }
            DataResult.Success(Unit)
        }
    } catch (e: Exception) {
        DataResult.Failure(e.toAppError())
    }

    override suspend fun signOut(): DataResult<Unit> = try {
        client.auth.signOut()
        publish(SessionState.SignedOut, emptyList())
        DataResult.Success(Unit)
    } catch (e: Exception) {
        DataResult.Failure(e.toAppError())
    }

    private fun publish(next: SessionState, team: List<TeamMember>) {
        resolved.value = next
        session.current.value = (next as? SessionState.SignedIn)?.manager
        session.team.value = team
    }
}

private fun StaffDto.toManager() = Manager(
    id = id,
    code = code,
    fullName = fullName,
    position = Position.of(position),
    branchId = branchId.orEmpty(),
    branchCode = branch?.code.orEmpty(),
    branchName = branch?.name.orEmpty(),
)

private fun StaffDto.toTeamMember() = TeamMember(
    id = id,
    code = code,
    fullName = fullName,
    position = Position.of(position),
    phone = phone,
    branchId = branchId,
)

internal fun Exception.toAppError(): AppError {
    val text = message.orEmpty()
    return when {
        text.contains("Invalid login", ignoreCase = true) ||
            text.contains("invalid_credentials", ignoreCase = true) ->
            AppError.Auth("invalid_credentials")

        this is java.io.IOException -> AppError.Network(text)
        else -> AppError.Unknown(text)
    }
}
