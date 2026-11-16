package com.example.domain

import java.time.Instant
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

data class LocalToolResult(
    val tool: String,
    val result: String
)

/**
 * Safe local tool router ported from backend/tools.py and backend/reasoning.py.
 * Evaluates bounded arithmetic expressions via recursive descent without dynamic code execution,
 * and resolves UTC time queries locally.
 */
object LocalToolRouter {
    private val calcPattern = Regex(
        """^(?:calculate|compute|what is)\s+([0-9().+*/%\- ]{1,120})\??$""",
        RegexOption.IGNORE_CASE
    )
    private val utcPattern = Regex(
        """\b(utc time|current utc time|time in utc)\b""",
        RegexOption.IGNORE_CASE
    )
    private val comparePattern = Regex(
        """\b(compare| versus | vs |pros and cons)\b""",
        RegexOption.IGNORE_CASE
    )
    private val planPattern = Regex(
        """\b(plan|build|create|implement|project)\b""",
        RegexOption.IGNORE_CASE
    )

    fun routeTool(message: String): LocalToolResult? {
        val trimmed = message.trim()
        val calcMatch = calcPattern.matchEntire(trimmed)
        if (calcMatch != null) {
            val expr = calcMatch.groupValues[1].trim()
            val value = calculate(expr)
            return LocalToolResult(tool = "calculator", result = value)
        }
        if (utcPattern.containsMatchIn(trimmed)) {
            return LocalToolResult(tool = "utc_time", result = Instant.now().toString())
        }
        return null
    }

    fun reasoningHint(message: String): String {
        val text = message.trim()
        return when {
            text.length > 500 -> "Break the request into key goals, constraints, and actionable steps."
            comparePattern.containsMatchIn(text) -> "Compare options using consistent criteria, evidence, trade-offs, and a recommendation."
            planPattern.containsMatchIn(text) -> "Identify requirements, sequence the work, check dependencies, and validate the result."
            else -> "Answer directly; verify assumptions and state uncertainty where relevant."
        }
    }

    fun calculate(expression: String): String {
        if (expression.length > 120) {
            throw IllegalArgumentException("Expression too long (max 120 characters)")
        }
        val parser = ArithmeticParser(expression)
        val result = parser.parseExpression()
        if (parser.hasRemaining()) {
            throw IllegalArgumentException("Only basic arithmetic is supported")
        }
        if (result.isNaN() || result.isInfinite() || abs(result) > 1e15) {
            throw IllegalArgumentException("Result too large")
        }
        return if (result % 1.0 == 0.0 && abs(result) <= Long.MAX_VALUE.toDouble()) {
            result.toLong().toString()
        } else {
            result.toString()
        }
    }

    private class ArithmeticParser(private val input: String) {
        private var pos = 0

        fun hasRemaining(): Boolean {
            skipWhitespace()
            return pos < input.length
        }

        private fun skipWhitespace() {
            while (pos < input.length && input[pos].isWhitespace()) {
                pos++
            }
        }

        fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipWhitespace()
                when {
                    match('+') -> value += parseTerm()
                    match('-') -> value -= parseTerm()
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parsePower()
            while (true) {
                skipWhitespace()
                when {
                    matchDoubleSlash() -> {
                        val divisor = parsePower()
                        if (divisor == 0.0) throw IllegalArgumentException("Division by zero")
                        value = floor(value / divisor)
                    }
                    matchDoubleStar() -> {
                        val exp = parsePower()
                        if (abs(exp) > 8.0) throw IllegalArgumentException("Exponent too large")
                        value = value.pow(exp)
                    }
                    match('*') -> value *= parsePower()
                    match('/') -> {
                        val divisor = parsePower()
                        if (divisor == 0.0) throw IllegalArgumentException("Division by zero")
                        value /= divisor
                    }
                    match('%') -> {
                        val divisor = parsePower()
                        if (divisor == 0.0) throw IllegalArgumentException("Modulo by zero")
                        value %= divisor
                    }
                    else -> return value
                }
            }
        }

        private fun parsePower(): Double {
            skipWhitespace()
            if (match('+')) return parsePower()
            if (match('-')) return -parsePower()
            val base = parsePrimary()
            skipWhitespace()
            if (matchDoubleStar()) {
                val exp = parsePower()
                if (abs(exp) > 8.0) throw IllegalArgumentException("Exponent too large")
                return base.pow(exp)
            }
            return base
        }

        private fun parsePrimary(): Double {
            skipWhitespace()
            if (match('(')) {
                val value = parseExpression()
                skipWhitespace()
                if (!match(')')) {
                    throw IllegalArgumentException("Missing closing parenthesis")
                }
                return value
            }
            val start = pos
            var hasDot = false
            while (pos < input.length) {
                val c = input[pos]
                if (c.isDigit()) {
                    pos++
                } else if (c == '.' && !hasDot) {
                    hasDot = true
                    pos++
                } else {
                    break
                }
            }
            if (start == pos) {
                throw IllegalArgumentException("Only basic arithmetic is supported")
            }
            return input.substring(start, pos).toDoubleOrNull()
                ?: throw IllegalArgumentException("Invalid number")
        }

        private fun match(ch: Char): Boolean {
            if (pos < input.length && input[pos] == ch) {
                pos++
                return true
            }
            return false
        }

        private fun matchDoubleSlash(): Boolean {
            if (pos + 1 < input.length && input[pos] == '/' && input[pos + 1] == '/') {
                pos += 2
                return true
            }
            return false
        }

        private fun matchDoubleStar(): Boolean {
            if (pos + 1 < input.length && input[pos] == '*' && input[pos + 1] == '*') {
                pos += 2
                return true
            }
            return false
        }
    }
}
