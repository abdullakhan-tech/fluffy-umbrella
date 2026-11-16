"""Small, explicit allow-list of safe local tools. Never executes arbitrary code."""
import ast
import operator
from datetime import datetime, timezone

_BINOPS = {ast.Add: operator.add, ast.Sub: operator.sub, ast.Mult: operator.mul, ast.Div: operator.truediv, ast.FloorDiv: operator.floordiv, ast.Mod: operator.mod, ast.Pow: operator.pow}
_UNARY = {ast.UAdd: operator.pos, ast.USub: operator.neg}

def _eval(node):
    if isinstance(node, ast.Expression): return _eval(node.body)
    if isinstance(node, ast.Constant) and type(node.value) in (int, float): return node.value
    if isinstance(node, ast.BinOp) and type(node.op) in _BINOPS:
        if isinstance(node.op, ast.Pow) and abs(_eval(node.right)) > 8: raise ValueError("Exponent too large")
        result = _BINOPS[type(node.op)](_eval(node.left), _eval(node.right))
        if abs(result) > 10**15: raise ValueError("Result too large")
        return result
    if isinstance(node, ast.UnaryOp) and type(node.op) in _UNARY: return _UNARY[type(node.op)](_eval(node.operand))
    raise ValueError("Only basic arithmetic is supported")

def calculate(expression: str) -> str:
    if len(expression) > 120: raise ValueError("Expression too long")
    tree = ast.parse(expression, mode="eval")
    return str(_eval(tree))

def run_tool(name: str, arguments: dict) -> dict:
    if name == "calculator":
        return {"tool": name, "result": calculate(str(arguments.get("expression", "")))}
    if name == "utc_time":
        return {"tool": name, "result": datetime.now(timezone.utc).isoformat()}
    raise ValueError("Tool is not available")
