package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.data.local.PreferencesRepository
import com.example.data.local.ZaytharPreferences
import com.example.domain.GUEST_USER_ID
import com.example.domain.GuestWorkspaceStore
import com.example.ui.NexusWorkspaceViewModel
import com.example.ui.ZaytharApp
import com.example.ui.ZaytharViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.theme.ZaytharTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.flowOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val prefsRepo = PreferencesRepository(applicationContext)

        // STEP 2: Initialize UI with strict auth gating before ViewModel/Firestore instantiation
        setContent {
            val prefs by prefsRepo.preferences.collectAsStateWithLifecycle(initialValue = ZaytharPreferences())
            val systemDark = isSystemInDarkTheme()
            val resolvedDark = prefs.themeMode.resolveIsDark(systemDark)
            LaunchedEffect(resolvedDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (resolvedDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (resolvedDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
            }

            val firebaseReady = remember {
                runCatching {
                    FirebaseApp.getApps(applicationContext).isNotEmpty() ||
                        FirebaseApp.initializeApp(applicationContext) != null
                }.getOrDefault(false)
            }
            val auth = remember(firebaseReady) {
                if (firebaseReady) runCatching { Firebase.auth }.getOrNull() else null
            }
            val currentUser by remember(auth) {
                auth?.authStateFlow() ?: flowOf(null)
            }.collectAsStateWithLifecycle(initialValue = runCatching { auth?.currentUser }.getOrNull())
            var isGuestMode by rememberSaveable { mutableStateOf(false) }
            val guestStore = remember(isGuestMode) { if (isGuestMode) GuestWorkspaceStore() else null }
            val scope = rememberCoroutineScope()
            val credentialManager = remember { runCatching { CredentialManager.create(this) }.getOrNull() }

            ZaytharTheme(
                themeMode = prefs.themeMode,
                darkTheme = resolvedDark,
                accentHex = prefs.accentHex,
                glowStrength = prefs.glowStrength / 100f,
                metalSpecularity = prefs.metalSpecularity / 100f,
                contrastStrength = prefs.contrastStrength / 100f
            ) {
                val user = currentUser
                if (user == null && !isGuestMode) {
                    AuthScreen(
                        motionEnabled = prefs.motion3dEnabled,
                        onAuthSuccess = { isGuestMode = false },
                        onContinueAsGuest = { isGuestMode = true }
                    )
                } else {
                    val guestActive = user == null && isGuestMode
                    val uid = user?.uid ?: GUEST_USER_ID
                    val activeGuestStore = if (guestActive) guestStore else null
                    key(uid) {
                        val viewModel: ZaytharViewModel = viewModel(
                            key = uid,
                            factory = ZaytharViewModel.provideFactory(
                                application = application,
                                prefsRepo = prefsRepo,
                                currentUserId = uid,
                                guestStore = activeGuestStore
                            )
                        )
                        val workspaceViewModel: NexusWorkspaceViewModel = viewModel(
                            key = "workspace_$uid",
                            factory = NexusWorkspaceViewModel.provideFactory(
                                application = application,
                                currentUserId = uid,
                                guestStore = activeGuestStore
                            )
                        )
                        ZaytharApp(
                            viewModel = viewModel,
                            workspaceViewModel = workspaceViewModel,
                            userEmail = user?.email,
                            userDisplayName = if (guestActive) "Guest User" else user?.displayName,
                            isGuest = guestActive,
                            onSignOut = {
                                if (guestActive) {
                                    isGuestMode = false
                                } else {
                                    signOut(
                                        credentialManager = credentialManager,
                                        scope = scope
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
