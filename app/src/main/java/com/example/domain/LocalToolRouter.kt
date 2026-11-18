package com.example.domain

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.MathContext
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.pow

data class ToolRouteResult(
    val tool: String,
    val result: String
)

object LocalToolRouter {

    private val calcPattern = Regex(
        pattern = """^(?:calc|calculate|what is|evaluate)\s+([0-9+\-*/().%\s^]+)\??$""",
        option = RegexOption.IGNORE_CASE
    )
    private val bareMathPattern = Regex("""^[0-9+\-*/().%\s^]+$""")
    private val timePattern = Regex(
        pattern = """\b(current utc time|time in utc|utc time|what time is it)\b""",
        option = RegexOption.IGNORE_CASE
    )
    private val unitPattern = Regex(
        pattern = """^(?:convert\s+)?(-?\d+(?:\.\d+)?)\s*(km|mi|miles|m|meters|ft|feet|cm|in|inch|inches|kg|lb|lbs|c|celsius|f|fahrenheit|kb|mb|gb|tb|ms|s|sec|min|hr|hours)\s+(?:to|in)\s+(km|mi|miles|m|meters|ft|feet|cm|in|inch|inches|kg|lb|lbs|c|celsius|f|fahrenheit|kb|mb|gb|tb|ms|s|sec|min|hr|hours)\??$""",
        option = RegexOption.IGNORE_CASE
    )
    private val jsonInspectPattern = Regex(
        pattern = """^(?:inspect|validate|format)\s+json\s+([\s\S]+)$""",
        option = RegexOption.IGNORE_CASE
    )
    private val uuidPattern = Regex(
        pattern = """^(?:generate\s+uuid|new\s+uuid|uuid\s*v4|uuid)$""",
        option = RegexOption.IGNORE_CASE
    )
    private val sha256Pattern = Regex(
        pattern = """^sha256\s+([\s\S]+)$""",
        option = RegexOption.IGNORE_CASE
    )
    private val base64EncodePattern = Regex(
        pattern = """^base64\s+encode\s+([\s\S]+)$""",
        option = RegexOption.IGNORE_CASE
    )
    private val base64DecodePattern = Regex(
        pattern = """^base64\s+decode\s+([\s\S]+)$""",
        option = RegexOption.IGNORE_CASE
    )
    private val hexToRgbPattern = Regex(
        pattern = """^#?([0-9a-fA-F]{6})\s+(?:to|in)\s+rgb$""",
        option = RegexOption.IGNORE_CASE
    )
    private val rgbToHexPattern = Regex(
        pattern = """^rgb\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})\s*\)\s+(?:to|in)\s+hex$""",
        option = RegexOption.IGNORE_CASE
    )
    private val textStatsPattern = Regex(
        pattern = """^(?:count\s+words|text\s+stats)\s+([\s\S]+)$""",
        option = RegexOption.IGNORE_CASE
    )

    fun routeTool(
        rawMessage: String,
        enabledPluginKeys: Set<String> = setOf(
            "calculator",
            "utc_clock",
            "unit_converter",
            "json_inspector",
            "crypto_hash_uuid",
            "color_text_metrics"
        )
    ): ToolRouteResult? {
        val message = rawMessage.trim()
        if (message.isEmpty()) return null

        if ("utc_clock" in enabledPluginKeys && timePattern.containsMatchIn(message) && message.length <= 80) {
            val nowUtc = ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'"))
            return ToolRouteResult(
                tool = "utc_time",
                result = "Current UTC time: $nowUtc (${ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)})"
            )
        }

        if ("unit_converter" in enabledPluginKeys) {
            unitPattern.matchEntire(message)?.let { m ->
                val value = m.groupValues[1].toDoubleOrNull()
                val from = normalizeUnit(m.groupValues[2])
                val to = normalizeUnit(m.groupValues[3])
                if (value != null) {
                    val converted = convertUnit(value, from, to)
                    if (converted != null) {
                        return ToolRouteResult(
                            tool = "unit_converter",
                            result = "${formatNumber(value)} $from = ${formatNumber(converted)} $to"
                        )
                    }
                }
            }
        }

        if ("json_inspector" in enabledPluginKeys) {
            jsonInspectPattern.matchEntire(message)?.let { m ->
                val rawJson = m.groupValues[1].trim()
                val formatted = inspectJson(rawJson)
                return ToolRouteResult(tool = "json_inspector", result = formatted)
            }
        }

        if ("crypto_hash_uuid" in enabledPluginKeys) {
            if (uuidPattern.matches(message)) {
                return ToolRouteResult(
                    tool = "crypto_uuid",
                    result = "Generated UUID v4: ${UUID.randomUUID()}"
                )
            }
            sha256Pattern.matchEntire(message)?.let { m ->
                val payload = m.groupValues[1]
                val digest = MessageDigest.getInstance("SHA-256")
                    .digest(payload.toByteArray(StandardCharsets.UTF_8))
                    .joinToString("") { "%02x".format(it) }
                return ToolRouteResult(tool = "crypto_sha256", result = "SHA-256: $digest")
            }
            base64EncodePattern.matchEntire(message)?.let { m ->
                val encoded = Base64.getEncoder()
                    .encodeToString(m.groupValues[1].toByteArray(StandardCharsets.UTF_8))
                return ToolRouteResult(tool = "crypto_base64", result = "Base64 Encoded: $encoded")
            }
            base64DecodePattern.matchEntire(message)?.let { m ->
                val token = m.groupValues[1].trim()
                return runCatching {
                    val decoded = String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8)
                    ToolRouteResult(tool = "crypto_base64", result = "Base64 Decoded: $decoded")
                }.getOrElse {
                    ToolRouteResult(tool = "crypto_base64", result = "Invalid Base64 input.")
                }
            }
        }

