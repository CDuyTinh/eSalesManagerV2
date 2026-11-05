package com.tinhcd.myesalesmanager

import android.content.Intent
import android.content.Context
import com.tinhcd.myesalesmanager.core.ui.theme.LanguageSettings
import com.tinhcd.myesalesmanager.push.PushRoute
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.tinhcd.myesalesmanager.core.ui.theme.MyeSalesTheme
import com.tinhcd.myesalesmanager.core.ui.theme.ThemeSettings
import com.tinhcd.myesalesmanager.core.ui.theme.isDark
import com.tinhcd.myesalesmanager.domain.model.SessionState
import com.tinhcd.myesalesmanager.domain.repository.AuthRepository
import com.tinhcd.myesalesmanager.navigation.AppNavHost
import com.tinhcd.myesalesmanager.navigation.Routes
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import com.tinhcd.myesalesmanager.feature.approval.LocalRejectReasons
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The least time the splash stays up, so a session restored at once does not make
 * it flash past as a flicker.
 */
private const val SPLASH_MIN_MS = 1_200L

/**
 * The most it waits for the session (mgr-bootstrap is a network call). The splash
 * used to give up after 0.8s and a spinner filled the rest of the wait; past this
 * budget the app now draws a copy of the splash itself instead.
 */
private const val SPLASH_MAX_MS = 8_000L

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // singleTop: a notification tapped while the app is open arrives here
    // instead of starting a second copy of the activity.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PushRoute.from(intent)
    }

    private val rootViewModel: RootViewModel by viewModels()

    @Inject lateinit var themeSettings: ThemeSettings

    // Ngôn ngữ: every resource this activity reads comes from a context set to
    // the app's language, not the phone's.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageSettings.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        PushRoute.from(intent)

        val launchedAt = SystemClock.elapsedRealtime()
        splash.setKeepOnScreenCondition {
            val elapsed = SystemClock.elapsedRealtime() - launchedAt
            elapsed < SPLASH_MIN_MS || (rootViewModel.startDestination.value == null && elapsed < SPLASH_MAX_MS)
        }

        // Every screen opens on the blue brand header, so the status bar icons
        // are always drawn light; the default follows the system theme and
        // put dark icons on the blue in light mode.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            val mode by themeSettings.mode.collectAsStateWithLifecycle()
            val dark = mode.isDark()
            // The navigation bar follows the app's choice, not the phone's: Tối
            // chosen on a light phone would otherwise put dark buttons on a dark bar.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { dark },
                )
                onDispose {}
            }
            MyeSalesTheme(darkTheme = dark) {
                val start by rootViewModel.startDestination.collectAsStateWithLifecycle()
                val rejectReasons by rootViewModel.rejectReasons.collectAsStateWithLifecycle()
                CompositionLocalProvider(LocalRejectReasons provides rejectReasons) {
                    when (val destination = start) {
                        null -> SplashHold()
                        else -> AppNavHost(startDestination = destination)
                    }
                }
            }
        }
    }
}

@HiltViewModel
class RootViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    /** For every decision dialog in the app, so no screen has to pass them along. */
    val rejectReasons: StateFlow<List<String>> =
        authRepository.rejectReasons.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

/**
 * The splash, drawn by the app: the same background and chart, at the size the
 * system shows it, so passing from one to the other cannot be seen. Only reached
 * when restoring the session outlasts SPLASH_MAX_MS.
 */
@Composable
private fun SplashHold() {
    Box(
        Modifier.fillMaxSize().background(colorResource(R.color.splash_background)),
        contentAlignment = Alignment.Center,
    ) {
        // The system shows ic_splash_logo (the chart at 84%) in a 288dp icon box.
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(242.dp))
    }
}
