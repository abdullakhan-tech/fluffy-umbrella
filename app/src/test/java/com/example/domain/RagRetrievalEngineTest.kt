package com.example.domain

import com.example.data.remote.FirestoreKnowledgeFact
import com.example.data.remote.FirestoreMemoryNote
import com.example.data.remote.FirestoreSessionMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RagRetrievalEngineTest {

    @Test
    fun tokenize_splitsCamelCaseAndSnakeCaseAndRemovesStopWords() {
        val tokens = RagRetrievalEngine.tokenize("clearAndStartNewChatSession auth_token_verifier the and in")
        assertTrue("clearandstartnewchatsession" in tokens)
        assertTrue("clear" in tokens)
        assertTrue("session" in tokens)
        assertTrue("auth" in tokens)
        assertTrue("verifier" in tokens)
        assertFalse("the" in tokens)
        assertFalse("and" in tokens)
    }

    @Test
    fun chunkText_splitsLongDocumentsWithOverlapOnWordBoundaries() {
        val longText = buildString {
            repeat(30) { idx ->
                append("Sentence number $idx describes neural pipeline calibration and memory safety. ")
            }
        }
        val chunks = RagRetrievalEngine.chunkText(longText, maxChunkChars = 280, overlapChars = 60)
        assertTrue(chunks.size >= 4)
        assertTrue(chunks.all { it.length <= 290 })
        // Verify consecutive chunks share overlapping words
        val firstTailWords = chunks[0].split(" ").takeLast(4).toSet()
        val secondHeadWords = chunks[1].split(" ").take(12).toSet()
        assertTrue(firstTailWords.intersect(secondHeadWords).isNotEmpty())
    }

    @Test
    fun retrieve_ranksRelevantDocumentsAndBoostsKnowledgeGraphTriples() {
        val facts = listOf(
            FirestoreKnowledgeFact(
                id = "f1",
                subject = "Zaythar RAG Engine",
                predicate = "ranks_via",
                objectValue = "BM25 and bigram phrase scoring",
                source = "architecture"
            )
        )
        val memories = listOf(
            FirestoreMemoryNote(
                id = "m1",
                content = "Prefer Compose StateFlow viewmodels for reactive UI state updates."
            ),
            FirestoreMemoryNote(
                id = "m2",
                content = "BM25 lexical scoring pairs with bigram phrase matching inside the RAG retrieval pipeline."
            ),
            FirestoreMemoryNote(
                id = "m3",
                content = "Weekend grocery reminder: tomatoes, basil, and olive oil."
            )
        )
        val messages = listOf(
            FirestoreSessionMessage(id = "s1", role = "user", text = "How does BM25 scoring work?")
        )

        val bundle = RagRetrievalEngine.retrieve(
            query = "BM25 bigram phrase scoring",
            memories = memories,
            facts = facts,
            recentMessages = messages,
            topK = 3
        )

        assertTrue(bundle.hasMatches)
        assertEquals(RagSourceType.KNOWLEDGE_GRAPH, bundle.chunks.first().sourceType)
        assertFalse(bundle.chunks.any { it.id.startsWith("m3") })
        val context = bundle.toContextBlock(maxChars = 1500)
        assertTrue(context.contains("[RAG-1 · GRAPH"))
    }

    @Test
    fun retrieveFromDocuments_suppressesNearDuplicateChunks() {
        val docs = listOf(
            RagDocument(
                id = "d1",
                sourceType = RagSourceType.MEMORY,
                title = "Note 1",
                content = "Zero-trust Firestore rules enforce UID scoping on every user document.",
                authorityWeight = 1.2f
            ),
            RagDocument(
                id = "d2",
                sourceType = RagSourceType.MEMORY,
                title = "Note 2",
                content = "Zero-trust Firestore rules enforce UID scoping on every user document.",
                authorityWeight = 1.0f
            ),
            RagDocument(
                id = "d3",
                sourceType = RagSourceType.MEMORY,
                title = "Note 3",
                content = "Android Photo Picker provides zero-permission media selection for Camera Eyes.",
                authorityWeight = 1.1f
            )
        )

        val bundle = RagRetrievalEngine.retrieveFromDocuments(
            query = "Firestore UID scoping and Photo Picker",
            documents = docs,
            topK = 5
        )

        val matchedIds = bundle.chunks.map { it.id.substringBefore('#') }
        assertTrue("d1" in matchedIds)
        assertFalse("d2" in matchedIds)
        assertTrue("d3" in matchedIds)
    }

    @Test
    fun retrieve_emptyQueryReturnsFallbackSortedRecentChunks() {
        val memories = listOf(
            FirestoreMemoryNote(id = "m1", content = "Neural Brain structured JSON synthesis"),
            FirestoreMemoryNote(id = "m2", content = "Code Copilot Kotlin coroutine safety")
        )
        val bundle = RagRetrievalEngine.retrieve(query = "   ", memories = memories, topK = 2)
        assertEquals(2, bundle.chunks.size)
        val bounded = bundle.toContextBlock(maxChars = 220)
        assertTrue(bounded.length <= 220)
        assertTrue(bundle.toContextBlock(maxChars = 80).isEmpty())
    }
}
