package com.example.ui

import com.example.data.remote.FirestoreRepository
import com.example.data.remote.FirestoreSessionAndBrainStore
import com.example.domain.GuestWorkspaceStore
import com.example.domain.SuggestedTriple
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class NexusWorkspaceCrudDelegate(
    private val scope: CoroutineScope,
    private val firestoreRepo: FirestoreRepository,
    private val workspaceStore: FirestoreSessionAndBrainStore,
    private val guestStore: GuestWorkspaceStore?
) {

    fun addMemoryNote(rawContent: String, onToast: (String) -> Unit) {
        val cleaned = rawContent.trim()
        if (cleaned.isEmpty()) return
        if (cleaned.length > 2000) {
            onToast("Note must be 2000 characters or fewer.")
            return
        }
        if (guestStore != null) {
            guestStore.addMemoryNote(cleaned)
            onToast("Note saved to local guest RAG memory.")
            return
        }
        scope.launch {
            val res = firestoreRepo.addMemoryNote(cleaned)
            onToast(
                if (res.isSuccess) "Note synced to Zaythar cloud memory."
                else "Could not save memory note. Please try again."
            )
        }
    }

    fun deleteMemoryNote(id: String, onToast: (String) -> Unit) {
        if (guestStore != null) {
            guestStore.deleteMemoryNote(id)
            return
        }
        scope.launch {
            if (firestoreRepo.deleteMemoryNote(id).isFailure) onToast("Failed to delete note.")
        }
    }

    fun addKnowledgeFact(
        rawSubject: String,
        rawPredicate: String,
        rawObject: String,
        source: String,
        onToast: (String) -> Unit
    ) {
        val subject = rawSubject.trim()
        val predicate = rawPredicate.trim()
        val obj = rawObject.trim()
        if (subject.isEmpty() || predicate.isEmpty() || obj.isEmpty()) {
            onToast("Fill in subject, relation, and object.")
            return
        }
        if (subject.length > 300 || predicate.length > 300 || obj.length > 300) {
            onToast("Each field must be 300 characters or fewer.")
            return
        }
        if (guestStore != null) {
            val res = guestStore.addKnowledgeFact(subject, predicate, obj, source)
            onToast(if (res.isSuccess) "Knowledge fact added to guest RAG graph." else "Could not add knowledge fact. Please try again.")
            return
        }
        scope.launch {
            if (firestoreRepo.countKnowledgeFacts() >= 200) {
                onToast("Knowledge graph limit reached (200 facts).")
                return@launch
            }
            val res = firestoreRepo.addKnowledgeFact(subject, predicate, obj, source)
            onToast(
                if (res.isSuccess) "Knowledge fact synced to Cloud Firestore."
                else "Could not save knowledge fact. Please try again."
            )
        }
    }

    fun deleteKnowledgeFact(id: String, onToast: (String) -> Unit) {
        if (guestStore != null) {
            guestStore.deleteKnowledgeFact(id)
            return
        }
        scope.launch {
            if (firestoreRepo.deleteKnowledgeFact(id).isFailure) onToast("Failed to delete knowledge fact.")
        }
    }

    fun commitSuggestedTriples(triples: List<SuggestedTriple>, onToast: (String) -> Unit) {
        if (triples.isEmpty()) return
        if (guestStore != null) {
            val saved = triples.count { t -> guestStore.addKnowledgeFact(t.subject, t.predicate, t.objectValue, "neural_brain").isSuccess }
            onToast("Committed $saved knowledge triple(s) to guest RAG graph.")
            return
        }
        scope.launch {
            val currentCount = firestoreRepo.countKnowledgeFacts()
            if (currentCount >= 200) {
                onToast("Knowledge graph limit reached (200 facts).")
                return@launch
            }
            val saved = triples.take(200 - currentCount).count { t ->
                firestoreRepo.addKnowledgeFact(t.subject, t.predicate, t.objectValue, source = "neural_brain").isSuccess
            }
            onToast("Committed $saved knowledge triple(s) to Cloud Firestore.")
        }
    }

    fun togglePlugin(pluginId: String, enabled: Boolean, onToast: (String) -> Unit) {
        if (guestStore != null) {
            guestStore.setPluginEnabled(pluginId, enabled)
            return
        }
        scope.launch {
            if (workspaceStore.setPluginEnabled(pluginId, enabled).isFailure) onToast("Failed to update plugin state.")
        }
    }

    fun addCustomPlugin(
        name: String,
        description: String,
        category: String,
        systemInstruction: String,
        onToast: (String) -> Unit
    ) {
        if (name.isBlank() || description.isBlank()) {
            onToast("Plugin name and description are required.")
            return
        }
        if (guestStore != null) {
            guestStore.addCustomPlugin(name, description, category, systemInstruction)
            onToast("Custom plugin plugged into guest core.")
            return
        }
        scope.launch {
            val res = workspaceStore.addCustomPlugin(name, description, category, systemInstruction)
            onToast(
                if (res.isSuccess) "Custom plugin plugged into Zaythar Core."
                else "Could not add plugin. Please try again."
            )
        }
    }

    fun deletePlugin(pluginId: String, onToast: (String) -> Unit) {
        if (guestStore != null) {
            guestStore.deletePlugin(pluginId)
            onToast("Plugin unplugged and removed.")
            return
        }
        scope.launch {
            val res = workspaceStore.deletePlugin(pluginId)
            onToast(if (res.isSuccess) "Plugin unplugged and removed." else "Failed to remove plugin.")
        }
    }
}
