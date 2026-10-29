package com.tinhcd.myesalesmanager.domain.model

/**
 * Whether there is a session, kept separate from whether the manager's profile
 * could be loaded — the same split MyeSalesSFA learned the hard way: a single
 * dropped `/mgr-bootstrap` must not send someone with a good session back to the
 * login screen.
 */
sealed interface SessionState {

    /** No valid session. The login screen is the only correct destination. */
    data object SignedOut : SessionState

    /** A valid session. [manager] is null while the profile has not arrived yet. */
    data class SignedIn(val manager: Manager?) : SessionState {
        val profileMissing: Boolean get() = manager == null
    }
}
