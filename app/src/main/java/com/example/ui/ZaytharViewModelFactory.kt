package com.example.ui

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.local.PreferencesRepository
import com.example.data.remote.FirestoreRepository
import com.example.data.remote.FirestoreSessionAndBrainStore
import com.example.domain.GuestWorkspaceStore
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException

internal fun createZaytharViewModelFactory(
    application: Application,
    prefsRepo: PreferencesRepository,
    currentUserId: String,
    guestStore: GuestWorkspaceStore? = null
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val db = FirebaseFirestore.getInstance(application.getString(R.string.firestore_database_id))
        val firebaseReady = runCatching { FirebaseApp.getApps(application).isNotEmpty() }.getOrDefault(false)
        ZaytharViewModel(
            FirestoreRepository(db),
            FirestoreSessionAndBrainStore(db),
            prefsRepo,
            AndroidTtsSpeaker(application),
            currentUserId,
            firebaseReady,
            guestStore
        )
    }
}

internal fun unwrapFirestoreUiError(error: Throwable): Throwable {
    val firestoreError = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    return firestoreError ?: error
}
