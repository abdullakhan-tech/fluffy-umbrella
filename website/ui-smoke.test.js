import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const html = readFileSync(join(root, "website/app.html"), "utf8");
const css = readFileSync(join(root, "website/app.css"), "utf8");
const js = readFileSync(join(root, "website/app.js"), "utf8");

test("web workspace loads its responsive shell and real script", () => {
  assert.match(html, /name="viewport"[^>]*width=device-width/);
  assert.match(html, /href="\/app\.css"/);
  assert.match(html, /src="\/app\.js"/);
  assert.match(css, /@media\s*\(max-width:\s*840px\)/);
  assert.match(css, /\.app-shell\s*\{[^}]*display:block/s);
  assert.match(css, /\.sidebar\s*\{[^}]*position:fixed/s);
  assert.match(css, /\.main-area\s*\{[^}]*width:100%/s);
});

test("every visible web workspace view has a renderer and navigation binding", () => {
  for (const view of ["chat", "agent", "security", "library", "gpts", "voice", "search", "projects", "code", "canvas", "tools", "settings"]) {
    assert.match(js, new RegExp("\\b" + view + ": render" + view[0].toUpperCase() + view.slice(1) + "\\b"));
  }
  assert.match(js, /else if \(currentView === "projects"\) bindProjectsActions\(\)/);
});

test("projects are real local records with create, open and delete actions", () => {
  assert.match(js, /const projects = Array\.isArray\(state\.projects\) \? state\.projects : \[\]/);
  assert.match(js, /id="projectCreateForm"/);
  assert.match(js, /data-project-open=/);
  assert.match(js, /data-project-delete=/);
  assert.match(js, /function bindProjectsActions\(\)/);
  assert.doesNotMatch(js, /Fluffy Umbrella AI Companion/);
});

test("disable-save-history preference prevents chat persistence", () => {
  assert.match(js, /if \(!state\.saveHistory\) persisted\.chats = \[\]/);
  assert.match(js, /saveHistoryCheck\.onchange/);
});
