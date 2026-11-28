import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

process.env.DATA_DIR = fs.mkdtempSync(path.join(os.tmpdir(), 'zo-'));
const { callTool, TOOLS } = await import('../lib/tools.js');
const { agentStore } = await import('../lib/store.js');
const { llmReady, ask } = await import('../lib/llm.js');
const { firecrawlReady } = await import('../lib/firecrawl.js');
const { firebaseReady } = await import('../lib/firebase.js');

const mk = () => ({
  d: {
    state: { approvals: [], audit: [] },
    save() {},
    draft() {},
    search: async () => [{ title: 't', url: 'https://example.com', snippet: 's' }],
    fetchText: async () => 'sample text content',
    scrape: async () => 'scraped text',
    crawl: async () => ({ crawled: 1 })
  },
  roster: {
    friday: { role: 'Coder', persona: 'Quick coder' },
    omnivex: { role: 'Researcher', persona: 'Deep researcher' },
    jarvis: { role: 'Orchestrator', persona: 'Coordinator' }
  }
});

test('allowlist: agent apna na hone wala tool nahi chala sakta', async () => {
  await assert.rejects(callTool('rio', ['draft_post'], { tool: 'propose_patch', args: {} }, mk()), /allowed nahi/);
});

test('propose_patch sirf approval queue mein jata hai', async () => {
  const c = mk();
  const r = await callTool('friday', ['propose_patch'], { tool: 'propose_patch', args: { title: 'Fix', files: [{ path: 'a.txt', content: 'x' }] } }, c);
  assert.match(r, /manzoori/);
  assert.equal(c.d.state.approvals[0].status, 'pending');
  assert.equal(c.d.state.audit.at(-1).tier, 'consequential');
});

test('secret files block', async () => {
  for (const p of ['.env', 'app/google-services.json', '../x', '/etc/passwd']) {
    await assert.rejects(callTool('friday', ['propose_patch'], { tool: 'propose_patch', args: { title: 't', files: [{ path: p, content: 'x' }] } }, mk()), /blocked/);
  }
});

test('remember sirf us agent ki memory mein likhta hai', async () => {
  await callTool('friday', ['remember'], { tool: 'remember', args: { fact: 'test fact' } }, mk());
  assert.ok(agentStore('friday').memory().facts.includes('test fact'));
  assert.equal(agentStore('jarvis').memory().facts.length, 0);
});

test('teach dusre agent ki memory mein lesson likhta hai', async () => {
  const c = mk();
  await callTool('friday', ['teach'], { tool: 'teach', args: { agent: 'omnivex', lesson: 'Hifazat zaroori hai' } }, c);
  assert.ok(agentStore('omnivex').memory().lessons.includes('Hifazat zaroori hai'));
});

test('firecrawl tools exist and respect permissions', async () => {
  assert.ok(TOOLS.firecrawl_scrape, 'firecrawl_scrape tool exists');
  assert.ok(TOOLS.firecrawl_crawl, 'firecrawl_crawl tool exists');
  assert.ok(TOOLS.firecrawl_search, 'firecrawl_search tool exists');

  // Omnivex can call firecrawl_scrape
  const c = mk();
  const res = await callTool('omnivex', ['firecrawl_scrape'], { tool: 'firecrawl_scrape', args: { url: 'https://example.com' } }, c);
  assert.ok(typeof res === 'string');
});

test('ai engine and offline fallback respond correctly', async () => {
  // Even without external API keys, fallback returns valid JSON structure
  const res = await ask('Boss system prompt', 'Sirf valid JSON do please');
  const parsed = JSON.parse(res);
  assert.ok(parsed.final || parsed.reply);
});

test('firebase status checks do not throw', () => {
  assert.equal(typeof firebaseReady(), 'boolean');
  assert.equal(typeof firecrawlReady(), 'boolean');
});
