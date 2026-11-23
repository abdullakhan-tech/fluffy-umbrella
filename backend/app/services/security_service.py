"""OpenJarvis-inspired Security Guardrails: Secret Scanner and Prompt Injection Protection."""

from __future__ import annotations

import logging
import re
from typing import List, Tuple
from app.models import SecurityFinding, SecurityScanResponse

logger = logging.getLogger(__name__)


class SecurityScanner:
    """Detects sensitive secrets and malicious prompt injection patterns."""

    SECRET_PATTERNS: List[Tuple[str, str, str, str]] = [
        ("openai_key", r"sk-[A-Za-z0-9_-]{20,}", "critical", "OpenAI API key"),
        ("anthropic_key", r"sk-ant-[A-Za-z0-9_-]{20,}", "critical", "Anthropic API key"),
        ("aws_access_key", r"AKIA[0-9A-Z]{16}", "critical", "AWS access key"),
        ("github_token", r"(?:ghp|gho|ghs|ghr|github_pat)_[A-Za-z0-9_]{36,}", "critical", "GitHub token"),
        ("private_key", r"-----BEGIN (?:RSA )?PRIVATE KEY-----", "critical", "Private key"),
        ("slack_token", r"xox[bpors]-[A-Za-z0-9-]{10,}", "high", "Slack token"),
        ("stripe_key", r"(?:sk|pk)_(?:test|live)_[A-Za-z0-9]{20,}", "critical", "Stripe key"),
        ("password_assignment", r"(?:password|passwd|pwd)\s*[=:]\s*\S{4,}", "high", "Password assignment"),
        ("db_connection_string", r"(?:postgres|mysql|mongodb|redis)://[^\s]{10,}", "high", "Database connection string"),
    ]

    INJECTION_PATTERNS: List[Tuple[str, str, str, str]] = [
        (
            "prompt_override",
            r"(?i)ignore\s+(all\s+)?(previous|prior|above)\s+(instructions?|prompts?|rules?)",
            "high",
            "Attempt to override system instructions",
        ),
        (
            "identity_override",
            r"(?i)you\s+are\s+now\s+(?:a\s+)?(?:different|new|my)",
            "high",
            "Attempt to force identity override",
        ),
        (
            "disregard_instructions",
            r"(?i)disregard\s+(?:all\s+)?(?:previous|prior|your)\s+(?:instructions?|programming|rules?)",
            "high",
            "Attempt to disregard prior instructions",
        ),
        (
            "code_eval_injection",
            r"(?i)(?:execute|run|eval)\s*\(",
            "high",
            "Code evaluation pattern in prompt",
        ),
        (
            "shell_injection",
            r"(?:;|\||&&)\s*(?:rm|curl|wget|nc|ncat|bash|sh|python|perl)\s",
            "high",
            "Shell command injection pattern",
        ),
    ]

    @classmethod
    def scan(cls, text: str, redact: bool = True) -> SecurityScanResponse:
        """Scans text for secrets and injection attempts, returning sanitized text."""
        if not text:
            return SecurityScanResponse(is_safe=True, clean_text="", findings=[])

        findings: List[SecurityFinding] = []
        clean = text
        has_critical = False

        for name, pattern, level, desc in cls.SECRET_PATTERNS:
            matches = list(re.finditer(pattern, clean))
            for m in matches:
                matched_val = m.group(0)
                findings.append(
                    SecurityFinding(
                        pattern=name,
                        threat_level=level,
                        description=desc,
                        matched_text=matched_val[:4] + "..." if len(matched_val) > 4 else "***",
                    )
                )
                if level in ("high", "critical"):
                    has_critical = True

            if redact and matches:
                clean = re.sub(pattern, "[REDACTED_SECRET]", clean)

        for name, pattern, level, desc in cls.INJECTION_PATTERNS:
            matches = list(re.finditer(pattern, text))
            for m in matches:
                matched_val = m.group(0)
                findings.append(
                    SecurityFinding(
                        pattern=name,
                        threat_level=level,
                        description=desc,
                        matched_text=matched_val[:30] + ("..." if len(matched_val) > 30 else ""),
                    )
                )
                has_critical = True

        return SecurityScanResponse(
            is_safe=(not has_critical),
            clean_text=clean,
            findings=findings,
        )


security_scanner = SecurityScanner()
