package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.data.local.PreferencesRepository
import com.example.data.local.ZaytharPreferences
import com.example.ui.NexusWorkspaceViewModel
import com.example.ui.ZaytharApp
import com.example.ui.ZaytharViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.theme.ZaytharTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

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
            val auth = remember { Firebase.auth }
            val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
            val scope = rememberCoroutineScope()
            val credentialManager = remember { CredentialManager.create(this) }

            ZaytharTheme(
                darkTheme = prefs.isDarkTheme,
                accentHex = prefs.accentHex,
                glowStrength = prefs.glowStrength / 100f,
                metalSpecularity = prefs.metalSpecularity / 100f,
                contrastStrength = prefs.contrastStrength / 100f
            ) {
                val user = currentUser
                if (user == null) {
                    AuthScreen(
                        motionEnabled = prefs.motion3dEnabled,
                        onAuthSuccess = { /* Reactive authStateFlow transitions to authenticated UI */ }
                    )
                } else {
                    val uid = user.uid
                    key(uid) {
                        val viewModel: ZaytharViewModel = viewModel(
                            key = uid,
                            factory = ZaytharViewModel.provideFactory(
                                application = application,
                                prefsRepo = prefsRepo,
                                currentUserId = uid
                            )
                        )
                        val workspaceViewModel: NexusWorkspaceViewModel = viewModel(
                            key = "workspace_$uid",
                            factory = NexusWorkspaceViewModel.provideFactory(
                                application = application,
                                currentUserId = uid
                            )
                        )
                        ZaytharApp(
                            viewModel = viewModel,
                            workspaceViewModel = workspaceViewModel,
                            userEmail = user.email,
                            userDisplayName = user.displayName,
                            onSignOut = {
                                signOut(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
