package com.tinhcd.myesalesmanager

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.LoadingBox
import com.tinhcd.myesalesmanager.core.ui.theme.MyeSalesTheme
import com.tinhcd.myesalesmanager.domain.model.SessionState
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import com.tinhcd.myesalesmanager.navigation.AppNavHost
import com.tinhcd.myesalesmanager.navigation.Routes
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Long enough to hide the hand-off into the first screen, and no longer. */
private const val SPLASH_HOLD_MS = 800L

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val rootViewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        val launchedAt = SystemClock.elapsedRealtime()
        splash.setKeepOnScreenCondition {
            rootViewModel.startDestination.value == null &&
                SystemClock.elapsedRealtime() - launchedAt < SPLASH_HOLD_MS
        }

        // Every screen opens on the blue brand header, so the status bar icons
        // are always drawn light; the default follows the system theme and
        // put dark icons on the blue in light mode.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            MyeSalesTheme {
                val start by rootViewModel.startDestination.collectAsStateWithLifecycle()
                when (val destination = start) {
                    null -> LoadingBox()
                    else -> AppNavHost(startDestination = destination)
                }
            }
        }
    }
}

@HiltViewModel
class RootViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            // Keyed on whether a session exists, not on whether the profile arrived:
            // the shell says so and offers a retry when it did not.
            _startDestination.value = when (authRepository.sessionState.first()) {
                is SessionState.SignedOut -> Routes.LOGIN
                is SessionState.SignedIn -> Routes.SHELL
            }
        }
    }
}
