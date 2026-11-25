package com.example.domain

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class WebSourceSnippet(
    val title: String,
    val url: String,
    val snippet: String
)

data class WebGroundingResult(
    val queryOrUrl: String,
    val snippets: List<WebSourceSnippet> = emptyList(),
    val error: String? = null
) {
    val sourceUrls: List<String>
        get() = snippets.map { it.url }.distinct().take(10)

    fun toContextBlock(): String {
        if (snippets.isEmpty()) return ""
        return buildString {
            append("LIVE PUBLIC WEB CONTEXT (unverified web data, cite sources concisely):\n")
            snippets.take(5).forEachIndexed { idx, s ->
                append("[${idx + 1}] ${s.title} (${s.url})\n")
                append(s.snippet.take(1500))
                append("\n\n")
            }
        }.trim()
    }
}

object WebConnector {
    private const val TAG = "WebConnector"
    private const val WEB_TIMEOUT_MS = 12_000L
    private const val MAX_BODY_CHARS = 32_000
    private val URL_REGEX = Regex("""https://[^\s<>"'\)\]]+""")

    fun extractPublicHttpsUrls(text: String): List<String> =
        URL_REGEX.findAll(text)
            .map { it.value.trimEnd('.', ',', ';', ':') }
            .filter { validatePublicHttpsUrl(it).isSuccess }
            .distinct()
            .take(2)
            .toList()

