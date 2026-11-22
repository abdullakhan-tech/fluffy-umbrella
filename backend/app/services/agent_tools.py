"""Deterministic agent tools enforcing safety, bounded execution, and least privilege (Rules 1, 4 & 5)."""

import ast
import operator
import re
from typing import Any, Dict, List, Tuple


class SafeMathEvaluator(ast.NodeVisitor):
    """Safely evaluates arithmetic expressions using AST without eval()."""

    ALLOWED_OPERATORS = {
        ast.Add: operator.add,
        ast.Sub: operator.sub,
        ast.Mult: operator.mul,
        ast.Div: operator.truediv,
        ast.FloorDiv: operator.floordiv,
        ast.Mod: operator.mod,
        ast.Pow: operator.pow,
        ast.USub: operator.neg,
        ast.UAdd: operator.pos,
    }

    def evaluate(self, expr_str: str) -> float:
        # Strip common math command prefixes
        cleaned = re.sub(r"^(calculate|compute|solve|math|eval)\s*:?\s*", "", expr_str, flags=re.IGNORECASE).strip()
        parsed = ast.parse(cleaned, mode="eval")
        return self.visit(parsed.body)

    def visit_BinOp(self, node: ast.BinOp) -> Any:
        op_type = type(node.op)
        if op_type not in self.ALLOWED_OPERATORS:
            raise ValueError(f"Unsupported math operator: {op_type.__name__}")

        left = self.visit(node.left)
        right = self.visit(node.right)

        # Protect against exponent DoS attacks
        if op_type == ast.Pow and (abs(right) > 100 or abs(left) > 10000):
            raise ValueError("Exponent or base exceeds safe calculation limits")

        if op_type in (ast.Div, ast.FloorDiv, ast.Mod) and right == 0:
            raise ZeroDivisionError("Division by zero")

        return self.ALLOWED_OPERATORS[op_type](left, right)

    def visit_UnaryOp(self, node: ast.UnaryOp) -> Any:
        op_type = type(node.op)
        if op_type not in self.ALLOWED_OPERATORS:
            raise ValueError(f"Unsupported unary operator: {op_type.__name__}")
        operand = self.visit(node.operand)
        return self.ALLOWED_OPERATORS[op_type](operand)

    def visit_Constant(self, node: ast.Constant) -> Any:
        if isinstance(node.value, (int, float)):
            return node.value
        raise ValueError(f"Non-numeric constant not permitted: {node.value}")

    def generic_visit(self, node: ast.AST) -> Any:
        raise ValueError(f"Expression syntax not allowed: {type(node).__name__}")


def execute_calculator(expression: str) -> Dict[str, Any]:
    """Evaluates an arithmetic expression safely."""
    evaluator = SafeMathEvaluator()
    try:
        result = evaluator.evaluate(expression)
        return {
            "success": True,
            "expression": expression,
            "result": result,
            "formatted": f"{result:g}" if isinstance(result, float) else str(result),
        }
    except Exception as e:
        return {
            "success": False,
            "expression": expression,
            "error": str(e),
        }


def execute_text_summarizer(text: str, max_points: int = 4) -> Dict[str, Any]:
    """Extracts structural summary, metrics, and key points from text."""
    clean_text = text.strip()
    if not clean_text:
        return {"success": False, "error": "Empty text provided"}

    sentences = [s.strip() for s in re.split(r"[.!?\n]+", clean_text) if s.strip()]
    words = clean_text.split()
    word_count = len(words)
    char_count = len(clean_text)

    # Pick informative key points
    key_points = sentences[:max_points]

    return {
        "success": True,
        "metrics": {
            "word_count": word_count,
            "char_count": char_count,
            "sentence_count": len(sentences),
        },
        "key_points": key_points,
        "condensed_summary": " ".join(key_points),
    }


def execute_code_inspector(code: str, language: str = "python") -> Dict[str, Any]:
    """Inspects code structure, basic syntax, and metrics safely."""
    clean_code = code.strip()
    if not clean_code:
        return {"success": False, "error": "Empty code snippet provided"}

    lines = clean_code.splitlines()
    line_count = len(lines)

    findings: List[str] = []
    syntax_valid = True

    if language.lower() in ("python", "py"):
        try:
            tree = ast.parse(clean_code)
            functions = [n.name for n in ast.walk(tree) if isinstance(n, (ast.FunctionDef, ast.AsyncFunctionDef))]
            classes = [n.name for n in ast.walk(tree) if isinstance(n, ast.ClassDef)]
            imports = [
                n.names[0].name for n in ast.walk(tree) if isinstance(n, ast.Import)
            ] + [
                f"{n.module}.{n.names[0].name}" for n in ast.walk(tree) if isinstance(n, ast.ImportFrom) and n.module
            ]

            findings.append(f"Python syntax parsed successfully ({len(functions)} functions, {len(classes)} classes)")
            return {
                "success": True,
                "syntax_valid": True,
                "metrics": {
                    "line_count": line_count,
                    "function_count": len(functions),
                    "class_count": len(classes),
                    "import_count": len(imports),
                },
                "functions": functions,
                "classes": classes,
                "imports": imports[:10],
                "findings": findings,
            }
        except SyntaxError as se:
            syntax_valid = False
            findings.append(f"Syntax error on line {se.lineno}: {se.msg}")
            return {
                "success": True,
                "syntax_valid": False,
                "error": f"Line {se.lineno}: {se.msg}",
                "findings": findings,
            }

    # Generic bracket balance inspection for other languages
    brackets = {"(": ")", "{": "}", "[": "]"}
    stack = []
    balanced = True
    for idx, ch in enumerate(clean_code, 1):
        if ch in brackets:
            stack.append((ch, idx))
        elif ch in brackets.values():
            if not stack:
                balanced = False
                findings.append(f"Unmatched closing bracket '{ch}' at pos {idx}")
                break
            top, _ = stack.pop()
            if brackets[top] != ch:
                balanced = False
                findings.append(f"Mismatched bracket '{top}' closed by '{ch}' at pos {idx}")
                break

    if stack and balanced:
        balanced = False
        findings.append(f"Unclosed open bracket '{stack[-1][0]}' at pos {stack[-1][1]}")

    return {
        "success": True,
        "syntax_valid": balanced,
        "metrics": {"line_count": line_count},
        "findings": findings or ["All brackets cleanly balanced."],
    }