        if ("color_text_metrics" in enabledPluginKeys) {
            hexToRgbPattern.matchEntire(message)?.let { m ->
                val hex = m.groupValues[1].uppercase(Locale.ROOT)
                val r = hex.substring(0, 2).toInt(16)
                val g = hex.substring(2, 4).toInt(16)
                val b = hex.substring(4, 6).toInt(16)
                return ToolRouteResult(tool = "color_converter", result = "#$hex = rgb($r, $g, $b)")
            }
            rgbToHexPattern.matchEntire(message)?.let { m ->
                val r = m.groupValues[1].toIntOrNull()
                val g = m.groupValues[2].toIntOrNull()
                val b = m.groupValues[3].toIntOrNull()
                if (r != null && g != null && b != null && r in 0..255 && g in 0..255 && b in 0..255) {
                    val hex = "#%02X%02X%02X".format(r, g, b)
                    return ToolRouteResult(tool = "color_converter", result = "rgb($r, $g, $b) = $hex")
                }
                return ToolRouteResult(tool = "color_converter", result = "RGB channels must be between 0 and 255.")
            }
            textStatsPattern.matchEntire(message)?.let { m ->
                val text = m.groupValues[1]
                val words = text.trim().split(Regex("""\s+""")).count { it.isNotBlank() }
                val chars = text.length
                val charsNoSpace = text.count { !it.isWhitespace() }
                val lines = text.lines().size
                val estTokens = ceil(chars / 4.0).toInt().coerceAtLeast(1)
                return ToolRouteResult(
                    tool = "text_metrics",
                    result = "Words: $words | Chars: $chars ($charsNoSpace non-space) | Lines: $lines | Est. Tokens: ~$estTokens"
                )
            }
        }

        if ("calculator" in enabledPluginKeys) {
            val match = calcPattern.matchEntire(message)
            val expr = when {
                match != null -> match.groupValues[1].trim()
                bareMathPattern.matches(message) && message.any { it in "+-*/%^" } -> message
                else -> null
            }

            if (expr != null) {
                return try {
                    val value = calculate(expr)
                    ToolRouteResult(tool = "calculator", result = value)
                } catch (e: IllegalArgumentException) {
                    ToolRouteResult(tool = "calculator", result = "Calculator error: ${e.message}")
                }
            }
        }

