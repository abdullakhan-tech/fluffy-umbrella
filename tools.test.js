import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
process.env.DATA_DIR = fs.mkdtempSync(path.join(os.tmpdir(), 'zo-'));
const { callTool } = await import('../lib/tools.js');
const { agentStore } = await import('../lib/store.js');
const mk = () => ({ d: { state: { approvals: [], audit: [] }, save() {}, draft() {} }, roster: { friday: { role: 'Coder', persona: '' } } });

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
