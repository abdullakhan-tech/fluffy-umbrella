import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const html = readFileSync(join(root, "website/app.html"), "utf8");
const css = readFileSync(join(root, "website/app.css"), "utf8");
const js = readFileSync(join(root, "website/app.js"), "utf8");

test("workspace loads one relative stylesheet and script", () => {
  assert.match(html, /name="viewport"[^>]*width=device-width/);
  assert.equal(html.split('href="app.css"').length - 1, 1);
  assert.equal(html.split('src="app.js"').length - 1, 1);
  assert.ok(!html.includes('href="/app.css"'));
  assert.ok(!html.includes('src="/app.js"'));
  assert.ok(html.includes('id="workspaceContent"'));
});

test("every visible navigation view has a renderer and route", () => {
  const views = [...html.matchAll(/data-view="([a-z]+)"/g)].map((match) => match[1]);
  assert.ok(views.length >= 10, "expected the full workspace navigation");
  for (const view of new Set(views)) {
    const renderer = view[0].toUpperCase() + view.slice(1);
    assert.ok(js.includes('case "' + view + '": render' + renderer + 'View(container)'), "missing route for " + view);
    assert.ok(js.includes("function render" + renderer + "View(container)"), "missing renderer for " + view);
  }
});

test("mobile layout has a real off-canvas sidebar", () => {
  assert.ok(css.includes(".app-shell") && css.includes("display: flex;"));
  assert.ok(css.includes("@media (max-width: 768px)"));
  assert.ok(css.includes("position: fixed;") && css.includes(".sidebar.open"));
  assert.ok(js.includes('classList.add("open")'));
  assert.ok(js.includes('classList.remove("open")'));
});

test("projects can be created, persisted, listed and deleted", () => {
  assert.ok(js.includes('localStorage.getItem("zaythar_projects")'));
  assert.ok(js.includes("function renderProjectsView(container)"));
  assert.ok(js.includes("function renderProjectsList()"));
  assert.ok(js.includes("STATE.projects.push("));
  assert.ok(js.includes('localStorage.setItem("zaythar_projects", JSON.stringify(STATE.projects))'));
  assert.ok(js.includes("STATE.projects = STATE.projects.filter"));
});


test("Python runner uses a real Pyodide runtime instead of echoing source text", () => {
  assert.ok(js.includes("loadPyodide"));
  assert.ok(js.includes("runPythonAsync(code)"));
  assert.ok(js.includes("setStdout"));
  assert.ok(!js.includes("Code executed successfully with return code 0."));
});

test("browser workspace does not advertise an unimplemented Firestore sync action", () => {
  assert.ok(js.includes("does not yet have an authenticated Firestore adapter"));
  assert.ok(!js.includes("id=\"syncFirestoreBtn\""));
  assert.ok(!js.includes('$("#syncFirestoreBtn")'));
});

test("untrusted toast and agent response text is inserted safely", () => {
  assert.ok(js.includes("messageNode.textContent = String(message ?? \"\")"));
  assert.ok(js.includes("escapeHtml(res.status)"));
  assert.ok(js.includes("escapeHtml(res.tool_used)"));
  assert.ok(!js.includes("toast.innerHTML ="));
});

test("offline chat clearly reports that no AI response was generated", () => {
  assert.ok(js.includes("no AI response was generated"));
  assert.ok(!js.includes("processing your session via local state"));
});

test("persona and project cards collapse to a single column on mobile", () => {
  assert.ok(css.includes(".personas-grid"));
  assert.ok(css.includes(".projects-grid"));
  assert.match(css, /@media \(max-width: 768px\)[\s\S]*\.personas-grid,[\s\S]*\.projects-grid[\s\S]*grid-template-columns: minmax\(0, 1fr\)/);
});
