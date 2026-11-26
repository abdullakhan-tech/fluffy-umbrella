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
  assert.equal((html.match(/<link rel="stylesheet" href="app\\.css">/g) || []).length, 1);
  assert.equal((html.match(/<script src="app\\.js" defer><\\/script>/g) || []).length, 1);
  assert.doesNotMatch(html, /(?:href|src)="\\/(?:app\\.css|app\\.js)"/);
  assert.match(html, /id="workspaceContent"/);
});

test("every visible navigation view has a renderer", () => {
  const views = [...html.matchAll(/data-view="([a-z]+)"/g)].map((match) => match[1]);
  assert.ok(views.length >= 10, "expected the full workspace navigation");
  for (const view of new Set(views)) {
    assert.match(js, new RegExp('case "' + view + '":\\\\s*render[A-Za-z]+\\\\(container\\\\)'));
    const renderer = view[0].toUpperCase() + view.slice(1);
    assert.match(js, new RegExp("function render" + renderer + "View\\\\(container\\\\)"));
  }
});

test("mobile layout has a real off-canvas sidebar", () => {
  assert.match(css, /\\.app-shell\\s*\\{[^}]*display:\\s*flex/s);
  assert.match(css, /@media\\s*\\(max-width:\\s*768px\\)/);
  assert.match(css, /\\.sidebar\\s*\\{[^}]*position:\\s*fixed/s);
  assert.match(css, /\\.sidebar\\.open\\s*\\{[^}]*transform:\\s*translateX\\(0\\)/s);
  assert.match(js, /openSidebarBtn[\\s\\S]*?classList\\.add\\("open"\\)/);
  assert.match(js, /closeSidebarBtn[\\s\\S]*?classList\\.remove\\("open"\\)/);
});

test("projects can be created, persisted, listed and deleted", () => {
  assert.match(js, /projects:\\s*JSON\\.parse\\(localStorage\\.getItem\\("zaythar_projects"/);
  assert.match(js, /function renderProjectsView\\(container\\)/);
  assert.match(js, /function renderProjectsList\\(\\)/);
  assert.match(js, /STATE\\.projects\\.push\\(/);
  assert.match(js, /localStorage\\.setItem\\("zaythar_projects", JSON\\.stringify\\(STATE\\.projects\\)\\)/);
  assert.match(js, /STATE\\.projects\\s*=\\s*STATE\\.projects\\.filter/);
});