        return null
    }

    fun inspectJson(rawJson: String): String {
        val trimmed = rawJson.trim()
        return try {
            when {
                trimmed.startsWith("{") -> {
                    val obj = JSONObject(trimmed)
                    "Valid JSON Object (${obj.length()} keys):\n${obj.toString(2)}"
                }
                trimmed.startsWith("[") -> {
                    val arr = JSONArray(trimmed)
                    "Valid JSON Array (${arr.length()} items):\n${arr.toString(2)}"
                }
                else -> "Invalid JSON: payload must start with '{' or '['."
            }
        } catch (e: Exception) {
            "Invalid JSON: ${e.localizedMessage ?: "syntax error"}"
        }
    }

    private fun normalizeUnit(raw: String): String = when (raw.lowercase(Locale.ROOT)) {
        "miles" -> "mi"
        "meters" -> "m"
        "feet" -> "ft"
        "inch", "inches" -> "in"
        "lbs" -> "lb"
        "celsius" -> "c"
        "fahrenheit" -> "f"
        "sec" -> "s"
        "hours" -> "hr"
        else -> raw.lowercase(Locale.ROOT)
    }

    fun convertUnit(value: Double, from: String, to: String): Double? {
        if (from == to) return value
        val exact = when (from to to) {
            "km" to "mi" -> value * 0.621371192
            "mi" to "km" -> value / 0.621371192
            "m" to "ft" -> value * 3.280839895
            "ft" to "m" -> value / 3.280839895
            "cm" to "in" -> value / 2.54
            "in" to "cm" -> value * 2.54
            "kg" to "lb" -> value * 2.20462262
            "lb" to "kg" -> value / 2.20462262
            "c" to "f" -> (value * 9.0 / 5.0) + 32.0
            "f" to "c" -> (value - 32.0) * 5.0 / 9.0
            "mb" to "gb" -> value / 1024.0
            "gb" to "mb" -> value * 1024.0
            else -> null
        }
        if (exact != null) return exact

        val dataUnitsKb = mapOf("kb" to 1.0, "mb" to 1024.0, "gb" to 1024.0 * 1024.0, "tb" to 1024.0 * 1024.0 * 1024.0)
        val fromData = dataUnitsKb[from]
        val toData = dataUnitsKb[to]
        if (fromData != null && toData != null) {
            return (value * fromData) / toData
        }

        val timeUnitsMs = mapOf("ms" to 1.0, "s" to 1000.0, "min" to 60_000.0, "hr" to 3_600_000.0)
        val fromTime = timeUnitsMs[from]
        val toTime = timeUnitsMs[to]
        if (fromTime != null && toTime != null) {
            return (value * fromTime) / toTime
        }

        return null
    }

    fun reasoningHint(message: String): String {
        val lower = message.lowercase()
        return when {
            listOf("code", "bug", "error", "api", "kotlin", "python", "sql", "compose")
                .any { it in lower } ->
                "Provide runnable, clean code with clear verification steps."
            listOf("plan", "schedule", "roadmap", "architecture", "design")
                .any { it in lower } ->
                "Break the answer into structured phases, risks, and immediate next actions."
            listOf("compare", "versus", "vs", "choose", "tradeoff")
                .any { it in lower } ->
                "Compare concrete options side by side and make a clear recommendation."
            else ->
                "Be concise, well-structured, and directly actionable."
        }
    }

    fun calculate(rawExpr: String): String {
        val expr = rawExpr.replace("**", "^").trim()
        require(expr.isNotEmpty() && expr.length <= 120) {
            "Expression must be 1 to 120 characters."
        }
        val parser = MathParser(expr)
        val result = parser.parseExpression()
        if (parser.pos < expr.length) {
            throw IllegalArgumentException("Unexpected token '${expr[parser.pos]}' at position ${parser.pos}.")
        }
        if (result.isNaN() || result.isInfinite()) {
            throw IllegalArgumentException("Numeric result is out of range.")
        }
        return formatNumber(result)
    }

    private fun formatNumber(result: Double): String {
        val bd = BigDecimal.valueOf(result)
            .round(MathContext(12))
            .stripTrailingZeros()
        val text = bd.toPlainString()
        return if (text == "-0") "0" else text
    }

    private class MathParser(private val input: String) {
        var pos = 0

        private fun skipWhitespace() {
            while (pos < input.length && input[pos].isWhitespace()) {
                pos++
            }
        }

        fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipWhitespace()
                if (pos >= input.length) break
                value = when (input[pos]) {
                    '+' -> { pos++; value + parseTerm() }
                    '-' -> { pos++; value - parseTerm() }
                    else -> break
                }
            }
            return value
        }

        private fun parseTerm(): Double {
            var value = parsePower()
            while (true) {
                skipWhitespace()
                if (pos >= input.length) break
                value = when (input[pos]) {
                    '*' -> { pos++; value * parsePower() }
                    '/' -> {
                        pos++
                        val divisor = parsePower()
                        if (divisor == 0.0) throw IllegalArgumentException("Division by zero is not allowed.")
                        value / divisor
                    }
                    '%' -> {
                        pos++
                        val divisor = parsePower()
                        if (divisor == 0.0) throw IllegalArgumentException("Modulo by zero is not allowed.")
                        value % divisor
                    }
                    else -> break
                }
            }
            return value
        }

        private fun parsePower(): Double {
            val base = parseUnary()
            skipWhitespace()
            if (pos < input.length && input[pos] == '^') {
                pos++
                val exponent = parsePower()
                if (kotlin.math.abs(exponent) > 10.0 && kotlin.math.abs(base) > 1.0) {
                    throw IllegalArgumentException("Exponent exceeds safe calculation limit.")
                }
                return base.pow(exponent)
            }
            return base
        }

        private fun parseUnary(): Double {
            skipWhitespace()
            if (pos < input.length) {
                if (input[pos] == '+') { pos++; return parseUnary() }
                if (input[pos] == '-') { pos++; return -parseUnary() }
            }
            return parseFactor()
        }

        private fun parseFactor(): Double {
            skipWhitespace()
            if (pos >= input.length) {
                throw IllegalArgumentException("Unexpected end of expression.")
            }
            if (input[pos] == '(') {
                pos++
                val value = parseExpression()
                skipWhitespace()
                if (pos >= input.length || input[pos] != ')') {
                    throw IllegalArgumentException("Missing closing parenthesis.")
                }
                pos++
                return value
            }
            val start = pos
            var hasDot = false
            while (pos < input.length && (input[pos].isDigit() || (input[pos] == '.' && !hasDot))) {
                if (input[pos] == '.') hasDot = true
                pos++
            }
            if (start == pos) {
                throw IllegalArgumentException("Expected a number at position $pos.")
            }
            return input.substring(start, pos).toDoubleOrNull()
                ?: throw IllegalArgumentException("Invalid number format.")
        }
    }
}
