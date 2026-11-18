package com.example

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.PreferencesRepository
import com.example.data.remote.FirestoreRepository
import com.example.data.remote.FirestoreSessionAndBrainStore
import com.example.ui.TtsSpeaker
import com.example.ui.ZaytharViewModel
import com.example.ui.auth.attemptAutoSignIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper

class StartupResilienceTest : FirestoreEmulatorTestBase() {

    private val noOpSpeaker = object : TtsSpeaker {
        override fun speak(text: String, languageCode: String) {}
        override fun stop() {}
        override fun shutdown() {}
    }

    @Test
    fun unauthenticatedFirestoreCalls_returnResultFailureInsteadOfThrowing() = runBlocking {
        auth.signOut()
        val repo = FirestoreRepository(firestore)
        val store = FirestoreSessionAndBrainStore(firestore)

        assertTrue(repo.ensureUserProfile("OPERATOR 01").isFailure)
        assertTrue(repo.addMemoryNote("Test note").isFailure)
        assertTrue(repo.addKnowledgeFact("A", "b", "c").isFailure)
        assertTrue(store.loadChatSessions().isFailure)
        assertTrue(store.ensureDefaultSession().isFailure)
        assertTrue(store.createChatSession().isFailure)
        assertTrue(store.seedBuiltInPluginsIfMissing().isFailure)
        assertTrue(store.clearAndStartNewChatSession("s1").isFailure)
    }

    @Test
    fun viewModel_whenAuthSignedOut_doesNotCrashDuringStartupSync() = runBlocking {
        auth.signOut()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = PreferencesRepository(context)
        val vm = ZaytharViewModel(
            firestoreRepo = FirestoreRepository(firestore),
            workspaceStore = FirestoreSessionAndBrainStore(firestore),
            prefsRepo = prefsRepo,
            ttsSpeaker = noOpSpeaker,
            currentUserId = "signed_out_user",
            firebaseInitialized = true
        )

        repeat(6) {
            ShadowLooper.idleMainLooper()
            delay(40)
        }
        assertTrue(vm.firebaseReady.value)
    }

    @Test
    fun attemptAutoSignIn_withNullCredentialManagerOrFinishingActivity_callsUnauthenticatedSafely() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            var unauthCalls = 0
            attemptAutoSignIn(
                context = context,
                credentialManager = null,
                onAuthSuccess = {},
                onUnauthenticated = { unauthCalls++ },
                scope = scope
            )
            assertTrue(unauthCalls == 1)

            var finishingCalls = 0
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val activity = Robolectric.buildActivity(Activity::class.java).create().get()
                activity.finish()
                attemptAutoSignIn(
                    context = activity,
                    credentialManager = androidx.credentials.CredentialManager.create(activity),
                    onAuthSuccess = {},
                    onUnauthenticated = { finishingCalls++ },
                    scope = scope
                )
            }
            assertTrue(finishingCalls == 1)
        } finally {
            scope.cancel()
        }
    }
}
