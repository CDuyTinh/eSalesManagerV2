package com.tinhcd.myesalesmanager.data.session

import com.tinhcd.myesalesmanager.domain.model.Manager
import com.tinhcd.myesalesmanager.domain.model.TeamMember
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the signed-in manager and their team so writes and filters can use them
 * without every caller threading them through. RLS re-checks team membership
 * server side, so this is a convenience, not the security boundary.
 */
@Singleton
class SessionStore @Inject constructor() {
    val current = MutableStateFlow<Manager?>(null)
    val team = MutableStateFlow<List<TeamMember>>(emptyList())
}
