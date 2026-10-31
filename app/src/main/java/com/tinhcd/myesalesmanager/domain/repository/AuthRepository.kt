package com.tinhcd.myesalesmanager.domain.repository

import com.tinhcd.myesalesmanager.domain.DataResult
import com.tinhcd.myesalesmanager.domain.model.Manager
import com.tinhcd.myesalesmanager.domain.model.SessionState
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Emits nothing until the stored session has been resolved one way or the other. */
    val sessionState: Flow<SessionState>

    /** The reps under the signed-in manager, as of the last profile fetch. */
    val team: Flow<List<TeamMember>>

    /** The SSs under an AM / RM, for filtering by supervisor; empty for an SS. */
    val supervisors: Flow<List<TeamMember>>

    /** @param username the code the manager types, e.g. "gsbh01". */
    suspend fun signIn(username: String, password: String): DataResult<Manager>

    /** Re-fetches the profile for a session that already exists. */
    suspend fun refreshProfile(): DataResult<Manager>

    /** Verifies [currentPassword] by re-authenticating before changing it. */
    suspend fun changePassword(currentPassword: String, newPassword: String): DataResult<Unit>

    suspend fun signOut(): DataResult<Unit>
}