    fun validatePublicHttpsUrl(rawUrl: String): Result<URI> {
        val trimmed = rawUrl.trim()
        val uri = runCatching { URI(trimmed) }.getOrElse {
            return Result.failure(IllegalArgumentException("Malformed URL."))
        }
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("Only secure https:// URLs are permitted."))
        }
        val host = uri.host?.lowercase()?.trim()
            ?: return Result.failure(IllegalArgumentException("URL host is missing."))
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal")) {
            return Result.failure(IllegalArgumentException("Local or internal hostnames are blocked."))
        }
        if (isPrivateOrReservedIpLiteral(host)) {
            return Result.failure(IllegalArgumentException("Private or loopback IP literals are blocked."))
        }
        return Result.success(uri)
    }

    internal fun isPrivateOrReservedIpLiteral(host: String): Boolean {
        val clean = host.trim('[', ']')
        val ipv4Parts = clean.split('.')
        if (ipv4Parts.size == 4 && ipv4Parts.all { it.toIntOrNull() in 0..255 }) {
            val b0 = ipv4Parts[0].toInt()
            val b1 = ipv4Parts[1].toInt()
            return b0 == 0 ||
                b0 == 10 ||
                b0 == 127 ||
                (b0 == 169 && b1 == 254) ||
                (b0 == 172 && b1 in 16..31) ||
                (b0 == 192 && b1 == 168) ||
                (b0 == 100 && b1 in 64..127)
        }
        if (clean.contains(':')) {
            val lower = clean.lowercase()
            return lower == "::1" ||
                lower == "::" ||
                lower.startsWith("fe80:") ||
                lower.startsWith("fc") ||
                lower.startsWith("fd")
        }
        return false
    }

    private fun isPublicResolvedAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress ||
            address.isLoopbackAddress ||
            address.isLinkLocalAddress ||
            address.isSiteLocalAddress ||
            address.isMulticastAddress
        ) {
            return false
        }
        return when (address) {
            is Inet4Address -> {
                val b = address.address
                val first = b[0].toInt() and 0xFF
                val second = b[1].toInt() and 0xFF
                !(first == 0 || first == 10 || first == 127 ||
                    (first == 100 && second in 64..127) ||
                    (first == 169 && second == 254) ||
                    (first == 172 && second in 16..31) ||
                    (first == 192 && second == 168))
            }
            is Inet6Address -> {
                val b0 = address.address[0].toInt() and 0xFF
                // Unique local fc00::/7
                (b0 and 0xFE) != 0xFC
            }
            else -> true
        }
    }

    suspend fun groundPromptWithInternet(prompt: String): WebGroundingResult = withContext(Dispatchers.IO) {
        val explicitUrls = extractPublicHttpsUrls(prompt)
        val collected = mutableListOf<WebSourceSnippet>()

        for (url in explicitUrls) {
            fetchWebPageSnippet(url).getOrNull()?.let { collected.add(it) }
        }

        val cleanQuery = prompt.replace(URL_REGEX, " ").trim()
        if (cleanQuery.length >= 3) {
            searchPublicWeb(cleanQuery).getOrNull()?.let { collected.addAll(it) }
        }

        if (collected.isEmpty() && explicitUrls.isNotEmpty()) {
            val firstErr = fetchWebPageSnippet(explicitUrls.first()).exceptionOrNull()?.localizedMessage
            WebGroundingResult(queryOrUrl = prompt.take(200), snippets = emptyList(), error = firstErr)
        } else {
            WebGroundingResult(queryOrUrl = prompt.take(200), snippets = collected.take(5))
        }
    }

    suspend fun fetchWebPageSnippet(rawUrl: String): Result<WebSourceSnippet> = withContext(Dispatchers.IO) {
        val uriResult = validatePublicHttpsUrl(rawUrl)
        if (uriResult.isFailure) {
            return@withContext Result.failure(uriResult.exceptionOrNull()!!)
        }
        val uri = uriResult.getOrThrow()
        try {
            withTimeout(WEB_TIMEOUT_MS) {
                val host = uri.host
                val resolved = InetAddress.getAllByName(host)
                if (resolved.isEmpty() || resolved.any { !isPublicResolvedAddress(it) }) {
                    throw SecurityException("Resolved address is non-public and blocked.")
                }
                val rawBody = httpGetText(uri.toASCIIString())
                val title = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                    .find(rawBody)?.groupValues?.getOrNull(1)
                    ?.let(::stripHtml)
                    ?.take(120)
                    ?.ifBlank { host }
                    ?: host
                val bodyText = stripHtml(rawBody).take(3000)
                if (bodyText.isBlank()) {
                    throw IllegalStateException("Fetched page contained no readable text.")
                }
                Result.success(WebSourceSnippet(title = title, url = uri.toASCIIString(), snippet = bodyText))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Web fetch failed for $rawUrl", e)
            Result.failure(e)
        }
    }

    suspend fun searchPublicWeb(query: String): Result<List<WebSourceSnippet>> = withContext(Dispatchers.IO) {
        val cleaned = query.trim().take(240)
        if (cleaned.length < 2) return@withContext Result.success(emptyList())
        try {
            withTimeout(WEB_TIMEOUT_MS) {
                val firecrawlResults = try {
                    val callable = FirebaseFunctions.getInstance("us-central1")
                        .getHttpsCallable("firecrawlSearch")
                    val response = callable.call(mapOf("query" to cleaned)).await()
                    val payload = response.data as? Map<*, *>
                        ?: throw IllegalStateException("Firecrawl returned an invalid response.")
                    val rows = payload["results"] as? List<*> ?: emptyList<Any>()
                    rows.mapNotNull { item ->
                        val row = item as? Map<*, *> ?: return@mapNotNull null
                        val title = (row["title"] as? String).orEmpty().ifBlank { "Web result" }
                        val url = (row["url"] as? String).orEmpty()
                        val snippet = (row["description"] as? String).orEmpty()
                        if (snippet.isBlank() || validatePublicHttpsUrl(url).isFailure) null
                        else WebSourceSnippet(title.take(120), url, snippet.take(1800))
                    }.distinctBy { it.url }.take(5)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Log.w(TAG, "Firebase Firecrawl search unavailable; using public fallback (" + failure.javaClass.simpleName + ").")
                    emptyList()
                }

                if (firecrawlResults.isNotEmpty()) {
                    return@withContext Result.success(firecrawlResults)
                }

                val results = mutableListOf<WebSourceSnippet>()
                val encoded = URLEncoder.encode(cleaned, StandardCharsets.UTF_8.name())

                // 1. DuckDuckGo Instant Answer API (free, zero-key)
                runCatching {
                    val ddgJson = httpGetText("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1")
                    val obj = JSONObject(ddgJson)
                    val abstractText = obj.optString("AbstractText", "").trim()
                    val abstractUrl = obj.optString("AbstractURL", "").trim()
                    val heading = obj.optString("Heading", cleaned).trim()
                    if (abstractText.isNotBlank() && abstractUrl.startsWith("https://")) {
                        results.add(WebSourceSnippet(heading.ifBlank { cleaned }, abstractUrl, abstractText.take(1600)))
                    }
                    val related: JSONArray? = obj.optJSONArray("RelatedTopics")
                    if (related != null) {
                        for (i in 0 until minOf(related.length(), 3)) {
                            val item = related.optJSONObject(i) ?: continue
                            val text = item.optString("Text", "").trim()
                            val firstUrl = item.optString("FirstURL", "").trim()
                            if (text.isNotBlank() && firstUrl.startsWith("https://")) {
                                results.add(WebSourceSnippet(text.take(70), firstUrl, text.take(800)))
                            }
                        }
                    }
                }

                // 2. Wikipedia OpenSearch + Summary REST (free, zero-key)
                if (results.size < 3) {
                    runCatching {
                        val wikiSearch = httpGetText(
                            "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&srlimit=3&format=json"
                        )
                        val queryObj = JSONObject(wikiSearch).optJSONObject("query")
                        val searchArr = queryObj?.optJSONArray("search")
                        if (searchArr != null) {
                            for (i in 0 until minOf(searchArr.length(), 3)) {
                                val entry = searchArr.optJSONObject(i) ?: continue
                                val title = entry.optString("title", "").trim()
                                val snippet = stripHtml(entry.optString("snippet", ""))
                                if (title.isNotBlank() && snippet.isNotBlank()) {
                                    val slug = URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8.name())
                                    results.add(
                                        WebSourceSnippet(
                                            title = "Wikipedia · $title",
                                            url = "https://en.wikipedia.org/wiki/$slug",
                                            snippet = snippet
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Result.success(results.distinctBy { it.url }.take(5))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Public web search failed", e)
            Result.failure(e)
        }
    }

    private fun httpGetText(urlString: String): String {
        val conn = (URI(urlString).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7_000
            readTimeout = 8_000
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "ZaytharAI-Android/2.0")
            setRequestProperty("Accept", "application/json, text/html, text/plain")
        }
        return try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code from ${URI(urlString).host}")
            }
            val stream = conn.inputStream
            BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { reader ->
                val sb = StringBuilder()
                val buffer = CharArray(2048)
                while (sb.length < MAX_BODY_CHARS) {
                    val read = reader.read(buffer)
                    if (read == -1) break
                    sb.append(buffer, 0, minOf(read, MAX_BODY_CHARS - sb.length))
                }
                sb.toString()
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun stripHtml(html: String): String {
        val noScripts = html
            .replace(Regex("""<script[^>]*>.*?</script>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
            .replace(Regex("""<style[^>]*>.*?</style>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), " ")
        return noScripts
            .replace(Regex("""<[^>]+>"""), " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
