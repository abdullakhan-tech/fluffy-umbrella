package com.example.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val operatorName: String = "OPERATOR 01",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreChatSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "Primary Nexus",
    val agent: String = "general",
    val lastMessagePreview: String = "",
    val messageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreSessionMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sessionId: String = "",
    val role: String = "user",
    val text: String = "",
    val agent: String = "general",
    val modelLabel: String = "",
    val hasImage: Boolean = false,
    val webSources: List<String> = emptyList(),
    val webSourceTitles: List<String> = emptyList(),
    val searchQueries: List<String> = emptyList(),
    val searchEntryPointHtml: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val text: String = "",
    val agent: String = "general",
    val modelLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreMemoryNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val content: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreKnowledgeFact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = "",
    val predicate: String = "",
    val objectValue: String = "",
    val source: String = "operator",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestorePluginConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val pluginKey: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "System",
    val enabled: Boolean = false,
    val systemInstruction: String = "",
    val isBuiltIn: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreBrainRun(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = "reasoner",
    val prompt: String = "",
    val summary: String = "",
    val confidence: Long = 0L,
    val insights: List<String> = emptyList(),
    val suggestedTriplesCount: Long = 0L,
    val modelUsed: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
